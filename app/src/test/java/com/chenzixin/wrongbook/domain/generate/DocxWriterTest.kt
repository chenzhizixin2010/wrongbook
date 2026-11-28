package com.chenzixin.wrongbook.domain.generate

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocxWriterTest {

    private fun sheet(withAnswers: Boolean = true) = PracticeSheet(
        title = "错题巩固练习",
        subtitle = "标签：函数 ｜ 方式：整理错题",
        items = listOf(
            PracticeItem(1, "求 f(x)=x^2 的最小值\nA. -1\nB. 0", "0", "平方非负"),
            PracticeItem(2, "解 2x+1=5", "x=2", ""),
        ),
        withAnswers = withAnswers,
    )

    private fun entries(bytes: ByteArray): Map<String, String> {
        val map = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                map[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        return map
    }

    @Test
    fun `docx 包含必要的三个部件`() {
        val files = entries(DocxWriter.build(sheet()))
        assertTrue(files.containsKey("[Content_Types].xml"))
        assertTrue(files.containsKey("_rels/.rels"))
        assertTrue(files.containsKey("word/document.xml"))
    }

    @Test
    fun `正文包含题干与选项`() {
        val doc = entries(DocxWriter.build(sheet()))["word/document.xml"].orEmpty()
        assertTrue(doc.contains("错题巩固练习"))
        assertTrue(doc.contains("求 f(x)=x^2 的最小值"))
        assertTrue(doc.contains("B. 0"))
    }

    @Test
    fun `换行会转成 w_br`() {
        val doc = entries(DocxWriter.build(sheet()))["word/document.xml"].orEmpty()
        assertTrue(doc.contains("<w:br/>"))
    }

    @Test
    fun `带答案时包含参考答案并分页`() {
        val doc = entries(DocxWriter.build(sheet(withAnswers = true)))["word/document.xml"].orEmpty()
        assertTrue(doc.contains("参考答案"))
        assertTrue(doc.contains("pageBreakBefore"))
        assertTrue(doc.contains("平方非负"))
    }

    @Test
    fun `不带答案时不出现参考答案`() {
        val doc = entries(DocxWriter.build(sheet(withAnswers = false)))["word/document.xml"].orEmpty()
        assertTrue(!doc.contains("参考答案"))
    }

    @Test
    fun `特殊字符被转义`() {
        val s = PracticeSheet(
            title = "测试",
            subtitle = "",
            items = listOf(PracticeItem(1, "若 a<b 且 c>d", "", "")),
            withAnswers = false,
        )
        val doc = entries(DocxWriter.build(s))["word/document.xml"].orEmpty()
        assertTrue(doc.contains("a&lt;b"))
        assertTrue(doc.contains("c&gt;d"))
    }

    @Test
    fun `卷子能序列化再反序列化`() {
        val original = sheet()
        val json = kotlinx.serialization.json.Json.encodeToString(
            PracticeSheet.serializer(), original,
        )
        val parsed = kotlinx.serialization.json.Json.decodeFromString(
            PracticeSheet.serializer(), json,
        )
        assertEquals(original.title, parsed.title)
        assertEquals(original.items.size, parsed.items.size)
        assertEquals(original.items[1].answer, parsed.items[1].answer)
    }

    @Test
    fun `插图会写成 media 部件并引用`() {
        val sheet = PracticeSheet(
            title = "t",
            subtitle = "",
            items = listOf(
                PracticeItem(1, "看下图", "", "", figures = listOf("figures/a.webp")),
            ),
            withAnswers = false,
        )
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3)
        val files = entries(
            DocxWriter.build(sheet) { path ->
                if (path == "figures/a.webp") DocxWriter.Figure(jpeg, 800, 600) else null
            },
        )
        assertTrue(files.containsKey("word/media/image1.jpeg"))
        assertTrue(files.containsKey("word/_rels/document.xml.rels"))
        assertTrue(files["word/document.xml"].orEmpty().contains("r:embed=\"rIdImg"))
        assertTrue(files["[Content_Types].xml"].orEmpty().contains("image/jpeg"))
    }

    @Test
    fun `没有插图时不生成媒体部件`() {
        val files = entries(DocxWriter.build(sheet()))
        assertTrue(!files.keys.any { it.startsWith("word/media/") })
    }
}
