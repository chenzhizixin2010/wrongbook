package com.chenzixin.wrongbook.domain.sr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TilingPlannerTest {

    @Test
    fun `小图只用一块`() {
        val plan = TilingPlanner.plan(100, 60)!!
        assertEquals(1, plan.count)
        assertEquals(400, plan.outWidth)
        assertEquals(240, plan.outHeight)
        val tile = plan.tiles.first()
        assertEquals(0, tile.readLeft)
        assertEquals(0, tile.insetX)
        assertEquals(100, tile.cleanWidth)
        assertEquals(60, tile.cleanHeight)
    }

    @Test
    fun `刚好一块大小也不浪费`() {
        val plan = TilingPlanner.plan(112, 112)!!
        assertEquals(1, plan.count)
        assertEquals(112, plan.tiles.first().cleanWidth)
    }

    @Test
    fun `每块的干净区域都在读取区域内`() {
        for (w in listOf(50, 112, 113, 200, 224, 300, 336)) {
            for (h in listOf(50, 112, 233, 336)) {
                val plan = TilingPlanner.plan(w, h) ?: continue
                plan.tiles.forEach { tile ->
                    assertTrue("insetX=${tile.insetX}", tile.insetX >= 0)
                    assertTrue("insetY=${tile.insetY}", tile.insetY >= 0)
                    assertTrue(
                        "越界 ${w}x${h}: ${tile.insetX}+${tile.cleanWidth}",
                        tile.insetX + tile.cleanWidth <= plan.tile,
                    )
                    assertTrue(
                        "越界 ${w}x${h}: ${tile.insetY}+${tile.cleanHeight}",
                        tile.insetY + tile.cleanHeight <= plan.tile,
                    )
                    // 干净区域必须完整落在源图内
                    assertTrue(tile.readLeft + tile.insetX + tile.cleanWidth <= w)
                    assertTrue(tile.readTop + tile.insetY + tile.cleanHeight <= h)
                    // 输出位置必须在输出图内
                    assertTrue(tile.outLeft + tile.cleanWidth * plan.scale <= plan.outWidth)
                    assertTrue(tile.outTop + tile.cleanHeight * plan.scale <= plan.outHeight)
                }
            }
        }
    }

    @Test
    fun `所有干净区域拼起来正好铺满原图`() {
        val w = 300
        val h = 224
        val plan = TilingPlanner.plan(w, h)!!
        val covered = Array(h) { BooleanArray(w) }
        plan.tiles.forEach { tile ->
            for (y in 0 until tile.cleanHeight) {
                for (x in 0 until tile.cleanWidth) {
                    covered[tile.readTop + tile.insetY + y][tile.readLeft + tile.insetX + x] = true
                }
            }
        }
        val missing = covered.sumOf { row -> row.count { !it } }
        assertEquals(0, missing)
    }

    @Test
    fun `块数超预算就返回 null`() {
        // 512×512 需要 5×5=25 块，远超默认预算
        assertNull(TilingPlanner.plan(512, 512))
        assertNull(TilingPlanner.plan(512, 512, maxTiles = 24))
        assertNotNull(TilingPlanner.plan(310, 310))
    }

    @Test
    fun `非法尺寸不崩`() {
        assertNull(TilingPlanner.plan(0, 100))
        assertNull(TilingPlanner.plan(100, -1))
        assertNull(TilingPlanner.plan(100, 100, pad = 64))
        assertNull(TilingPlanner.plan(100, 100, tile = 0))
        assertNull(TilingPlanner.plan(100, 100, scale = 0))
    }

    @Test
    fun `长条图也只算少量块`() {
        // 一条 40×900 的窄图：1 列 9 行
        val plan = TilingPlanner.plan(40, 900)
        assertNotNull(plan)
        assertEquals(1, plan!!.cols)
        assertEquals(9, plan.rows)
        assertEquals(9, plan.count)
    }

    @Test
    fun `输出尺寸是输入的 4 倍`() {
        val plan = TilingPlanner.plan(224, 112)!!
        assertEquals(224 * 4, plan.outWidth)
        assertEquals(112 * 4, plan.outHeight)
        assertEquals(4, plan.scale)
    }
}
