package com.dailybook.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.ui.theme.ThemeMode

private enum class ClearTarget(val label: String, val message: String) {
    TRANSACTIONS("清除所有记账记录", "所有收支记录都会被删除，且无法恢复。"),
    TODOS("清除所有待办", "所有待办事项都会被删除，且无法恢复。"),
    EVERYTHING("清空全部数据", "记账记录和待办都会被删除，且无法恢复。")
}

@Composable
fun SettingsScreen(
    state: UiState,
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val themeMode by vm.settings.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by vm.settings.dynamicColor.collectAsStateWithLifecycle()
    var clearing by remember { mutableStateOf<ClearTarget?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(14.dp))

        SettingCard("外观") {
            Text("主题", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChip("跟随系统", ThemeMode.SYSTEM, themeMode) { vm.setThemeMode(it) }
                ThemeChip("浅色", ThemeMode.LIGHT, themeMode) { vm.setThemeMode(it) }
                ThemeChip("深色", ThemeMode.DARK, themeMode) { vm.setThemeMode(it) }
            }
            Spacer(Modifier.height(12.dp))
            SwitchRow("动态取色（Android 12+）", dynamicColor) { vm.setDynamicColor(it) }
        }

        SettingCard("数据") {
            Text(
                text = "当前共 ${state.monthCount} 笔本月记录，${state.pendingCount + state.doneCount} 条待办",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { clearing = ClearTarget.TRANSACTIONS },
                modifier = Modifier.fillMaxWidth()
            ) { Text(ClearTarget.TRANSACTIONS.label) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { clearing = ClearTarget.TODOS },
                modifier = Modifier.fillMaxWidth()
            ) { Text(ClearTarget.TODOS.label) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { clearing = ClearTarget.EVERYTHING },
                modifier = Modifier.fillMaxWidth()
            ) { Text(ClearTarget.EVERYTHING.label) }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "日常本 v1.0",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "记账 + 待办二合一。所有数据都存在手机本地（Room 数据库），" +
                        "不联网、不上传、不需要任何权限。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }

    clearing?.let { target ->
        AlertDialog(
            onDismissRequest = { clearing = null },
            title = { Text("确认${target.label}？") },
            text = { Text(target.message) },
            confirmButton = {
                TextButton(onClick = {
                    when (target) {
                        ClearTarget.TRANSACTIONS -> vm.clearTransactions()
                        ClearTarget.TODOS -> vm.clearTodos()
                        ClearTarget.EVERYTHING -> vm.clearAll()
                    }
                    clearing = null
                }) { Text("确定清除") }
            },
            dismissButton = {
                TextButton(onClick = { clearing = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ThemeChip(
    label: String,
    mode: ThemeMode,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    FilterChip(
        selected = current == mode,
        onClick = { onSelect(mode) },
        label = { Text(label) }
    )
}
