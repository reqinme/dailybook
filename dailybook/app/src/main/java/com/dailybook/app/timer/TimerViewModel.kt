package com.dailybook.app.timer

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailybook.app.data.AppSettings
import com.dailybook.app.data.DailyRepository
import com.dailybook.app.data.FocusRepository
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.notify.Notifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerUiState(
    val phase: Phase = Phase.FOCUS,
    val remainingMillis: Long = 25 * 60_000L,
    val totalMillis: Long = 25 * 60_000L,
    val isRunning: Boolean = false,
    /** 本轮循环里已完成的专注次数 */
    val focusInCycle: Int = 0,
    val settings: AppSettings = AppSettings(),
    /** 当前选中的专注目标（待办标题），空表示未选择 */
    val focusTaskTitle: String = ""
) {
    val progress: Float
        get() = if (totalMillis <= 0L) 0f
        else ((totalMillis - remainingMillis).toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)

    val timeText: String
        get() {
            val totalSeconds = (remainingMillis + 999L) / 1000L
            return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
        }
}

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FocusRepository(application)
    private val dailyRepo = DailyRepository(application)
    private val settingsStore = SettingsStore.get(application)
    private val notifier = Notifier(application)

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private var ticker: Job? = null
    private var endAt = 0L

    init {
        viewModelScope.launch {
            repo.settings.collect { newSettings ->
                _state.update { current ->
                    val running = current.isRunning
                    val untouched = current.remainingMillis == current.totalMillis
                    val oldDuration = current.settings.durationMillisFor(current.phase)
                    val newDuration = newSettings.durationMillisFor(current.phase)
                    // 计时中不打断剩余时间；空闲且时长变化（或尚未开始）时同步刷新
                    if (!running && (untouched || oldDuration != newDuration)) {
                        current.copy(
                            settings = newSettings,
                            totalMillis = newDuration,
                            remainingMillis = newDuration
                        )
                    } else {
                        current.copy(settings = newSettings)
                    }
                }
            }
        }
        viewModelScope.launch {
            settingsStore.focusTaskTitle.collect { title ->
                _state.update { it.copy(focusTaskTitle = title) }
            }
        }
    }

    fun toggle() {
        if (_state.value.isRunning) pause() else start()
    }

    fun start() {
        if (_state.value.isRunning) return
        val remaining = _state.value.remainingMillis
        if (remaining <= 0L) return
        endAt = SystemClock.elapsedRealtime() + remaining
        _state.update { it.copy(isRunning = true) }
        startTicker()
    }

    fun pause() {
        if (!_state.value.isRunning) return
        stopTicker()
        val remaining = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        _state.update { it.copy(isRunning = false, remainingMillis = remaining) }
    }

    fun reset() {
        stopTicker()
        _state.update { it.copy(isRunning = false, remainingMillis = it.totalMillis) }
    }

    /** 手动切换阶段（会停止当前计时） */
    fun selectPhase(phase: Phase) {
        stopTicker()
        val total = _state.value.settings.durationMillisFor(phase)
        _state.update {
            it.copy(phase = phase, totalMillis = total, remainingMillis = total, isRunning = false)
        }
    }

    /** 跳过当前阶段，不计入统计 */
    fun skip() {
        stopTicker()
        viewModelScope.launch { advance(countFocus = false, allowAutoStart = false) }
    }

    // ---- 设置写入接口 ----

    fun setFocusMinutes(v: Int) { viewModelScope.launch { repo.setFocusMinutes(v) } }

    fun setShortBreakMinutes(v: Int) { viewModelScope.launch { repo.setShortBreakMinutes(v) } }

    fun setLongBreakMinutes(v: Int) { viewModelScope.launch { repo.setLongBreakMinutes(v) } }

    fun setLongBreakEvery(v: Int) { viewModelScope.launch { repo.setLongBreakEvery(v) } }

    fun setAutoStart(v: Boolean) { viewModelScope.launch { repo.setAutoStart(v) } }

    fun setVibrate(v: Boolean) { viewModelScope.launch { repo.setVibrate(v) } }

    fun setKeepScreenOn(v: Boolean) { viewModelScope.launch { repo.setKeepScreenOn(v) } }

    fun clearFocusTask() = settingsStore.clearFocusTask()

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun startTicker() {
        stopTicker()
        ticker = viewModelScope.launch {
            while (isActive) {
                val remaining = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                _state.update { it.copy(remainingMillis = remaining) }
                if (remaining <= 0L) {
                    val keepRunning = advance(countFocus = true, allowAutoStart = true)
                    if (!keepRunning) break
                }
                delay(200L)
            }
        }
    }

    /**
     * 进入下一个阶段。
     * @return 是否立刻自动开始下一阶段
     */
    private suspend fun advance(countFocus: Boolean, allowAutoStart: Boolean): Boolean {
        val snapshot = _state.value
        val finished = snapshot.phase

        if (countFocus && finished == Phase.FOCUS) {
            // 写入专注记录表（统计页的「今日/连续/累计」由这张表派生）
            val durationMillis = snapshot.settings.durationMillisFor(Phase.FOCUS)
            val now = System.currentTimeMillis()
            dailyRepo.recordFocusSession(
                startedAtMillis = now - durationMillis,
                endedAtMillis = now,
                minutes = (durationMillis / 60_000L).toInt(),
                taskTitle = settingsStore.focusTaskTitle.value
            )
        }

        val finishedFocus = finished == Phase.FOCUS
        val cycleDone = snapshot.focusInCycle + if (finishedFocus) 1 else 0
        val next = if (finishedFocus) {
            if (cycleDone % snapshot.settings.longBreakEvery == 0) Phase.LONG_BREAK else Phase.SHORT_BREAK
        } else {
            Phase.FOCUS
        }

        if (countFocus) {
            val title = if (finishedFocus) "专注完成 🍅" else "休息结束"
            val text = if (finishedFocus) {
                if (next == Phase.LONG_BREAK) "很棒！来一次长休息吧" else "喝口水，短暂休息一下"
            } else {
                "回到专注，继续加油"
            }
            notifier.notifyPhaseFinished(title, text)
            if (snapshot.settings.vibrate) notifier.vibrate()
        }

        val total = snapshot.settings.durationMillisFor(next)
        val autoStart = allowAutoStart && snapshot.settings.autoStartNext

        _state.update {
            it.copy(
                phase = next,
                totalMillis = total,
                remainingMillis = total,
                focusInCycle = cycleDone,
                isRunning = autoStart
            )
        }

        if (autoStart) {
            endAt = SystemClock.elapsedRealtime() + total
        }
        return autoStart
    }
}
