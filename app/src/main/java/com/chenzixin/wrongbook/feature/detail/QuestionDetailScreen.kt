package com.chenzixin.wrongbook.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.chenzixin.wrongbook.domain.ai.RefinedQuestion
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoText
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
                    CupertinoButton(onClick = { confirmDelete = true }) {
                        CupertinoText(text = "删除", color = colors.danger)
                    }
                }
            },
        )

        val question = item?.question ?: return@Column
        val tags = item?.tags.orEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            if (question.imagePath.isNotBlank()) {
                AsyncImage(
                    model = File(context.filesDir, question.imagePath),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.groupedBackground),
                    contentScale = ContentScale.FillWidth,
                )
            }

            WbSection(header = if (question.refinedText != null) "题干（AI 整理）" else "题目原文") {
                Column(modifier = Modifier.padding(16.dp)) {
                    CupertinoText(
                        text = (question.refinedText ?: question.rawOcrText).ifBlank { "（没有识别到文字）" },
                        fontSize = 15.sp,
                        color = colors.label,
                    )
                }
            }

            if (!question.answer.isNullOrBlank() || !question.analysis.isNullOrBlank()) {
                WbSection(header = "答案与解析") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (!question.answer.isNullOrBlank()) {
                            CupertinoText(
                                text = "答案：${question.answer}",
                                fontSize = 15.sp,
                                color = colors.label,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        if (!question.analysis.isNullOrBlank()) {
                            CupertinoText(
                                text = question.analysis.orEmpty(),
                                fontSize = 14.sp,
                                color = colors.secondaryLabel,
                            )
                        }
                    }
                }
            }

            if (tags.isNotEmpty()) {
                WbSection(header = "标签") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        tags.forEach { tag ->
                            CupertinoText(
                                text = tag.name,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.groupedBackground)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 13.sp,
                                color = colors.tint,
                            )
                        }
                    }
                }
            }

            WbSection(header = "学习状态", footer = "掌握度越高，复习间隔越长。") {
                Column(modifier = Modifier.padding(16.dp)) {
                    CupertinoText(
                        text = "掌握度 ${question.masteryLevel}/5 · 已复习 ${question.reviewCount} 次",
                        fontSize = 15.sp,
                        color = colors.label,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CupertinoButton(
                    onClick = { vm.askAi(QuestionDetailViewModel.AskAiKind.EXPLAIN) },
                    modifier = Modifier.weight(1f),
                ) {
                    CupertinoText(text = "AI 讲解")
                }
                CupertinoButton(
                    onClick = vm::refine,
                    modifier = Modifier.weight(1f),
                ) {
                    CupertinoText(text = "AI 整理")
                }
                CupertinoButton(
                    onClick = vm::generateVariants,
                    modifier = Modifier.weight(1f),
                ) {
                    CupertinoText(text = "同类题")
                }
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
private fun RefinePreviewDialog(
    initial: RefinedQuestion,
    onDismiss: () -> Unit,
    onSave: (RefinedQuestion) -> Unit,
) {
    val colors = wbColors()
    var stem by remember { mutableStateOf(initial.stem) }
    var answer by remember { mutableStateOf(initial.answer) }
    var analysis by remember { mutableStateOf(initial.analysis) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardBackground)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            CupertinoText(text = "确认整理结果", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            WbTextField(value = stem, onValueChange = { stem = it }, placeholder = "题干", singleLine = false)
            Spacer(Modifier.height(8.dp))
            WbTextField(value = answer, onValueChange = { answer = it }, placeholder = "答案", singleLine = false)
            Spacer(Modifier.height(8.dp))
            WbTextField(value = analysis, onValueChange = { analysis = it }, placeholder = "解析", singleLine = false)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CupertinoButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    CupertinoText(text = "取消")
                }
                CupertinoButton(
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
                ) {
                    CupertinoText(text = "保存", fontWeight = FontWeight.SemiBold)
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardBackground)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            CupertinoText(text = "生成了 ${variants.size} 道同类题", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            variants.forEachIndexed { index, variant ->
                CupertinoText(
                    text = "${index + 1}. ${variant.stem.take(80)}",
                    modifier = Modifier.padding(vertical = 6.dp),
                    fontSize = 14.sp,
                    color = colors.label,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CupertinoButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    CupertinoText(text = "取消")
                }
                CupertinoButton(onClick = onSaveAll, modifier = Modifier.weight(1f)) {
                    CupertinoText(text = "全部存入题库", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
