package com.chenzixin.wrongbook.domain.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InkBoundsTest {

    @Test
    fun `框住中间的文字块`() {
        // 100x100 的画布，只有中间 20..80 有墨
        val box = InkBounds.detect(100, 100, step = 1) { x, y ->
            x in 20 until 80 && y in 30 until 70
        }
        assertNotNull(box)
        assertEquals(0.17f, box!!.left, 0.02f)
        assertEquals(0.82f, box.right, 0.02f)
        assertEquals(0.27f, box.top, 0.02f)
        assertEquals(0.72f, box.bottom, 0.02f)
    }

    @Test
    fun `全白返回 null`() {
        assertNull(InkBounds.detect(100, 100, step = 1) { _, _ -> false })
    }

    @Test
    fun `空白画布不产生错误框`() {
        assertNull(InkBounds.detect(100, 100, step = 1) { _, _ -> false })
        assertNull(InkBounds.detect(0, 0) { _, _ -> true })
    }

    @Test
    fun `边框不会超出 0 到 1`() {
        // 全部都有墨，扩张后仍要 clamp
        val box = InkBounds.detect(50, 50, step = 1) { _, _ -> true }
        assertNotNull(box)
        assertEquals(0f, box!!.left, 0.0001f)
        assertEquals(1f, box.right, 0.0001f)
        assertEquals(0f, box.top, 0.0001f)
        assertEquals(1f, box.bottom, 0.0001f)
    }

    @Test
    fun `零散墨点足够多也算有内容`() {
        val box = InkBounds.detect(100, 100, step = 2) { x, y ->
            x == 50 && y % 10 == 0
        }
        assertNotNull(box)
    }
}
