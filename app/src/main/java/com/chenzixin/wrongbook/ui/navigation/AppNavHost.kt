package com.chenzixin.wrongbook.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chenzixin.wrongbook.feature.chat.ChatSessionScreen
import com.chenzixin.wrongbook.feature.chat.ChatTabScreen
import com.chenzixin.wrongbook.feature.detail.QuestionDetailScreen
import com.chenzixin.wrongbook.feature.export.ExportScreen
import com.chenzixin.wrongbook.feature.export.ImportScreen
import com.chenzixin.wrongbook.feature.home.HomeTabScreen
import com.chenzixin.wrongbook.feature.generate.GenerateScreen
import com.chenzixin.wrongbook.feature.scan.CameraCaptureScreen
import com.chenzixin.wrongbook.feature.scan.ScanTabScreen
import com.chenzixin.wrongbook.feature.settings.ProfileEditScreen
import com.chenzixin.wrongbook.feature.settings.SettingsTabScreen
import com.chenzixin.wrongbook.feature.settings.UpdateScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.chenzixin.wrongbook.BuildConfig
import com.chenzixin.wrongbook.feature.update.UpdateCheckViewModel
import com.chenzixin.wrongbook.ui.component.UpdateRecommendedDialog
import com.chenzixin.wrongbook.ui.component.WbAmbientBackground
import com.chenzixin.wrongbook.ui.component.WbPageBase
import com.chenzixin.wrongbook.ui.component.WbGlassTabBar
import com.chenzixin.wrongbook.ui.component.WbIcons
import com.chenzixin.wrongbook.ui.component.WbTabItem
import com.chenzixin.wrongbook.ui.theme.wbColors
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.Gearshape
import io.github.robinpcrd.cupertino.icons.outlined.ListBullet
import io.github.robinpcrd.cupertino.icons.outlined.Paperplane
import io.github.robinpcrd.cupertino.icons.outlined.RectanglePortraitAndArrowForward
import io.github.robinpcrd.cupertino.icons.outlined.SquareAndPencil
import kotlinx.coroutines.delay

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

/**
 * 5 个主 Tab。
 *
 * 图标说明：Cupertino 3.3.1 内置图标集只有 44 个 outlined 图标，没有相机/书本图标，
 * 这里用语义最近的替代。
 */
sealed interface WbTab {
    val route: String
    val label: String
    val icon: ImageVector

    data object Home : WbTab {
        override val route = Routes.HOME
        override val label = "题库"
        override val icon: ImageVector get() = CupertinoIcons.Outlined.ListBullet
    }

    data object Scan : WbTab {
        override val route = Routes.SCAN
        override val label = "扫描"
        override val icon: ImageVector get() = WbIcons.Camera
    }

    data object Chat : WbTab {
        override val route = Routes.CHAT
        override val label = "AI 对话"
        override val icon: ImageVector get() = CupertinoIcons.Outlined.Paperplane
    }

    data object Generate : WbTab {
        override val route = Routes.GENERATE
        override val label = "生成"
        override val icon: ImageVector get() = CupertinoIcons.Outlined.SquareAndPencil
    }

    data object Settings : WbTab {
        override val route = Routes.SETTINGS
        override val label = "设置"
        override val icon: ImageVector get() = CupertinoIcons.Outlined.Gearshape
    }
}

val WbTabs: List<WbTab> = listOf(
    WbTab.Home,
    WbTab.Scan,
    WbTab.Chat,
    WbTab.Generate,
    WbTab.Settings,
)

private val TabItems: List<WbTabItem> = WbTabs.map { WbTabItem(label = it.label, icon = it.icon) }

/**
 * App 外壳：极光画布 → 页面内容 → 悬浮玻璃标签栏。
 *
 * 不再用 CupertinoScaffold：通栏底部栏会把内容「切断」，
 * 而液态玻璃需要内容从半透明栏后面穿过去。安全区改由 [WbRoute] 分页处理。
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val updateVm: UpdateCheckViewModel = hiltViewModel()
    val updateState by updateVm.state.collectAsState()

    // 启动静默检查一次：大版本才弹窗，小版本不打扰
    LaunchedEffect(Unit) {
        // 等开屏淡出后再查更新，避免「建议更新」弹窗压在品牌页上。
        delay(1_650)
        updateVm.checkOnce()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedIndex = WbTabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val showBottomBar = WbTabs.any { it.route == currentRoute }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val colors = wbColors()

    Box(modifier = Modifier.fillMaxSize().background(colors.canvasBottom)) {
        WbAmbientBackground()

        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
            // iOS 风格滑页：新页从右滑入，旧页向左退 1/4；返回时反过来
            enterTransition = {
                slideInHorizontally(tween(320)) { it } + fadeIn(tween(200))
            },
            exitTransition = {
                slideOutHorizontally(tween(320)) { -it / 4 } + fadeOut(tween(200))
            },
            popEnterTransition = {
                slideInHorizontally(tween(320)) { -it / 4 } + fadeIn(tween(200))
            },
            popExitTransition = {
                slideOutHorizontally(tween(320)) { it } + fadeOut(tween(200))
            },
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
                // 相机要全屏出血到状态栏/导航栏下面，自己处理安全区。
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

        if (showBottomBar) {
            // 没有真实背景模糊，用一层向上淡出的画布色把「穿过标签栏」的内容压暗：
            // 既保留玻璃的透，又不让正文穿透到栏上抢读。
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.5f to Color.Transparent,
                                0.82f to colors.canvasBottom.copy(alpha = 0.55f),
                                1f to colors.canvasBottom.copy(alpha = 0.92f),
                            ),
                        ),
                    ),
            )
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

        AnimatedVisibility(
            visible = showBottomBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(320),
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200),
            ) + fadeOut(animationSpec = tween(140)),
        ) {
            WbGlassTabBar(
                items = TabItems,
                selectedIndex = selectedIndex,
                onSelect = { index ->
                    val tab = WbTabs[index]
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = navBottom + 10.dp),
            )
        }
    }
}

/**
 * 安全区包装。
 *
 * - 主 Tab 页：只避让状态栏，底部留给标签栏（内容从玻璃后面穿过）；
 * - 二级页：上下都避让；
 * - 相机页：都不避让，全屏出血。
 */
@Composable
private fun WbRoute(
    insetTop: Boolean = true,
    insetBottom: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 底图铺满全屏（含状态栏/手势条区域），否则顶部底部会露出一条纯色「白条」
        WbPageBase()

        // 只让内容避让系统栏
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
}
