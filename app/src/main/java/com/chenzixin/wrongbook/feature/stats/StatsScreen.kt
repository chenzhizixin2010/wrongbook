package com.chenzixin.wrongbook.feature.stats

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.ui.component.WbSurface
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbTextButton
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.component.WbText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Favorites

/** 学习统计：掌握度分布 + 标签分布。 */
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    vm: StatsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val colors = wbColors()

    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(
            title = "学习统计",
            onBack = onBack,
            actions = { WbTextButton(text = "刷新", onClick = vm::refresh) },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            OverviewCard(
                total = state.total,
                mastery = state.mastery,
                loading = state.loading,
            )

            WbSection(
                header = "掌握度分布",
                icon = MiuixIcons.Refresh,
                footer = "0 = 还没掌握，5 = 已彻底掌握。数值来自每次 AI 整理与手动调整。",
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val mastery = (0..5).map { level ->
                        level to (state.mastery.firstOrNull { it.masteryLevel == level }?.count ?: 0)
                    }
                    val maxCount = mastery.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                    mastery.forEach { (level, count) ->
                        StatBar(
                            label = "$level 级",
                            value = count,
                            max = maxCount,
                            bar = if (level >= 4) colors.tint else colors.tint.copy(alpha = 0.55f),
                        )
                    }
                }
            }

            WbSection(
                header = "标签分布",
                icon = MiuixIcons.Favorites,
                footer = "只显示题量前 10 的标签。",
            ) {
                if (state.tagCounts.isEmpty()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        WbText(
                            text = "还没有标签数据，去「设置 → 标签与收藏夹」建几个",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = colors.secondaryLabel,
                        )
                    }
                } else {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val maxTag = state.tagCounts.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                        state.tagCounts.forEachIndexed { index, tag ->
                            StatBar(
                                label = tag.name,
                                value = tag.count,
                                max = maxTag,
                                bar = if (index == 0) colors.tint else colors.tint.copy(alpha = 0.45f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 总览：环形进度 + 三档数量。 */
@Composable
private fun OverviewCard(
    total: Int,
    mastery: List<com.chenzixin.wrongbook.data.local.model.MasteryCount>,
    loading: Boolean,
) {
    val colors = wbColors()
    fun countOf(levels: IntRange): Int =
        mastery.filter { it.masteryLevel in levels }.sumOf { it.count }

    val mastered = countOf(4..5)
    val reviewing = countOf(3..3)
    val unmastered = countOf(0..2)
    val progress = if (total == 0) 0f else mastered.toFloat() / total

    WbSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 28.dp,
        elevation = 14.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 11.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth,
                        size.height - strokeWidth,
                    )
                    drawArc(
                        color = colors.secondaryLabel.copy(alpha = 0.14f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    if (progress > 0f) {
                        drawArc(
                            color = colors.tint,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WbText(
                        text = if (loading) "…" else "$mastered",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.label,
                    )
                    WbText(
                        text = "已掌握",
                        fontSize = 10.sp,
                        color = colors.secondaryLabel,
                    )
                }
            }

            Spacer(Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                WbText(
                    text = if (loading) "正在统计…" else "共 $total 道错题",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.label,
                )
                Spacer(Modifier.height(4.dp))
                WbText(
                    text = "掌握度越高，说明这类题越稳",
                    fontSize = 11.5.sp,
                    color = colors.secondaryLabel,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatPill(label = "未掌握", value = unmastered, color = colors.danger, modifier = Modifier.weight(1f))
                    StatPill(label = "在巩固", value = reviewing, color = colors.warning, modifier = Modifier.weight(1f))
                    StatPill(label = "已掌握", value = mastered, color = colors.success, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = wbColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WbText(
            text = "$value",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Spacer(Modifier.height(1.dp))
        WbText(
            text = label,
            fontSize = 9.sp,
            color = colors.secondaryLabel,
            maxLines = 1,
        )
    }
}

/** 简易条形图：不引图表库，圆角 + 渐变已经足够读。 */
@Composable
private fun StatBar(
    label: String,
    value: Int,
    max: Int,
    bar: Color,
) {
    val colors = wbColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(70.dp)) {
            WbText(
                text = label,
                fontSize = 12.sp,
                color = colors.secondaryLabel,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(9.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(colors.secondaryLabel.copy(alpha = 0.10f)),
        ) {
            val fraction = (value.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f)
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(9.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(bar),
                )
            }
        }
        Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.CenterEnd) {
            WbText(text = "$value", fontSize = 12.sp, color = colors.label)
        }
    }
}
