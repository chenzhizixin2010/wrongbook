package com.chenzixin.wrongbook.feature.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.component.WbIcon
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbPageHeader
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbText
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 扫描 Tab：HyperOS 极简入口。
 *
 * 去掉取景框插画、渐变圆钮和玻璃卡；只保留「图标 + 说明 + 一个主按钮 + 两组内嵌列表」。
 * 真正的拍照页是 [CameraCaptureScreen]。
 */
@Composable
fun ScanTabScreen(onStartCapture: () -> Unit) {
    val c = wbColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp),
    ) {
        WbPageHeader(
            title = "扫描",
            kicker = "拍照识字 · 本地完成",
            subtitle = "一整页拍下来，自动切成一道道题",
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (wbUiStyle() == UiStyle.MD3E) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(c.tintSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    WbIcon(
                        imageVector = MiuixIcons.Scan,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = c.accent,
                    )
                }
            } else {
                WbIcon(
                    imageVector = MiuixIcons.Scan,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = c.accent,
                )
            }
            Spacer(Modifier.height(16.dp))
            WbText(
                text = "把整页错题拍下来",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = c.label,
            )
            Spacer(Modifier.height(6.dp))
            WbText(
                text = "自动识别题号并框出每一道题，选中的才会入库",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = c.secondaryLabel,
            )
            Spacer(Modifier.height(18.dp))
            WbPrimaryButton(
                text = "立即扫描",
                onClick = onStartCapture,
                icon = MiuixIcons.Scan,
            )
            Spacer(Modifier.height(10.dp))
            WbText(
                text = "识别在手机上完成 · 图片不上传",
                fontSize = 11.sp,
                color = c.secondaryLabel,
            )
        }

        WbSection(header = "能力") {
            WbListRow(
                title = "本地 OCR",
                subtitle = "ML Kit 中文模型内置，断网可用",
                icon = MiuixIcons.Notes,
            )
            WbListRow(
                title = "智能切题",
                subtitle = "按题号切分，透视自动矫正",
                icon = MiuixIcons.Search,
            )
            WbListRow(
                title = "框选入库",
                subtitle = "题号自动编号，可多选、可打标签",
                icon = MiuixIcons.Ok,
                showDivider = false,
            )
        }

        WbSection(header = "流程") {
            WbListRow(
                title = "1. 对准整页",
                subtitle = "尽量铺平、光线均匀，四角都在画面内",
            )
            WbListRow(
                title = "2. 点按快门",
                subtitle = "自动矫正透视并识别题目区域",
            )
            WbListRow(
                title = "3. 框选入库",
                subtitle = "选中的题才会保存到题库",
                showDivider = false,
            )
        }

        WbText(
            text = "识别完全在本机完成，图片不会上传；只有你主动点「AI 整理」时才会发送文本。",
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = c.secondaryLabel,
        )
    }
}
