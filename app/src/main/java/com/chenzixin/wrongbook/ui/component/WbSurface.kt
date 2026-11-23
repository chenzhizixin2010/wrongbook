package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 页面背景：Miuix 纯色；iOS 柔和极光渐变 + 两团光晕。 */
@Composable
fun WbScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val style = wbUiStyle()
    val glass = style == UiStyle.IOS || style == UiStyle.V090
    val v090 = style == UiStyle.V090
    val md3e = style == UiStyle.MD3E
    val c = wbColors()
    if (md3e) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(c.canvasTop)
                    val colors = c.aurora
                    if (colors.isNotEmpty()) {
                        drawCircle(
                            color = colors[0].copy(alpha = if (c.isDark) 0.45f else 0.70f),
                            radius = size.width * 0.62f,
                            center = Offset(size.width * 0.08f, size.height * 0.10f),
                        )
                        if (colors.size > 1) {
                            drawCircle(
                                color = colors[1].copy(alpha = if (c.isDark) 0.38f else 0.55f),
                                radius = size.width * 0.66f,
                                center = Offset(size.width * 0.98f, size.height * 0.28f),
                            )
                        }
                        if (colors.size > 2) {
                            drawRoundRect(
                                color = colors[2].copy(alpha = if (c.isDark) 0.28f else 0.42f),
                                topLeft = Offset(-size.width * 0.15f, size.height * 0.78f),
                                size = Size(size.width * 0.8f, size.height * 0.42f),
                                cornerRadius = CornerRadius(size.width * 0.35f),
                            )
                        }
                    }
                },
            content = content,
        )
        return
    }
    if (!glass) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
            content = content,
        )
        return
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(listOf(c.canvasTop, c.canvasBottom)),
                    size = Size(size.width, size.height),
                )
                val colors = c.aurora
                if (colors.isNotEmpty()) {
                    val blend = if (v090 && c.isDark) BlendMode.Screen else BlendMode.SrcOver
                    val boost = if (v090) 1.35f else 1f
                    fun blob(color: Color, alpha: Float, cx: Float, cy: Float, r: Float) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color.copy(alpha = (alpha * boost).coerceAtMost(1f)), Color.Transparent),
                                center = Offset(size.width * cx, size.height * cy),
                                radius = size.width * r,
                            ),
                            radius = size.width * r,
                            center = Offset(size.width * cx, size.height * cy),
                            blendMode = blend,
                        )
                    }
                    blob(colors[0], if (v090) 0.70f else 0.55f, 0.06f, 0.06f, 0.95f)
                    if (colors.size > 1) blob(colors[1], if (v090) 0.62f else 0.42f, 0.98f, 0.18f, 0.85f)
                    if (colors.size > 2) blob(colors[2], if (v090) 0.52f else 0.35f, 0.18f, 0.92f, 0.75f)
                    if (colors.size > 3) blob(colors[3], if (v090) 0.55f else 0.35f, 0.86f, 0.88f, 0.8f)
                }
            },
        content = content,
    )
}

/**
 * 分组容器。
 *
 * Miuix：纯色 surface，无渐变、无模糊。
 * iOS：轻玻璃 — 半透明白/黑渐变 + 1dp 高光描边 + 柔和投影；只在 iOS 皮肤内启用。
 */
@Composable
fun WbSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    tint: Color? = null,
    underlay: Color? = null,
    elevation: Dp = 0.dp,
    glow: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS
    val v090 = style == UiStyle.V090
    val md3e = style == UiStyle.MD3E
    val c = wbColors()
    val shape = RoundedCornerShape(cornerRadius)

    if (!ios && !v090) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(tint ?: underlay ?: if (md3e) c.cardBackground else MiuixTheme.colorScheme.surfaceContainer)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(contentPadding),
            content = content,
        )
        return
    }

    val solid = tint ?: underlay
    Box(
        modifier = modifier
            .shadow(
                elevation = if (elevation > 0.dp) elevation else 10.dp,
                shape = shape,
                ambientColor = c.shadow,
                spotColor = c.shadow,
            )
            .clip(shape)
            .background(
                if (solid != null) {
                    Brush.verticalGradient(listOf(solid, solid))
                } else {
                    Brush.verticalGradient(listOf(c.glassTop, c.glassBottom))
                },
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(c.glassStrokeTop, c.glassStrokeBottom),
                ),
                shape = shape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        // 顶部高光 + 极光染色，模拟液态玻璃
        if (solid == null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            0f to c.glassSheen,
                            0.35f to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    ),
            )
            if (v090) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(c.accent.copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(0f, 0f),
                                radius = 520f,
                            ),
                        ),
                )
            }
        }
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

/** 圆形图标按钮：Miuix 为实心/透明 IconButton；iOS 为玻璃圆钮。 */
@Composable
fun WbIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    tint: Color? = null,
    containerColor: Color? = null,
) {
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS || style == UiStyle.V090
    val md3e = style == UiStyle.MD3E
    val c = wbColors()
    if (md3e) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(50))
                .background(containerColor ?: c.tintSoft)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            WbIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = tint ?: c.accent,
            )
        }
    } else if (ios) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(50))
                .background(containerColor ?: c.glassTop)
                .border(1.dp, c.glassStrokeTop, RoundedCornerShape(50))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            WbIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = tint ?: c.accent,
            )
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(size),
            backgroundColor = containerColor ?: Color.Transparent,
        ) {
            WbIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = tint ?: MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

/** 图标底板：Miuix 小色块；iOS 圆角 tintSoft。 */
@Composable
fun WbIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    corner: Dp = 9.dp,
    color: Color? = null,
) {
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS || style == UiStyle.V090
    val md3e = style == UiStyle.MD3E
    val c = wbColors()
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(if (ios || md3e) 50.dp else corner))
            .background(color ?: if (ios || md3e) c.tintSoft else MiuixTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        WbIcon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(size * 0.55f),
            tint = if (ios || md3e) c.accent else MiuixTheme.colorScheme.onPrimaryContainer,
        )
    }
}
