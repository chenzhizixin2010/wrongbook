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
        if (!file.exists() || file.length() == 0L) return false

        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return false
        val upright = autoOrient(original)
        if (upright !== original) original.recycle()

        val enhanced = upright.toPaperClean()
        if (enhanced !== upright) upright.recycle()

        val full = enhanced.scaledToMaxSide(1600)
        val bytes = full.toWebp()
        if (full !== enhanced) full.recycle()
        // 先确认新图自己还能解出来，解不出来就别覆盖原图（宁可这张不处理）
        if (bytes.isEmpty() || !decodable(bytes)) {
            enhanced.recycle()
            return false
        }

        val replaced = writeAtomically(file, bytes)

        // 缩略图：路径由主图推导，推导结果与主图同名就跳过 ——
        // 否则会拿 200px 的缩略图把主图覆盖掉（老版本对 .jpg/.png 路径就会这样）
        val thumbRelative = thumbPathOf(relativePath)
        if (thumbRelative != null) {
            val thumbFile = File(context.filesDir, thumbRelative)
            if (thumbFile.exists() && thumbFile.absolutePath != file.absolutePath) {
                val thumb = enhanced.scaledToMaxSide(200)
                val thumbBytes = thumb.toWebp(quality = 70)
                if (thumb !== enhanced) thumb.recycle()
                if (thumbBytes.isNotEmpty() && decodable(thumbBytes)) {
                    runCatching { writeAtomically(thumbFile, thumbBytes) }
                }
            }
        }
        enhanced.recycle()
        return replaced
    }

    /** 只读文件头判断能不能解出来（InJustDecodeBounds 不分配像素内存）。 */
    private fun decodable(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        return runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            options.outWidth > 0 && options.outHeight > 0
        }.getOrDefault(false)
    }

    /**
     * 原子替换：先写 .tmp 再改名。
     *
     * 绝对不允许「先删原文件再改名」—— 那一步失败就等于把用户的题目图弄丢了
     * （0.13.0 之前就是这么写的，重命名失败会留下一个指向空文件的数据库记录）。
     */
    private fun writeAtomically(target: File, bytes: ByteArray): Boolean {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        return try {
            tmp.writeBytes(bytes)
            if (tmp.renameTo(target)) {
                true
            } else {
                // 改名失败（有些 ROM / 外置存储会拒绝）：直接覆盖写，原文件保留到最后一步
                target.writeBytes(bytes)
                tmp.delete()
                true
            }
        } catch (t: java.io.IOException) {
            runCatching { tmp.delete() }
            false
        }
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

    companion object {
        /** OCR 总共读不到这么多字符时，认为打分不可信 */
        const val MIN_OCR_CHARS = 8

        /**
         * 由主图相对路径推导缩略图路径（纯函数，可单测）。
         *
         * 返回 null 表示**不要**重建缩略图：路径没有扩展名，或者推导结果会和主图同名
         * （例如老数据里图片是 `.jpg`，硬拼 `_thumb.webp` 会生成同目录的另一个文件；
         * 而用 `replace(".webp", ...)` 这种写法则会得到和主图完全一样的路径 —— 那会把主图覆盖掉）。
         */
        fun thumbPathOf(path: String): String? {
            val slash = path.lastIndexOf('/')
            val dot = path.lastIndexOf('.')
            if (dot <= slash || dot <= 0) return null
            val candidate = path.substring(0, dot) + "_thumb.webp"
            return candidate.takeIf { it != path }
        }
    }
}
