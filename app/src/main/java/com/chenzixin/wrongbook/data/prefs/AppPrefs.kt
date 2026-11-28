package com.chenzixin.wrongbook.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chenzixin.wrongbook.domain.update.UpdateChannel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "app_prefs")

/**
 * 应用级开关：开屏动画、首次使用引导、更新通道、AI 联网搜题。
 *
 * 不放 ThemePrefs 里，是为了让「外观」和「行为」各自独立演进。
 */
@Singleton
class AppPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val splashKey = booleanPreferencesKey("splash_enabled")
    private val channelKey = stringPreferencesKey("update_channel")
    private val guideSeenKey = booleanPreferencesKey("guide_seen")
    private val aiSearchKey = booleanPreferencesKey("ai_web_search")
    private val srKey = booleanPreferencesKey("super_resolution")

    /** 是否播放开屏动画，默认开。 */
    val splashEnabled: Flow<Boolean> = context.appDataStore.data.map { it[splashKey] ?: true }

    /**
     * 版本计划（更新通道），默认 [UpdateChannel.BETA]。
     *
     * 旧版本只有 `beta_channel` 布尔值；那个键不再读取——Beta 现在是默认值，
     * 老用户升级上来行为不变，也不会因为键消失而崩溃。
     */
    val updateChannel: Flow<UpdateChannel> = context.appDataStore.data.map {
        UpdateChannel.fromKey(it[channelKey])
    }

    /** AI 出题时是否允许模型调用联网检索，默认开。 */
    val aiWebSearch: Flow<Boolean> = context.appDataStore.data.map { it[aiSearchKey] ?: true }

    /**
     * 题目插图「高清重绘」是否用 AI 超分（EDSR×4），默认开。
     *
     * 关掉后只用 Lanczos + 反锐化，秒出；开启时小图会慢几秒但边缘更利。
     */
    val superResolution: Flow<Boolean> = context.appDataStore.data.map { it[srKey] ?: true }

    /**
     * 是否已经看过首次使用的用户指南，默认 false。
     *
     * 只在「看完」或「跳过」时置 true；升级上来的老用户没有这个键，会看一次，可跳过。
     */
    val guideSeen: Flow<Boolean> = context.appDataStore.data.map { it[guideSeenKey] ?: false }

    suspend fun setSplashEnabled(value: Boolean) {
        context.appDataStore.edit { it[splashKey] = value }
    }

    suspend fun setUpdateChannel(channel: UpdateChannel) {
        context.appDataStore.edit { it[channelKey] = channel.name }
    }

    suspend fun setAiWebSearch(value: Boolean) {
        context.appDataStore.edit { it[aiSearchKey] = value }
    }

    suspend fun setSuperResolution(value: Boolean) {
        context.appDataStore.edit { it[srKey] = value }
    }

    suspend fun markGuideSeen() {
        context.appDataStore.edit { it[guideSeenKey] = true }
    }

    /** 设置 → 使用手册 → 重看快速引导。 */
    suspend fun resetGuideSeen() {
        context.appDataStore.edit { it[guideSeenKey] = false }
    }
}
