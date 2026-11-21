package com.chenzixin.wrongbook.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 题目 ↔ 标签 多对多关联。
 *
 * 两侧都是 CASCADE：删题时清掉它的关联，删标签时也只清掉关联行 —— 题目本身不动。
 * 也就是说「删标签」永远等于「解除分类」，不会误删错题（这是验收项之一）。
 */
@Entity(
    tableName = "question_tag_cross_ref",
    primaryKeys = ["questionId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId"), Index("questionId")],
)
data class QuestionTagCrossRef(
    val questionId: Long,
    val tagId: Long,
)
