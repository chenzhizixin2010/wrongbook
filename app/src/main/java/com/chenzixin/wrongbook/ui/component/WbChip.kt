package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoText

/** 可选中的圆角标签片（筛选栏、打标签、收藏夹共用）。 */
@Composable
fun WbChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLight: Boolean = false,
) {
    val colors = wbColors()
    val bg = when {
        selected -> colors.tint
        onLight -> Color(0x33000000)
        else -> colors.groupedBackground
    }
    val fg = when {
        selected -> Color.White
        onLight -> Color.White
        else -> colors.label
    }
    CupertinoText(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        fontSize = 13.sp,
        color = fg,
    )
}
