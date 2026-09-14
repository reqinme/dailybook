package com.dailybook.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailybook.app.timer.TimerViewModel
import com.dailybook.app.ui.LedgerScreen
import com.dailybook.app.ui.SettingsScreen
import com.dailybook.app.ui.StatsScreen
import com.dailybook.app.ui.TimerScreen
import com.dailybook.app.ui.TodoScreen
import com.dailybook.app.ui.theme.DailyBookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyBookApp()
        }
    }
}

private data class Tab(val title: String, val icon: ImageVector)

/** 宽屏阈值：>= 600dp（平板、横屏手机、折叠屏展开）改用侧边导航栏 */
private const val WIDE_SCREEN_DP = 600

/** 内容区域最大宽度，避免平板上文字被拉得过于松散 */
private val MAX_CONTENT_WIDTH = 720.dp

@Composable
fun DailyBookApp(
    vm: MainViewModel = viewModel(),
    timerVm: TimerViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val timerState by timerVm.state.collectAsStateWithLifecycle()
    val themeMode by vm.settings.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by vm.settings.dynamicColor.collectAsStateWithLifecycle()

    val tabs = remember {
        listOf(
            Tab("记账", Icons.Filled.AccountBalanceWallet),
            Tab("待办", Icons.Filled.Checklist),
            Tab("专注", Icons.Filled.Timer),
            Tab("统计", Icons.Filled.BarChart),
            Tab("设置", Icons.Filled.Settings)
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    val wideScreen = LocalConfiguration.current.screenWidthDp >= WIDE_SCREEN_DP

    DailyBookTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        // Android 13+ 需要授权才能弹出「专注结束」通知
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { }
        )
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 专注计时中保持屏幕常亮
        KeepScreenOn(enabled = timerState.isRunning && timerState.settings.keepScreenOn)

        if (wideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationRailItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) }
                        )
                    }
                }
                AppContent(
                    selectedTab = selectedTab,
                    state = state,
                    timerState = timerState,
                    vm = vm,
                    timerVm = timerVm,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                )
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                label = { Text(tab.title) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                AppContent(
                    selectedTab = selectedTab,
                    state = state,
                    timerState = timerState,
                    vm = vm,
                    timerVm = timerVm,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

/** 内容区：宽屏时限宽居中，切换标签带淡入淡出 */
@Composable
private fun AppContent(
    selectedTab: Int,
    state: UiState,
    timerState: com.dailybook.app.timer.TimerUiState,
    vm: MainViewModel,
    timerVm: TimerViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = MAX_CONTENT_WIDTH)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                        fadeOut(animationSpec = tween(120))
                },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    0 -> LedgerScreen(state = state, vm = vm)
                    1 -> TodoScreen(state = state, vm = vm)
                    2 -> TimerScreen(state = timerState, stats = state.focusStats, vm = timerVm)
                    3 -> StatsScreen(state = state, vm = vm)
                    else -> SettingsScreen(state = state, vm = vm, timerVm = timerVm)
                }
            }
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
