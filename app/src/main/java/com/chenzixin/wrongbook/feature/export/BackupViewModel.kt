package com.chenzixin.wrongbook.feature.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.export.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repo: BackupRepository,
) : ViewModel() {

    data class UiState(
        val busy: Boolean = false,
        val message: String? = null,
        val finished: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun export(uri: Uri) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null, finished = false) }
        viewModelScope.launch {
            when (val r = repo.export(uri)) {
                is WbResult.Ok -> _state.update {
                    it.copy(busy = false, finished = true, message = "已导出 ${r.value} 道题")
                }
                is WbResult.Err -> _state.update { it.copy(busy = false, message = r.message) }
            }
        }
    }

    fun import(uri: Uri) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null, finished = false) }
        viewModelScope.launch {
            when (val r = repo.import(uri)) {
                is WbResult.Ok -> _state.update {
                    it.copy(
                        busy = false,
                        finished = true,
                        message = "导入完成：新增 ${r.value.importedQuestions} 道，跳过重复 ${r.value.skipped} 道",
                    )
                }
                is WbResult.Err -> _state.update { it.copy(busy = false, message = r.message) }
            }
        }
    }

    fun reset() = _state.update { UiState() }
}
