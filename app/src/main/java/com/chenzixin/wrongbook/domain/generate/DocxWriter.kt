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

    /** 插图：JPEG 字节 + 原始像素尺寸（用来算 Word 里的显示大小）。 */
    data class Figure(val bytes: ByteArray, val width: Int, val height: Int)

    fun build(sheet: PracticeSheet, loadFigure: (String) -> Figure? = { null }): ByteArray {
        val media = mutableListOf<Pair<String, Figure>>()   // rId -> figure
        var mediaIndex = 0

        fun figureParagraph(path: String): String {
            val figure = loadFigure(path) ?: return ""
            val rId = "rIdImg${mediaIndex++}"
            media += rId to figure
            return imageParagraph(rId, figure, mediaIndex)
        }

        val body = buildString {
            // 标题
            append(paragraph(sheet.title, bold = true, sizePt = 36, spacingAfter = 120))
            append(paragraph(sheet.subtitle, sizePt = 20, color = "666666", spacingAfter = 240))

            sheet.items.forEach { item ->
                append(paragraph("${item.index}. ${item.stem}", sizePt = 24, spacingAfter = 80))
                item.figures.forEach { path -> append(figureParagraph(path)) }
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

        val relsXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
            media.forEachIndexed { index, (rId, _) ->
                append(
                    "<Relationship Id=\\\"$rId\\\" Type=\\\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\\\" Target=\\\"media/image${index + 1}.jpeg\\\"/>",
                )
            }
            append("</Relationships>")
        }

        val document = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<w:body>$body<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/></w:sectPr></w:body>
</w:document>"""

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Default Extension="jpeg" ContentType="image/jpeg"/>
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

            zip.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zip.write(relsXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            media.forEachIndexed { index, (_, figure) ->
                zip.putNextEntry(ZipEntry("word/media/image${index + 1}.jpeg"))
                zip.write(figure.bytes)
                zip.closeEntry()
            }
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

    /** 居中的内嵌图片段落；宽度上限约 12cm（1cm ≈ 360000 EMU）。 */
    private fun imageParagraph(rId: String, figure: Figure, id: Int): String {
        val maxWidthEmu = 4_320_000L
        val aspect = figure.height.toDouble() / figure.width.coerceAtLeast(1)
        val width = maxWidthEmu
        val height = (width * aspect).toLong().coerceAtMost(5_400_000L)
        return buildString {
            append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:drawing>")
            append("<wp:inline xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">")
            append("<wp:extent cx=\"$width\" cy=\"$height\"/>")
            append("<wp:docPr id=\"$id\" name=\"Figure $id\"/>")
            append("<a:graphic xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">")
            append("<a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">")
            append("<pic:pic xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">")
            append("<pic:nvPicPr><pic:cNvPr id=\"$id\" name=\"image$id.jpeg\"/><pic:cNvPicPr/></pic:nvPicPr>")
            append("<pic:blipFill><a:blip r:embed=\"$rId\"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>")
            append("<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"$width\" cy=\"$height\"/></a:xfrm>")
            append("<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr></pic:pic>")
            append("</a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>")
        }
    }
}
