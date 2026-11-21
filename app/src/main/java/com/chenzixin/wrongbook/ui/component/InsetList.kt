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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText

/**
 * iOS「分组列表」容器：灰底 + 圆角卡片 + 可选表头/表尾。
 *
 * Cupertino 3.3.1 未提供 CupertinoList，列表样式由使用方组合，
 * 因此在这里封一层：全 App 的分组视觉只改这一个文件。
 */
@Composable
fun WbSection(
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = wbColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (header != null) {
            CupertinoText(
                text = header,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                fontSize = 13.sp,
                color = colors.secondaryLabel,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.cardBackground),
            content = content,
        )
        if (footer != null) {
            CupertinoText(
                text = footer,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
                fontSize = 13.sp,
                color = colors.secondaryLabel,
            )
        }
    }
}

/**
 * 分组列表中的一行。
 *
 * @param showDivider 最后一行传 false，避免卡片底部多一条线。
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                CupertinoIcon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = colors.tint,
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                CupertinoText(text = title, fontSize = 17.sp, color = colors.label)
                if (subtitle != null) {
                    CupertinoText(
                        text = subtitle,
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 13.sp,
                        color = colors.secondaryLabel,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (icon != null) 46.dp else 16.dp)
                    .height(1.dp)
                    .background(colors.separator),
            )
        }
    }
}
