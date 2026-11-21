package com.chenzixin.wrongbook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chenzixin.wrongbook.data.local.dao.ApiProfileDao
import com.chenzixin.wrongbook.data.local.dao.ChatDao
import com.chenzixin.wrongbook.data.local.dao.QuestionDao
import com.chenzixin.wrongbook.data.local.dao.QuestionTagCrossRefDao
import com.chenzixin.wrongbook.data.local.dao.TagDao
import com.chenzixin.wrongbook.data.local.entity.ApiProfileEntity
import com.chenzixin.wrongbook.data.local.entity.ChatMessageEntity
import com.chenzixin.wrongbook.data.local.entity.ChatSessionEntity
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.entity.QuestionTagCrossRef
import com.chenzixin.wrongbook.data.local.entity.TagEntity

/**
 * 数据库。
 *
 * 约定：
 * - exportSchema = true，schema 导出到 app/schemas（见 app/build.gradle.kts 的 ksp 参数）；
 * - 每次改表必须写 Migration 并升 version，禁止 fallbackToDestructiveMigration（会丢用户错题）。
 */
@Database(
    entities = [
        QuestionEntity::class,
        TagEntity::class,
        QuestionTagCrossRef::class,
        ApiProfileEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class WrongBookDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao

    abstract fun tagDao(): TagDao

    abstract fun questionTagCrossRefDao(): QuestionTagCrossRefDao

    abstract fun apiProfileDao(): ApiProfileDao
    abstract fun chatDao(): ChatDao
}
