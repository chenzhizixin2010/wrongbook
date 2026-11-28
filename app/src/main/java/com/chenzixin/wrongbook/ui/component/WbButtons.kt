package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.ButtonDefaults as M3ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 语义化按钮。
 * Miuix：16dp 圆角实心 Button。
 * iOS：胶囊 filled / tinted / plain 三种形态。
 */

@Composable
fun WbPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: Int = 17,
    icon: ImageVector? = null,
) {
    val style = wbUiStyle()
    if (style == UiStyle.MD3E) {
        val colors = M3MaterialTheme.colorScheme
        M3Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 56.dp),
            enabled = enabled,
            shape = CircleShape,
            colors = M3ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ),
        ) {
            if (icon != null) {
                WbIcon(icon, null, Modifier.size(19.dp), colors.onPrimary)
                Spacer(Modifier.width(7.dp))
            }
            M3Text(text = text, style = M3MaterialTheme.typography.titleMedium)
        }
        return
    }
    if (style == UiStyle.IOS || style == UiStyle.V090) {
        val c = wbColors()
        IosButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            background = c.accent,
            contentColor = c.onAccent,
            icon = icon,
            filled = true,
            backgroundBrush = if (style == UiStyle.V090) {
                Brush.linearGradient(listOf(c.accent, c.accentEnd))
            } else {
                null
            },
        )
        return
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColorsPrimary(),
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            WbIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = LocalContentColor.current,
            )
            Spacer(Modifier.width(6.dp))
        }
        WbText(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
fun WbSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: Int = 17,
) {
    if (wbUiStyle() == UiStyle.MD3E) {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 56.dp),
            enabled = enabled,
            shape = CircleShape,
        ) {
            M3Text(text = text, style = M3MaterialTheme.typography.titleMedium)
        }
        return
    }
    if (wbUiStyle() == UiStyle.IOS || wbUiStyle() == UiStyle.V090) {
        val c = wbColors()
        IosButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            background = c.tintSoft,
            contentColor = c.accent,
            filled = false,
        )
        return
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(),
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        WbText(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
fun WbDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: Int = 17,
) {
    if (wbUiStyle() == UiStyle.MD3E) {
        val colors = M3MaterialTheme.colorScheme
        M3Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 56.dp),
            enabled = enabled,
            shape = CircleShape,
            colors = M3ButtonDefaults.buttonColors(
                containerColor = colors.error,
                contentColor = colors.onError,
            ),
        ) {
            M3Text(text = text, style = M3MaterialTheme.typography.titleMedium)
        }
        return
    }
    if (wbUiStyle() == UiStyle.IOS || wbUiStyle() == UiStyle.V090) {
        val c = wbColors()
        IosButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            background = c.danger,
            contentColor = Color.White,
            filled = true,
        )
        return
    }
    val c = MiuixTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            color = c.errorContainer,
            contentColor = c.onErrorContainer,
        ),
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        WbText(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
fun WbTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color? = null,
    fontSize: Int = 15,
) {
    if (wbUiStyle() == UiStyle.MD3E) {
        M3TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = M3ButtonDefaults.textButtonColors(
                contentColor = color ?: M3MaterialTheme.colorScheme.primary,
            ),
        ) {
            M3Text(text = text, style = M3MaterialTheme.typography.labelLarge)
        }
        return
    }
    if (wbUiStyle() == UiStyle.IOS || wbUiStyle() == UiStyle.V090) {
        val c = wbColors()
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(50))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WbText(
                text = text,
                fontSize = 17.sp,
                color = if (enabled) (color ?: c.accent) else c.secondaryLabel,
                maxLines = 1,
            )
        }
        return
    }
    val c = MiuixTheme.colorScheme
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            color = Color.Transparent,
            disabledColor = Color.Transparent,
            textColor = color ?: c.primary,
            disabledTextColor = c.disabledOnSurface,
        ),
    )
}

@Composable
fun WbContentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    if (wbUiStyle() == UiStyle.MD3E) {
        M3Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 56.dp),
            enabled = enabled,
            shape = CircleShape,
            content = content,
        )
        return
    }
    if (wbUiStyle() == UiStyle.IOS || wbUiStyle() == UiStyle.V090) {
        val c = wbColors()
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(50))
                .background(c.tintSoft)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        return
    }
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(),
        content = content,
    )
}

@Composable
private fun IosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    background: Color,
    contentColor: Color,
    icon: ImageVector? = null,
    filled: Boolean,
    backgroundBrush: Brush? = null,
) {
    val c = wbColors()
    val md3e = wbUiStyle() == UiStyle.MD3E
    val bg = when {
        !enabled -> c.secondaryLabel.copy(alpha = 0.18f)
        filled -> background
        else -> background
    }
    val fg = when {
        !enabled -> c.secondaryLabel
        filled -> contentColor
        else -> contentColor
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = if (md3e) 56.dp else 50.dp)
            .clip(RoundedCornerShape(50))
            .background(backgroundBrush ?: SolidColor(bg))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            WbIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = fg,
            )
            Spacer(Modifier.width(7.dp))
        }
        WbText(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
        )
    }
}
