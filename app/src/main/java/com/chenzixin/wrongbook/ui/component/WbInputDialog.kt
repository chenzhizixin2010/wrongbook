package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoText

/** 单行输入弹窗（新建标签、重命名等）。 */
@Composable
fun WbInputDialog(
    visible: Boolean,
    title: String,
    placeholder: String,
    confirmText: String = "确定",
    initialValue: String = "",
    /** 多行模式：编辑题目正文时用，输入框拉高、允许换行 */
    multiline: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val colors = wbColors()
    var value by remember(visible) { mutableStateOf(initialValue) }

    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            modifier = Modifier.width(304.dp),
            cornerRadius = 30.dp,
            elevation = 34.dp,
            underlay = colors.dialogScrim,
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                CupertinoText(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.label,
                )
                Spacer(Modifier.height(14.dp))
                WbTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = placeholder,
                    singleLine = !multiline,
                    modifier = if (multiline) {
                        Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    },
                )
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    WbSecondaryButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    WbPrimaryButton(
                        text = confirmText,
                        onClick = {
                            val trimmed = value.trim()
                            if (trimmed.isNotEmpty()) onConfirm(trimmed)
                        },
                        modifier = Modifier.weight(1f),
                        fontSize = 15,
                    )
                }
            }
        }
    }
}
