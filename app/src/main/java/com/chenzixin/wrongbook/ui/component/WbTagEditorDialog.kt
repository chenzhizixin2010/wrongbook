package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.component.WbText

/**
 * 标签编辑弹窗：点一下立即生效（不再要求「保存」），
 * 顺便支持现场新建标签 —— 已入库的题也能随时补标签。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WbTagEditorDialog(
    visible: Boolean,
    allTags: List<TagEntity>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val colors = wbColors()
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        WbSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevation = 30.dp,
            underlay = colors.dialogScrim,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WbText(
                        text = "编辑标签",
                        modifier = Modifier.weight(1f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.label,
                    )
                    WbTextButton(text = "完成", onClick = onDismiss)
                }

                Spacer(Modifier.height(12.dp))

                if (allTags.isEmpty()) {
                    WbText(
                        text = "还没有标签，先建一个吧",
                        fontSize = 13.sp,
                        color = colors.secondaryLabel,
                    )
                    Spacer(Modifier.height(10.dp))
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            allTags.forEach { tag ->
                                WbChip(
                                    label = tag.name,
                                    selected = tag.id in selectedIds,
                                    onClick = { onToggle(tag.id) },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (creating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            WbTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                placeholder = "新标签名",
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        WbPrimaryButton(
                            text = "建立",
                            onClick = {
                                if (newName.isNotBlank()) {
                                    onCreate(newName.trim())
                                    newName = ""
                                    creating = false
                                }
                            },
                            fontSize = 14,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.groupedBackground)
                            .clickable { creating = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        WbText(
                            text = "＋ 新建标签",
                            fontSize = 14.sp,
                            color = colors.tint,
                        )
                    }
                }
            }
        }
    }
}
