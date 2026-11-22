package com.chenzixin.wrongbook.core

/**
 * 文字行方向判断：判断一页纸上的文字是**横排**还是**竖排**（即图片要不要转 90°）。
 *
 * 为什么需要：OCR 打分在低分辨率/低对比度的老图上经常全是 0，分不出方向；
 * 这里用投影轮廓补一刀 —— 横排文字的行方向（y）会呈现强烈的「有字/空白」交替，
 * 其方差显著高于列方向；竖排反之。
 *
 * 纯 JVM 实现，可直接在单测里验证。
 */
object OrientationDetector {

    /** 文字是否竖排（需要顺时针转 90° 才是正的）。 */
    fun isVerticalText(gray: IntArray, width: Int, height: Int): Boolean {
        if (width <= 1 || height <= 1 || gray.size < width * height) return false

        val rowSums = IntArray(height)
        val colSums = IntArray(width)

        for (y in 0 until height) {
            var rowTotal = 0
            val rowOffset = y * width
            for (x in 0 until width) {
                val ink = 255 - gray[rowOffset + x].coerceIn(0, 255)
                rowTotal += ink
                colSums[x] += ink
            }
            rowSums[y] = rowTotal
        }

        val rowVar = variance(rowSums)
        val colVar = variance(colSums)
        if (rowVar <= 0.0 && colVar <= 0.0) return false

        // 行方向的起伏明显更大 → 横排
        return colVar > rowVar * 1.6
    }

    private fun variance(values: IntArray): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.sum().toDouble() / values.size
        var sum = 0.0
        values.forEach { v ->
            val d = v - mean
            sum += d * d
        }
        return sum / values.size
    }
}
