package com.chenzixin.wrongbook.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.ui.component.WbLargeTitle
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.ChevronForward
import io.github.robinpcrd.cupertino.icons.outlined.Star

/**
 * 设置 Tab。
 *
 * P2：API 配置（多套 Profile、Keystore 加密、测试连接）。
 * P7.5：导出 / 导入（SAF）。
 * P8：隐私政策、备案号、第三方 SDK 清单。
 */
@Composable
fun SettingsTabScreen(
    onOpenProfile: (Long) -> Unit,
    onOpenTags: () -> Unit,
    onOpenUpdate: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenImport: () -> Unit,
) {
    val vm: SettingsViewModel = hiltViewModel()
    val profiles by vm.profiles.collectAsState()
    val colors = wbColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 24.dp),
    ) {
        Box(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
            WbLargeTitle(text = "设置")
        }

        WbSection(
            header = "模型与 API",
            footer = if (profiles.isEmpty()) {
                "兼容 OpenAI 协议的服务都能接入：DeepSeek、通义、智谱、自建网关等。"
            } else {
                "点配置查看详情；默认配置用于 AI 对话与识图。"
            },
        ) {
            if (profiles.isEmpty()) {
                WbListRow(
                    title = "还没有 API 配置",
                    subtitle = "点右侧「添加」开始",
                    showDivider = false,
                    trailing = {
                        CupertinoButton(onClick = { onOpenProfile(0L) }) {
                            CupertinoText(text = "添加")
                        }
                    },
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
                                    tint = colors.tint,
                                )
                            } else {
                                CupertinoIcon(
                                    imageVector = CupertinoIcons.Outlined.ChevronForward,
                                    contentDescription = null,
                                    tint = colors.secondaryLabel,
                                )
                            }
                        },
                    )
                }
            }
        }

        WbSection(header = "题库分类") {
            WbListRow(
                title = "标签与收藏夹",
                subtitle = "自定义分类、收藏夹只是打了标记的标签",
                showDivider = false,
                onClick = onOpenTags,
            )
        }

        WbSection(header = "数据") {
            WbListRow(
                title = "导出题库",
                subtitle = "ZIP：题目 JSON + 图片",
                showDivider = true,
                onClick = onOpenExport,
            )
            WbListRow(
                title = "导入题库",
                subtitle = "从 ZIP 恢复或迁移到新机",
                showDivider = false,
                onClick = onOpenImport,
            )
        }

        WbSection(header = "关于") {
            WbListRow(
                title = "隐私政策",
                subtitle = "题目与对话只存在本机",
                showDivider = true,
                onClick = { /* P8：打开正式隐私政策地址 */ },
            )
            WbListRow(
                title = "检查更新",
                subtitle = "从 GitHub Releases 拉取新版本",
                showDivider = true,
                onClick = onOpenUpdate,
            )
            WbListRow(
                title = "版本",
                subtitle = com.chenzixin.wrongbook.BuildConfig.VERSION_NAME,
                showDivider = false,
            )
        }

        CupertinoText(
            text = "题目图片与对话内容只保存在本机，不会上传服务器。",
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
            fontSize = 13.sp,
            color = colors.secondaryLabel,
        )
    }
}
