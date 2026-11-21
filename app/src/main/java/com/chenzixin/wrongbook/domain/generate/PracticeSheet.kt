package com.chenzixin.wrongbook.domain.generate

/**
 * 练习卷模型与可打印 HTML。
 *
 * 打印用系统 WebView + PrintManager：HTML/CSS 排版比用 PdfDocument 手动画文字省事得多，
 * 而且中文断行、分页、页码都由打印框架处理。
 */
data class PracticeItem(
    val index: Int,
    val stem: String,
    val answer: String = "",
    val analysis: String = "",
)

data class PracticeSheet(
    val title: String,
    val subtitle: String,
    val items: List<PracticeItem>,
    val withAnswers: Boolean,
)

object PracticeSheetHtml {

    fun build(sheet: PracticeSheet): String {
        val questions = sheet.items.joinToString("\n") { item ->
            """
            <section class="q">
              <div class="stem"><span class="no">${item.index}.</span> ${escape(item.stem)}</div>
              <div class="space"></div>
            </section>
            """.trimIndent()
        }

        val answers = if (sheet.withAnswers) {
            val rows = sheet.items.joinToString("\n") { item ->
                """
                <div class="ans">
                  <div class="ans-no">${item.index}.</div>
                  <div class="ans-body">
                    <div class="ans-text">${escape(item.answer.ifBlank { "（未提供答案）" })}</div>
                    ${if (item.analysis.isNotBlank()) "<div class=\"ans-analysis\">解析：${escape(item.analysis)}</div>" else ""}
                  </div>
                </div>
                """.trimIndent()
            }
            """<section class="answers" style="page-break-before: always;">
                 <h2>参考答案</h2>
                 $rows
               </section>"""
        } else {
            ""
        }

        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  @page { size: A4; margin: 18mm 16mm; }
  body { font-family: -apple-system, "Noto Sans CJK SC", "PingFang SC", "Microsoft YaHei", sans-serif;
         color: #111; font-size: 12pt; line-height: 1.6; margin: 0; }
  header { border-bottom: 1.5pt solid #111; padding-bottom: 6pt; margin-bottom: 14pt; }
  h1 { font-size: 17pt; margin: 0 0 4pt 0; }
  .sub { font-size: 10pt; color: #555; }
  .q { margin: 0 0 14pt 0; page-break-inside: avoid; }
  .stem { white-space: pre-wrap; }
  .no { font-weight: 600; }
  .space { height: 52pt; border-bottom: 0.5pt dashed #bbb; margin-top: 6pt; }
  .answers h2 { font-size: 14pt; border-bottom: 1pt solid #111; padding-bottom: 4pt; }
  .ans { display: flex; gap: 8pt; margin: 8pt 0; page-break-inside: avoid; }
  .ans-no { font-weight: 600; min-width: 22pt; }
  .ans-analysis { color: #444; font-size: 10.5pt; margin-top: 2pt; white-space: pre-wrap; }
  footer { position: fixed; bottom: 0; left: 0; right: 0; text-align: center;
           font-size: 9pt; color: #888; }
</style>
</head>
<body>
  <header>
    <h1>${escape(sheet.title)}</h1>
    <div class="sub">${escape(sheet.subtitle)}</div>
  </header>
  $questions
  $answers
  <footer>错题本 · 自动生成</footer>
</body>
</html>
        """.trimIndent()
    }

    /** 必须转义，否则题干里的 `<` 会把后面的 HTML 结构吃掉。 */
    fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
