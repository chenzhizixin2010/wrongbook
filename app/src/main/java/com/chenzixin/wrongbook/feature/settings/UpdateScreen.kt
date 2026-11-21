package com.chenzixin.wrongbook.feature.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.BuildConfig
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoText

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
        WbTopBar(title = "检查更新", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            WbSection(
                header = "版本",
                footer = "更新来自 GitHub Releases（${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}），直连失败会自动尝试镜像站。",
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CupertinoText(
                        text = "当前版本 ${state.currentVersion}",
                        fontSize = 15.sp,
                        color = colors.label,
                    )
                    Spacer(Modifier.height(6.dp))
                    when {
                        state.checking -> CupertinoText(
                            text = "正在检查…",
                            fontSize = 13.sp,
                            color = colors.secondaryLabel,
                        )

                        state.info != null -> CupertinoText(
                            text = "发现新版本 ${state.info!!.version}",
                            fontSize = 15.sp,
                            color = colors.tint,
                        )

                        state.checked -> CupertinoText(
                            text = "已是最新版本",
                            fontSize = 13.sp,
                            color = colors.secondaryLabel,
                        )
                    }
                }
            }

            val info = state.info
            if (info != null) {
                WbSection(header = "更新内容（${info.version}）") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CupertinoText(
                            text = info.notes.ifBlank { "这个版本没有写更新说明" }.take(1500),
                            fontSize = 14.sp,
                            color = colors.label,
                        )
                    }
                }

                WbSection(header = "下载") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val progress = state.progress
                        if (progress != null) {
                            CupertinoText(
                                text = "${progress.percent}% · ${progress.sourceLabel} · " +
                                    "${progress.downloaded / 1024 / 1024}MB / ${progress.total / 1024 / 1024}MB",
                                fontSize = 13.sp,
                                color = colors.secondaryLabel,
                            )
                            Spacer(Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.groupedBackground),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(progress.percent / 100f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.tint),
                                ) {}
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            when {
                                state.downloadedFile != null -> CupertinoButton(onClick = vm::install) {
                                    CupertinoText(text = "安装更新", fontSize = 16.sp)
                                }

                                state.downloading -> CupertinoButton(onClick = vm::cancelDownload) {
                                    CupertinoText(text = "取消下载")
                                }

                                else -> CupertinoButton(onClick = vm::download) {
                                    CupertinoText(
                                        text = if (info.sizeBytes > 0) {
                                            "下载（${info.sizeBytes / 1024 / 1024}MB）"
                                        } else {
                                            "下载"
                                        },
                                        fontSize = 16.sp,
                                    )
                                }
                            }
                            CupertinoButton(onClick = vm::check, enabled = !state.checking) {
                                CupertinoText(text = "重新检查")
                            }
                        }
                    }
                }
            }

            WbSection(
                header = "手动更新",
                footer = "如果自动更新不可用，可以直接去 GitHub Releases 页面下载 APK。",
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CupertinoText(
                        text = state.info?.pageUrl?.takeIf { it.isNotBlank() }
                            ?: "https://github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases",
                        fontSize = 13.sp,
                        color = colors.tint,
                    )
                    Spacer(Modifier.height(12.dp))
                    CupertinoButton(onClick = vm::openInstallSettings) {
                        CupertinoText(text = "允许安装未知应用")
                    }
                }
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
