package com.focusflow.timer.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focusflow.timer.timer.TimerUiState
import com.focusflow.timer.timer.TimerViewModel
import com.focusflow.timer.ui.theme.ThemeMode
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: TimerUiState,
    vm: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val settings = state.settings

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        SectionCard("时长") {
            DurationSlider(
                label = "专注时长",
                value = settings.focusMinutes,
                range = 5..90,
                onChange = { vm.setFocusMinutes(it) }
            )
            DurationSlider(
                label = "短休息",
                value = settings.shortBreakMinutes,
                range = 1..30,
                onChange = { vm.setShortBreakMinutes(it) }
            )
            DurationSlider(
                label = "长休息",
                value = settings.longBreakMinutes,
                range = 5..45,
                onChange = { vm.setLongBreakMinutes(it) }
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
                        onClick = { vm.setLongBreakEvery((settings.longBreakEvery - 1).coerceAtLeast(2)) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "减少", modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "每 ${settings.longBreakEvery} 个",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    FilledTonalIconButton(
                        onClick = { vm.setLongBreakEvery((settings.longBreakEvery + 1).coerceAtMost(8)) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "增加", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        SectionCard("提醒与行为") {
            SwitchRow("阶段结束震动提醒", settings.vibrate) { vm.setVibrate(it) }
            SwitchRow("自动开始下一阶段", settings.autoStartNext) { vm.setAutoStart(it) }
            SwitchRow("计时中保持屏幕常亮", settings.keepScreenOn) { vm.setKeepScreenOn(it) }
        }

        SectionCard("外观") {
            Text("主题", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChip("跟随系统", ThemeMode.SYSTEM, settings.themeMode) { vm.setThemeMode(it) }
                ThemeChip("浅色", ThemeMode.LIGHT, settings.themeMode) { vm.setThemeMode(it) }
                ThemeChip("深色", ThemeMode.DARK, settings.themeMode) { vm.setThemeMode(it) }
            }
            Spacer(Modifier.height(10.dp))
            SwitchRow("动态取色（Android 12+）", settings.dynamicColor) { vm.setDynamicColor(it) }
        }

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "专注时钟 v1.0",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "使用 Kotlin + Jetpack Compose 构建，数据仅保存在本机，不联网、不上传。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
