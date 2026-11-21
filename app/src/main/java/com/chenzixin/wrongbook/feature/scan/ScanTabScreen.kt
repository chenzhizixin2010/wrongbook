package com.chenzixin.wrongbook.feature.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoText

/**
 * 扫描 Tab。
 *
 * P0：入口占位（真正的拍照页是 P3 的全屏路由，不放 Tab 里）。
 * P3：CameraX 预览 + 快门 → OCR → 题目切分 → 多选框选 → 入库。
 *
 * 注意：相机权限在 P3 进入拍照页时才申请，不要放在 App 启动时。
 */
@Composable
fun ScanTabScreen(onStartCapture: () -> Unit) {
    val colors = wbColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CupertinoText(
            text = "扫描",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(top = 8.dp),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = colors.label,
        )

        Column(
            modifier = Modifier.padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CupertinoText(
                text = "把错题拍清楚，一次可以拍一整页",
                fontSize = 15.sp,
                color = colors.secondaryLabel,
            )
            CupertinoButton(onClick = onStartCapture) {
                CupertinoText(text = "拍照扫描")
            }
        }

        WbSection(
            header = "说明",
            footer = "识别完全在本机完成，图片不会上传；只有你主动点「AI 整理」时才会发送文本。",
        ) {
            WbListRow(title = "本地 OCR", subtitle = "ML Kit 中文模型随安装包内置，断网可用", showDivider = false)
        }
    }
}
