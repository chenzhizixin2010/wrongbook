package com.chenzixin.wrongbook.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.data.local.entity.ChatSessionEntity
import com.chenzixin.wrongbook.ui.component.GlassSurface
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbGlassIconButton
import com.chenzixin.wrongbook.ui.component.WbPageHeader
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSectionLabel
import com.chenzixin.wrongbook.ui.component.wbTabBarSpace
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.Paperplane
import io.github.robinpcrd.cupertino.icons.outlined.Plus
import io.github.robinpcrd.cupertino.icons.outlined.Trash
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** AI 对话 Tab：会话列表（支持置顶与删除）。 */
@Composable
fun ChatTabScreen(
    onOpenSession: (Long) -> Unit,
    vm: ChatListViewModel = hiltViewModel(),
) {
    val sessions by vm.sessions.collectAsState()
    val created by vm.createdSessionId.collectAsState()
    val colors = wbColors()
    val bottomSpace = wbTabBarSpace()
    var pendingDelete by remember { mutableStateOf<ChatSessionEntity?>(null) }

    LaunchedEffect(created) {
        created?.let {
            vm.consumeCreated()
            onOpenSession(it)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomSpace),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item(key = "header") {
            WbPageHeader(
                title = "AI 对话",
                kicker = "AI 学习助手",
                subtitle = if (sessions.isEmpty()) {
                    "问思路、要同类题、让 AI 整理错题"
                } else {
                    "${sessions.size} 个会话 · 支持流式回复"
                },
                trailing = {
                    WbGlassIconButton(
                        icon = CupertinoIcons.Outlined.Plus,
                        contentDescription = "新建对话",
                        onClick = vm::createSession,
                        size = 46.dp,
                        iconSize = 19.dp,
                        tint = colors.tint,
                    )
                },
            )
        }

        item(key = "section") {
            Box(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                ),
            ) {
                WbSectionLabel(
                    text = "会话",
                    trailing = if (sessions.isEmpty()) null else "${sessions.size} 个",
                )
            }
        }

        if (sessions.isEmpty()) {
            item(key = "empty") {
                ChatEmptyState(
                    onCreate = vm::createSession,
                )
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                SessionCard(
                    session = session,
                    onClick = { onOpenSession(session.id) },
                    onDelete = { pendingDelete = session },
                )
                Spacer(Modifier.height(9.dp))
            }
        }
    }

    pendingDelete?.let { session ->
        WbAlertDialog(
            visible = true,
            title = "删除对话「${session.title}」？",
            message = "对话消息会一并删除，不可恢复。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                vm.deleteSession(session.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ChatEmptyState(onCreate: () -> Unit) {
    val colors = wbColors()
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 28.dp,
        elevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
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
                    modifier = Modifier.size(30.dp),
                    tint = Color.White,
                )
            }
            Spacer(Modifier.height(18.dp))
            CupertinoText(
                text = "还没有会话",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.label,
            )
            Spacer(Modifier.height(6.dp))
            CupertinoText(
                text = "先在设置里配好模型，再回来开始对话",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = colors.secondaryLabel,
            )
            Spacer(Modifier.height(20.dp))
            WbPrimaryButton(
                text = "新建对话",
                onClick = onCreate,
                icon = CupertinoIcons.Outlined.Plus,
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: ChatSessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = wbColors()
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 20.dp,
        elevation = 5.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(colors.accent.copy(alpha = 0.22f), colors.accentEnd.copy(alpha = 0.16f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CupertinoIcon(
                    imageVector = CupertinoIcons.Outlined.Paperplane,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.tint,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CupertinoText(
                        text = session.title,
                        modifier = Modifier.weight(1f, fill = false),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (session.pinned) {
                        Spacer(Modifier.width(6.dp))
                        CupertinoText(
                            text = "置顶",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.tint,
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                CupertinoText(
                    text = formatTime(session.updatedAt),
                    fontSize = 11.5.sp,
                    color = colors.secondaryLabel,
                )
            }

            Spacer(Modifier.width(6.dp))

            WbGlassIconButton(
                icon = CupertinoIcons.Outlined.Trash,
                contentDescription = "删除会话",
                onClick = onDelete,
                size = 34.dp,
                iconSize = 14.dp,
                tint = colors.danger,
                containerColor = colors.danger.copy(alpha = 0.12f),
            )
        }
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
