package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 单行/多行输入弹窗。 */
@Composable
fun WbInputDialog(
    visible: Boolean,
    title: String,
    placeholder: String,
    confirmText: String = "确定",
    initialValue: String = "",
    multiline: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    var value by remember(visible) { mutableStateOf(initialValue) }
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS || style == UiStyle.V090
    val c = wbColors()

    if (ios) {
        Dialog(onDismissRequest = onDismiss) {
            WbSurface(
                modifier = Modifier.width(300.dp),
                cornerRadius = 16.dp,
                elevation = 24.dp,
                underlay = c.cardBackground,
            ) {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    WbText(
                        text = title,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.label,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    WbTextField(
                        value = value,
                        onValueChange = { value = it },
                        placeholder = placeholder,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        singleLine = !multiline,
                    )
                    Spacer(Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(c.separator.copy(alpha = 0.55f)),
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        WbTextButton(
                            text = "取消",
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(48.dp)
                                .background(c.separator.copy(alpha = 0.55f)),
                        )
                        WbTextButton(
                            text = confirmText,
                            onClick = {
                                val trimmed = value.trim()
                                if (trimmed.isNotEmpty()) onConfirm(trimmed)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            color = c.accent,
                        )
                    }
                }
            }
        }
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(304.dp),
            cornerRadius = 20.dp,
            insideMargin = PaddingValues(20.dp),
        ) {
            Column {
                WbText(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                WbTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = placeholder,
                    singleLine = !multiline,
                )
                Spacer(Modifier.height(18.dp))
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
                    )
                }
            }
        }
    }
}
