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
import com.chenzixin.wrongbook.domain.generate.OptionFormatter
import com.chenzixin.wrongbook.domain.generate.VariantParser
import com.chenzixin.wrongbook.domain.tag.TagFilter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 生成练习卷，分两步：
 *
 * 1. [inspect]：只挑题、只看缺什么（答案/解析），让 UI 有机会问用户「要不要 AI 补全」；
 * 2. [generate]：真正出卷 —— 按需补全答案、按需生成变式题，最后渲染成可打印 HTML。
 *
 * 为什么拆两步：补全答案是花钱的调用，必须先问用户；
 * 而 AI 出题失败要让用户看到原因，不能默默退回「整理错题」。
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

    /** 选题结果 + 缺口信息。 */
    data class Plan(
        val sources: List<QuestionWithTags>,
        val items: List<PracticeItem>,
        val missingAnswer: Int,
        val missingAnalysis: Int,
        val aiAvailable: Boolean,
    ) {
        val missingCount: Int get() = missingAnswer + missingAnalysis
        val needsFill: Boolean get() = missingAnswer > 0 || missingAnalysis > 0
    }

    data class Generated(
        val sheet: PracticeSheet,
        /** 非空表示有降级或部分失败，需要提示用户 */
        val warning: String? = null,
    )

    suspend fun inspect(request: Request): WbResult<Plan> = withContext(io) {
        val all = runCatching { questionRepo.observeAll().first() }.getOrDefault(emptyList())
        if (all.isEmpty()) return@withContext WbResult.Err("题库还是空的，先扫描几道错题")

        val picked = pickQuestions(all, request.filter, request.count)
        if (picked.isEmpty()) {
            return@withContext WbResult.Err("这个标签组合下没有题目，换个标签试试")
        }

        val items = picked.mapIndexed { index, item ->
            val q = item.question
            PracticeItem(
                index = index + 1,
                // 选择题常见「A. … B. …」挤一行，这里按选项边界断行
                stem = OptionFormatter.wrap((q.refinedText ?: q.rawOcrText).trim()),
                answer = q.answer.orEmpty(),
                analysis = q.analysis.orEmpty(),
            )
        }

        WbResult.Ok(
            Plan(
                sources = picked,
                items = items,
                missingAnswer = items.count { it.answer.isBlank() },
                missingAnalysis = items.count { it.analysis.isBlank() },
                aiAvailable = runCatching { settings.resolveActive() }.getOrNull() != null,
            ),
        )
    }

    suspend fun generate(
        plan: Plan,
        request: Request,
        fillMissing: Boolean,
    ): WbResult<Generated> = withContext(io) {
        val warnings = mutableListOf<String>()
        var items = plan.items

        // 1) 按需补答案/解析
        if (fillMissing && plan.needsFill) {
            val filled = enrich(plan, request)
            if (filled != null) {
                items = filled
            } else {
                warnings += "AI 补全答案失败，卷子上仍有空缺"
            }
        }

        var modeLabel = "错题整理"

        // 2) 按需出变式题
        if (request.useAi) {
            when (val result = generateVariants(plan.sources, request.count)) {
                is WbResult.Ok -> {
                    items = result.value
                    modeLabel = "AI 生成变式题"
                }
                is WbResult.Err -> {
                    warnings += result.message
                    modeLabel = "错题整理（AI 未生效）"
                }
            }
        }

        val renumbered = items.mapIndexed { index, item -> item.copy(index = index + 1) }
        val subtitle = buildString {
            append("标签：").append(subtitleTags(plan, request))
            append(" ｜ 方式：").append(modeLabel)
            append(" ｜ 题量：").append(renumbered.size)
        }

        WbResult.Ok(
            Generated(
                sheet = PracticeSheet(
                    title = request.title,
                    subtitle = subtitle,
                    items = renumbered,
                    withAnswers = request.withAnswers,
                ),
                warning = warnings.takeIf { it.isNotEmpty() }?.joinToString("\n"),
            ),
        )
    }

    fun renderHtml(sheet: PracticeSheet): String = PracticeSheetHtml.build(sheet)

    // ---------------------------------------------------------------- 内部实现

    /** 掌握度低的优先，其次时间新的优先；题目不够就有多少给多少。 */
    private fun pickQuestions(
        all: List<QuestionWithTags>,
        filter: TagFilter,
        count: Int,
    ): List<QuestionWithTags> = all
        .filter { filter.isEmpty || filter.matches(it.tags.map { t -> t.id }) }
        .sortedWith(compareBy({ it.question.masteryLevel }, { -it.question.createdAt }))
        .take(count)

    private fun subtitleTags(plan: Plan, request: Request): String =
        plan.sources
            .flatMap { it.tags }
            .distinctBy { it.id }
            .filter { it.id in request.filter.tagIds }
            .joinToString("、") { it.name }
            .ifBlank { "全部题目" }

    /**
     * 补答案 + 解析：一次请求批量补齐（最多 8 道，控制单次成本），
     * 成功的话顺便写回题库，下次不用再补。
     */
    private suspend fun enrich(plan: Plan, request: Request): List<PracticeItem>? {
        val active = settings.resolveActive() ?: return null
        val targets = plan.sources.take(MAX_ENRICH)
        if (targets.isEmpty()) return null

        val payload = buildJsonArray {
            targets.forEachIndexed { index, item ->
                addJsonObject {
                    put("id", index)
                    val q = item.question
                    put("question", (q.refinedText ?: q.rawOcrText).take(400))
                    if (q.answer.isNullOrBlank()) put("needAnswer", true)
                }
            }
        }.toString()

        val prompt = listOf(
            ChatMsg.system(
                "你是老师。给下面的题目补全「答案」和「解析」（中文，简洁，公式用纯文本）。" +
                    "只输出 JSON，不要解释，格式：" +
                    """{"items":[{"id":0,"answer":"答案","analysis":"解析"}]}""",
            ),
            ChatMsg.user("题目列表：$payload"),
        )

        val raw = completeWithRetry(active.profile, active.apiKey, prompt)
            ?: return null
        val map = parseEnrich(raw) ?: return null

        // 写回题库 + 更新本次 items
        val updated = plan.items.toMutableList()
        targets.forEachIndexed { index, source ->
            val patch = map[index] ?: return@forEachIndexed
            val answer = patch.first
            val analysis = patch.second
            val q = source.question
            runCatching {
                questionRepo.update(
                    q.copy(
                        answer = q.answer?.ifBlank { null } ?: answer,
                        analysis = q.analysis?.ifBlank { null } ?: analysis,
                    ),
                )
            }
            // items 与 sources 同序，直接用序号
            val pos = index
            if (pos in updated.indices) {
                updated[pos] = updated[pos].copy(
                    answer = updated[pos].answer.ifBlank { answer },
                    analysis = updated[pos].analysis.ifBlank { analysis },
                )
            }
        }
        return updated
    }

    private suspend fun generateVariants(
        source: List<QuestionWithTags>,
        count: Int,
    ): WbResult<List<PracticeItem>> {
        val active = settings.resolveActive()
            ?: return WbResult.Err("没有可用的 API 配置（Key 可能没保存），已退回整理错题")

        val usable = source.filter { (it.question.refinedText ?: it.question.rawOcrText).isNotBlank() }
        if (usable.isEmpty()) {
            return WbResult.Err("选中的题目没有文字内容，AI 无法据此出题")
        }
        val materials = usable.take(5).joinToString("\n\n") { item ->
            val q = item.question
            "题目：${(q.refinedText ?: q.rawOcrText).take(300)}\n答案：${q.answer.orEmpty().take(200)}"
        }
        val prompt = listOf(
            ChatMsg.system(
                "你是出题老师。根据给定错题生成 $count 道同类型、难度相近的变式题，" +
                    "每题都要给答案和解析。只输出 JSON，不要解释，格式：" +
                    """{"questions":[{"stem":"题干","answer":"答案","analysis":"解析"}]}""",
            ),
            ChatMsg.user("参考错题：\n$materials"),
        )

        val raw = completeWithRetry(active.profile, active.apiKey, prompt)
            ?: return WbResult.Err("AI 出题请求失败（超时或网络错误），已退回整理错题")
        val parsed = VariantParser.parse(raw)
        if (parsed.isEmpty()) {
            return WbResult.Err(
                "AI 返回的内容无法解析成题目，已退回整理错题。模型原话片段：" +
                    raw.replace("\n", " ").take(140),
            )
        }
        return WbResult.Ok(parsed)
    }

    /** JSON 模式优先；失败再普通模式；都失败返回 null。 */
    private suspend fun completeWithRetry(
        profile: com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity,
        apiKey: String,
        prompt: List<ChatMsg>,
    ): String? {
        val budget = profile.maxTokens
        val first = llm.complete(profile, apiKey, prompt, jsonMode = true, maxTokens = budget)
        if (first is WbResult.Ok) {
            val text = first.value
            if (text.contains('{')) return text
        }
        val second = llm.complete(profile, apiKey, prompt, jsonMode = false, maxTokens = budget)
        return (second as? WbResult.Ok)?.value
    }

    private fun parseEnrich(raw: String): Map<Int, Pair<String, String>>? {
        val root = extractObject(raw) ?: return null
        val array = root["items"]?.jsonArray ?: return null
        val map = mutableMapOf<Int, Pair<String, String>>()
        array.forEach { element ->
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return@forEach
            val answer = obj["answer"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val analysis = obj["analysis"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (answer.isNotBlank() || analysis.isNotBlank()) map[id] = answer to analysis
        }
        return map.takeIf { it.isNotEmpty() }
    }

    private fun extractObject(raw: String): kotlinx.serialization.json.JsonObject? {
        val text = raw.trim()
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching {
            json.parseToJsonElement(text.substring(start, end + 1)).jsonObject
        }.getOrNull()
    }

    private companion object {
        const val MAX_ENRICH = 8
    }
}
