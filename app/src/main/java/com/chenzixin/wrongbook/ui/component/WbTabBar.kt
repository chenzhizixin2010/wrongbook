package com.chenzixin.wrongbook.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoText

/** 标签栏的一项。UI 层不直接依赖导航层的 sealed class。 */
data class WbTabItem(
    val label: String,
    val icon: ImageVector,
)

/** 玻璃标签栏的内容高度（不含安全区）。 */
val WbTabBarHeight: Dp = 68.dp

/**
 * 列表需要预留的底部空间：标签栏本身 + 悬浮间距 + 系统导航条。
 *
 * 页面把这段空间加进 scroll 容器的 contentPadding，内容就会从玻璃后面穿过去，
 * 而不是被一条实心栏「切断」—— 这是液态玻璃最关键的一处动效。
 */
@Composable
fun wbTabBarSpace(): Dp {
    val navBottom = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    return WbTabBarHeight + 12.dp + navBottom + 26.dp
}

/**
 * 悬浮玻璃标签栏。
 *
 * iOS 26 的标签栏不再是通栏，而是一块悬浮胶囊：
 * - 底板：玻璃材质 + 24dp 投影；
 * - 选中：用一块「液态」胶囊滑过去（spring 跟随），强调色淡染；
 * - 未选中：次级文字色，图标略小。
 */
@Composable
fun WbGlassTabBar(
    items: List<WbTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val colors = wbColors()

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(WbTabBarHeight),
        cornerRadius = WbTabBarHeight / 2,
        elevation = 26.dp,
        underlay = colors.barScrim,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp, vertical = 7.dp),
        ) {
            val cellWidth = maxWidth / items.size
            val indicatorOffset by animateDpAsState(
                targetValue = cellWidth * selectedIndex,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                label = "tabIndicator",
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(cellWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colors.tintSoft)
                    .border(
                        width = 1.dp,
                        color = colors.tint.copy(alpha = if (colors.isDark) 0.38f else 0.22f),
                        shape = RoundedCornerShape(percent = 50),
                    ),
            )

            Row(modifier = Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val iconTint by animateColorAsState(
                        targetValue = if (selected) colors.tint else colors.secondaryLabel,
                        label = "tabIcon",
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 0.94f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                        label = "tabIconScale",
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable { onSelect(index) }
                            .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CupertinoIcon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier
                                .height(20.dp)
                                .width(20.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                            tint = iconTint,
                        )
                        Spacer(Modifier.height(3.dp))
                        CupertinoText(
                            text = item.label,
                            fontSize = 9.5.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = iconTint,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
