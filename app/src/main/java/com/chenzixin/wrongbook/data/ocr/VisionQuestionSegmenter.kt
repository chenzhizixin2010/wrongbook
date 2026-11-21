package com.chenzixin.wrongbook.data.ocr

import android.graphics.Bitmap
import com.chenzixin.wrongbook.core.ImageUtils.toBase64Jpeg
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import com.chenzixin.wrongbook.data.remote.ChatMsg
import com.chenzixin.wrongbook.data.remote.LlmClient
import com.chenzixin.wrongbook.data.remote.LlmErrors
import com.chenzixin.wrongbook.di.IoDispatcher
import com.chenzixin.wrongbook.domain.scan.QuestionRegion
import com.chenzixin.wrongbook.domain.scan.VisionRegionParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * 视觉模型切题。
 *
 * 返回 WbResult 而不是「成功 or null」——失败原因要能显示给用户，
 * 否则用户只会看到「AI 没生效、识别很烂」，却不知道是超时、限流还是解析失败。
 *
 * 容错顺序：
 * 1. JSON 模式请求（maxTokens 放大到 4000，整页题目才装得下）；
 * 2. 解析失败/请求失败时，再退回普通模式重试一次（有些视觉模型对 response_format 支持不好）；
 * 3. 仍失败 → 把原因交给调用方，由它降级到本地 OCR 并提示。
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
    ): WbResult<List<QuestionRegion>> = withContext(io) {
        val base64 = bitmap.toBase64Jpeg(maxSide = 1600, quality = 82)
        val visionProfile = profile.copy(chatModel = profile.visionModel.ifBlank { profile.chatModel })
        val prompt = buildPrompt()

        // 第一次：JSON 模式
        val first = llm.complete(
            profile = visionProfile,
            apiKey = apiKey,
            messages = listOf(ChatMsg.user(prompt, images = listOf(base64))),
            jsonMode = true,
            maxTokens = visionProfile.maxTokens.coerceAtLeast(4096),
        )

        when (first) {
            is WbResult.Ok -> {
                VisionRegionParser.parse(first.value, bitmap.width, bitmap.height)
                    ?.let { return@withContext WbResult.Ok(it) }
                // 解析不出来：多半是 JSON 被截断或包了多余文字，换个模式重试
            }
            is WbResult.Err -> {
                // Key 无效 / 地址错 / 限流，重试也没意义，直接返回；
                // 400 往往是 response_format 不被支持，换个模式值得再试一次
                val fatal = listOf("API Key", "404", "401", "403", "429").any { first.message.contains(it) }
                if (fatal) return@withContext first
            }
        }

        // 第二次：普通模式（部分视觉模型在 json_object 下会返回空内容）
        val second = llm.complete(
            profile = visionProfile,
            apiKey = apiKey,
            messages = listOf(ChatMsg.user(prompt, images = listOf(base64))),
            jsonMode = false,
            maxTokens = visionProfile.maxTokens.coerceAtLeast(4096),
        )
        when (second) {
            is WbResult.Ok -> VisionRegionParser.parse(second.value, bitmap.width, bitmap.height)
                ?.let { WbResult.Ok(it) }
                ?: WbResult.Err(LlmErrors.PARSE)
            is WbResult.Err -> second
        }
    }

    private fun buildPrompt(): String = """
        你是试卷题目转录助手。请找出这张图片中每一道独立的题目（不含页眉页脚、页码、分数标注）。

        转录规则（很重要）：
        1. 只转录**印刷体**的题目正文、选项、以及印刷体的「解/答/分析」内容；
        2. **完全忽略手写笔迹**：学生的手写答案、批改、圈画、打勾、叉、箭头、批注一律不要出现在结果里；
        3. 公式用纯文本表达（如 x^2、H2O、ΔH=-74.8 kJ/mol），不要输出 LaTeX 命令；
        4. 保留题号与选项字母；跨行的一句话要合并成一行；
        5. 看不清的字用 □ 占位，不要猜。

        只输出 JSON，不要解释，格式：
        {"questions":[{"index":12,"label":"12.","bbox":[0.05,0.10,0.95,0.30],"text":"该题的文字"}]}
        bbox 为归一化坐标 [左,上,右,下]，取值 0~1；题号识别不到时 index 填 -1、label 填空字符串。
    """.trimIndent()
}
