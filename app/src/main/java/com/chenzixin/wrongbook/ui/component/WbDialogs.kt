package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TextButton as M3TextButton
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 二次确认弹窗：Miuix 实心 Card；iOS 玻璃 Alert + 横向按钮。 */
@Composable
fun WbAlertDialog(
    visible: Boolean,
    title: String,
    message: String? = null,
    confirmText: String = "确定",
    dismissText: String = "取消",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS || style == UiStyle.V090
    val md3e = style == UiStyle.MD3E
    val c = wbColors()

    if (md3e) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                M3Text(text = title, style = M3MaterialTheme.typography.headlineSmall)
            },
            text = if (message != null) {
                { M3Text(text = message, style = M3MaterialTheme.typography.bodyMedium) }
            } else {
                null
            },
            confirmButton = {
                M3TextButton(onClick = onConfirm) {
                    M3Text(text = confirmText, color = if (destructive) M3MaterialTheme.colorScheme.error else M3MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                M3TextButton(onClick = onDismiss) {
                    M3Text(text = dismissText)
                }
            },
            shape = M3MaterialTheme.shapes.extraLarge,
            containerColor = M3MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        return
    }

    if (ios) {
        Dialog(onDismissRequest = onDismiss) {
            WbSurface(
                modifier = Modifier.width(288.dp),
                cornerRadius = 16.dp,
                elevation = 24.dp,
                underlay = c.cardBackground,
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WbText(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.label,
                        textAlign = TextAlign.Center,
                    )
                    if (message != null) {
                        WbText(
                            text = message,
                            modifier = Modifier.padding(top = 6.dp),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = c.secondaryLabel,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 18.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(c.separator.copy(alpha = 0.55f)),
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        WbTextButton(
                            text = dismissText,
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
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            color = if (destructive) c.danger else c.accent,
                        )
                    }
                }
            }
        }
        return
    }

    val mc = MiuixTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(304.dp),
            cornerRadius = 20.dp,
            insideMargin = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WbText(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = mc.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (message != null) {
                    WbText(
                        text = message,
                        modifier = Modifier.padding(top = 8.dp),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = mc.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WbSecondaryButton(
                        text = dismissText,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    if (destructive) {
                        WbDangerButton(
                            text = confirmText,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        WbPrimaryButton(
                            text = confirmText,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** 轻量提示条。 */
@Composable
fun WbHintBar(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS || style == UiStyle.V090
    val c = wbColors()
    WbSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        cornerRadius = if (ios) 10.dp else 12.dp,
        elevation = if (ios) 8.dp else 0.dp,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        WbText(
            text = text,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = c.secondaryLabel,
        )
    }
}
