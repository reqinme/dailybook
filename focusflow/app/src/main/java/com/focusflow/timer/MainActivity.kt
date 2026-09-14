package com.focusflow.timer

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
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusflow.timer.timer.TimerViewModel
import com.focusflow.timer.ui.SettingsScreen
import com.focusflow.timer.ui.StatsScreen
import com.focusflow.timer.ui.TimerScreen
import com.focusflow.timer.ui.theme.FocusFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusFlowApp()
        }
    }
}

private data class TabItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun FocusFlowApp(vm: TimerViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val tabs = remember {
        listOf(
            TabItem("计时", Icons.Filled.Timer),
            TabItem("统计", Icons.Filled.BarChart),
            TabItem("设置", Icons.Filled.Settings)
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    FocusFlowTheme(
        themeMode = state.settings.themeMode,
        dynamicColor = state.settings.dynamicColor
    ) {
        // Android 13+ 需要用户授权才能弹出完成提醒
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { }
        )
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        KeepScreenOn(enabled = state.isRunning && state.settings.keepScreenOn)

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
                    0 -> TimerScreen(state = state, vm = vm)
                    1 -> StatsScreen(state = state, vm = vm)
                    else -> SettingsScreen(state = state, vm = vm)
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
