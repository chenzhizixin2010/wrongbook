package com.chenzixin.wrongbook.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.data.prefs.UiStyle
import io.github.robinpcrd.cupertino.theme.ColorScheme
import io.github.robinpcrd.cupertino.theme.CupertinoTheme
import io.github.robinpcrd.cupertino.theme.Shapes
import io.github.robinpcrd.cupertino.theme.Typography
import io.github.robinpcrd.cupertino.theme.darkColorScheme
import io.github.robinpcrd.cupertino.theme.lightColorScheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.defaultTextStyles

/**
 * 设计令牌。
 *
 * Miuix 皮肤：映射 `MiuixTheme.colorScheme`（含 Monet）。
 * iOS 皮肤：映射 Cupertino 的 ColorScheme，并补一组玻璃/极光令牌；
 * 这些令牌只在 iOS 皮肤内生效，Miuix 侧仍为纯色。
 */
data class WbColors(
    val tint: Color,
    val groupedBackground: Color,
    val cardBackground: Color,
    val separator: Color,
    val label: Color,
    val secondaryLabel: Color,
    val danger: Color,

    val isDark: Boolean,

    val accent: Color,
    val accentEnd: Color,
    val success: Color,
    val warning: Color,
    val onAccent: Color,

    val canvasTop: Color,
    val canvasBottom: Color,
    val aurora: List<Color>,

    val glassTop: Color,
    val glassBottom: Color,
    val glassStrokeTop: Color,
    val glassStrokeBottom: Color,
    val glassSheen: Color,
    val glassFill: Color,
    val shadow: Color,
    val tintSoft: Color,
    val barScrim: Color,
    val dialogScrim: Color,
)

val LocalWbColors = staticCompositionLocalOf<WbColors> {
    error("WbColors 未提供：请用 WrongBookTheme 包裹页面")
}

val LocalUiStyle = staticCompositionLocalOf { UiStyle.MIUIX }

@Composable
fun wbColors(): WbColors = LocalWbColors.current

@Composable
fun wbUiStyle(): UiStyle = LocalUiStyle.current

@Composable
fun WrongBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    uiStyle: UiStyle = UiStyle.MIUIX,
    content: @Composable () -> Unit,
) {
    val ios = uiStyle == UiStyle.IOS
    val v090 = uiStyle == UiStyle.V090
    val controller = remember(darkTheme, uiStyle) {
        ThemeController(
            colorSchemeMode = if (ios || v090) {
                if (darkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light
            } else {
                ColorSchemeMode.MonetSystem
            },
            isDark = darkTheme,
        )
    }
    val textStyles = remember {
        defaultTextStyles(
            title1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
            title2 = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
            title3 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
            title4 = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
        )
    }
    val scheme = if (darkTheme) darkColorScheme() else lightColorScheme()

    // 两套 provider 始终存在，只切换取值；内容树位置不变，切风格不会重置导航。
    MiuixTheme(controller = controller, textStyles = textStyles) {
        CupertinoTheme(
            colorScheme = scheme,
            shapes = Shapes(),
            typography = Typography(),
        ) {
            val colors = when {
                v090 -> v090Colors(darkTheme)
                ios -> iosColors(scheme, darkTheme)
                else -> miuixColors(MiuixTheme.colorScheme, darkTheme)
            }
            CompositionLocalProvider(
                LocalWbColors provides colors,
                LocalUiStyle provides uiStyle,
            ) {
                content()
            }
        }
    }
}

private fun miuixColors(c: Colors, dark: Boolean): WbColors = WbColors(
    tint = c.primary,
    groupedBackground = c.surfaceContainer,
    cardBackground = c.surface,
    separator = c.dividerLine,
    label = c.onBackground,
    secondaryLabel = c.onSurfaceVariantSummary,
    danger = c.error,

    isDark = dark,

    accent = c.primary,
    accentEnd = c.primary,
    success = c.primary,
    warning = c.secondary,
    onAccent = c.onPrimary,

    canvasTop = c.background,
    canvasBottom = c.background,
    aurora = emptyList(),

    glassTop = c.surface,
    glassBottom = c.surface,
    glassStrokeTop = c.outline,
    glassStrokeBottom = c.outline,
    glassSheen = c.surface,
    glassFill = c.surfaceContainerHigh,
    shadow = c.outline,
    tintSoft = c.primaryContainer,
    barScrim = c.surface,
    dialogScrim = Color.Black.copy(alpha = 0.32f),
)

private fun v090Colors(dark: Boolean): WbColors {
    val accent = if (dark) Color(0xFF6EA8FF) else Color(0xFF3867F5)
    val accentEnd = if (dark) Color(0xFFA78BFA) else Color(0xFF8B5CF6)
    val label = if (dark) Color(0xFFF8FAFC) else Color(0xFF111827)
    val secondary = if (dark) Color(0xFF94A3B8) else Color(0xFF667085)
    return WbColors(
        tint = accent,
        groupedBackground = if (dark) Color(0xFF0B1020) else Color(0xFFEEF2FF),
        cardBackground = if (dark) Color(0xFF161C2E) else Color.White,
        separator = if (dark) Color(0xFF2A3350) else Color(0xFFD6D9E8),
        label = label,
        secondaryLabel = secondary,
        danger = if (dark) Color(0xFFFF6B6B) else Color(0xFFFF3B30),

        isDark = dark,

        accent = accent,
        accentEnd = accentEnd,
        success = if (dark) Color(0xFF34D399) else Color(0xFF10B981),
        warning = if (dark) Color(0xFFFBBF24) else Color(0xFFF59E0B),
        onAccent = Color.White,

        canvasTop = if (dark) Color(0xFF0B1226) else Color(0xFFEAF2FF),
        canvasBottom = if (dark) Color(0xFF1A0B2E) else Color(0xFFFDEBFF),
        aurora = if (dark) {
            listOf(
                Color(0xFF2563EB),
                Color(0xFF7C3AED),
                Color(0xFF059669),
                Color(0xFFDB2777),
            )
        } else {
            listOf(
                Color(0xFF93C5FD),
                Color(0xFFC4B5FD),
                Color(0xFF6EE7B7),
                Color(0xFFF9A8D4),
            )
        },

        glassTop = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.78f),
        glassBottom = if (dark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.52f),
        glassStrokeTop = if (dark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.92f),
        glassStrokeBottom = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.42f),
        glassSheen = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.42f),
        glassFill = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.38f),
        shadow = Color.Black.copy(alpha = if (dark) 0.52f else 0.16f),
        tintSoft = accent.copy(alpha = if (dark) 0.24f else 0.12f),
        barScrim = if (dark) Color(0xFF0B1020).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.62f),
        dialogScrim = Color.Black.copy(alpha = 0.30f),
    )
}

private fun iosColors(scheme: ColorScheme, dark: Boolean): WbColors {
    val red = if (dark) Color(0xFFFF453A) else Color(0xFFFF3B30)
    val green = if (dark) Color(0xFF30D158) else Color(0xFF34C759)
    val orange = if (dark) Color(0xFFFFD60A) else Color(0xFFFF9F0A)
    val indigo = if (dark) Color(0xFF5E5CE6) else Color(0xFF5856D6)
    val glassTop = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.74f)
    val glassBottom = if (dark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.52f)
    val strokeTop = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.85f)
    val strokeBottom = if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)

    return WbColors(
        tint = scheme.accent,
        groupedBackground = scheme.systemGroupedBackground,
        cardBackground = scheme.secondarySystemGroupedBackground,
        separator = scheme.separator,
        label = scheme.label,
        secondaryLabel = scheme.secondaryLabel,
        danger = red,

        isDark = dark,

        accent = scheme.accent,
        accentEnd = indigo,
        success = green,
        warning = orange,
        onAccent = Color.White,

        canvasTop = scheme.systemGroupedBackground,
        canvasBottom = scheme.systemBackground,
        aurora = if (dark) {
            listOf(
                Color(0xFF0B1220),
                Color(0xFF141024),
                Color(0xFF1A1010),
                Color(0xFF0B1A14),
            )
        } else {
            listOf(
                Color(0xFFEAF1FF),
                Color(0xFFF3ECFF),
                Color(0xFFFFF0E6),
                Color(0xFFE8FAF0),
            )
        },

        glassTop = glassTop,
        glassBottom = glassBottom,
        glassStrokeTop = strokeTop,
        glassStrokeBottom = strokeBottom,
        glassSheen = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.30f),
        glassFill = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.34f),
        shadow = Color.Black.copy(alpha = if (dark) 0.45f else 0.14f),
        tintSoft = scheme.accent.copy(alpha = if (dark) 0.22f else 0.12f),
        barScrim = scheme.systemBackground.copy(alpha = 0.86f),
        dialogScrim = Color.Black.copy(alpha = 0.32f),
    )
}
