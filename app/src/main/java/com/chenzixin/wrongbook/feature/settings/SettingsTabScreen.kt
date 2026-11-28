package com.chenzixin.wrongbook.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.BuildConfig
import com.chenzixin.wrongbook.data.prefs.ThemeMode
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.domain.update.UpdateChannel
import com.chenzixin.wrongbook.ui.component.ErrataMark
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbBadge
import com.chenzixin.wrongbook.ui.component.WbDivider
import com.chenzixin.wrongbook.ui.component.WbHintBar
import com.chenzixin.wrongbook.ui.component.WbIcon
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbSwitchRow
import com.chenzixin.wrongbook.ui.component.WbPageHeader
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbSurface
import com.chenzixin.wrongbook.ui.component.WbText
import com.chenzixin.wrongbook.ui.component.WbTextButton
import com.chenzixin.wrongbook.ui.theme.LocalUiStyle
import com.chenzixin.wrongbook.ui.theme.LocalWbColors
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Ok

/**
 * 设置页：同一套信息结构，两套皮肤渲染。
 *
 * - Miuix：SmallTitle + Card + BasicComponent；
 * - iOS：uppercase section + inset grouped 玻璃卡 + Cupertino 行。
 * 所有行都走 [WbSection] / [WbListRow]，不再直接依赖 Miuix Card。
 */
@Composable
fun SettingsTabScreen(
    onOpenProfile: (Long) -> Unit,
    onOpenTags: () -> Unit,
    onOpenUpdate: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenAboutDev: () -> Unit,
    onOpenGuide: () -> Unit,
) {
    val vm: SettingsViewModel = hiltViewModel()
    val profiles by vm.profiles.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val uiStyle by vm.uiStyle.collectAsState()
    val reprocess by vm.reprocess.collectAsState()
    val reprocessMessage by vm.reprocessMessage.collectAsState()
    val splashEnabled by vm.splashEnabled.collectAsState()
    val updateChannel by vm.updateChannel.collectAsState()
    val superResolution by vm.superResolution.collectAsState()
    val srInfo by vm.srInfo.collectAsState()
    val srTest by vm.srTest.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }
    val c = wbColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        WbPageHeader(
            title = "设置",
            kicker = "本地优先 · Errata",
            subtitle = "错题、图片与对话只在这台手机上",
        )

        WbSection(header = "应用") {
            WbListRow(
                title = "Errata",
                subtitle = "AI Mistake Notebook · v${BuildConfig.VERSION_NAME}",
                leading = { ErrataMark(size = 40.dp) },
                showDivider = true,
            )
            WbListRow(
                title = "本地优先",
                subtitle = "题目与图片只存在本机；无网络也可拍照、OCR、整理",
                showDivider = false,
            )
        }

        WbSection(header = "模型与 API") {
            if (profiles.isEmpty()) {
                WbListRow(
                    title = "还没有 API 配置",
                    subtitle = "兼容 OpenAI 协议的服务都能接入：DeepSeek、智谱、硅基流动等",
                    showDivider = true,
                )
            } else {
                profiles.forEachIndexed { index, profile ->
                    WbListRow(
                        title = profile.name,
                        subtitle = buildString {
                            append(profile.chatModel)
                            if (profile.visionModel.isNotBlank()) append(" · 识图 ${profile.visionModel}")
                        },
                        showDivider = true,
                        onClick = { onOpenProfile(profile.id) },
                        trailing = if (profile.isDefault) {
                            {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    WbBadge(text = "默认")
                                    Spacer(Modifier.width(6.dp))
                                    WbIcon(
                                        imageVector = MiuixIcons.ChevronForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = c.secondaryLabel,
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
            }
            WbListRow(
                title = "添加 API 配置",
                subtitle = "Key 只存密文，不会出现在日志或导出包里",
                showDivider = false,
                onClick = { onOpenProfile(0L) },
            )
        }

        WbSection(header = "题库分类") {
            WbListRow(
                title = "标签与收藏夹",
                subtitle = "自定义分类；收藏夹只是打了标记的标签",
                showDivider = true,
                onClick = onOpenTags,
            )
            WbListRow(
                title = "学习统计",
                subtitle = "掌握度分布、标签分布",
                showDivider = false,
                onClick = onOpenStats,
            )
        }

        WbSection(header = "外观") {
            WbListRow(
                title = "界面风格",
                subtitle = uiStyle.label,
                showDivider = true,
                onClick = { showStyleDialog = true },
            )
            WbListRow(
                title = "主题",
                subtitle = themeMode.label,
                showDivider = true,
                onClick = { showThemeDialog = true },
            )
            WbListRow(
                title = "动态取色",
                subtitle = "由系统壁纸生成主题色；Android 12 以下使用默认蓝色",
                showDivider = true,
            )
            WbSwitchRow(
                title = "开屏动画",
                summary = "关闭后启动直接进主界面，不播放品牌开屏",
                checked = splashEnabled,
                onCheckedChange = vm::setSplashEnabled,
            )
        }

        WbSection(
            header = "图像增强",
            footer = "「题目详情 → 题目插图 → 高清重绘」会用模型把小图放大 4 倍（约 5–8 秒）；关掉则秒出。",
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                WbSwitchRow(
                    title = "AI 超分放大",
                    summary = srInfo,
                    checked = superResolution,
                    onCheckedChange = vm::setSuperResolution,
                )
            }
            WbListRow(
                title = "超分自检",
                subtitle = srTest ?: "在这台手机上跑一次，确认模型可用",
                showDivider = false,
                onClick = vm::runSrSelfTest,
            )
        }

        WbSection(header = "数据") {
            WbListRow(
                title = "导出题库",
                subtitle = "ZIP：题目 JSON + 图片，不含 API Key",
                showDivider = true,
                onClick = onOpenExport,
            )
            WbListRow(
                title = "导入题库",
                subtitle = "从 ZIP 恢复或迁移到新机，自动跳过重复题",
                showDivider = true,
                onClick = onOpenImport,
            )
            WbListRow(
                title = "重新处理题目图片",
                subtitle = when (val p = reprocess) {
                    null -> "统一摆正、白底黑字；会覆盖现有图片，建议先导出备份"
                    else -> "处理中 ${p.done}/${p.total}…"
                },
                showDivider = false,
                onClick = if (reprocess == null) {
                    { vm.reprocessImages() }
                } else {
                    null
                },
            )
        }

        if (reprocessMessage != null) {
            WbHintBar(
                text = reprocessMessage.orEmpty(),
                onClick = vm::clearReprocessMessage,
            )
        }

        WbSection(header = "帮助") {
            WbListRow(
                title = "使用手册",
                subtitle = "第一次使用？5 页快速上手，完整说明随时重看",
                showDivider = false,
                onClick = onOpenGuide,
            )
        }

        WbSection(header = "关于") {
            WbListRow(
                title = "测试版计划",
                subtitle = "${updateChannel.label} · ${updateChannel.summary}",
                showDivider = true,
                onClick = { showChannelDialog = true },
            )
            WbListRow(
                title = "关于开发者",
                subtitle = "版本信息、仓库与开源许可",
                showDivider = true,
                onClick = onOpenAboutDev,
            )
            WbListRow(
                title = "隐私政策",
                subtitle = "题目与对话只存在本机",
                showDivider = true,
                onClick = { showPrivacy = true },
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

        WbText(
            text = "Errata 不会上传题目图片；只有你主动使用 AI 功能时才会发送文本。",
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = c.secondaryLabel,
        )
    }

    if (showThemeDialog) {
        ChoiceDialog(
            title = "选择主题",
            options = ThemeMode.entries.map { it.label to null },
            selectedIndex = ThemeMode.entries.indexOf(themeMode),
            onPick = {
                vm.setThemeMode(ThemeMode.entries[it])
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showStyleDialog) {
        ChoiceDialog(
            title = "界面风格",
            options = UiStyle.entries.map { it.label to it.summary },
            selectedIndex = UiStyle.entries.indexOf(uiStyle),
            onPick = {
                vm.setUiStyle(UiStyle.entries[it])
                showStyleDialog = false
            },
            onDismiss = { showStyleDialog = false },
        )
    }

    if (showChannelDialog) {
        ChoiceDialog(
            title = "选择版本计划",
            options = UpdateChannel.entries.map { it.label to it.summary },
            selectedIndex = UpdateChannel.entries.indexOf(updateChannel),
            onPick = {
                vm.setUpdateChannel(UpdateChannel.entries[it])
                showChannelDialog = false
            },
            onDismiss = { showChannelDialog = false },
        )
    }

    if (showPrivacy) {
        WbAlertDialog(
            visible = true,
            title = "隐私政策",
            message = "题目、图片和对话只保存在本机。只有你主动使用 AI 功能时，才会把当前文本发送给你配置的服务商；API Key 加密存储，不写入日志或导出包。",
            confirmText = "知道了",
            onConfirm = { showPrivacy = false },
            onDismiss = { showPrivacy = false },
        )
    }
}

/** 单选弹窗：两套皮肤共用 WbSurface + WbListRow。 */
@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String?>>,
    selectedIndex: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = wbColors()
    val md3e = wbUiStyle() == UiStyle.MD3E
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(
            LocalWbColors provides c,
            LocalUiStyle provides wbUiStyle(),
        ) {
            WbSurface(
                modifier = Modifier.width(320.dp),
                cornerRadius = if (md3e) 28.dp else 16.dp,
                elevation = 24.dp,
                underlay = c.cardBackground,
            ) {
                Column {
                    WbText(
                        text = title,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
                        fontSize = if (md3e) 24.sp else 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.label,
                    )
                    WbDivider()
                    options.forEachIndexed { index, option ->
                        WbListRow(
                            title = option.first,
                            subtitle = option.second,
                            showDivider = index != options.lastIndex,
                            onClick = { onPick(index) },
                            trailing = {
                                if (index == selectedIndex) {
                                    WbIcon(
                                        imageVector = MiuixIcons.Ok,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = c.accent,
                                    )
                                }
                            },
                        )
                    }
                    WbDivider()
                    WbTextButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}
