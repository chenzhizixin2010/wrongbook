package com.chenzixin.wrongbook.domain.generate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueprintParserTest {

    @Test
    fun `解析标准蓝图`() {
        val raw = """{"subject":"物理","topic":"机械能守恒","difficulty":"中","types":["选择题","计算题"],"traps":["漏算摩擦做功"]}"""
        val blueprint = BlueprintParser.parse(raw)!!
        assertEquals("物理", blueprint.subject)
        assertEquals("机械能守恒", blueprint.topic)
        assertEquals("中", blueprint.difficulty)
        assertEquals(listOf("选择题", "计算题"), blueprint.types)
        assertEquals(listOf("漏算摩擦做功"), blueprint.traps)
        assertTrue(blueprint.promptLine().contains("机械能守恒"))
    }

    @Test
    fun `容忍 Markdown 代码块和前后废话`() {
        val raw = "好的，分析如下：\n```json\n{\"topic\":\"牛顿第二定律\"}\n```\n希望有帮助"
        val blueprint = BlueprintParser.parse(raw)
        assertNotNull(blueprint)
        assertEquals("牛顿第二定律", blueprint!!.topic)
    }

    @Test
    fun `数组写成顿号串也能拆`() {
        val blueprint = BlueprintParser.parse("""{"types":"选择题、填空题"}""")
        assertEquals(listOf("选择题", "填空题"), blueprint!!.types)
    }

    @Test
    fun `中文键名也认`() {
        val blueprint = BlueprintParser.parse("""{"学科":"数学","考点":"导数","难度":"难"}""")
        assertEquals("数学", blueprint!!.subject)
        assertEquals("导数", blueprint.topic)
    }

    @Test
    fun `空内容返回 null`() {
        assertNull(BlueprintParser.parse(""))
        assertNull(BlueprintParser.parse("模型今天不想干活"))
        assertNull(BlueprintParser.parse("{}"))
    }
}
