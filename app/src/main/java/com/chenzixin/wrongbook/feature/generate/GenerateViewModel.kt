package com.chenzixin.wrongbook.feature.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.generate.PracticeRepository
import com.chenzixin.wrongbook.data.practice.PracticeSheetStore
import com.chenzixin.wrongbook.domain.generate.DocxWriter
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.data.repository.SettingsRepository
import com.chenzixin.wrongbook.data.repository.TagRepository
import com.chenzixin.wrongbook.domain.tag.TagFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GenerateViewModel @Inject constructor(
    private val practiceRepo: PracticeRepository,
    private val tagRepo: TagRepository,
    private val settings: SettingsRepository,
    private val sheetStore: PracticeSheetStore,
    private val appPrefs: com.chenzixin.wrongbook.data.prefs.AppPrefs,
) : ViewModel() {

    data class UiState(
        val tags: List<TagEntity> = emptyList(),
        val selectedTagIds: Set<Long> = emptySet(),
        val matchAll: Boolean = false,
        val count: Int = 10,
        val withAnswers: Boolean = true,
        val useAi: Boolean = false,
        val webSearch: Boolean = true,
        val generating: Boolean = false,
        /** 生成过程中的阶段文案，例如「按考点出题…」 */
        val stage: String? = null,
        val html: String? = null,
        val sheetTitle: String = "错题巩固练习",
        val message: String? = null,
        /** 待确认「要不要补全答案」的方案 */
        val pendingPlan: PracticeRepository.Plan? = null,
        /** 生成后的提示（AI 失败等） */
        val warning: String? = null,
        val aiAvailable: Boolean = true,
        val history: List<PracticeSheetStore.Record> = emptyList(),
        val lastSheet: com.chenzixin.wrongbook.domain.generate.PracticeSheet? = null,
    ) {
        val filter: TagFilter get() = TagFilter(tagIds = selectedTagIds, matchAll = matchAll)
        val askFill: Boolean get() = pendingPlan != null
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { tagRepo.observeAll().first() }.onSuccess { list ->
                _state.update { it.copy(tags = list) }
            }
            val available = runCatching { settings.resolveActive() }.getOrNull() != null
            _state.update { it.copy(aiAvailable = available) }
            refreshHistory()
        }
        viewModelScope.launch {
            runCatching { appPrefs.aiWebSearch.first() }.onSuccess { on ->
                _state.update { it.copy(webSearch = on) }
            }
        }
    }

    fun toggleTag(id: Long) = _state.update { s ->
        s.copy(selectedTagIds = if (id in s.selectedTagIds) s.selectedTagIds - id else s.selectedTagIds + id)
    }

    fun setMatchAll(value: Boolean) = _state.update { it.copy(matchAll = value) }

    fun setCount(value: Int) = _state.update { it.copy(count = value.coerceIn(1, 50)) }

    fun setWithAnswers(value: Boolean) = _state.update { it.copy(withAnswers = value) }

    fun setUseAi(value: Boolean) = _state.update { it.copy(useAi = value) }

    fun setWebSearch(value: Boolean) {
        _state.update { it.copy(webSearch = value) }
        viewModelScope.launch { appPrefs.setAiWebSearch(value) }
    }

    fun clearSheet() = _state.update { it.copy(html = null) }

    fun onTitleChange(value: String) = _state.update { it.copy(sheetTitle = value) }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun dismissWarning() = _state.update { it.copy(warning = null) }

    /** 第一步：只选题、看缺口，必要时先把「要不要 AI 补答案」抛给用户。 */
    fun generate() {
        val s = _state.value
        if (s.generating) return
        _state.update { it.copy(generating = true, message = null) }
        viewModelScope.launch {
            val request = s.toRequest()
            when (val result = practiceRepo.inspect(request)) {
                is WbResult.Ok -> {
                    val plan = result.value
                    if (plan.needsFill && plan.aiAvailable) {
                        // 先别生成，问用户
                        _state.update { it.copy(generating = false, pendingPlan = plan) }
                    } else {
                        produce(plan, fillMissing = false)
                    }
                }
                is WbResult.Err -> _state.update { it.copy(generating = false, message = result.message) }
            }
        }
    }

    /** 用户选择：用 AI 补全答案与解析。 */
    fun confirmFill() {
        val plan = _state.value.pendingPlan ?: return
        _state.update { it.copy(pendingPlan = null, generating = true) }
        viewModelScope.launch { produce(plan, fillMissing = true) }
    }

    /** 用户选择：不补，直接生成。 */
    fun skipFill() {
        val plan = _state.value.pendingPlan ?: return
        _state.update { it.copy(pendingPlan = null, generating = true) }
        viewModelScope.launch { produce(plan, fillMissing = false) }
    }

    private suspend fun produce(plan: PracticeRepository.Plan, fillMissing: Boolean) {
        val request = _state.value.toRequest()
        val onStage: (String) -> Unit = { text -> _state.update { it.copy(stage = text) } }
        when (val result = practiceRepo.generate(plan, request, fillMissing, onStage)) {
            is WbResult.Ok -> {
                val sheet = result.value.sheet
                val html = practiceRepo.renderHtml(sheet)
                // 存一份历史，方便以后重新打印/另存 PDF
                runCatching {
                    sheetStore.save(
                        html = html,
                        title = sheet.title,
                        subtitle = sheet.subtitle,
                        count = sheet.items.size,
                        sheetJson = sheetJsonOf(sheet),
                    )
                }
                _state.update {
                    it.copy(
                        generating = false,
                        stage = null,
                        html = html,
                        warning = result.value.warning,
                        lastSheet = sheet,
                    )
                }
                refreshHistory()
            }
            is WbResult.Err -> _state.update {
                it.copy(generating = false, stage = null, message = result.message)
            }
        }
    }

    private fun UiState.toRequest() = PracticeRepository.Request(
        filter = filter,
        count = count,
        useAi = useAi,
        withAnswers = withAnswers,
        title = sheetTitle.ifBlank { "错题巩固练习" },
        webSearch = webSearch,
    )

    // ---------------------------------------------------------------- 历史记录

    private fun refreshHistory() {
        viewModelScope.launch {
            val list = runCatching { sheetStore.list() }.getOrDefault(emptyList())
            _state.update { it.copy(history = list) }
        }
    }

    /** 打开一份历史卷：直接进预览，可再次打印或另存 PDF。 */
    fun openHistory(record: PracticeSheetStore.Record) {
        viewModelScope.launch {
            val html = runCatching { sheetStore.read(record.id) }.getOrNull()
            if (html == null) {
                _state.update { it.copy(message = "这份历史记录的文件不在了") }
            } else {
                val sheet = parseSheet(record.sheetJson)
                _state.update { it.copy(html = html, lastSheet = sheet) }
            }
        }
    }

    fun deleteHistory(record: PracticeSheetStore.Record) {
        viewModelScope.launch {
            when (val r = sheetStore.delete(record.id)) {
                is WbResult.Ok -> {
                    refreshHistory()
                    _state.update { it.copy(message = "已删除这条历史记录") }
                }
                is WbResult.Err -> _state.update { it.copy(message = r.message) }
            }
        }
    }

    /** 导出 Word：返回 docx 字节；没有卷子时返回 null。 */
    fun buildDocx(): ByteArray? {
        val sheet = _state.value.lastSheet ?: return null
        return runCatching { DocxWriter.build(sheet, practiceRepo::figureJpeg) }.getOrNull()
    }

    private fun sheetJsonOf(sheet: com.chenzixin.wrongbook.domain.generate.PracticeSheet): String =
        runCatching { kotlinx.serialization.json.Json.encodeToString(
            com.chenzixin.wrongbook.domain.generate.PracticeSheet.serializer(), sheet,
        ) }.getOrDefault("")

    private fun parseSheet(json: String): com.chenzixin.wrongbook.domain.generate.PracticeSheet? {
        if (json.isBlank()) return null
        return runCatching {
            kotlinx.serialization.json.Json.decodeFromString(
                com.chenzixin.wrongbook.domain.generate.PracticeSheet.serializer(), json,
            )
        }.getOrNull()
    }
}
