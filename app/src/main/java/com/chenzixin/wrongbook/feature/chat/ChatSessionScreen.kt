package com.chenzixin.wrongbook.feature.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.launch
import com.chenzixin.wrongbook.data.local.entity.ChatMessageEntity
import com.chenzixin.wrongbook.ui.component.MarkdownText
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoText

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
    val colors = wbColors()
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

    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(title = session?.title ?: "对话", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        ) {
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
                    .background(colors.groupedBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pendingImages.forEach { relative ->
                    Box {
                        AsyncImage(
                            model = File(context.filesDir, relative),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        CupertinoText(
                            text = "✕",
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.cardBackground)
                                .clickable { pendingImages = pendingImages - relative }
                                .padding(horizontal = 4.dp),
                            fontSize = 11.sp,
                            color = colors.danger,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.groupedBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WbTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = "输入问题，例如「这道题的第二步为什么成立」",
                    singleLine = false,
                )
            }
            CupertinoButton(onClick = { pickImage.launch("image/*") }) { CupertinoText(text = "＋图") }
            Spacer(Modifier.width(8.dp))
            if (streaming != null) {
                CupertinoButton(onClick = vm::stop) { CupertinoText(text = "停止") }
            } else {
                CupertinoButton(
                    onClick = {
                        val text = input
                        val images = pendingImages
                        input = ""
                        pendingImages = emptyList()
                        vm.send(text, images)
                    },
                ) { CupertinoText(text = "发送") }
            }
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
private fun MessageBubble(message: ChatMessageEntity) {
    if (message.role == "user") {
        val colors = wbColors()
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
                )
            }
            CupertinoText(
                text = message.content,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.tint)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 16.sp,
                color = androidx.compose.ui.graphics.Color.White,
            )
        }
    } else {
        AssistantBubble(text = message.content, streaming = false)
    }
}

@Composable
private fun AssistantBubble(text: String, streaming: Boolean, thinking: String = "") {
    val colors = wbColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CupertinoText(text = "AI 生成", fontSize = 11.sp, color = colors.tint)
            if (streaming) {
                CupertinoText(text = " · 生成中…", fontSize = 11.sp, color = colors.secondaryLabel)
            }
        }
        Spacer(Modifier.height(6.dp))
        if (text.isBlank()) {
            CupertinoText(
                text = if (thinking.isNotBlank()) "思考中…" else "…",
                fontSize = 16.sp,
                color = colors.secondaryLabel,
            )
            if (thinking.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                CupertinoText(
                    text = thinking.takeLast(240),
                    fontSize = 12.sp,
                    color = colors.secondaryLabel,
                )
            }
        } else {
            MarkdownText(
                markdown = text,
                textColor = colors.label.toArgb(),
                textSizeSp = 16f,
            )
        }
        if (text.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            CupertinoText(
                text = "内容由 AI 生成，请自行核对",
                fontSize = 11.sp,
                color = colors.secondaryLabel,
            )
        }
    }
}
