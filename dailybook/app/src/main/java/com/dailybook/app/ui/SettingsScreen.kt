package com.dailybook.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.timer.TimerUiState
import com.dailybook.app.timer.TimerViewModel
import com.dailybook.app.ui.theme.ThemeMode
import kotlin.math.roundToInt

private enum class ClearTarget(val label: String, val message: String) {
    TRANSACTIONS("清除所有记账记录", "所有收支记录都会被删除，且无法恢复。"),
    TODOS("清除所有待办", "所有待办事项都会被删除，且无法恢复。"),
    FOCUS_STATS("清除专注统计", "专注次数和连续天数会被清零，且无法恢复。"),
    EVERYTHING("清空全部数据", "记账记录、待办和专注统计都会被删除，且无法恢复。")
}

@Composable
fun SettingsScreen(
    state: UiState,
    timerState: TimerUiState,
    vm: MainViewModel,
    timerVm: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val themeMode by vm.settings.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by vm.settings.dynamicColor.collectAsStateWithLifecycle()
    var clearing by remember { mutableStateOf<ClearTarget?>(null) }

    val focusSettings = timerState.settings

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

        SettingCard("专注计时") {
            DurationSlider(
                label = "专注时长",
                value = focusSettings.focusMinutes,
                range = 5..90,
                onChange = { timerVm.setFocusMinutes(it) }
            )
            DurationSlider(
                label = "短休息",
                value = focusSettings.shortBreakMinutes,
                range = 1..30,
                onChange = { timerVm.setShortBreakMinutes(it) }
            )
            DurationSlider(
                label = "长休息",
                value = focusSettings.longBreakMinutes,
                range = 5..45,
                onChange = { timerVm.setLongBreakMinutes(it) }
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("长休息间隔", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = {
                            timerVm.setLongBreakEvery(
                                (focusSettings.longBreakEvery - 1).coerceAtLeast(2)
                            )
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Filled.Remove,
                            contentDescription = "减少",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "每 ${focusSettings.longBreakEvery} 个",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    FilledTonalIconButton(
                        onClick = {
                            timerVm.setLongBreakEvery(
                                (focusSettings.longBreakEvery + 1).coerceAtMost(8)
                            )
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "增加",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            SwitchRow("阶段结束震动提醒", focusSettings.vibrate) { timerVm.setVibrate(it) }
            SwitchRow("自动开始下一阶段", focusSettings.autoStartNext) { timerVm.setAutoStart(it) }
            SwitchRow("计时中保持屏幕常亮", focusSettings.keepScreenOn) { timerVm.setKeepScreenOn(it) }
        }

        SettingCard("数据") {
            Text(
                text = "本月 ${state.monthCount} 笔记录 · ${state.pendingCount + state.doneCount} 条待办 · " +
                    "今日 ${timerState.stats.todayCount} 个专注",
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
                onClick = { clearing = ClearTarget.FOCUS_STATS },
                modifier = Modifier.fillMaxWidth()
            ) { Text(ClearTarget.FOCUS_STATS.label) }
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
                    text = "日常本 v1.1",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "记账 + 待办 + 专注计时，三合一。数据全部存在手机本地" +
                        "（记账/待办用 Room 数据库，专注记录用本地偏好存储），" +
                        "不联网、不上传。只有专注计时的通知和震动需要系统权限。",
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
                        ClearTarget.FOCUS_STATS -> timerVm.clearStats()
                        ClearTarget.EVERYTHING -> {
                            vm.clearAll()
                            timerVm.clearStats()
                        }
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
private fun DurationSlider(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${local.roundToInt()} 分钟",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            onValueChangeFinished = { onChange(local.roundToInt()) }
        )
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
