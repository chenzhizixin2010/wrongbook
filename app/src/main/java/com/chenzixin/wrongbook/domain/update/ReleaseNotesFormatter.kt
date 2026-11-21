package com.chenzixin.wrongbook.domain.update

/**
 * Release 正文是 Markdown，直接塞进弹窗会看到一堆 `#`、`- `。
 * 这里做一层轻量清洗：保留结构与可读性，不引 Markdown 渲染器。
 */
object ReleaseNotesFormatter {

    fun toPlainText(markdown: String, maxChars: Int = 1200): String {
        if (markdown.isBlank()) return ""
        val lines = markdown.lines().map { raw ->
            val line = raw.trimEnd()
            when {
                line.startsWith("### ") -> line.removePrefix("### ").trim()
                line.startsWith("## ") -> line.removePrefix("## ").trim()
                line.startsWith("# ") -> line.removePrefix("# ").trim()
                line.startsWith("- ") -> "• " + line.removePrefix("- ").trim()
                line.startsWith("* ") -> "• " + line.removePrefix("* ").trim()
                line.startsWith("> ") -> line.removePrefix("> ").trim()
                line == ">" -> ""
                else -> line
            }
        }
        val text = lines.joinToString("\n")
            // 去掉加粗/斜体标记，弹窗里不渲染 Markdown，留着就是裸符号
            .replace("**", "")
            .replace("__", "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        return if (text.length <= maxChars) text else text.take(maxChars).trimEnd() + "…"
    }
}
