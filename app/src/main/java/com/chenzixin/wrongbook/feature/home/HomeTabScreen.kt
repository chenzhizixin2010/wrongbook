package com.chenzixin.wrongbook.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.chenzixin.wrongbook.data.local.entity.QuestionEntity
import com.chenzixin.wrongbook.data.local.model.QuestionWithTags
import com.chenzixin.wrongbook.ui.component.WbChip
import com.chenzixin.wrongbook.ui.component.WbLargeTitle
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoText
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
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val colors = wbColors()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) { WbLargeTitle(text = "题库") }
            CupertinoButton(onClick = onNewQuestion) { CupertinoText(text = "新建") }
        }

        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            com.chenzixin.wrongbook.ui.component.WbTextField(
                value = state.keyword,
                onValueChange = vm::setKeyword,
                placeholder = "搜索题干或 OCR 文本",
            )
        }

        if (state.tags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
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
            Spacer(Modifier.height(8.dp))
        }

        if (state.questions.isEmpty()) {
            WbSection(
                header = if (state.filter.isEmpty) "全部题目" else "筛选结果",
                footer = if (state.filter.isEmpty) {
                    "拍照扫描后，选中的题目会自动出现在这里。"
                } else {
                    "没有同时满足这些标签的题目，换个组合试试。"
                },
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    CupertinoText(
                        text = if (state.filter.isEmpty) "还没有题目，去「扫描」拍一张吧" else "没有匹配的题目",
                        fontSize = 15.sp,
                        color = colors.secondaryLabel,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            ) {
                items(state.questions, key = { it.question.id }) { item ->
                    QuestionCard(item = item, onClick = { onOpenQuestion(item.question.id) })
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(item: QuestionWithTags, onClick: () -> Unit) {
    val colors = wbColors()
    val context = LocalContext.current
    val question = item.question
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardBackground)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (question.thumbPath.isNotBlank()) {
            AsyncImage(
                model = File(context.filesDir, question.thumbPath),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.groupedBackground),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.groupedBackground),
                contentAlignment = Alignment.Center,
            ) {
                CupertinoText(text = "文字", fontSize = 11.sp, color = colors.secondaryLabel)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            CupertinoText(text = previewText(question), fontSize = 15.sp, color = colors.label)
            val tagLine = item.tags.joinToString(" · ") { it.name }
            CupertinoText(
                text = if (tagLine.isEmpty()) "未分类" else tagLine,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 12.sp,
                color = colors.secondaryLabel,
            )
        }
    }
}

private fun previewText(question: QuestionEntity): String {
    val text = question.refinedText?.takeIf { it.isNotBlank() } ?: question.rawOcrText
    val oneLine = text.replace('\n', ' ').trim()
    return if (oneLine.isEmpty()) "（无文字）" else oneLine.take(60)
}
