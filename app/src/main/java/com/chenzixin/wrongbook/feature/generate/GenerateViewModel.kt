package com.chenzixin.wrongbook.feature.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.generate.PracticeRepository
import com.chenzixin.wrongbook.data.local.entity.TagEntity
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
    ) {
        val filter: TagFilter get() = TagFilter(tagIds = selectedTagIds, matchAll = matchAll)
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { tagRepo.observeAll().first() }.onSuccess { list ->
                _state.update { it.copy(tags = list) }
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

    fun clearSheet() = _state.update { it.copy(html = null) }

    fun onTitleChange(value: String) = _state.update { it.copy(sheetTitle = value) }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun generate() {
        val s = _state.value
        if (s.generating) return
        _state.update { it.copy(generating = true, message = null) }
        viewModelScope.launch {
            val request = PracticeRepository.Request(
                filter = s.filter,
                count = s.count,
                useAi = s.useAi,
                withAnswers = s.withAnswers,
                title = s.sheetTitle.ifBlank { "错题巩固练习" },
            )
            when (val result = practiceRepo.buildSheet(request)) {
                is WbResult.Ok -> {
                    val html = practiceRepo.renderHtml(result.value)
                    _state.update { it.copy(generating = false, html = html) }
                }
                is WbResult.Err -> _state.update { it.copy(generating = false, message = result.message) }
            }
        }
    }
}
