package com.chenzixin.wrongbook.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrientationDetectorTest {

    /** 画一页"横排文字"：若干条水平墨带，其余空白。 */
    private fun horizontalText(w: Int, h: Int): IntArray {
        val gray = IntArray(w * h) { 255 }
        var y = 4
        while (y < h - 4) {
            for (dy in 0 until 3) {
                for (x in 4 until w - 4) {
                    val yy = y + dy
                    if (yy < h) gray[yy * w + x] = 20
                }
            }
            y += 12
        }
        return gray
    }

    /** 把横排图旋转 90°，得到竖排文字。 */
    private fun rotate90(src: IntArray, w: Int, h: Int): Pair<IntArray, Pair<Int, Int>> {
        val out = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val nx = h - 1 - y
            val ny = x
            out[ny * h + nx] = src[y * w + x]
        }
        return out to (h to w)
    }

    @Test
    fun `横排文字判定为不旋转`() {
        val w = 120
        val h = 90
        val gray = horizontalText(w, h)
        assertFalse(OrientationDetector.isVerticalText(gray, w, h))
    }

    @Test
    fun `竖排文字判定为需要旋转`() {
        val w = 120
        val h = 90
        val (rotated, size) = rotate90(horizontalText(w, h), w, h)
        assertTrue(OrientationDetector.isVerticalText(rotated, size.first, size.second))
    }

    @Test
    fun `纯白页面不会误判`() {
        assertFalse(OrientationDetector.isVerticalText(IntArray(100 * 100) { 255 }, 100, 100))
    }

    @Test
    fun `尺寸非法时返回 false`() {
        assertFalse(OrientationDetector.isVerticalText(IntArray(0), 0, 0))
    }
}
