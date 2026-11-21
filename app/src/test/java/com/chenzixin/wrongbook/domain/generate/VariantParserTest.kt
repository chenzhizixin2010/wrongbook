package com.chenzixin.wrongbook.domain.generate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VariantParserTest {

    @Test
    fun `标准 JSON 能解析`() {
        val raw = """{"questions":[{"stem":"求 x^2 的最小值","answer":"0","analysis":"平方非负"}]}"""
        val items = VariantParser.parse(raw)
        assertEquals(1, items.size)
        assertEquals("求 x^2 的最小值", items[0].stem)
        assertEquals("0", items[0].answer)
        assertEquals("平方非负", items[0].analysis)
    }

    @Test
    fun `中文键名也能解析`() {
        val raw = """{"题目":[{"题干":"解方程 2x=4","答案":"x=2","解析":"两边除以 2"}]}"""
        val items = VariantParser.parse(raw)
        assertEquals(1, items.size)
        assertEquals("解方程 2x=4", items[0].stem)
        assertEquals("x=2", items[0].answer)
    }

    @Test
    fun `其他数组名也能解析`() {
        val raw = """{"items":[{"question":"求 1+1","answer":"2"}]}"""
        assertEquals(1, VariantParser.parse(raw).size)
    }

    @Test
    fun `代码块包裹的 JSON 能解析`() {
        val raw = "```json\n{\"questions\":[{\"stem\":\"题目A\",\"answer\":\"A\"}]}\n```"
        val items = VariantParser.parse(raw)
        assertEquals(1, items.size)
        assertEquals("题目A", items[0].stem)
    }

    @Test
    fun `纯文本按题号切段`() {
        val raw = """
            1. 求 f(x)=x^2 的最小值
            2. 解方程 3x+1=7
            3. 计算 12×8
        """.trimIndent()
        val items = VariantParser.parse(raw)
        assertEquals(3, items.size)
        assertTrue(items[0].stem.contains("最小值"))
        assertEquals("", items[0].answer)
    }

    @Test
    fun `中文序号也能切段`() {
        val raw = "一、求面积\n二、求周长"
        val items = VariantParser.parse(raw)
        assertEquals(2, items.size)
    }

    @Test
    fun `无法解析时返回空列表`() {
        assertTrue(VariantParser.parse("抱歉，我无法完成这个请求").isEmpty())
        assertTrue(VariantParser.parse("").isEmpty())
        assertTrue(VariantParser.parse("{}").isEmpty())
    }

    @Test
    fun `题干为空的对象会被跳过`() {
        val raw = """{"questions":[{"answer":"只有答案"},{"stem":"正常题目","answer":"1"}]}"""
        val items = VariantParser.parse(raw)
        assertEquals(1, items.size)
        assertEquals("正常题目", items[0].stem)
    }
}
