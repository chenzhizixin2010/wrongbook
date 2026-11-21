package com.chenzixin.wrongbook.di

import com.chenzixin.wrongbook.data.ocr.MlKitOcrEngine
import com.chenzixin.wrongbook.data.ocr.OcrEngine
import com.chenzixin.wrongbook.data.remote.LlmClient
import com.chenzixin.wrongbook.data.remote.OpenAiCompatLlmClient
import com.chenzixin.wrongbook.data.repository.ChatRepository
import com.chenzixin.wrongbook.data.repository.ChatRepositoryImpl
import com.chenzixin.wrongbook.data.repository.QuestionRepository
import com.chenzixin.wrongbook.data.repository.QuestionRepositoryImpl
import com.chenzixin.wrongbook.data.repository.SettingsRepository
import com.chenzixin.wrongbook.data.repository.SettingsRepositoryImpl
import com.chenzixin.wrongbook.data.repository.TagRepository
import com.chenzixin.wrongbook.data.repository.TagRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindQuestionRepository(impl: QuestionRepositoryImpl): QuestionRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindLlmClient(impl: OpenAiCompatLlmClient): LlmClient

    @Binds
    @Singleton
    abstract fun bindOcrEngine(impl: MlKitOcrEngine): OcrEngine
}
