package com.chenzixin.wrongbook.domain.generate

/**
 * AI 出题质量闸门（离线规则，纯 Kotlin 可单测）。
 *
 * 为什么需要它：大模型出的变式题常见四类问题——
 * 1. **不可作答**：题干写「如图」却给不出图、题干被截断、答案写「略」；
 * 2. **重复**：同一道题吐两遍，或者干脆把原题抄回来只改一个数；
 * 3. **选择题残缺**：只有 A/B 两个选项，或者答案给的是解析段落而不是选项；
 * 4. **没解析**：只给答案，学生看完还是不会。
 *
 * 这里全部用规则判掉，判掉的题会带着原因回传给模型重出，而不是直接印在卷子上。
 */
object QuestionQuality {

    const val PASS_SCORE = 65

    enum class Severity { FATAL, MINOR }

    data class Issue(
        val code: String,
        /** 给用户/模型看的中文原因 */
        val hint: String,
        val severity: Severity,
        val penalty: Int = 0,
    )

    data class Report(
        val score: Int,
        val issues: List<Issue>,
        val sanitized: PracticeItem,
    ) {
        val accepted: Boolean get() = issues.none { it.severity == Severity.FATAL } && score >= PASS_SCORE
        val fatalHints: List<String> get() = issues.filter { it.severity == Severity.FATAL }.map { it.hint }
        val allHints: List<String> get() = issues.map { it.hint }
    }

    data class Outcome(
        val accepted: List<PracticeItem>,
        /** 被拒的题 + 原因，用来给模型「返工单」 */
        val rejected: List<Pair<PracticeItem, List<String>>>,
    ) {
        val rejectedCount: Int get() = rejected.size
    }

    private val OPTION_MARK = Regex("(?m)^\\s*([A-FＡ-Ｆ])[.．、)）:：]")
    private val NUMBERS = Regex("\\d+(?:\\.\\d+)?")
    private val LEADING_NOISE = Regex(
        "^(?:第\\s*\\d+\\s*题|题目\\s*\\d*|试题\\s*\\d*|\\d{1,2}\\s*[.、)）]|[（(]\\d{1,2}[)）])\\s*",
    )
    private val PLACEHOLDER_ANSWERS = setOf(
        "略", "略。", "无", "-", "—", "见解析", "见解析。", "答案见解析", "同上", "todo", "tbd", "null",
    )
    private val FIGURE_HINTS = listOf("如图", "图中", "下图", "上图", "图示", "如下图", "右图", "左图")
    /** 答案开头的选项字母：允许「答案为 A」「选 B」「A.」这几种写法。 */
    private val CHOICE_HEAD = Regex("^[^A-Fa-f]{0,6}?([A-Fa-f])")

    // ---------------------------------------------------------------- 规范化

    /** 去掉模型自带题号、多余空行与首尾空白。 */
    fun sanitizeStem(raw: String): String {
        var text = raw.replace("\r\n", "\n").replace('\u3000', ' ').trim()
        // 模型常把题号写进题干，卷子自己会编号，这里剥掉
        repeat(2) {
            val stripped = LEADING_NOISE.replaceFirst(text, "").trim()
            if (stripped != text) text = stripped
        }
        return text.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    /** 字母选项的序号；没有选项时返回空。 */
    fun optionLetters(stem: String): List<Char> = OPTION_MARK.findAll(stem)
        .map { it.groupValues[1].first() }
        .distinct()
        .toList()

    fun numbers(text: String): List<String> = NUMBERS.findAll(text).map { it.value }.toList()

    // ---------------------------------------------------------------- 单项质检

    fun check(
        item: PracticeItem,
        sources: List<String> = emptyList(),
        accepted: List<PracticeItem> = emptyList(),
    ): Report {
        val stem = OptionFormatter.wrap(sanitizeStem(item.stem))
        val answer = item.answer.trim()
        val analysis = item.analysis.trim()
        val issues = mutableListOf<Issue>()

        // 1) 题干
        if (stem.length < 6) {
            issues += Issue("stem_empty", "题干为空或太短", Severity.FATAL)
        } else if (stem.endsWith("…") || stem.endsWith("...") || stem.contains("（此处略）") || stem.contains("(略)")) {
            issues += Issue("stem_truncated", "题干被截断或含「略」", Severity.FATAL)
        }

        // 2) 图：AI 不能出图，凡是依赖「如图」的题都不可作答
        if (item.figures.isEmpty() && FIGURE_HINTS.any { stem.contains(it) }) {
            issues += Issue("figure_missing", "题干依赖插图，但 AI 无法生成图片", Severity.FATAL)
        }

        // 3) 答案
        if (answer.isBlank()) {
            issues += Issue("answer_missing", "缺少答案", Severity.FATAL)
        } else if (PLACEHOLDER_ANSWERS.contains(answer.lowercase().trimEnd('。', '.', ' '))) {
            issues += Issue("answer_placeholder", "答案写的是「$answer」，等于没给", Severity.FATAL)
        }

        // 4) 解析
        if (analysis.isBlank()) {
            issues += Issue("analysis_missing", "缺少解析", Severity.FATAL)
        } else if (analysis.length < 16) {
            issues += Issue("analysis_short", "解析太短，没讲清思路", Severity.MINOR, 20)
        }

        // 5) 选择题结构
        val letters = optionLetters(stem)
        when {
            letters.isEmpty() -> Unit
            // 只剩一个选项 = 模型把选项写漏了
            letters.size == 1 -> issues += Issue("options_incomplete", "选择题只有一个选项", Severity.FATAL)
            // 判断题（对/错）常常只有两个，给一点扣分放行
            letters.size in 2..3 -> issues += Issue("options_incomplete", "只识别到 ${letters.size} 个选项", Severity.MINOR, 20)
            letters.size > 6 -> issues += Issue("options_too_many", "选项超过 6 个，格式可能乱了", Severity.MINOR, 10)
            else -> {
                val upper = answer.uppercase()
                val picked = CHOICE_HEAD.find(upper)?.groupValues?.get(1)?.firstOrNull()
                    // 答案就是「E」这种不在选项里的字母时也要认出来
                    ?: upper.trim().firstOrNull()?.takeIf { it in 'A'..'F' }
                when {
                    picked == null -> issues += Issue(
                        "choice_answer_unclear",
                        "选择题答案应以选项字母开头",
                        Severity.MINOR,
                        20,
                    )
                    picked !in letters -> issues += Issue(
                        "choice_answer_invalid",
                        "答案 $picked 不在选项里",
                        Severity.FATAL,
                    )
                }
            }
        }

        // 6) 与原题重复
        sources.forEach { source ->
            if (similarity(stem, source) >= 0.90) {
                issues += Issue("duplicate_source", "和原题几乎一模一样", Severity.FATAL)
            }
        }

        // 7) 与本次已收的题重复
        accepted.forEach { other ->
            if (similarity(stem, other.stem) >= 0.82) {
                issues += Issue("duplicate_sibling", "和本次另一道题重复", Severity.FATAL)
            }
        }

        // 8) 换了皮没换数：结构几乎一致且数字完全相同 → 难度没变
        sources.firstOrNull { source ->
            val structure = similarity(shape(stem), shape(source))
            structure >= 0.85 && numbers(stem).isNotEmpty() && numbers(stem) == numbers(source)
        }?.let {
            issues += Issue("numbers_unchanged", "只改了措辞，数字和原题一样", Severity.MINOR, 15)
        }

        val penalty = issues.sumOf { it.penalty }
        return Report(
            score = (100 - penalty).coerceIn(0, 100),
            issues = issues,
            sanitized = item.copy(stem = stem, answer = answer, analysis = analysis),
        )
    }

    /**
     * 批量过闸：按顺序逐条检查（后面的题要跟已收的题比重复）。
     *
     * [needed] 只用于提前收工，不收满不影响正确性。
     */
    fun filter(
        items: List<PracticeItem>,
        sources: List<String> = emptyList(),
        needed: Int = Int.MAX_VALUE,
    ): Outcome {
        val accepted = mutableListOf<PracticeItem>()
        val rejected = mutableListOf<Pair<PracticeItem, List<String>>>()
        items.forEach { item ->
            // 收满就停：多出来的不算「被拒」，否则统计会给用户错误印象
            if (accepted.size >= needed) return@forEach
            val report = check(item, sources, accepted)
            if (report.accepted) {
                accepted += report.sanitized
            } else {
                rejected += report.sanitized to report.allHints
            }
        }
        return Outcome(accepted, rejected)
    }

    // ---------------------------------------------------------------- 相似度

    /** 字符二元组 Jaccard 相似度，0..1。数字参与计算。 */
    fun similarity(a: String, b: String): Double = jaccard(normalize(a), normalize(b))

    /** 把数字统一成 `#`，用来判断「结构是否一样」。 */
    fun shape(text: String): String = NUMBERS.replace(text) { "#" }

    private fun normalize(text: String): String = text.lowercase()
        .filter { it.isLetterOrDigit() }

    private fun jaccard(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        if (a.length < 2 || b.length < 2) return if (a == b) 1.0 else 0.0
        val x = grams(a)
        val y = grams(b)
        if (x.isEmpty() || y.isEmpty()) return 0.0
        var inter = 0
        x.forEach { (gram, count) ->
            val other = y[gram] ?: return@forEach
            inter += minOf(count, other)
        }
        val union = x.values.sum() + y.values.sum() - inter
        return if (union <= 0) 0.0 else inter.toDouble() / union
    }

    private fun grams(text: String): Map<String, Int> {
        val map = HashMap<String, Int>(text.length)
        for (i in 0 until text.length - 1) {
            val gram = text.substring(i, i + 2)
            map[gram] = (map[gram] ?: 0) + 1
        }
        return map
    }
}
