package com.chenzixin.wrongbook.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.camera.core.ImageProxy
import com.google.common.util.concurrent.ListenableFuture
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** 相机/图片相关的纯工具；不持有 Context，方便复用与测试。 */
object ImageUtils {

    /** ImageProxy（JPEG）→ Bitmap，并按 rotationDegrees 摆正。 */
    fun imageProxyToBitmap(proxy: ImageProxy): Bitmap? {
        val buffer = proxy.planes.firstOrNull()?.buffer ?: return null
        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val degrees = proxy.imageInfo.rotationDegrees
        return if (degrees == 0) decoded else decoded.rotated(degrees.toFloat())
    }

    fun Bitmap.rotated(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    /** 按长边缩放（只缩不放）。 */
    fun Bitmap.scaledToMaxSide(maxSide: Int): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= maxSide) return this
        val scale = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    /** 归一化矩形 → 原图上的像素矩形（已做边界收敛，避免 createBitmap 抛异常）。 */
    fun Bitmap.cropToNormalized(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ): Bitmap {
        val x = (left.coerceIn(0f, 1f) * width).toInt().coerceIn(0, width - 1)
        val y = (top.coerceIn(0f, 1f) * height).toInt().coerceIn(0, height - 1)
        val r = (right.coerceIn(0f, 1f) * width).toInt().coerceIn(x + 1, width)
        val b = (bottom.coerceIn(0f, 1f) * height).toInt().coerceIn(y + 1, height)
        return Bitmap.createBitmap(this, x, y, r - x, b - y)
    }

    /** 转 base64 JPEG，供视觉模型使用（不含 data: 前缀）。 */
    fun Bitmap.toBase64Jpeg(maxSide: Int = 1400, quality: Int = 80): String {
        val scaled = scaledToMaxSide(maxSide)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (scaled !== this) scaled.recycle()
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }


    /**
     * OCR 前预处理：灰度 + 提高对比度 + 保证足够分辨率。
     *
     * 实测：手写/倾斜的试卷照片直接丢给 ML Kit，二值化不足会让细笔画被吞掉；
     * 先灰度+拉对比度、并保证短边不低（≥1200px）能明显提升中文识别率。
     */
    fun Bitmap.prepareForOcr(maxSide: Int = 2200, minWidth: Int = 1200): Bitmap {
        var work = scaledToMaxSide(maxSide)
        if (work.width < minWidth) {
            val ratio = minWidth.toFloat() / work.width
            val up = Bitmap.createScaledBitmap(
                work,
                minWidth,
                (work.height * ratio).toInt().coerceAtLeast(1),
                true,
            )
            if (up !== work) {
                if (work !== this) work.recycle()
                work = up
            }
        }

        val out = Bitmap.createBitmap(work.width, work.height, Bitmap.Config.ARGB_8888)
        val matrix = android.graphics.ColorMatrix().apply {
            setSaturation(0f)
            postConcat(
                android.graphics.ColorMatrix(
                    floatArrayOf(
                        1.45f, 0f, 0f, 0f, -55f,
                        0f, 1.45f, 0f, 0f, -55f,
                        0f, 0f, 1.45f, 0f, -55f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        }
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
        }
        android.graphics.Canvas(out).drawBitmap(work, 0f, 0f, paint)
        if (work !== this) work.recycle()
        return out
    }


    /**
     * 估算「有文字的区域」，用于拍照后自动框选。
     *
     * 缩到 400px 再逐像素判断，几十毫秒就能出结果；返回归一化坐标。
     */
    fun Bitmap.detectContentBox(): com.chenzixin.wrongbook.domain.scan.ContentBox? {
        val small = scaledToMaxSide(400)
        val w = small.width
        val h = small.height
        if (w <= 0 || h <= 0) return null
        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        if (small !== this) small.recycle()

        val threshold = 0.55f * 255f
        val box = com.chenzixin.wrongbook.domain.scan.InkBounds.detect(w, h, step = 2) { x, y ->
            val c = pixels[y * w + x]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            (0.299f * r + 0.587f * g + 0.114f * b) < threshold
        } ?: return null

        return com.chenzixin.wrongbook.domain.scan.ContentBox(
            left = box.left,
            top = box.top,
            right = box.right,
            bottom = box.bottom,
        )
    }


    /**
     * 把题目图处理成「白底黑字」。
     *
     * 流程：灰度 → Sauvola 局部二值化 → 反相检查（保证字黑底白）→
     * 二值化结果太极端（几乎全黑/全白）时退回灰度增强，避免把整题压成黑块。
     */
    fun Bitmap.toHighContrast(): Bitmap {
        val src = scaledToMaxSide(1600)
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return src

        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val gray = IntArray(w * h)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            gray[i] = ((0.299f * r + 0.587f * g + 0.114f * b).toInt()).coerceIn(0, 255)
        }

        val binary = Binarizer.ensureWhiteBackground(Binarizer.sauvola(gray, w, h))
        val blackRatio = Binarizer.blackRatio(binary)

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        if (blackRatio in 0.005f..0.55f) {
            val buffer = IntArray(w * h) { i ->
                if (binary[i] == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
            out.setPixels(buffer, 0, w, 0, 0, w, h)
        } else {
            // 二值化失败：整片过黑或过白，退回「灰度 + 拉对比度」，至少比原图清楚
            val buffer = IntArray(w * h) { i ->
                val v = ((gray[i] - 128) * 1.35f + 128f).toInt().coerceIn(0, 255)
                (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
            out.setPixels(buffer, 0, w, 0, 0, w, h)
        }

        if (src !== this) src.recycle()
        return out
    }


    /**
     * 纸面提亮（保留彩色）：阴影/偏黄的纸变白，墨迹的颜色和深浅都保留。
     *
     * 相比 [toHighContrast] 的黑白二值化，这个更"温和"：
     * 铅笔的浅灰还在、红笔蓝笔还是彩色，只是背景不再发灰发暗。
     */
    fun Bitmap.toPaperClean(strength: Float = 1f): Bitmap {
        val src = scaledToMaxSide(1600)
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return src

        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val flattened = IlluminationFlattener.flatten(pixels, w, h, strength)

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(flattened, 0, w, 0, 0, w, h)
        if (src !== this) src.recycle()
        return out
    }


    /**
     * 高清重绘：把小图放大并锐化，让印刷体轮廓更清楚。
     *
     * 不是 AI 超分（离线、无模型），而是「双三次放大 + 反锐化掩模 + 轻度提亮」：
     * 对扫描件/截图这类线条图效果不错，对照片只能算勉强。
     */
    fun Bitmap.restoreHd(targetMaxSide: Int = 2400, strength: Float = 0.55f): Bitmap {
        val scale = (targetMaxSide.toFloat() / maxOf(width, height)).coerceIn(1f, 3f)
        val w = (width * scale).toInt().coerceAtLeast(1)
        val h = (height * scale).toInt().coerceAtLeast(1)

        // 1) Lanczos3 放大（比双线性保留更多细节）
        val srcPixels = IntArray(width * height)
        getPixels(srcPixels, 0, width, 0, 0, width, height)
        val upscaledPixels = Lanczos.upscale(srcPixels, width, height, w.toDouble() / width)

        // 2) 反锐化掩模：原图 - 模糊，再加回去
        val blurred = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (dy in -1..1) {
                    val yy = y + dy
                    if (yy !in 0 until h) continue
                    for (dx in -1..1) {
                        val xx = x + dx
                        if (xx !in 0 until w) continue
                        val c = upscaledPixels[yy * w + xx]
                        r += (c shr 16) and 0xFF
                        g += (c shr 8) and 0xFF
                        b += c and 0xFF
                        count++
                    }
                }
                blurred[y * w + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
        }

        val out = IntArray(w * h)
        for (i in upscaledPixels.indices) {
            val a = upscaledPixels[i]
            val bl = blurred[i]
            fun sharp(channel: Int): Int {
                val original = (a shr channel) and 0xFF
                val blur = (bl shr channel) and 0xFF
                val sharpened = original + (original - blur) * strength
                val lifted = sharpened * 1.04f + 4f
                return lifted.toInt().coerceIn(0, 255)
            }
            out[i] = (0xFF shl 24) or (sharp(16) shl 16) or (sharp(8) shl 8) or sharp(0)
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    /** 转成 JPEG 字节：HTML 内嵌、Word 媒体部件都用它。 */
    fun Bitmap.toJpegBytes(maxSide: Int = 900, quality: Int = 82): ByteArray {
        val scaled = scaledToMaxSide(maxSide)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (scaled !== this) scaled.recycle()
        return out.toByteArray()
    }

    fun Bitmap.toWebp(quality: Int = 82): ByteArray {
        val out = ByteArrayOutputStream()
        // WEBP_LOSSY 需要 API 30+，旧机型退回 WEBP（已废弃但可用）
        val format = if (android.os.Build.VERSION.SDK_INT >= 30) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        compress(format, quality, out)
        return out.toByteArray()
    }

    /**
     * 透视矫正：把任意四边形（源图像素坐标）拉正成矩形。
     *
     * 反向映射 + 双线性采样，避免输出出现空洞；输出长边不超过 [maxSide]。
     * 退化四边形返回 null，由 UI 提示「边框太扁，重新拖一下」。
     */
    fun Bitmap.warpQuad(
        quad: List<Pair<Float, Float>>,
        maxSide: Int = 1600,
    ): Bitmap? {
        if (quad.size != 4) return null
        val h = com.chenzixin.wrongbook.domain.scan.Homography.fromUnitSquareTo(
            quad.map { it.first.toDouble() to it.second.toDouble() },
        ) ?: return null

        fun dist(a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
            val dx = a.first - b.first
            val dy = a.second - b.second
            return kotlin.math.sqrt(dx * dx + dy * dy)
        }

        val topW = dist(quad[0], quad[1])
        val bottomW = dist(quad[3], quad[2])
        val leftH = dist(quad[0], quad[3])
        val rightH = dist(quad[1], quad[2])

        var outW = ((topW + bottomW) / 2f).toInt().coerceIn(8, maxSide)
        var outH = ((leftH + rightH) / 2f).toInt().coerceIn(8, maxSide)
        val scale = kotlin.math.min(1f, maxSide.toFloat() / maxOf(outW, outH).toFloat())
        outW = (outW * scale).toInt().coerceAtLeast(8)
        outH = (outH * scale).toInt().coerceAtLeast(8)

        val srcPixels = IntArray(width * height)
        getPixels(srcPixels, 0, width, 0, 0, width, height)
        val dst = IntArray(outW * outH)

        for (y in 0 until outH) {
            val v = (y + 0.5) / outH
            for (x in 0 until outW) {
                val u = (x + 0.5) / outW
                val mapped = h.map(u, v) ?: continue
                val sx = mapped.first.toFloat() - 0.5f
                val sy = mapped.second.toFloat() - 0.5f
                if (sx < -1f || sy < -1f || sx > width.toFloat() || sy > height.toFloat()) continue

                val x0 = kotlin.math.floor(sx).toInt().coerceIn(0, width - 1)
                val y0 = kotlin.math.floor(sy).toInt().coerceIn(0, height - 1)
                val x1 = (x0 + 1).coerceAtMost(width - 1)
                val y1 = (y0 + 1).coerceAtMost(height - 1)
                val fx = (sx - x0).coerceIn(0f, 1f)
                val fy = (sy - y0).coerceIn(0f, 1f)

                val c00 = srcPixels[y0 * width + x0]
                val c10 = srcPixels[y0 * width + x1]
                val c01 = srcPixels[y1 * width + x0]
                val c11 = srcPixels[y1 * width + x1]
                dst[y * outW + x] = bilinear(c00, c10, c01, c11, fx, fy)
            }
        }
        return Bitmap.createBitmap(dst, outW, outH, Bitmap.Config.ARGB_8888)
    }

    private fun bilinear(c00: Int, c10: Int, c01: Int, c11: Int, fx: Float, fy: Float): Int {
        fun channel(shift: Int): Int {
            val a = ((c00 shr shift) and 0xFF) * (1 - fx) * (1 - fy)
            val b = ((c10 shr shift) and 0xFF) * fx * (1 - fy)
            val c = ((c01 shr shift) and 0xFF) * (1 - fx) * fy
            val d = ((c11 shr shift) and 0xFF) * fx * fy
            return (a + b + c + d).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}

/** CameraX / Guava 的 ListenableFuture → 协程（由调用方传 executor，避免依赖 Context）。 */
suspend fun <T> ListenableFuture<T>.awaitResult(executor: java.util.concurrent.Executor): T =
    suspendCancellableCoroutine { cont ->
        addListener(
            {
                try {
                    cont.resume(get())
                } catch (t: Throwable) {
                    cont.resumeWithException(t)
                }
            },
            executor,
        )
        cont.invokeOnCancellation { cancel(true) }
    }
