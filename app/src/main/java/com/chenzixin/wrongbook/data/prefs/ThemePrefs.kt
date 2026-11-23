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

/** 两套真正独立的界面语言：澎湃 OS（Miuix）与 iOS（Cupertino + 轻玻璃）。 */
enum class UiStyle(val label: String, val summary: String) {
    MIUIX("澎湃 OS", "HyperOS 内嵌列表、贴底导航、纯色卡片"),
    IOS("iOS", "Cupertino 分组列表、玻璃材质、SF 风格字阶"),
    V090("0.9.0 液态玻璃", "极光背景、悬浮胶囊导航、蓝紫渐变按钮"),
    MD3E("MD3 Expressive", "Material 3 Expressive：药丸按钮、大圆角、动态色、弹性导航"),
}

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

/** 外观设置：深浅色 + 界面风格；独立于 API 配置，避免每次改主题都碰数据库。 */
@Singleton
class ThemePrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val modeKey = stringPreferencesKey("theme_mode")
    private val styleKey = stringPreferencesKey("ui_style")

    val mode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        when (prefs[modeKey]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val uiStyle: Flow<UiStyle> = context.themeDataStore.data.map { prefs ->
        when (prefs[styleKey]) {
            UiStyle.IOS.name -> UiStyle.IOS
            UiStyle.V090.name -> UiStyle.V090
            UiStyle.MD3E.name -> UiStyle.MD3E
            else -> UiStyle.MIUIX
        }
    }

    suspend fun setMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[modeKey] = mode.name }
    }

    suspend fun setUiStyle(style: UiStyle) {
        context.themeDataStore.edit { it[styleKey] = style.name }
    }
}
