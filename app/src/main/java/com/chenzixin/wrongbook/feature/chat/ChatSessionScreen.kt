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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.chenzixin.wrongbook.data.local.entity.ChatMessageEntity
import com.chenzixin.wrongbook.ui.component.GlassSurface
import com.chenzixin.wrongbook.ui.component.MarkdownText
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbChip
import com.chenzixin.wrongbook.ui.component.WbGlassIconButton
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.component.WbTextButton
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.Paperplane
import io.github.robinpcrd.cupertino.icons.outlined.PlusCircle
import io.github.robinpcrd.cupertino.icons.outlined.Xmark
import java.io.File
import kotlinx.coroutines.launch

/** 单个会话：消息流 + 输入框；AI 回复走 SSE 流式渲染。 */
@Composable
fun ChatSessionScreen(
    onBack: () -> Unit,
    vm: ChatSessionViewModel = hiltViewModel(),
) {
    val session by vm.session.collectAsState()
    val messages by vm.messages.collectAsState()
    val streaming by vm.streaming.collectAsState()
    val allTags by vm.allTags.collectAsState()
    var savingText by remember { mutableStateOf<String?>(null) }
    var pickingQuestion by remember { mutableStateOf(false) }
    val questions by vm.questions.collectAsState()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        WbTopBar(
            title = session?.title ?: "对话",
            onBack = onBack,
            actions = {
                WbTextButton(text = "选题", onClick = { pickingQuestion = true })
                if (streaming == null) {
                    WbTextButton(
                        text = "继续",
                        onClick = { vm.send("请从上次中断的地方继续，不要重复已经给过的内容。") },
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 6.dp,
                bottom = 10.dp,
            ),
        ) {
            if (messages.isEmpty() && streaming == null) {
                item(key = "welcome") {
                    ChatWelcome(onPick = { input = it })
                }
            }
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    onSaveAsQuestion = { text -> savingText = text },
                )
            }
            if (streaming != null) {
                item(key = "streaming") {
                    AssistantBubble(text = streaming.orEmpty(), streaming = true, thinking = thinking)
                }
            }
        }

        if (pickingQuestion) {
            QuestionPickerDialog(
                questions = questions,
                onPick = { item ->
                    val stem = (item.question.refinedText ?: item.question.rawOcrText).trim()
                    input = "【题目】$stem\n\n【我的问题】"
                    pickingQuestion = false
                },
                onDismiss = { pickingQuestion = false },
            )
        }

        SaveQuestionFromChatHost(
            vm = vm,
            savingText = savingText,
            allTags = allTags,
            onDismiss = { savingText = null },
        )

        if (pendingImages.isNotEmpty()) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 6.dp),
                cornerRadius = 18.dp,
                elevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    pendingImages.forEach { relative ->
                        Box {
                            AsyncImage(
                                model = File(context.filesDir, relative),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(17.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { pendingImages = pendingImages - relative },
                                contentAlignment = Alignment.Center,
                            ) {
                                CupertinoIcon(
                                    imageVector = CupertinoIcons.Outlined.Xmark,
                                    contentDescription = "移除图片",
                                    modifier = Modifier.size(8.dp),
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        Composer(
            input = input,
            onInputChange = { input = it },
            streaming = streaming != null,
            onPickImage = { pickImage.launch("image/*") },
            onStop = vm::stop,
            onSend = {
                val text = input
                val images = pendingImages
                input = ""
                pendingImages = emptyList()
                vm.send(text, images)
            },
        )
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

/** 输入区：玻璃胶囊，左附图、右发送。 */
@Composable
private fun Composer(
    input: String,
    onInputChange: (String) -> Unit,
    streaming: Boolean,
    onPickImage: () -> Unit,
    onStop: () -> Unit,
    onSend: () -> Unit,
) {
    val colors = wbColors()
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 6.dp, bottom = 10.dp),
        cornerRadius = 26.dp,
        elevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            WbGlassIconButton(
                icon = CupertinoIcons.Outlined.PlusCircle,
                contentDescription = "添加图片",
                onClick = onPickImage,
                size = 38.dp,
                iconSize = 17.dp,
                tint = colors.secondaryLabel,
            )
            Spacer(Modifier.width(6.dp))

            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 9.dp),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = colors.label,
                ),
                cursorBrush = SolidColor(colors.tint),
                maxLines = 4,
                decorationBox = { inner ->
                    Box {
                        if (input.isEmpty()) {
                            CupertinoText(
                                text = "问思路、要同类题…",
                                fontSize = 15.sp,
                                color = colors.secondaryLabel.copy(alpha = 0.8f),
                                maxLines = 1,
                            )
                        }
                        inner()
                    }
                },
            )

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (streaming) {
                            Brush.linearGradient(
                                listOf(colors.danger, colors.danger.copy(alpha = 0.8f)),
                            )
                        } else {
                            Brush.linearGradient(listOf(colors.accent, colors.accentEnd))
                        },
                    )
                    .clickable {
                        if (streaming) onStop() else if (input.isNotBlank()) onSend()
                    },
                contentAlignment = Alignment.Center,
            ) {
                CupertinoIcon(
                    imageVector = if (streaming) CupertinoIcons.Outlined.Xmark else CupertinoIcons.Outlined.Paperplane,
                    contentDescription = if (streaming) "停止生成" else "发送",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SaveQuestionFromChatHost(
    vm: ChatSessionViewModel,
    savingText: String?,
    allTags: List<TagEntity>,
    onDismiss: () -> Unit,
) {
    val text = savingText ?: return
    SaveQuestionFromChatDialog(
        rawText = text,
        allTags = allTags,
        onSave = { stem, answer, analysis, tagIds ->
            vm.saveAsQuestion(stem, answer, analysis, tagIds)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun ChatWelcome(onPick: (String) -> Unit) {
    val colors = wbColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(colors.accent.copy(alpha = 0.95f), colors.accentEnd.copy(alpha = 0.9f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            CupertinoIcon(
                imageVector = CupertinoIcons.Outlined.Paperplane,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Color.White,
            )
        }
        Spacer(Modifier.height(16.dp))
        CupertinoText(
            text = "想从哪里开始？",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.label,
        )
        Spacer(Modifier.height(6.dp))
        CupertinoText(
            text = "可以带图提问，也可以从题库带题进来",
            fontSize = 12.5.sp,
            color = colors.secondaryLabel,
        )
        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "这道题的第二步为什么成立？",
                "帮我把这道错题整理成干净题干和解析",
                "出两道同类题，换几个数字练手",
            ).forEach { prompt ->
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    elevation = 3.dp,
                    onClick = { onPick(prompt) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CupertinoText(
                            text = prompt,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = colors.label,
                            maxLines = 1,
                        )
                        CupertinoIcon(
                            imageVector = CupertinoIcons.Outlined.Paperplane,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = colors.tint.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageEntity,
    onSaveAsQuestion: ((String) -> Unit)? = null,
) {
    if (message.role == "user") {
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
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                wbColors().accent.copy(alpha = 0.92f),
                                wbColors().accentEnd.copy(alpha = 0.88f),
                            ),
                        ),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                CupertinoText(
                    text = message.content,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = Color.White,
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            AssistantBubble(text = message.content, streaming = false)
            if (onSaveAsQuestion != null && message.content.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    WbTextButton(
                        text = "存为错题",
                        onClick = { onSaveAsQuestion(message.content) },
                        fontSize = 12,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantBubble(text: String, streaming: Boolean, thinking: String = "") {
    val colors = wbColors()
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(colors.accent, colors.accentEnd)),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    CupertinoText(text = "A", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(7.dp))
                CupertinoText(
                    text = "AI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.tint,
                )
                if (streaming) {
                    CupertinoText(
                        text = " · 生成中…",
                        fontSize = 10.5.sp,
                        color = colors.secondaryLabel,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (text.isBlank()) {
                CupertinoText(
                    text = if (thinking.isNotBlank()) "思考中…" else "…",
                    fontSize = 15.sp,
                    color = colors.secondaryLabel,
                )
                if (thinking.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    CupertinoText(
                        text = thinking.takeLast(240),
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        color = colors.secondaryLabel.copy(alpha = 0.8f),
                    )
                }
            } else {
                MarkdownText(
                    markdown = text,
                    textColor = colors.label.toArgb(),
                    textSizeSp = 15.5f,
                )
            }
            if (text.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                CupertinoText(
                    text = "内容由 AI 生成，请自行核对",
                    fontSize = 10.sp,
                    color = colors.secondaryLabel.copy(alpha = 0.75f),
                )
            }
        }
    }
}
