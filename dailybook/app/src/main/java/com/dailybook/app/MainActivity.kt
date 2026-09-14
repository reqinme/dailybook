package com.dailybook.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
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
            Box(Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> LedgerScreen(state = state, vm = vm)
                    1 -> TodoScreen(state = state, vm = vm)
                    2 -> TimerScreen(state = timerState, vm = timerVm)
                    3 -> StatsScreen(state = state, timerState = timerState, vm = vm)
                    else -> SettingsScreen(
                        state = state,
                        timerState = timerState,
                        vm = vm,
                        timerVm = timerVm
                    )
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
