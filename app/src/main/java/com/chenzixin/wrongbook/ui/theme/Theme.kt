package com.chenzixin.wrongbook.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme as M3ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes as M3Shapes
import androidx.compose.material3.Typography as M3Typography
import androidx.compose.material3.darkColorScheme as m3DarkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme as m3LightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    val md3e = uiStyle == UiStyle.MD3E
    val controller = remember(darkTheme, uiStyle) {
        ThemeController(
            colorSchemeMode = if (ios || v090 || md3e) {
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
    val context = LocalContext.current
    val m3Scheme = remember(darkTheme, md3e, context) {
        if (md3e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) md3eDarkScheme() else md3eLightScheme()
            }
        } else {
            if (darkTheme) m3DarkColorScheme() else m3LightColorScheme()
        }
    }
    val m3Shapes = remember {
        M3Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp),
        )
    }
    val m3Typography = remember { M3Typography() }

    // 三套 provider 始终存在，只切换取值；内容树位置不变，切风格不会重置导航。
    MiuixTheme(controller = controller, textStyles = textStyles) {
        CupertinoTheme(
            colorScheme = scheme,
            shapes = Shapes(),
            typography = Typography(),
        ) {
            MaterialTheme(
                colorScheme = m3Scheme,
                shapes = m3Shapes,
                typography = m3Typography,
            ) {
                val colors = when {
                    md3e -> md3eColors(m3Scheme, darkTheme)
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

private fun md3eLightScheme(): M3ColorScheme = m3LightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private fun md3eDarkScheme(): M3ColorScheme = m3DarkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF332D41),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

private fun md3eColors(scheme: M3ColorScheme, dark: Boolean): WbColors {
    val primary = scheme.primary
    val onPrimary = scheme.onPrimary
    val primaryContainer = scheme.primaryContainer
    val secondaryContainer = scheme.secondaryContainer
    val tertiary = scheme.tertiary
    val tertiaryContainer = scheme.tertiaryContainer
    val surface = scheme.surface
    val surfaceContainer = scheme.surfaceContainer
    val surfaceContainerHigh = scheme.surfaceContainerHigh
    val onSurface = scheme.onSurface
    val onSurfaceVariant = scheme.onSurfaceVariant
    val outlineVariant = scheme.outlineVariant
    val error = scheme.error

    return WbColors(
        tint = primary,
        groupedBackground = surfaceContainer,
        cardBackground = surfaceContainer,
        separator = outlineVariant,
        label = onSurface,
        secondaryLabel = onSurfaceVariant,
        danger = error,

        isDark = dark,

        accent = primary,
        accentEnd = tertiary,
        success = if (dark) Color(0xFFB5CC9C) else Color(0xFF386A20),
        warning = if (dark) Color(0xFFE8C48C) else Color(0xFF7D5700),
        onAccent = onPrimary,

        canvasTop = surface,
        canvasBottom = if (dark) Color(0xFF1B1720) else Color(0xFFF7F2FA),
        aurora = listOf(primaryContainer, secondaryContainer, tertiaryContainer, primaryContainer),

        glassTop = surfaceContainerHigh,
        glassBottom = surfaceContainerHigh,
        glassStrokeTop = outlineVariant,
        glassStrokeBottom = outlineVariant,
        glassSheen = surfaceContainerHigh,
        glassFill = surfaceContainer,
        shadow = Color.Black.copy(alpha = if (dark) 0.40f else 0.10f),
        tintSoft = primaryContainer,
        barScrim = surfaceContainer,
        dialogScrim = Color.Black.copy(alpha = 0.32f),
    )
}

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
