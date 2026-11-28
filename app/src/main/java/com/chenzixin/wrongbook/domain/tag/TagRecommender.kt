package com.chenzixin.wrongbook.domain.tag

/**
 * 根据 OCR 文本推荐标签（离线规则版）。
 *
 * 设计取舍：不依赖大模型也能用（离线优先），因此用「学科关键词 + 知识点关键词」两张表。
 * 命中越多置信度越高，最多返回 5 个，避免把整个标签库都推给用户。
 *
 * 纯函数，可在 JVM 上直接测。
 */
object TagRecommender {

    data class Suggestion(
        val name: String,
        val confidence: Float,
    )

    /** 学科名 → 判别关键词。命中最多者即为该题学科。 */
    private val SUBJECTS = linkedMapOf(
        "数学" to listOf("函数", "方程", "不等式", "导数", "积分", "向量", "数列", "概率", "三角函数",
            "几何", "面积", "体积", "根号", "sin", "cos", "tan", "log", "x^", "="),
        "物理" to listOf("力", "速度", "加速度", "电场", "磁场", "电压", "电流", "电阻", "功率",
            "重力", "摩擦", "动量", "能量", "波长", "频率", "g=", "m/s"),
        "化学" to listOf("化学", "反应", "溶液", "mol", "摩尔", "氧化", "还原", "离子", "化合物",
            "H2O", "CO2", "NaOH", "HCl", "pH", "催化"),
        "语文" to listOf("文言文", "古诗", "默写", "阅读", "作文", "成语", "病句", "修辞", "作者",
            "诗句", "注音", "字形"),
        "英语" to listOf("英语", "单词", "语法", "时态", "从句", "翻译", "the ", " to ", "ing", "从句"),
        "生物" to listOf("细胞", "遗传", "基因", "酶", "光合", "呼吸作用", "DNA", "蛋白质", "生态"),
        "历史" to listOf("历史", "朝代", "战争", "革命", "年", "世纪", "改革", "条约"),
        "地理" to listOf("地理", "气候", "地形", "洋流", "经纬", "板块", "人口", "城市化"),
        "政治" to listOf("政治", "哲学", "经济", "法律", "文化", "矛盾", "价值规律"),
    )

    /** 知识点标签：命中即推荐，标签名尽量和常见分类一致。 */
    private val TOPICS = linkedMapOf(
        "函数" to listOf("函数", "f(x)", "定义域", "值域", "单调"),
        "导数" to listOf("导数", "f'(x)", "求导", "切线", "极值"),
        "三角函数" to listOf("sin", "cos", "tan", "弧度", "周期"),
        "数列" to listOf("数列", "等差", "等比", "通项", "求和"),
        "概率统计" to listOf("概率", "期望", "方差", "排列", "组合"),
        "立体几何" to listOf("棱锥", "棱柱", "球", "二面角", "体积"),
        "解析几何" to listOf("椭圆", "双曲线", "抛物线", "直线方程", "圆"),
        "电学" to listOf("电场", "电路", "欧姆", "电压", "电流", "电阻"),
        "力学" to listOf("受力", "摩擦", "加速度", "牛顿", "动量"),
        "化学方程式" to listOf("反应", "方程式", "配平", "摩尔", "mol"),
        "有机化学" to listOf("烃", "醇", "醛", "酯", "苯"),
        "阅读理解" to listOf("阅读", "主旨", "作者", "划线"),
        "文言实词" to listOf("之", "其", "而", "以", "文言"),
        "时态语态" to listOf("时态", "被动", "完成时", "过去式"),
    )

    fun suggest(text: String, limit: Int = 5): List<Suggestion> {
        val source = text.lowercase()
        if (source.isBlank()) return emptyList()

        val scores = mutableMapOf<String, Float>()

        // 学科：命中关键词数 / 关键词总数，取最高的
        SUBJECTS.forEach { (subject, keywords) ->
            val hits = keywords.count { source.contains(it.lowercase()) }
            if (hits > 0) {
                scores[subject] = (0.5f + hits * 0.1f).coerceAtMost(0.95f)
            }
        }

        // 知识点
        TOPICS.forEach { (topic, keywords) ->
            val hits = keywords.count { source.contains(it.lowercase()) }
            if (hits > 0) {
                scores[topic] = (0.4f + hits * 0.15f).coerceAtMost(0.9f)
            }
        }

        return scores.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { Suggestion(it.key, it.value) }
    }

    /** 只取学科，用于自动填 subject 字段（只看学科命中数，不被知识点标签干扰）。 */
    fun detectSubject(text: String): String? {
        val source = text.lowercase()
        if (source.isBlank()) return null
        return SUBJECTS.entries
            .map { entry -> entry.key to entry.value.count { source.contains(it.lowercase()) } }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }
}
