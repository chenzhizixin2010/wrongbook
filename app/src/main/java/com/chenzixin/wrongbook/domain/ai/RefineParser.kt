package com.chenzixin.wrongbook.domain.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** AI 整理出来的结构化题目。 */
data class RefinedQuestion(
    val stem: String,
    val answer: String = "",
    val analysis: String = "",
    val subject: String = "",
    val knowledgePoints: List<String> = emptyList(),
)

/**
 * 解析模型的 JSON 输出。
 *
 * 现实里模型经常：套 ```json 代码块、前后加解释、字段缺失、被截断。
 * 这里做「宽松解析」，失败返回 null 由上层走兜底（原文即题干 + 提示用户）。
 */
object RefineParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(raw: String): RefinedQuestion? {
        val obj = extractObject(raw) ?: return null
        val stem = obj["stem"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (stem.isEmpty()) return null
        return RefinedQuestion(
            stem = stem,
            answer = obj["answer"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            analysis = obj["analysis"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            subject = obj["subject"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            knowledgePoints = obj["knowledgePoints"]?.let(::stringList).orEmpty(),
        )
    }

    /** 解析同类题数组：既接受 {"questions":[...]} 也接受裸数组。 */
    fun parseVariants(raw: String): List<RefinedQuestion> {
        val array = extractArray(raw) ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val stem = obj["stem"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (stem.isEmpty()) return@mapNotNull null
            RefinedQuestion(
                stem = stem,
                answer = obj["answer"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                analysis = obj["analysis"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                subject = obj["subject"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                knowledgePoints = obj["knowledgePoints"]?.let(::stringList).orEmpty(),
            )
        }
    }

    private fun extractObject(raw: String): JsonObject? {
        val text = stripFences(raw)
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { json.parseToJsonElement(text.substring(start, end + 1)).jsonObject }
            .getOrNull()
    }

    private fun extractArray(raw: String): JsonArray? {
        val text = stripFences(raw)
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        val candidate = if (start >= 0 && end > start) text.substring(start, end + 1) else text
        runCatching { json.parseToJsonElement(candidate).jsonArray }.getOrNull()?.let { return it }
        // 也接受被包在对象里的 {"questions": [...]}
        extractObject(raw)?.get("questions")?.let { return it as? JsonArray }
        return null
    }

    private fun stringList(element: kotlinx.serialization.json.JsonElement): List<String> =
        runCatching {
            element.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf(String::isNotBlank) }
        }.getOrDefault(emptyList())

    private fun stripFences(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.contains("```")) return trimmed
        val fenceStart = trimmed.indexOf("```")
        val afterLang = trimmed.indexOf('\n', fenceStart).let { if (it < 0) fenceStart + 3 else it + 1 }
        val fenceEnd = trimmed.lastIndexOf("```")
        return if (fenceEnd > afterLang) trimmed.substring(afterLang, fenceEnd) else trimmed
    }
}
