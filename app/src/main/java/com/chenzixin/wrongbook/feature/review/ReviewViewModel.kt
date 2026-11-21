package com.chenzixin.wrongbook.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.model.MasteryCount
import com.chenzixin.wrongbook.data.local.model.TagCount
import com.chenzixin.wrongbook.data.repository.QuestionRepository
import com.chenzixin.wrongbook.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repo: QuestionRepository,
    private val tagRepo: TagRepository,
) : ViewModel() {

    data class UiState(
        val due: List<QuestionEntity> = emptyList(),
        val index: Int = 0,
        val loading: Boolean = true,
        val reviewing: Boolean = false,
        val message: String? = null,
        val total: Int = 0,
        val mastery: List<MasteryCount> = emptyList(),
        val tagCounts: List<TagCount> = emptyList(),
    ) {
        val current: QuestionEntity? get() = due.getOrNull(index)
        val dueCount: Int get() = due.size
        val finished: Boolean get() = reviewing && index >= due.size
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val due = runCatching { repo.due(System.currentTimeMillis()) }.getOrDefault(emptyList())
            val total = runCatching { repo.count() }.getOrDefault(0)
            val mastery = runCatching { repo.masteryCounts() }.getOrDefault(emptyList())
            val tags = runCatching { tagRepo.tagCounts(8) }.getOrDefault(emptyList())
            _state.value = UiState(
                due = due,
                loading = false,
                total = total,
                mastery = mastery,
                tagCounts = tags,
            )
        }
    }

    fun start() {
        _state.update { it.copy(reviewing = true, index = 0) }
    }

    fun stop() {
        _state.update { it.copy(reviewing = false, index = 0) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    /** 答对/答错：更新掌握度与下次复习时间，然后进入下一题。 */
    fun answer(correct: Boolean) {
        val current = _state.value.current ?: return
        viewModelScope.launch {
            when (repo.applyReview(current, correct, System.currentTimeMillis())) {
                is WbResult.Ok -> _state.update { it.copy(index = it.index + 1) }
                is WbResult.Err -> _state.update { it.copy(message = "保存失败，请重试") }
            }
        }
    }
}
