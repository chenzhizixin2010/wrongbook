package com.chenzixin.wrongbook.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lanczos3 重采样（纯 JVM）。
 *
 * 相比 `Bitmap.createScaledBitmap` 默认的双线性，Lanczos 保留高频更狠，
 * 试卷/几何图的细线、小字放大后不容易糊成一团，是「没有模型时最划算的质量提升」。
 *
 * 实现要点：分离式两遍（先水平后垂直），每遍对每个目标点取 6 个源点做卷积。
 */
object Lanczos {

    private const val A = 3.0

    /** 输入 ARGB，返回放大 scale 倍后的 ARGB。 */
    fun upscale(src: IntArray, width: Int, height: Int, scale: Double): IntArray {
        if (width <= 0 || height <= 0 || scale <= 0.0) return src
        val dstW = (width * scale).toInt().coerceAtLeast(1)
        val dstH = (height * scale).toInt().coerceAtLeast(1)
        if (dstW == width && dstH == height) return src

        // 先水平
        val horizontal = IntArray(dstW * height)
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until dstW) {
                val srcX = (x + 0.5) / scale - 0.5
                val base = srcX.toInt()
                var r = 0.0
                var g = 0.0
                var b = 0.0
                var a = 0.0
                var weightSum = 0.0
                for (i in base - 2..base + 3) {
                    val clamped = i.coerceIn(0, width - 1)
                    val w = kernel(srcX - i)
                    if (w == 0.0) continue
                    val c = src[rowOffset + clamped]
                    a += ((c ushr 24) and 0xFF) * w
                    r += ((c shr 16) and 0xFF) * w
                    g += ((c shr 8) and 0xFF) * w
                    b += (c and 0xFF) * w
                    weightSum += w
                }
                horizontal[y * dstW + x] = pack(a, r, g, b, weightSum)
            }
        }

        // 再垂直
        val out = IntArray(dstW * dstH)
        for (y in 0 until dstH) {
            val srcY = (y + 0.5) / scale - 0.5
            val base = srcY.toInt()
            for (x in 0 until dstW) {
                var r = 0.0
                var g = 0.0
                var b = 0.0
                var a = 0.0
                var weightSum = 0.0
                for (j in base - 2..base + 3) {
                    val clamped = j.coerceIn(0, height - 1)
                    val w = kernel(srcY - j)
                    if (w == 0.0) continue
                    val c = horizontal[clamped * dstW + x]
                    a += ((c ushr 24) and 0xFF) * w
                    r += ((c shr 16) and 0xFF) * w
                    g += ((c shr 8) and 0xFF) * w
                    b += (c and 0xFF) * w
                    weightSum += w
                }
                out[y * dstW + x] = pack(a, r, g, b, weightSum)
            }
        }
        return out
    }

    /** Lanczos 核：|x| < 3 时有效。 */
    fun kernel(x: Double): Double {
        val ax = abs(x)
        if (ax < 1e-9) return 1.0
        if (ax >= A) return 0.0
        val px = PI * x
        return (A * sin(px) * sin(px / A)) / (px * px)
    }

    private fun pack(a: Double, r: Double, g: Double, b: Double, weightSum: Double): Int {
        if (weightSum == 0.0) return 0
        // 加 0.5 四舍五入，避免浮点误差把整块颜色改掉 1 个色阶
        fun c(v: Double) = (v / weightSum + 0.5).toInt().coerceIn(0, 255)
        return (c(a) shl 24) or (c(r) shl 16) or (c(g) shl 8) or c(b)
    }

    /** 未使用，保留 cos 的引用避免误删导入（Lanczos 的另一种写法会用到）。 */
    @Suppress("unused")
    private fun cosineWindow(x: Double) = 0.5 - 0.5 * cos(x)
}
