package com.chenzixin.wrongbook.domain.generate

import com.chenzixin.wrongbook.domain.tag.TagRecommender

/**
 * 离线题型风格库：给「AI 出同类题」当写作范本。
 *
 * 为什么用离线库而不是联网搜题：真实考卷的排版、设问方式、干扰项套路是**稳定的**，
 * 全部写死成规则就能让模型照着写，比让它自己发挥质量高一截，而且零网络依赖。
 * 联网搜题是可选增强（见 [PracticeRepository]），本库永远可用。
 *
 * 纯数据 + 纯函数，可在 JVM 上直接测。
 */
object QuestionArchetypes {

    /** 所有科目共用的硬要求：这几条决定了卷子能不能用。 */
    val COMMON: List<String> = listOf(
        "题干必须自洽：单独看这一道题就能作答，不许出现「如图」「同上」「略」「见前文」",
        "不许出需要插图的题（AI 生成不了图）；几何关系用文字说清，例如「等腰三角形 ABC，AB=AC=5，BC=6」",
        "选择题固定四个选项，A—D 各占一行，选项长短接近，不许写「以上都对」「以上都不对」",
        "答案必须是可判定的最终结果：计算题给数值和单位，选择题给选项字母，填空给要填的内容",
        "解析写清「第一步…第二步…」，并点明本题的易错点，至少 40 字",
        "不许照抄原题：数字、情境、设问方向至少改两项；也不要只换一个数字就交差",
        "难度与原题一致，不许超纲到没学过的知识",
        "全部中文标点；数学式子用纯文本写法（x^2、√(3)、a/b、m/s²），不要 Markdown 公式块",
    )

    private val BY_SUBJECT: Map<String, List<String>> = mapOf(
        "数学" to listOf(
            "计算题要给可验算的具体数值，避免出现除不尽的分数；结果能化简就化简",
            "函数题必须写清定义域；不等式题注意端点取不取等号",
            "概率题说清「不放回 / 放回」「至少 / 恰好」",
            "解析几何用纯文字给出已知条件（焦点、通径、斜率），不要让读者想象图形",
        ),
        "物理" to listOf(
            "所有数据带国际单位，计算过程保留单位；g 取 10 m/s² 时要在题干写明",
            "受力分析用文字描述各力方向（如「水平向右的拉力」「沿斜面向下的摩擦力」）",
            "选择题的干扰项要对应典型错误：漏算重力、方向搞反、单位未换算",
            "实验题写清器材与测量量，不给图也能答",
        ),
        "化学" to listOf(
            "化学式写成 H2O、CO2 这种形式；方程式用 → 并在箭头上下标注条件",
            "计算题给出相对原子质量（H=1、O=16、Na=23…）",
            "摩尔体积、浓度计算保留一位小数，并注明单位",
            "离子方程式注意电荷守恒与拆分规则",
        ),
        "生物" to listOf(
            "遗传题写清亲本基因型与表现型，比例写成 3:1 这种形式",
            "实验设计题给出自变量、因变量与对照组",
            "不要出现需要看显微镜照片才能判断的题",
        ),
        "英语" to listOf(
            "语法填空给出一句完整语境，空格用 ___ 表示，并在括号里给提示词",
            "选择题四个选项词性必须一致、形式接近",
            "解析要说明「为什么选它」以及「其他选项错在哪」",
        ),
        "语文" to listOf(
            "文言实词题给出原句，四个选项分别解释，答案指明正确义项",
            "阅读理解材料控制在 200 字以内，设问指向明确（概括/赏析/作用）",
            "默写题给出上下句，答案只写要补的那一句",
        ),
        "历史" to listOf(
            "材料题给出一段 100 字以内的史料或数据，设问问原因或影响",
            "时间、人物、事件必须真实存在，不许编造",
            "选择题干扰项要用同时期但不对应的史实",
        ),
        "地理" to listOf(
            "区域题用文字描述位置与气候特征，不依赖地图",
            "计算题（时区、比例尺、正午太阳高度）给出全部已知量",
            "选择题干扰项对应常见误区（成因混淆、方向颠倒）",
        ),
        "政治" to listOf(
            "材料题给出一段 100 字以内的时政或生活情境",
            "答案要用教材术语（如「矛盾的普遍性与特殊性」），不要口语化",
            "设问明确指向「体现」「说明」「启示」中的一种",
        ),
    )

    /** 学科 → 该学科的写作要点；识别不到学科时只给通用要求。 */
    fun forSubject(subject: String?): List<String> {
        val key = subject?.trim().orEmpty()
        if (key.isEmpty()) return emptyList()
        // 允许传入「高中物理」这种带前缀的名字
        return BY_SUBJECT.entries.firstOrNull { key.contains(it.key) }?.value ?: emptyList()
    }

    /** 从标签名 + 题干里猜学科；猜不到返回 null。 */
    fun detectSubject(subjects: List<String>): String? =
        subjects.firstNotNullOfOrNull { tag ->
            BY_SUBJECT.keys.firstOrNull { tag.contains(it) }
        }

    fun detectSubjectFromText(text: String): String? = TagRecommender.detectSubject(text)

    /** 拼成给模型的「出题规范」段落。 */
    fun promptBlock(subjects: List<String>, difficulty: String = ""): String = buildString {
        appendLine("【出题规范 · 必须逐条满足】")
        COMMON.forEach { appendLine("- $it") }
        val subjectKeys = if (subjects.isEmpty()) {
            emptyList()
        } else {
            BY_SUBJECT.keys.filter { key -> subjects.any { it.contains(key) } }
        }
        if (subjectKeys.isNotEmpty()) {
            appendLine("【学科要求】")
            subjectKeys.forEach { key ->
                BY_SUBJECT.getValue(key).forEach { appendLine("- $key：$it") }
            }
        }
        if (difficulty.isNotBlank()) {
            appendLine("【难度】$difficulty（与原题保持一致，不额外加难）")
        }
    }
}
