package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 分组容器。
 *
 * Miuix：SmallTitle + 一张 16dp 圆角 Card。
 * iOS：灰色 uppercase 小标题 + 一张 12dp 圆角的 inset grouped 卡。
 * 两套都只包「一组」行，绝不为单行单独套卡。
 */
@Composable
fun WbSection(
    header: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS
    val v090 = style == UiStyle.V090
    val c = wbColors()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = if (ios) 18.dp else 6.dp,
                bottom = if (ios) 6.dp else 6.dp,
            ),
    ) {
        if (header != null || action != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (ios || v090) 16.dp else 12.dp,
                        end = if (ios) 4.dp else 4.dp,
                        top = if (ios) 0.dp else 10.dp,
                        bottom = 6.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (header != null) {
                    if (ios || v090) {
                        WbText(
                            text = if (v090) header else header.uppercase(),
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.4.sp,
                            color = c.secondaryLabel,
                        )
                    } else {
                        SmallTitle(
                            text = header,
                            modifier = Modifier.weight(1f),
                            insideMargin = PaddingValues(0.dp),
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (action != null) action()
            }
        }

        if (ios || v090) {
            WbSurface(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = if (v090) 24.dp else 12.dp,
                elevation = if (v090) 12.dp else 0.dp,
            ) {
                Column(content = content)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(0.dp),
            ) {
                Column(content = content)
            }
        }

        if (footer != null) {
            WbText(
                text = footer,
                modifier = Modifier.padding(
                    start = if (ios) 0.dp else 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = 4.dp,
                ),
                fontSize = if (ios) 13.sp else 11.sp,
                lineHeight = if (ios) 18.sp else 16.sp,
                color = c.secondaryLabel,
            )
        }
    }
}

@Composable
fun WbDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp,
) {
    val c = wbColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(1.dp)
            .background(c.separator.copy(alpha = if (wbUiStyle() == UiStyle.IOS) 0.55f else 1f)),
    )
}

/**
 * 分组列表中的一行。
 *
 * iOS：44dp 行高、17sp 标题、13sp 摘要、右侧 chevron。
 * Miuix：BasicComponent。
 */
@Composable
fun WbListRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS
    val v090 = style == UiStyle.V090
    val c = wbColors()

    if (ios || v090) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(horizontal = 16.dp, vertical = if (v090) 13.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(12.dp))
                } else if (icon != null) {
                    WbIcon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(if (v090) 20.dp else 22.dp),
                        tint = c.accent,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    WbText(
                        text = title,
                        fontSize = if (v090) 16.sp else 17.sp,
                        fontWeight = if (v090) FontWeight.SemiBold else FontWeight.Normal,
                        color = c.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        WbText(
                            text = subtitle,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            color = c.secondaryLabel,
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(10.dp))
                    trailing()
                } else if (onClick != null) {
                    Spacer(Modifier.width(8.dp))
                    WbIcon(
                        imageVector = MiuixIcons.ChevronForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = c.secondaryLabel,
                    )
                }
            }
            if (showDivider) WbDivider(startIndent = if (leading != null) 68.dp else if (icon != null) 50.dp else 16.dp)
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        BasicComponent(
            title = title,
            summary = subtitle,
            startAction = leading ?: if (icon != null) {
                {
                    WbIcon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            } else {
                null
            },
            endActions = {
                if (trailing != null) {
                    trailing()
                } else if (onClick != null) {
                    WbIcon(
                        imageVector = MiuixIcons.ChevronForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    )
                }
            },
            onClick = onClick,
        )
        if (showDivider) {
            WbDivider(startIndent = if (leading != null) 68.dp else if (icon != null) 52.dp else 16.dp)
        }
    }
}

/** 卡片列表的小节标题 + 右侧计数。 */
@Composable
fun WbSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val ios = wbUiStyle() == UiStyle.IOS
    val c = wbColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (ios) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WbText(
            text = if (ios) text.uppercase() else text,
            modifier = Modifier.weight(1f),
            fontSize = if (ios) 13.sp else 13.sp,
            fontWeight = if (ios) FontWeight.Normal else FontWeight.Medium,
            letterSpacing = if (ios) 0.4.sp else 0.sp,
            color = c.secondaryLabel,
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            WbText(
                text = trailing,
                fontSize = 11.sp,
                color = c.secondaryLabel,
            )
        }
    }
}
