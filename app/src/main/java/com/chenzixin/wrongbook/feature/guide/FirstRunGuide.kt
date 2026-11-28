package com.chenzixin.wrongbook.feature.guide

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.ui.component.WbIcon
import com.chenzixin.wrongbook.ui.component.WbPrimaryButton
import com.chenzixin.wrongbook.ui.component.WbScreenBackground
import com.chenzixin.wrongbook.ui.component.WbText
import com.chenzixin.wrongbook.ui.component.WbTextButton
import com.chenzixin.wrongbook.ui.theme.wbColors
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Send

/**
 * 首次启动的用户指南：5 页横向引导，看完或跳过后写 `guide_seen`。
 *
 * 覆盖在已渲染的主界面之上（与开屏同一层思路），不参与导航，
 * 所以不会重置 Tab 栈；之后的完整说明在 设置 → 帮助 → 使用手册。
 */
@Composable
fun FirstRunGuide(
    onFinish: () -> Unit,
    onOpenManual: (() -> Unit)? = null,
) {
    val pages = GuideIntroPages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val c = wbColors()
    val page = pagerState.currentPage.coerceIn(0, pages.lastIndex)
    val isLast = page == pages.lastIndex

    // 系统返回键也按「跳过」处理，避免第一次进来就退到桌面。
    BackHandler { onFinish() }

    WbScreenBackground {
        // 盖在主界面之上，空余区域的点按也不要漏到底下的 Tab。
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) { detectTapGestures { } },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isLast) {
                    WbTextButton(text = "跳过", onClick = onFinish)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                key = { it },
            ) { index ->
                GuideIntroPageView(page = pages[index])
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    pages.indices.forEach { index ->
                        GuideDot(active = index == page)
                        if (index != pages.lastIndex) Spacer(Modifier.width(6.dp))
                    }
                }

                Spacer(Modifier.height(18.dp))

                WbPrimaryButton(
                    text = if (isLast) "开始使用" else "下一步",
                    onClick = {
                        if (isLast) {
                            onFinish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(page + 1) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = if (isLast) MiuixIcons.Ok else null,
                )

                if (isLast && onOpenManual != null) {
                    Spacer(Modifier.height(4.dp))
                    WbTextButton(
                        text = "打开完整使用手册",
                        onClick = onOpenManual,
                    )
                }

                Spacer(Modifier.height(8.dp))
                WbText(
                    text = "之后可在 设置 → 帮助 → 使用手册 重看",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = c.secondaryLabel,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun GuideIntroPageView(page: GuidePage) {
    val c = wbColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(c.tintSoft),
            contentAlignment = Alignment.Center,
        ) {
            WbIcon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = c.accent,
            )
        }

        Spacer(Modifier.height(24.dp))

        WbText(
            text = page.kicker,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            color = c.accent,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        WbText(
            text = page.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp,
            color = c.label,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        WbText(
            text = page.body,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = c.secondaryLabel,
            textAlign = TextAlign.Center,
        )

        if (page.bullets.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                page.bullets.forEach { bullet ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(c.accent),
                        )
                        Spacer(Modifier.width(9.dp))
                        WbText(
                            text = bullet,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = c.label,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideDot(active: Boolean) {
    val c = wbColors()
    val width by animateDpAsState(
        targetValue = if (active) 20.dp else 6.dp,
        label = "guideDot",
    )
    Box(
        modifier = Modifier
            .width(width)
            .height(6.dp)
            .clip(CircleShape)
            .background(if (active) c.accent else c.separator.copy(alpha = 0.55f)),
    )
}

private data class GuidePage(
    val kicker: String,
    val title: String,
    val body: String,
    val bullets: List<String>,
    val icon: ImageVector,
)

private val GuideIntroPages: List<GuidePage> = listOf(
    GuidePage(
        kicker = "欢迎",
        title = "错题，别再抄一遍",
        body = "Errata 把拍照、识别、分类和练习放在一台手机里完成。题目和图片只保存在本机。",
        bullets = listOf(
            "拍照或手输都能建题",
            "断网时依然可用",
            "没有账号，也没有广告",
        ),
        icon = MiuixIcons.Notes,
    ),
    GuidePage(
        kicker = "第一步",
        title = "拍一整页，自动切题",
        body = "对准整页拍下来，Errata 会先帮你把纸拉正，再按题号框出每一道题。",
        bullets = listOf(
            "识别在本机完成，图片不上传",
            "点按框选，只有选中的题会入库",
            "题号、文字、图片一起保存",
        ),
        icon = MiuixIcons.Scan,
    ),
    GuidePage(
        kicker = "第二步",
        title = "打标签，找得到",
        body = "标签就是你的分类和收藏夹。一题可以打多个标签，也能按「全部满足 / 任一满足」筛选。",
        bullets = listOf(
            "标签可改名、改色、调排序",
            "搜索题干、答案或 OCR 原文",
            "设置里可看掌握度与标签分布",
        ),
        icon = MiuixIcons.Favorites,
    ),
    GuidePage(
        kicker = "可选",
        title = "AI 是加分项，不是前提",
        body = "配一个 OpenAI 兼容的服务商，就能让 AI 讲解、整理、出同类题，甚至直接识图切题。",
        bullets = listOf(
            "Key 需要自己申请，App 不内置共享 Key",
            "不配置也能用本地 OCR、题库和练习卷",
            "只有主动使用 AI 时才发送内容",
        ),
        icon = MiuixIcons.Send,
    ),
    GuidePage(
        kicker = "数据",
        title = "数据在你手里",
        body = "错题、图片和对话都存在这台手机；想换机时导出 ZIP，到新手机再导入即可。",
        bullets = listOf(
            "API Key 用系统 Keystore 加密",
            "导出包不含明文 Key",
            "卸载前记得先导出备份",
        ),
        icon = MiuixIcons.Lock,
    ),
)
