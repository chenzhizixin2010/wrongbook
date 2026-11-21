package com.chenzixin.wrongbook.feature.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.generate.PracticeRepository
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
) : ViewModel() {

    data class UiState(
        val tags: List<TagEntity> = emptyList(),
        val selectedTagIds: Set<Long> = emptySet(),
        val matchAll: Boolean = false,
        val count: Int = 10,
        val withAnswers: Boolean = true,
        val useAi: Boolean = false,
        val generating: Boolean = false,
        val html: String? = null,
        val sheetTitle: String = "错题巩固练习",
        val message: String? = null,
        /** 待确认「要不要补全答案」的方案 */
        val pendingPlan: PracticeRepository.Plan? = null,
        /** 生成后的提示（AI 失败等） */
        val warning: String? = null,
        val aiAvailable: Boolean = true,
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
        }
    }

    fun toggleTag(id: Long) = _state.update { s ->
        s.copy(selectedTagIds = if (id in s.selectedTagIds) s.selectedTagIds - id else s.selectedTagIds + id)
    }

    fun setMatchAll(value: Boolean) = _state.update { it.copy(matchAll = value) }

    fun setCount(value: Int) = _state.update { it.copy(count = value.coerceIn(1, 50)) }

    fun setWithAnswers(value: Boolean) = _state.update { it.copy(withAnswers = value) }

    fun setUseAi(value: Boolean) = _state.update { it.copy(useAi = value) }

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
        when (val result = practiceRepo.generate(plan, request, fillMissing)) {
            is WbResult.Ok -> {
                val html = practiceRepo.renderHtml(result.value.sheet)
                _state.update {
                    it.copy(
                        generating = false,
                        html = html,
                        warning = result.value.warning,
                    )
                }
            }
            is WbResult.Err -> _state.update { it.copy(generating = false, message = result.message) }
        }
    }

    private fun UiState.toRequest() = PracticeRepository.Request(
        filter = filter,
        count = count,
        useAi = useAi,
        withAnswers = withAnswers,
        title = sheetTitle.ifBlank { "错题巩固练习" },
    )
}
