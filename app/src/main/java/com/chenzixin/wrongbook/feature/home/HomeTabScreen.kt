package com.chenzixin.wrongbook.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbBadge
import com.chenzixin.wrongbook.ui.component.WbChip
import com.chenzixin.wrongbook.ui.component.WbDangerButton
import com.chenzixin.wrongbook.ui.component.WbIconButton
import com.chenzixin.wrongbook.ui.component.WbIcon
import com.chenzixin.wrongbook.ui.component.WbPageHeader
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSecondaryButton
import com.chenzixin.wrongbook.ui.component.WbDivider
import com.chenzixin.wrongbook.ui.component.WbSectionLabel
import com.chenzixin.wrongbook.ui.component.WbSurface
import com.chenzixin.wrongbook.ui.component.WbText
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import com.chenzixin.wrongbook.ui.component.WbTextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

/**
 * 题库 Tab：标准 HyperOS 内嵌列表。
 *
 * 题目不再一条一个玻璃卡；每 8 条放进一张 Card 分组，行与行之间用分隔线。
 */
@Composable
fun HomeTabScreen(
    onOpenQuestion: (Long) -> Unit,
    onNewQuestion: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenGuide: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val c = wbColors()
    var confirmDelete by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = if (state.selectionMode) 88.dp else 16.dp,
            ),
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
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            WbIconButton(
                                icon = MiuixIcons.SelectAll,
                                contentDescription = if (state.selectionMode) "退出选择" else "选择题目",
                                onClick = {
                                    if (state.selectionMode) vm.exitSelectionMode() else vm.enterSelectionMode()
                                },
                                size = 40.dp,
                                iconSize = 22.dp,
                                tint = c.accent,
                                containerColor = if (state.selectionMode) c.tintSoft else null,
                            )
                            WbIconButton(
                                icon = MiuixIcons.Add,
                                contentDescription = "新建题目",
                                onClick = onNewQuestion,
                                size = 40.dp,
                                iconSize = 22.dp,
                                tint = c.accent,
                            )
                        }
                    },
                )
            }

            item(key = "search") {
                WbTextField(
                    value = state.keyword,
                    onValueChange = vm::setKeyword,
                    placeholder = "搜索题干、答案或 OCR 文本",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    leadingIcon = MiuixIcons.Search,
                    trailing = if (state.keyword.isEmpty()) {
                        null
                    } else {
                        {
                            WbIconButton(
                                icon = MiuixIcons.Close,
                                contentDescription = "清空搜索",
                                onClick = { vm.setKeyword("") },
                                size = 24.dp,
                                iconSize = 16.dp,
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
                            .padding(start = 16.dp, end = 16.dp, top = 10.dp),
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
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
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
                        onOpenGuide = onOpenGuide,
                    )
                }
            } else {
                items(
                    items = state.questions.chunked(8),
                    key = { chunk -> chunk.first().question.id },
                ) { chunk ->
                    WbSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        cornerRadius = if (wbUiStyle() == UiStyle.V090) 24.dp else 16.dp,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Column {
                        chunk.forEachIndexed { index, item ->
                            QuestionRow(
                                item = item,
                                selectable = state.selectionMode,
                                selected = item.question.id in state.selectedIds,
                                onClick = {
                                    if (state.selectionMode) {
                                        vm.toggleSelect(item.question.id)
                                    } else {
                                        onOpenQuestion(item.question.id)
                                    }
                                },
                            )
                            if (index != chunk.lastIndex) {
                                WbDivider(startIndent = 72.dp)
                            }
                        }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        if (state.selectionMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(c.cardBackground),
            ) {
                WbDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WbSecondaryButton(
                        text = if (state.selectedIds.size == state.questions.size && state.questions.isNotEmpty()) "取消全选" else "全选",
                        onClick = {
                            if (state.selectedIds.size == state.questions.size) vm.clearSelection() else vm.selectAll()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    WbDangerButton(
                        text = "删除（${state.selectedIds.size}）",
                        onClick = { if (state.selectedIds.isNotEmpty()) confirmDelete = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    WbAlertDialog(
        visible = confirmDelete,
        title = "删除选中的 ${state.selectedIds.size} 道题？",
        message = "题目和图片都会被删除，无法恢复。",
        confirmText = "删除",
        destructive = true,
        onConfirm = {
            confirmDelete = false
            vm.deleteSelected()
        },
        onDismiss = { confirmDelete = false },
    )

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

/** 空状态：页面居中，极简线框图标 + 一句话 + 一个按钮；无卡片、无渐变。 */
@Composable
private fun HomeEmptyState(
    filtered: Boolean,
    onOpenScan: () -> Unit,
    onOpenGuide: () -> Unit,
) {
    val c = wbColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = if (wbUiStyle() == UiStyle.MD3E) 20.dp else 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (wbUiStyle() == UiStyle.MD3E) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(c.tintSoft),
                contentAlignment = Alignment.Center,
            ) {
                WbIcon(
                    imageVector = MiuixIcons.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = c.accent,
                )
            }
        } else {
            WbIcon(
                imageVector = MiuixIcons.Notes,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = c.secondaryLabel,
            )
        }
        Spacer(Modifier.height(16.dp))
        WbText(
            text = if (filtered) "没有匹配的题目" else "还没有错题",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = c.label,
        )
        Spacer(Modifier.height(6.dp))
        WbText(
            text = if (filtered) "换个标签组合，或者清空筛选" else "拍一整页，自动切成一道道题",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = c.secondaryLabel,
        )
        if (!filtered) {
            Spacer(Modifier.height(20.dp))
            WbPrimaryButton(
                text = "去扫描",
                onClick = onOpenScan,
                icon = MiuixIcons.Scan,
            )
            Spacer(Modifier.height(2.dp))
            WbTextButton(
                text = "查看使用手册",
                onClick = onOpenGuide,
            )
        }
    }
}

@Composable
private fun QuestionRow(
    item: QuestionWithTags,
    onClick: () -> Unit,
    selectable: Boolean = false,
    selected: Boolean = false,
) {
    val c = wbColors()
    val context = LocalContext.current
    val question = item.question

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectable) {
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) c.accent else Color.Transparent)
                    .border(1.5.dp, if (selected) c.accent else c.separator, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    WbIcon(
                        imageVector = MiuixIcons.Ok,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = c.onAccent,
                    )
                }
            }
        }

        if (question.thumbPath.isNotBlank()) {
            AsyncImage(
                model = File(context.filesDir, question.thumbPath),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.groupedBackground),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.groupedBackground),
                contentAlignment = Alignment.Center,
            ) {
                WbIcon(
                    imageVector = MiuixIcons.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = c.secondaryLabel,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            WbText(
                text = previewText(question),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = c.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (item.tags.isEmpty()) {
                    WbBadge(text = "未分类", color = c.secondaryLabel)
                } else {
                    item.tags.take(2).forEach { tag ->
                        WbBadge(text = tag.name, color = parseTagColor(tag.color))
                    }
                    if (item.tags.size > 2) {
                        WbBadge(text = "+${item.tags.size - 2}", color = c.secondaryLabel)
                    }
                }
                Spacer(Modifier.weight(1f))
                MasteryDots(level = question.masteryLevel)
            }
        }
    }
}

/** 掌握度小圆点：纯色 primary / outline。 */
@Composable
private fun MasteryDots(level: Int) {
    val c = wbColors()
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { index ->
            val lit = index < level.coerceIn(0, 5)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(if (lit) c.accent else c.separator.copy(alpha = 0.45f)),
            )
        }
    }
}

private fun previewText(question: QuestionEntity): String {
    val text = question.refinedText?.takeIf { it.isNotBlank() } ?: question.rawOcrText
    val oneLine = text.replace('\n', ' ').trim()
    return if (oneLine.isEmpty()) "（无文字）" else oneLine.take(80)
}

private fun parseTagColor(hex: String): Color? = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrNull()
