package com.chenzixin.wrongbook.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 公式分隔符归一。
 *
 * 这一组测试锁的是一个真实事故（0.13.0）：
 * Markwon 的 latex 插件正则只匹配 `$$...$$`，单 `$...$` 会被当普通字符，
 * 于是「AI 讲解里的公式显示不出来」；而为了修行内公式随手打开 `inlinesEnabled`
 * 又因为缺 inline-parser 插件导致「点开题目就崩」。
 *
 * 注意 Kotlin 字符串里 `$` 要写成 `\$`，否则会被当成模板。
 */
class LatexSanitizerTest {

    private val dd = "\$\$"

    @Test
    fun `单美元行内公式升级成双美元`() {
        val out = LatexSanitizer.sanitize("已知函数 \$f(x)=x^2+1\$，求 \$f(2)\$ 的值。")
        assertEquals("已知函数 ${dd}f(x)=x^2+1$dd，求 ${dd}f(2)$dd 的值。", out)
    }

    @Test
    fun `已经是双美元的不再动`() {
        val src = "行前\n\n$dd\n\\int_0^1 x dx\n$dd\n\n行后"
        assertEquals(src, LatexSanitizer.sanitize(src))
    }

    @Test
    fun `同一行既有块级又有行内`() {
        val src = "看这个 ${dd}a+b$dd 和这个 \$c^2\$ 都对"
        assertEquals("看这个 ${dd}a+b$dd 和这个 ${dd}c^2$dd 都对", LatexSanitizer.sanitize(src))
    }

    @Test
    fun `货币写法不动`() {
        assertEquals("价格 \$5 和 \$10 元", LatexSanitizer.sanitize("价格 \$5 和 \$10 元"))
    }

    @Test
    fun `单字符公式也能升级`() {
        assertEquals("求 ${dd}x$dd 的值", LatexSanitizer.sanitize("求 \$x\$ 的值"))
    }

    @Test
    fun `反斜杠括号归一`() {
        assertEquals("${dd}x^2$dd", LatexSanitizer.sanitize("\\(x^2\\)"))
    }

    @Test
    fun `方括号归一成块级`() {
        assertEquals("${dd}\\frac{1}{2}$dd", LatexSanitizer.sanitize("\\[\\frac{1}{2}\\]"))
    }

    @Test
    fun `双重转义折叠`() {
        assertEquals("${dd}-110.5\\mathrm{kJ/mol}$dd", LatexSanitizer.sanitize("${dd}-110.5\\\\mathrm{kJ/mol}$dd"))
    }

    @Test
    fun `反引号包住的公式解包`() {
        assertEquals("面积是 ${dd}S=ab$dd", LatexSanitizer.sanitize("面积是 `\$S=ab\$`"))
    }

    @Test
    fun `没有美元符号时原样返回`() {
        assertEquals("普通文本，没有公式", LatexSanitizer.sanitize("普通文本，没有公式"))
        assertEquals("", LatexSanitizer.sanitize(""))
    }

    @Test
    fun `重复清洗结果稳定`() {
        val once = LatexSanitizer.sanitize("求 \$x^2\$ 的值")
        assertEquals(once, LatexSanitizer.sanitize(once))
    }

    @Test
    fun `跨行的单个美元不会被硬拼起来`() {
        val src = "第一行 \$a\n第二行 b\$"
        val out = LatexSanitizer.sanitize(src)
        assertTrue(out.contains("\n"))
        assertEquals(src, out)
    }
}
