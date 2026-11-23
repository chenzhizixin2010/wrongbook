package com.chenzixin.wrongbook.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "app_prefs")

/**
 * 应用级开关：开屏动画、Beta 更新通道。
 *
 * 不放 ThemePrefs 里，是为了让「外观」和「行为」各自独立演进。
 */
@Singleton
class AppPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val splashKey = booleanPreferencesKey("splash_enabled")
    private val betaKey = booleanPreferencesKey("beta_channel")

    /** 是否播放开屏动画，默认开。 */
    val splashEnabled: Flow<Boolean> = context.appDataStore.data.map { it[splashKey] ?: true }

    /** 是否接收 GitHub 上的预发布版本（pre-release），默认关。 */
    val betaChannel: Flow<Boolean> = context.appDataStore.data.map { it[betaKey] ?: false }

    suspend fun setSplashEnabled(value: Boolean) {
        context.appDataStore.edit { it[splashKey] = value }
    }

    suspend fun setBetaChannel(value: Boolean) {
        context.appDataStore.edit { it[betaKey] = value }
    }
}
