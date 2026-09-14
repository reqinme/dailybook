package com.dailybook.app.i18n

/**
 * 通用组件与计时页文案。
 *
 * 约定同 [AppStrings]：每个函数第一个参数都是 [Lang]，四语文案在编译期必须给全（见 [pick] / [pickf]）。
 * 分类名、账户名、待办标题等**数据**不进这里，界面这里只放组件自带的固定文案。
 */
object CommonStrings {

    // ---- 月份切换条（Components.MonthSwitcher）----
    fun prevMonth(lang: Lang) = pick(lang, "上个月", "上個月", "Previous month", "前の月")
    fun nextMonth(lang: Lang) = pick(lang, "下个月", "下個月", "Next month", "次の月")
    fun backToThisMonth(lang: Lang) = pick(lang, "回本月", "回到本月", "This month", "今月へ")

    // ---- 搜索框（Components.SearchField）----
    fun clearSearch(lang: Lang) = pick(lang, "清除搜索", "清除搜尋", "Clear search", "検索をクリア")

    // ---- 计时页：阶段名 ----
    // 阶段名本身复用 AppStrings.phaseFocus / phaseShortBreak / phaseLongBreak，
    // 这里只放「进行中」的说法（原来由 label + "中" 拼出来）。
    fun focusRunning(lang: Lang) = pick(lang, "专注中", "專注中", "Focusing", "集中中")
    fun shortBreakRunning(lang: Lang) = pick(lang, "短休息中", "短休息中", "On a short break", "小休憩中")
    fun longBreakRunning(lang: Lang) = pick(lang, "长休息中", "長休息中", "On a long break", "長休憩中")

    // ---- 计时页：目标与按钮 ----
    /** 「🎯 当前目标：<待办标题>」，标题是数据，不改 */
    fun focusGoalLabel(lang: Lang, title: String) = pickf(
        lang,
        "🎯 当前目标：%s", "🎯 目前目標：%s",
        "🎯 Current goal: %s", "🎯 現在の目標：%s",
        title
    )

    fun clearFocusGoal(lang: Lang) = pick(lang, "取消专注目标", "取消專注目標", "Clear goal", "目標を解除")
    fun reset(lang: Lang) = pick(lang, "重置", "重設", "Reset", "リセット")
    fun start(lang: Lang) = pick(lang, "开始", "開始", "Start", "開始")
    fun pause(lang: Lang) = pick(lang, "暂停", "暫停", "Pause", "一時停止")
    fun skip(lang: Lang) = pick(lang, "跳过", "跳過", "Skip", "スキップ")

    // ---- 计时页：统计与提示 ----
    /** 「第 2 / 4 个番茄」 */
    fun pomodoroProgress(lang: Lang, index: Int, total: Int) = pickf(
        lang,
        "第 %d / %d 个番茄", "第 %d / %d 個番茄",
        "Pomodoro %d of %d", "%d / %d 個目のポモドーロ",
        index, total
    )

    /** 「今日 3 个专注 · 75 分钟 · 累计 12 个」 */
    fun todayFocusSummary(lang: Lang, sessions: Int, minutes: Int, total: Int) = pickf(
        lang,
        "今日 %d 个专注 · %d 分钟 · 累计 %d 个",
        "今日 %d 個專注 · %d 分鐘 · 累計 %d 個",
        "Today: %d sessions · %d min · %d total",
        "今日 %d 回・%d 分・累計 %d 回",
        sessions, minutes, total
    )

    fun autoStartNext(lang: Lang) = pick(
        lang,
        "结束后自动开始下一阶段", "結束後自動開始下一階段",
        "Next phase starts automatically", "終了後、次のフェーズを自動で開始"
    )

    fun manualStartNext(lang: Lang) = pick(
        lang,
        "结束后需手动开始下一阶段", "結束後需手動開始下一階段",
        "Next phase needs a manual start", "終了後、次のフェーズは手動で開始"
    )

    // ---- 计时页：计时模式（番茄钟 / 正计时）----

    /** 模式胶囊：番茄钟 */
    fun timerModePomodoro(lang: Lang) = pick(
        lang, "番茄钟", "番茄鐘", "Pomodoro", "ポモドーロ"
    )

    /** 模式胶囊：正计时 */
    fun timerModeStopwatch(lang: Lang) = pick(
        lang, "正计时", "正計時", "Stopwatch", "ストップウォッチ"
    )

    /** 圆环中间的「进行中」文案（正计时） */
    fun stopwatchRunning(lang: Lang) = pick(
        lang, "正计时中", "正計時中", "Counting up", "計測中"
    )

    /** 圆环下方的短提示：正计时不自动结束 */
    fun stopwatchHint(lang: Lang) = pick(
        lang,
        "不自动结束，点「完成」记一次专注",
        "不會自動結束，點「完成」記一次專注",
        "No auto stop — tap Finish to log a session",
        "自動では終わりません。「完了」で集中を記録"
    )

    /** 正计时页面底部的说明：不足 1 分钟不记录 */
    fun stopwatchNote(lang: Lang) = pick(
        lang,
        "「完成」按已过去的整分钟记一次专注；不足 1 分钟不记录。",
        "「完成」按已過去的整分鐘記一次專注；不足 1 分鐘不記錄。",
        "Finish logs one focus session using the whole minutes elapsed; under 1 minute is not recorded.",
        "「完了」は経過した整数分を集中として記録します。1分未満は記録しません。"
    )

    /** 正计时的结束按钮 */
    fun finish(lang: Lang) = pick(
        lang, "完成", "完成", "Finish", "完了"
    )
}
