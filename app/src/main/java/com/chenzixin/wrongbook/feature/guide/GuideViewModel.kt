package com.chenzixin.wrongbook.feature.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.data.prefs.AppPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 首次使用引导的状态。
 *
 * [guideSeen] 用 `null` 表示 DataStore 还没读完：这时既不弹引导也不查更新，
 * 避免老用户启动时先闪一下引导层。
 */
@HiltViewModel
class GuideViewModel @Inject constructor(
    private val appPrefs: AppPrefs,
) : ViewModel() {

    val guideSeen: StateFlow<Boolean?> = appPrefs.guideSeen
        .map<Boolean, Boolean?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun markSeen() {
        viewModelScope.launch { appPrefs.markGuideSeen() }
    }

    fun resetSeen() {
        viewModelScope.launch { appPrefs.resetGuideSeen() }
    }
}
