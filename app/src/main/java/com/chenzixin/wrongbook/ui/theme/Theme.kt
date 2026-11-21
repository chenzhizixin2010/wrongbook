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
 * 设计令牌 —— 「液态玻璃」视觉体系。
 *
 * 分两层：
 * 1) 组件库内部配色 —— 交给 CupertinoTheme（按钮、导航栏、对话框等自带配色）；
 * 2) 我们自己画的容器 —— 极光背景、玻璃面板、标签、按钮渐变，统一走 [WbColors]。
 *
 * 玻璃的观感由三件事决定，缺一不可：
 * - 底：会呼吸的彩色极光画布（[com.chenzixin.wrongbook.ui.component.WbAmbientBackground]）；
 * - 面：半透明填充 + 边缘高光 + 底部内阴影（[com.chenzixin.wrongbook.ui.component.GlassSurface]）；
 * - 光：从左上打来的镜面反光与彩色光晕（按钮、悬浮标签栏）。
 *
 * 业务代码只引用 [wbColors] 里的语义色，不出现任何裸色值。
 */
data class WbColors(
    // —— 兼容层：既有页面仍在用的语义色 ——
    val tint: Color,
    val groupedBackground: Color,
    val cardBackground: Color,
    val separator: Color,
    val label: Color,
    val secondaryLabel: Color,
    val danger: Color,

    // —— 主题标识 ——
    val isDark: Boolean,

    // —— 强调色（渐变按钮的起止）——
    val accent: Color,
    val accentEnd: Color,
    val success: Color,
    val warning: Color,
    val onAccent: Color,

    // —— 画布：极光 ——
    val canvasTop: Color,
    val canvasBottom: Color,
    val aurora: List<Color>,

    // —— 玻璃材质 ——
    /** 玻璃填充：上亮下暗，模拟厚度。 */
    val glassTop: Color,
    val glassBottom: Color,

    /** 玻璃边缘：左上受光、右下背光。 */
    val glassStrokeTop: Color,
    val glassStrokeBottom: Color,

    /** 镜面高光 / 可点击区域的浅色底。 */
    val glassSheen: Color,
    val glassFill: Color,

    /** 投影颜色（带一点蓝，避免死灰）。 */
    val shadow: Color,

    /** 强调色的低透明度版本，用于选中胶囊、图标底板。 */
    val tintSoft: Color,

    /** 悬浮标签栏的底衬：没有真实背景模糊时，靠它保证栏上文字可读。 */
    val barScrim: Color,

    /** 对话框底衬：比标签栏更实，避免页面文字透进弹窗。 */
    val dialogScrim: Color,
)

private val LightColors = WbColors(
    tint = Color(0xFF0A84FF),
    groupedBackground = Color(0x0F1B2A5E),
    cardBackground = Color(0xB3FFFFFF),
    separator = Color(0x143C3C43),
    label = Color(0xFF0B0B12),
    secondaryLabel = Color(0xFF6B6B7B),
    danger = Color(0xFFFF3B30),

    isDark = false,

    accent = Color(0xFF0A84FF),
    accentEnd = Color(0xFF6C5CE7),
    success = Color(0xFF34C759),
    warning = Color(0xFFFF9F0A),
    onAccent = Color.White,

    canvasTop = Color(0xFFF5F7FD),
    canvasBottom = Color(0xFFE9EDF8),
    aurora = listOf(
        Color(0x666AA6FF),
        Color(0x59C08CFF),
        Color(0x4D4FE0D0),
        Color(0x40FFB3D1),
    ),

    glassTop = Color(0xA8FFFFFF),
    glassBottom = Color(0x6BFFFFFF),
    glassStrokeTop = Color(0xF2FFFFFF),
    glassStrokeBottom = Color(0x33FFFFFF),
    glassSheen = Color(0xCCFFFFFF),
    glassFill = Color(0x99FFFFFF),
    shadow = Color(0x1A1B2A5E),
    tintSoft = Color(0x1F0A84FF),
    barScrim = Color(0xE2FFFFFF),
    dialogScrim = Color(0xFFFFFFFF),
)

private val DarkColors = WbColors(
    tint = Color(0xFF4C9AFF),
    groupedBackground = Color(0x14FFFFFF),
    cardBackground = Color(0x1FFFFFFF),
    separator = Color(0x1FFFFFFF),
    label = Color(0xFFF5F5FA),
    secondaryLabel = Color(0xFF9C9CB0),
    danger = Color(0xFFFF453A),

    isDark = true,

    accent = Color(0xFF3D8BFF),
    accentEnd = Color(0xFF9B7BFF),
    success = Color(0xFF30D158),
    warning = Color(0xFFFFB340),
    onAccent = Color.White,

    canvasTop = Color(0xFF08090F),
    canvasBottom = Color(0xFF04050A),
    aurora = listOf(
        Color(0xCC0B2A6F),
        Color(0xB32B1466),
        Color(0x99123F3C),
        Color(0x661A0D3D),
    ),

    glassTop = Color(0x29FFFFFF),
    glassBottom = Color(0x0DFFFFFF),
    glassStrokeTop = Color(0x73FFFFFF),
    glassStrokeBottom = Color(0x14FFFFFF),
    glassSheen = Color(0x4DFFFFFF),
    glassFill = Color(0x1AFFFFFF),
    shadow = Color(0x99000000),
    tintSoft = Color(0x2E4C9AFF),
    barScrim = Color(0xF00B0C12),
    dialogScrim = Color(0xFF0A0B11),
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
