package com.chenzixin.wrongbook.feature.scan

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.component.GlassSurface
import com.chenzixin.wrongbook.ui.component.WbIconTile
import com.chenzixin.wrongbook.ui.component.WbIcons
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbPageHeader
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.wbTabBarSpace
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.CheckmarkCircle
import io.github.robinpcrd.cupertino.icons.outlined.ListBullet
import io.github.robinpcrd.cupertino.icons.outlined.MagnifyingGlass
import io.github.robinpcrd.cupertino.icons.outlined.Plus
import io.github.robinpcrd.cupertino.icons.outlined.RectanglePortraitAndArrowForward

/**
 * 扫描 Tab：入口页。
 *
 * 真正的拍照页是 P3 的全屏路由（[CameraCaptureScreen]），不放 Tab 里。
 * 这里只负责一件事：把「拍一页 → 自动切题 → 批量入库」讲清楚，并给一个足够大的行动按钮。
 *
 * 注意：相机权限在进入拍照页时才申请，不要放在 App 启动时。
 */
@Composable
fun ScanTabScreen(onStartCapture: () -> Unit) {
    val colors = wbColors()
    val bottomSpace = wbTabBarSpace()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomSpace),
    ) {
        WbPageHeader(
            title = "扫描",
            kicker = "拍照识字 · 本地完成",
            subtitle = "一整页拍下来，自动切成一道道题",
        )

        HeroCard(onStartCapture = onStartCapture)

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FeatureTile(
                icon = CupertinoIcons.Outlined.MagnifyingGlass,
                title = "本地识别",
                subtitle = "断网可用",
                modifier = Modifier.weight(1f),
            )
            FeatureTile(
                icon = CupertinoIcons.Outlined.ListBullet,
                title = "智能切题",
                subtitle = "按题号分割",
                modifier = Modifier.weight(1f),
            )
            FeatureTile(
                icon = CupertinoIcons.Outlined.CheckmarkCircle,
                title = "框选入库",
                subtitle = "多选再保存",
                modifier = Modifier.weight(1f),
            )
        }

        WbSection(
            header = "扫描流程",
            footer = "识别完全在本机完成，图片不会上传；只有你主动点「AI 整理」时才会发送文本。",
        ) {
            WbListRow(
                title = "对准整页",
                subtitle = "尽量铺平、光线均匀，四角都在画面内",
                icon = CupertinoIcons.Outlined.RectanglePortraitAndArrowForward,
            )
            WbListRow(
                title = "点按快门",
                subtitle = "自动矫正透视并识别题目区域",
                icon = CupertinoIcons.Outlined.Plus,
            )
            WbListRow(
                title = "框选入库",
                subtitle = "题号自动编号，可多选、可打标签",
                icon = CupertinoIcons.Outlined.ListBullet,
                showDivider = false,
            )
        }
    }
}

@Composable
private fun HeroCard(onStartCapture: () -> Unit) {
    val colors = wbColors()

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 30.dp,
        elevation = 18.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScanFrame(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
            )

            Spacer(Modifier.height(14.dp))
            CupertinoText(
                text = "把整页错题拍下来",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.label,
            )
            Spacer(Modifier.height(6.dp))
            CupertinoText(
                text = "自动识别题号并框出每一道题，选中的才会入库",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = colors.secondaryLabel,
            )
            Spacer(Modifier.height(14.dp))
            WbPrimaryButton(
                text = "立即扫描",
                onClick = onStartCapture,
                icon = WbIcons.Camera,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(colors.success),
                )
                Spacer(Modifier.width(6.dp))
                CupertinoText(
                    text = "识别在手机上完成 · 图片不上传",
                    fontSize = 11.sp,
                    color = colors.secondaryLabel,
                )
            }
        }
    }
}

/** 取景框插画：四角括号 + 缓慢下移的扫描线。 */
@Composable
private fun ScanFrame(modifier: Modifier = Modifier) {
    val colors = wbColors()
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanProgress",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = 6.dp.toPx()
            val w = size.width - inset * 2
            val h = size.height - inset * 2
            val topLeft = Offset(inset, inset)
            val radius = CornerRadius(26.dp.toPx())
            val bracket = 26.dp.toPx()
            val stroke = 2.5.dp.toPx()
            val lineColor = colors.tint.copy(alpha = 0.85f)
            val outlineColor = colors.tint.copy(alpha = 0.16f)

            // 淡淡的取景框
            drawRoundRect(
                color = outlineColor,
                topLeft = topLeft,
                size = Size(w, h),
                cornerRadius = radius,
                style = Stroke(width = 1.dp.toPx()),
            )

            // 四角括号
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, y),
                    end = Offset(x + dx * bracket, y),
                    strokeWidth = stroke,
                )
                drawLine(
                    color = lineColor,
                    start = Offset(x, y),
                    end = Offset(x, y + dy * bracket),
                    strokeWidth = stroke,
                )
            }
            val left = topLeft.x + 10.dp.toPx()
            val right = topLeft.x + w - 10.dp.toPx()
            val top = topLeft.y + 10.dp.toPx()
            val bottom = topLeft.y + h - 10.dp.toPx()
            corner(left, top, 1f, 1f)
            corner(right, top, -1f, 1f)
            corner(left, bottom, 1f, -1f)
            corner(right, bottom, -1f, -1f)

            // 扫描线
            val scanY = top + (bottom - top) * (0.12f + 0.76f * progress)
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        colors.tint.copy(alpha = 0.7f),
                        colors.accentEnd.copy(alpha = 0.55f),
                        Color.Transparent,
                    ),
                    startX = left,
                    endX = right,
                ),
                topLeft = Offset(left, scanY - 1.dp.toPx()),
                size = Size(right - left, 2.dp.toPx()),
            )
        }

        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(colors.accent, colors.accentEnd),
                    ),
                )
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(size.width * 0.32f, size.height * 0.24f),
                            radius = size.width * 0.7f,
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            CupertinoIcon(
                imageVector = WbIcons.Camera,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun FeatureTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = wbColors()
    GlassSurface(
        modifier = modifier,
        cornerRadius = 20.dp,
        elevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 11.dp),
        ) {
            WbIconTile(icon = icon, size = 30.dp, corner = 10.dp)
            Spacer(Modifier.height(10.dp))
            CupertinoText(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.label,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            CupertinoText(
                text = subtitle,
                fontSize = 10.sp,
                color = colors.secondaryLabel,
                maxLines = 1,
            )
        }
    }
}
