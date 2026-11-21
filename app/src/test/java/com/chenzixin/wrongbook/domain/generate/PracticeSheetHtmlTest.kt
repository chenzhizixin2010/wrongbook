package com.chenzixin.wrongbook.domain.generate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSheetHtmlTest {

    private fun sheet(withAnswers: Boolean = true) = PracticeSheet(
        title = "数学错题练习",
        subtitle = "标签：函数 ｜ 出题方式：整理错题",
        items = listOf(
            PracticeItem(1, "求 f(x)=x^2 的最小值", "0", "配方"),
            PracticeItem(2, "解 2x+1=5", "x=2", ""),
        ),
        withAnswers = withAnswers,
    )

    @Test
    fun `包含题目与序号`() {
        val html = PracticeSheetHtml.build(sheet())
        assertTrue(html.contains("数学错题练习"))
        assertTrue(html.contains("求 f(x)=x^2 的最小值"))
        assertTrue(html.contains("<span class=\"no\">1.</span>"))
        assertTrue(html.contains("<span class=\"no\">2.</span>"))
    }

    @Test
    fun `附答案时另起一页`() {
        val html = PracticeSheetHtml.build(sheet(withAnswers = true))
        assertTrue(html.contains("参考答案"))
        assertTrue(html.contains("page-break-before: always"))
        assertTrue(html.contains("解析：配方"))
    }

    @Test
    fun `不带答案时不出现答案区`() {
        val html = PracticeSheetHtml.build(sheet(withAnswers = false))
        assertFalse(html.contains("参考答案"))
        assertFalse(html.contains("page-break-before"))
    }

    @Test
    fun `题干里的 HTML 被转义`() {
        val html = PracticeSheetHtml.build(
            PracticeSheet(
                title = "测试",
                subtitle = "",
                items = listOf(PracticeItem(1, "若 a<b 且 c>d，求 & 的值", "", "")),
                withAnswers = false,
            ),
        )
        assertTrue(html.contains("a&lt;b"))
        assertTrue(html.contains("c&gt;d"))
        assertTrue(html.contains("&amp;"))
        assertFalse(html.contains("a<b 且"))
    }

    @Test
    fun `空答案有占位文案`() {
        val html = PracticeSheetHtml.build(
            PracticeSheet("t", "", listOf(PracticeItem(1, "题目", "", "")), withAnswers = true),
        )
        assertTrue(html.contains("（未提供答案）"))
    }
}
