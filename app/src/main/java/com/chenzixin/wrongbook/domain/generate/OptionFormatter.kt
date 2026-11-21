package com.chenzixin.wrongbook.domain.generate

/**
 * 选项换行：把「A. -1    B. 0    C. 1    D. 2」这种一行里塞四个选项的排版，
 * 拆成每个选项独占一行。
 *
 * 为什么需要：OCR 出来的选择题经常是**横向排布**，四个选项挤在同一行；
 * 拍照裁切后这一行可能很长，生成练习卷时按自然宽度折行会从选项中间断开，
 * 读起来很别扭。按选项边界断行才是「阅读顺序正确」的做法。
 */
object OptionFormatter {

    /** 选项标记：A. / A、 / A) / （A） / (A)，前面必须不是字母数字，避免误伤 "A股" 这种词。 */
    private val OPTION = Regex("(?<![A-Za-z0-9])(?:[（(]?([A-Fa-f])[）)]\\s*|[（(]?([A-Fa-f])[.、:：]\\s*)")

    fun wrap(text: String): String {
        if (text.isBlank()) return text

        return text.lines().joinToString("\n") { line ->
            wrapLine(line)
        }
    }

    private fun wrapLine(line: String): String {
        // 至少要出现两个选项标记才值得换行（单选项的普通句子别动）
        val hits = OPTION.findAll(line).toList()
        if (hits.size < 2) return line

        val sb = StringBuilder()
        var cursor = 0
        hits.forEachIndexed { index, match ->
            val start = match.range.first
            if (index > 0 && start > cursor) {
                // 选项前补一个换行（去掉多余空格）
                sb.append(line.substring(cursor, start).trimEnd())
                sb.append('\n')
                cursor = start
            }
        }
        sb.append(line.substring(cursor))
        return sb.toString().trimEnd()
    }
}
