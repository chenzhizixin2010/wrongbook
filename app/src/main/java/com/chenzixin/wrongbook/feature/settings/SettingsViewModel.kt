package com.chenzixin.wrongbook.feature.settings

import androidx.lifecycle.ViewModel
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.maintenance.ImageMaintenance
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import com.chenzixin.wrongbook.data.prefs.ThemeMode
import com.chenzixin.wrongbook.data.prefs.ThemePrefs
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    repo: SettingsRepository,
    private val themePrefs: ThemePrefs,
    private val maintenance: ImageMaintenance,
    private val appPrefs: com.chenzixin.wrongbook.data.prefs.AppPrefs,
) : ViewModel() {

    val splashEnabled: StateFlow<Boolean> = appPrefs.splashEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val betaChannel: StateFlow<Boolean> = appPrefs.betaChannel.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    fun setSplashEnabled(value: Boolean) {
        viewModelScope.launch { appPrefs.setSplashEnabled(value) }
    }

    fun setBetaChannel(value: Boolean) {
        viewModelScope.launch { appPrefs.setBetaChannel(value) }
    }

    /** 图片重处理进度；null 表示没在跑。 */
    private val _reprocess = MutableStateFlow<ImageMaintenance.Progress?>(null)
    val reprocess: StateFlow<ImageMaintenance.Progress?> = _reprocess.asStateFlow()

    private val _reprocessMessage = MutableStateFlow<String?>(null)
    val reprocessMessage: StateFlow<String?> = _reprocessMessage.asStateFlow()

    fun clearReprocessMessage() {
        _reprocessMessage.value = null
    }

    /** 把历史题目图统一「摆正 + 白底黑字」。 */
    fun reprocessImages() {
        if (_reprocess.value?.running == true) return
        viewModelScope.launch {
            _reprocessMessage.value = null
            when (val r = maintenance.reprocessAll { p -> _reprocess.value = p }) {
                is WbResult.Ok -> {
                    val p = r.value
                    _reprocess.value = null
                    _reprocessMessage.value = "处理完成：成功 ${p.done - p.failed} 张" +
                        if (p.failed > 0) "，失败 ${p.failed} 张" else ""
                }
                is WbResult.Err -> {
                    _reprocess.value = null
                    _reprocessMessage.value = r.message
                }
            }
        }
    }

    val profiles: StateFlow<List<ApiProfileEntity>> =
        repo.observeProfiles().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val themeMode: StateFlow<ThemeMode> = themePrefs.mode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM,
    )

    val uiStyle: StateFlow<UiStyle> = themePrefs.uiStyle.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiStyle.MIUIX,
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePrefs.setMode(mode) }
    }

    fun setUiStyle(style: UiStyle) {
        viewModelScope.launch { themePrefs.setUiStyle(style) }
    }
}
