package com.dailybook.app.data

import android.content.Context
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.ui.theme.ThemeMode
import com.dailybook.app.ui.theme.ThemePalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** 定期汇总的通知频率 */
enum class SummaryMode {
    OFF,
    WEEKLY,
    MONTHLY;

    fun label(lang: Lang): String = when (this) {
        OFF -> AppStrings.summaryOff(lang)
        WEEKLY -> AppStrings.summaryWeekly(lang)
        MONTHLY -> AppStrings.summaryMonthly(lang)
    }
}

/**
 * 轻量设置存储（SharedPreferences），全局单例。
 *
 * 主题、动态取色、月度预算、当前专注目标这类「少量标量状态」统一放这里。
 * 用单例（而不是每个 ViewModel 各建一个）是为了让多个 ViewModel 共享同一份
 * StateFlow，避免出现「A 改了设置在 B 那边读不到」的问题。
 */
class SettingsStore private constructor(context: Context) {
    private val prefs =
        context.getSharedPreferences("dailybook_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    /** 界面语言（App 内切换，不跟随系统） */
    private val _lang = MutableStateFlow(Lang.of(prefs.getString(KEY_LANG, null)))
    val lang: StateFlow<Lang> = _lang.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, false))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    /** 月度预算（分），0 表示未设置 */
    private val _monthlyBudgetCents = MutableStateFlow(prefs.getLong(KEY_BUDGET, 0L))
    val monthlyBudgetCents: StateFlow<Long> = _monthlyBudgetCents.asStateFlow()

    /** 分类预算：分类名 → 每月上限（分），只保存大于 0 的项 */
    private val _categoryBudgets = MutableStateFlow(loadCategoryBudgets())
    val categoryBudgets: StateFlow<Map<String, Long>> = _categoryBudgets.asStateFlow()

    /** 每晚记账提醒 */
    private val _ledgerReminderEnabled = MutableStateFlow(prefs.getBoolean(KEY_LEDGER_REMIND, false))
    val ledgerReminderEnabled: StateFlow<Boolean> = _ledgerReminderEnabled.asStateFlow()

    private val _ledgerReminderHour = MutableStateFlow(prefs.getInt(KEY_LEDGER_REMIND_HOUR, 21))
    val ledgerReminderHour: StateFlow<Int> = _ledgerReminderHour.asStateFlow()

    private val _ledgerReminderMinute = MutableStateFlow(prefs.getInt(KEY_LEDGER_REMIND_MINUTE, 0))
    val ledgerReminderMinute: StateFlow<Int> = _ledgerReminderMinute.asStateFlow()

    /** 每日专注目标（个），0 表示不设目标 */
    private val _focusGoal = MutableStateFlow(prefs.getInt(KEY_FOCUS_GOAL, 0))
    val focusGoal: StateFlow<Int> = _focusGoal.asStateFlow()

    /** 定期汇总：关 / 每周 / 每月 */
    private val _summaryMode = MutableStateFlow(
        runCatching { SummaryMode.valueOf(prefs.getString(KEY_SUMMARY, null) ?: SummaryMode.OFF.name) }
            .getOrDefault(SummaryMode.OFF)
    )
    val summaryMode: StateFlow<SummaryMode> = _summaryMode.asStateFlow()

    /** 预算预警：用到 80% 或超支时推一条通知 */
    private val _budgetAlert = MutableStateFlow(prefs.getBoolean(KEY_BUDGET_ALERT, true))
    val budgetAlert: StateFlow<Boolean> = _budgetAlert.asStateFlow()

    /** 当前选中的专注目标（某条待办） */
    private val _focusTaskId = MutableStateFlow(prefs.getLong(KEY_FOCUS_TASK_ID, NO_TASK))
    val focusTaskId: StateFlow<Long> = _focusTaskId.asStateFlow()

    private val _focusTaskTitle = MutableStateFlow(prefs.getString(KEY_FOCUS_TASK_TITLE, "").orEmpty())
    val focusTaskTitle: StateFlow<String> = _focusTaskTitle.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setLang(lang: Lang) {
        prefs.edit().putString(KEY_LANG, lang.tag).apply()
        _lang.value = lang
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC, enabled).apply()
        _dynamicColor.value = enabled
    }

    fun setMonthlyBudget(cents: Long) {
        val safe = cents.coerceAtLeast(0L)
        prefs.edit().putLong(KEY_BUDGET, safe).apply()
        _monthlyBudgetCents.value = safe
    }

    /** 设置某个分类的月度预算；传 0 或负数表示取消这个分类的预算 */
    fun setCategoryBudget(category: String, cents: Long) {
        val key = category.trim()
        if (key.isEmpty()) return
        val next = _categoryBudgets.value.toMutableMap()
        if (cents <= 0L) next.remove(key) else next[key] = cents
        _categoryBudgets.value = next
        prefs.edit().putString(KEY_CATEGORY_BUDGETS, JSONObject(next).toString()).apply()
    }

    fun clearCategoryBudgets() {
        _categoryBudgets.value = emptyMap()
        prefs.edit().remove(KEY_CATEGORY_BUDGETS).apply()
    }

    fun setLedgerReminder(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LEDGER_REMIND, enabled).apply()
        _ledgerReminderEnabled.value = enabled
    }

    fun setLedgerReminderTime(hour: Int, minute: Int) {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        prefs.edit()
            .putInt(KEY_LEDGER_REMIND_HOUR, h)
            .putInt(KEY_LEDGER_REMIND_MINUTE, m)
            .apply()
        _ledgerReminderHour.value = h
        _ledgerReminderMinute.value = m
    }

    fun setFocusGoal(count: Int) {
        val safe = count.coerceIn(0, 20)
        prefs.edit().putInt(KEY_FOCUS_GOAL, safe).apply()
        _focusGoal.value = safe
    }

    fun setSummaryMode(mode: SummaryMode) {
        prefs.edit().putString(KEY_SUMMARY, mode.name).apply()
        _summaryMode.value = mode
    }

    fun setBudgetAlert(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BUDGET_ALERT, enabled).apply()
        _budgetAlert.value = enabled
    }

    /** 配色方案（v1.8） */
    private val _palette = MutableStateFlow(loadPalette())
    val palette: StateFlow<ThemePalette> = _palette.asStateFlow()

    fun setPalette(value: ThemePalette) {
        prefs.edit().putString(KEY_PALETTE, value.name).apply()
        _palette.value = value
    }

    private fun loadPalette(): ThemePalette = runCatching {
        ThemePalette.valueOf(prefs.getString(KEY_PALETTE, null) ?: ThemePalette.TEAL.name)
    }.getOrDefault(ThemePalette.TEAL)

    /**
     * 自定义背景图：用户挑的一张图片，记住系统文件选择器授权过的 URI。
     * 空串 = 不用背景图；图片本身不复制进应用私有目录。
     */
    private val _backgroundUri = MutableStateFlow(prefs.getString(KEY_BACKGROUND, "").orEmpty())
    val backgroundUri: StateFlow<String> = _backgroundUri.asStateFlow()

    /** 背景图上的蒙版浓度（0~80）：越大文字越清楚、图越淡 */
    private val _backgroundScrim = MutableStateFlow(prefs.getInt(KEY_BACKGROUND_SCRIM, 30))
    val backgroundScrim: StateFlow<Int> = _backgroundScrim.asStateFlow()

    fun setBackgroundUri(uri: String?) {
        val value = uri.orEmpty()
        prefs.edit().putString(KEY_BACKGROUND, value).apply()
        _backgroundUri.value = value
    }

    fun setBackgroundScrim(percent: Int) {
        val value = percent.coerceIn(0, 80)
        prefs.edit().putInt(KEY_BACKGROUND_SCRIM, value).apply()
        _backgroundScrim.value = value
    }

    // ---- 学习设置（课表 / GPA / 上课提醒）----

    /** 学期起始日（周一，当天 00:00）：用来把「第几周」换算成真实日期；0 = 还没设 */
    private val _termStartMillis = MutableStateFlow(prefs.getLong(KEY_TERM_START, 0L))
    val termStartMillis: StateFlow<Long> = _termStartMillis.asStateFlow()

    fun setTermStartMillis(millis: Long) {
        prefs.edit().putLong(KEY_TERM_START, millis.coerceAtLeast(0L)).apply()
        _termStartMillis.value = millis.coerceAtLeast(0L)
    }

    /** GPA 计算口径：4.0 或 5.0（默认 4.0） */
    private val _gpaScale = MutableStateFlow(prefs.getFloat(KEY_GPA_SCALE, 4.0f).toDouble())
    val gpaScale: StateFlow<Double> = _gpaScale.asStateFlow()

    fun setGpaScale(scale: Double) {
        val value = if (scale >= 4.5) 5.0 else 4.0
        prefs.edit().putFloat(KEY_GPA_SCALE, value.toFloat()).apply()
        _gpaScale.value = value
    }

    /** 上课提醒：开关 + 提前多少分钟 */
    private val _classReminder = MutableStateFlow(prefs.getBoolean(KEY_CLASS_REMIND, false))
    val classReminder: StateFlow<Boolean> = _classReminder.asStateFlow()

    private val _classReminderMinutes = MutableStateFlow(prefs.getInt(KEY_CLASS_REMIND_MIN, 15))
    val classReminderMinutes: StateFlow<Int> = _classReminderMinutes.asStateFlow()

    fun setClassReminder(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLASS_REMIND, enabled).apply()
        _classReminder.value = enabled
    }

    fun setClassReminderMinutes(minutes: Int) {
        val value = minutes.coerceIn(5, 60)
        prefs.edit().putInt(KEY_CLASS_REMIND_MIN, value).apply()
        _classReminderMinutes.value = value
    }

    /** 某个币种最近用过的汇率（×RATE_SCALE），下次记同一币种不用重填 */
    fun currencyRate(code: String): Long =
        prefs.getLong(KEY_RATE_PREFIX + code, Currencies.RATE_SCALE)

    fun setCurrencyRate(code: String, rateScaled: Long) {
        if (code.isBlank() || rateScaled <= 0L) return
        prefs.edit().putLong(KEY_RATE_PREFIX + code, rateScaled).apply()
    }

    /** 某个预警是否已经发过（按月 + 阈值去重，避免每记一笔都提醒） */
    fun isBudgetWarned(key: String): Boolean = warnedKeys().contains(key)

    fun markBudgetWarned(key: String) {
        val next = warnedKeys().toMutableSet().apply { add(key) }
        prefs.edit().putStringSet(KEY_BUDGET_WARNED, next).apply()
    }

    private fun warnedKeys(): Set<String> = prefs.getStringSet(KEY_BUDGET_WARNED, emptySet()).orEmpty()

    private fun loadCategoryBudgets(): Map<String, Long> {
        val raw = prefs.getString(KEY_CATEGORY_BUDGETS, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence()
                .associateWith { key -> json.optLong(key, 0L) }
                .filterValues { it > 0L }
        }.getOrDefault(emptyMap())
    }

    fun setFocusTask(id: Long, title: String) {
        prefs.edit()
            .putLong(KEY_FOCUS_TASK_ID, id)
            .putString(KEY_FOCUS_TASK_TITLE, title)
            .apply()
        _focusTaskId.value = id
        _focusTaskTitle.value = title
    }

    fun clearFocusTask() = setFocusTask(NO_TASK, "")

    // ---- 待办提醒的记账本（哪条提醒过了、哪条排过闹钟） ----

    /** 已提醒过的键："<id>:<dueMillis>" 与 "<id>:<dueMillis>:overdue" */
    fun remindedKeys(): Set<String> = prefs.getStringSet(KEY_REMINDED, emptySet()).orEmpty()

    fun addRemindedKey(key: String) {
        val next = remindedKeys().toMutableSet().apply { add(key) }
        prefs.edit().putStringSet(KEY_REMINDED, next).apply()
    }

    /** 当前排过提醒闹钟的待办 id，便于删除或改期后撤销 */
    fun scheduledTodoIds(): Set<String> = prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()

    fun setScheduledTodoIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_SCHEDULED, ids).apply()
    }

    companion object {
        const val NO_TASK = -1L

        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANG = "ui_language"
        private const val KEY_DYNAMIC = "dynamic_color"
        private const val KEY_BUDGET = "monthly_budget_cents"
        private const val KEY_CATEGORY_BUDGETS = "category_budgets"
        private const val KEY_LEDGER_REMIND = "ledger_reminder_enabled"
        private const val KEY_LEDGER_REMIND_HOUR = "ledger_reminder_hour"
        private const val KEY_LEDGER_REMIND_MINUTE = "ledger_reminder_minute"
        private const val KEY_FOCUS_GOAL = "focus_daily_goal"
        private const val KEY_SUMMARY = "periodic_summary_mode"
        private const val KEY_BUDGET_ALERT = "budget_alert_enabled"
        private const val KEY_BUDGET_WARNED = "budget_warned_keys"
        private const val KEY_RATE_PREFIX = "currency_rate_"
        private const val KEY_PALETTE = "theme_palette"
        private const val KEY_BACKGROUND = "background_uri"
        private const val KEY_BACKGROUND_SCRIM = "background_scrim"
        private const val KEY_TERM_START = "term_start_millis"
        private const val KEY_GPA_SCALE = "gpa_scale"
        private const val KEY_CLASS_REMIND = "class_reminder_enabled"
        private const val KEY_CLASS_REMIND_MIN = "class_reminder_minutes"
        private const val KEY_FOCUS_TASK_ID = "focus_task_id"
        private const val KEY_FOCUS_TASK_TITLE = "focus_task_title"
        private const val KEY_REMINDED = "reminded_keys"
        private const val KEY_SCHEDULED = "scheduled_todo_ids"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
    }
}
