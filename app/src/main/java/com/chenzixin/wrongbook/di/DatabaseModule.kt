package com.chenzixin.wrongbook.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chenzixin.wrongbook.data.local.WrongBookDatabase
import com.chenzixin.wrongbook.data.local.dao.ApiProfileDao
import com.chenzixin.wrongbook.data.local.dao.ChatDao
import com.chenzixin.wrongbook.data.local.dao.QuestionDao
import com.chenzixin.wrongbook.data.local.dao.QuestionImageDao
import com.chenzixin.wrongbook.data.local.dao.QuestionTagCrossRefDao
import com.chenzixin.wrongbook.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WrongBookDatabase =
        Room.databaseBuilder(context, WrongBookDatabase::class.java, "wrongbook.db")
            .addMigrations(MIGRATION_1_2)
            // 外键约束必须在建库时显式打开，否则 CASCADE 不生效
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            })
            .build()

    /**
     * v1 → v2：新增「题目插图」表。
     *
     * 只加表不动老数据，所以是纯增量迁移；老用户的错题一条都不会丢。
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `question_images` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `questionId` INTEGER NOT NULL,
                    `path` TEXT NOT NULL,
                    `thumbPath` TEXT NOT NULL,
                    `kind` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    FOREIGN KEY(`questionId`) REFERENCES `questions`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_images_questionId` ON `question_images` (`questionId`)")
        }
    }

    @Provides
    fun provideQuestionImageDao(db: WrongBookDatabase): QuestionImageDao = db.questionImageDao()

    @Provides
    fun provideQuestionDao(db: WrongBookDatabase): QuestionDao = db.questionDao()

    @Provides
    fun provideApiProfileDao(db: WrongBookDatabase): ApiProfileDao = db.apiProfileDao()

    @Provides
    fun provideChatDao(db: WrongBookDatabase): ChatDao = db.chatDao()

    @Provides
    fun provideTagDao(db: WrongBookDatabase): TagDao = db.tagDao()

    @Provides
    fun provideQuestionTagCrossRefDao(db: WrongBookDatabase): QuestionTagCrossRefDao =
        db.questionTagCrossRefDao()
}
