package com.chenzixin.wrongbook.domain.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatQuestionExtractorTest {

    @Test
    fun `按答案与解析切段`() {
        val raw = """
            1. 求 f(x)=x^2 的最小值。
            答案：0
            解析：平方非负，x=0 时取到。
        """.trimIndent()
        val q = ChatQuestionExtractor.extract(raw)
        assertTrue(q.stem.contains("最小值"))
        assertEquals("0", q.answer)
        assertTrue(q.analysis.contains("平方非负"))
    }

    @Test
    fun `只有答案没有解析`() {
        val q = ChatQuestionExtractor.extract("计算 2+2。\n答案：4")
        assertEquals("4", q.answer)
        assertEquals("", q.analysis)
        assertTrue(q.stem.contains("2+2"))
    }

    @Test
    fun `没有标记时整段当题干`() {
        val q = ChatQuestionExtractor.extract("解方程 3x+1=7，x 等于多少？")
        assertEquals("解方程 3x+1=7，x 等于多少？", q.stem)
        assertEquals("", q.answer)
    }

    @Test
    fun `去掉 markdown 装饰`() {
        val q = ChatQuestionExtractor.extract("## 题目\n**答案**：B")
        assertFalse(q.stem.contains("#"))
        assertEquals("B", q.answer)
    }

    @Test
    fun `空内容标记为空`() {
        assertTrue(ChatQuestionExtractor.extract("").isEmpty)
        assertTrue(ChatQuestionExtractor.extract("   ").isEmpty)
    }

    @Test
    fun `解析出现在答案之前也能切`() {
        val raw = "题目：求 1+1\n解析：基础运算\n答案：2"
        val q = ChatQuestionExtractor.extract(raw)
        assertEquals("2", q.answer)
        assertTrue(q.analysis.contains("基础运算"))
    }

    @Test
    fun `列表符号被清理`() {
        val q = ChatQuestionExtractor.extract("- 第一步\n- 第二步\n答案：完成")
        assertFalse(q.stem.contains("- "))
        assertEquals("完成", q.answer)
    }
}
