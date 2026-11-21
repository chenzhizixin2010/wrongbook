package com.chenzixin.wrongbook.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class StatsViewModel @Inject constructor(
    private val questionRepo: QuestionRepository,
    private val tagRepo: TagRepository,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val total: Int = 0,
        val mastery: List<MasteryCount> = emptyList(),
        val tagCounts: List<TagCount> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val total = runCatching { questionRepo.count() }.getOrDefault(0)
            val mastery = runCatching { questionRepo.masteryCounts() }.getOrDefault(emptyList())
            val tags = runCatching { tagRepo.tagCounts(10) }.getOrDefault(emptyList())
            _state.value = UiState(loading = false, total = total, mastery = mastery, tagCounts = tags)
        }
    }
}
