package com.chenzixin.wrongbook.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.chenzixin.wrongbook.domain.ai.RefinedQuestion
import com.chenzixin.wrongbook.ui.component.GlassSurface
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbBadge
import com.chenzixin.wrongbook.ui.component.WbGlassIconButton
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSecondaryButton
import com.chenzixin.wrongbook.ui.component.WbInputDialog
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbTagEditorDialog
import com.chenzixin.wrongbook.ui.component.WbTextButton
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.CheckmarkCircle
import io.github.robinpcrd.cupertino.icons.outlined.ListBullet
import io.github.robinpcrd.cupertino.icons.outlined.Paperplane
import io.github.robinpcrd.cupertino.icons.outlined.SquareAndPencil
import io.github.robinpcrd.cupertino.icons.outlined.Star
import io.github.robinpcrd.cupertino.icons.outlined.Trash
import io.github.robinpcrd.cupertino.icons.outlined.WrenchAndScrewdriver
import java.io.File

/** 题目详情：原图、OCR 文本、标签、掌握度，以及三个 AI 动作。 */
@Composable
fun QuestionDetailScreen(
    onBack: () -> Unit,
    onOpenChat: (Long) -> Unit,
    vm: QuestionDetailViewModel = hiltViewModel(),
) {
    val item by vm.item.collectAsState()
    val deleted by vm.deleted.collectAsState()
    val message by vm.message.collectAsState()
    val aiSessionId by vm.aiSessionId.collectAsState()
    val aiState by vm.ai.collectAsState()
    val colors = wbColors()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var editingText by remember { mutableStateOf(false) }
    var editingTags by remember { mutableStateOf(false) }
    val allTags by vm.allTags.collectAsState()
    var editingValue by remember { mutableStateOf("") }
    var editingRefined by remember { mutableStateOf<RefinedQuestion?>(null) }
    var variantsToSave by remember { mutableStateOf<List<RefinedQuestion>?>(null) }

    LaunchedEffect(deleted) {
        if (deleted) onBack()
    }

    LaunchedEffect(aiSessionId) {
        aiSessionId?.let {
            vm.consumeAiSession()
            onOpenChat(it)
        }
    }

    LaunchedEffect(aiState.refined) {
        aiState.refined?.let { editingRefined = it }
    }

    LaunchedEffect(aiState.variants) {
        if (aiState.variants.isNotEmpty()) variantsToSave = aiState.variants
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(
            title = "题目详情",
            onBack = onBack,
            actions = {
                if (item != null) {
                    WbGlassIconButton(
                        icon = CupertinoIcons.Outlined.Trash,
                        contentDescription = "删除题目",
                        onClick = { confirmDelete = true },
                        size = 40.dp,
                        iconSize = 16.dp,
                        tint = colors.danger,
                        containerColor = colors.danger.copy(alpha = 0.12f),
                    )
                }
            },
        )

        val question = item?.question ?: return@Column
        val tags = item?.tags.orEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            if (question.imagePath.isNotBlank()) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cornerRadius = 26.dp,
                    elevation = 8.dp,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(5.dp),
                ) {
                    AsyncImage(
                        model = File(context.filesDir, question.imagePath),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(21.dp))
                            .background(colors.groupedBackground),
                        contentScale = ContentScale.FillWidth,
                    )
                }
            }

            WbSection(
                header = if (question.refinedText != null) "题干 · AI 整理" else "题目原文",
                icon = CupertinoIcons.Outlined.ListBullet,
                footer = "识别不准可以手动改，改完按保存。",
                action = {
                    WbTextButton(
                        text = "编辑",
                        onClick = {
                            editingValue = question.refinedText ?: question.rawOcrText
                            editingText = true
                        },
                    )
                },
            ) {
                SelectionContainer {
                    CupertinoText(
                        text = (question.refinedText ?: question.rawOcrText).ifBlank { "（没有识别到文字）" },
                        modifier = Modifier.padding(16.dp),
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        color = colors.label,
                    )
                }
            }

            if (!question.answer.isNullOrBlank() || !question.analysis.isNullOrBlank()) {
                WbSection(header = "答案与解析", icon = CupertinoIcons.Outlined.CheckmarkCircle) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (!question.answer.isNullOrBlank()) {
                            CupertinoText(
                                text = "答案",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                color = colors.tint,
                            )
                            Spacer(Modifier.height(4.dp))
                            CupertinoText(
                                text = question.answer.orEmpty(),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.label,
                            )
                        }
                        if (!question.answer.isNullOrBlank() && !question.analysis.isNullOrBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.7.dp)
                                    .background(colors.separator),
                            )
                            Spacer(Modifier.height(14.dp))
                        }
                        if (!question.analysis.isNullOrBlank()) {
                            CupertinoText(
                                text = "解析",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                color = colors.secondaryLabel,
                            )
                            Spacer(Modifier.height(4.dp))
                            CupertinoText(
                                text = question.analysis.orEmpty(),
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = colors.label.copy(alpha = 0.86f),
                            )
                        }
                    }
                }
            }

            WbSection(
                header = "标签",
                icon = CupertinoIcons.Outlined.Star,
                footer = if (tags.isEmpty()) "还没打标签，点「编辑」可以随时补。" else null,
                action = {
                    WbTextButton(text = "编辑", onClick = { editingTags = true })
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (tags.isEmpty()) {
                        CupertinoText(
                            text = "未分类",
                            fontSize = 14.sp,
                            color = colors.secondaryLabel,
                        )
                    }
                    tags.forEach { tag ->
                        WbBadge(text = tag.name, color = parseTagColor(tag.color))
                    }
                }
            }

            WbSection(
                header = "掌握状态",
                icon = CupertinoIcons.Outlined.CheckmarkCircle,
                footer = "掌握度用于统计与生成练习卷的抽题权重。",
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CupertinoText(
                            text = "${question.masteryLevel}",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.tint,
                        )
                        CupertinoText(
                            text = " / 5",
                            fontSize = 15.sp,
                            color = colors.secondaryLabel,
                        )
                        Spacer(Modifier.width(14.dp))
                        MasteryBars(level = question.masteryLevel)
                    }
                    Spacer(Modifier.height(10.dp))
                    CupertinoText(
                        text = "已整理 ${question.reviewCount} 次",
                        fontSize = 12.sp,
                        color = colors.secondaryLabel,
                    )
                }
            }

            WbSection(header = "AI 工具", icon = CupertinoIcons.Outlined.Paperplane) {
                WbListRow(
                    title = "AI 讲解",
                    subtitle = "用对话讲清这道题的思路",
                    icon = CupertinoIcons.Outlined.Paperplane,
                    onClick = { vm.askAi(QuestionDetailViewModel.AskAiKind.EXPLAIN) },
                )
                WbListRow(
                    title = "AI 整理",
                    subtitle = "把 OCR 原文整理成干净题干与解析",
                    icon = CupertinoIcons.Outlined.WrenchAndScrewdriver,
                    onClick = vm::refine,
                )
                WbListRow(
                    title = "生成同类题",
                    subtitle = "换数字再练一遍，可全部入库",
                    icon = CupertinoIcons.Outlined.SquareAndPencil,
                    showDivider = false,
                    onClick = vm::generateVariants,
                )
            }
        }
    }

    if (aiState.loading) {
        WbAlertDialog(
            visible = true,
            title = "AI 正在处理",
            message = "整理题目需要几秒钟，请稍候",
            confirmText = "知道了",
            onConfirm = { },
            onDismiss = { },
        )
    }

    editingRefined?.let { refined ->
        RefinePreviewDialog(
            initial = refined,
            onDismiss = {
                editingRefined = null
                vm.dismissAi()
            },
            onSave = { edited ->
                editingRefined = null
                vm.saveRefined(edited)
            },
        )
    }

    variantsToSave?.let { variants ->
        VariantsPreviewDialog(
            variants = variants,
            onDismiss = {
                variantsToSave = null
                vm.dismissAi()
            },
            onSaveAll = {
                variantsToSave = null
                vm.saveVariants(variants)
            },
        )
    }

    WbTagEditorDialog(
        visible = editingTags,
        allTags = allTags,
        selectedIds = (item?.tags ?: emptyList()).map { it.id }.toSet(),
        onToggle = vm::toggleTag,
        onCreate = vm::createTag,
        onDismiss = { editingTags = false },
    )

    WbInputDialog(
        visible = editingText,
        title = "编辑题目文字",
        placeholder = "把识别错的地方改对",
        confirmText = "保存",
        initialValue = editingValue,
        multiline = true,
        onConfirm = { text ->
            editingText = false
            vm.saveText(text)
        },
        onDismiss = { editingText = false },
    )

    WbAlertDialog(
        visible = confirmDelete,
        title = "删除这道题？",
        message = "图片和记录都会被删除，无法恢复。",
        confirmText = "删除",
        destructive = true,
        onConfirm = {
            confirmDelete = false
            vm.delete()
        },
        onDismiss = { confirmDelete = false },
    )

    if (message != null) {
        WbAlertDialog(
            visible = true,
            title = "提示",
            message = message,
            confirmText = "知道了",
            onConfirm = vm::clearMessage,
            onDismiss = vm::clearMessage,
        )
    }
}

@Composable
private fun MasteryBars(level: Int) {
    val colors = wbColors()
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { index ->
            val lit = index < level.coerceIn(0, 5)
            Box(
                modifier = Modifier
                    .width(9.dp)
                    .height(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (lit) {
                            Brush.linearGradient(listOf(colors.accent, colors.accentEnd))
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    colors.secondaryLabel.copy(alpha = 0.18f),
                                    colors.secondaryLabel.copy(alpha = 0.18f),
                                ),
                            )
                        },
                    ),
            )
        }
    }
}

private fun parseTagColor(hex: String): androidx.compose.ui.graphics.Color = runCatching {
    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(androidx.compose.ui.graphics.Color(0xFF0A84FF))

@Composable
private fun RefinePreviewDialog(
    initial: RefinedQuestion,
    onDismiss: () -> Unit,
    onSave: (RefinedQuestion) -> Unit,
) {
    var stem by remember { mutableStateOf(initial.stem) }
    var answer by remember { mutableStateOf(initial.answer) }
    var analysis by remember { mutableStateOf(initial.analysis) }
    val colors = wbColors()

    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevation = 34.dp,
            underlay = colors.dialogScrim,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                DialogTitle("确认整理结果")
                Spacer(Modifier.height(14.dp))
                WbTextField(value = stem, onValueChange = { stem = it }, placeholder = "题干", singleLine = false)
                Spacer(Modifier.height(9.dp))
                WbTextField(value = answer, onValueChange = { answer = it }, placeholder = "答案", singleLine = false)
                Spacer(Modifier.height(9.dp))
                WbTextField(value = analysis, onValueChange = { analysis = it }, placeholder = "解析", singleLine = false)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WbSecondaryButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    WbPrimaryButton(
                        text = "保存",
                        onClick = {
                            onSave(
                                initial.copy(
                                    stem = stem.trim(),
                                    answer = answer.trim(),
                                    analysis = analysis.trim(),
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        fontSize = 15,
                    )
                }
            }
        }
    }
}

@Composable
private fun VariantsPreviewDialog(
    variants: List<RefinedQuestion>,
    onDismiss: () -> Unit,
    onSaveAll: () -> Unit,
) {
    val colors = wbColors()
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevation = 34.dp,
            underlay = colors.dialogScrim,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                DialogTitle("生成了 ${variants.size} 道同类题")
                Spacer(Modifier.height(10.dp))
                variants.forEachIndexed { index, variant ->
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        WbBadge(text = "${index + 1}", color = colors.tint)
                        Spacer(Modifier.width(8.dp))
                        CupertinoText(
                            text = variant.stem.take(90),
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            color = colors.label,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WbSecondaryButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    WbPrimaryButton(
                        text = "全部存入题库",
                        onClick = onSaveAll,
                        modifier = Modifier.weight(1f),
                        fontSize = 14,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogTitle(text: String) {
    CupertinoText(
        text = text,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color = wbColors().label,
    )
}
