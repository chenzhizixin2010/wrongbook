package com.chenzixin.wrongbook.domain.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionRegionParserTest {

    @Test
    fun `解析归一化坐标`() {
        val raw = """{"questions":[
            {"index":1,"label":"1.","bbox":[0.07,0.055,0.39,0.13],"text":"1. Solve 2x+3=11"},
            {"index":2,"label":"2.","bbox":[0.07,0.295,0.69,0.39],"text":"2. Find f'(x)"}
        ]}"""
        val regions = VisionRegionParser.parse(raw, 900, 1200)
        assertEquals(2, regions?.size)
        assertEquals(1, regions!![0].index)
        assertEquals(0.07f, regions[0].rect.left, 0.001f)
        assertEquals("2. Find f'(x)", regions[1].text)
    }

    @Test
    fun `像素坐标自动归一化`() {
        val raw = """{"questions":[{"index":3,"label":"3.","bbox":[90,70,350,160],"text":"third"}]}"""
        val regions = VisionRegionParser.parse(raw, 900, 1000)
        assertEquals(1, regions?.size)
        assertEquals(0.1f, regions!![0].rect.left, 0.001f)
        assertEquals(0.07f, regions[0].rect.top, 0.001f)
        assertEquals(0.16f, regions[0].rect.bottom, 0.001f)
    }

    @Test
    fun `容忍代码块包裹`() {
        val raw = "```json\n{\"questions\":[{\"index\":1,\"bbox\":[0.1,0.1,0.9,0.2],\"text\":\"a\"}]}\n```"
        assertEquals(1, VisionRegionParser.parse(raw, 100, 100)?.size)
    }

    @Test
    fun `裸数组也接受`() {
        val raw = """[{"index":1,"bbox":[0.1,0.1,0.9,0.2],"text":"a"}]"""
        assertEquals(1, VisionRegionParser.parse(raw, 100, 100)?.size)
    }

    @Test
    fun `非法或空结果返回 null 以便降级`() {
        assertNull(VisionRegionParser.parse("抱歉，我无法识别这张图片", 100, 100))
        assertNull(VisionRegionParser.parse("""{"questions":[]}""", 100, 100))
        assertNull(VisionRegionParser.parse("""{"questions":[{"index":1,"bbox":[0.5,0.5,0.5,0.6]}]}""", 100, 100))
        assertNull(VisionRegionParser.parse("""{"questions":[]}""", 0, 0))
    }

    @Test
    fun `缺 text 保留区域但文字为空`() {
        val regions = VisionRegionParser.parse(
            """{"questions":[{"index":5,"bbox":[0.1,0.1,0.5,0.3]}]}""",
            100,
            100,
        )
        assertEquals(1, regions?.size)
        assertTrue(regions!![0].text.isEmpty())
        assertEquals(5, regions[0].index)
    }

    @Test
    fun `越界坐标被收敛到 0 到 1`() {
        val regions = VisionRegionParser.parse(
            """{"questions":[{"index":1,"bbox":[-0.2,0.1,1.4,1.2],"text":"x"}]}""",
            100,
            100,
        )
        val rect = regions!!.single().rect
        assertEquals(0f, rect.left, 0.0001f)
        assertEquals(1f, rect.right, 0.0001f)
        assertEquals(1f, rect.bottom, 0.0001f)
    }

    @Test
    fun `bbox 写成字符串也能解析`() {
        val raw = """{"questions":[{"index":1,"label":"1.","bbox":"0.1,0.2,0.5,0.4","text":"string box"}]}"""
        val regions = VisionRegionParser.parse(raw, 1000, 1000)
        assertEquals(1, regions?.size)
        assertEquals(0.1f, regions!![0].rect.left, 0.001f)
        assertEquals(0.4f, regions[0].rect.bottom, 0.001f)
    }

    @Test
    fun `bbox 写成对象也能解析`() {
        val raw = """{"questions":[{"index":2,"label":"2.","bbox":{"left":0.2,"top":0.3,"right":0.6,"bottom":0.5},"text":"object box"}]}"""
        val regions = VisionRegionParser.parse(raw, 1000, 1000)
        assertEquals(1, regions?.size)
        assertEquals(0.2f, regions!![0].rect.left, 0.001f)
        assertEquals(0.6f, regions[0].rect.right, 0.001f)
    }

    @Test
    fun `bbox 用宽高也能换算`() {
        val raw = """{"questions":[{"index":3,"bbox":{"x":0.1,"y":0.2,"width":0.3,"height":0.2},"text":"wh"}]}"""
        val regions = VisionRegionParser.parse(raw, 1000, 1000)
        assertEquals(1, regions?.size)
        assertEquals(0.4f, regions!![0].rect.right, 0.001f)
    }

    @Test
    fun `题号带点也能取到数字`() {
        val raw = """{"questions":[{"index":"12.","bbox":[0.1,0.1,0.5,0.3],"text":"twelve"}]}"""
        val regions = VisionRegionParser.parse(raw, 1000, 1000)
        assertEquals(12, regions!![0].index)
    }

    @Test
    fun `代码块包裹的 JSON 能解析`() {
        val raw = "```json\n{\"questions\":[{\"index\":1,\"bbox\":[0.1,0.1,0.5,0.3],\"text\":\"fenced\"}]}\n```"
        val regions = VisionRegionParser.parse(raw, 1000, 1000)
        assertEquals(1, regions?.size)
        assertEquals("fenced", regions!![0].text)
    }
}
