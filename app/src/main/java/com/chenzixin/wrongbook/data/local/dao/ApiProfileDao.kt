package com.chenzixin.wrongbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiProfileDao {

    @Insert
    suspend fun insert(profile: ApiProfileEntity): Long

    @Update
    suspend fun update(profile: ApiProfileEntity)

    @Query("DELETE FROM api_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM api_profiles ORDER BY isDefault DESC, createdAt ASC")
    fun observeAll(): Flow<List<ApiProfileEntity>>

    @Query("SELECT * FROM api_profiles ORDER BY isDefault DESC, createdAt ASC")
    suspend fun getAllOnce(): List<ApiProfileEntity>

    @Query("SELECT * FROM api_profiles WHERE id = :id")
    suspend fun findById(id: Long): ApiProfileEntity?

    @Query("SELECT * FROM api_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun findDefault(): ApiProfileEntity?

    @Query("UPDATE api_profiles SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE api_profiles SET isDefault = 1 WHERE id = :id")
    suspend fun markDefault(id: Long)

    @Query("SELECT COUNT(*) FROM api_profiles")
    suspend fun count(): Int
}
