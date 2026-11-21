package com.chenzixin.wrongbook.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chenzixin.wrongbook.data.local.model.QuestionWithTags
import com.chenzixin.wrongbook.ui.component.GlassSurface
import com.chenzixin.wrongbook.ui.component.WbTextButton
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoText

/**
 * 从题库挑一道题带进对话：搜索 + 列表，选中后把题干插进输入框。
 *
 * 这是「题库 ↔ 对话」双向联动里对话侧的那一半；
 * 题库侧（详情页 → AI 讲解）走的是另一条路由。
 */
@Composable
fun QuestionPickerDialog(
    questions: List<QuestionWithTags>,
    onPick: (QuestionWithTags) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = wbColors()
    var keyword by remember { mutableStateOf("") }
    val filtered = remember(questions, keyword) {
        if (keyword.isBlank()) questions.take(50) else questions.filter { item ->
            val text = (item.question.refinedText ?: item.question.rawOcrText)
            text.contains(keyword, ignoreCase = true)
        }.take(50)
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevation = 30.dp,
            underlay = colors.dialogScrim,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row {
                    CupertinoText(
                        text = "从题库选题",
                        modifier = Modifier.weight(1f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.label,
                    )
                    WbTextButton(text = "关闭", onClick = onDismiss)
                }

                Spacer(Modifier.height(10.dp))
                WbTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    placeholder = "搜索题干",
                )
                Spacer(Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    CupertinoText(
                        text = if (questions.isEmpty()) "题库还是空的" else "没有匹配的题目",
                        modifier = Modifier.padding(vertical = 24.dp),
                        fontSize = 13.sp,
                        color = colors.secondaryLabel,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(filtered, key = { it.question.id }) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(item) }
                                    .padding(vertical = 10.dp),
                            ) {
                                CupertinoText(
                                    text = (item.question.refinedText ?: item.question.rawOcrText)
                                        .replace("\n", " ")
                                        .take(60),
                                    fontSize = 14.sp,
                                    color = colors.label,
                                    maxLines = 2,
                                )
                                if (item.tags.isNotEmpty()) {
                                    CupertinoText(
                                        text = item.tags.joinToString(" · ") { it.name },
                                        modifier = Modifier.padding(top = 2.dp),
                                        fontSize = 11.sp,
                                        color = colors.secondaryLabel,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
