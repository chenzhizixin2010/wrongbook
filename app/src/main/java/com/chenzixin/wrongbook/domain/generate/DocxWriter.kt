package com.chenzixin.wrongbook.domain.generate

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 生成最小可用的 .docx。
 *
 * 不引第三方库（Word 文档本质就是一堆 XML 打包成 zip）：
 * - `[Content_Types].xml` 声明部件类型
 * - `_rels/.rels` 指向主文档
 * - `word/document.xml` 才是正文
 *
 * Word / WPS / Google Docs 都能直接打开，也方便老师二次排版。
 */
object DocxWriter {

    fun build(sheet: PracticeSheet): ByteArray {
        val body = buildString {
            // 标题
            append(paragraph(sheet.title, bold = true, sizePt = 36, spacingAfter = 120))
            append(paragraph(sheet.subtitle, sizePt = 20, color = "666666", spacingAfter = 240))

            sheet.items.forEach { item ->
                append(paragraph("${item.index}. ${item.stem}", sizePt = 24, spacingAfter = 80))
                append(paragraph(" ", sizePt = 24, spacingAfter = 240))
            }

            if (sheet.withAnswers && sheet.items.any { it.answer.isNotBlank() || it.analysis.isNotBlank() }) {
                // 答案另起一页
                append(paragraph("参考答案", bold = true, sizePt = 30, spacingAfter = 160, pageBreakBefore = true))
                sheet.items.forEach { item ->
                    append(paragraph("${item.index}. ${item.answer.ifBlank { "（未提供）" }}", sizePt = 22, spacingAfter = 40))
                    if (item.analysis.isNotBlank()) {
                        append(paragraph("解析：${item.analysis}", sizePt = 20, color = "444444", spacingAfter = 120))
                    }
                }
            }
        }

        val document = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>$body<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/></w:sectPr></w:body>
</w:document>"""

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(contentTypes.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(rels.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(document.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun paragraph(
        text: String,
        bold: Boolean = false,
        sizePt: Int = 24,
        color: String? = null,
        spacingAfter: Int = 80,
        pageBreakBefore: Boolean = false,
    ): String {
        val halfPoints = sizePt * 2
        val props = buildString {
            append("<w:spacing w:after=\"$spacingAfter\"/>")
            if (pageBreakBefore) append("<w:pageBreakBefore/>")
        }
        val runProps = buildString {
            if (bold) append("<w:b/>")
            if (color != null) append("<w:color w:val=\"$color\"/>")
        }
        // 题干里的换行要转成 <w:br/>，否则 Word 里会连成一行
        val runs = text.split("\n").joinToString("<w:br/>") { segment ->
            "<w:r>${if (runProps.isEmpty()) "" else "<w:rPr>$runProps<w:sz w:val=\"$halfPoints\"/></w:rPr>"}<w:t xml:space=\"preserve\">${escape(segment)}</w:t></w:r>"
        }
        return "<w:p><w:pPr>$props<w:rPr>${if (bold) "<w:b/>" else ""}<w:sz w:val=\"$halfPoints\"/></w:rPr></w:pPr>$runs</w:p>"
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
