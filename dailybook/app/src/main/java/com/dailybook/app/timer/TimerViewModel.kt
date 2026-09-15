package com.dailybook.app.timer

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailybook.app.data.AppSettings
import com.dailybook.app.data.DailyRepository
import com.dailybook.app.data.FocusRepository
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.notify.Notifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 计时模式。
 *
 * - [POMODORO]：原来的番茄钟，倒数计时，阶段结束后自动进入下一阶段。
 * - [STOPWATCH]：正计时，从 0 往上走，没有阶段切换、也不会自动结束，由用户点「完成」收尾。
 */
enum class TimerMode { POMODORO, STOPWATCH }

data class TimerUiState(
    val phase: Phase = Phase.FOCUS,
    val remainingMillis: Long = 25 * 60_000L,
    val totalMillis: Long = 25 * 60_000L,
    val isRunning: Boolean = false,
    /** 本轮循环里已完成的专注次数 */
    val focusInCycle: Int = 0,
    val settings: AppSettings = AppSettings(),
    /** 当前选中的专注目标（待办标题），空表示未选择 */
    val focusTaskTitle: String = "",
    /** 当前计时模式 */
    val mode: TimerMode = TimerMode.POMODORO,
    /** 正计时已经过去的毫秒数；番茄钟模式下恒为 0，完全不参与原有逻辑 */
    val elapsedMillis: Long = 0L
) {
    val isStopwatch: Boolean get() = mode == TimerMode.STOPWATCH

    val progress: Float
        get() = if (isStopwatch) {
            // 正计时没有「总量」，圆环改成「一分钟走一圈」，保留进度反馈
            (elapsedMillis % 60_000L).toFloat() / 60_000f
        } else {
            if (totalMillis <= 0L) 0f
            else ((totalMillis - remainingMillis).toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
        }

    val timeText: String
        get() {
            val totalSeconds = if (isStopwatch) elapsedMillis / 1000L
            else (remainingMillis + 999L) / 1000L
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

    /** 番茄钟：当前阶段的结束时刻（elapsedRealtime） */
    private var endAt = 0L

    /** 正计时：暂停前已经累计的毫秒数 */
    private var stopwatchBaseMillis = 0L

    /** 正计时：本次开始计时的时刻（elapsedRealtime），没有在跑时为 0 */
    private var stopwatchStartedAt = 0L

    /**
     * 番茄钟：当前阶段**真正在跑**的毫秒数累计（暂停的时段不计入）。
     *
     * 写专注记录时用它，而不是「设置里填的时长」：设置值只说明这一阶段**应该**跑多久，
     * 一旦阶段长度改过（暂停时把专注时长从 25 分钟调成 10 分钟），用设置值就会记出一条
     * 和实际不符的记录 —— 一个只跑了 10 分钟的阶段被记成 25 分钟，还会进「今日专注」和统计。
     */
    private var phaseRunMillis = 0L

    /**
     * 番茄钟：当前这一小段连续运行是从哪个时刻开始的（[SystemClock.elapsedRealtime]）；
     * 没在跑时为 0。暂停时把这一段累加进 [phaseRunMillis] 再清零，所以暂停时间天然被排除。
     */
    private var phaseRunStartedAt = 0L

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
        val now = SystemClock.elapsedRealtime()
        when (_state.value.mode) {
            TimerMode.POMODORO -> {
                val remaining = _state.value.remainingMillis
                if (remaining <= 0L) return
                endAt = now + remaining
                // 这一段连续运行的起点：暂停过的那些时间不会被算进来
                phaseRunStartedAt = now
            }
            TimerMode.STOPWATCH -> {
                // 从暂停处接着往上走
                stopwatchBaseMillis = _state.value.elapsedMillis
                stopwatchStartedAt = now
            }
        }
        _state.update { it.copy(isRunning = true) }
        startTicker()
    }

    fun pause() {
        val snapshot = _state.value
        if (!snapshot.isRunning) return
        stopTicker()
        val now = SystemClock.elapsedRealtime()
        if (snapshot.mode == TimerMode.STOPWATCH) {
            stopwatchBaseMillis = stopwatchMillisAt(now)
            stopwatchStartedAt = 0L
            _state.update { it.copy(isRunning = false, elapsedMillis = stopwatchBaseMillis) }
        } else {
            // 先把这一段的运行时长落进累计值，再冻结倒计时 —— 两边的「已过去多久」永远一致
            phaseRunMillis = phaseRunMillisAt(now)
            phaseRunStartedAt = 0L
            val remaining = (endAt - now).coerceAtLeast(0L)
            _state.update { it.copy(isRunning = false, remainingMillis = remaining) }
        }
    }

    /**
     * 重置：番茄钟回到当前阶段开头，正计时回到 00:00。
     *
     * 正计时的重置**不再静默丢掉这一段**：它和番茄钟的重置一样，把已经跑过的时间记成一条
     * 「中断」的专注记录（不足 1 分钟不记）。理由：这个按钮紧挨着「完成」，
     * 两种模式下同一个按钮却是两种后果（番茄钟会记录、正计时什么都不留）本来就不一致；
     * 而这里的取舍是「宁可多留一条可以自己删的记录，也不要悄悄蒸发掉一小时的专注」。
     * 想主动丢弃这一段，用户可以在统计里删 —— 数据丢了是找不回来的。
     */
    fun reset() {
        val snapshot = _state.value
        val abandoned = abandonedFocusMillis(snapshot)
        stopTicker()
        if (snapshot.mode == TimerMode.STOPWATCH) {
            // 暂停中时 snapshot.elapsedMillis 就是已经累计的时长；在跑时按现在算
            val elapsed = if (snapshot.isRunning) {
                stopwatchMillisAt(SystemClock.elapsedRealtime())
            } else {
                snapshot.elapsedMillis
            }
            stopwatchBaseMillis = 0L
            stopwatchStartedAt = 0L
            recordInterruptedFocus(elapsed)
            _state.update { it.copy(isRunning = false, elapsedMillis = 0L) }
        } else {
            recordInterruptedFocus(abandoned)
            clearPhaseRun()
            _state.update { it.copy(isRunning = false, remainingMillis = it.totalMillis) }
        }
    }

    /** 手动切换阶段（会停止当前计时） */
    fun selectPhase(phase: Phase) {
        val abandoned = abandonedFocusMillis(_state.value)
        stopTicker()
        recordInterruptedFocus(abandoned)
        clearPhaseRun()
        val total = _state.value.settings.durationMillisFor(phase)
        _state.update {
            it.copy(phase = phase, totalMillis = total, remainingMillis = total, isRunning = false)
        }
    }

    /** 跳过当前阶段；专注阶段中途跳过会记一条「中断」 */
    fun skip() {
        val abandoned = abandonedFocusMillis(_state.value)
        stopTicker()
        recordInterruptedFocus(abandoned)
        clearPhaseRun()
        viewModelScope.launch { advance(countFocus = false, allowAutoStart = false) }
    }

    // ---- 计时模式 ----

    /** 切换计时模式：先停表并把当前这一轮重置掉，再换模式 */
    fun setMode(mode: TimerMode) {
        if (_state.value.mode == mode) return
        // 离开番茄钟时，专注阶段已投入的时间记成「中断」（休息阶段本来就是 0）。
        //
        // 正计时这边仍然是**只有「完成」和「重置」会写记录**：切换模式等于用户主动放弃这一轮，
        // 这里不补记（和以前一样）。这是一条会丢掉正计时时长的路，但目前是刻意保留的行为 ——
        // 换成「切模式也记一条」会把「误按切换」变成一条假数据，两种取舍都不完美。
        val abandoned = abandonedFocusMillis(_state.value)
        stopTicker()
        stopwatchBaseMillis = 0L
        stopwatchStartedAt = 0L
        endAt = 0L
        clearPhaseRun()
        recordInterruptedFocus(abandoned)
        _state.update { current ->
            if (mode == TimerMode.STOPWATCH) {
                current.copy(mode = mode, isRunning = false, elapsedMillis = 0L)
            } else {
                // 回到番茄钟时按当前阶段时长重新铺满
                val total = current.settings.durationMillisFor(current.phase)
                current.copy(
                    mode = mode,
                    isRunning = false,
                    totalMillis = total,
                    remainingMillis = total,
                    elapsedMillis = 0L
                )
            }
        }
    }

    /**
     * 正计时「完成」：把已经过去的时间记成一条专注记录，然后回到 00:00。
     * 不足 1 分钟不记录（和番茄钟的中断记录规则一致）。
     * 正计时没有「阶段」，所以不会触发通知 / 震动，也不改番茄钟的阶段与计数。
     */
    fun finishStopwatch() {
        val snapshot = _state.value
        if (snapshot.mode != TimerMode.STOPWATCH) return
        val elapsed = if (snapshot.isRunning) {
            stopwatchMillisAt(SystemClock.elapsedRealtime())
        } else {
            snapshot.elapsedMillis
        }
        stopTicker()
        stopwatchBaseMillis = 0L
        stopwatchStartedAt = 0L
        recordFocus(elapsed, interrupted = false)
        _state.update { it.copy(isRunning = false, elapsedMillis = 0L) }
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

    /** 正计时到 [now] 为止累计的毫秒数 */
    private fun stopwatchMillisAt(now: Long): Long =
        if (stopwatchStartedAt == 0L) stopwatchBaseMillis
        else stopwatchBaseMillis + (now - stopwatchStartedAt).coerceAtLeast(0L)

    /**
     * 番茄钟当前阶段到 [now] 为止**真正跑过**的毫秒数（暂停的时段不计入）。
     * 写专注记录（正常走完 / 中断）时都用它，而不是「设置里填的时长」——
     * 设置值只说明原本该跑多久，中途改过设置、暂停过，它就和实际对不上。
     */
    private fun phaseRunMillisAt(now: Long): Long =
        if (phaseRunStartedAt == 0L) phaseRunMillis
        else phaseRunMillis + (now - phaseRunStartedAt).coerceAtLeast(0L)

    /** 换阶段 / 换模式 / 重置时，把「当前阶段跑了多久」清零 */
    private fun clearPhaseRun() {
        phaseRunMillis = 0L
        phaseRunStartedAt = 0L
    }

    /**
     * 停止 / 重置 / 跳过时该记多少毫秒：只认番茄钟的**专注**阶段（休息阶段永远不记中断），
     * 其余一律 0；时长取正向累计的 [phaseRunMillisAt]，和「阶段正常走完」那条记录同一个口径。
     *
     * 阶段正常走完的那一笔由 [advance] 记录，并且记完就把累计清零，
     * 所以这里永远不会把同一段专注记两遍。
     */
    private fun abandonedFocusMillis(snapshot: TimerUiState): Long {
        if (snapshot.mode != TimerMode.POMODORO) return 0L
        if (snapshot.phase != Phase.FOCUS) return 0L
        return phaseRunMillisAt(SystemClock.elapsedRealtime())
    }

    /**
     * 中断记录：专注阶段没走完就被停止 / 重置 / 跳过时，把已经专注的那部分记下来。
     * 不足 1 分钟不记（否则误点一下就会污染统计）；阶段正常走完的路径在 [advance] 里记录，
     * 走的是完全不同的一支，所以不会重复记录。
     * 正计时的「重置」也走这里（见 [reset]）：同一个按钮在两种模式下都要留下记录，别悄悄丢时间。
     */
    private fun recordInterruptedFocus(elapsedMillis: Long) =
        recordFocus(elapsedMillis, interrupted = true)

    /**
     * 写一条专注记录。
     * 不足 1 分钟不写：番茄钟的中断记录、正计时的完成 / 重置都用这一条规则，
     * 免得误点一下就在统计里冒出 0 分钟的记录。
     */
    private fun recordFocus(elapsedMillis: Long, interrupted: Boolean) {
        val minutes = (elapsedMillis / 60_000L).toInt()
        if (minutes < 1) return
        val taskTitle = settingsStore.focusTaskTitle.value
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            dailyRepo.recordFocusSession(
                startedAtMillis = now - elapsedMillis,
                endedAtMillis = now,
                minutes = minutes,
                taskTitle = taskTitle,
                interrupted = interrupted
            )
        }
    }

    private fun startTicker() {
        stopTicker()
        ticker = viewModelScope.launch {
            while (isActive) {
                if (_state.value.mode == TimerMode.STOPWATCH) {
                    // 正计时只往上走：不判结束、不切阶段、不发通知
                    val elapsed = stopwatchMillisAt(SystemClock.elapsedRealtime())
                    _state.update { it.copy(elapsedMillis = elapsed) }
                } else {
                    val remaining = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                    _state.update { it.copy(remainingMillis = remaining) }
                    if (remaining <= 0L) {
                        val keepRunning = advance(countFocus = true, allowAutoStart = true)
                        if (!keepRunning) break
                    }
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

        // 这一阶段**真正跑了多久**：暂停的时间不算，也不再读「设置里的时长」
        // （设置值只说明原本该跑多久；计时途中改过设置的话，它会和实际跑的长度对不上）。
        // 现在这一秒可能还在跑（倒计时刚归零、ticker 还没收工），所以按「运行中」算到此刻为止。
        val runMillis = phaseRunMillisAt(SystemClock.elapsedRealtime())
        clearPhaseRun()

        if (countFocus && finished == Phase.FOCUS) {
            // 写入专注记录表（统计页的「今日/连续/累计」由这张表派生）。
            // startedAtMillis 也从实际运行时长倒推：这样这一行的「起止跨度」和「分钟数」永远自洽 ——
            // 以前写的是 now - 设置里的时长，暂停过 / 改过设置时，行上显示的跨度和分钟数就对不上。
            recordFocus(runMillis, interrupted = false)
        }

        val finishedFocus = finished == Phase.FOCUS
        val cycleDone = snapshot.focusInCycle + if (finishedFocus) 1 else 0
        val next = if (finishedFocus) {
            if (cycleDone % snapshot.settings.longBreakEvery == 0) Phase.LONG_BREAK else Phase.SHORT_BREAK
        } else {
            Phase.FOCUS
        }

        if (countFocus) {
            val lang = settingsStore.lang.value
            val title = if (finishedFocus) AppStrings.focusDoneTitle(lang) else AppStrings.breakDoneTitle(lang)
            val text = if (finishedFocus) {
                if (next == Phase.LONG_BREAK) AppStrings.focusDoneLongBreak(lang)
                else AppStrings.focusDoneShortBreak(lang)
            } else {
                AppStrings.breakDoneBackToFocus(lang)
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
            val startAt = SystemClock.elapsedRealtime()
            endAt = startAt + total
            // 新阶段立刻就在跑：运行计时的起点要一起跟上，否则这一整段会被算成 0
            phaseRunStartedAt = startAt
        }
        return autoStart
    }
}
