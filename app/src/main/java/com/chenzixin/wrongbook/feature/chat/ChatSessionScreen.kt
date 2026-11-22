package com.chenzixin.wrongbook.feature.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.chenzixin.wrongbook.data.local.entity.ChatMessageEntity
import com.chenzixin.wrongbook.ui.component.MarkdownText
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbIcon
import com.chenzixin.wrongbook.ui.component.WbIconButton
import com.chenzixin.wrongbook.ui.component.WbSurface
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbText
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

/** 单个会话：消息流 + 输入框；AI 回复走 SSE 流式渲染。 */
@Composable
fun ChatSessionScreen(
    onBack: () -> Unit,
    vm: ChatSessionViewModel = hiltViewModel(),
) {
    val session by vm.session.collectAsState()
    val messages by vm.messages.collectAsState()
    val streaming by vm.streaming.collectAsState()
    val thinking by vm.thinking.collectAsState()
    val status by vm.status.collectAsState()
    val c = wbColors()
    var input by remember { mutableStateOf("") }
    var pendingImages by remember { mutableStateOf<List<String>>(emptyList()) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                vm.stageImage(it)?.let { path -> pendingImages = pendingImages + path }
            }
        }
    }

    val itemCount = messages.size + if (streaming != null) 1 else 0
    LaunchedEffect(itemCount, streaming?.length) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        WbTopBar(title = session?.title ?: "对话", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 10.dp),
        ) {
            if (messages.isEmpty() && streaming == null) {
                item(key = "welcome") {
                    ChatWelcome(onPick = { input = it })
                }
            }
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
            if (streaming != null) {
                item(key = "streaming") {
                    AssistantBubble(text = streaming.orEmpty(), streaming = true, thinking = thinking)
                }
            }
        }

        if (pendingImages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.cardBackground)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pendingImages.forEach { relative ->
                    Box {
                        AsyncImage(
                            model = File(context.filesDir, relative),
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(c.groupedBackground)
                                .clickable { pendingImages = pendingImages - relative },
                            contentAlignment = Alignment.Center,
                        ) {
                            WbIcon(
                                imageVector = MiuixIcons.Close,
                                contentDescription = "移除图片",
                                modifier = Modifier.size(12.dp),
                                tint = c.label,
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.cardBackground)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            WbIconButton(
                icon = MiuixIcons.AddCircle,
                contentDescription = "添加图片",
                onClick = { pickImage.launch("image/*") },
                size = 40.dp,
                iconSize = 24.dp,
                tint = c.accent,
            )
            Spacer(Modifier.width(6.dp))
            WbTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = "问思路、要同类题…",
                modifier = Modifier.weight(1f),
                singleLine = false,
            )
            Spacer(Modifier.width(6.dp))
            WbIconButton(
                icon = if (streaming != null) MiuixIcons.Close else MiuixIcons.Send,
                contentDescription = if (streaming != null) "停止生成" else "发送",
                onClick = {
                    if (streaming != null) {
                        vm.stop()
                    } else if (input.isNotBlank()) {
                        val text = input
                        val images = pendingImages
                        input = ""
                        pendingImages = emptyList()
                        vm.send(text, images)
                    }
                },
                size = 40.dp,
                iconSize = 20.dp,
                tint = c.onAccent,
                containerColor = if (streaming != null) c.danger else c.accent,
            )
        }
    }

    if (status != null) {
        WbAlertDialog(
            visible = true,
            title = "提示",
            message = status,
            confirmText = "知道了",
            onConfirm = vm::clearStatus,
            onDismiss = vm::clearStatus,
        )
    }
}

@Composable
private fun ChatWelcome(onPick: (String) -> Unit) {
    val c = wbColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(c.tintSoft),
            contentAlignment = Alignment.Center,
        ) {
            WbIcon(
                imageVector = MiuixIcons.Send,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = c.accent,
            )
        }
        Spacer(Modifier.height(16.dp))
        WbText(
            text = "想从哪里开始？",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = c.label,
        )
        Spacer(Modifier.height(6.dp))
        WbText(
            text = "可以带图提问，也可以从题库带题进来",
            fontSize = 14.sp,
            color = c.secondaryLabel,
        )
        Spacer(Modifier.height(20.dp))
        WbSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(0.dp),
        ) {
            Column {
            listOf(
                "这道题的第二步为什么成立？",
                "整理成干净题干和解析",
                "出两道同类题练手",
            ).forEachIndexed { index, prompt ->
                WbListRow(
                    title = prompt,
                    showDivider = index != 2,
                    onClick = { onPick(prompt) },
                )
            }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessageEntity) {
    if (message.role == "user") {
        val c = wbColors()
        val v090 = wbUiStyle() == UiStyle.V090
        val context = LocalContext.current
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            message.imagePaths.split("\n").filter { it.isNotBlank() }.forEach { path ->
                AsyncImage(
                    model = File(context.filesDir, path),
                    contentDescription = null,
                    modifier = Modifier
                        .size(140.dp)
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                    .background(
                        if (v090) {
                            Brush.linearGradient(listOf(c.accent, c.accentEnd))
                        } else {
                            SolidColor(c.accent)
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                WbText(
                    text = message.content,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = c.onAccent,
                )
            }
        }
    } else {
        AssistantBubble(text = message.content, streaming = false)
    }
}

@Composable
private fun AssistantBubble(text: String, streaming: Boolean, thinking: String = "") {
    val c = wbColors()
    WbSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(c.accent),
                contentAlignment = Alignment.Center,
            ) {
                WbText(
                    text = "A",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.onAccent,
                )
            }
            Spacer(Modifier.width(6.dp))
            WbText(
                text = "AI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = c.accent,
            )
            if (streaming) {
                WbText(
                    text = " · 生成中…",
                    fontSize = 11.sp,
                    color = c.secondaryLabel,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (text.isBlank()) {
            WbText(
                text = if (thinking.isNotBlank()) "思考中…" else "…",
                fontSize = 16.sp,
                color = c.secondaryLabel,
            )
            if (thinking.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                WbText(
                    text = thinking.takeLast(240),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = c.secondaryLabel,
                )
            }
        } else {
            MarkdownText(
                markdown = text,
                textColor = c.label.toArgb(),
                textSizeSp = 16f,
            )
        }
        if (text.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            WbText(
                text = "内容由 AI 生成，请自行核对",
                fontSize = 11.sp,
                color = c.secondaryLabel,
            )
        }
        }
    }
}
