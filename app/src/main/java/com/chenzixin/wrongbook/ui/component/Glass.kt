package com.chenzixin.wrongbook.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chenzixin.wrongbook.ui.theme.WbColors
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoIcon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 会缓慢漂移的极光画布。
 *
 * 为什么不用纯色背景：液态玻璃的「透」必须靠后面的颜色才能被看见。
 * 四团低饱和径向渐变缓慢呼吸，玻璃面板压上去才会有折射感。
 *
 * 性能说明：动画只走绘制阶段（不触发重组），周期 28s；低端机可传 animated = false。
 * TODO(人验)：低端机滚动时是否掉帧。
 */

/**
 * 每个页面的底层：不透明纯色 + 静态极光。
 *
 * 为什么每个页面都要一层：
 * 全面屏手势返回时，系统会把两个页面同时做淡入淡出；如果页面本身透明，
 * 上一页的文字会和下一页叠在一起（观感很差），也会透出壁纸。
 * 这里给每页铺一层不透明底，转场时就是「一页盖一页」，不再互相透视。
 */
@Composable
fun WbPageBase(modifier: Modifier = Modifier) {
    val colors = wbColors()
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(listOf(colors.canvasTop, colors.canvasBottom)),
        )
        // 两团静态光斑：位置固定，避免每页动画不同步造成的闪烁
        val first = colors.aurora.getOrNull(0) ?: colors.accent
        val second = colors.aurora.getOrNull(1) ?: colors.accentEnd
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(first.copy(alpha = if (colors.isDark) 0.35f else 0.5f), Color.Transparent),
                center = Offset(size.width * 0.18f, size.height * 0.06f),
                radius = size.minDimension * 0.85f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(second.copy(alpha = if (colors.isDark) 0.28f else 0.38f), Color.Transparent),
                center = Offset(size.width * 0.86f, size.height * 0.72f),
                radius = size.minDimension * 0.75f,
            ),
        )
    }
}

@Composable
fun WbAmbientBackground(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    if (animated) {
        val transition = rememberInfiniteTransition(label = "aurora")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 28_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "auroraProgress",
        )
        AuroraCanvas(modifier, progress)
    } else {
        AuroraCanvas(modifier, 0f)
    }
}

@Composable
private fun AuroraCanvas(modifier: Modifier, progress: Float) {
    val colors = wbColors()
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(listOf(colors.canvasTop, colors.canvasBottom)),
        )

        val angle = progress * 2f * PI.toFloat()
        val blobs = colors.aurora

        fun blob(color: Color, cx: Float, cy: Float, radius: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0f)),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }

        blob(
            blobs[0],
            size.width * 0.12f + sin(angle) * size.width * 0.10f,
            size.height * 0.06f,
            size.height * 0.62f,
        )
        blob(
            blobs[1],
            size.width * 0.95f - cos(angle) * size.width * 0.08f,
            size.height * 0.22f,
            size.height * 0.56f,
        )
        blob(
            blobs[2],
            size.width * 0.02f + cos(angle) * size.width * 0.05f,
            size.height * 0.88f,
            size.height * 0.52f,
        )
        blob(
            blobs[3],
            size.width * 0.72f + sin(angle + 1.2f) * size.width * 0.08f,
            size.height * 0.78f,
            size.height * 0.46f,
        )

        // 顶部环境光：让状态栏下面有一层柔和亮面，而不是死平。
        drawRect(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (colors.isDark) 0.05f else 0.22f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = size.height * 0.32f,
            ),
        )

        if (colors.isDark) {
            // 暗色下压暗四周，玻璃边缘的高光才有对比。
            drawRect(
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.38f)),
                    center = Offset(size.width * 0.5f, size.height * 0.42f),
                    radius = size.maxDimension * 0.78f,
                ),
            )
        }
    }
}

/**
 * 玻璃面板：全 App 唯一的「材质」实现。
 *
 * 叠了 6 层：
 * 1. 投影（带色偏，不是死灰）；
 * 2. 上亮下暗的半透明填充（厚度）；
 * 3. 按下时整片变亮（触感反馈）；
 * 4. 左上 → 右下的斜向镜面反光；
 * 5. 左上角一片外溢光斑（液态感的关键）；
 * 6. 1dp 受光描边 + 内圈高光（玻璃切断边缘）。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    tint: Color? = null,
    underlay: Color? = null,
    elevation: Dp = 10.dp,
    glow: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = wbColors()
    val shape = RoundedCornerShape(cornerRadius)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "glassPress",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "glassScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = glow ?: colors.shadow,
                spotColor = glow ?: colors.shadow,
            )
            .clip(shape)
            .drawBehind { drawGlass(colors, cornerRadius, tint, pressProgress, underlay) }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

/** 在 [GlassSurface] 的 drawBehind 里画玻璃本体。 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlass(
    colors: WbColors,
    cornerRadius: Dp,
    tint: Color?,
    pressed: Float,
    underlay: Color?,
) {
    val radiusPx = cornerRadius.toPx()
    val corner = CornerRadius(radiusPx)

    if (underlay != null) {
        drawRoundRect(color = underlay, cornerRadius = corner)
    }

    // 1. 填充：上亮下暗；有色玻璃则压一层强调色。
    drawRoundRect(
        brush = if (tint != null) {
            Brush.verticalGradient(
                listOf(tint.copy(alpha = 0.94f), tint.copy(alpha = 0.76f)),
                startY = 0f,
                endY = size.height,
            )
        } else {
            Brush.verticalGradient(
                listOf(colors.glassTop, colors.glassBottom),
                startY = 0f,
                endY = size.height,
            )
        },
        cornerRadius = corner,
    )

    // 2. 按下变亮。
    if (pressed > 0f) {
        drawRoundRect(
            color = Color.White.copy(alpha = if (colors.isDark) 0.10f * pressed else 0.22f * pressed),
            cornerRadius = corner,
        )
    }

    // 3. 斜向镜面反光（只覆盖上半部，越往下越透）。
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(colors.glassSheen.copy(alpha = 0.22f), Color.Transparent),
            start = Offset(0f, 0f),
            end = Offset(size.width * 0.9f, size.height * 0.85f),
        ),
        cornerRadius = corner,
    )

    // 4. 左上角外溢光斑：让玻璃看起来是「凸」的。
    val hotspotRadius = size.width * 0.55f
    val hotspotCenter = Offset(size.width * 0.10f, -size.height * 0.18f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(colors.glassSheen.copy(alpha = 0.30f), Color.Transparent),
            center = hotspotCenter,
            radius = hotspotRadius,
        ),
        radius = hotspotRadius,
        center = hotspotCenter,
    )

    // 5. 外描边：左上受光、右下背光。
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(colors.glassStrokeTop, colors.glassStrokeBottom),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
        ),
        cornerRadius = corner,
        style = Stroke(width = 1.dp.toPx()),
    )

    // 6. 内圈高光：顶部一圈更亮的 1dp，模拟玻璃切口。
    val inset = 1.4.dp.toPx()
    if (size.width > inset * 3 && size.height > inset * 3) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (colors.isDark) 0.34f else 0.55f),
                    Color.Transparent,
                ),
                startY = inset,
                endY = size.height * 0.5f,
            ),
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = CornerRadius((radiusPx - inset).coerceAtLeast(0f)),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

/** 圆形玻璃图标按钮（返回、新增、删除）。 */
@Composable
fun WbGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 18.dp,
    tint: Color? = null,
    containerColor: Color? = null,
) {
    val colors = wbColors()
    if (containerColor != null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(containerColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            CupertinoIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = tint ?: colors.label,
            )
        }
        return
    }
    GlassSurface(
        modifier = modifier.size(size),
        cornerRadius = size / 2,
        elevation = 6.dp,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CupertinoIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = tint ?: colors.label,
            )
        }
    }
}

/** 渐变图标底板：分组表头、空状态、功能入口共用。 */
@Composable
fun WbIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    corner: Dp = 11.dp,
    color: Color? = null,
) {
    val colors = wbColors()
    val base = color ?: colors.accent
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(
                Brush.linearGradient(
                    listOf(base.copy(alpha = 0.26f), colors.accentEnd.copy(alpha = 0.14f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        CupertinoIcon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(size * 0.52f),
            tint = base,
        )
    }
}
