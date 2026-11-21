package com.chenzixin.wrongbook.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
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
import kotlinx.coroutines.launch

data class HomeUiState(
    val questions: List<QuestionWithTags> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val filter: TagFilter = TagFilter.NONE,
    val keyword: String = "",
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val message: String? = null,
) {
    val hasAnyQuestion: Boolean get() = questions.isNotEmpty() || !filter.isEmpty
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: QuestionRepository,
    tagRepo: TagRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(TagFilter.NONE)
    private val keyword = MutableStateFlow("")
    private val selectionMode = MutableStateFlow(false)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val message = MutableStateFlow<String?>(null)

    /** 把 5 个本地状态先合成一个，避免 combine 超过 5 个参数（Kotlin 只提供到 5 参重载）。 */
    private data class LocalState(
        val filter: TagFilter,
        val keyword: String,
        val selectionMode: Boolean,
        val selectedIds: Set<Long>,
        val message: String?,
    )

    private val local = combine(filter, keyword, selectionMode, selectedIds, message) { f, kw, sm, ids, msg ->
        LocalState(f, kw, sm, ids, msg)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repo.observeAll(),
        tagRepo.observeAll(),
        local,
    ) { questions, tags, l ->
        val f = l.filter
        val kw = l.keyword
        val filtered = questions.filter { item ->
            val tagOk = f.isEmpty || f.matches(item.tags.map { it.id })
            val keywordOk = kw.isBlank() || run {
                val text = (item.question.refinedText ?: "") + item.question.rawOcrText
                text.contains(kw, ignoreCase = true)
            }
            tagOk && keywordOk
        }
        HomeUiState(
            questions = filtered,
            tags = tags,
            filter = f,
            keyword = kw,
            selectionMode = l.selectionMode,
            selectedIds = l.selectedIds,
            message = l.message,
        )
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

    // ---------- 批量管理 ----------

    fun enterSelectionMode() {
        selectionMode.value = true
        selectedIds.value = emptySet()
    }

    fun exitSelectionMode() {
        selectionMode.value = false
        selectedIds.value = emptySet()
    }

    fun toggleSelect(id: Long) {
        selectedIds.update { if (id in it) it - id else it + id }
    }

    fun selectAll() {
        selectedIds.value = uiState.value.questions.map { it.question.id }.toSet()
    }

    fun clearSelection() = selectedIds.update { emptySet() }

    fun clearMessage() = message.update { null }

    /** 批量删除：逐条删，失败计数后统一提示，避免一条失败全盘中断。 */
    fun deleteSelected() {
        val ids = selectedIds.value
        if (ids.isEmpty()) {
            message.value = "先勾选要删除的题目"
            return
        }
        viewModelScope.launch {
            var failed = 0
            ids.forEach { id ->
                if (repo.delete(id) !is WbResult.Ok) failed++
            }
            selectedIds.value = emptySet()
            selectionMode.value = false
            message.value = if (failed == 0) {
                "已删除 ${ids.size} 道题"
            } else {
                "删除完成，${ids.size - failed} 道成功，$failed 道失败"
            }
        }
    }
}
