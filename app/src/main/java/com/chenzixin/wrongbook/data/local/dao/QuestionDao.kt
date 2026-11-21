package com.chenzixin.wrongbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.model.QuestionWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Insert
    suspend fun insert(question: QuestionEntity): Long

    @Update
    suspend fun update(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id")
    fun observeById(id: Long): Flow<QuestionEntity?>

    @Transaction
    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    fun observeAllWithTags(): Flow<List<QuestionWithTags>>

    @Transaction
    @Query("SELECT * FROM questions WHERE id = :id")
    fun observeByIdWithTags(id: Long): Flow<QuestionWithTags?>

    /**
     * 关键词搜索：题干（AI 整理后的）与 OCR 原文都参与匹配。
     * 注意：LIKE 对大小写不敏感仅限 ASCII，中文无影响。
     */
    @Query(
        """
        SELECT * FROM questions
        WHERE (:keyword = ''
               OR refinedText LIKE '%' || :keyword || '%'
               OR rawOcrText LIKE '%' || :keyword || '%'
               OR answer LIKE '%' || :keyword || '%')
        ORDER BY createdAt DESC
        """,
    )
    fun search(keyword: String): Flow<List<QuestionEntity>>

    /**
     * 多标签筛选。
     *
     * @param tagCount 0 = 不筛选；AND 语义传归一化后的标签数；OR 语义传 1。
     *                 HAVING COUNT(DISTINCT tagId) >= tagCount 同时覆盖两种语义。
     */
    @Query(
        """
        SELECT * FROM questions
        WHERE :tagCount = 0
           OR id IN (
                SELECT questionId FROM question_tag_cross_ref
                WHERE tagId IN (:tagIds)
                GROUP BY questionId
                HAVING COUNT(DISTINCT tagId) >= :tagCount
           )
        ORDER BY createdAt DESC
        """,
    )
    fun observeByTags(tagIds: List<Long>, tagCount: Int): Flow<List<QuestionEntity>>


    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE rawOcrText = :text AND createdAt = :createdAt")
    suspend fun countByContent(text: String, createdAt: Long): Int

    @Query("SELECT masteryLevel AS masteryLevel, COUNT(*) AS count FROM questions GROUP BY masteryLevel")
    suspend fun masteryCounts(): List<com.chenzixin.wrongbook.data.local.model.MasteryCount>

}
