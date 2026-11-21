package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chenzixin.wrongbook.data.update.UpdateInfo
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoText

/**
 * 大版本更新提示（只在主版本/次版本变化时弹）。
 *
 * 小版本（0.4.0 → 0.4.1）不弹这个，避免每次小修都打断用户。
 */
@Composable
fun UpdateRecommendedDialog(
    info: UpdateInfo,
    currentVersion: String,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    val colors = wbColors()
    Dialog(onDismissRequest = onLater) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardBackground)
                .padding(20.dp),
        ) {
            CupertinoText(
                text = "发现新版本 ${info.version}",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.label,
            )
            CupertinoText(
                text = "当前 $currentVersion → 建议更新到大版本",
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 12.sp,
                color = colors.tint,
            )

            if (info.notes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    CupertinoText(
                        text = info.notes.take(800),
                        fontSize = 13.sp,
                        color = colors.secondaryLabel,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                WbSecondaryButton(text = "稍后", onClick = onLater, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                WbPrimaryButton(text = "去更新", onClick = onUpdate, modifier = Modifier.weight(1f))
            }
        }
    }
}
