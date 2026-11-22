package com.chenzixin.wrongbook.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chenzixin.wrongbook.ui.component.WbDangerButton
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSecondaryButton
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.component.WbText

/**
 * 服务商引导：申请入口 + 领取步骤 + 一键填入。
 *
 * 免费 Key 必须用户自己去平台申请，App 只负责把"去哪申请、怎么填"讲清楚。
 */
@Composable
fun ProviderGuideDialog(
    preset: ProviderPreset,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = wbColors()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardBackground)
                .padding(20.dp),
        ) {
            WbText(
                text = preset.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.label,
            )
            if (preset.needsVpn) {
                WbText(
                    text = "需要网络可达（国内请自备代理）",
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 12.sp,
                    color = colors.danger,
                )
            }

            WbText(
                text = preset.quota,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 13.sp,
                color = colors.secondaryLabel,
            )

            Spacer(Modifier.height(16.dp))
            WbText(
                text = "怎么拿 Key",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.label,
            )
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                preset.steps.forEachIndexed { index, step ->
                    Row {
                        WbText(
                            text = "${index + 1}.",
                            modifier = Modifier.width(22.dp),
                            fontSize = 13.sp,
                            color = colors.tint,
                        )
                        WbText(
                            text = step,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = colors.label,
                        )
                    }
                }
            }

            if (preset.applyUrl.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.groupedBackground)
                        .clickable { openUrl(context, preset.applyUrl) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WbText(
                        text = "去申请 Key",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = colors.tint,
                        fontWeight = FontWeight.SemiBold,
                    )
                    WbText(text = "↗", fontSize = 14.sp, color = colors.tint)
                }
                WbText(
                    text = preset.applyUrl,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                    fontSize = 11.sp,
                    color = colors.secondaryLabel,
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WbSecondaryButton(
                    text = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                WbPrimaryButton(
                    text = "填入这套配置",
                    onClick = {
                        onApply()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 用系统浏览器打开；打不开就静默（不弹错误打断流程）。 */
fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
