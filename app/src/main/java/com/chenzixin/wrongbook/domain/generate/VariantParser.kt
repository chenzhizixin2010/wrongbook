package com.chenzixin.wrongbook.domain.generate

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 解析 AI 出题结果。
 *
 * 真实模型经常不守规矩，所以做了三层：
 * 1. 标准 JSON（`questions` / `items` / `data` / `variants` 都认）；
 * 2. 中英文键名都认（stem / 题干 / 题目 …）；
 * 3. 连 JSON 都不是时，按题号把纯文本切成若干题，答案留空让用户自己写。
 *
 * 纯函数，可在 JVM 上直接测。
 */
object VariantParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String): List<PracticeItem> {
        fromJson(raw)?.let { if (it.isNotEmpty()) return it }
        return fromPlainText(raw)
    }

    private fun fromJson(raw: String): List<PracticeItem>? {
        val root = extractObject(raw) ?: return null
        val array = listOf(
            "questions", "items", "data", "variants",
            "题目", "试题", "题目列表",
        ).firstNotNullOfOrNull { key ->
            root[key]?.let { runCatching { it.jsonArray }.getOrNull() }
        } ?: return null

        return array.mapNotNull { element ->
            val obj = element.jsonObject
            val stem = firstText(obj, "stem", "question", "text", "题干", "题目", "题目内容").trim()
            if (stem.isEmpty()) return@mapNotNull null
            PracticeItem(
                index = 0,
                stem = stem,
                answer = firstText(obj, "answer", "答案", "解答"),
                analysis = firstText(obj, "analysis", "explain", "解析", "分析", "详解"),
            )
        }
    }

    private fun fromPlainText(raw: String): List<PracticeItem> {
        val text = raw.trim()
        if (text.isEmpty()) return emptyList()
        val pattern = Regex("(?m)^\\s*(?:\\d{1,2}[.、)]|（\\d{1,2}）|\\(\\d{1,2}\\)|一、|二、|三、|四、)")
        val matches = pattern.findAll(text).toList()
        if (matches.isEmpty()) return emptyList()

        return matches.mapIndexedNotNull { index, match ->
            val start = match.range.first
            val end = matches.getOrNull(index + 1)?.range?.first ?: text.length
            val chunk = text.substring(start, end).trim()
            if (chunk.length < 4) return@mapIndexedNotNull null
            PracticeItem(index = 0, stem = chunk, answer = "", analysis = "")
        }
    }

    private fun firstText(obj: JsonObject, vararg keys: String): String {
        keys.forEach { key ->
            val value = obj[key]?.jsonPrimitive?.contentOrNull
            if (!value.isNullOrBlank()) return value
        }
        return ""
    }

    private fun extractObject(raw: String): JsonObject? {
        val text = raw.trim()
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching {
            json.parseToJsonElement(text.substring(start, end + 1)).jsonObject
        }.getOrNull()
    }
}
