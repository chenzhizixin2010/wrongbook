package com.chenzixin.wrongbook.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import com.chenzixin.wrongbook.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    val isDefault: Boolean = false,
    val isNew: Boolean = true,
    val saving: Boolean = false,
    val testing: Boolean = false,
    val message: String? = null,
    val finished: Boolean = false,
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val repo: SettingsRepository,
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
                            isDefault = p.isDefault,
                            isNew = false,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onBaseUrlChange(v: String) = _state.update { it.copy(baseUrl = v) }
    fun onChatModelChange(v: String) = _state.update { it.copy(chatModel = v) }
    fun onVisionModelChange(v: String) = _state.update { it.copy(visionModel = v) }
    fun onApiKeyChange(v: String) = _state.update { it.copy(apiKeyInput = v) }

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
    }

    fun adjustTemperature(delta: Float) = _state.update {
        it.copy(temperature = (it.temperature + delta).coerceIn(0f, 2f))
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun save() {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            val entity = ApiProfileEntity(
                id = s.id,
                name = s.name,
                baseUrl = s.baseUrl,
                chatModel = s.chatModel,
                visionModel = s.visionModel,
                temperature = s.temperature,
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
                        finished = true,
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
