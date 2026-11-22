package com.chenzixin.wrongbook.feature.generate

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbChip
import com.chenzixin.wrongbook.ui.component.WbIconButton
import com.chenzixin.wrongbook.ui.component.WbIcon
import com.chenzixin.wrongbook.ui.component.WbInputDialog
import com.chenzixin.wrongbook.ui.component.WbPageHeader
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbSurface
import com.chenzixin.wrongbook.ui.component.WbSwitchRow
import com.chenzixin.wrongbook.ui.component.WbText
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 生成 Tab：选标签 → 整理错题 / AI 出同类题 → 可打印版。
 *
 * 用 HyperOS 的 Preference 行表达「出题方式」，用胶囊表达题量；没有渐变和玻璃卡。
 */
@Composable
fun GenerateScreen(vm: GenerateViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    val html = state.html
    if (html == null) {
        SetupContent(
            state = state,
            onToggleTag = vm::toggleTag,
            onMatchAll = vm::setMatchAll,
            onCount = vm::setCount,
            onWithAnswers = vm::setWithAnswers,
            onUseAi = vm::setUseAi,
            onTitle = vm::onTitleChange,
            onGenerate = vm::generate,
        )
    } else {
        PreviewContent(
            html = html,
            generating = state.generating,
            title = state.sheetTitle,
            onBack = vm::clearSheet,
            onPrint = { webView -> printSheet(context, webView) },
        )
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
private fun SetupContent(
    state: GenerateViewModel.UiState,
    onToggleTag: (Long) -> Unit,
    onMatchAll: (Boolean) -> Unit,
    onCount: (Int) -> Unit,
    onWithAnswers: (Boolean) -> Unit,
    onUseAi: (Boolean) -> Unit,
    onTitle: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    val c = wbColors()
    var showTitleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        WbPageHeader(
            title = "生成",
            kicker = "可打印练习卷",
            subtitle = state.sheetTitle,
            trailing = {
                WbIconButton(
                    icon = MiuixIcons.Edit,
                    contentDescription = "修改练习卷标题",
                    onClick = { showTitleDialog = true },
                    size = 40.dp,
                    iconSize = 22.dp,
                    tint = c.accent,
                )
            },
        )

        WbSection(
            header = "选择标签",
            footer = if (state.tags.isEmpty()) {
                null
            } else {
                "已选 ${state.selectedTagIds.size} 个标签；「全部满足」要求题目同时具备所有标签。"
            },
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (state.tags.isEmpty()) {
                    WbText(
                        text = "还没有标签；不选标签 = 从全部错题出题。",
                        fontSize = 13.sp,
                        color = c.secondaryLabel,
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WbChip(
                            label = if (state.matchAll) "全部满足" else "任一满足",
                            selected = true,
                            onClick = { onMatchAll(!state.matchAll) },
                        )
                        state.tags.forEach { tag ->
                            WbChip(
                                label = tag.name,
                                selected = tag.id in state.selectedTagIds,
                                onClick = { onToggleTag(tag.id) },
                            )
                        }
                    }
                }
            }
        }

        WbSection(header = "出题方式") {
            WbListRow(
                title = "整理错题",
                subtitle = "按标签抽原题，附解析",
                onClick = { onUseAi(false) },
                trailing = {
                    if (!state.useAi) {
                        WbIcon(
                            imageVector = MiuixIcons.Ok,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = c.accent,
                        )
                    }
                },
            )
            WbListRow(
                title = "AI 同类题",
                subtitle = "换个数字再练；失败自动回退为整理错题",
                onClick = { onUseAi(true) },
                showDivider = false,
                trailing = {
                    if (state.useAi) {
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

        WbSection(header = "题量与答案") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WbText(
                        text = "题量",
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        color = c.label,
                    )
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 20).forEach { n ->
                            WbChip(
                                label = "$n",
                                selected = state.count == n,
                                onClick = { onCount(n) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                WbSwitchRow(
                    title = "附答案解析",
                    summary = "打印在卷子末尾",
                    checked = state.withAnswers,
                    onCheckedChange = onWithAnswers,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        WbPrimaryButton(
            text = if (state.generating) "生成中…" else "生成练习卷",
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            enabled = !state.generating,
            icon = MiuixIcons.Edit,
        )

        WbText(
            text = "生成后可以预览，并调起系统打印（可选择「另存为 PDF」）。",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = c.secondaryLabel,
        )
    }

    WbInputDialog(
        visible = showTitleDialog,
        title = "练习卷标题",
        placeholder = "Errata 错题巩固练习",
        initialValue = state.sheetTitle,
        onConfirm = {
            onTitle(it)
            showTitleDialog = false
        },
        onDismiss = { showTitleDialog = false },
    )
}

@Composable
private fun PreviewContent(
    html: String,
    generating: Boolean,
    title: String,
    onBack: () -> Unit,
    onPrint: (WebView) -> Unit,
) {
    val c = wbColors()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loaded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(
            title = "练习卷预览",
            onBack = onBack,
            actions = {
                WbPrimaryButton(
                    text = if (!loaded || generating) "准备中…" else "打印 / PDF",
                    onClick = { webView?.let(onPrint) },
                    enabled = loaded && !generating,
                    icon = MiuixIcons.Share,
                )
            },
        )

        WbSurface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            cornerRadius = 12.dp,
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(c.cardBackground)) {
                if (!loaded) {
                    WbText(
                        text = "正在排版…",
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 14.sp,
                        color = c.secondaryLabel,
                    )
                }
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            settings.allowFileAccess = false
                            // 练习卷是打印纸，固定白底；界面容器仍用 Miuix surface。
                            setBackgroundColor(android.graphics.Color.WHITE)
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView, url: String?) {
                                    loaded = true
                                }
                            }
                            loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                            webView = this
                        }
                    },
                )
            }
        }
    }
}

/** 调系统打印；用户可以在打印面板里选「另存为 PDF」或直接连打印机。 */
private fun printSheet(context: Context, webView: WebView) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val adapter = webView.createPrintDocumentAdapter("Errata 练习卷")
    val attributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .build()
    printManager.print("Errata 练习卷", adapter, attributes)
}
