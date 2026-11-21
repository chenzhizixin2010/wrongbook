package com.chenzixin.wrongbook.domain.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrLineCleanerTest {

    private fun line(text: String, top: Float = 0f) = OcrLine(
        text = text,
        left = 0f,
        top = top,
        right = 100f,
        bottom = top + 20f,
        confidence = 0.8f,
    )

    @Test
    fun `丢掉只有一个字符的碎片`() {
        val cleaned = OcrLineCleaner.clean(listOf(line("A"), line("。"), line("数学题目")))
        assertEquals(1, cleaned.size)
        assertEquals("数学题目", cleaned[0].text)
    }

    @Test
    fun `丢掉纯符号行`() {
        val cleaned = OcrLineCleaner.clean(
            listOf(line("|"), line("——"), line("（ ）"), line("求 f(x)=x^2 的最小值")),
        )
        assertEquals(1, cleaned.size)
    }

    @Test
    fun `保留含数字和字母的行`() {
        assertTrue(OcrLineCleaner.isMeaningful("12."))
        assertTrue(OcrLineCleaner.isMeaningful("x = 3"))
        assertTrue(OcrLineCleaner.isMeaningful("H2O"))
    }

    @Test
    fun `空字符串和空白不算有效`() {
        assertFalse(OcrLineCleaner.isMeaningful(""))
        assertFalse(OcrLineCleaner.isMeaningful("   "))
        assertFalse(OcrLineCleaner.isMeaningful(" 。"))
    }

    @Test
    fun `保持原有顺序`() {
        val cleaned = OcrLineCleaner.clean(
            listOf(line("第一行", top = 0f), line("×", top = 30f), line("第二行", top = 60f)),
        )
        assertEquals(listOf("第一行", "第二行"), cleaned.map { it.text })
    }
}
