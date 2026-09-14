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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focusflow.timer.timer.Phase
import com.focusflow.timer.timer.TimerUiState
import com.focusflow.timer.timer.TimerViewModel

@Composable
fun TimerScreen(
    state: TimerUiState,
    vm: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val accent = when (state.phase) {
        Phase.FOCUS -> MaterialTheme.colorScheme.primary
        Phase.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
        Phase.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
    }
    val track = MaterialTheme.colorScheme.surfaceVariant
    val longEvery = state.settings.longBreakEvery
    val cyclePosition = (state.focusInCycle % longEvery) + 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 阶段选择
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Phase.entries.forEach { phase ->
                FilterChip(
                    selected = state.phase == phase,
                    onClick = { vm.selectPhase(phase) },
                    label = {
                        Text("${phase.label} ${state.settings.durationMillisFor(phase) / 60_000}′")
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        RingTimer(
            progress = state.progress,
            timeText = state.timeText,
            phaseLabel = if (state.isRunning) state.phase.label + "中" else state.phase.label,
            hint = "第 $cyclePosition / $longEvery 个番茄",
            accent = accent,
            trackColor = track,
            modifier = Modifier.size(268.dp)
        )

        Spacer(Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            FilledTonalIconButton(
                onClick = { vm.reset() },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "重置")
            }

            Button(
                onClick = { vm.toggle() },
                modifier = Modifier
                    .height(58.dp)
                    .width(154.dp),
                shape = RoundedCornerShape(29.dp)
            ) {
                Icon(
                    imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (state.isRunning) "暂停" else "开始",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            FilledTonalIconButton(
                onClick = { vm.skip() },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = "跳过")
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "今日已完成 ${state.stats.todayCount} 个专注 · 累计 ${state.stats.totalCount} 个",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (state.settings.autoStartNext) "结束后自动开始下一阶段" else "结束后需手动开始下一阶段",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }
}
