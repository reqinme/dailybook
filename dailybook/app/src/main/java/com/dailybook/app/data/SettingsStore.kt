package com.dailybook.app.data

import android.content.Context
import com.dailybook.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, false))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    /** 月度预算（分），0 表示未设置 */
    private val _monthlyBudgetCents = MutableStateFlow(prefs.getLong(KEY_BUDGET, 0L))
    val monthlyBudgetCents: StateFlow<Long> = _monthlyBudgetCents.asStateFlow()

    /** 当前选中的专注目标（某条待办） */
    private val _focusTaskId = MutableStateFlow(prefs.getLong(KEY_FOCUS_TASK_ID, NO_TASK))
    val focusTaskId: StateFlow<Long> = _focusTaskId.asStateFlow()

    private val _focusTaskTitle = MutableStateFlow(prefs.getString(KEY_FOCUS_TASK_TITLE, "").orEmpty())
    val focusTaskTitle: StateFlow<String> = _focusTaskTitle.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
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
        private const val KEY_DYNAMIC = "dynamic_color"
        private const val KEY_BUDGET = "monthly_budget_cents"
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
