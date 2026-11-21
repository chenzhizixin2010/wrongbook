package com.chenzixin.wrongbook.data.repository

import com.chenzixin.wrongbook.core.WbResult
import com.chenzixin.wrongbook.core.runCatchingWb
import com.chenzixin.wrongbook.data.local.dao.ChatDao
import com.chenzixin.wrongbook.data.local.entity.ChatMessageEntity
import com.chenzixin.wrongbook.data.local.entity.ChatSessionEntity
import com.chenzixin.wrongbook.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val dao: ChatDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ChatRepository {

    override fun observeSessions(): Flow<List<ChatSessionEntity>> = dao.observeSessions()

    override fun observeSession(id: Long): Flow<ChatSessionEntity?> = dao.observeSession(id)

    override fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>> =
        dao.observeMessages(sessionId)

    override suspend fun messagesOnce(sessionId: Long): List<ChatMessageEntity> =
        withContext(io) { dao.messagesOnce(sessionId) }

    override suspend fun createSession(title: String, profileId: Long?): WbResult<Long> =
        withContext(io) {
            runCatchingWb("新建对话失败") {
                dao.insertSession(ChatSessionEntity(title = title, profileId = profileId))
            }
        }

    override suspend fun renameSession(id: Long, title: String): WbResult<Unit> = withContext(io) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return@withContext WbResult.Err("标题不能为空")
        runCatchingWb("重命名失败") { dao.renameSession(id, trimmed, System.currentTimeMillis()) }
    }

    override suspend fun setPinned(id: Long, pinned: Boolean): WbResult<Unit> = withContext(io) {
        runCatchingWb("操作失败") { dao.setPinned(id, pinned, System.currentTimeMillis()) }
    }

    override suspend fun deleteSession(id: Long): WbResult<Unit> = withContext(io) {
        runCatchingWb("删除对话失败") { dao.deleteSession(id) }
    }

    override suspend fun appendMessage(
        sessionId: Long,
        role: String,
        content: String,
        imagePaths: List<String>,
    ): WbResult<Long> = withContext(io) {
        runCatchingWb("消息保存失败") {
            val now = System.currentTimeMillis()
            val id = dao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = role,
                    content = content,
                    imagePaths = imagePaths.joinToString("\n"),
                    createdAt = now,
                ),
            )
            dao.touchSession(sessionId, now)
            id
        }
    }

    override suspend fun updateMessage(id: Long, content: String): WbResult<Unit> = withContext(io) {
        runCatchingWb("消息更新失败") {
            // 用 REPLACE 语义会丢 createdAt，这里直接写一个最小更新
            dao.updateMessageContent(id, content)
        }
    }
}
