package com.chenzixin.wrongbook.feature.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.data.local.entity.ChatSessionEntity
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbBadge
import com.chenzixin.wrongbook.ui.component.WbIconButton
import com.chenzixin.wrongbook.ui.component.WbIcon
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbPageHeader
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbText
import com.chenzixin.wrongbook.ui.theme.wbColors
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
    val c = wbColors()
    var pendingDelete by remember { mutableStateOf<ChatSessionEntity?>(null) }

    LaunchedEffect(created) {
        created?.let {
            vm.consumeCreated()
            onOpenSession(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp),
    ) {
        WbPageHeader(
            title = "AI 对话",
            kicker = "AI 学习助手",
            subtitle = if (sessions.isEmpty()) {
                "问思路、要同类题、让 AI 整理错题"
            } else {
                "${sessions.size} 个会话 · 支持流式回复"
            },
            trailing = {
                WbIconButton(
                    icon = MiuixIcons.Add,
                    contentDescription = "新建对话",
                    onClick = vm::createSession,
                    size = 40.dp,
                    iconSize = 22.dp,
                    tint = c.accent,
                )
            },
        )

        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WbIcon(
                    imageVector = MiuixIcons.Send,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = c.secondaryLabel,
                )
                Spacer(Modifier.height(16.dp))
                WbText(
                    text = "还没有会话",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = c.label,
                )
                Spacer(Modifier.height(6.dp))
                WbText(
                    text = "先在设置里配好模型，再回来开始对话",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = c.secondaryLabel,
                )
                Spacer(Modifier.height(20.dp))
                WbPrimaryButton(
                    text = "新建对话",
                    onClick = vm::createSession,
                    icon = MiuixIcons.Add,
                )
            }
        } else {
            WbSection(header = "会话") {
                sessions.forEachIndexed { index, session ->
                    WbListRow(
                        title = session.title + if (session.pinned) "  ·  置顶" else "",
                        subtitle = formatTime(session.updatedAt),
                        icon = MiuixIcons.Send,
                        showDivider = index != sessions.lastIndex,
                        onClick = { onOpenSession(session.id) },
                        trailing = {
                            WbIconButton(
                                icon = MiuixIcons.Delete,
                                contentDescription = "删除会话",
                                onClick = { pendingDelete = session },
                                size = 34.dp,
                                iconSize = 20.dp,
                                tint = c.danger,
                            )
                        },
                    )
                }
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

private fun formatTime(millis: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
