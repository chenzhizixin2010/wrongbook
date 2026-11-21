package com.chenzixin.wrongbook.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一道错题。
 *
 * 图片只存本机私有目录路径（filesDir/questions/），文件名用 UUID；
 * 不存绝对路径前缀，方便后续 App 升级换目录时迁移。
 */
@Entity(
    tableName = "questions",
    indices = [Index("subject"), Index("nextReviewAt"), Index("createdAt")],
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 原题图（已裁剪），相对 filesDir 的路径 */
    val imagePath: String,
    /** 列表缩略图，长边 200px */
    val thumbPath: String,
    /** OCR 原始文本，保留用于追溯与重新整理 */
    val rawOcrText: String = "",
    /** AI 整理后的题干（可空 = 未整理） */
    val refinedText: String? = null,
    val answer: String? = null,
    val analysis: String? = null,
    val subject: String? = null,
    /** SCAN / AI_CHAT / MANUAL */
    val source: String = "SCAN",
    /** 掌握度 0~5，简化 SM-2 使用 */
    val masteryLevel: Int = 0,
    val reviewCount: Int = 0,
    val lastReviewAt: Long? = null,
    val nextReviewAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
