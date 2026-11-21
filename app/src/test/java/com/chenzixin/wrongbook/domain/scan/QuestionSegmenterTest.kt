package com.chenzixin.wrongbook.domain.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionSegmenterTest {

    private fun line(text: String, top: Float, height: Float = 20f, left: Float = 20f, right: Float = 380f) =
        OcrLine(text = text, left = left, top = top, right = right, bottom = top + height)

    private fun ocr(vararg lines: OcrLine) = OcrResult(
        imageWidth = 400,
        imageHeight = 1000,
        lines = lines.toList(),
    )

    // ---------- 题号锚点 ----------

    @Test
    fun `识别数字题号`() {
        val anchor = AnchorParser.parse("12. 求函数最小值")
        assertTrue(anchor is AnchorParser.Numbered)
        assertEquals(12, (anchor as AnchorParser.Numbered).index)
    }

    @Test
    fun `识别括号题号`() {
        val anchor = AnchorParser.parse("（3）解方程")
        assertEquals(3, (anchor as AnchorParser.Numbered).index)
    }

    @Test
    fun `识别中文大题号`() {
        val anchor = AnchorParser.parse("三、解答题（共 3 小题）")
        assertTrue(anchor is AnchorParser.Section)
    }

    @Test
    fun `小数不会被当成题号`() {
        assertNull(AnchorParser.parse("3.14 是圆周率"))
        assertNull(AnchorParser.parse("成绩 9.5 分"))
    }

    @Test
    fun `普通文本没有锚点`() {
        assertNull(AnchorParser.parse("如图所示，求阴影面积"))
    }

    // ---------- 按题号切分 ----------

    @Test
    fun `三题照片切出三个区域且坐标归一化`() {
        val result = QuestionSegmenter.segment(
            ocr(
                line("1. 求极限 lim x→0", 100f),
                line("解：原式 = 1", 130f),
                line("2. 已知函数 f(x)", 200f),
                line("求 f'(x)", 230f),
                line("3. 证明三角形相似", 300f),
            ),
        )

        assertEquals(3, result.size)
        assertEquals(listOf(1, 2, 3), result.map { it.index })
        result.forEach {
            assertTrue(it.rect.left in 0f..1f)
            assertTrue(it.rect.top in 0f..1f)
            assertTrue(it.rect.right in 0f..1f)
            assertTrue(it.rect.bottom in 0f..1f)
            assertTrue(it.rect.bottom > it.rect.top)
        }
        assertEquals(100f / 1000f, result.first().rect.top, 0.001f)
    }

    @Test
    fun `区域文本包含该题的所有行`() {
        val result = QuestionSegmenter.segment(
            ocr(
                line("1. 第一题", 100f),
                line("补充条件", 130f),
                line("2. 第二题", 200f),
            ),
        )

        assertTrue(result[0].text.contains("第一题"))
        assertTrue(result[0].text.contains("补充条件"))
        assertTrue(!result[0].text.contains("第二题"))
    }

    @Test
    fun `孤立的大题标题不产生区域`() {
        val result = QuestionSegmenter.segment(
            ocr(
                line("三、解答题", 100f),
                line("1. 第一小题", 140f),
            ),
        )

        assertEquals(1, result.size)
        assertEquals(1, result.single().index)
    }

    // ---------- 无题号兜底 ----------

    @Test
    fun `没有题号时按行距切段`() {
        val result = QuestionSegmenter.segment(
            ocr(
                line("第一段第一行", 100f),
                line("第一段第二行", 125f),
                line("第二段第一行", 400f),
                line("第二段第二行", 425f),
            ),
        )

        assertEquals(2, result.size)
        assertEquals(-1, result[0].index)
    }

    @Test
    fun `空结果不崩溃`() {
        assertTrue(QuestionSegmenter.segment(ocr()).isEmpty())
        assertTrue(
            QuestionSegmenter.segment(OcrResult(0, 0, listOf(line("1. x", 10f)))).isEmpty(),
        )
    }
}
