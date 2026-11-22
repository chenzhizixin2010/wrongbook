package com.chenzixin.wrongbook.feature.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.component.WbIcon
import com.chenzixin.wrongbook.ui.component.WbText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Share

/** 导出题库：ZIP（JSON + 图片），通过系统文件选择器落盘，不申请存储权限。 */
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    vm: BackupViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val colors = wbColors()

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(vm::export) }

    LaunchedEffect(state.finished) {
        if (state.finished) onBack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(title = "导出题库", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            WbSection(
                header = "导出内容",
                icon = MiuixIcons.Share,
                footer = "备份里不包含 API Key，可以放心分享给别的设备。",
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    WbText(
                        text = "题目与图片、标签、AI 对话记录会打包成一个 ZIP 文件。",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = colors.label,
                    )
                    Spacer(Modifier.height(16.dp))
                    WbPrimaryButton(
                        text = if (state.busy) "导出中…" else "选择保存位置",
                        onClick = { createLauncher.launch("errata-backup.zip") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.busy,
                        icon = MiuixIcons.Share,
                    )
                }
            }

            WbSection(header = "包含哪些数据", icon = MiuixIcons.Lock) {
                WbListRow(title = "题目与图片", subtitle = "原图 + 200px 缩略图", showDivider = true)
                WbListRow(title = "标签与关联", subtitle = "多标签、收藏夹", showDivider = true)
                WbListRow(title = "AI 对话记录", subtitle = "不含 API Key", showDivider = false)
            }

            NoteRow("导出过程不联网；文件由系统文件选择器写入你指定的位置。")
        }

        if (state.message != null) {
            WbAlertDialog(
                visible = true,
                title = "导出结果",
                message = state.message,
                confirmText = "知道了",
                onConfirm = vm::clearMessage,
                onDismiss = vm::clearMessage,
            )
        }
    }
}

/** 导入题库：选 ZIP，重复题目自动跳过，同一个包导入两次是幂等的。 */
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    vm: BackupViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val colors = wbColors()

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(vm::import) }

    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(title = "导入题库", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            WbSection(
                header = "从备份恢复",
                icon = MiuixIcons.AddCircle,
                footer = "重复的题目会被跳过，不会产生两份。",
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    WbText(
                        text = "选择之前导出的 ZIP 文件，图片会一起恢复。",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = colors.label,
                    )
                    Spacer(Modifier.height(16.dp))
                    WbPrimaryButton(
                        text = if (state.busy) "导入中…" else "选择备份文件",
                        onClick = {
                            openLauncher.launch(
                                arrayOf("application/zip", "application/octet-stream", "*/*"),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.busy,
                        icon = MiuixIcons.AddCircle,
                    )
                    if (state.busy) {
                        Spacer(Modifier.height(12.dp))
                        WbText(
                            text = "正在解包并写入数据库，请保持页面打开…",
                            fontSize = 12.sp,
                            color = colors.secondaryLabel,
                        )
                    }
                }
            }

            WbSection(header = "判重规则") {
                WbListRow(title = "按内容指纹判重", subtitle = "同一道题导入两次只会留一份", showDivider = true)
                WbListRow(title = "版本校验", subtitle = "备份版本不兼容时会明确提示", showDivider = false)
            }

            NoteRow("导入会合并到现有题库，不会清空已有数据。")
        }

        if (state.message != null) {
            WbAlertDialog(
                visible = true,
                title = "导入结果",
                message = state.message,
                confirmText = "知道了",
                onConfirm = {
                    vm.clearMessage()
                    if (state.finished) onBack()
                },
                onDismiss = {
                    vm.clearMessage()
                    if (state.finished) onBack()
                },
            )
        }
    }
}

@Composable
private fun NoteRow(text: String) {
    val colors = wbColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WbText(
            text = text,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = colors.secondaryLabel,
        )
    }
}
