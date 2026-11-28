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
    private val superResolutionEngine: com.chenzixin.wrongbook.data.ml.SuperResolutionEngine,
) : ViewModel() {

    val splashEnabled: StateFlow<Boolean> = appPrefs.splashEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val updateChannel: StateFlow<com.chenzixin.wrongbook.domain.update.UpdateChannel> =
        appPrefs.updateChannel.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = com.chenzixin.wrongbook.domain.update.UpdateChannel.DEFAULT,
        )

    fun setSplashEnabled(value: Boolean) {
        viewModelScope.launch { appPrefs.setSplashEnabled(value) }
    }

    fun setUpdateChannel(channel: com.chenzixin.wrongbook.domain.update.UpdateChannel) {
        viewModelScope.launch { appPrefs.setUpdateChannel(channel) }
    }

    // ---------------------------------------------------------------- 超分模型

    val superResolution: StateFlow<Boolean> = appPrefs.superResolution.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    /** 模型一句话描述：进设置页后才在后台算，不在构造期碰磁盘/模型。 */
    private val _srInfo = MutableStateFlow("EDSR ×4 · 内置模型")
    val srInfo: StateFlow<String> = _srInfo.asStateFlow()

    init {
        viewModelScope.launch {
            _srInfo.value = runCatching { superResolutionEngine.describe() }
                .getOrDefault("EDSR ×4 · 内置模型")
        }
    }

    private val _srTest = MutableStateFlow<String?>(null)
    val srTest: StateFlow<String?> = _srTest.asStateFlow()

    fun setSuperResolution(value: Boolean) {
        viewModelScope.launch { appPrefs.setSuperResolution(value) }
    }

    /** 在这台机器上真跑一次模型，把耗时/失败原因告诉用户。 */
    fun runSrSelfTest() {
        _srTest.value = "自检中…（最多几秒）"
        viewModelScope.launch {
            _srTest.value = when (val r = superResolutionEngine.selfTest()) {
                is WbResult.Ok -> "模型可用：${r.value.tiles} 块共 ${r.value.totalMillis} ms" +
                    "（平均 ${r.value.perTileMillis} ms/块）"
                is WbResult.Err -> r.message
            }
        }
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
