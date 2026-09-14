package com.focusflow.timer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focusflow.timer.data.DayCount
import com.focusflow.timer.timer.TimerUiState
import com.focusflow.timer.timer.TimerViewModel
import java.time.LocalDate

private val WEEKDAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

private fun weekdayLabel(date: LocalDate): String = WEEKDAY_LABELS[date.dayOfWeek.value - 1]

@Composable
fun StatsScreen(
    state: TimerUiState,
    vm: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val stats = state.stats
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "统计",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("今日", stats.todayCount.toString(), "个专注", Modifier.weight(1f))
            StatCard("连续", stats.streak.toString(), "天", Modifier.weight(1f))
            StatCard("累计", stats.totalCount.toString(), "个专注", Modifier.weight(1f))
        }

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "最近 7 天",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(18.dp))
                WeekChart(stats.recentDays)
            }
        }

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "什么是番茄工作法？",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "专注 25 分钟算一个「番茄」，然后休息 5 分钟；每完成 4 个番茄，" +
                        "进行一次 15 分钟的长休息。你也可以在设置里调整这些时长。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedButton(
            onClick = { confirmReset = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("清除统计数据")
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("清除统计数据？") },
            text = { Text("今日、连续天数和累计专注记录都会被清零，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearStats()
                    confirmReset = false
                }) { Text("确定清除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeekChart(days: List<DayCount>) {
    val maxCount = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val barMaxHeight = 120.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val fraction = day.count.toFloat() / maxCount.toFloat()
            val barHeight = (barMaxHeight * fraction).coerceAtLeast(6.dp)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (day.count > 0) day.count.toString() else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (day.count > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = weekdayLabel(day.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
