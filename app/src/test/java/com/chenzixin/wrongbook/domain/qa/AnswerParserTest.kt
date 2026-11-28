package com.chenzixin.wrongbook.domain.qa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerParserTest {

    @Test
    fun `答案与解析分别切开`() {
        val r = AnswerParser.parse("答案：B\n解析：由动能定理可得 v=3m/s")
        assertEquals("B", r.answer)
        assertTrue(r.analysis.contains("动能定理"))
    }

    @Test
    fun `只有解析时全部当解析`() {
        val r = AnswerParser.parse("解析：先求导，再令导数为零")
        assertEquals("", r.answer)
        assertTrue(r.analysis.contains("先求导"))
    }

    @Test
    fun `没有标记时短文本当答案`() {
        val r = AnswerParser.parse("x=2")
        assertEquals("x=2", r.answer)
        assertEquals("", r.analysis)
    }

    @Test
    fun `没有标记时长文本当解析`() {
        val text = "这一题需要先分析受力情况，然后根据牛顿第二定律列方程，最后代入数值求解得到加速度。"
        val r = AnswerParser.parse(text)
        assertEquals("", r.answer)
        assertTrue(r.analysis.length > 10)
    }

    @Test
    fun `题干残留不会混进答案`() {
        val r = AnswerParser.parse("8. 求 f(x) 的最小值\n答案：0\n解析：平方非负")
        assertEquals("0", r.answer)
        assertTrue(r.analysis.contains("平方非负"))
    }

    @Test
    fun `空文本返回空结果`() {
        val r = AnswerParser.parse("   ")
        assertEquals("", r.answer)
        assertEquals("", r.analysis)
    }
}
