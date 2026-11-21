package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoText

/**
 * 二次确认弹窗。玻璃面板 + 大圆角，背景由系统 Dialog 自动压暗。
 *
 * 按钮统一走 [WbPrimaryButton] / [WbSecondaryButton] / [WbDangerButton]，
 * 避免出现「灰字压蓝底」这种看不清的组合。
 */
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
    val colors = wbColors()
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            modifier = Modifier.width(304.dp),
            cornerRadius = 30.dp,
            elevation = 34.dp,
            underlay = colors.dialogScrim,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CupertinoText(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.label,
                    textAlign = TextAlign.Center,
                )
                if (message != null) {
                    CupertinoText(
                        text = message,
                        modifier = Modifier.padding(top = 9.dp),
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = colors.secondaryLabel,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp),
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
                            fontSize = 15,
                        )
                    }
                }
            }
        }
    }
}

/** 轻量提示条（一次性 message，点击消失）。 */
@Composable
fun WbHintBar(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = wbColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            elevation = 4.dp,
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CupertinoText(
                    text = text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colors.secondaryLabel,
                )
            }
        }
    }
}
