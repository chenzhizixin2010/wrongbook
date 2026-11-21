package com.chenzixin.wrongbook.domain.scan

/**
 * OCR 结果的清洗：把识别噪声挡在切分之前。
 *
 * 实测问题：ML Kit 在试卷照片上会吐出只有一两个符号的碎片行（比如 "|" "。" "▌"），
 * 这些行会被当成独立题目或把相邻题目切断，所以先按「有没有实际语义」过滤一遍。
 */
object OcrLineCleaner {

    fun clean(lines: List<OcrLine>): List<OcrLine> =
        lines.filter { isMeaningful(it.text) }

    fun isMeaningful(raw: String): Boolean {
        val text = raw.trim()
        if (text.length < 2) return false
        // 至少要有一个汉字 / 字母 / 数字，纯符号行（分割线、括号、箭头）直接丢掉
        return text.any { ch ->
            ch.isLetterOrDigit() || ch in CJK_RANGE
        }
    }

    private val CJK_RANGE = '\u4e00'..'\u9fff'
}
