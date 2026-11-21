package com.chenzixin.wrongbook.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RefineParserTest {

    @Test
    fun `解析标准 JSON`() {
        val raw = """{"stem":"求 f(x)=x^2 的最小值","answer":"0","analysis":"配方可得","subject":"数学","knowledgePoints":["二次函数"]}"""
        val parsed = RefineParser.parse(raw)!!
        assertEquals("求 f(x)=x^2 的最小值", parsed.stem)
        assertEquals("0", parsed.answer)
        assertEquals(listOf("二次函数"), parsed.knowledgePoints)
    }

    @Test
    fun `容忍 markdown 代码块包裹`() {
        val raw = """
            ```json
            {"stem":"解方程 2x=4","answer":"x=2","analysis":"两边除以 2","subject":"数学"}
            ```
        """.trimIndent()
        assertEquals("解方程 2x=4", RefineParser.parse(raw)?.stem)
    }

    @Test
    fun `容忍前后多余解释`() {
        val raw = "好的，整理如下：\n{\"stem\":\"证明三角形内角和 180 度\",\"answer\":\"\",\"analysis\":\"过顶点作平行线\"}\n希望有帮助。"
        assertEquals("证明三角形内角和 180 度", RefineParser.parse(raw)?.stem)
    }

    @Test
    fun `缺少 stem 视为解析失败`() {
        assertNull(RefineParser.parse("""{"answer":"42"}"""))
    }

    @Test
    fun `完全不是 JSON 时返回 null`() {
        assertNull(RefineParser.parse("抱歉，我无法处理这张图片。"))
        assertNull(RefineParser.parse(""))
    }

    @Test
    fun `截断的 JSON 返回 null 而不是抛异常`() {
        assertNull(RefineParser.parse("""{"stem":"求导"""))
    }

    @Test
    fun `解析同类题数组`() {
        val raw = """
            ```json
            {"questions":[
              {"stem":"变式一","answer":"1","analysis":"a","knowledgePoints":["导数"]},
              {"stem":"变式二","answer":"2","analysis":"b"}
            ]}
            ```
        """.trimIndent()
        val variants = RefineParser.parseVariants(raw)
        assertEquals(2, variants.size)
        assertEquals("变式一", variants[0].stem)
        assertEquals("变式二", variants[1].stem)
    }

    @Test
    fun `裸数组也能解析`() {
        val raw = """[{"stem":"变式一","answer":"1"},{"stem":"变式二","answer":"2"}]"""
        assertEquals(2, RefineParser.parseVariants(raw).size)
    }

    @Test
    fun `数组里的坏元素被跳过`() {
        val raw = """[{"stem":"好的"},{"answer":"没有题干"},{"stem":""}]"""
        assertEquals(1, RefineParser.parseVariants(raw).size)
        assertTrue(RefineParser.parseVariants("[]").isEmpty())
    }
}
