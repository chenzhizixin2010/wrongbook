package com.chenzixin.wrongbook.domain.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class HomographyTest {

    private val eps = 1e-6

    @Test
    fun `单位正方形到自身是恒等变换`() {
        val h = Homography.fromUnitSquareTo(
            listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0),
        )!!
        val (x, y) = h.map(0.5, 0.5)!!
        assertEquals(0.5, x, eps)
        assertEquals(0.5, y, eps)
    }

    @Test
    fun `四个角点精确映射`() {
        val quad = listOf(10.0 to 20.0, 110.0 to 20.0, 110.0 to 220.0, 10.0 to 220.0)
        val h = Homography.fromUnitSquareTo(quad)!!
        assertEquals(10.0, h.map(0.0, 0.0)!!.first, eps)
        assertEquals(20.0, h.map(0.0, 0.0)!!.second, eps)
        assertEquals(110.0, h.map(1.0, 0.0)!!.first, eps)
        assertEquals(220.0, h.map(1.0, 1.0)!!.second, eps)
    }

    @Test
    fun `梯形（倾斜拍摄）边中点仍然落在边中点`() {
        // 上边短、下边长的梯形
        val quad = listOf(30.0 to 0.0, 70.0 to 0.0, 100.0 to 100.0, 0.0 to 100.0)
        val h = Homography.fromUnitSquareTo(quad)!!

        // 上边中点：(0.5, 0) → (50, 0)
        assertEquals(50.0, h.map(0.5, 0.0)!!.first, 1e-3)
        assertEquals(0.0, h.map(0.5, 0.0)!!.second, 1e-3)

        // 中心点映射后应仍落在四边形内部（透视投影不保「几何中心」）
        val (cx, cy) = h.map(0.5, 0.5)!!
        assertTrue("cx=$cx", cx in 0.0..100.0)
        assertTrue("cy=$cy", cy in 0.0..100.0)
    }

    @Test
    fun `退化四边形返回 null`() {
        assertNull(Homography.fromUnitSquareTo(listOf(0.0 to 0.0, 0.0 to 0.0, 0.0 to 0.0, 0.0 to 0.0)))
        assertNull(Homography.fromUnitSquareTo(listOf(0.0 to 0.0, 1.0 to 1.0, 2.0 to 2.0, 3.0 to 3.0)))
        assertNull(Homography.fromUnitSquareTo(listOf(0.0 to 0.0, 1.0 to 0.0)))
    }

    @Test
    fun `非退化任意四边形都能求解`() {
        val quad = listOf(12.0 to 8.0, 300.0 to 40.0, 280.0 to 220.0, 20.0 to 260.0)
        assertNotNull(Homography.fromUnitSquareTo(quad))
    }
}
