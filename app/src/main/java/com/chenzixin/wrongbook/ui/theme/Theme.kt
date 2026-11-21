package com.chenzixin.wrongbook.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.robinpcrd.cupertino.theme.CupertinoTheme
import io.github.robinpcrd.cupertino.theme.darkColorScheme
import io.github.robinpcrd.cupertino.theme.lightColorScheme

/**
 * 设计令牌。
 *
 * 分两层：
 * 1) 组件库内部配色 —— 交给 CupertinoTheme（按钮、导航栏、对话框等自带配色）；
 * 2) 我们自己的容器 —— 分组卡片、分隔线、标题/副标题文字，用 WbColors。
 *
 * 这样做的好处：将来若放弃 Cupertino 换成纯 Material 3 或自绘，
 * 只需要改这一个文件，业务代码里不出现任何 Cupertino 颜色常量。
 */
data class WbColors(
    val tint: Color,
    val groupedBackground: Color,
    val cardBackground: Color,
    val separator: Color,
    val label: Color,
    val secondaryLabel: Color,
    val danger: Color,
)

private val LightColors = WbColors(
    tint = Color(0xFF007AFF),
    groupedBackground = Color(0xFFF2F2F7),
    cardBackground = Color(0xFFFFFFFF),
    separator = Color(0x1F3C3C43),
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    danger = Color(0xFFFF3B30),
)

private val DarkColors = WbColors(
    tint = Color(0xFF0A84FF),
    groupedBackground = Color(0xFF000000),
    cardBackground = Color(0xFF1C1C1E),
    separator = Color(0x2E545458),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    danger = Color(0xFFFF453A),
)

val LocalWbColors = staticCompositionLocalOf { LightColors }

/** 在 Composable 里取当前配色，跟随 [WrongBookTheme] 的深浅色。 */
@Composable
fun wbColors(): WbColors = LocalWbColors.current

@Composable
fun WrongBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalWbColors provides if (darkTheme) DarkColors else LightColors) {
        CupertinoTheme(
            colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
            content = content,
        )
    }
}
