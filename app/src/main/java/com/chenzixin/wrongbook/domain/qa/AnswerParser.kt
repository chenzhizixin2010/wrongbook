package com.chenzixin.wrongbook.domain.qa

/**
 * 把「答案照片」识别出的文字拆成答案与解析。
 *
 * 老师/答案页的排版通常是：
 * ```
 * 答案：B
 * 解析：由动能定理可得……
 * ```
 * 但也有只写解析、或整段混排的情况，所以这里做三级降级：
 * 1. 有「答案」和「解析」标记 → 按标记切；
 * 2. 只有解析标记 → 全部当解析；
 * 3. 都没有 → 短文本当答案、长文本当解析。
 */
data class AnswerText(
    val answer: String,
    val analysis: String,
)

object AnswerParser {

    private val ANSWER_MARKS = listOf("答案", "答：", "答:", "参考答案")
    private val ANALYSIS_MARKS = listOf("解析", "分析", "解答", "详解", "思路", "点评")

    fun parse(raw: String): AnswerText {
        val text = raw.replace("\r\n", "\n").trim()
        if (text.isBlank()) return AnswerText("", "")

        val answerIndex = firstIndex(text, ANSWER_MARKS)
        val analysisIndex = firstIndex(text, ANALYSIS_MARKS)

        if (answerIndex == null && analysisIndex == null) {
            // 没有标记：短文本更像答案，长文本更像解析
            return if (text.length <= 60) {
                AnswerText(text, "")
            } else {
                AnswerText("", text)
            }
        }

        val cut = listOfNotNull(
            answerIndex?.let { it to true },
            analysisIndex?.let { it to false },
        ).sortedBy { it.first }

        val head = text.substring(0, cut.first().first).trim()
        var answer = ""
        var analysis = ""

        cut.forEachIndexed { index, (start, isAnswer) ->
            val end = cut.getOrNull(index + 1)?.first ?: text.length
            val chunk = stripLabel(text.substring(start, end).trim())
            if (isAnswer) {
                if (answer.isBlank()) answer = chunk
            } else {
                if (analysis.isBlank()) analysis = chunk else analysis += "\n" + chunk
            }
        }

        // 标记前面的内容通常是题干残留，忽略；若两者都空则整体兜底
        if (answer.isBlank() && analysis.isBlank()) {
            return AnswerText(head.ifBlank { text }, "")
        }
        return AnswerText(answer, analysis)
    }

    private fun firstIndex(text: String, marks: List<String>): Int? =
        marks.mapNotNull { mark -> text.indexOf(mark).takeIf { it >= 0 } }.minOrNull()

    private fun stripLabel(chunk: String): String =
        chunk.replace(Regex("^(参考答案|答案|答|解析|分析|解答|详解|思路|点评|说明)\\s*[:：]?"), "").trim()
}
