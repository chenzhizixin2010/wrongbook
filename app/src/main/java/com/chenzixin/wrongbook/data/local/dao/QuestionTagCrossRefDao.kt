package com.chenzixin.wrongbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chenzixin.wrongbook.data.local.entity.QuestionTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionTagCrossRefDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(refs: List<QuestionTagCrossRef>)

    @Query("DELETE FROM question_tag_cross_ref WHERE questionId = :questionId")
    suspend fun deleteByQuestionId(questionId: Long)

    /** 删标签时调用：只解除分类，题目本身不动。 */
    @Query("DELETE FROM question_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteByTagId(tagId: Long)

    @Query("SELECT tagId FROM question_tag_cross_ref WHERE questionId = :questionId")
    suspend fun tagIdsOf(questionId: Long): List<Long>

    @Query("SELECT DISTINCT tagId FROM question_tag_cross_ref")
    fun observeUsedTagIds(): Flow<List<Long>>
}
