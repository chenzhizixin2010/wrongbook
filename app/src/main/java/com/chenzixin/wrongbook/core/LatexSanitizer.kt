package com.chenzixin.wrongbook.core

/**
 * LaTeX 文本清洗：把模型输出里各种写法归一成 Markwon 能识别的形式。
 *
 * 常见脏数据：
 * - `\( ... \)` / `\[ ... \]`（LaTeX 原生分隔符）
 * - 单 `$ ... $` —— **Markwon 的 latex 插件只认 `$$...$$`**（行内也用双美元，它的正则就是
 *   `(\${2})([\s\S]+?)\1`），单美元永远当普通字符，所以必须在这里升级成双美元
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

        // 5) 单 $ ... $ 升级成 $$ ... $$（Markwon 只认双美元）
        text = doubleDollarInline(text)

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

    /**
     * 把单 `$...$` 升级成 `$$...$$`。
     *
     * 已经成对的 `$$...$$` 先整段挖出来占位，避免被二次处理；
     * 内容是「首尾有空白的 `$5 和 $10`」这种当普通文字（价格、美元），不动它。
     */
    fun doubleDollarInline(text: String): String {
        if (!text.contains('$')) return text

        val placeholders = mutableListOf<String>()
        val masked = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            if (text.startsWith("$$", i)) {
                val end = text.indexOf("$$", i + 2)
                if (end > i + 2) {
                    placeholders += text.substring(i, end + 2)
                    masked.append('\u0001').append(placeholders.size - 1).append('\u0002')
                    i = end + 2
                    continue
                }
            }
            masked.append(text[i])
            i++
        }

        val upgraded = SINGLE_DOLLAR.replace(masked.toString()) { match ->
            val body = match.groupValues[1]
            if (body.first().isWhitespace() || body.last().isWhitespace()) {
                match.value
            } else {
                "$$" + body + "$$"
            }
        }

        if (placeholders.isEmpty()) return upgraded
        val out = StringBuilder(upgraded.length)
        var k = 0
        while (k < upgraded.length) {
            val ch = upgraded[k]
            if (ch == '\u0001') {
                val end = upgraded.indexOf('\u0002', k)
                if (end > k) {
                    val index = upgraded.substring(k + 1, end).toIntOrNull()
                    if (index != null && index in placeholders.indices) {
                        out.append(placeholders[index])
                        k = end + 1
                        continue
                    }
                }
            }
            out.append(ch)
            k++
        }
        return out.toString()
    }

    /** 单美元配对：中间不能有 `$`，也不能跨行。 */
    private val SINGLE_DOLLAR = Regex("\\$([^$\\n]+?)\\$")

    private fun collapse(segment: String): String =
        Regex("\\\\\\\\+([A-Za-z])").replace(segment) { "\\" + it.groupValues[1] }
}
