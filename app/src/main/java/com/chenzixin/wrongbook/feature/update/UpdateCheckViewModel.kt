package com.chenzixin.wrongbook.feature.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.BuildConfig
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.prefs.UpdatePrefs
import com.chenzixin.wrongbook.data.update.UpdateInfo
import com.chenzixin.wrongbook.data.update.UpdateRepository
import com.chenzixin.wrongbook.domain.update.UpdateChannel
import com.chenzixin.wrongbook.domain.update.UpdateLevel
import com.chenzixin.wrongbook.domain.update.UpdateLevelClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 启动时静默检查一次更新。
 *
 * - 大版本（0.4.x → 0.5.0）→ 主页弹「建议更新」，点稍后记住版本，不再反复打扰；
 * - 小版本（0.4.0 → 0.4.1）→ 不弹窗，只进「设置 → 检查更新」可见。
 * 网络失败一律静默，绝不在启动时弹错误。
 */
@HiltViewModel
class UpdateCheckViewModel @Inject constructor(
    private val repo: UpdateRepository,
    private val prefs: UpdatePrefs,
    private val appPrefs: com.chenzixin.wrongbook.data.prefs.AppPrefs,
) : ViewModel() {

    data class UiState(
        val info: UpdateInfo? = null,
        val level: UpdateLevel = UpdateLevel.NONE,
        val showDialog: Boolean = false,
        val channel: UpdateChannel = UpdateChannel.DEFAULT,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var started = false

    fun checkOnce() {
        if (started) return
        started = true
        viewModelScope.launch {
            val channel = runCatching { appPrefs.updateChannel.first() }
                .getOrDefault(UpdateChannel.DEFAULT)
            val result = repo.check(
                owner = BuildConfig.GITHUB_OWNER,
                repo = BuildConfig.GITHUB_REPO,
                currentVersion = BuildConfig.VERSION_NAME,
                channel = channel,
                alphaRepo = BuildConfig.ALPHA_GITHUB_REPO,
            )
            val info = (result as? WbResult.Ok)?.value ?: return@launch
            val level = UpdateLevelClassifier.classify(BuildConfig.VERSION_NAME, info.version)
            val dismissed = runCatching { prefs.dismissedMajorVersion() }.getOrNull()
            _state.value = UiState(
                info = info,
                level = level,
                channel = channel,
                // 测试通道（Beta / Alpha）收到测试版时，无论版本级别一律弹窗
                showDialog = info.isPrerelease ||
                    info.isAlpha ||
                    (level == UpdateLevel.MAJOR && dismissed != info.version),
            )
        }
    }

    fun dismiss() {
        val version = _state.value.info?.version
        _state.update { it.copy(showDialog = false) }
        if (version != null) {
            viewModelScope.launch { runCatching { prefs.dismissMajor(version) } }
        }
    }
}
