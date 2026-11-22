package com.chenzixin.wrongbook.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.domain.chat.ChatQuestionExtractor
import com.chenzixin.wrongbook.domain.chat.ExtractedQuestion
import com.chenzixin.wrongbook.ui.component.WbSurface
import com.chenzixin.wrongbook.ui.component.WbChip
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSecondaryButton
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.component.WbText

/**
 * 把一条 AI 回答存成错题：先自动拆出题干/答案/解析，三段都能改，再选标签。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SaveQuestionFromChatDialog(
    rawText: String,
    allTags: List<TagEntity>,
    onSave: (stem: String, answer: String, analysis: String, tagIds: List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = wbColors()
    val parsed: ExtractedQuestion = remember(rawText) { ChatQuestionExtractor.extract(rawText) }
    var stem by remember(rawText) { mutableStateOf(parsed.stem) }
    var answer by remember(rawText) { mutableStateOf(parsed.answer) }
    var analysis by remember(rawText) { mutableStateOf(parsed.analysis) }
    var selected by remember(rawText) { mutableStateOf<Set<Long>>(emptySet()) }

    Dialog(onDismissRequest = onDismiss) {
        WbSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevation = 30.dp,
            underlay = colors.dialogScrim,
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                WbText(
                    text = "存为错题",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.label,
                )
                WbText(
                    text = "已自动拆分，识别不准的可以直接改",
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 12.sp,
                    color = colors.secondaryLabel,
                )

                Spacer(Modifier.height(14.dp))
                FieldLabel("题干")
                WbTextField(
                    value = stem,
                    onValueChange = { stem = it },
                    placeholder = "题干",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                )

                Spacer(Modifier.height(12.dp))
                FieldLabel("答案")
                WbTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    placeholder = "答案（可空）",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                )

                Spacer(Modifier.height(12.dp))
                FieldLabel("解析")
                WbTextField(
                    value = analysis,
                    onValueChange = { analysis = it },
                    placeholder = "解析（可空）",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                )

                if (allTags.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("标签")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        allTags.forEach { tag ->
                            WbChip(
                                label = tag.name,
                                selected = tag.id in selected,
                                onClick = {
                                    selected = if (tag.id in selected) selected - tag.id else selected + tag.id
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WbSecondaryButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    WbPrimaryButton(
                        text = "存入题库",
                        onClick = { onSave(stem, answer, analysis, selected.toList()) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    val colors = wbColors()
    WbText(
        text = text,
        modifier = Modifier.padding(bottom = 6.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.secondaryLabel,
    )
}
