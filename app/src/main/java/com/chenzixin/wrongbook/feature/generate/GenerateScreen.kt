package com.chenzixin.wrongbook.feature.generate

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.ui.component.GlassSurface
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbChip
import com.chenzixin.wrongbook.ui.component.WbGlassIconButton
import com.chenzixin.wrongbook.ui.component.WbInputDialog
import com.chenzixin.wrongbook.ui.component.WbPageHeader
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.ui.component.WbTextButton
import com.chenzixin.wrongbook.ui.component.wbTabBarSpace
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.ChevronBackward
import io.github.robinpcrd.cupertino.icons.outlined.ListBullet
import io.github.robinpcrd.cupertino.icons.outlined.Paperplane
import io.github.robinpcrd.cupertino.icons.outlined.SquareAndArrowUp
import io.github.robinpcrd.cupertino.icons.outlined.SquareAndPencil
import io.github.robinpcrd.cupertino.icons.outlined.Star

/**
 * 生成 Tab：选标签 → 整理错题 / AI 出同类题 → 可打印版（系统打印服务，可另存 PDF）。
 *
 * 信息层级：先选「出什么」，再选「出多少」，最后才填标题——把最影响结果的选项放最上面。
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
            onBack = vm::clearSheet,
            onPrint = { webView -> printSheet(context, webView) },
        )
    }

    state.pendingPlan?.let { plan ->
        WbAlertDialog(
            visible = true,
            title = "有 ${plan.missingCount} 处缺少答案或解析",
            message = buildString {
                append("本次选的 ${plan.items.size} 道题里：")
                if (plan.missingAnswer > 0) append("${plan.missingAnswer} 道没有答案；")
                if (plan.missingAnalysis > 0) append("${plan.missingAnalysis} 道没有解析。")
                append("要用 AI 补全吗？补全结果会同时写回题库。")
            },
            confirmText = "AI 补全",
            onConfirm = vm::confirmFill,
            dismissText = "跳过",
            onDismiss = vm::skipFill,
        )
    }

    if (state.warning != null) {
        WbAlertDialog(
            visible = true,
            title = "已生成，但有问题",
            message = state.warning,
            confirmText = "知道了",
            onConfirm = vm::dismissWarning,
            onDismiss = vm::dismissWarning,
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
    val colors = wbColors()
    val bottomSpace = wbTabBarSpace()
    var showTitleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomSpace),
    ) {
        WbPageHeader(
            title = "生成",
            kicker = "可打印练习卷",
            subtitle = "按标签抽题，AI 也可以出同类题",
            trailing = {
                WbGlassIconButton(
                    icon = CupertinoIcons.Outlined.SquareAndPencil,
                    contentDescription = "修改练习卷标题",
                    onClick = { showTitleDialog = true },
                    size = 46.dp,
                    iconSize = 18.dp,
                    tint = colors.tint,
                )
            },
        )

        WbSection(
            header = "选择标签",
            icon = CupertinoIcons.Outlined.Star,
            footer = if (state.selectedTagIds.isEmpty()) {
                null
            } else {
                "已选 ${state.selectedTagIds.size} 个标签；「全部满足」要求题目同时具备所有标签。"
            },
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.tags.isNotEmpty()) {
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
                if (state.tags.isEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    CupertinoText(
                        text = "还没有标签；不选标签 = 从全部错题出题。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = colors.secondaryLabel,
                    )
                }
            }
        }

        WbSection(
            header = "出题方式",
            icon = CupertinoIcons.Outlined.SquareAndPencil,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeTile(
                        icon = CupertinoIcons.Outlined.ListBullet,
                        title = "整理错题",
                        subtitle = "按标签抽原题",
                        selected = !state.useAi,
                        onClick = { onUseAi(false) },
                        modifier = Modifier.weight(1f),
                    )
                    ModeTile(
                        icon = CupertinoIcons.Outlined.Paperplane,
                        title = "AI 同类题",
                        subtitle = "换个数字再练",
                        selected = state.useAi,
                        onClick = { onUseAi(true) },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (state.useAi && !state.aiAvailable) {
                    CupertinoText(
                        text = "没有可用的 API 配置，AI 出题会退回整理错题。去「设置 → 模型与 API」确认 Key 已保存。",
                        modifier = Modifier.padding(top = 10.dp),
                        fontSize = 12.sp,
                        color = colors.danger,
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CupertinoText(
                        text = "题量",
                        modifier = Modifier.width(34.dp),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.secondaryLabel,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(5, 10, 15, 20).forEach { n ->
                            WbChip(
                                label = "$n 题",
                                selected = state.count == n,
                                onClick = { onCount(n) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onWithAnswers(!state.withAnswers) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CupertinoText(
                        text = "附答案解析",
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = colors.label,
                    )
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (state.withAnswers) colors.tint else colors.secondaryLabel.copy(alpha = 0.22f)),
                        contentAlignment = if (state.withAnswers) Alignment.CenterEnd else Alignment.CenterStart,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .size(22.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color.White),
                        )
                    }
                }
            }
        }

        WbPrimaryButton(
            text = if (state.generating) "生成中…" else "生成练习卷",
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            enabled = !state.generating,
            icon = CupertinoIcons.Outlined.SquareAndPencil,
        )
        Spacer(Modifier.height(4.dp))
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
private fun ModeTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = wbColors()
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) {
                    Brush.linearGradient(
                        listOf(colors.accent.copy(alpha = 0.14f), colors.accentEnd.copy(alpha = 0.10f)),
                    )
                } else {
                    Brush.linearGradient(
                        listOf(colors.glassFill, colors.glassFill.copy(alpha = 0.5f)),
                    )
                },
            )
            .border(
                width = 1.dp,
                color = if (selected) colors.tint.copy(alpha = 0.45f) else colors.glassStrokeTop.copy(alpha = 0.4f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) colors.tint.copy(alpha = 0.18f) else colors.secondaryLabel.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                CupertinoIcon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (selected) colors.tint else colors.secondaryLabel,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        CupertinoText(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.label,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        CupertinoText(
            text = subtitle,
            fontSize = 10.5.sp,
            color = colors.secondaryLabel,
            maxLines = 1,
        )
    }
}

@Composable
private fun PreviewContent(
    html: String,
    generating: Boolean,
    onBack: () -> Unit,
    onPrint: (WebView) -> Unit,
) {
    val colors = wbColors()
    val bottomSpace = wbTabBarSpace()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loaded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomSpace),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WbGlassIconButton(
                icon = CupertinoIcons.Outlined.ChevronBackward,
                contentDescription = "返回修改",
                onClick = onBack,
                size = 40.dp,
                iconSize = 17.dp,
                tint = colors.tint,
            )
            CupertinoText(
                text = "练习卷预览",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.label,
                maxLines = 1,
            )
            WbPrimaryButton(
                text = if (!loaded || generating) "准备中…" else "打印 / PDF",
                onClick = { webView?.let(onPrint) },
                enabled = loaded && !generating,
                fontSize = 13,
                icon = CupertinoIcons.Outlined.SquareAndArrowUp,
            )
        }

        GlassSurface(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            cornerRadius = 22.dp,
            elevation = 10.dp,
        ) {
            if (!loaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CupertinoText(
                        text = "正在排版…",
                        fontSize = 13.sp,
                        color = colors.secondaryLabel,
                    )
                }
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp)),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = false
                        settings.allowFileAccess = false
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

/** 调系统打印；用户可以在打印面板里选「另存为 PDF」或直接连打印机。 */
private fun printSheet(context: Context, webView: WebView) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val adapter = webView.createPrintDocumentAdapter("Errata 练习卷")
    val attributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .build()
    printManager.print("Errata 练习卷", adapter, attributes)
}
