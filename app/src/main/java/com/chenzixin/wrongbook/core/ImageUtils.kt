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
