package com.chenzixin.wrongbook.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.model.QuestionWithTags
import com.chenzixin.wrongbook.ui.component.GlassSurface
import com.chenzixin.wrongbook.ui.component.WbBadge
import com.chenzixin.wrongbook.ui.component.WbChip
import com.chenzixin.wrongbook.ui.component.WbGlassIconButton
import com.chenzixin.wrongbook.ui.component.WbIcons
import com.chenzixin.wrongbook.ui.component.WbPageHeader
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSectionLabel
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.ui.component.wbTabBarSpace
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.ListBullet
import io.github.robinpcrd.cupertino.icons.outlined.MagnifyingGlass
import io.github.robinpcrd.cupertino.icons.outlined.Plus
import io.github.robinpcrd.cupertino.icons.outlined.RectanglePortraitAndArrowForward
import io.github.robinpcrd.cupertino.icons.outlined.Xmark
import java.io.File

/**
 * 题库 Tab：题目列表 + 多标签筛选（AND / OR）。
 *
 * 筛选在内存里做：题目量级是「个人错题本」（几百条），比再加一套带 JOIN 的查询简单，
 * 语义与 [com.chenzixin.wrongbook.domain.tag.TagFilter] 的单测保持一致。
 */
@Composable
fun HomeTabScreen(
    onOpenQuestion: (Long) -> Unit,
    onNewQuestion: () -> Unit,
    onOpenScan: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val colors = wbColors()
    val bottomSpace = wbTabBarSpace()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomSpace),
    ) {
        item(key = "header") {
            WbPageHeader(
                title = "题库",
                kicker = "本地优先 · Errata",
                subtitle = buildString {
                    append("${state.questions.size} 道错题")
                    if (state.tags.isNotEmpty()) append(" · ${state.tags.size} 个标签")
                },
                trailing = {
                    WbGlassIconButton(
                        icon = CupertinoIcons.Outlined.Plus,
                        contentDescription = "新建题目",
                        onClick = onNewQuestion,
                        size = 46.dp,
                        iconSize = 19.dp,
                        tint = colors.tint,
                    )
                },
            )
        }

        item(key = "search") {
            WbTextField(
                value = state.keyword,
                onValueChange = vm::setKeyword,
                placeholder = "搜索题干、答案或 OCR 文本",
                modifier = Modifier.padding(horizontal = 16.dp),
                leadingIcon = CupertinoIcons.Outlined.MagnifyingGlass,
                trailing = if (state.keyword.isEmpty()) {
                    null
                } else {
                    {
                        WbGlassIconButton(
                            icon = CupertinoIcons.Outlined.Xmark,
                            contentDescription = "清空搜索",
                            onClick = { vm.setKeyword("") },
                            size = 26.dp,
                            iconSize = 12.dp,
                        )
                    }
                },
            )
        }

        if (state.tags.isNotEmpty()) {
            item(key = "filters") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WbChip(
                        label = if (state.filter.matchAll) "全部满足" else "任一满足",
                        selected = !state.filter.isEmpty,
                        onClick = { vm.setMatchAll(!state.filter.matchAll) },
                    )
                    state.tags.forEach { tag ->
                        WbChip(
                            label = tag.name,
                            selected = tag.id in state.filter.tagIds,
                            onClick = { vm.toggleTag(tag.id) },
                        )
                    }
                    if (!state.filter.isEmpty) {
                        WbChip(label = "清除", selected = false, onClick = vm::clearFilter)
                    }
                }
            }
        }

        item(key = "section") {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                WbSectionLabel(
                    text = if (state.filter.isEmpty) "全部题目" else "筛选结果",
                    trailing = "${state.questions.size} 道",
                )
            }
        }

        if (state.questions.isEmpty()) {
            item(key = "empty") {
                HomeEmptyState(
                    filtered = !state.filter.isEmpty,
                    onOpenScan = onOpenScan,
                )
            }
        } else {
            items(state.questions, key = { it.question.id }) { item ->
                QuestionCard(item = item, onClick = { onOpenQuestion(item.question.id) })
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun HomeEmptyState(filtered: Boolean, onOpenScan: () -> Unit) {
    val colors = wbColors()
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 28.dp,
        elevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(colors.accent.copy(alpha = 0.95f), colors.accentEnd.copy(alpha = 0.9f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CupertinoIcon(
                    imageVector = CupertinoIcons.Outlined.ListBullet,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White,
                )
            }
            Spacer(Modifier.height(18.dp))
            CupertinoText(
                text = if (filtered) "没有匹配的题目" else "还没有错题",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.label,
            )
            Spacer(Modifier.height(6.dp))
            CupertinoText(
                text = if (filtered) "换个标签组合，或者清空筛选再看" else "拍一整页，自动切成一道道题",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = colors.secondaryLabel,
            )
            if (!filtered) {
                Spacer(Modifier.height(20.dp))
                WbPrimaryButton(
                    text = "去扫描",
                    onClick = onOpenScan,
                    icon = WbIcons.Camera,
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(item: QuestionWithTags, onClick: () -> Unit) {
    val colors = wbColors()
    val context = LocalContext.current
    val question = item.question

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 22.dp,
        elevation = 6.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (question.thumbPath.isNotBlank()) {
                AsyncImage(
                    model = File(context.filesDir, question.thumbPath),
                    contentDescription = null,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(colors.groupedBackground),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(colors.accent.copy(alpha = 0.22f), colors.accentEnd.copy(alpha = 0.12f)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    CupertinoIcon(
                        imageVector = CupertinoIcons.Outlined.ListBullet,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = colors.tint,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                CupertinoText(
                    text = previewText(question),
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    color = colors.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (item.tags.isEmpty()) {
                        WbBadge(text = "未分类", color = colors.secondaryLabel)
                    } else {
                        item.tags.take(2).forEach { tag ->
                            WbBadge(text = tag.name, color = parseTagColor(tag.color))
                        }
                        if (item.tags.size > 2) {
                            WbBadge(text = "+${item.tags.size - 2}", color = colors.secondaryLabel)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    MasteryDots(level = question.masteryLevel)
                }
            }
        }
    }
}

/** 掌握度小圆点：5 格，已点亮的用强调色渐变。 */
@Composable
private fun MasteryDots(level: Int) {
    val colors = wbColors()
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { index ->
            val lit = index < level.coerceIn(0, 5)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (lit) {
                            Brush.linearGradient(listOf(colors.accent, colors.accentEnd))
                        } else {
                            Brush.linearGradient(
                                listOf(colors.secondaryLabel.copy(alpha = 0.25f), colors.secondaryLabel.copy(alpha = 0.25f)),
                            )
                        },
                    ),
            )
        }
    }
}

private fun previewText(question: QuestionEntity): String {
    val text = question.refinedText?.takeIf { it.isNotBlank() } ?: question.rawOcrText
    val oneLine = text.replace('\n', ' ').trim()
    return if (oneLine.isEmpty()) "（无文字）" else oneLine.take(80)
}

private fun parseTagColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF0A84FF))
