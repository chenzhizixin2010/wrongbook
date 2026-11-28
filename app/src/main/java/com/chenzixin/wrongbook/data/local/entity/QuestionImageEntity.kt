package com.chenzixin.wrongbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 题目的附加图片：几何图形、函数图像、实验装置图等。
 *
 * 和题目主图（[QuestionEntity.imagePath]）分开存：
 * 主图是照片裁剪出来的题面，插图是单独拍/单独导入的图，两者的处理链路不同。
 */
@Entity(
    tableName = "question_images",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("questionId")],
)
data class QuestionImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    /** 原图相对 filesDir 的路径 */
    val path: String,
    /** 200px 缩略图路径 */
    val thumbPath: String,
    /** FIGURE = 题目插图；ANSWER = 答案解析里的图 */
    val kind: String = "FIGURE",
    val createdAt: Long = System.currentTimeMillis(),
)
