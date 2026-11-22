package com.chenzixin.wrongbook.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chenzixin.wrongbook.BuildConfig
import com.chenzixin.wrongbook.feature.chat.ChatSessionScreen
import com.chenzixin.wrongbook.feature.chat.ChatTabScreen
import com.chenzixin.wrongbook.feature.detail.QuestionDetailScreen
import com.chenzixin.wrongbook.feature.export.ExportScreen
import com.chenzixin.wrongbook.feature.export.ImportScreen
import com.chenzixin.wrongbook.feature.generate.GenerateScreen
import com.chenzixin.wrongbook.feature.home.HomeTabScreen
import com.chenzixin.wrongbook.feature.scan.CameraCaptureScreen
import com.chenzixin.wrongbook.feature.scan.ScanTabScreen
import com.chenzixin.wrongbook.feature.settings.ProfileEditScreen
import com.chenzixin.wrongbook.feature.settings.SettingsTabScreen
import com.chenzixin.wrongbook.feature.settings.UpdateScreen
import com.chenzixin.wrongbook.feature.update.UpdateCheckViewModel
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.component.UpdateRecommendedDialog
import com.chenzixin.wrongbook.ui.component.WbDivider
import com.chenzixin.wrongbook.ui.component.WbIcon
import com.chenzixin.wrongbook.ui.component.WbScreenBackground
import com.chenzixin.wrongbook.ui.component.WbSurface
import com.chenzixin.wrongbook.ui.component.WbText
import com.chenzixin.wrongbook.ui.theme.wbColors
import com.chenzixin.wrongbook.ui.theme.wbUiStyle
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState

/**
 * 路由表。二级页面用 `Routes.xxx()` 生成路径，避免手拼字符串出错。
 */
object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val CHAT = "chat"
    const val GENERATE = "generate"
    const val SETTINGS = "settings"

    const val CAPTURE = "scan/capture"

    const val CHAT_SESSION = "chat/{sessionId}"
    fun chatSession(id: Long) = "chat/$id"

    const val MANUAL = "manual/new"

    const val QUESTION = "question/{questionId}"
    fun question(id: Long) = "question/$id"

    const val PROFILE_EDIT = "settings/profile/{profileId}"
    fun profileEdit(id: Long) = "settings/profile/$id"

    const val TAGS = "tags"
    const val UPDATE = "settings/update"
    const val STATS = "settings/stats"
    const val EXPORT = "settings/export"
    const val IMPORT = "settings/import"
}

/** 5 个主 Tab，图标全部来自 Miuix 图标库。 */
sealed interface WbTab {
    val route: String
    val label: String
    val icon: ImageVector

    data object Home : WbTab {
        override val route = Routes.HOME
        override val label = "题库"
        override val icon: ImageVector get() = MiuixIcons.Notes
    }

    data object Scan : WbTab {
        override val route = Routes.SCAN
        override val label = "扫描"
        override val icon: ImageVector get() = MiuixIcons.Scan
    }

    data object Chat : WbTab {
        override val route = Routes.CHAT
        override val label = "AI 对话"
        override val icon: ImageVector get() = MiuixIcons.Send
    }

    data object Generate : WbTab {
        override val route = Routes.GENERATE
        override val label = "生成"
        override val icon: ImageVector get() = MiuixIcons.Edit
    }

    data object Settings : WbTab {
        override val route = Routes.SETTINGS
        override val label = "设置"
        override val icon: ImageVector get() = MiuixIcons.Settings
    }
}

val WbTabs: List<WbTab> = listOf(
    WbTab.Home,
    WbTab.Scan,
    WbTab.Chat,
    WbTab.Generate,
    WbTab.Settings,
)

/**
 * App 外壳：Miuix Scaffold + 贴底 NavigationBar。
 *
 * 底部导航不再悬浮；内容区通过 Scaffold 的 PaddingValues 让出导航栏高度，
 * 不做「内容从玻璃后面穿过」的花活。
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = WbTabs.any { it.route == currentRoute }

    val updateVm: UpdateCheckViewModel = hiltViewModel()
    val updateState by updateVm.state.collectAsState()
    val style = wbUiStyle()
    val ios = style == UiStyle.IOS
    val v090 = style == UiStyle.V090
    val c = wbColors()
    LaunchedEffect(Unit) {
        // 等开屏淡出后再查更新，避免「建议更新」弹窗压在品牌页上。
        delay(1_650)
        updateVm.checkOnce()
    }

    WbScreenBackground {
    Scaffold(
        containerColor = if (ios || v090) androidx.compose.ui.graphics.Color.Transparent else MiuixTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                if (v090) {
                    V090TabBar(
                        tabs = WbTabs,
                        currentRoute = currentRoute,
                        onSelect = { tab ->
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    )
                } else if (ios) {
                    IosTabBar(
                        tabs = WbTabs,
                        currentRoute = currentRoute,
                        onSelect = { tab ->
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    )
                } else {
                    NavigationBar {
                        WbTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = {
                                    if (currentRoute != tab.route) {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = tab.icon,
                                label = tab.label,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.HOME) {
                    WbRoute(insetBottom = false) {
                        HomeTabScreen(
                            onOpenQuestion = { id -> navController.navigate(Routes.question(id)) },
                            onNewQuestion = { navController.navigate(Routes.MANUAL) },
                            onOpenScan = { navController.navigate(Routes.SCAN) },
                        )
                    }
                }
                composable(Routes.SCAN) {
                    WbRoute(insetBottom = false) {
                        ScanTabScreen(onStartCapture = { navController.navigate(Routes.CAPTURE) })
                    }
                }
                composable(Routes.CHAT) {
                    WbRoute(insetBottom = false) {
                        ChatTabScreen(onOpenSession = { id -> navController.navigate(Routes.chatSession(id)) })
                    }
                }
                composable(
                    route = Routes.CHAT_SESSION,
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType; defaultValue = 0L }),
                ) {
                    WbRoute {
                        ChatSessionScreen(onBack = { navController.popBackStack() })
                    }
                }
                composable(Routes.GENERATE) {
                    WbRoute(insetBottom = false) {
                        GenerateScreen()
                    }
                }
                composable(Routes.SETTINGS) {
                    WbRoute(insetBottom = false) {
                        SettingsTabScreen(
                            onOpenProfile = { id -> navController.navigate(Routes.profileEdit(id)) },
                            onOpenTags = { navController.navigate(Routes.TAGS) },
                            onOpenUpdate = { navController.navigate(Routes.UPDATE) },
                            onOpenStats = { navController.navigate(Routes.STATS) },
                            onOpenExport = { navController.navigate(Routes.EXPORT) },
                            onOpenImport = { navController.navigate(Routes.IMPORT) },
                        )
                    }
                }
                composable(
                    route = Routes.PROFILE_EDIT,
                    arguments = listOf(navArgument("profileId") { type = NavType.LongType; defaultValue = 0L }),
                ) {
                    WbRoute {
                        ProfileEditScreen(onBack = { navController.popBackStack() })
                    }
                }
                composable(Routes.CAPTURE) {
                    WbRoute(insetTop = false, insetBottom = false) {
                        CameraCaptureScreen(
                            onDone = {
                                navController.popBackStack()
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
                composable(Routes.MANUAL) {
                    WbRoute {
                        com.chenzixin.wrongbook.feature.detail.ManualQuestionScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
                composable(
                    route = Routes.QUESTION,
                    arguments = listOf(navArgument("questionId") { type = NavType.LongType; defaultValue = 0L }),
                ) {
                    WbRoute {
                        QuestionDetailScreen(
                            onBack = { navController.popBackStack() },
                            onOpenChat = { id -> navController.navigate(Routes.chatSession(id)) },
                        )
                    }
                }
                composable(Routes.TAGS) {
                    WbRoute {
                        com.chenzixin.wrongbook.feature.tag.TagsScreen(onBack = { navController.popBackStack() })
                    }
                }
                composable(Routes.STATS) {
                    WbRoute {
                        com.chenzixin.wrongbook.feature.stats.StatsScreen(onBack = { navController.popBackStack() })
                    }
                }
                composable(Routes.UPDATE) {
                    WbRoute {
                        UpdateScreen(onBack = { navController.popBackStack() })
                    }
                }
                composable(Routes.EXPORT) {
                    WbRoute {
                        ExportScreen(onBack = { navController.popBackStack() })
                    }
                }
                composable(Routes.IMPORT) {
                    WbRoute {
                        ImportScreen(onBack = { navController.popBackStack() })
                    }
                }
            }

            val info = updateState.info
            if (updateState.showDialog && info != null) {
                UpdateRecommendedDialog(
                    info = info,
                    currentVersion = BuildConfig.VERSION_NAME,
                    onUpdate = {
                        updateVm.dismiss()
                        navController.navigate(Routes.UPDATE)
                    },
                    onLater = { updateVm.dismiss() },
                )
            }
        }
    }
    }
}

/**
 * 0.9.0 液态玻璃底栏：悬浮胶囊 + 渐变选中态。
 * 只在这个复古皮肤里启用，Miuix/iOS 仍贴底。
 */
@Composable
private fun V090TabBar(
    tabs: List<WbTab>,
    currentRoute: String?,
    onSelect: (WbTab) -> Unit,
) {
    val c = wbColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        WbSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 30.dp,
            elevation = 18.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(tab) }
                            .padding(vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        Brush.linearGradient(listOf(c.accent, c.accentEnd))
                                    } else {
                                        SolidColor(androidx.compose.ui.graphics.Color.Transparent)
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            WbIcon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(20.dp),
                                tint = if (selected) c.onAccent else c.secondaryLabel,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        WbText(
                            text = tab.label,
                            fontSize = 9.sp,
                            color = if (selected) c.accent else c.secondaryLabel,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS 底部 TabBar：贴底、半透明底、顶部发丝线。
 * 不是悬浮胶囊；只是 Cupertino 风格的系统 TabBar。
 */
@Composable
private fun IosTabBar(
    tabs: List<WbTab>,
    currentRoute: String?,
    onSelect: (WbTab) -> Unit,
) {
    val c = wbColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.barScrim),
    ) {
        WbDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 6.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WbIcon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp),
                        tint = if (selected) c.accent else c.secondaryLabel,
                    )
                    Spacer(Modifier.height(2.dp))
                    WbText(
                        text = tab.label,
                        fontSize = 10.sp,
                        color = if (selected) c.accent else c.secondaryLabel,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * 安全区包装。
 * - 主 Tab 页：只避状态栏，底部由 Scaffold 的 NavigationBar 让位；
 * - 二级页：上下都避让；
 * - 相机页：全屏出血，自己处理。
 */
@Composable
private fun WbRoute(
    insetTop: Boolean = true,
    insetBottom: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (insetTop) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier,
            )
            .then(
                if (insetBottom) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier,
            ),
    ) {
        content()
    }
}
