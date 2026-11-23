package com.chenzixin.wrongbook.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import com.chenzixin.wrongbook.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val id: Long = 0L,
    val name: String = "",
    val baseUrl: String = "https://api.deepseek.com/v1",
    val chatModel: String = "deepseek-flash",
    val visionModel: String = "deepseek-flash",
    val apiKeyInput: String = "",
    val hasSavedKey: Boolean = false,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 0,
    val isDefault: Boolean = false,
    val isNew: Boolean = true,
    val saving: Boolean = false,
    val testing: Boolean = false,
    val visionTesting: Boolean = false,
    val message: String? = null,
    /** 刚自动保存过，标题栏显示「已自动保存」 */
    val autoSaved: Boolean = false,
    val finished: Boolean = false,
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val visionDiagnostics: com.chenzixin.wrongbook.data.ocr.VisionDiagnostics,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: Long = savedStateHandle.get<Long>("profileId") ?: 0L

    private val _state = MutableStateFlow(ProfileEditUiState())
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

    init {
        if (profileId != 0L) {
            viewModelScope.launch {
                repo.getProfile(profileId)?.let { p ->
                    _state.update {
                        it.copy(
                            id = p.id,
                            name = p.name,
                            baseUrl = p.baseUrl,
                            chatModel = p.chatModel,
                            visionModel = p.visionModel,
                            hasSavedKey = p.apiKeyEnc.isNotEmpty(),
                            temperature = p.temperature,
            maxTokens = p.maxTokens,
                            isDefault = p.isDefault,
                            isNew = false,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(v: String) {
        _state.update { it.copy(name = v) }
        scheduleAutoSave()
    }

    fun onBaseUrlChange(v: String) {
        _state.update { it.copy(baseUrl = v) }
        scheduleAutoSave()
    }

    fun onChatModelChange(v: String) {
        _state.update { it.copy(chatModel = v) }
        scheduleAutoSave()
    }

    fun onVisionModelChange(v: String) {
        _state.update { it.copy(visionModel = v) }
        scheduleAutoSave()
    }

    fun onApiKeyChange(v: String) {
        _state.update { it.copy(apiKeyInput = v) }
        scheduleAutoSave()
    }

    fun setMaxTokens(value: Int) {
        _state.update {
            // 0 表示「不限制」，其余夹在合理区间（上限按 50 倍档位放宽）
            it.copy(maxTokens = if (value <= 0) 0 else value.coerceIn(1024, 4096000))
        }
        scheduleAutoSave()
    }

    /** 一键填入服务商预设（只填地址与模型名，Key 仍需用户自己贴）。 */
    fun applyPreset(preset: ProviderPreset) {
        _state.update {
            it.copy(
                name = preset.name,
                baseUrl = preset.baseUrl,
                chatModel = preset.chatModel,
                visionModel = preset.visionModel,
            )
        }
        scheduleAutoSave()
    }

    fun adjustTemperature(delta: Float) {
        _state.update { it.copy(temperature = (it.temperature + delta).coerceIn(0f, 2f)) }
        scheduleAutoSave()
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private var autoSaveJob: Job? = null

    /** 任何字段改动都排一次自动保存（防抖 700ms），不再需要手动点保存。 */
    fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(700)
            performSave(silent = true)
        }
    }

    /**
     * 真正落库。
     * @param silent true = 自动保存：成功时不跳转、不弹提示，只在标题栏显示「已自动保存」
     */
    fun performSave(silent: Boolean = false) {
        val s = _state.value
        if (s.saving) return
        // 新建配置时，关键字段没填全就别写库，避免产生一堆空配置
        if (s.name.isBlank() || s.baseUrl.isBlank() || s.chatModel.isBlank()) return
        _state.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            val entity = ApiProfileEntity(
                id = s.id,
                name = s.name,
                baseUrl = s.baseUrl,
                chatModel = s.chatModel,
                visionModel = s.visionModel,
                temperature = s.temperature,
                maxTokens = s.maxTokens,
                isDefault = s.isDefault,
                apiKeyEnc = "",
            )
            val key = s.apiKeyInput.takeIf { it.isNotBlank() }
            when (val r = repo.saveProfile(entity, key)) {
                is WbResult.Ok -> _state.update {
                    it.copy(
                        id = r.value,
                        isNew = false,
                        hasSavedKey = it.hasSavedKey || key != null,
                        apiKeyInput = "",
                        saving = false,
                        finished = !silent,
                        message = if (silent) null else it.message,
                        autoSaved = silent,
                    )
                }
                is WbResult.Err -> _state.update { it.copy(saving = false, message = r.message) }
            }
        }
    }

    fun testConnection() {
        val s = _state.value
        if (s.testing) return
        if (s.id == 0L) {
            _state.update { it.copy(message = "先保存配置，再测试连接") }
            return
        }
        if (s.apiKeyInput.isNotBlank()) {
            _state.update { it.copy(message = "Key 有修改，请先保存再测试") }
            return
        }
        _state.update { it.copy(testing = true, message = null) }
        viewModelScope.launch {
            when (val r = repo.testConnection(s.id)) {
                is WbResult.Ok -> _state.update { it.copy(testing = false, message = "连接成功，往返 ${r.value} ms") }
                is WbResult.Err -> _state.update { it.copy(testing = false, message = r.message) }
            }
        }
    }

    /**
     * 识图自检：用设备现画的样张走一遍真实视觉链路。
     * 结果（成功/N 道题，或具体报错）直接显示，方便定位「为什么只用了本地识别」。
     */
    fun testVision() {
        val s = _state.value
        if (s.visionTesting) return
        if (s.id == 0L) {
            _state.update { it.copy(message = "先保存配置，再测试识图") }
            return
        }
        if (s.visionModel.isBlank()) {
            _state.update { it.copy(message = "视觉模型留空，扫描时只会用本地 OCR。先填上模型名（如 deepseek-flash）") }
            return
        }
        _state.update { it.copy(visionTesting = true, message = null) }
        viewModelScope.launch {
            val profile = repo.getProfile(s.id)
            val key = repo.revealApiKey(s.id)
            if (profile == null || key.isNullOrBlank()) {
                _state.update { it.copy(visionTesting = false, message = "读不到这套配置或 Key，请重新保存") }
                return@launch
            }
            when (val r = visionDiagnostics.run(profile, key)) {
                is WbResult.Ok -> _state.update { it.copy(visionTesting = false, message = r.value) }
                is WbResult.Err -> _state.update { it.copy(visionTesting = false, message = r.message) }
            }
        }
    }

    fun setDefault() {
        val s = _state.value
        if (s.id == 0L) {
            _state.update { it.copy(message = "先保存配置") }
            return
        }
        viewModelScope.launch {
            when (val r = repo.setDefault(s.id)) {
                is WbResult.Ok -> _state.update { it.copy(isDefault = true, message = "已设为默认配置") }
                is WbResult.Err -> _state.update { it.copy(message = r.message) }
            }
        }
    }

    fun delete() {
        val s = _state.value
        if (s.id == 0L) {
            _state.update { it.copy(finished = true) }
            return
        }
        viewModelScope.launch {
            when (val r = repo.deleteProfile(s.id)) {
                is WbResult.Ok -> _state.update { it.copy(finished = true) }
                is WbResult.Err -> _state.update { it.copy(message = r.message) }
            }
        }
    }
}
