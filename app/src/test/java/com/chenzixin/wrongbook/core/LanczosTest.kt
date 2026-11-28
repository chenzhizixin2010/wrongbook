package com.chenzixin.wrongbook.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanczosTest {

    @Test
    fun `核函数在零点为一、在支撑外为零`() {
        assertEquals(1.0, Lanczos.kernel(0.0), 1e-6)
        assertEquals(0.0, Lanczos.kernel(3.0), 1e-9)
        assertEquals(0.0, Lanczos.kernel(-4.2), 1e-9)
        assertTrue(Lanczos.kernel(0.5) > 0.0)
    }

    @Test
    fun `放大后尺寸正确`() {
        val src = IntArray(4 * 4) { 0xFF336699.toInt() }
        val out = Lanczos.upscale(src, 4, 4, 2.0)
        assertEquals(8 * 8, out.size)
    }

    @Test
    fun `纯色图放大后仍是同一个颜色`() {
        val color = 0xFF336699.toInt()
        val src = IntArray(6 * 6) { color }
        val out = Lanczos.upscale(src, 6, 6, 2.0)
        assertTrue(out.all { it == color })
    }

    @Test
    fun `不缩放时原样返回`() {
        val src = IntArray(9) { it }
        assertEquals(src, Lanczos.upscale(src, 3, 3, 1.0))
    }

    @Test
    fun `尺寸非法时原样返回`() {
        val src = IntArray(4)
        assertEquals(src, Lanczos.upscale(src, 0, 0, 2.0))
    }
}
