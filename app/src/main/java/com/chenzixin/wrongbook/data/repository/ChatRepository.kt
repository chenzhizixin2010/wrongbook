package com.chenzixin.wrongbook.data.repository

import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.data.local.entity.ChatMessageEntity
import com.chenzixin.wrongbook.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    fun observeSessions(): Flow<List<ChatSessionEntity>>

    fun observeSession(id: Long): Flow<ChatSessionEntity?>

    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>>

    suspend fun messagesOnce(sessionId: Long): List<ChatMessageEntity>

    suspend fun createSession(title: String = "新对话", profileId: Long? = null): WbResult<Long>

    suspend fun renameSession(id: Long, title: String): WbResult<Unit>

    suspend fun setPinned(id: Long, pinned: Boolean): WbResult<Unit>

    suspend fun deleteSession(id: Long): WbResult<Unit>

    suspend fun appendMessage(
        sessionId: Long,
        role: String,
        content: String,
        imagePaths: List<String> = emptyList(),
    ): WbResult<Long>

    suspend fun updateMessage(id: Long, content: String): WbResult<Unit>
}
