package com.chenzixin.wrongbook.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.chenzixin.wrongbook.core.ImageUtils.restoreHd
import com.chenzixin.wrongbook.core.ImageUtils.scaledToMaxSide
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.QuestionImageStore
import com.chenzixin.wrongbook.data.local.dao.QuestionImageDao
import com.chenzixin.wrongbook.data.local.entity.QuestionImageEntity
import com.chenzixin.wrongbook.data.ml.SuperResolutionEngine
import com.chenzixin.wrongbook.data.prefs.AppPrefs
import com.chenzixin.wrongbook.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 题目插图的读写：单独上传、删除、高清重绘。
 *
 * 「高清重绘」有两条路：
 * 1. **AI 超分**（默认）：内置 EDSR×4 模型，小图分块放大 4 倍，边缘更利，约 5–8 秒；
 * 2. **Lanczos + 反锐化**：秒出，不依赖模型，图太大或模型不可用时自动走这条。
 *
 * 重绘后写入新文件、替换数据库记录，旧文件随之删除，避免目录里堆垃圾。
 */
@Singleton
class QuestionImageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: QuestionImageDao,
    private val store: QuestionImageStore,
    private val superResolution: SuperResolutionEngine,
    private val appPrefs: AppPrefs,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    fun observe(questionId: Long): Flow<List<QuestionImageEntity>> = dao.observeByQuestion(questionId)

    suspend fun add(questionId: Long, bitmap: Bitmap, kind: String = "FIGURE"): WbResult<Long> =
        withContext(io) {
            when (val saved = store.saveFigure(bitmap)) {
                is WbResult.Ok -> runCatching {
                    dao.insert(
                        QuestionImageEntity(
                            questionId = questionId,
                            path = saved.value.imagePath,
                            thumbPath = saved.value.thumbPath,
                            kind = kind,
                        ),
                    )
                }.fold(
                    onSuccess = { WbResult.Ok(it) },
                    onFailure = { WbResult.Err("插图入库失败", it) },
                )
                is WbResult.Err -> saved
            }
        }

    suspend fun delete(image: QuestionImageEntity): WbResult<Unit> = withContext(io) {
        runCatching {
            dao.deleteById(image.id)
            store.delete(listOf(image.path, image.thumbPath))
        }.fold(
            onSuccess = { WbResult.Ok(Unit) },
            onFailure = { WbResult.Err("删除失败", it) },
        )
    }

    /**
     * 高清重绘：读原图 → 超分或放大锐化 → 覆盖为新记录 → 删除旧文件。
     *
     * [onStage] 只在超分时被调用（可能要几秒），普通放大不会。
     */
    suspend fun restoreHd(
        image: QuestionImageEntity,
        onStage: (String) -> Unit = {},
    ): WbResult<Unit> = withContext(io) {
        val file = File(context.filesDir, image.path)
        if (!file.exists()) return@withContext WbResult.Err("原图文件不见了")

        val decoded = BitmapFactory.decodeFile(file.absolutePath)
            ?: return@withContext WbResult.Err("图片解码失败")

        val enabled = runCatching { appPrefs.superResolution.first() }.getOrDefault(true)
        var enhanced: Bitmap? = null
        if (enabled) {
            onStage("AI 超分中，最多 9 块（约 5–8 秒）…")
            enhanced = runCatching {
                superResolution.upscale(decoded) { done, total ->
                    onStage("AI 超分中 $done/$total…")
                }
            }.getOrNull()?.bitmap
        }

        val result = enhanced?.let { upscaled ->
            // 超分只会把「小图」放大，理论上不会超上限；这里仍兜一层
            if (maxOf(upscaled.width, upscaled.height) > HD_MAX_SIDE) {
                upscaled.scaledToMaxSide(HD_MAX_SIDE).also {
                    if (it !== upscaled) upscaled.recycle()
                }
            } else {
                upscaled
            }
        } ?: decoded.restoreHd()

        if (result !== decoded) decoded.recycle()

        when (val saved = store.saveFigure(result)) {
            is WbResult.Ok -> {
                runCatching {
                    dao.insert(
                        image.copy(
                            id = 0,
                            path = saved.value.imagePath,
                            thumbPath = saved.value.thumbPath,
                        ),
                    )
                    dao.deleteById(image.id)
                    store.delete(listOf(image.path, image.thumbPath))
                }.fold(
                    onSuccess = { WbResult.Ok(Unit) },
                    onFailure = { WbResult.Err("重绘结果保存失败", it) },
                )
            }
            is WbResult.Err -> saved
        }
    }

    private companion object {
        const val HD_MAX_SIDE = 2400
    }
}
