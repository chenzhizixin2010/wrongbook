package com.chenzixin.wrongbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一条消息。
 *
 * imagePaths 用 "\n" 分隔的相对路径（P0 简单方案）；
 * P6 若需要结构化元数据，再换 JSON + TypeConverter，注意配 Room 迁移。
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("createdAt")],
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    /** system / user / assistant */
    val role: String,
    val content: String,
    val imagePaths: String = "",
    /** 输入+输出 token，用于成本统计（拿不到时为 null） */
    val tokenUsage: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
