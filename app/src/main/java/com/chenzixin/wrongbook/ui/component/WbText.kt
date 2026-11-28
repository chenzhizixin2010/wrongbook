package com.chenzixin.wrongbook.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.data.prefs.UiStyle
import androidx.compose.material3.Text as M3Text
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 文本：按皮肤落到不同字阶。
 *
 * Miuix：就近映射到 `MiuixTheme.textStyles`；
 * iOS：映射到 Cupertino 的 11 级字阶（34/28/22/20/17/16/15/13/12/11）。
 *
 * 调用方仍然可以传裸字号，但最终显示值一定来自两套字阶之一。
 */
@Composable
fun WbText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
) {
    val style = wbUiStyle()
    if (style == UiStyle.MD3E) {
        val size = if (fontSize == TextUnit.Unspecified) 16f else fontSize.value
        M3Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = size.sp,
            fontWeight = fontWeight ?: when {
                size >= 28f -> FontWeight.Bold
                size >= 20f -> FontWeight.SemiBold
                size >= 15.5f -> FontWeight.Medium
                else -> FontWeight.Normal
            },
            letterSpacing = if (letterSpacing == TextUnit.Unspecified) {
                if (size >= 28f) (-0.6).sp else TextUnit.Unspecified
            } else {
                letterSpacing
            },
            lineHeight = if (lineHeight == TextUnit.Unspecified) (size * 1.28f).sp else lineHeight,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
            softWrap = softWrap,
        )
        return
    }
    if (style == UiStyle.V090) {
        val size = if (fontSize == TextUnit.Unspecified) 16f else fontSize.value
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = size.sp,
            fontWeight = fontWeight ?: when {
                size >= 24f -> FontWeight.Bold
                size >= 16.5f -> FontWeight.SemiBold
                else -> FontWeight.Normal
            },
            letterSpacing = if (letterSpacing == TextUnit.Unspecified) {
                if (size >= 26f) (-0.5).sp else TextUnit.Unspecified
            } else {
                letterSpacing
            },
            lineHeight = if (lineHeight == TextUnit.Unspecified) (size * 1.32f).sp else lineHeight,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
            softWrap = softWrap,
        )
        return
    }
    if (style == UiStyle.IOS) {
        val size = iosFontSize(fontSize)
        val weight = fontWeight ?: when {
            size >= 20f -> FontWeight.Bold
            size >= 17f -> FontWeight.Medium
            else -> FontWeight.Normal
        }
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = size.sp,
            fontWeight = weight,
            letterSpacing = if (letterSpacing == TextUnit.Unspecified) {
                if (size >= 28f) (-0.6).sp else TextUnit.Unspecified
            } else {
                letterSpacing
            },
            lineHeight = if (lineHeight == TextUnit.Unspecified) (size * 1.28f).sp else lineHeight,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
            softWrap = softWrap,
        )
        return
    }

    val styles = MiuixTheme.textStyles
    val base = when {
        fontSize == TextUnit.Unspecified -> styles.body1
        fontSize.value >= 28f -> styles.title1
        fontSize.value >= 21f -> styles.title2
        fontSize.value >= 17.5f -> styles.title4
        fontSize.value >= 17f -> styles.headline1
        fontSize.value >= 15.5f -> styles.body1
        fontSize.value >= 13.5f -> styles.body2
        fontSize.value >= 12f -> styles.footnote1
        else -> styles.footnote2
    }

    Text(
        text = text,
        modifier = modifier,
        color = if (color == Color.Unspecified) base.color else color,
        fontSize = base.fontSize,
        fontFamily = base.fontFamily,
        fontWeight = fontWeight ?: base.fontWeight,
        letterSpacing = if (letterSpacing == TextUnit.Unspecified) base.letterSpacing else letterSpacing,
        lineHeight = if (lineHeight == TextUnit.Unspecified) base.lineHeight else lineHeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
    )
}

private fun iosFontSize(size: TextUnit): Float = when {
    size == TextUnit.Unspecified -> 17f
    size.value >= 32f -> 34f
    size.value >= 26f -> 28f
    size.value >= 21f -> 22f
    size.value >= 18f -> 20f
    size.value >= 16.5f -> 17f
    size.value >= 15f -> 16f
    size.value >= 13.5f -> 15f
    size.value >= 12f -> 13f
    size.value >= 10.5f -> 12f
    else -> 11f
}

/** 图标：委托给 Miuix 的 Icon；两套皮肤共用同一套线性图标。 */
@Composable
fun WbIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    if (tint == Color.Unspecified) {
        Icon(imageVector, contentDescription, modifier)
    } else {
        Icon(imageVector, contentDescription, modifier, tint)
    }
}
