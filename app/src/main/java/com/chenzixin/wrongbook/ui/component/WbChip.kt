package com.chenzixin.wrongbook.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoText

/**
 * 可选中的胶囊标签（筛选栏、打标签、收藏夹共用）。
 *
 * 选中态不是简单换底色，而是「实心强调色渐变 + 彩色投影」，
 * 未选中态是一块很薄的玻璃片，压在极光背景上能透出底色。
 */
@Composable
fun WbChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLight: Boolean = false,
) {
    val colors = wbColors()
    val shape = RoundedCornerShape(percent = 50)
    val background = when {
        selected -> Brush.linearGradient(listOf(colors.accent, colors.accentEnd))
        onLight -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.14f)),
        )

        else -> Brush.verticalGradient(listOf(colors.glassTop, colors.glassBottom))
    }
    val contentColor = if (selected || onLight) Color.White else colors.label

    Row(
        modifier = modifier
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 9.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = colors.accent.copy(alpha = 0.45f),
                        spotColor = colors.accent.copy(alpha = 0.35f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(background)
            .then(
                if (!selected && !onLight) {
                    Modifier.border(
                        width = 1.dp,
                        color = colors.glassStrokeTop.copy(alpha = 0.55f),
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CupertinoText(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** 只读小徽标（标签、状态），不做点击。 */
@Composable
fun WbBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val colors = wbColors()
    val base = color ?: colors.tint
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(base.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CupertinoText(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = base,
            maxLines = 1,
        )
    }
}
