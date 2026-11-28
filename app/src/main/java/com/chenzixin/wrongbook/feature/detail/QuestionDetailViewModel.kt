package com.chenzixin.wrongbook.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.QuestionImageStore
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.model.QuestionWithTags
import com.chenzixin.wrongbook.data.remote.ChatMsg
import com.chenzixin.wrongbook.data.remote.LlmClient
import com.chenzixin.wrongbook.data.remote.LlmErrors
import com.chenzixin.wrongbook.data.repository.ChatRepository
import com.chenzixin.wrongbook.data.repository.QuestionRepository
import com.chenzixin.wrongbook.data.repository.SettingsRepository
import com.chenzixin.wrongbook.domain.ai.RefineParser
import com.chenzixin.wrongbook.domain.ai.RefinedQuestion
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class QuestionDetailViewModel @Inject constructor(
    private val repo: QuestionRepository,
    private val chatRepo: ChatRepository,
    private val settings: SettingsRepository,
    private val llm: LlmClient,
    private val imageStore: QuestionImageStore,
    savedStateHandle: SavedStateHandle,
    private val tagRepo: com.chenzixin.wrongbook.data.repository.TagRepository,
    private val imageRepo: com.chenzixin.wrongbook.data.repository.QuestionImageRepository,
    private val ocr: com.chenzixin.wrongbook.data.ocr.OcrEngine,
    private val orienter: com.chenzixin.wrongbook.data.ocr.OcrOrienter,
) : ViewModel() {

    private val questionId: Long = savedStateHandle.get<Long>("questionId") ?: 0L

    val item: StateFlow<QuestionWithTags?> = repo.observeById(questionId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    /** 点了「AI 讲解」后创建出来的会话 id，界面据此跳转。 */
    private val _aiSessionId = MutableStateFlow<Long?>(null)
    val aiSessionId: StateFlow<Long?> = _aiSessionId.asStateFlow()

    data class AiState(
        val loading: Boolean = false,
        val refined: RefinedQuestion? = null,
        val variants: List<RefinedQuestion> = emptyList(),
    )

    /** 题目插图（几何图等），与题面主图分开 */
    val figures: kotlinx.coroutines.flow.StateFlow<List<com.chenzixin.wrongbook.data.local.entity.QuestionImageEntity>> =
        imageRepo.observe(questionId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** 答案扫描的识别结果，供用户确认后写入 */
    private val _answerScan = MutableStateFlow<com.chenzixin.wrongbook.domain.qa.AnswerText?>(null)
    val answerScan: kotlinx.coroutines.flow.StateFlow<com.chenzixin.wrongbook.domain.qa.AnswerText?> =
        _answerScan.asStateFlow()

    private val _scanningAnswer = MutableStateFlow(false)
    val scanningAnswer: kotlinx.coroutines.flow.StateFlow<Boolean> = _scanningAnswer.asStateFlow()

    private val _ai = MutableStateFlow(AiState())
    val ai: StateFlow<AiState> = _ai.asStateFlow()

    enum class AskAiKind(val title: String) {
        EXPLAIN("AI 讲解"),
        REFINE("AI 整理"),
    }

    fun consumeAiSession() {
        _aiSessionId.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    fun dismissAi() {
        _ai.value = AiState()
    }

    fun delete() {
        val question = item.value?.question ?: return
        viewModelScope.launch {
            when (repo.delete(question.id)) {
                is WbResult.Ok -> {
                    // 先删库再删文件：文件删失败不影响数据一致性
                    imageStore.delete(listOf(question.imagePath, question.thumbPath))
                    _deleted.value = true
                }
                is WbResult.Err -> _message.value = "删除失败，请重试"
            }
        }
    }

    fun askAi(kind: AskAiKind) {
        val question = item.value?.question ?: return
        // 优先用整理后的题干，识别错字少一些；没有整理过才退回 OCR 原文
        val stem = (question.refinedText ?: question.rawOcrText).trim()
        val prompt = when (kind) {
            AskAiKind.EXPLAIN -> "请讲解这道题，并给出答案与解析：\n$stem"
            AskAiKind.REFINE -> "请把下面这道题整理成结构化格式（题干/答案/解析/知识点）：\n$stem"
        }
        viewModelScope.launch {
            when (val created = chatRepo.createSession(title = kind.title)) {
                is WbResult.Ok -> {
                    chatRepo.appendMessage(created.value, "user", prompt)
                    _aiSessionId.value = created.value
                }
                is WbResult.Err -> _message.value = created.message
            }
        }
    }

    /** AI 整理：结构化回填题干/答案/解析/学科，失败时用原文兜底。 */
    fun refine() {
        val question = item.value?.question ?: return
        if (_ai.value.loading) return
        viewModelScope.launch {
            val active = settings.resolveActive()
            if (active == null) {
                _message.value = LlmErrors.NO_KEY
                return@launch
            }
            _ai.update { it.copy(loading = true) }
            val prompt = listOf(
                ChatMsg.system(
                    "你是题目结构化助手。只输出 JSON，不要任何解释。字段：" +
                        "stem(题干)、answer(答案)、analysis(解析)、subject(学科)、knowledgePoints(字符串数组)。",
                ),
                ChatMsg.user("题目原文：\n${question.rawOcrText}"),
            )
            val result = llm.complete(active.profile, active.apiKey, prompt, jsonMode = true, maxTokens = 1200)
            val refined = when (result) {
                is WbResult.Ok -> RefineParser.parse(result.value)
                is WbResult.Err -> null
            }
            if (refined == null) {
                // 兜底：原文当题干，用户仍可手工补答案
                _ai.update {
                    AiState(refined = RefinedQuestion(stem = question.rawOcrText))
                }
                _message.value = "模型返回无法解析，已用原文作为题干，请手动补全"
            } else {
                _ai.update { AiState(refined = refined) }
            }
        }
    }

    /** 手动编辑题干：OCR 再差也有兜底，用户改完立即生效。 */

    /** 全部标签，编辑弹窗里用。 */
    val allTags: kotlinx.coroutines.flow.StateFlow<List<com.chenzixin.wrongbook.data.local.entity.TagEntity>> =
        tagRepo.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** 勾选/取消勾选标签，立即写库（不做「确定」暂存，避免用户以为没生效）。 */
    fun toggleTag(tagId: Long) {
        val current = item.value?.tags?.map { it.id }.orEmpty().toMutableList()
        if (tagId in current) current.remove(tagId) else current.add(tagId)
        viewModelScope.launch {
            when (val r = repo.setTags(questionId, current)) {
                is WbResult.Ok -> Unit
                is WbResult.Err -> _message.value = r.message
            }
        }
    }

    /** 现场建标签并直接挂到这道题上。 */
    fun createTag(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            when (val r = tagRepo.create(name = name.trim())) {
                is WbResult.Ok -> {
                    val current = item.value?.tags?.map { it.id }.orEmpty() + r.value
                    repo.setTags(questionId, current)
                }
                is WbResult.Err -> _message.value = r.message
            }
        }
    }

    fun saveText(text: String) {
        val question = item.value?.question ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            when (repo.update(question.copy(refinedText = text.trim()))) {
                is WbResult.Ok -> _message.value = "已保存"
                is WbResult.Err -> _message.value = "保存失败，请重试"
            }
        }
    }

    fun saveRefined(refined: RefinedQuestion) {
        val question = item.value?.question ?: return
        viewModelScope.launch {
            val updated = question.copy(
                refinedText = refined.stem,
                answer = refined.answer.ifBlank { question.answer },
                analysis = refined.analysis.ifBlank { question.analysis },
                subject = refined.subject.ifBlank { question.subject },
            )
            when (repo.update(updated)) {
                is WbResult.Ok -> {
                    _ai.value = AiState()
                    _message.value = "已保存整理结果"
                }
                is WbResult.Err -> _message.value = "保存失败，请重试"
            }
        }
    }

    /** 生成同类题：1~3 道变式，用户确认后入库。 */
    fun generateVariants() {
        val question = item.value?.question ?: return
        if (_ai.value.loading) return
        viewModelScope.launch {
            val active = settings.resolveActive()
            if (active == null) {
                _message.value = LlmErrors.NO_KEY
                return@launch
            }
            _ai.update { it.copy(loading = true) }
            val prompt = listOf(
                ChatMsg.system(
                    "你是出题老师。基于给定错题生成 2 道同类变式题，只输出 JSON：" +
                        """{"questions":[{"stem":"","answer":"","analysis":"","knowledgePoints":[""]}]}""",
                ),
                ChatMsg.user("原题：\n${question.refinedText ?: question.rawOcrText}"),
            )
            val result = llm.complete(active.profile, active.apiKey, prompt, jsonMode = true, maxTokens = 1500)
            val variants = when (result) {
                is WbResult.Ok -> RefineParser.parseVariants(result.value)
                is WbResult.Err -> emptyList()
            }
            if (variants.isEmpty()) {
                _ai.value = AiState()
                _message.value = "没有生成出可用的变式题，稍后再试"
            } else {
                _ai.value = AiState(variants = variants)
            }
        }
    }

    fun saveVariants(variants: List<RefinedQuestion>) {
        viewModelScope.launch {
            var saved = 0
            variants.forEach { v ->
                val entity = QuestionEntity(
                    imagePath = "",
                    thumbPath = "",
                    rawOcrText = v.stem,
                    refinedText = v.stem,
                    answer = v.answer,
                    analysis = v.analysis,
                    subject = v.subject,
                    source = "AI_CHAT",
                )
                if (repo.add(entity) is WbResult.Ok) saved++
            }
            _ai.value = AiState()
            _message.value = if (saved == 0) "保存失败" else "已保存 $saved 道变式题"
        }
    }

    // ---------------------------------------------------------------- 插图

    fun addFigure(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            when (val r = imageRepo.add(questionId, bitmap)) {
                is WbResult.Ok -> _message.value = "已添加插图"
                is WbResult.Err -> _message.value = r.message
            }
        }
    }

    fun deleteFigure(image: com.chenzixin.wrongbook.data.local.entity.QuestionImageEntity) {
        viewModelScope.launch {
            when (val r = imageRepo.delete(image)) {
                is WbResult.Ok -> _message.value = "已删除插图"
                is WbResult.Err -> _message.value = r.message
            }
        }
    }

    fun restoreFigureHd(image: com.chenzixin.wrongbook.data.local.entity.QuestionImageEntity) {
        viewModelScope.launch {
            _message.value = "正在重绘…"
            // 超分要几秒，阶段文案直接写进同一个弹窗，不会反复弹
            when (val r = imageRepo.restoreHd(image) { stage -> _message.value = stage }) {
                is WbResult.Ok -> _message.value = "已生成高清图"
                is WbResult.Err -> _message.value = r.message
            }
        }
    }

    // ---------------------------------------------------------------- 答案扫描

    /** OCR 识别答案照片，拆成答案与解析后交给用户确认。 */
    fun scanAnswer(bitmap: android.graphics.Bitmap) {
        if (_scanningAnswer.value) return
        _scanningAnswer.value = true
        viewModelScope.launch {
            // 答案页经常是横着拍的，先摆正再 OCR，否则竖排识别率极低
            val upright = runCatching { orienter.upright(bitmap) }.getOrDefault(bitmap)
            val result = runCatching { ocr.recognize(upright) }
            _scanningAnswer.value = false
            val text = result.getOrNull()?.lines?.joinToString("\n") { it.text }.orEmpty()
            if (text.isBlank()) {
                _message.value = "没识别到文字，换个角度或光线再试"
                return@launch
            }
            _answerScan.value = com.chenzixin.wrongbook.domain.qa.AnswerParser.parse(text)
        }
    }

    fun dismissAnswerScan() {
        _answerScan.value = null
    }

    /** 用户确认（可编辑）后写库。 */
    fun saveScannedAnswer(answer: String, analysis: String) {
        val question = item.value?.question ?: return
        _answerScan.value = null
        viewModelScope.launch {
            when (repo.update(
                question.copy(
                    answer = answer.trim().ifBlank { null },
                    analysis = analysis.trim().ifBlank { null },
                ),
            )) {
                is WbResult.Ok -> _message.value = "已写入答案与解析"
                is WbResult.Err -> _message.value = "保存失败，请重试"
            }
        }
    }
}
