package com.chenzixin.wrongbook.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * SSE 行解析 + OpenAI 兼容响应字段提取。
 *
 * 抽成纯对象是为了能在 JVM 上单测：跨包断行、[DONE]、错误帧、脏数据都在测试里覆盖。
 */
object SseParser {

    sealed interface Event {
        data class Chunk(val json: String) : Event
        data object Done : Event
    }

    /**
     * @return null 表示这一行可以忽略（空行、`event:`/`id:` 等）
     */
    fun parse(line: String): Event? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        if (!trimmed.startsWith("data:")) return null
        val payload = trimmed.removePrefix("data:").trim()
        if (payload.isEmpty()) return null
        if (payload == "[DONE]") return Event.Done
        return Event.Chunk(payload)
    }
}

object ChatJson {

    /** 取 choices[0].finish_reason，没有就返回空串。 */
    fun finishReasonOf(raw: String): String {
        val root = parseObject(raw) ?: return ""
        val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return ""
        return choice["finish_reason"]?.jsonPrimitive?.contentOrNull.orEmpty()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** 取 delta.content（流式）或 message.content（非流式）。 */
    fun contentOf(raw: String): String? {
        val root = parseObject(raw) ?: return null
        val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        val fromDelta = choice["delta"]?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
        if (!fromDelta.isNullOrEmpty()) return fromDelta
        return choice["message"]?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
    }

    /** 取 delta.reasoning_content（推理模型的思考过程）。 */
    fun reasoningOf(raw: String): String? {
        val root = parseObject(raw) ?: return null
        val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        return choice["delta"]?.jsonObject?.get("reasoning_content")?.jsonPrimitive?.contentOrNull
    }

    /** 取 error.message。 */
    fun errorMessageOf(raw: String): String? {
        val root = parseObject(raw) ?: return null
        val err = root["error"] ?: return null
        return err.jsonObject["message"]?.jsonPrimitive?.contentOrNull
    }

    /** 取 usage.total_tokens（拿不到返回 null）。 */
    fun totalTokensOf(raw: String): Int? {
        val root = parseObject(raw) ?: return null
        return root["usage"]?.jsonObject?.get("total_tokens")?.jsonPrimitive?.contentOrNull?.toIntOrNull()
    }

    private fun parseObject(raw: String): JsonObject? =
        try {
            json.parseToJsonElement(raw).jsonObject
        } catch (t: Throwable) {
            null
        }
}
