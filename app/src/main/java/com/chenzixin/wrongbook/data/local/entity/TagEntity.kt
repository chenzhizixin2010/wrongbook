package com.chenzixin.wrongbook.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 标签 = 分类 / 收藏夹的统一抽象（不单独建「收藏夹」表）。
 *
 * - isFolder = true：在 UI 上呈现为「收藏夹 / 分类夹」；
 * - isFolder = false：呈现为彩色 Chip；
 * - parentId 支持两级（学科 → 章节），不做无限层级，避免筛选 UI 复杂化。
 */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true), Index("parentId")],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** #RRGGBB */
    val color: String = "#007AFF",
    val parentId: Long? = null,
    val isFolder: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
