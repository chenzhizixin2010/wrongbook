package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.ChevronBackward

/**
 * 二级页面顶栏：玻璃返回按钮 + 居中标题 + 右侧操作。
 *
 * 不做整条玻璃栏：iOS 26 的导航栏在静止时是「隐形」的，
 * 只有返回键和操作项浮在极光背景上，滚动时再补一层材质。
 */
@Composable
fun WbTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = wbColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        if (onBack != null) {
            WbGlassIconButton(
                icon = CupertinoIcons.Outlined.ChevronBackward,
                contentDescription = "返回",
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
                size = 40.dp,
                iconSize = 17.dp,
                tint = colors.tint,
            )
        }
        CupertinoText(
            text = title,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 58.dp),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.label,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/** 页面内的超大标题（iOS Large Title），可带 eyebrow 与副标题。 */
@Composable
fun WbLargeTitle(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    kicker: String? = null,
) {
    val colors = wbColors()
    Column(modifier = modifier) {
        if (kicker != null) {
            Row(
                modifier = Modifier.padding(bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(colors.accent, colors.accentEnd)),
                        ),
                )
                Spacer(Modifier.width(6.dp))
                CupertinoText(
                    text = kicker,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    color = colors.tint,
                    maxLines = 1,
                )
            }
        }
        CupertinoText(
            text = text,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.7).sp,
            lineHeight = 40.sp,
            color = colors.label,
            maxLines = 1,
        )
        if (subtitle != null) {
            CupertinoText(
                text = subtitle,
                modifier = Modifier.padding(top = 5.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = colors.secondaryLabel,
            )
        }
    }
}

/**
 * 主 Tab 页统一的页面头：eyebrow + 大标题 + 副标题 + 右侧操作。
 * 左右 20dp、顶部 14dp，和悬浮标签栏的 16dp 边距形成同一套栅格。
 */
@Composable
fun WbPageHeader(
    title: String,
    kicker: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WbLargeTitle(
            text = title,
            modifier = Modifier.weight(1f),
            subtitle = subtitle,
            kicker = kicker,
        )
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/** 极简分隔线：给「说明」「脚注」这类区域用。 */
@Composable
fun WbHairline(modifier: Modifier = Modifier) {
    val colors = wbColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.7.dp)
            .background(colors.separator),
    )
}
