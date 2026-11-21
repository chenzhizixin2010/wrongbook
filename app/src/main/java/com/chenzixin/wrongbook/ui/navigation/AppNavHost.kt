package com.chenzixin.wrongbook.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chenzixin.wrongbook.feature.chat.ChatSessionScreen
import com.chenzixin.wrongbook.feature.chat.ChatTabScreen
import com.chenzixin.wrongbook.feature.detail.QuestionDetailScreen
import com.chenzixin.wrongbook.feature.home.HomeTabScreen
import com.chenzixin.wrongbook.feature.review.ReviewTabScreen
import com.chenzixin.wrongbook.feature.scan.CameraCaptureScreen
import com.chenzixin.wrongbook.feature.scan.ScanTabScreen
import com.chenzixin.wrongbook.feature.settings.ProfileEditScreen
import com.chenzixin.wrongbook.feature.settings.SettingsTabScreen
import com.chenzixin.wrongbook.feature.settings.UpdateScreen
import com.chenzixin.wrongbook.feature.export.ExportScreen
import com.chenzixin.wrongbook.feature.export.ImportScreen
import io.github.robinpcrd.cupertino.CupertinoIcon
import io.github.robinpcrd.cupertino.CupertinoNavigationBar
import io.github.robinpcrd.cupertino.CupertinoNavigationBarItem
import io.github.robinpcrd.cupertino.CupertinoScaffold
import io.github.robinpcrd.cupertino.CupertinoText
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.ArrowClockwise
import io.github.robinpcrd.cupertino.icons.outlined.Gearshape
import io.github.robinpcrd.cupertino.icons.outlined.ListBullet
import io.github.robinpcrd.cupertino.icons.outlined.Paperplane
import io.github.robinpcrd.cupertino.icons.outlined.RectanglePortraitAndArrowForward

/**
 * 路由表。二级页面用 `Routes.xxx()` 生成路径，避免手拼字符串出错。
 */
object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val CHAT = "chat"
    const val REVIEW = "review"
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
    const val EXPORT = "settings/export"
    const val IMPORT = "settings/import"
}

/**
 * 5 个主 Tab。
 *
 * 图标说明：Cupertino 3.3.1 内置图标集只有 44 个 outlined 图标，没有相机/书本图标，
 * 这里用语义最近的替代；P8 统一替换为正式图标时再调整。
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
        override val icon: ImageVector get() = CupertinoIcons.Outlined.RectanglePortraitAndArrowForward
    }

    data object Chat : WbTab {
        override val route = Routes.CHAT
        override val label = "AI 对话"
        override val icon: ImageVector get() = CupertinoIcons.Outlined.Paperplane
    }

    data object Review : WbTab {
        override val route = Routes.REVIEW
        override val label = "巩固"
        override val icon: ImageVector get() = CupertinoIcons.Outlined.ArrowClockwise
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
    WbTab.Review,
    WbTab.Settings,
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = WbTabs.any { it.route == currentRoute }

    CupertinoScaffold(
        bottomBar = {
            if (showBottomBar) {
                CupertinoNavigationBar {
                    WbTabs.forEach { tab ->
                        CupertinoNavigationBarItem(
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
                            icon = {
                                CupertinoIcon(imageVector = tab.icon, contentDescription = tab.label)
                            },
                            label = { CupertinoText(text = tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeTabScreen(
                    onOpenQuestion = { id -> navController.navigate(Routes.question(id)) },
                    onNewQuestion = { navController.navigate(Routes.MANUAL) },
                )
            }
            composable(Routes.SCAN) {
                ScanTabScreen(onStartCapture = { navController.navigate(Routes.CAPTURE) })
            }
            composable(Routes.CHAT) {
                ChatTabScreen(onOpenSession = { id -> navController.navigate(Routes.chatSession(id)) })
            }
            composable(
                route = Routes.CHAT_SESSION,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType; defaultValue = 0L }),
            ) {
                ChatSessionScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.REVIEW) { ReviewTabScreen() }
            composable(Routes.SETTINGS) {
                SettingsTabScreen(
                    onOpenProfile = { id -> navController.navigate(Routes.profileEdit(id)) },
                    onOpenTags = { navController.navigate(Routes.TAGS) },
                    onOpenUpdate = { navController.navigate(Routes.UPDATE) },
                    onOpenExport = { navController.navigate(Routes.EXPORT) },
                    onOpenImport = { navController.navigate(Routes.IMPORT) },
                )
            }
            composable(
                route = Routes.PROFILE_EDIT,
                arguments = listOf(navArgument("profileId") { type = NavType.LongType; defaultValue = 0L }),
            ) {
                ProfileEditScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CAPTURE) {
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
            composable(Routes.MANUAL) {
                com.chenzixin.wrongbook.feature.detail.ManualQuestionScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.QUESTION,
                arguments = listOf(navArgument("questionId") { type = NavType.LongType; defaultValue = 0L }),
            ) {
                QuestionDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChat = { id -> navController.navigate(Routes.chatSession(id)) },
                )
            }
            composable(Routes.TAGS) {
                com.chenzixin.wrongbook.feature.tag.TagsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.UPDATE) {
                UpdateScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.EXPORT) {
                ExportScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.IMPORT) {
                ImportScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
