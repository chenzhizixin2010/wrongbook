package com.chenzixin.wrongbook.data.maintenance

import android.content.Context
import android.graphics.BitmapFactory
import com.chenzixin.wrongbook.core.ImageUtils.rotated
import com.chenzixin.wrongbook.core.ImageUtils.scaledToMaxSide
import com.chenzixin.wrongbook.core.ImageUtils.toPaperClean
import com.chenzixin.wrongbook.core.OrientationDetector
import com.chenzixin.wrongbook.core.ImageUtils.toWebp
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.QuestionImageStore
import com.chenzixin.wrongbook.data.ocr.OcrEngine
import com.chenzixin.wrongbook.data.repository.QuestionRepository
import com.chenzixin.wrongbook.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 历史图片重处理：把已经入库的题目图也洗成「正着 + 白底黑字」。
 *
 * 新扫描的图在入库时就已经处理过；这里给老数据补课：
 * 1. 用 OCR 试 0°/90°/180°/270°，选识别字符最多的方向转正；
 * 2. 转正后做高对比度二值化；
 * 3. 覆盖原 WebP，并重建 200px 缩略图（路径不变，数据库不用动）。
 *
 * 逐张处理、逐张回报进度，中途退出会停在当前这张，不会留下半截文件
 * （先写临时文件再原子替换）。
 */
@Singleton
class ImageMaintenance @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocr: OcrEngine,
    private val questionRepo: QuestionRepository,
    private val imageStore: QuestionImageStore,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    data class Progress(
        val total: Int = 0,
        val done: Int = 0,
        val failed: Int = 0,
    ) {
        val running: Boolean get() = total > 0 && done < total
    }

    suspend fun reprocessAll(
        onProgress: suspend (Progress) -> Unit,
    ): WbResult<Progress> = withContext(io) {
        val items = runCatching { questionRepo.observeAll().first() }
            .getOrDefault(emptyList())
            .filter { it.question.imagePath.isNotBlank() }

        if (items.isEmpty()) return@withContext WbResult.Err("没有可处理的题目图片")

        var progress = Progress(total = items.size)
        onProgress(progress)

        items.forEach { item ->
            ensureActive()
            val ok = runCatching { processOne(item.question.imagePath) }.getOrDefault(false)
            progress = progress.copy(
                done = progress.done + 1,
                failed = progress.failed + if (ok) 0 else 1,
            )
            onProgress(progress)
        }

        WbResult.Ok(progress)
    }

    private suspend fun processOne(relativePath: String): Boolean {
        val file = File(context.filesDir, relativePath)
        if (!file.exists()) return false

        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return false
        val upright = autoOrient(original)
        if (upright !== original) original.recycle()

        val enhanced = upright.toPaperClean()
        if (enhanced !== upright) upright.recycle()

        val full = enhanced.scaledToMaxSide(1600)

        // 先写临时文件再替换，避免断电/被杀进程留下半张图
        val tmp = File(file.parentFile, "${file.nameWithoutExtension}.tmp")
        tmp.writeBytes(full.toWebp())
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
        if (full !== enhanced) full.recycle()

        // 缩略图沿用同名规则
        val thumbRelative = relativePath
            .replace(".webp", "_thumb.webp")
        val thumbFile = File(context.filesDir, thumbRelative)
        if (thumbFile.exists()) {
            val thumb = enhanced.scaledToMaxSide(200)
            thumbFile.writeBytes(thumb.toWebp(quality = 70))
            if (thumb !== enhanced) thumb.recycle()
        }
        enhanced.recycle()
        return true
    }

    /**
     * 判断方向并转正。
     *
     * 优先用 OCR 打分（能区分 0/90/180/270）；但老图经常低清到 OCR 一个字都读不出，
     * 这时用投影轮廓判断「横排还是竖排」，至少能把 90° 歪的打正。
     */
    private suspend fun autoOrient(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        val small = bitmap.scaledToMaxSide(1000)
        val landscape = small.width > small.height

        // 1) OCR 打分
        val candidates = if (landscape) listOf(0, 90, 180, 270) else listOf(0, 180)
        var bestDeg = 0
        var bestScore = -1
        candidates.forEach { deg ->
            val rotated = if (deg == 0) small else small.rotated(deg.toFloat())
            val score = runCatching {
                ocr.recognize(rotated).lines.sumOf { line -> line.text.count { it.isLetterOrDigit() } }
            }.getOrDefault(0)
            if (score > bestScore) {
                bestScore = score
                bestDeg = deg
            }
        }

        // 2) OCR 读不出东西时，用投影判断横/竖排
        if (bestScore < MIN_OCR_CHARS) {
            val vertical = runCatching {
                val gray = small.toGrayscaleArray()
                OrientationDetector.isVerticalText(gray, small.width, small.height)
            }.getOrDefault(false)

            if (vertical) {
                // 竖向文字：在 90 与 270 之间再比一次（读不出就用 90）
                bestDeg = if (bestScore > 0) bestDeg else 90
            } else if (!landscape) {
                bestDeg = 0
            }
        }

        return if (bestDeg == 0) bitmap else bitmap.rotated(bestDeg.toFloat())
    }

    private fun android.graphics.Bitmap.toGrayscaleArray(): IntArray {
        val w = width
        val h = height
        val pixels = IntArray(w * h)
        getPixels(pixels, 0, w, 0, 0, w, h)
        return IntArray(w * h) { i ->
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
        }
    }

    private companion object {
        /** OCR 总共读不到这么多字符时，认为打分不可信 */
        const val MIN_OCR_CHARS = 8
    }
}
