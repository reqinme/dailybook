package com.dailybook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailybook.app.ui.LedgerScreen
import com.dailybook.app.ui.SettingsScreen
import com.dailybook.app.ui.StatsScreen
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
fun DailyBookApp(vm: MainViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val themeMode by vm.settings.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by vm.settings.dynamicColor.collectAsStateWithLifecycle()

    val tabs = remember {
        listOf(
            Tab("记账", Icons.Filled.AccountBalanceWallet),
            Tab("待办", Icons.Filled.Checklist),
            Tab("统计", Icons.Filled.BarChart),
            Tab("设置", Icons.Filled.Settings)
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    DailyBookTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
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
                    2 -> StatsScreen(state = state, vm = vm)
                    else -> SettingsScreen(state = state, vm = vm)
                }
            }
        }
    }
}
