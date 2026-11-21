package com.chenzixin.wrongbook.feature.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val repo: TagRepository,
) : ViewModel() {

    val tags: StateFlow<List<TagEntity>> = repo.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun create(name: String, isFolder: Boolean, color: String, parentId: Long? = null) {
        viewModelScope.launch {
            when (val r = repo.create(name = name, color = color, isFolder = isFolder, parentId = parentId)) {
                is WbResult.Ok -> _message.value = "已创建「${name.trim()}」"
                is WbResult.Err -> _message.value = r.message
            }
        }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch {
            when (val r = repo.rename(id, name)) {
                is WbResult.Ok -> _message.value = "已重命名"
                is WbResult.Err -> _message.value = r.message
            }
        }
    }

    fun update(tag: TagEntity): Boolean {
        viewModelScope.launch {
            when (val r = repo.update(tag)) {
                is WbResult.Ok -> _message.value = "已保存"
                is WbResult.Err -> _message.value = r.message
            }
        }
        return true
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            when (val r = repo.deleteTag(id)) {
                is WbResult.Ok -> _message.value = "已删除标签（题目不受影响）"
                is WbResult.Err -> _message.value = r.message
            }
        }
    }
}

/** 预设色板（iOS 系统色），避免让用户自己输十六进制。 */
val TAG_PALETTE = listOf(
    "#007AFF" to "蓝",
    "#34C759" to "绿",
    "#FF9500" to "橙",
    "#FF3B30" to "红",
    "#AF52DE" to "紫",
    "#FF2D55" to "粉",
    "#5856D6" to "靛",
    "#00C7BE" to "青",
    "#8E8E93" to "灰",
    "#FFCC00" to "黄",
)
