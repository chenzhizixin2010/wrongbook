package com.chenzixin.wrongbook.domain.scan

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 解析视觉模型返回的题目切分结果。
 *
 * 约定模型输出：
 * ```json
 * {"questions":[{"index":1,"label":"1.","bbox":[0.05,0.10,0.95,0.28],"text":"..."}]}
 * ```
 * bbox 期望是归一化 0..1；但模型经常直接给像素坐标，这里做兼容：
 * 只要出现 > 1.5 的值，就按像素处理，用图片宽高归一化。
 *
 * 解析失败返回 null，调用方自动降级到本地 OCR（三段式容错的第一层）。
 */
object VisionRegionParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(raw: String, imageWidth: Int, imageHeight: Int): List<QuestionRegion>? {
        if (imageWidth <= 0 || imageHeight <= 0) return null
        val array = extractQuestions(raw) ?: return null

        val regions = array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val bbox = obj["bbox"]?.jsonArray ?: return@mapNotNull null
            if (bbox.size < 4) return@mapNotNull null
            val values = bbox.mapNotNull { it.jsonPrimitive.contentOrNull?.toFloatOrNull() }
            if (values.size < 4) return@mapNotNull null

            val pixelLike = values.take(4).any { it > 1.5f }
            val scaleX = if (pixelLike) imageWidth.toFloat() else 1f
            val scaleY = if (pixelLike) imageHeight.toFloat() else 1f

            val rect = NormRect(
                left = values[0] / scaleX,
                top = values[1] / scaleY,
                right = values[2] / scaleX,
                bottom = values[3] / scaleY,
            ).clampToUnit()

            if (rect.right - rect.left < 0.01f || rect.bottom - rect.top < 0.005f) return@mapNotNull null

            val text = obj["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
            QuestionRegion(
                index = obj["index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: -1,
                label = obj["label"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                rect = rect,
                text = text,
                confidence = 0.85f,
                isSectionHeader = false,
            )
        }

        return regions.takeIf { it.isNotEmpty() }
    }

    private fun extractQuestions(raw: String): JsonArray? {
        val text = stripFences(raw)
        // 优先取 {"questions":[...]}
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start >= 0 && end > start) {
            val obj = runCatching {
                json.parseToJsonElement(text.substring(start, end + 1)).jsonObject
            }.getOrNull()
            val questions = obj?.get("questions") as? JsonArray
            if (questions != null) return questions
        }
        // 其次兼容裸数组
        val aStart = text.indexOf('[')
        val aEnd = text.lastIndexOf(']')
        if (aStart >= 0 && aEnd > aStart) {
            return runCatching {
                json.parseToJsonElement(text.substring(aStart, aEnd + 1)).jsonArray
            }.getOrNull()
        }
        return null
    }

    private fun stripFences(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.contains("```")) return trimmed
        val fenceStart = trimmed.indexOf("```")
        val afterLang = trimmed.indexOf('\n', fenceStart).let { if (it < 0) fenceStart + 3 else it + 1 }
        val fenceEnd = trimmed.lastIndexOf("```")
        return if (fenceEnd > afterLang) trimmed.substring(afterLang, fenceEnd) else trimmed
    }
}
