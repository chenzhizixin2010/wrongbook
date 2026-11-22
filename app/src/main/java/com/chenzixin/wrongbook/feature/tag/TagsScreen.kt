package com.chenzixin.wrongbook.feature.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.ui.component.WbSurface
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbBadge
import com.chenzixin.wrongbook.ui.component.WbIconButton
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbSecondaryButton
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.ui.component.WbTextButton
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.chenzixin.wrongbook.ui.component.WbIcon
import com.chenzixin.wrongbook.ui.component.WbText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Delete

/**
 * 标签 / 收藏夹管理：新建、改名、改色、调排序、删。
 *
 * 「收藏夹」就是 isFolder = true 的普通标签，不额外建表（见 DECISIONS D4）。
 */
@Composable
fun TagsScreen(
    onBack: () -> Unit,
    vm: TagsViewModel = hiltViewModel(),
) {
    val tags by vm.tags.collectAsState()
    val message by vm.message.collectAsState()
    val colors = wbColors()

    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TagEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<TagEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(
            title = "标签与收藏夹",
            onBack = onBack,
            actions = {
                WbTextButton(text = "新建", onClick = { creating = true })
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            if (tags.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WbIcon(
                        imageVector = MiuixIcons.Favorites,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = colors.secondaryLabel,
                    )
                    Spacer(Modifier.height(16.dp))
                    WbText(
                        text = "还没有标签",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.label,
                    )
                    Spacer(Modifier.height(6.dp))
                    WbText(
                        text = "标签用来分类错题，「收藏夹」只是打了标记的标签",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = colors.secondaryLabel,
                    )
                }
            } else {
                WbSection(
                    header = "全部标签",
                    icon = MiuixIcons.Favorites,
                    footer = "两级结构；「收藏夹」会出现在题库顶部的筛选区。",
                ) {
                    val rows = flattenTree(tags)
                    rows.forEachIndexed { index, (tag, depth) ->
                        TagRow(
                            tag = tag,
                            depth = depth,
                            showDivider = index != rows.lastIndex,
                            onClick = { editing = tag },
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        TagFormDialog(
            title = "新建标签",
            initial = null,
            onDismiss = { creating = false },
            onConfirm = { name, folder, color, _ ->
                creating = false
                vm.create(name, folder, color, parentId = null)
            },
        )
    }

    editing?.let { tag ->
        TagFormDialog(
            title = "编辑标签",
            initial = tag,
            onDismiss = { editing = null },
            onDelete = { editing = null; confirmDelete = tag },
            onConfirm = { name, folder, color, sort ->
                editing = null
                vm.update(tag.copy(name = name, isFolder = folder, color = color, sortOrder = sort))
            },
        )
    }

    confirmDelete?.let { tag ->
        WbAlertDialog(
            visible = true,
            title = "删除标签「${tag.name}」？",
            message = "只会解除分类，题目不会被删除。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                vm.delete(tag.id)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null },
        )
    }

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
private fun TagRow(
    tag: TagEntity,
    depth: Int,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    val colors = wbColors()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp + (depth * 18).dp, end = 14.dp, top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(parseTagColor(tag.color).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(parseTagColor(tag.color)),
                )
            }
            Spacer(Modifier.width(11.dp))
            WbText(
                text = tag.name,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.label,
                maxLines = 1,
            )
            if (tag.isFolder) {
                Spacer(Modifier.width(8.dp))
                WbBadge(text = "收藏夹", color = colors.warning)
            }
            Spacer(Modifier.weight(1f))
            WbText(
                text = "排序 ${tag.sortOrder}",
                fontSize = 10.sp,
                color = colors.secondaryLabel.copy(alpha = 0.75f),
            )
            Spacer(Modifier.width(7.dp))
            WbIcon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = colors.secondaryLabel.copy(alpha = 0.5f),
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (depth > 0) 70.dp else 51.dp, end = 16.dp)
                    .height(0.7.dp)
                    .background(colors.separator),
            )
        }
    }
}

/** 把标签按两级树展开成 (标签, 层级) 列表，保证父在子前。 */
private fun flattenTree(tags: List<TagEntity>): List<Pair<TagEntity, Int>> {
    val byParent = tags.groupBy { it.parentId }
    val result = mutableListOf<Pair<TagEntity, Int>>()
    fun walk(parentId: Long?, depth: Int) {
        byParent[parentId].orEmpty()
            .sortedWith(compareBy({ it.sortOrder }, { it.name }))
            .forEach { tag ->
                result += tag to depth
                if (depth < 1) walk(tag.id, depth + 1)
            }
    }
    walk(null, 0)
    tags.filter { it.parentId != null && tags.none { p -> p.id == it.parentId } }
        .forEach { result += it to 0 }
    return result
}

@Composable
private fun parseTagColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(MiuixTheme.colorScheme.primary)

@Composable
private fun TagFormDialog(
    title: String,
    initial: TagEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, folder: Boolean, color: String, sortOrder: Int) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val colors = wbColors()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var folder by remember { mutableStateOf(initial?.isFolder ?: false) }
    var color by remember { mutableStateOf(initial?.color ?: TAG_PALETTE.first().first) }
    var sortOrder by remember { mutableStateOf(initial?.sortOrder ?: 0) }

    Dialog(onDismissRequest = onDismiss) {
        WbSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevation = 34.dp,
            underlay = colors.dialogScrim,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WbText(
                        text = title,
                        modifier = Modifier.weight(1f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.label,
                    )
                    if (onDelete != null) {
                        WbIconButton(
                            icon = MiuixIcons.Delete,
                            contentDescription = "删除标签",
                            onClick = onDelete,
                            size = 34.dp,
                            iconSize = 14.dp,
                            tint = colors.danger,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                WbTextField(value = name, onValueChange = { name = it }, placeholder = "标签名，例如 函数")
                Spacer(Modifier.height(16.dp))

                WbText(
                    text = "颜色",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = colors.secondaryLabel,
                )
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    TAG_PALETTE.take(5).forEach { (hex, _) ->
                        ColorDot(hex = hex, selected = color == hex, onClick = { color = hex })
                    }
                }
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    TAG_PALETTE.drop(5).forEach { (hex, _) ->
                        ColorDot(hex = hex, selected = color == hex, onClick = { color = hex })
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WbText(
                        text = "排序",
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = colors.label,
                    )
                    StepperButton(text = "－") { sortOrder -= 1 }
                    Box(
                        modifier = Modifier.width(38.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        WbText(text = "$sortOrder", fontSize = 15.sp, color = colors.label)
                    }
                    StepperButton(text = "＋") { sortOrder += 1 }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(colors.glassFill.copy(alpha = 0.4f))
                        .clickable { folder = !folder }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WbText(
                        text = "作为收藏夹",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = colors.label,
                    )
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(26.dp)
                            .clip(CircleShape)
                            .background(if (folder) colors.tint else colors.secondaryLabel.copy(alpha = 0.22f)),
                        contentAlignment = if (folder) Alignment.CenterEnd else Alignment.CenterStart,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MiuixTheme.colorScheme.onPrimary),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WbSecondaryButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    WbPrimaryButton(
                        text = "保存",
                        onClick = { if (name.isNotBlank()) onConfirm(name.trim(), folder, color, sortOrder) },
                        modifier = Modifier.weight(1f),
                        fontSize = 15,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    val colors = wbColors()
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.tintSoft)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        WbText(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.tint,
        )
    }
}

@Composable
private fun ColorDot(hex: String, selected: Boolean, onClick: () -> Unit) {
    val colors = wbColors()
    val dotColor = parseTagColor(hex)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(dotColor)
            .then(
                if (selected) {
                    Modifier.border(2.dp, colors.label, CircleShape)
                } else {
                    Modifier.border(1.dp, colors.glassStrokeTop.copy(alpha = 0.5f), CircleShape)
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            WbText(
                text = "✓",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onPrimary,
            )
        }
    }
}
