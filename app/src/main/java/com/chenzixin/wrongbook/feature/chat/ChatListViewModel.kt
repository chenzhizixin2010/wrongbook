package com.chenzixin.wrongbook.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.ChatSessionEntity
import com.chenzixin.wrongbook.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repo: ChatRepository,
) : ViewModel() {

    val sessions: StateFlow<List<ChatSessionEntity>> = repo.observeSessions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _createdSessionId = MutableStateFlow<Long?>(null)
    val createdSessionId: StateFlow<Long?> = _createdSessionId.asStateFlow()

    fun consumeCreated() {
        _createdSessionId.value = null
    }

    fun createSession() {
        viewModelScope.launch {
            when (val r = repo.createSession()) {
                is WbResult.Ok -> _createdSessionId.value = r.value
                is WbResult.Err -> Unit
            }
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch { repo.deleteSession(id) }
    }

    fun togglePinned(session: ChatSessionEntity) {
        viewModelScope.launch { repo.setPinned(session.id, !session.pinned) }
    }
}
