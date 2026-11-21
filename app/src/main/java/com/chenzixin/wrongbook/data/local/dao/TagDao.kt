package com.chenzixin.wrongbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

   /** 名称唯一：冲突时直接抛，由 Repository 转成「标签已存在」文案。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE parentId IS NULL ORDER BY sortOrder ASC, name ASC")
    fun observeRoots(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE parentId = :parentId ORDER BY sortOrder ASC, name ASC")
    fun observeChildren(parentId: Long): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun findById(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Query(
        """
        SELECT t.name AS name, COUNT(c.questionId) AS count
        FROM tags t LEFT JOIN question_tag_cross_ref c ON c.tagId = t.id
        GROUP BY t.id
        ORDER BY count DESC, t.name ASC
        LIMIT :limit
        """,
    )
    suspend fun tagCounts(limit: Int): List<com.chenzixin.wrongbook.data.local.model.TagCount>

    @Query("SELECT COUNT(*) FROM question_tag_cross_ref WHERE tagId = :tagId")
    suspend fun countQuestions(tagId: Long): Int
}
