package com.chenzixin.wrongbook.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.BuildConfig
import com.chenzixin.wrongbook.data.prefs.ThemeMode
import com.chenzixin.wrongbook.ui.component.GlassSurface
import com.chenzixin.wrongbook.ui.component.WbBadge
import com.chenzixin.wrongbook.ui.component.WbGlassIconButton
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbPageHeader
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbSecondaryButton
import com.chenzixin.wrongbook.ui.component.WbTextButton
import com.chenzixin.wrongbook.ui.component.wbTabBarSpace
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.ArrowClockwise
import io.github.robinpcrd.cupertino.icons.outlined.FaceSmiling
import io.github.robinpcrd.cupertino.icons.outlined.Gearshape
import io.github.robinpcrd.cupertino.icons.outlined.InfoCircle
import io.github.robinpcrd.cupertino.icons.outlined.ListBullet
import io.github.robinpcrd.cupertino.icons.outlined.Lock
import io.github.robinpcrd.cupertino.icons.outlined.SquareAndArrowUp
import io.github.robinpcrd.cupertino.icons.outlined.Star
import io.github.robinpcrd.cupertino.icons.outlined.WrenchAndScrewdriver

/**
 * 设置 Tab：模型与 API、题库分类、学习数据、外观、数据、关于。
 *
 * 视觉上做成「一张身份卡 + 若干玻璃分组」：
 * 身份卡放最上面，确保用户一眼看到版本与模型状态；其余按使用频率排列。
 */
@Composable
fun SettingsTabScreen(
    onOpenProfile: (Long) -> Unit,
    onOpenTags: () -> Unit,
    onOpenUpdate: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenImport: () -> Unit,
) {
    val vm: SettingsViewModel = hiltViewModel()
    val profiles by vm.profiles.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val colors = wbColors()
    val bottomSpace = wbTabBarSpace()
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomSpace),
    ) {
        WbPageHeader(
            title = "设置",
            kicker = "个性化与数据",
            subtitle = "错题、图片与对话都只在这台手机上",
        )

        IdentityCard(profileCount = profiles.size)

        WbSection(
            header = "模型与 API",
            icon = CupertinoIcons.Outlined.WrenchAndScrewdriver,
            footer = if (profiles.isEmpty()) {
                "兼容 OpenAI 协议的服务都能接入：DeepSeek、智谱、硅基流动、自建网关等。"
            } else {
                "点配置查看详情；默认配置用于 AI 对话、识图与生成。"
            },
        ) {
            if (profiles.isEmpty()) {
                WbListRow(
                    title = "还没有 API 配置",
                    subtitle = "点右侧「添加」开始",
                    showDivider = false,
                    trailing = { WbTextButton(text = "添加", onClick = { onOpenProfile(0L) }) },
                )
            } else {
                profiles.forEachIndexed { index, profile ->
                    WbListRow(
                        title = profile.name,
                        subtitle = buildString {
                            append(profile.chatModel)
                            if (profile.visionModel.isNotBlank()) append(" · 识图 ${profile.visionModel}")
                            if (profile.isDefault) append(" · 默认")
                        },
                        showDivider = index != profiles.lastIndex,
                        onClick = { onOpenProfile(profile.id) },
                        trailing = {
                            if (profile.isDefault) {
                                CupertinoIcon(
                                    imageVector = CupertinoIcons.Outlined.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = colors.tint,
                                )
                            }
                        },
                    )
                }
            }
        }

        WbSection(header = "题库分类", icon = CupertinoIcons.Outlined.Star) {
            WbListRow(
                title = "标签与收藏夹",
                subtitle = "自定义分类，收藏夹只是打了标记的标签",
                showDivider = false,
                onClick = onOpenTags,
            )
        }

        WbSection(header = "学习数据", icon = CupertinoIcons.Outlined.ArrowClockwise) {
            WbListRow(
                title = "学习统计",
                subtitle = "掌握度分布、标签分布",
                showDivider = false,
                onClick = onOpenStats,
            )
        }

        WbSection(header = "外观", icon = CupertinoIcons.Outlined.FaceSmiling) {
            WbListRow(
                title = "主题",
                subtitle = themeMode.label,
                showDivider = false,
                onClick = { showThemeDialog = true },
            )
        }

        WbSection(header = "数据", icon = CupertinoIcons.Outlined.SquareAndArrowUp) {
            WbListRow(
                title = "导出题库",
                subtitle = "ZIP：题目 JSON + 图片",
                showDivider = true,
                onClick = onOpenExport,
                trailing = {
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Outlined.SquareAndArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = colors.secondaryLabel,
                    )
                },
            )
            WbListRow(
                title = "导入题库",
                subtitle = "从 ZIP 恢复或迁移到新机",
                showDivider = false,
                onClick = onOpenImport,
            )
        }

        WbSection(header = "关于", icon = CupertinoIcons.Outlined.InfoCircle) {
            WbListRow(
                title = "隐私政策",
                subtitle = "题目与对话只存在本机",
                showDivider = true,
                onClick = { /* 上架前替换为正式地址 */ },
            )
            WbListRow(
                title = "检查更新",
                subtitle = "从 GitHub Releases 拉取新版本",
                showDivider = true,
                onClick = onOpenUpdate,
            )
            WbListRow(
                title = "版本",
                subtitle = BuildConfig.VERSION_NAME,
                showDivider = false,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CupertinoIcon(
                imageVector = CupertinoIcons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = colors.secondaryLabel.copy(alpha = 0.7f),
            )
            Spacer(Modifier.width(7.dp))
            CupertinoText(
                text = "题目图片与对话内容只保存在本机，不会上传服务器。",
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = colors.secondaryLabel,
            )
        }
    }

    if (showThemeDialog) {
        ThemeDialog(
            current = themeMode,
            onPick = {
                vm.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }
}

/** 顶部身份卡：应用图标 + 版本 + 模型配置状态。 */
@Composable
private fun IdentityCard(profileCount: Int) {
    val colors = wbColors()
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 27.dp,
        elevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(colors.accent, colors.accentEnd),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CupertinoIcon(
                    imageVector = CupertinoIcons.Outlined.ListBullet,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White,
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                CupertinoText(
                    text = "错题本",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.label,
                )
                Spacer(Modifier.height(3.dp))
                CupertinoText(
                    text = "v${BuildConfig.VERSION_NAME} · 本地优先",
                    fontSize = 11.5.sp,
                    color = colors.secondaryLabel,
                )
            }
            WbBadge(
                text = if (profileCount == 0) "未配模型" else "$profileCount 套模型",
                color = if (profileCount == 0) colors.warning else colors.success,
            )
        }
    }
}

@Composable
private fun ThemeDialog(
    current: ThemeMode,
    onPick: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = wbColors()
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            modifier = Modifier.width(304.dp),
            cornerRadius = 30.dp,
            elevation = 34.dp,
            underlay = colors.dialogScrim,
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                CupertinoText(
                    text = "外观",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.label,
                )
                Spacer(Modifier.height(4.dp))
                CupertinoText(
                    text = "深色模式下玻璃会变成暗色磨砂",
                    fontSize = 12.sp,
                    color = colors.secondaryLabel,
                )
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = mode == current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) colors.tintSoft else Color.Transparent,
                                )
                                .clickable { onPick(mode) }
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CupertinoText(
                                text = mode.label,
                                modifier = Modifier.weight(1f),
                                fontSize = 15.5.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) colors.tint else colors.label,
                            )
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(colors.tint),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CupertinoText(text = "✓", fontSize = 11.sp, color = Color.White)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(colors.separator),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                WbSecondaryButton(
                    text = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
