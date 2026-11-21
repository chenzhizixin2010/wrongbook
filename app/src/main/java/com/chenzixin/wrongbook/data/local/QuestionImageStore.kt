package com.chenzixin.wrongbook.data.local

import android.content.Context
import android.graphics.Bitmap
import com.chenzixin.wrongbook.core.ImageUtils.cropToNormalized
import com.chenzixin.wrongbook.core.ImageUtils.toPaperClean
import com.chenzixin.wrongbook.core.ImageUtils.scaledToMaxSide
import com.chenzixin.wrongbook.core.ImageUtils.toWebp
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.core.runCatchingWb
import com.chenzixin.wrongbook.di.IoDispatcher
import com.chenzixin.wrongbook.domain.scan.NormRect
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * 题目图片落盘：
 * - 原题图：长边 ≤1600px、WebP 质量 82
 * - 缩略图：长边 200px
 * - 数据库只存相对 filesDir 的路径（换目录/换机迁移简单）
 */
@Singleton
class QuestionImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    data class SavedImage(val imagePath: String, val thumbPath: String)

    private val dir: File
        get() = File(context.filesDir, DIR_QUESTIONS).apply { if (!exists()) mkdirs() }

    suspend fun saveCrop(source: Bitmap, rect: NormRect): WbResult<SavedImage> = withContext(io) {
        runCatchingWb("图片保存失败") {
            val name = UUID.randomUUID().toString()
            val cropped = source.cropToNormalized(rect.left, rect.top, rect.right, rect.bottom)

            // 入库前提亮纸面：颜色保留，只把背景推到接近纯白
            val enhanced = cropped.toPaperClean()

            val full = enhanced.scaledToMaxSide(MAX_SIDE)
            val fullFile = File(dir, "$name.webp")
            fullFile.writeBytes(full.toWebp())

            val thumb = enhanced.scaledToMaxSide(THUMB_SIDE)
            val thumbFile = File(dir, "${name}_thumb.webp")
            thumbFile.writeBytes(thumb.toWebp(quality = 70))

            if (full !== enhanced) full.recycle()
            if (thumb !== enhanced) thumb.recycle()
            if (enhanced !== cropped) enhanced.recycle()
            cropped.recycle()

            SavedImage(
                imagePath = "$DIR_QUESTIONS/${fullFile.name}",
                thumbPath = "$DIR_QUESTIONS/${thumbFile.name}",
            )
        }
    }

    /** 删题时清理图片；失败不抛（文件被占用不值得阻塞主流程）。 */
    suspend fun delete(paths: List<String>): Unit = withContext(io) {
        paths.filter { it.isNotBlank() }.forEach { relative ->
            runCatching { File(context.filesDir, relative).delete() }
        }
    }

    /** 读取原图用于详情页大图展示。 */
    suspend fun absoluteFile(relativePath: String): File = withContext(io) {
        File(context.filesDir, relativePath)
    }

    private companion object {
        const val DIR_QUESTIONS = "questions"
        const val MAX_SIDE = 1600
        const val THUMB_SIDE = 200
    }
}
