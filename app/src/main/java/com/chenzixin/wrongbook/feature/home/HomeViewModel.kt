package com.chenzixin.wrongbook.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.data.local.model.QuestionWithTags
import com.chenzixin.wrongbook.data.repository.QuestionRepository
import com.chenzixin.wrongbook.data.repository.TagRepository
import com.chenzixin.wrongbook.domain.tag.TagFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val questions: List<QuestionWithTags> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val filter: TagFilter = TagFilter.NONE,
    val keyword: String = "",
) {
    val hasAnyQuestion: Boolean get() = questions.isNotEmpty() || !filter.isEmpty
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    repo: QuestionRepository,
    tagRepo: TagRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(TagFilter.NONE)
    private val keyword = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        repo.observeAll(),
        tagRepo.observeAll(),
        filter,
        keyword,
    ) { questions, tags, f, kw ->
        val filtered = questions.filter { item ->
            val tagOk = f.isEmpty || f.matches(item.tags.map { it.id })
            val keywordOk = kw.isBlank() || run {
                val text = (item.question.refinedText ?: "") + item.question.rawOcrText
                text.contains(kw, ignoreCase = true)
            }
            tagOk && keywordOk
        }
        HomeUiState(questions = filtered, tags = tags, filter = f, keyword = kw)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun toggleTag(id: Long) = filter.update { current ->
        current.copy(tagIds = if (id in current.tagIds) current.tagIds - id else current.tagIds + id)
    }

    fun setMatchAll(matchAll: Boolean) = filter.update { it.copy(matchAll = matchAll) }

    fun clearFilter() {
        filter.value = TagFilter.NONE
    }

    fun setKeyword(value: String) {
        keyword.value = value
    }
}
