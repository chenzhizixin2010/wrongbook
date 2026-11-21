package com.chenzixin.wrongbook.data.ocr

import android.graphics.Bitmap
import com.chenzixin.wrongbook.core.ImageUtils.toBase64Jpeg
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import com.chenzixin.wrongbook.data.remote.ChatMsg
import com.chenzixin.wrongbook.data.remote.LlmClient
import com.chenzixin.wrongbook.di.IoDispatcher
import com.chenzixin.wrongbook.domain.scan.QuestionRegion
import com.chenzixin.wrongbook.domain.scan.VisionRegionParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * 视觉模型切分（可选能力）。
 *
 * 只有配置了 visionModel 才会调用；返回 null 表示「不可用 / 解析失败」，
 * 调用方必须降级到本地 ML Kit OCR —— 这是三段式容错的第二层。
 */
@Singleton
class VisionQuestionSegmenter @Inject constructor(
    private val llm: LlmClient,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    suspend fun segment(
        profile: ApiProfileEntity,
        apiKey: String,
        bitmap: Bitmap,
    ): List<QuestionRegion>? = withContext(io) {
        val base64 = bitmap.toBase64Jpeg(maxSide = 1400, quality = 80)
        val visionProfile = profile.copy(chatModel = profile.visionModel.ifBlank { profile.chatModel })
        val prompt = """
            你是试卷题目切分助手。请找出这张图片中每一道独立的题目（不含页眉页脚、页码、分数标注）。
            只输出 JSON，不要解释，格式：
            {"questions":[{"index":12,"label":"12.","bbox":[0.05,0.10,0.95,0.30],"text":"该题的文字"}]}
            bbox 为归一化坐标 [左,上,右,下]，取值 0~1；题号识别不到时 index 填 -1、label 填空字符串。
        """.trimIndent()

        when (
            val result = llm.complete(
                profile = visionProfile,
                apiKey = apiKey,
                messages = listOf(ChatMsg.user(prompt, images = listOf(base64))),
                jsonMode = true,
                maxTokens = 2000,
            )
        ) {
            is WbResult.Ok -> VisionRegionParser.parse(result.value, bitmap.width, bitmap.height)
            is WbResult.Err -> null
        }
    }
}
