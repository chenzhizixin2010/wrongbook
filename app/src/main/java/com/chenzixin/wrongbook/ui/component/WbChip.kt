package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 可选中的筛选胶囊：纯色 primary / surfaceContainerHigh，无渐变无阴影。 */
@Composable
fun WbChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLight: Boolean = false,
) {
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS
    val v090 = style == UiStyle.V090
    val c = MiuixTheme.colorScheme
    val wb = wbColors()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    v090 && selected -> Brush.linearGradient(listOf(wb.accent, wb.accentEnd))
                    v090 -> SolidColor(wb.tintSoft)
                    ios -> SolidColor(if (selected) wb.accent else wb.tintSoft)
                    else -> SolidColor(if (selected) c.primary else c.surfaceContainerHigh)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WbText(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (ios || v090) {
                if (selected) wb.onAccent else wb.accent
            } else {
                if (selected) c.onPrimary else c.onSurface
            },
            maxLines = 1,
        )
    }
}

/** 只读小徽标（标签、状态）。 */
@Composable
fun WbBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS
    val v090 = style == UiStyle.V090
    val c = MiuixTheme.colorScheme
    val wb = wbColors()
    val base = color ?: if (ios) wb.accent else c.primary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (color == null) (if (ios) wb.tintSoft else c.primaryContainer) else base.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WbText(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (color == null) (if (ios) wb.accent else c.onPrimaryContainer) else base,
            maxLines = 1,
        )
    }
}
