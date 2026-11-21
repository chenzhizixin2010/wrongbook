package com.chenzixin.wrongbook.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
}

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

/** 外观设置：只有「系统/浅色/深色」一项，独立于 API 配置，避免每次改主题都碰数据库。 */
@Singleton
class ThemePrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("theme_mode")

    val mode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        when (prefs[key]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[key] = mode.name }
    }
}
