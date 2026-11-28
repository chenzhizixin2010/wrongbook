package com.chenzixin.wrongbook.data.generate

import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import com.chenzixin.wrongbook.data.local.model.QuestionWithTags
import com.chenzixin.wrongbook.data.remote.ChatMsg
import com.chenzixin.wrongbook.data.remote.LlmClient
import com.chenzixin.wrongbook.data.repository.QuestionRepository
import com.chenzixin.wrongbook.data.repository.SettingsRepository
import com.chenzixin.wrongbook.di.IoDispatcher
import com.chenzixin.wrongbook.domain.generate.Blueprint
import com.chenzixin.wrongbook.domain.generate.BlueprintParser
import com.chenzixin.wrongbook.domain.generate.PracticeItem
import com.chenzixin.wrongbook.domain.generate.PracticeSheet
import com.chenzixin.wrongbook.domain.generate.PracticeSheetHtml
import com.chenzixin.wrongbook.domain.generate.OptionFormatter
import com.chenzixin.wrongbook.domain.generate.QuestionArchetypes
import com.chenzixin.wrongbook.domain.generate.QuestionQuality
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
 * AI 出题走「蓝图 → 出题 → 质检 → 返工」四段式（[generateVariants]）：
 * 先让模型把考点摊开，再按规范出题，本地规则闸门判掉不可作答/重复/无解析的题，
 * 判掉的原因作为「返工单」回传，只补不足的部分。这样出的题比一次性吐 10 道稳得多。
 */
@Singleton
class PracticeRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val questionImageDao: com.chenzixin.wrongbook.data.local.dao.QuestionImageDao,
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
        /** 是否允许模型调用服务端内置的联网检索（不支持会静默降级） */
        val webSearch: Boolean = false,
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
        /** 被质量闸门判掉、没能印出来的题数 */
        val dropped: Int = 0,
    )

    /** AI 出题的结果：过了闸门的题 + 过程说明。 */
    private data class VariantOutcome(
        val items: List<PracticeItem>,
        val dropped: Int,
        val notes: List<String>,
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
                figures = runCatching {
                    questionImageDao.listByQuestion(q.id).map { it.path }
                }.getOrDefault(emptyList()),
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

    /**
     * 真正出卷。
     *
     * [onStage] 用于把「正在分析考点 / 出题 / 质检」这类阶段文案推给 UI，
     * 因为一次 AI 出卷可能要十几秒，只转圈用户会以为卡死。
     */
    suspend fun generate(
        plan: Plan,
        request: Request,
        fillMissing: Boolean,
        onStage: (String) -> Unit = {},
    ): WbResult<Generated> = withContext(io) {
        val warnings = mutableListOf<String>()
        var items = plan.items
        var dropped = 0

        // 1) 按需补答案/解析
        if (fillMissing && plan.needsFill) {
            onStage("补全答案与解析…")
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
            when (val result = generateVariants(plan, request.count, request.webSearch, onStage)) {
                is WbResult.Ok -> {
                    val outcome = result.value
                    // AI 吐出来的选项同样可能挤一行，这里统一再过一遍换行
                    items = outcome.items.map { it.copy(stem = OptionFormatter.wrap(it.stem)) }
                    modeLabel = "AI 生成变式题"
                    dropped = outcome.dropped
                    warnings += outcome.notes
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
                warning = warnings.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.joinToString("\n"),
                dropped = dropped,
            ),
        )
    }

    fun renderHtml(sheet: PracticeSheet): String = PracticeSheetHtml.build(sheet, ::figureDataUri)

    /** 把插图读成 base64 data URI，WebView 不联网也能显示。 */
    private fun figureDataUri(path: String): String? = runCatching {
        val file = java.io.File(context.filesDir, path)
        if (!file.exists()) return@runCatching null
        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            ?: return@runCatching null
        val bytes = com.chenzixin.wrongbook.core.ImageUtils.run { bitmap.toJpegBytes(900, 82) }
        bitmap.recycle()
        "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }.getOrNull()

    /** 供 Word 导出：JPEG 字节 + 像素尺寸。 */
    fun figureJpeg(path: String): com.chenzixin.wrongbook.domain.generate.DocxWriter.Figure? = runCatching {
        val file = java.io.File(context.filesDir, path)
        if (!file.exists()) return@runCatching null
        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            ?: return@runCatching null
        val width = bitmap.width
        val height = bitmap.height
        val bytes = com.chenzixin.wrongbook.core.ImageUtils.run { bitmap.toJpegBytes(900, 82) }
        bitmap.recycle()
        com.chenzixin.wrongbook.domain.generate.DocxWriter.Figure(bytes, width, height)
    }.getOrNull()

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

    private fun stemOf(item: QuestionWithTags): String =
        (item.question.refinedText ?: item.question.rawOcrText).orEmpty().trim()

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

        val raw = ask(active.profile, active.apiKey, prompt).text ?: return null
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

    /**
     * 出变式题。
     *
     * 循环结构：每轮最多要 8 道 → 过质量闸门 → 不够就带着「返工单」再来一轮。
     * 只要有一道过了闸门就返回 Ok，一道都没有才返回 Err（让 UI 退回整理错题）。
     */
    private suspend fun generateVariants(
        plan: Plan,
        count: Int,
        webSearch: Boolean,
        onStage: (String) -> Unit,
    ): WbResult<VariantOutcome> {
        val active = settings.resolveActive()
            ?: return WbResult.Err("没有可用的 API 配置（Key 可能没保存），已退回整理错题")

        val usable = plan.sources.filter { stemOf(it).isNotBlank() }
        if (usable.isEmpty()) {
            return WbResult.Err("选中的题目没有文字内容，AI 无法据此出题")
        }

        val sourceStems = usable.take(MAX_SOURCES).map { stemOf(it).take(320) }
        val subjects = collectSubjects(usable)
        val notes = mutableListOf<String>()

        // 第一段：先摊开考点，出题时逐条对齐
        onStage("分析考点与题型…")
        val blueprint = buildBlueprint(active.profile, active.apiKey, sourceStems, subjects)
        if (blueprint != null && blueprint.topic.isNotBlank()) {
            notes += "考点：${blueprint.topic}"
        }

        val accepted = mutableListOf<PracticeItem>()
        var rejectedCount = 0
        var complaints = emptyList<String>()
        var lastFailure: String? = null
        val maxCalls = ((count + 5) / 6 + 1).coerceIn(2, 5)

        for (call in 1..maxCalls) {
            val remaining = count - accepted.size
            if (remaining <= 0) break
            val want = minOf(remaining + 2, MAX_BATCH)

            onStage(if (call == 1) "按考点出题…" else "返工重出 $remaining 道…")
            val prompt = buildQuestionPrompt(
                blueprint = blueprint,
                subjects = subjects,
                sources = sourceStems,
                want = want,
                avoid = accepted.map { it.stem },
                complaints = complaints,
            )
            // 联网检索只在第一轮尝试：能搜到真实题目最好，搜不到也别把后面几轮拖慢
            val completion = ask(
                active.profile,
                active.apiKey,
                prompt,
                webSearch = webSearch && call == 1,
            )
            if (completion.downgraded) notes += "这个模型不支持联网检索，已改用本地题型库"
            val raw = completion.text ?: run {
                lastFailure = "AI 出题请求失败（超时或网络错误）"
                break
            }

            val parsed = VariantParser.parse(raw)
            if (parsed.isEmpty()) {
                lastFailure = "AI 返回的内容无法解析成题目"
                break
            }

            val outcome = QuestionQuality.filter(
                items = parsed,
                sources = sourceStems,
                needed = remaining,
            )
            accepted += outcome.accepted
            rejectedCount += outcome.rejectedCount
            complaints = outcome.rejected.take(6).map { (item, hints) ->
                "「${item.stem.take(30)}…」—— ${hints.joinToString("；")}"
            }
        }

        if (accepted.isEmpty()) {
            val reason = lastFailure ?: "AI 出的题都没通过质量检查（缺答案/缺解析/依赖插图/重复）"
            return WbResult.Err("$reason，已退回整理错题")
        }

        if (accepted.size < count) {
            notes += "只出了 ${accepted.size} 道合格的题（要求 $count 道）"
        }
        if (rejectedCount > 0) {
            notes += buildString {
                append("质检刷掉 ").append(rejectedCount).append(" 道")
                val sample = complaints.firstOrNull()?.substringAfter("—— ")?.takeIf { it.isNotBlank() }
                if (sample != null) append("（例如：").append(sample).append("）")
            }
        }
        val trimmed = accepted.take(count)
        return WbResult.Ok(
            VariantOutcome(
                items = trimmed,
                dropped = rejectedCount,
                notes = notes,
            ),
        )
    }

    /** 第一段：考点蓝图；失败就当没有，不影响出题。 */
    private suspend fun buildBlueprint(
        profile: ApiProfileEntity,
        apiKey: String,
        sources: List<String>,
        subjects: List<String>,
    ): Blueprint? {
        val prompt = listOf(
            ChatMsg.system(
                "你是教研组长。读下面的错题，提炼它们的学科、核心考点、难度、题型和要考的易错点。" +
                    "只输出 JSON，不要解释，格式：" +
                    """{"subject":"学科","topic":"核心考点","difficulty":"易/中/难","types":["选择题"],"traps":["易错点"]}""",
            ),
            ChatMsg.user(
                buildString {
                    if (subjects.isNotEmpty()) appendLine("已知学科线索：${subjects.take(6).joinToString("、")}")
                    appendLine("错题：")
                    sources.forEachIndexed { i, s -> appendLine("${i + 1}. $s") }
                },
            ),
        )
        val raw = ask(profile, apiKey, prompt).text ?: return null
        return BlueprintParser.parse(raw)
    }

    /** 第二段：出题提示词。规范和参考题放在一起，模型才不会自由发挥。 */
    private fun buildQuestionPrompt(
        blueprint: Blueprint?,
        subjects: List<String>,
        sources: List<String>,
        want: Int,
        avoid: List<String>,
        complaints: List<String>,
    ): List<ChatMsg> {
        val system = buildString {
            appendLine("你是资深命题老师，负责给错题出「同类型、同难度」的变式练习题。")
            appendLine(QuestionArchetypes.promptBlock(subjects, blueprint?.difficulty.orEmpty()))
            appendLine("【输出格式】只输出 JSON，不要解释，不要 Markdown 代码块：")
            appendLine(
                """{"questions":[{"stem":"题干（选择题把 A—D 每个选项各写一行）",""" +
                    """"answer":"答案","analysis":"解析","type":"选择题/填空题/计算题",""" +
                    """"difficulty":"易/中/难","point":"考点"}]}""",
            )
            appendLine("【题量】必须正好 $want 道；宁可少出一道，也不许凑数或重复。")
        }
        val user = buildString {
            if (blueprint != null && !blueprint.isEmpty) {
                appendLine("【出题蓝图】")
                appendLine(blueprint.promptLine())
            }
            appendLine("【参考错题（照它们的考点和难度出，不要照抄）】")
            sources.forEachIndexed { i, s -> appendLine("${i + 1}. $s") }
            if (avoid.isNotEmpty()) {
                appendLine("【本次已经出过的题，换不同情境和数字】")
                avoid.take(10).forEach { appendLine("- ${it.take(40)}") }
            }
            if (complaints.isNotEmpty()) {
                appendLine("【上一轮这些题被判不合格，别再犯】")
                complaints.forEach { appendLine("- $it") }
            }
            appendLine("请严格按规范出 $want 道题。")
        }
        return listOf(ChatMsg.system(system), ChatMsg.user(user))
    }

    /** 从已选错题的标签和文本里收集学科线索（给题型库和出题规范用）。 */
    private fun collectSubjects(sources: List<QuestionWithTags>): List<String> = buildList {
        sources.forEach { item ->
            addAll(item.tags.map { it.name })
            item.question.subject?.takeIf { it.isNotBlank() }?.let { add(it) }
            QuestionArchetypes.detectSubjectFromText(stemOf(item))?.let { add(it) }
        }
    }.distinct().take(8)

    private data class Completion(val text: String?, val downgraded: Boolean)

    /**
     * 发请求并尽量拿到 JSON：JSON 模式优先，失败再普通模式；
     * 开了联网检索却拿不到结果时，去掉工具重试并标记「已降级」。
     */
    private suspend fun ask(
        profile: ApiProfileEntity,
        apiKey: String,
        prompt: List<ChatMsg>,
        webSearch: Boolean = false,
    ): Completion {
        val budget = profile.maxTokens
        if (webSearch) {
            val withTools = llm.complete(
                profile = profile,
                apiKey = apiKey,
                messages = prompt,
                jsonMode = true,
                maxTokens = budget,
                webSearch = true,
            )
            if (withTools is WbResult.Ok && withTools.value.contains('{')) {
                return Completion(withTools.value, downgraded = false)
            }
            val plainJson = llm.complete(profile, apiKey, prompt, jsonMode = true, maxTokens = budget)
            if (plainJson is WbResult.Ok && plainJson.value.contains('{')) {
                return Completion(plainJson.value, downgraded = true)
            }
            val plain = llm.complete(profile, apiKey, prompt, jsonMode = false, maxTokens = budget)
            return Completion((plain as? WbResult.Ok)?.value, downgraded = true)
        }

        val first = llm.complete(profile, apiKey, prompt, jsonMode = true, maxTokens = budget)
        if (first is WbResult.Ok && first.value.contains('{')) {
            return Completion(first.value, downgraded = false)
        }
        val second = llm.complete(profile, apiKey, prompt, jsonMode = false, maxTokens = budget)
        return Completion((second as? WbResult.Ok)?.value, downgraded = false)
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

        /** 单次请求最多要多少道：太多容易把输出撑爆到 max_tokens 之外。 */
        const val MAX_BATCH = 8

        /** 参考题最多塞几道进提示词，防止 prompt 过长。 */
        const val MAX_SOURCES = 5
    }
}
