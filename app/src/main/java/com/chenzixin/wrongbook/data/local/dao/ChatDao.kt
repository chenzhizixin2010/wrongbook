package com.chenzixin.wrongbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chenzixin.wrongbook.data.local.entity.ChatMessageEntity
import com.chenzixin.wrongbook.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Insert
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun renameSession(id: Long, title: String, now: Long)

    @Query("UPDATE chat_sessions SET pinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, now: Long)

    @Query("UPDATE chat_sessions SET updatedAt = :now WHERE id = :id")
    suspend fun touchSession(id: Long, now: Long)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearMessages(sessionId: Long)

    @Query("SELECT * FROM chat_sessions ORDER BY pinned DESC, updatedAt DESC")
    fun observeSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<ChatSessionEntity?>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC, id ASC")
    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC, id ASC")
    suspend fun messagesOnce(sessionId: Long): List<ChatMessageEntity>

    @Query("UPDATE chat_messages SET content = :content WHERE id = :id")
    suspend fun updateMessageContent(id: Long, content: String)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun messageCount(sessionId: Long): Int
}
