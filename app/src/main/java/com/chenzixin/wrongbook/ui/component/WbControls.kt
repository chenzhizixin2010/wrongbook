package com.chenzixin.wrongbook.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import io.github.robinpcrd.cupertino.CupertinoSwitch
import top.yukonga.miuix.kmp.preference.SwitchPreference

/** 开关行：iOS 用 CupertinoSwitch，Miuix 用 SwitchPreference。 */
@Composable
fun WbSwitchRow(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (wbUiStyle() == UiStyle.MD3E) {
        val c = wbColors()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                WbText(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = c.label,
                )
                if (summary != null) {
                    Spacer(Modifier.height(2.dp))
                    WbText(
                        text = summary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        color = c.secondaryLabel,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Md3Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        return
    }
    if (wbUiStyle() == UiStyle.IOS || wbUiStyle() == UiStyle.V090) {
        val c = wbColors()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                WbText(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = c.label,
                )
                if (summary != null) {
                    Spacer(Modifier.height(2.dp))
                    WbText(
                        text = summary,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        color = c.secondaryLabel,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            CupertinoSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
        return
    }
    SwitchPreference(
        title = title,
        summary = summary,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

@androidx.compose.runtime.Composable
private fun Md3Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = wbColors()
    val track by animateColorAsState(
        targetValue = if (checked) c.accent else c.groupedBackground,
        label = "md3Track",
    )
    val thumbX by animateDpAsState(
        targetValue = if (checked) 24.dp else 3.dp,
        label = "md3Thumb",
    )
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .clickable { onCheckedChange(!checked) },
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .offset(x = thumbX, y = 4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) c.onAccent else c.secondaryLabel),
        )
    }
}
