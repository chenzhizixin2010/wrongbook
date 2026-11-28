package com.chenzixin.wrongbook.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.BuildConfig
import com.chenzixin.wrongbook.domain.update.ReleaseNotesFormatter
import com.chenzixin.wrongbook.ui.component.WbSurface
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbBadge
import com.chenzixin.wrongbook.ui.component.WbDangerButton
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSecondaryButton
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbTextButton
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.component.WbText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Share

/**
 * 检查更新：从 GitHub Releases 拉最新 APK，直连失败自动走镜像。
 */
@Composable
fun UpdateScreen(
    onBack: () -> Unit,
    vm: UpdateViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val colors = wbColors()

    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(
            title = "检查更新",
            onBack = onBack,
            actions = {
                WbTextButton(
                    text = if (state.checking) "检查中…" else "重新检查",
                    onClick = vm::check,
                    enabled = !state.checking,
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            VersionCard(
                currentVersion = state.currentVersion,
                checking = state.checking,
                newVersion = state.info?.version,
                checked = state.checked,
                channelLabel = state.channel.label,
            )

            val info = state.info
            if (info != null) {
                WbSection(
                    header = "更新内容",
                    icon = MiuixIcons.Info,
                ) {
                    WbText(
                        text = ReleaseNotesFormatter.toPlainText(info.notes, 2000).ifBlank { "这个版本没有写更新说明" },
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.5.sp,
                        lineHeight = 21.sp,
                        color = colors.label,
                    )
                }

                WbSection(header = "下载", icon = MiuixIcons.Share) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val progress = state.progress
                        if (progress != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WbText(
                                    text = "${progress.percent}%",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.tint,
                                )
                                Spacer(Modifier.weight(1f))
                                WbText(
                                    text = buildString {
                                        append(progress.speedText())
                                        append(" · ").append(progress.sizeText())
                                        progress.etaSeconds?.let { append(" · 剩 ").append(it).append("s") }
                                        append(" · ").append(progress.sourceLabel)
                                    },
                                    fontSize = 11.sp,
                                    color = colors.secondaryLabel,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(colors.secondaryLabel.copy(alpha = 0.12f)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((progress.percent / 100f).coerceIn(0f, 1f))
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(colors.tint),
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            when {
                                state.downloadedFile != null -> WbPrimaryButton(
                                    text = "安装更新",
                                    onClick = vm::install,
                                    modifier = Modifier.weight(1f),
                                    icon = MiuixIcons.Share,
                                )

                                state.downloading -> WbDangerButton(
                                    text = "取消下载",
                                    onClick = vm::cancelDownload,
                                    modifier = Modifier.weight(1f),
                                )

                                else -> WbPrimaryButton(
                                    text = if (info.sizeBytes > 0) {
                                        "下载（${info.sizeBytes / 1024 / 1024}MB）"
                                    } else {
                                        "下载"
                                    },
                                    onClick = vm::download,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            WbSection(
                header = when {
                    state.info?.isAlpha == true -> "手动更新（Alpha 内测）"
                    state.info?.isPrerelease == true -> "手动更新（预发布）"
                    else -> "手动更新"
                },
                icon = MiuixIcons.Refresh,
                footer = "如果自动更新不可用，可以直接去 GitHub Releases 页面下载 APK。",
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val releasesUrl = state.info?.pageUrl?.takeIf { it.isNotBlank() }
                        ?: "https://github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases"
                    val context = LocalContext.current
                    WbText(
                        text = releasesUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openUrl(context, releasesUrl) }
                            .padding(vertical = 4.dp),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = colors.tint,
                    )
                    Spacer(Modifier.height(8.dp))
                    WbSecondaryButton(
                        text = "打开下载页",
                        onClick = { openUrl(context, releasesUrl) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(14.dp))
                    WbSecondaryButton(
                        text = "允许安装未知应用",
                        onClick = vm::openInstallSettings,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WbText(
                    text = "更新来自 GitHub Releases（${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}），直连失败会自动尝试镜像站。",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = colors.secondaryLabel,
                )
            }
        }
    }

    if (state.message != null) {
        WbAlertDialog(
            visible = true,
            title = "提示",
            message = state.message,
            confirmText = "知道了",
            onConfirm = vm::clearMessage,
            onDismiss = vm::clearMessage,
        )
    }
}

@Composable
private fun VersionCard(
    currentVersion: String,
    checking: Boolean,
    newVersion: String?,
    checked: Boolean,
    channelLabel: String,
) {
    val colors = wbColors()
    WbSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 28.dp,
        elevation = 14.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                WbText(
                    text = "当前版本",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = colors.secondaryLabel,
                )
                Spacer(Modifier.height(4.dp))
                WbText(
                    text = currentVersion,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = colors.label,
                )
                Spacer(Modifier.height(6.dp))
                WbText(
                    text = when {
                        checking -> "正在连接 GitHub…"
                        newVersion != null -> "发现新版本 $newVersion"
                        checked -> "已经是最新版本"
                        else -> "尚未检查"
                    },
                    fontSize = 12.sp,
                    color = colors.secondaryLabel,
                )
                Spacer(Modifier.height(2.dp))
                WbText(
                    text = "版本计划：$channelLabel（在 设置 → 关于 里切换）",
                    fontSize = 11.sp,
                    color = colors.secondaryLabel,
                )
            }
            WbBadge(
                text = when {
                    checking -> "检查中"
                    newVersion != null -> "有新版本"
                    checked -> "已是最新"
                    else -> "待检查"
                },
                color = when {
                    checking -> colors.warning
                    newVersion != null -> colors.tint
                    checked -> colors.success
                    else -> colors.secondaryLabel
                },
            )
        }
    }
}
