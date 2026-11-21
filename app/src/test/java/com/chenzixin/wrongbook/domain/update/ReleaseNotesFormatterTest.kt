package com.chenzixin.wrongbook.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesFormatterTest {

    @Test
    fun `去掉 markdown 标题符号`() {
        val text = ReleaseNotesFormatter.toPlainText("# v0.4.0\n\n## 新增\n- 生成页")
        assertTrue(text.contains("v0.4.0"))
        assertTrue(text.contains("新增"))
        assertTrue(!text.contains("#"))
    }

    @Test
    fun `列表项换成圆点`() {
        val text = ReleaseNotesFormatter.toPlainText("- 第一条\n- 第二条")
        assertEquals("• 第一条\n• 第二条", text)
    }

    @Test
    fun `压缩多余空行`() {
        val text = ReleaseNotesFormatter.toPlainText("a\n\n\n\nb")
        assertEquals("a\n\nb", text)
    }

    @Test
    fun `超长内容截断并加省略号`() {
        val text = ReleaseNotesFormatter.toPlainText("x".repeat(50), maxChars = 20)
        assertEquals(21, text.length)
        assertTrue(text.endsWith("…"))
    }

    @Test
    fun `空正文返回空串`() {
        assertEquals("", ReleaseNotesFormatter.toPlainText(""))
        assertEquals("", ReleaseNotesFormatter.toPlainText("   "))
    }

    @Test
    fun `中文列表也能正常处理`() {
        val raw = "## 修复\n- 透视矫正手柄过小\n- 按钮对比度"
        val text = ReleaseNotesFormatter.toPlainText(raw)
        assertEquals("修复\n• 透视矫正手柄过小\n• 按钮对比度", text)
    }
}
