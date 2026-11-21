package com.chenzixin.wrongbook.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.BuildConfig
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.update.DownloadProgress
import com.chenzixin.wrongbook.data.update.UpdateInfo
import com.chenzixin.wrongbook.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repo: UpdateRepository,
) : ViewModel() {

    data class UiState(
        val currentVersion: String = BuildConfig.VERSION_NAME,
        val checking: Boolean = false,
        val info: UpdateInfo? = null,
        val message: String? = null,
        val downloading: Boolean = false,
        val progress: DownloadProgress? = null,
        val downloadedFile: File? = null,
        val checked: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    init {
        check()
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun check() {
        if (_state.value.checking) return
        _state.update { it.copy(checking = true, message = null) }
        viewModelScope.launch {
            val result = repo.check(
                owner = BuildConfig.GITHUB_OWNER,
                repo = BuildConfig.GITHUB_REPO,
                currentVersion = BuildConfig.VERSION_NAME,
            )
            when (result) {
                is WbResult.Ok -> _state.update {
                    it.copy(
                        checking = false,
                        checked = true,
                        info = result.value,
                        message = if (result.value == null) "已是最新版本" else null,
                    )
                }
                is WbResult.Err -> _state.update {
                    it.copy(checking = false, checked = true, message = result.message)
                }
            }
        }
    }

    fun download() {
        val info = _state.value.info ?: return
        if (_state.value.downloading) return
        _state.update { it.copy(downloading = true, progress = null, message = null) }
        downloadJob = viewModelScope.launch {
            try {
                repo.download(info).collect { p -> _state.update { it.copy(progress = p) } }
                _state.update {
                    it.copy(
                        downloading = false,
                        downloadedFile = repo.downloadedApk(info),
                    )
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                _state.update {
                    it.copy(downloading = false, message = "下载失败：${t.message ?: "请稍后重试"}")
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _state.update { it.copy(downloading = false, progress = null) }
    }

    fun install() {
        val file = _state.value.downloadedFile ?: return
        when (val r = repo.install(file)) {
            is WbResult.Ok -> Unit
            is WbResult.Err -> _state.update { it.copy(message = r.message) }
        }
    }

    fun openInstallSettings() {
        repo.openInstallSettings()
    }
}
