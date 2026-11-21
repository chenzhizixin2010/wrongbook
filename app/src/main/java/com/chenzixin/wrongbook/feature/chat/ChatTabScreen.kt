package com.chenzixin.wrongbook.feature.chat

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.data.local.entity.ChatSessionEntity
import com.chenzixin.wrongbook.ui.component.WbLargeTitle
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** AI 对话 Tab：会话列表。 */
@Composable
fun ChatTabScreen(
    onOpenSession: (Long) -> Unit,
    vm: ChatListViewModel = hiltViewModel(),
) {
    val sessions by vm.sessions.collectAsState()
    val created by vm.createdSessionId.collectAsState()
    val colors = wbColors()

    LaunchedEffect(created) {
        created?.let {
            vm.consumeCreated()
            onOpenSession(it)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                WbLargeTitle(text = "AI 对话")
            }
            CupertinoButton(onClick = vm::createSession) { CupertinoText(text = "新建") }
        }

        if (sessions.isEmpty()) {
            WbSection(
                header = "会话",
                footer = "先在「设置 → 模型与 API」配置一个可用的模型，再开始对话。",
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    CupertinoText(
                        text = "还没有会话，点右上角「新建」开始",
                        fontSize = 15.sp,
                        color = colors.secondaryLabel,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        onClick = { onOpenSession(session.id) },
                        onLongClick = { vm.togglePinned(session) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: ChatSessionEntity, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = wbColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (session.pinned) {
                CupertinoText(text = "📌 ", fontSize = 14.sp, color = colors.tint)
            }
            CupertinoText(text = session.title, fontSize = 16.sp, color = colors.label)
        }
        CupertinoText(
            text = formatTime(session.updatedAt),
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 12.sp,
            color = colors.secondaryLabel,
        )
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
