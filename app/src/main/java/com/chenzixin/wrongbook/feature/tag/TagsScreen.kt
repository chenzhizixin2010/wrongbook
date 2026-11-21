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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.data.local.entity.TagEntity
import com.chenzixin.wrongbook.ui.component.WbAlertDialog
import com.chenzixin.wrongbook.ui.component.WbChip
import com.chenzixin.wrongbook.ui.component.WbTextField
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoText

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
                CupertinoButton(onClick = { creating = true }) { CupertinoText(text = "新建") }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (tags.isEmpty()) {
                CupertinoText(
                    text = "还没有标签。标签用来分类错题，「收藏夹」只是打了标记的标签。",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = colors.secondaryLabel,
                )
            }
            flattenTree(tags).forEach { (tag, depth) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.cardBackground)
                        .clickable { editing = tag }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (depth > 0) Spacer(Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(parseTagColor(tag.color)),
                    )
                    Spacer(Modifier.width(10.dp))
                    CupertinoText(
                        text = if (tag.isFolder) "📁 ${tag.name}" else tag.name,
                        fontSize = 16.sp,
                        color = colors.label,
                    )
                    Spacer(Modifier.width(8.dp))
                    if (tag.isFolder) {
                        CupertinoText(text = "收藏夹", fontSize = 11.sp, color = colors.tint)
                    }
                    Spacer(Modifier.weight(1f))
                    CupertinoText(
                        text = "排序 ${tag.sortOrder}",
                        fontSize = 11.sp,
                        color = colors.secondaryLabel,
                    )
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

private fun parseTagColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF007AFF))

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
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardBackground)
                .padding(20.dp),
        ) {
            CupertinoText(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.label)
            Spacer(Modifier.height(12.dp))

            WbTextField(value = name, onValueChange = { name = it }, placeholder = "标签名，例如 函数")
            Spacer(Modifier.height(14.dp))

            CupertinoText(text = "颜色", fontSize = 13.sp, color = colors.secondaryLabel)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TAG_PALETTE.take(5).forEach { (hex, _) ->
                    ColorDot(hex = hex, selected = color == hex, onClick = { color = hex })
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TAG_PALETTE.drop(5).forEach { (hex, _) ->
                    ColorDot(hex = hex, selected = color == hex, onClick = { color = hex })
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CupertinoText(text = "排序", fontSize = 13.sp, color = colors.secondaryLabel)
                Spacer(Modifier.width(12.dp))
                CupertinoButton(onClick = { sortOrder -= 1 }) { CupertinoText(text = "－") }
                CupertinoText(text = "$sortOrder", fontSize = 15.sp, color = colors.label)
                CupertinoButton(onClick = { sortOrder += 1 }) { CupertinoText(text = "＋") }
                Spacer(Modifier.weight(1f))
                CupertinoText(text = "越小越靠前", fontSize = 11.sp, color = colors.secondaryLabel)
            }

            Spacer(Modifier.height(10.dp))
            WbChip(
                label = if (folder) "✓ 作为收藏夹" else "作为收藏夹",
                selected = folder,
                onClick = { folder = !folder },
            )

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) {
                    CupertinoButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                        CupertinoText(text = "删除", color = colors.danger)
                    }
                }
                CupertinoButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    CupertinoText(text = "取消")
                }
                CupertinoButton(
                    onClick = { if (name.isNotBlank()) onConfirm(name.trim(), folder, color, sortOrder) },
                    modifier = Modifier.weight(1f),
                ) {
                    CupertinoText(text = "保存", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ColorDot(hex: String, selected: Boolean, onClick: () -> Unit) {
    val dotColor = parseTagColor(hex)
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(dotColor)
            .then(
                if (selected) {
                    Modifier.border(2.dp, wbColors().label, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
    )
}
