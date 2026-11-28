package com.chenzixin.wrongbook.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IlluminationFlattenerTest {

    private fun argb(r: Int, g: Int, b: Int) =
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun red(c: Int) = (c shr 16) and 0xFF
    private fun green(c: Int) = (c shr 8) and 0xFF
    private fun blue(c: Int) = c and 0xFF

    @Test
    fun `偏暗的纸被提亮到接近纯白`() {
        val w = 64
        val h = 64
        // 纸面 190（偏灰），中间一块深灰字
        val pixels = IntArray(w * h) { argb(190, 190, 190) }
        for (y in 30 until 34) for (x in 30 until 34) pixels[y * w + x] = argb(40, 40, 40)

        val out = IlluminationFlattener.flatten(pixels, w, h)
        assertTrue("纸面应该被提亮", red(out[5 * w + 5]) >= 235)
        assertTrue("字仍然比纸暗", red(out[32 * w + 32]) < 120)
    }

    @Test
    fun `明暗不均的画面也能拉平`() {
        val w = 96
        val h = 48
        val pixels = IntArray(w * h) { i ->
            val x = i % w
            val base = 150 + x * 80 / w   // 左边暗、右边亮
            argb(base, base, base)
        }
        val out = IlluminationFlattener.flatten(pixels, w, h)
        val left = red(out[20 * w + 5])
        val right = red(out[20 * w + 90])
        assertTrue("左右纸面都应接近白", left >= 230 && right >= 230)
        assertTrue("两侧差异被拉平", kotlin.math.abs(left - right) <= 22)
    }

    @Test
    fun `彩色墨迹保持色相`() {
        val w = 64
        val h = 64
        val pixels = IntArray(w * h) { argb(200, 200, 195) }
        // 一块红笔
        for (y in 10 until 20) for (x in 10 until 20) pixels[y * w + x] = argb(180, 60, 60)

        val out = IlluminationFlattener.flatten(pixels, w, h)
        val c = out[15 * w + 15]
        assertTrue("红色通道仍应最大", red(c) > green(c) && red(c) > blue(c))
    }

    @Test
    fun `强提亮比弱提亮更白`() {
        val w = 32
        val h = 32
        val pixels = IntArray(w * h) { argb(170, 170, 170) }

        val soft = IlluminationFlattener.flatten(pixels, w, h, strength = 0.4f)
        val hard = IlluminationFlattener.flatten(pixels, w, h, strength = 1f)
        assertTrue(red(hard[10 * w + 10]) >= red(soft[10 * w + 10]))
    }

    @Test
    fun `纯白画面不会被压暗`() {
        val w = 32
        val h = 32
        val pixels = IntArray(w * h) { argb(255, 255, 255) }
        val out = IlluminationFlattener.flatten(pixels, w, h)
        assertTrue(red(out[0]) >= 250)
    }

    @Test
    fun `尺寸不合法时原样返回`() {
        val pixels = IntArray(4) { argb(10, 10, 10) }
        assertEquals(pixels, IlluminationFlattener.flatten(pixels, 0, 0))
    }
}
