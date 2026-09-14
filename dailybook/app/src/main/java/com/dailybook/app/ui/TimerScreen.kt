package com.dailybook.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.dailybook.app.FocusStats
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.CommonStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.timer.Phase
import com.dailybook.app.timer.TimerUiState
import com.dailybook.app.timer.TimerViewModel

/** 阶段名（专注 / 短休息 / 长休息） */
private fun phaseName(lang: Lang, phase: Phase): String = when (phase) {
    Phase.FOCUS -> AppStrings.phaseFocus(lang)
    Phase.SHORT_BREAK -> AppStrings.phaseShortBreak(lang)
    Phase.LONG_BREAK -> AppStrings.phaseLongBreak(lang)
}

/** 进行中的阶段名（专注中 / 短休息中 / 长休息中） */
private fun runningPhaseName(lang: Lang, phase: Phase): String = when (phase) {
    Phase.FOCUS -> CommonStrings.focusRunning(lang)
    Phase.SHORT_BREAK -> CommonStrings.shortBreakRunning(lang)
    Phase.LONG_BREAK -> CommonStrings.longBreakRunning(lang)
}

/** 专注（番茄钟）页 */
@Composable
fun TimerScreen(
    state: TimerUiState,
    stats: FocusStats,
    vm: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val accent = when (state.phase) {
        Phase.FOCUS -> MaterialTheme.colorScheme.primary
        Phase.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
        Phase.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
    }
    val longEvery = state.settings.longBreakEvery
    val cyclePosition = (state.focusInCycle % longEvery) + 1

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 圆环按可用空间自适应：小屏缩小、平板放大，上下留出按钮空间
        val ringSize = min(min(maxWidth * 0.78f, maxHeight * 0.46f), 320.dp)
            .coerceAtLeast(160.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 阶段选择
            ChipFlow {
                Phase.entries.forEach { phase ->
                    FilterChip(
                        selected = state.phase == phase,
                        onClick = { vm.selectPhase(phase) },
                        label = {
                            Text("${phaseName(lang, phase)} ${state.settings.durationMillisFor(phase) / 60_000}′")
                        }
                    )
                }
            }

            if (state.focusTaskTitle.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = CommonStrings.focusGoalLabel(lang, state.focusTaskTitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { vm.clearFocusTask() }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = CommonStrings.clearFocusGoal(lang),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            RingTimer(
                progress = state.progress,
                timeText = state.timeText,
                phaseLabel = if (state.isRunning) {
                    runningPhaseName(lang, state.phase)
                } else {
                    phaseName(lang, state.phase)
                },
                hint = CommonStrings.pomodoroProgress(lang, cyclePosition, longEvery),
                accent = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(ringSize)
            )

            Spacer(Modifier.height(26.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { vm.reset() },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = CommonStrings.reset(lang))
                }

                Button(
                    onClick = { vm.toggle() },
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .width(148.dp),
                    shape = Shapes.pill
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.isRunning) CommonStrings.pause(lang) else CommonStrings.start(lang),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalIconButton(
                    onClick = { vm.skip() },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = CommonStrings.skip(lang))
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = CommonStrings.todayFocusSummary(
                    lang, stats.todayCount, stats.todayMinutes, stats.totalCount
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (state.settings.autoStartNext) {
                    CommonStrings.autoStartNext(lang)
                } else {
                    CommonStrings.manualStartNext(lang)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
