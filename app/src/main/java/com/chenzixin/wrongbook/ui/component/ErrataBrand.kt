package com.chenzixin.wrongbook.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.R
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Errata 品牌标记：primaryContainer 圆角块 + primary 三横杠 E。
 * 纯色，无渐变、无玻璃。
 */
@Composable
fun ErrataMark(
    size: Dp = 112.dp,
    modifier: Modifier = Modifier,
    onBrand: Boolean = false,
) {
    val c = MiuixTheme.colorScheme
    val wb = wbColors()
    val style = wbUiStyle()
    val md3e = style == UiStyle.MD3E
    val ios = style == UiStyle.IOS || style == UiStyle.MD3E
    val container = when {
        onBrand -> c.onPrimary.copy(alpha = 0.14f)
        ios -> wb.tintSoft
        else -> c.primaryContainer
    }
    val barColor = when {
        onBrand -> c.onPrimary
        ios -> wb.accent
        else -> c.primary
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(if (md3e) 14.dp else 28.dp))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.26f)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = h * 0.14f

            fun bar(y: Float, endX: Float) {
                drawLine(
                    color = barColor,
                    start = Offset(w * 0.04f, h * y),
                    end = Offset(w * endX, h * y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            bar(0.14f, 0.96f)
            bar(0.50f, 0.64f)
            bar(0.86f, 0.96f)
        }
    }
}

/**
 * 开屏：纯色背景 + 品牌标记 + 文字。
 *
 * 只是盖在已渲染主界面之上，由 MainActivity 在 1.2s 后淡出；不做初始化。
 */
@Composable
fun ErrataSplash(modifier: Modifier = Modifier) {
    val c = MiuixTheme.colorScheme
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }


    val markScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f),
        label = "markScale",
    )
    val wordAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 160),
        label = "wordAlpha",
    )
    val wordOffset by animateDpAsState(
        targetValue = if (started) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 600, delayMillis = 160),
        label = "wordOffset",
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 280),
        label = "subtitleAlpha",
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 380),
        label = "taglineAlpha",
    )
    val footerAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 480),
        label = "footerAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.background)
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ErrataMark(
                size = 112.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = markScale
                    scaleY = markScale
                },
            )

            Spacer(Modifier.height(28.dp))

            WbText(
                text = "Errata",
                modifier = Modifier.graphicsLayer {
                    alpha = wordAlpha
                    translationY = wordOffset.toPx()
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = c.onBackground,
            )

            Spacer(Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha },
            ) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(1.dp)
                        .background(c.outline),
                )
                Spacer(Modifier.width(10.dp))
                WbText(
                    text = stringResource(R.string.app_subtitle).uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                    color = c.onSurfaceVariantSummary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(1.dp)
                        .background(c.outline),
                )
            }

            Spacer(Modifier.height(18.dp))

            WbText(
                text = stringResource(R.string.app_tagline),
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha },
                fontSize = 14.sp,
                color = c.onSurfaceVariantSummary,
            )
        }

        WbText(
            text = stringResource(R.string.app_footer),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp)
                .graphicsLayer { alpha = footerAlpha },
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
            color = c.onSurfaceVariantSummary,
        )
    }
}
