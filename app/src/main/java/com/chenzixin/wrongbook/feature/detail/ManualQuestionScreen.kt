package com.chenzixin.wrongbook.feature.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.chenzixin.wrongbook.ui.component.WbChip
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoText

/**
 * 手动新建题目：没有图片也能建，适合抄错题本或老师口头讲的题。
 * 用 source = MANUAL 标记，题库里没有缩略图时显示占位。
 */
@Composable
fun ManualQuestionScreen(
    onBack: () -> Unit,
    vm: ManualQuestionViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val colors = wbColors()

    LaunchedEffect(state.finished) {
        if (state.finished) onBack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(
            title = "手动新建题目",
            onBack = onBack,
            actions = {
                CupertinoButton(onClick = vm::save, enabled = !state.saving) {
                    CupertinoText(text = if (state.saving) "保存中…" else "保存")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            WbSection(header = "题目", footer = "题干必填；答案是可选，之后可以用 AI 讲解补上。") {
                Column(modifier = Modifier.padding(16.dp)) {
                    WbTextField(
                        value = state.stem,
                        onValueChange = vm::onStem,
                        placeholder = "题干，例如：求函数 f(x)=x²-4x+3 的最小值",
                        singleLine = false,
                    )
                }
            }

            WbSection(header = "答案与解析") {
                Column(modifier = Modifier.padding(16.dp)) {
                    WbTextField(
                        value = state.answer,
                        onValueChange = vm::onAnswer,
                        placeholder = "答案（可空）",
                        singleLine = false,
                    )
                    Spacer(Modifier.height(12.dp))
                    WbTextField(
                        value = state.analysis,
                        onValueChange = vm::onAnalysis,
                        placeholder = "解析（可空）",
                        singleLine = false,
                    )
                    Spacer(Modifier.height(12.dp))
                    WbTextField(
                        value = state.subject,
                        onValueChange = vm::onSubject,
                        placeholder = "学科（可空），例如 数学",
                    )
                }
            }

            if (state.tags.isNotEmpty()) {
                WbSection(header = "标签") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        state.tags.forEach { tag ->
                            WbChip(
                                label = tag.name,
                                selected = tag.id in state.selectedTagIds,
                                onClick = { vm.toggleTag(tag.id) },
                            )
                        }
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
