package com.chenzixin.wrongbook.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一次 AI 对话（会话）。 */
@Entity(
    tableName = "chat_sessions",
    indices = [Index("updatedAt")],
)
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "新对话",
    /** 使用的 ApiProfile；为空表示跟随默认配置 */
    val profileId: Long? = null,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
