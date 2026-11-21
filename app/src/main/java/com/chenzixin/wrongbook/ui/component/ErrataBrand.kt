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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.R
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoText

/**
 * Errata 品牌标记：玻璃底 + 蓝紫渐变的 E。
 *
 * E 由三条圆头横杠组成，和开屏字标（衬线 Errata）一起构成最小识别单元；
 * 圆角/受光边缘复用 [GlassSurface]，所以在深色模式下不会「白块」。
 */
@Composable
fun ErrataMark(
    size: Dp = 128.dp,
    modifier: Modifier = Modifier,
) {
    val colors = wbColors()
    GlassSurface(
        modifier = modifier.size(size),
        cornerRadius = size * 0.30f,
        elevation = 24.dp,
        glow = colors.accent.copy(alpha = 0.32f),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.24f),
        ) {
            val w = this.size.width
            val h = this.size.height
            val stroke = h * 0.135f
            val brush = Brush.linearGradient(
                colors = listOf(colors.accent, colors.accentEnd),
                start = Offset(0f, 0f),
                end = Offset(w, h),
            )

            fun bar(y: Float, endX: Float) {
                drawLine(
                    brush = brush,
                    start = Offset(w * 0.04f, h * y),
                    end = Offset(w * endX, h * y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            bar(0.14f, 0.96f)
            bar(0.50f, 0.66f)
            bar(0.86f, 0.96f)
        }
    }
}

/**
 * 开屏。
 *
 * 不在这里做耗时初始化：它只是盖在已经渲染好的主界面之上，
 * 由 [com.chenzixin.wrongbook.MainActivity] 在 1s 后淡出，所以不会拖慢冷启动。
 */
@Composable
fun ErrataSplash(modifier: Modifier = Modifier) {
    val colors = wbColors()
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
        targetValue = if (started) 0.dp else 18.dp,
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
            // 开屏期间吞掉点击，避免用户误触到下面已经就绪的主界面。
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        WbAmbientBackground()

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(320.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.accent.copy(alpha = if (colors.isDark) 0.30f else 0.18f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = size.minDimension / 2f,
                        ),
                    )
                },
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ErrataMark(
                size = 128.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = markScale
                    scaleY = markScale
                },
            )

            Spacer(Modifier.height(34.dp))

            BasicText(
                text = "Errata",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 48.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.8).sp,
                    color = colors.label,
                ),
                modifier = Modifier.graphicsLayer {
                    alpha = wordAlpha
                    translationY = wordOffset.toPx()
                },
            )

            Spacer(Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha },
            ) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, colors.tint.copy(alpha = 0.7f)),
                            ),
                        ),
                )
                Spacer(Modifier.width(10.dp))
                CupertinoText(
                    text = stringResource(R.string.app_subtitle).uppercase(),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.2.sp,
                    color = colors.secondaryLabel,
                    maxLines = 1,
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(colors.tint.copy(alpha = 0.7f), Color.Transparent),
                            ),
                        ),
                )
            }

            Spacer(Modifier.height(22.dp))

            CupertinoText(
                text = stringResource(R.string.app_tagline),
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha },
                fontSize = 13.sp,
                letterSpacing = 0.4.sp,
                color = colors.secondaryLabel,
            )
        }

        CupertinoText(
            text = stringResource(R.string.app_footer),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 22.dp)
                .graphicsLayer { alpha = footerAlpha },
            fontSize = 10.5.sp,
            letterSpacing = 0.6.sp,
            color = colors.secondaryLabel.copy(alpha = 0.8f),
        )
    }
}
