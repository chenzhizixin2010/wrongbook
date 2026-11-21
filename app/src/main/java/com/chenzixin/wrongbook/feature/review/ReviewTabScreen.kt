package com.chenzixin.wrongbook.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbLargeTitle
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoText
import java.io.File

/**
 * 巩固 Tab：今日待复习队列 + 答对/答错。
 *
 * 判分规则见 [com.chenzixin.wrongbook.domain.review.ReviewScheduler]：
 * 答对升一级、间隔变长；答错归零、10 分钟后重做。
 */
@Composable
fun ReviewTabScreen(vm: ReviewViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val colors = wbColors()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Box(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
            WbLargeTitle(text = "巩固")
        }

        if (!state.reviewing) {
            WbSection(
                header = "今天",
                footer = "答对升一级，间隔变长；答错 10 分钟后会再出现。",
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CupertinoText(
                        text = when {
                            state.loading -> "正在统计…"
                            state.dueCount == 0 -> "今天没有到期的错题，休息一下"
                            else -> "有 ${state.dueCount} 道题等你复习"
                        },
                        fontSize = 15.sp,
                        color = colors.label,
                    )
                    if (state.due.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        CupertinoButton(onClick = vm::start) { CupertinoText(text = "开始复习") }
                    }
                }
            }

            WbSection(header = "掌握度分布", footer = "0 = 全错，5 = 已彻底掌握。") {
                Column(modifier = Modifier.padding(16.dp)) {
                    val mastery = (0..5).map { level ->
                        level to (state.mastery.firstOrNull { it.masteryLevel == level }?.count ?: 0)
                    }
                    val maxCount = mastery.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                    mastery.forEach { (level, count) ->
                        StatBar(
                            label = "${level} 级",
                            value = count,
                            max = maxCount,
                            barColor = if (level >= 4) colors.tint else colors.secondaryLabel,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    CupertinoText(
                        text = "全部 ${state.total} 道 · 待复习 ${state.dueCount} 道",
                        fontSize = 13.sp,
                        color = colors.secondaryLabel,
                    )
                }
            }

            if (state.tagCounts.isNotEmpty()) {
                WbSection(header = "标签分布", footer = "只显示题量前 8 的标签。") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val maxTag = state.tagCounts.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                        state.tagCounts.forEach { tag ->
                            StatBar(
                                label = tag.name,
                                value = tag.count,
                                max = maxTag,
                                barColor = colors.tint,
                            )
                        }
                    }
                }
            }
        } else if (state.finished) {
            WbSection(header = "完成", footer = "答错的题 10 分钟后会重新出现在队列里。") {
                Column(modifier = Modifier.padding(16.dp)) {
                    CupertinoText(text = "今天的复习做完了 🎉", fontSize = 16.sp, color = colors.label)
                    Spacer(Modifier.height(12.dp))
                    CupertinoButton(onClick = { vm.stop(); vm.refresh() }) {
                        CupertinoText(text = "回到列表")
                    }
                }
            }
        } else {
            val current = state.current
            if (current != null) {
                WbSection(header = "第 ${state.index + 1} / ${state.total} 题") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (current.imagePath.isNotBlank()) {
                            AsyncImage(
                                model = File(context.filesDir, current.imagePath),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.groupedBackground),
                                contentScale = ContentScale.Fit,
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        CupertinoText(
                            text = (current.refinedText ?: current.rawOcrText).take(200),
                            fontSize = 15.sp,
                            color = colors.label,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CupertinoButton(onClick = { vm.answer(false) }, modifier = Modifier.weight(1f)) {
                                CupertinoText(text = "答错了", color = colors.danger)
                            }
                            CupertinoButton(onClick = { vm.answer(true) }, modifier = Modifier.weight(1f)) {
                                CupertinoText(text = "答对了", color = colors.tint)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        CupertinoButton(onClick = vm::stop) { CupertinoText(text = "退出复习") }
                    }
                }
            }
        }
    }

    if (state.message != null) {
        WbAlertDialog(
            visible = true,
            title = "提示",
            message = state.message,
            confirmText = "知道了",
            onConfirm = vm::clearMessage,
            onDismiss = vm::clearMessage,
            )
    }
}

/** 简易条形图：不引图表库，宽度按比例撑满即可读。 */
@Composable
private fun StatBar(label: String, value: Int, max: Int, barColor: androidx.compose.ui.graphics.Color) {
    val colors = wbColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(70.dp)) {
            CupertinoText(
                text = label,
                fontSize = 13.sp,
                color = colors.secondaryLabel,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.groupedBackground),
        ) {
            val fraction = (value.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f)
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor),
                )
            }
        }
        Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.CenterEnd) {
            CupertinoText(text = "$value", fontSize = 13.sp, color = colors.label)
        }
    }
}
