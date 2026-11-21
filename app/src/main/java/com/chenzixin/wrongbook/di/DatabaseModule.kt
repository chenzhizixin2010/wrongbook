package com.chenzixin.wrongbook.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chenzixin.wrongbook.data.local.WrongBookDatabase
import com.chenzixin.wrongbook.data.local.dao.ApiProfileDao
import com.chenzixin.wrongbook.data.local.dao.ChatDao
import com.chenzixin.wrongbook.data.local.dao.QuestionDao
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
            // 外键约束必须在建库时显式打开，否则 CASCADE 不生效
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            })
            .build()

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
