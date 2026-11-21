package com.chenzixin.wrongbook.data.generate

import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.model.QuestionWithTags
import com.chenzixin.wrongbook.data.remote.ChatMsg
import com.chenzixin.wrongbook.data.remote.LlmClient
import com.chenzixin.wrongbook.data.repository.QuestionRepository
import com.chenzixin.wrongbook.data.repository.SettingsRepository
import com.chenzixin.wrongbook.di.IoDispatcher
import com.chenzixin.wrongbook.domain.generate.PracticeItem
import com.chenzixin.wrongbook.domain.generate.PracticeSheet
import com.chenzixin.wrongbook.domain.generate.PracticeSheetHtml
import com.chenzixin.wrongbook.domain.tag.TagFilter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 生成练习卷：
 * 1. 按标签筛出错题（AND / OR，与题库筛选同一套语义）；
 * 2. 两种出题方式：
 *    - 整理：直接从错题里抽，优先挑掌握度低的；
 *    - AI 变式：把抽到的错题交给模型，生成同类题（失败自动退回「整理」）。
 * 3. 输出可打印 HTML。
 */
@Singleton
class PracticeRepository @Inject constructor(
    private val questionRepo: QuestionRepository,
    private val settings: SettingsRepository,
    private val llm: LlmClient,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class Request(
        val filter: TagFilter,
        val count: Int,
        val useAi: Boolean,
        val withAnswers: Boolean,
        val title: String = "错题巩固练习",
    )

    suspend fun buildSheet(request: Request): WbResult<PracticeSheet> = withContext(io) {
        val all = runCatching { questionRepo.observeAll().first() }.getOrDefault(emptyList())
        if (all.isEmpty()) return@withContext WbResult.Err("题库还是空的，先扫描几道错题")

        val picked = pickQuestions(all, request.filter, request.count)
        if (picked.isEmpty()) {
            return@withContext WbResult.Err("这个标签组合下没有题目，换个标签试试")
        }

        var items = picked.mapIndexed { index, item ->
            val q = item.question
            PracticeItem(
                index = index + 1,
                stem = (q.refinedText ?: q.rawOcrText).trim(),
                answer = q.answer.orEmpty(),
                analysis = q.analysis.orEmpty(),
            )
        }
        var modeLabel = "错题整理"

        if (request.useAi) {
            val generated = generateWithAi(picked, request.count)
            if (generated != null && generated.isNotEmpty()) {
                items = generated.mapIndexed { index, item -> item.copy(index = index + 1) }
                modeLabel = "AI 生成变式题"
            } else {
                modeLabel = "错题整理（AI 不可用，已回退）"
            }
        }

        val tagNames = all
            .flatMap { it.tags }
            .distinctBy { it.id }
            .filter { it.id in request.filter.tagIds }
            .joinToString("、") { it.name }
            .ifBlank { "全部题目" }

        val subtitle = buildString {
            append("标签：").append(tagNames)
            append(" ｜ 方式：").append(modeLabel)
            append(" ｜ 题量：").append(items.size)
        }

        WbResult.Ok(
            PracticeSheet(
                title = request.title,
                subtitle = subtitle,
                items = items,
                withAnswers = request.withAnswers,
            ),
        )
    }

    /** 掌握度低的优先，其次时间新的优先；题目不够就有多少给多少。 */
    private fun pickQuestions(
        all: List<QuestionWithTags>,
        filter: TagFilter,
        count: Int,
    ): List<QuestionWithTags> = all
        .filter { filter.isEmpty || filter.matches(it.tags.map { t -> t.id }) }
        .sortedWith(compareBy({ it.question.masteryLevel }, { -it.question.createdAt }))
        .take(count)

    private suspend fun generateWithAi(
        source: List<QuestionWithTags>,
        count: Int,
    ): List<PracticeItem>? {
        val active = settings.resolveActive() ?: return null
        val materials = source.take(5).joinToString("\n\n") { item ->
            val q = item.question
            "题目：${(q.refinedText ?: q.rawOcrText).take(300)}\n答案：${q.answer.orEmpty().take(200)}"
        }
        val prompt = listOf(
            ChatMsg.system(
                "你是出题老师。根据给定错题生成 $count 道同类型、难度相近的变式题。" +
                    "只输出 JSON，不要解释，格式：" +
                    """{"questions":[{"stem":"题干","answer":"答案","analysis":"解析"}]}""",
            ),
            ChatMsg.user("参考错题：\n$materials"),
        )
        val result = llm.complete(active.profile, active.apiKey, prompt, jsonMode = true, maxTokens = 2500)
        if (result !is WbResult.Ok) return null

        return parseVariants(result.value)
    }

    /** 解析变式题；复用宽松提取（容忍代码块、前后废话），失败返回 null 触发回退。 */
    private fun parseVariants(raw: String): List<PracticeItem>? {
        val text = raw.trim()
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        val payload = if (start >= 0 && end > start) text.substring(start, end + 1) else text
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return null
        val array = root["questions"]?.jsonArray ?: return null
        val items = array.mapNotNull { element ->
            val obj = element.jsonObject
            val stem = obj["stem"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (stem.isEmpty()) return@mapNotNull null
            PracticeItem(
                index = 0,
                stem = stem,
                answer = obj["answer"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                analysis = obj["analysis"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            )
        }
        return items.takeIf { it.isNotEmpty() }
    }

    fun renderHtml(sheet: PracticeSheet): String = PracticeSheetHtml.build(sheet)
}
