package com.chenzixin.wrongbook.domain.generate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionArchetypesTest {

    @Test
    fun `通用规范覆盖最坑的四件事`() {
        val common = QuestionArchetypes.COMMON.joinToString("\n")
        assertTrue(common.contains("如图"))
        assertTrue(common.contains("以上都不对"))
        assertTrue(common.contains("解析"))
        assertTrue(common.contains("四个选项"))
    }

    @Test
    fun `识别学科`() {
        assertEquals("物理", QuestionArchetypes.detectSubject(listOf("高中物理", "力学")))
        assertEquals("数学", QuestionArchetypes.detectSubject(listOf("数学", "函数")))
        assertNull(QuestionArchetypes.detectSubject(listOf("第三章", "复习")))
    }

    @Test
    fun `从题干猜学科`() {
        assertEquals("化学", QuestionArchetypes.detectSubjectFromText("将 0.1 mol NaOH 溶于水，求 pH"))
    }

    @Test
    fun `按学科给要求`() {
        val physics = QuestionArchetypes.forSubject("物理")
        assertTrue(physics.any { it.contains("单位") })
        assertTrue(QuestionArchetypes.forSubject("高中物理").isNotEmpty())
        assertTrue(QuestionArchetypes.forSubject(null).isEmpty())
        assertTrue(QuestionArchetypes.forSubject("音乐").isEmpty())
    }

    @Test
    fun `提示词块包含通用规范与学科要求`() {
        val block = QuestionArchetypes.promptBlock(listOf("物理"), difficulty = "中")
        assertTrue(block.contains("出题规范"))
        assertTrue(block.contains("物理"))
        assertTrue(block.contains("难度"))
    }

    @Test
    fun `没有学科时只给通用规范`() {
        val block = QuestionArchetypes.promptBlock(emptyList())
        assertTrue(block.contains("出题规范"))
        assertTrue(!block.contains("学科要求"))
    }
}
