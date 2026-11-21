package com.chenzixin.wrongbook.domain.generate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OptionFormatterTest {

    @Test
    fun `一行四个选项被拆成四行`() {
        val input = "1. 下列哪个是偶数？ A. 3    B. 4    C. 5    D. 6"
        val out = OptionFormatter.wrap(input)
        val lines = out.lines()
        assertEquals(4, lines.size)
        assertTrue(lines[0].contains("下列哪个是偶数"))
        assertTrue(lines[1].startsWith("B."))
        assertTrue(lines[3].startsWith("D."))
    }

    @Test
    fun `中文括号选项也能拆`() {
        val input = "求值（A）-1（B）0（C）1"
        val lines = OptionFormatter.wrap(input).lines()
        assertEquals(3, lines.size)
        assertTrue(lines[1].contains("0"))
    }

    @Test
    fun `只有题目没有选项时保持原样`() {
        val input = "已知函数 f(x)=x^2，求最小值。"
        assertEquals(input, OptionFormatter.wrap(input))
    }

    @Test
    fun `单个选项不触发换行`() {
        val input = "选 A. 因为它是偶数"
        assertEquals(input, OptionFormatter.wrap(input))
    }

    @Test
    fun `多行文本逐行处理`() {
        val input = "第一问 求导\n第二问 A. 1 B. 2"
        val lines = OptionFormatter.wrap(input).lines()
        assertEquals(3, lines.size)
        assertEquals("第一问 求导", lines[0])
    }

    @Test
    fun `不会误伤普通英文单词`() {
        val input = "The A. test B. case"
        // 这里确实是选项格式，应当换行；但 "A股" 不该被拆
        assertFalseSplit("A股上涨 3 个点")
    }

    private fun assertFalseSplit(text: String) {
        assertEquals(text, OptionFormatter.wrap(text))
    }
}
