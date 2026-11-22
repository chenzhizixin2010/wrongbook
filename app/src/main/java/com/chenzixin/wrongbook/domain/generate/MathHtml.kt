package com.chenzixin.wrongbook.domain.generate

/**
 * 把题干里简单的 LaTeX 片段转成 HTML（打印用）。
 *
 * 打印走的是 WebView，没有 KaTeX/MathJax 可用（离线、不引 5MB 的库），
 * 所以只做「够看」的轻量处理：
 * - `$...$` 里的内容用衬线斜体显示
 * - `^2` / `^{n}` → `<sup>`，`_1` / `_{n}` → `<sub>`
 * - 常见符号替换：× ÷ ± ≤ ≥ ≠ ∞ π ° √
 *
 * 复杂排版（分式、矩阵）会退化成纯文本，但不会出现一堆 `$` 和 `^`。
 */
object MathHtml {

    private val inlineMath = Regex("\\$([^$]+)\\$")

    fun format(text: String): String {
        val escaped = PracticeSheetHtml.escape(text)
        return escaped.lines().joinToString("<br/>") { line ->
            inlineMath.replace(line) { match ->
                "<span class=\"math\">${convert(match.groupValues[1])}</span>"
            }
        }
    }

    private fun convert(raw: String): String {
        var s = raw
        // 上下标：先处理花括号形式，再处理单字符
        s = Regex("\\^\\{([^}]*)}").replace(s) { "<sup>${it.groupValues[1]}</sup>" }
        s = Regex("_\\{([^}]*)}").replace(s) { "<sub>${it.groupValues[1]}</sub>" }
        s = Regex("\\^(-?[0-9A-Za-z])").replace(s) { "<sup>${it.groupValues[1]}</sup>" }
        s = Regex("_([0-9A-Za-z])").replace(s) { "<sub>${it.groupValues[1]}</sub>" }

        // 常用符号
        val symbols = mapOf(
            "\\times" to "×",
            "\\div" to "÷",
            "\\pm" to "±",
            "\\leq" to "≤",
            "\\geq" to "≥",
            "\\neq" to "≠",
            "\\infty" to "∞",
            "\\pi" to "π",
            "\\degree" to "°",
            "\\sqrt" to "√",
        )
        symbols.forEach { (from, to) -> s = s.replace(from, to) }

        // 去掉其余反斜杠命令，至少别显示成 \frac{a}{b}
        s = Regex("\\\\[A-Za-z]+").replace(s, "")
        s = s.replace("{", "").replace("}", "")
        return s.trim()
    }
}
