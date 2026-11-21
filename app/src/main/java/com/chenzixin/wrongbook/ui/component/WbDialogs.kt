package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoText

/** 二次确认弹窗（删除等危险操作用）。 */
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
        Column(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardBackground)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CupertinoText(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.label,
            )
            if (message != null) {
                CupertinoText(
                    text = message,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 14.sp,
                    color = colors.secondaryLabel,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CupertinoButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    CupertinoText(text = dismissText, color = colors.secondaryLabel)
                }
                CupertinoButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                ) {
                    CupertinoText(
                        text = confirmText,
                        color = if (destructive) colors.danger else colors.tint,
                        fontWeight = FontWeight.SemiBold,
                    )
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.cardBackground)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        CupertinoText(text = text, fontSize = 14.sp, color = colors.secondaryLabel)
    }
}
