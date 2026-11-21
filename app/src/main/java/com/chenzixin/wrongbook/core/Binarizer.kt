package com.chenzixin.wrongbook.core

import kotlin.math.max
import kotlin.math.sqrt

/**
 * 题目图的高对比度化：白底黑字。
 *
 * 为什么不用简单全局阈值：手机拍的试卷几乎都有明暗不均（手影、灯光），
 * 全局阈值会把背光那一半整片压黑。这里用 Sauvola 局部阈值 ——
 * 按每个像素邻域的均值/标准差算阈值，阴影下也能保住笔画。
 *
 * 纯 JVM 实现（输入是灰度数组），可以在单测里直接验证，不依赖 Bitmap。
 */
object Binarizer {

    /**
     * @param gray 灰度值 0..255，长度 = width * height
     * @return 每个像素 0（黑）或 255（白）
     */
    fun sauvola(
        gray: IntArray,
        width: Int,
        height: Int,
        radius: Int = max(8, minOf(width, height) / 24),
        k: Float = 0.22f,
    ): IntArray {
        require(gray.size >= width * height) { "灰度数组长度不足" }
        if (width <= 0 || height <= 0) return IntArray(0)

        // 积分图：sum 与 sumOfSquares，用于 O(1) 求窗口均值/方差
        val stride = width + 1
        val sum = LongArray(stride * (height + 1))
        val sumSq = LongArray(stride * (height + 1))

        for (y in 0 until height) {
            var rowSum = 0L
            var rowSumSq = 0L
            for (x in 0 until width) {
                val v = gray[y * width + x].coerceIn(0, 255).toLong()
                rowSum += v
                rowSumSq += v * v
                val idx = (y + 1) * stride + (x + 1)
                sum[idx] = sum[idx - stride] + rowSum
                sumSq[idx] = sumSq[idx - stride] + rowSumSq
            }
        }

        val out = IntArray(width * height)
        // Sauvola 的标准差项有 128 的尺度常数
        val r = 128.0
        for (y in 0 until height) {
            val y0 = max(0, y - radius)
            val y1 = minOf(height - 1, y + radius)
            for (x in 0 until width) {
                val x0 = max(0, x - radius)
                val x1 = minOf(width - 1, x + radius)
                val area = ((x1 - x0 + 1) * (y1 - y0 + 1)).toDouble()

                val s = rectSum(sum, stride, x0, y0, x1, y1).toDouble()
                val sq = rectSum(sumSq, stride, x0, y0, x1, y1).toDouble()
                val mean = s / area
                val variance = (sq / area) - mean * mean
                val std = sqrt(variance.coerceAtLeast(0.0))
                val threshold = mean * (1.0 + k * (std / r - 1.0))

                out[y * width + x] = if (gray[y * width + x] > threshold) 255 else 0
            }
        }
        return out
    }

    /**
     * 保证结果是「白底黑字」：黑像素超过一半就整体反相。
     *
     * 拍反的、或者整页偏暗的照片，Sauvola 会把背景判成黑；
     * 直接反相比重新调参更稳。
     */
    fun ensureWhiteBackground(binary: IntArray): IntArray {
        if (blackRatio(binary) <= 0.5f) return binary
        return IntArray(binary.size) { if (binary[it] == 0) 255 else 0 }
    }

    /** 黑像素占比；用来判断二值化是否失败（整片黑/整片白）。 */
    fun blackRatio(binary: IntArray): Float {
        if (binary.isEmpty()) return 0f
        val black = binary.count { it == 0 }
        return black.toFloat() / binary.size
    }

    private fun rectSum(integral: LongArray, stride: Int, x0: Int, y0: Int, x1: Int, y1: Int): Long {
        val a = integral[y0 * stride + x0]
        val b = integral[y0 * stride + (x1 + 1)]
        val c = integral[(y1 + 1) * stride + x0]
        val d = integral[(y1 + 1) * stride + (x1 + 1)]
        return d - b - c + a
    }
}
