package com.chenzixin.wrongbook.feature.scan

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import com.chenzixin.wrongbook.core.ImageUtils.detectContentBox
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.core.ImageUtils.warpQuad
import com.chenzixin.wrongbook.ui.component.WbText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 透视矫正：拖四个角，把倾斜拍摄的试卷拉正。
 *
 * 数学在 [com.chenzixin.wrongbook.domain.scan.Homography]（有单测），
 * 这里只负责交互与调用 warpQuad。
 */
@Composable
fun PerspectiveCropScreen(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit,
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val imgW = bitmap.width.toFloat()
    val imgH = bitmap.height.toFloat()

    // 初始框：优先自动框住「有文字的区域」，识别不到内容才退回 4% 内缩
    var quad by remember(bitmap) {
        val auto = bitmap.detectContentBox()
        val l = (auto?.left ?: 0.04f) * imgW
        val t = (auto?.top ?: 0.04f) * imgH
        val r = (auto?.right ?: 0.96f) * imgW
        val b = (auto?.bottom ?: 0.96f) * imgH
        mutableStateOf(
            listOf(
                l to t,
                r to t,
                r to b,
                l to b,
            ),
        )
    }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableStateOf(-1) }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val hitRadius = with(density) { 56.dp.toPx() }
    val currentQuad by androidx.compose.runtime.rememberUpdatedState(quad)

    // 画布内图片的缩放与偏移（触摸与绘制共用同一套计算）
    fun layout(size: IntSize): Triple<Float, Float, Float> {
        if (size.width == 0 || size.height == 0) return Triple(1f, 0f, 0f)
        val scale = min(size.width / imgW, size.height / imgH)
        val offsetX = (size.width - imgW * scale) / 2f
        val offsetY = (size.height - imgH * scale) / 2f
        return Triple(scale, offsetX, offsetY)
    }

    val c = MiuixTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize().background(c.background)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                // 注意：key 只能放 canvasSize。之前把 quad 也放进 key，
                // 拖动时 quad 一变就重启 pointerInput，手势立刻被取消 —— 表现就是「拖不动」。
                .pointerInput(canvasSize) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val (scale, ox, oy) = layout(canvasSize)
                            dragging = currentQuad.indexOfFirst { p ->
                                val sx = ox + p.first * scale
                                val sy = oy + p.second * scale
                                (Offset(sx, sy) - start).getDistance() < hitRadius
                            }
                        },
                        onDragEnd = { dragging = -1 },
                        onDragCancel = { dragging = -1 },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val index = dragging
                            if (index < 0) return@detectDragGestures
                            val (scale, _, _) = layout(canvasSize)
                            if (scale <= 0f) return@detectDragGestures
                            val current = currentQuad[index]
                            val nx = (current.first + dragAmount.x / scale).coerceIn(0f, imgW)
                            val ny = (current.second + dragAmount.y / scale).coerceIn(0f, imgH)
                            quad = currentQuad.toMutableList().also { it[index] = nx to ny }
                        },
                    )
                },
        ) {
            val scale = min(size.width / imgW, size.height / imgH)
            val offsetX = (size.width - imgW * scale) / 2f
            val offsetY = (size.height - imgH * scale) / 2f

            drawImage(
                image = image,
                dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                dstSize = IntSize((imgW * scale).toInt(), (imgH * scale).toInt()),
            )

            fun screen(p: Pair<Float, Float>) = Offset(offsetX + p.first * scale, offsetY + p.second * scale)

            val path = Path().apply {
                val first = screen(quad[0])
                moveTo(first.x, first.y)
                quad.drop(1).forEach { p -> val s = screen(p); lineTo(s.x, s.y) }
                close()
            }
            drawPath(path = path, color = c.primary.copy(alpha = 0.20f))
            drawPath(path = path, color = c.primary, style = Stroke(width = 4f))

            // 手柄尺寸必须按 dp 换算：Canvas 里直接写 18f 是 18px，高密度屏上小到点不中
            val outer = 18.dp.toPx()
            val inner = 12.dp.toPx()
            quad.forEach { p ->
                val s = screen(p)
                drawCircle(color = c.onSurface.copy(alpha = 0.40f), radius = outer * 1.7f, center = s)
                drawCircle(color = c.onSurface, radius = outer, center = s)
                drawCircle(color = c.primary, radius = inner, center = s)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(c.surface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            WbText(
                text = "拖动四个圆点贴住试卷四角；拍歪了也不用重拍",
                fontSize = 12.sp,
                color = c.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.chenzixin.wrongbook.ui.component.WbChip(
                    label = "重拍",
                    selected = false,
                    onClick = onCancel,
                    onLight = true,
                )
                com.chenzixin.wrongbook.ui.component.WbChip(
                    label = "取全图",
                    selected = false,
                    onClick = {
                        quad = listOf(0f to 0f, imgW to 0f, imgW to imgH, 0f to imgH)
                    },
                    onLight = true,
                )
                Spacer(Modifier.weight(1f))
                com.chenzixin.wrongbook.ui.component.WbPrimaryButton(
                    text = if (working) "处理中…" else "拉正并识别",
                    onClick = {
                        if (!working) {
                            working = true
                            scope.launch {
                                val result = withContext(Dispatchers.Default) {
                                    runCatching { bitmap.warpQuad(quad) }.getOrNull()
                                }
                                working = false
                                if (result != null) onConfirm(result)
                            }
                        }
                    },
                    enabled = !working,
                    fontSize = 14,
                )
            }
        }
    }
}
