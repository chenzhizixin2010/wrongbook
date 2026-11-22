package com.chenzixin.wrongbook.domain.generate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathHtmlTest {

    // Kotlin 字符串里 $ 是模板符号，测试数据统一用 D 表示美元符，避免转义地狱
    private val d = "$"

    @Test
    fun `行内公式被包成 math 片段`() {
        val out = MathHtml.format("求 ${d}x^2${d} 的最小值")
        assertTrue(out.contains("<span class=\"math\">"))
        assertTrue(out.contains("<sup>2</sup>"))
        assertFalse(out.contains(d))
    }

    @Test
    fun `花括号上下标`() {
        val out = MathHtml.format("${d}a_{n+1}${d} 与 ${d}x^{10}${d}")
        assertTrue(out.contains("<sub>n+1</sub>"))
        assertTrue(out.contains("<sup>10</sup>"))
    }

    @Test
    fun `常用符号被替换`() {
        val out = MathHtml.format("${d}a \\times b \\leq c${d}")
        assertTrue(out.contains("×"))
        assertTrue(out.contains("≤"))
        assertFalse(out.contains("\\times"))
    }

    @Test
    fun `公式外的 HTML 被转义`() {
        val out = MathHtml.format("若 a<b 且 c>d，求 ${d}x^2${d}")
        assertTrue(out.contains("a&lt;b"))
        assertTrue(out.contains("c&gt;d"))
    }

    @Test
    fun `换行转成 br`() {
        val out = MathHtml.format("第一行\n第二行")
        assertTrue(out.contains("<br/>"))
    }

    @Test
    fun `没有公式时原样返回（只转义）`() {
        val out = MathHtml.format("普通题干 2+2=4")
        assertEquals("普通题干 2+2=4", out)
    }
}
