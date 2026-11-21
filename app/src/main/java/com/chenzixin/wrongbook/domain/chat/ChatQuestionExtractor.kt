package com.chenzixin.wrongbook.domain.chat

/**
 * 从 AI 回复里抽出一道题（题干 / 答案 / 解析）。
 *
 * 对话式回答的格式太多，这里只做「够用」的启发式：
 * 1. 先去掉 Markdown 装饰（#、**、`、列表符号、代码块围栏）；
 * 2. 按「答案 / 解析 / 解答 / 点评」这类标记切段；
 * 3. 标记之前的就是题干；同一段里若出现「A. B. C. D.」选项也归入题干。
 *
 * 抽不准也没关系——UI 里三段都可编辑，用户改完再入库。
 */
data class ExtractedQuestion(
    val stem: String,
    val answer: String,
    val analysis: String,
) {
    val isEmpty: Boolean get() = stem.isBlank() && answer.isBlank()
}

object ChatQuestionExtractor {

    private val ANSWER_MARKS = listOf("答案", "答：", "答:", "结果", "所以答案是")
    private val ANALYSIS_MARKS = listOf("解析", "分析", "解答", "详解", "思路", "点评", "说明")

    fun extract(raw: String): ExtractedQuestion {
        val text = clean(raw)
        if (text.isBlank()) return ExtractedQuestion("", "", "")

        val answerIndex = firstIndexOf(text, ANSWER_MARKS)
        val analysisIndex = firstIndexOf(text, ANALYSIS_MARKS)

        // 两个标记都可能出现，按位置切：题干 | 答案 | 解析
        val cutPoints = listOfNotNull(
            answerIndex?.let { it to ANSWER },
            analysisIndex?.let { it to ANALYSIS },
        ).sortedBy { it.first }

        if (cutPoints.isEmpty()) {
            return ExtractedQuestion(stem = text.trim(), answer = "", analysis = "")
        }

        val stem = text.substring(0, cutPoints.first().first).trim()
        var answer = ""
        var analysis = ""

        cutPoints.forEachIndexed { index, (start, kind) ->
            val end = cutPoints.getOrNull(index + 1)?.first ?: text.length
            val chunk = text.substring(start, end).trim()
            val body = stripLabel(chunk)
            when (kind) {
                ANSWER -> if (answer.isBlank()) answer = body
                ANALYSIS -> if (analysis.isBlank()) analysis = body else analysis += "\n" + body
            }
        }

        return ExtractedQuestion(
            stem = stem.ifBlank { text.trim() },
            answer = answer,
            analysis = analysis,
        )
    }

    private const val ANSWER = 0
    private const val ANALYSIS = 1

    private fun firstIndexOf(text: String, marks: List<String>): Int? =
        marks.mapNotNull { mark ->
            val idx = text.indexOf(mark)
            if (idx >= 0) idx else null
        }.minOrNull()

    /** 去掉「答案：」这类前缀，只留内容。 */
    private fun stripLabel(chunk: String): String =
        chunk.replace(Regex("^(答案|答|解析|分析|解答|详解|思路|点评|结果|说明)\\s*[:：]?"), "").trim()

    private fun clean(raw: String): String {
        var text = raw
        // 代码块围栏
        text = text.replace("```", "")
        // 行首的 #、>、列表符号
        text = text.lines().joinToString("\n") { line ->
            line.trimStart()
                .replace(Regex("^#+\\s*"), "")   // 去掉 1~n 级标题的 #
                .removePrefix("> ")
                .removePrefix("- ")
                .removePrefix("* ")
        }
        // 加粗/斜体标记
        text = text.replace("**", "").replace("__", "")
        // 行内代码
        text = text.replace("`", "")
        // LaTeX 分隔符（保留公式本体）
        text = text.replace("\\$\\$", "").replace("\\(", "").replace("\\)", "").replace("\\[", "").replace("\\]", "")
        text = text.replace("$", "")
        return text.replace(Regex("\n{3,}"), "\n\n").trim()
    }
}
