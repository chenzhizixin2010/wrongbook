package com.chenzixin.wrongbook.data.local.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.entity.QuestionTagCrossRef
import com.chenzixin.wrongbook.data.local.entity.TagEntity

/** 一道题 + 它的全部标签（多对多，经交叉表 join）。 */
data class QuestionWithTags(
    @Embedded val question: QuestionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = QuestionTagCrossRef::class,
            parentColumn = "questionId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
)
