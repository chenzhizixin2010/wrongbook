package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
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
