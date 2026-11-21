package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText

/**
 * 统一的按钮语义。
 *
 * 视觉规格：
 * - 主要：强调色 → 紫的渐变胶囊 + 彩色投影 + 顶部高光；
 * - 次要：玻璃胶囊 + 强调色字；
 * - 危险：玻璃胶囊 + 红字（不用实心红，避免误触视觉过重）；
 * - 文字：无底色，仅用于导航栏/卡片内的轻操作。
 */

/** 主要操作：渐变玻璃胶囊 + 白字。 */
@Composable
fun WbPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: Int = 16,
    icon: ImageVector? = null,
) {
    val colors = wbColors()
    val shape = RoundedCornerShape(percent = 50)
    val contentColor = if (enabled) Color.White else Color.White.copy(alpha = 0.72f)

    Row(
        modifier = modifier
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 16.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = colors.accent.copy(alpha = 0.5f),
                        spotColor = colors.accent.copy(alpha = 0.38f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.linearGradient(listOf(colors.accent, colors.accentEnd))
                } else {
                    Brush.linearGradient(
                        listOf(
                            colors.secondaryLabel.copy(alpha = 0.45f),
                            colors.secondaryLabel.copy(alpha = 0.3f),
                        ),
                    )
                },
            )
            .drawBehind {
                // 玻璃胶囊顶部的镜面反光。
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.75f,
                    ),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CupertinoIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size((fontSize + 3).dp),
                tint = contentColor,
            )
            Spacer(Modifier.width(8.dp))
        }
        CupertinoText(
            text = text,
            color = contentColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** 次要操作：淡强调色胶囊 + 强调色字（在白玻璃弹窗里也能看清边界）。 */
@Composable
fun WbSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: Int = 15,
) {
    val colors = wbColors()
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(shape)
            .background(
                if (enabled) colors.tint.copy(alpha = 0.10f) else colors.secondaryLabel.copy(alpha = 0.08f),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CupertinoText(
            text = text,
            color = if (enabled) colors.tint else colors.secondaryLabel,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/** 危险操作：淡红玻璃胶囊 + 红字。 */
@Composable
fun WbDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: Int = 15,
) {
    val colors = wbColors()
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(shape)
            .background(colors.danger.copy(alpha = 0.12f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CupertinoText(
            text = text,
            color = colors.danger,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/** 文字按钮：透明底 + 强调色字（「重拍」「全选」这种轻操作）。 */
@Composable
fun WbTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color? = null,
    fontSize: Int = 15,
) {
    val colors = wbColors()
    val fg = color ?: colors.tint
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CupertinoText(
            text = text,
            color = if (enabled) fg else fg.copy(alpha = 0.5f),
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/** 仅供需要自定义内容（如带图标）的按钮使用。 */
@Composable
fun WbContentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    CupertinoButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
}
