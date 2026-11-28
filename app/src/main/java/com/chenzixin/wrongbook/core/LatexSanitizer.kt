package com.chenzixin.wrongbook.core

/**
 * LaTeX 文本清洗：把模型输出里各种写法归一成 Markwon 能识别的形式。
 *
 * 常见脏数据：
 * - `\( ... \)` / `\[ ... \]`（LaTeX 原生分隔符）
 * - 双重转义：`\\mathrm{kJ}`（JSON 里多转义了一层）
 * - 反引号包裹的公式
 *
 * 纯函数，可单测。
 */
object LatexSanitizer {

    fun sanitize(markdown: String): String {
        if (markdown.isBlank()) return markdown
        var text = markdown

        // 1) \( ... \) → $ ... $
        text = text.replace("\\(", "$").replace("\\)", "$")
        // 2) \[ ... \] → $$ ... $$
        text = text.replace("\\[", "$$").replace("\\]", "$$")

        // 3) 数学片段内的双反斜杠折叠成单反斜杠
        text = collapseEscapesInsideMath(text)

        // 4) 行内代码包住的公式：`$x^2$` → $x^2$
        text = Regex("`(\\$[^`]+\\$)`").replace(text) { it.groupValues[1] }

        return text
    }

    /** 只在 `$...$` / `$$...$$` 里把 `\\command` 修成 `\command`。 */
    private fun collapseEscapesInsideMath(text: String): String {
        val builder = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch == '$') {
                val double = i + 1 < text.length && text[i + 1] == '$'
                val closer = if (double) "$$" else "$"
                val start = i + closer.length
                val end = text.indexOf(closer, start)
                if (end > start) {
                    builder.append(closer)
                    builder.append(collapse(text.substring(start, end)))
                    builder.append(closer)
                    i = end + closer.length
                    continue
                }
            }
            builder.append(ch)
            i++
        }
        return builder.toString()
    }

    private fun collapse(segment: String): String =
        Regex("\\\\\\\\+([A-Za-z])").replace(segment) { "\\" + it.groupValues[1] }
}
