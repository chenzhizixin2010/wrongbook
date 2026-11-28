package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 二级页顶栏。
 * Miuix：56dp + 居中标题 + 底部分隔线。
 * iOS：44dp nav bar + 左侧返回 + 状态栏下方毛玻璃底。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WbTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS
    val v090 = style == UiStyle.V090
    val md3e = style == UiStyle.MD3E
    val c = wbColors()

    if (md3e) {
        TopAppBar(
            title = {
                M3Text(text = title, style = M3MaterialTheme.typography.titleLarge)
            },
            navigationIcon = {
                if (onBack != null) {
                    M3IconButton(onClick = onBack) {
                        WbIcon(
                            imageVector = MiuixIcons.ChevronBackward,
                            contentDescription = "返回",
                            modifier = Modifier.size(24.dp),
                            tint = M3MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = M3MaterialTheme.colorScheme.surface,
                titleContentColor = M3MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = M3MaterialTheme.colorScheme.onSurface,
            ),
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (ios || v090) c.barScrim else c.cardBackground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (ios) 44.dp else if (v090) 52.dp else if (md3e) 64.dp else 56.dp)
                .padding(horizontal = if (ios) 4.dp else 8.dp),
        ) {
            if (onBack != null) {
                if (ios) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(44.dp)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        WbIcon(
                            imageVector = MiuixIcons.ChevronBackward,
                            contentDescription = "返回",
                            modifier = Modifier.size(23.dp),
                            tint = c.accent,
                        )
                    }
                } else {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(40.dp),
                    ) {
                        WbIcon(
                            imageVector = MiuixIcons.ChevronBackward,
                            contentDescription = "返回",
                            modifier = Modifier.size(22.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            WbText(
                text = title,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp),
                fontSize = if (md3e) 22.sp else 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.label,
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
        WbDivider()
    }
}

/** 页面大标题：iOS largeTitle 34 / Miuix 28。 */
@Composable
fun WbLargeTitle(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    kicker: String? = null,
) {
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS
    val v090 = style == UiStyle.V090
    val md3e = style == UiStyle.MD3E
    val c = wbColors()
    Column(modifier = modifier) {
        if (kicker != null) {
            if (md3e) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(c.tintSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    WbText(
                        text = kicker,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = c.accent,
                        maxLines = 1,
                    )
                }
            } else {
                WbText(
                    text = when {
                        v090 -> "• $kicker"
                        ios -> kicker.uppercase()
                        else -> kicker
                    },
                    modifier = Modifier.padding(bottom = if (ios) 2.dp else 4.dp),
                    fontSize = 11.sp,
                    letterSpacing = if (ios) 1.sp else 0.sp,
                    color = if (ios || v090) c.secondaryLabel else c.tint,
                    maxLines = 1,
                )
            }
        }
        WbText(
            text = text,
            fontSize = when {
                md3e -> 36.sp
                ios -> 34.sp
                v090 -> 30.sp
                else -> 28.sp
            },
            fontWeight = FontWeight.Bold,
            lineHeight = when {
                md3e -> 44.sp
                ios -> 41.sp
                v090 -> 36.sp
                else -> 34.sp
            },
            color = c.label,
            maxLines = 1,
        )
        if (subtitle != null) {
            WbText(
                text = subtitle,
                modifier = Modifier.padding(top = if (ios) 2.dp else 4.dp),
                fontSize = if (ios) 15.sp else 13.sp,
                lineHeight = if (ios) 20.sp else 18.sp,
                color = c.secondaryLabel,
            )
        }
    }
}

/** 主 Tab 页统一的页面头。 */
@Composable
fun WbPageHeader(
    title: String,
    kicker: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS
    val v090 = style == UiStyle.V090
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = if (ios) 8.dp else if (v090) 10.dp else 12.dp,
                bottom = if (ios) 8.dp else 8.dp,
            ),
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

@Composable
fun WbHairline(modifier: Modifier = Modifier) {
    WbDivider(modifier = modifier)
}
