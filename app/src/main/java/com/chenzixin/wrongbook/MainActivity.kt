package com.chenzixin.wrongbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.chenzixin.wrongbook.data.prefs.ThemeMode
import com.chenzixin.wrongbook.data.prefs.ThemePrefs
import com.chenzixin.wrongbook.data.prefs.UiStyle
import com.chenzixin.wrongbook.ui.component.ErrataSplash
import com.chenzixin.wrongbook.ui.navigation.AppNavHost
import com.chenzixin.wrongbook.ui.theme.WrongBookTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePrefs: ThemePrefs

    @Inject
    lateinit var appPrefs: com.chenzixin.wrongbook.data.prefs.AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        // 手势导航条的系统「对比压暗层」会让底部出现一条与页面不一致的色带
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        setContent {
            val mode by themePrefs.mode.collectAsState(initial = ThemeMode.SYSTEM)
            val splashEnabled by appPrefs.splashEnabled.collectAsState(initial = true)
            val uiStyle by themePrefs.uiStyle.collectAsState(initial = UiStyle.MIUIX)
            val dark = when (mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // enableEdgeToEdge() 只按系统深浅色推断图标颜色；用户在设置里强制主题时要跟着改，
            // 否则会出现「深色背景 + 深色状态栏图标」。
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            WrongBookTheme(darkTheme = dark, uiStyle = uiStyle) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                    if (splashEnabled) SplashHost()
                }
            }
        }
    }
}

/**
 * 开屏只是盖在已渲染主界面之上的一层，淡出后即销毁。
 *
 * 用 rememberSaveable：旋转屏幕/切换深色时不会重新播一遍；
 * 冷启动时系统启动窗口（品牌标记）→ 开屏（极光 + 字标）→ 主界面，是一条连续的视觉路径。
 */
@Composable
private fun SplashHost() {
    var visible by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1_200)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = EnterTransition.None,
        exit = fadeOut(animationSpec = tween(420)),
    ) {
        ErrataSplash()
    }
}

