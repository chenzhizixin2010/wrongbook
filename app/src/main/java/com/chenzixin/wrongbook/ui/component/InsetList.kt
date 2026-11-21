package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.ChevronForward

/**
 * iOS「分组列表」容器，液态玻璃版：
 * 小标题（可带渐变图标） + 悬浮玻璃卡片 + 表尾说明。
 *
 * Cupertino 3.3.1 未提供 CupertinoList，列表样式由使用方组合，
 * 因此在这里封一层：全 App 的分组视觉只改这一个文件。
 */
@Composable
fun WbSection(
    header: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    /** 标题行右侧的操作区（比如「编辑」按钮） */
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = wbColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        if (header != null) {
            Row(
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    WbIconTile(icon = icon, size = 22.dp, corner = 7.dp)
                    Spacer(Modifier.width(8.dp))
                }
                CupertinoText(
                    text = header,
                    modifier = Modifier.weight(1f, fill = false),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = colors.secondaryLabel,
                    maxLines = 1,
                )
                if (action != null) {
                    Spacer(Modifier.weight(1f))
                    action()
                }
            }
        }
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            elevation = 8.dp,
        ) {
            Column(content = content)
        }
        if (footer != null) {
            CupertinoText(
                text = footer,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 9.dp),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = colors.secondaryLabel,
            )
        }
    }
}

/**
 * 分组列表中的一行。
 *
 * @param showDivider 最后一行传 false，避免卡片底部多一条线。
 * @param trailing 右侧自定义内容；不传且有 [onClick] 时自动补一个 chevron。
 */
@Composable
fun WbListRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = wbColors()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                WbIconTile(icon = icon, size = 32.dp, corner = 10.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                CupertinoText(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.label,
                    maxLines = 1,
                )
                if (subtitle != null) {
                    CupertinoText(
                        text = subtitle,
                        modifier = Modifier.padding(top = 3.dp),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = colors.secondaryLabel,
                    )
                }
            }
            when {
                trailing != null -> {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }

                onClick != null -> {
                    Spacer(Modifier.width(8.dp))
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Outlined.ChevronForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colors.secondaryLabel.copy(alpha = 0.55f),
                    )
                }
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (icon != null) 60.dp else 16.dp, end = 16.dp)
                    .height(0.7.dp)
                    .background(colors.separator),
            )
        }
    }
}

/** 卡片式列表（题库、会话）用的小节标题，带右侧计数。 */
@Composable
fun WbSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val colors = wbColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CupertinoText(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            color = colors.secondaryLabel,
        )
        if (trailing != null) {
            CupertinoText(
                text = trailing,
                fontSize = 12.sp,
                color = colors.secondaryLabel.copy(alpha = 0.8f),
            )
        }
    }
}
