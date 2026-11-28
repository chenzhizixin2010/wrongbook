package com.chenzixin.wrongbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chenzixin.wrongbook.data.local.entity.QuestionImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionImageDao {

    @Query("SELECT * FROM question_images WHERE questionId = :questionId ORDER BY createdAt ASC")
    fun observeByQuestion(questionId: Long): Flow<List<QuestionImageEntity>>

    @Query("SELECT * FROM question_images WHERE questionId = :questionId ORDER BY createdAt ASC")
    suspend fun listByQuestion(questionId: Long): List<QuestionImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: QuestionImageEntity): Long

    @Query("DELETE FROM question_images WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM question_images WHERE questionId = :questionId")
    suspend fun countByQuestion(questionId: Long): Int
}
