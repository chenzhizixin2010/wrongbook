package com.chenzixin.wrongbook.domain.generate

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * 出题蓝图：先让模型说清「考什么、考到什么程度、设什么陷阱」，再让它出题。
 *
 * 单步「直接出 10 道题」时模型很容易越写越水（后面几道几乎重复）；
 * 先出蓝图能把考点摊开，出题时逐条对齐，质量明显更稳。
 */
data class Blueprint(
    val subject: String = "",
    val topic: String = "",
    val difficulty: String = "",
    val types: List<String> = emptyList(),
    val traps: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = subject.isBlank() && topic.isBlank() && types.isEmpty() && traps.isEmpty()

    /** 拼进提示词的一行摘要。 */
    fun promptLine(): String = buildString {
        if (subject.isNotBlank()) append("学科：").append(subject).append("；")
        if (topic.isNotBlank()) append("考点：").append(topic).append("；")
        if (difficulty.isNotBlank()) append("难度：").append(difficulty).append("；")
        if (types.isNotEmpty()) append("题型：").append(types.joinToString("、")).append("；")
        if (traps.isNotEmpty()) append("要考的易错点：").append(traps.joinToString("、"))
    }.trimEnd('；', ' ')
}

/** 解析蓝图的 JSON；模型乱写时返回 null，调用方直接跳过这一步。 */
object BlueprintParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String): Blueprint? {
        val text = raw.trim()
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val root = runCatching {
            json.parseToJsonElement(text.substring(start, end + 1)) as JsonObject
        }.getOrNull() ?: return null

        val blueprint = Blueprint(
            subject = text(root, "subject", "学科", "科目"),
            topic = text(root, "topic", "考点", "知识点"),
            difficulty = text(root, "difficulty", "难度"),
            types = list(root, "types", "题型", "questionTypes"),
            traps = list(root, "traps", "陷阱", "易错点"),
        )
        return blueprint.takeIf { !it.isEmpty }
    }

    private fun text(root: JsonObject, vararg keys: String): String {
        keys.forEach { key ->
            val value = root[key]?.jsonPrimitive?.contentOrNull
            if (!value.isNullOrBlank()) return value
        }
        return ""
    }

    private fun list(root: JsonObject, vararg keys: String): List<String> {
        keys.forEach { key ->
            val element = root[key] ?: return@forEach
            val array = runCatching { element.jsonArray }.getOrNull()
            if (array != null) {
                return array.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf(String::isNotBlank) }
            }
            // 模型偶尔把数组写成 "选择题、计算题"
            val single = element.jsonPrimitive.contentOrNull
            if (!single.isNullOrBlank()) {
                return single.split('、', ',', '，', '/').map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
        return emptyList()
    }
}
