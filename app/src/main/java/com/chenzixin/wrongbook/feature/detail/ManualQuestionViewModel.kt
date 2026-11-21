package com.chenzixin.wrongbook.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.data.repository.QuestionRepository
import com.chenzixin.wrongbook.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 手动新建题目（没有图片，纯文字）。 */
@HiltViewModel
class ManualQuestionViewModel @Inject constructor(
    private val repo: QuestionRepository,
    private val tagRepo: TagRepository,
) : ViewModel() {

    data class UiState(
        val stem: String = "",
        val answer: String = "",
        val analysis: String = "",
        val subject: String = "",
        val tags: List<TagEntity> = emptyList(),
        val selectedTagIds: Set<Long> = emptySet(),
        val saving: Boolean = false,
        val message: String? = null,
        val finished: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { tagRepo.observeAll().first() }
                .onSuccess { tags -> _state.update { it.copy(tags = tags) } }
        }
    }

    fun onStem(v: String) = _state.update { it.copy(stem = v) }
    fun onAnswer(v: String) = _state.update { it.copy(answer = v) }
    fun onAnalysis(v: String) = _state.update { it.copy(analysis = v) }
    fun onSubject(v: String) = _state.update { it.copy(subject = v) }
    fun toggleTag(id: Long) = _state.update {
        it.copy(selectedTagIds = if (id in it.selectedTagIds) it.selectedTagIds - id else it.selectedTagIds + id)
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            when (val r = tagRepo.create(name = name)) {
                is WbResult.Ok -> {
                    val latest = runCatching { tagRepo.observeAll().first() }.getOrDefault(emptyList())
                    _state.update {
                        it.copy(tags = latest, selectedTagIds = it.selectedTagIds + r.value)
                    }
                }
                is WbResult.Err -> _state.update { it.copy(message = r.message) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun save() {
        val s = _state.value
        if (s.saving) return
        if (s.stem.isBlank()) {
            _state.update { it.copy(message = "题干不能为空") }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val entity = QuestionEntity(
                imagePath = "",
                thumbPath = "",
                rawOcrText = s.stem.trim(),
                refinedText = s.stem.trim(),
                answer = s.answer.trim().ifBlank { null },
                analysis = s.analysis.trim().ifBlank { null },
                subject = s.subject.trim().ifBlank { null },
                source = "MANUAL",
            )
            when (val r = repo.add(entity, s.selectedTagIds.toList())) {
                is WbResult.Ok -> _state.update { it.copy(saving = false, finished = true) }
                is WbResult.Err -> _state.update { it.copy(saving = false, message = r.message) }
            }
        }
    }
}
