package com.focusflow.timer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focusflow.timer.timer.Phase
import com.focusflow.timer.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_flow_prefs")

/** 用户可配置项 */
data class AppSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakEvery: Int = 4,
    val autoStartNext: Boolean = false,
    val vibrate: Boolean = true,
    val keepScreenOn: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false
) {
    fun durationMillisFor(phase: Phase): Long = when (phase) {
        Phase.FOCUS -> focusMinutes
        Phase.SHORT_BREAK -> shortBreakMinutes
        Phase.LONG_BREAK -> longBreakMinutes
    } * 60_000L
}

/** 某一天的专注完成数 */
data class DayCount(val date: LocalDate, val count: Int)

/** 统计数据 */
data class FocusStats(
    val todayCount: Int = 0,
    val totalCount: Int = 0,
    val recentDays: List<DayCount> = emptyList()
) {
    /** 连续专注天数（从今天或昨天往前数） */
    val streak: Int
        get() {
            if (recentDays.isEmpty()) return 0
            var s = 0
            for (d in recentDays.reversed()) {
                if (d.count > 0) s++ else break
            }
            return s
        }
}

private object Keys {
    val FOCUS_MIN = intPreferencesKey("focus_minutes")
    val SHORT_MIN = intPreferencesKey("short_break_minutes")
    val LONG_MIN = intPreferencesKey("long_break_minutes")
    val LONG_EVERY = intPreferencesKey("long_break_every")
    val AUTO_START = booleanPreferencesKey("auto_start_next")
    val VIBRATE = booleanPreferencesKey("vibrate")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val TOTAL_COUNT = intPreferencesKey("total_focus_count")
    val HISTORY = stringSetPreferencesKey("focus_history")
}

private val DAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

class FocusRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            focusMinutes = p[Keys.FOCUS_MIN] ?: 25,
            shortBreakMinutes = p[Keys.SHORT_MIN] ?: 5,
            longBreakMinutes = p[Keys.LONG_MIN] ?: 15,
            longBreakEvery = p[Keys.LONG_EVERY] ?: 4,
            autoStartNext = p[Keys.AUTO_START] ?: false,
            vibrate = p[Keys.VIBRATE] ?: true,
            keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: true,
            themeMode = runCatching {
                ThemeMode.valueOf(p[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: false
        )
    }

    val stats: Flow<FocusStats> = context.dataStore.data.map { p ->
        val map = parseHistory(p[Keys.HISTORY] ?: emptySet())
        val today = LocalDate.now()
        val days = (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            DayCount(d, map[d] ?: 0)
        }
        FocusStats(
            todayCount = map[today] ?: 0,
            totalCount = p[Keys.TOTAL_COUNT] ?: 0,
            recentDays = days
        )
    }

    private fun parseHistory(raw: Set<String>): Map<LocalDate, Int> = raw.mapNotNull { entry ->
        val idx = entry.lastIndexOf('=')
        if (idx <= 0) return@mapNotNull null
        val date = runCatching { LocalDate.parse(entry.substring(0, idx), DAY_FMT) }.getOrNull()
        val count = entry.substring(idx + 1).toIntOrNull()
        if (date == null || count == null) null else date to count
    }.toMap()

    private suspend fun putHistory(days: Map<LocalDate, Int>) {
        val encoded = days.entries
            .filter { it.value > 0 }
            .map { "${it.key.format(DAY_FMT)}=${it.value}" }
            .toSet()
        context.dataStore.edit { it[Keys.HISTORY] = encoded }
    }

    suspend fun recordCompletedFocus() {
        val today = LocalDate.now()
        context.dataStore.edit { p ->
            val map = parseHistory(p[Keys.HISTORY] ?: emptySet()).toMutableMap()
            map[today] = (map[today] ?: 0) + 1
            p[Keys.TOTAL_COUNT] = (p[Keys.TOTAL_COUNT] ?: 0) + 1
            val encoded = map.entries
                .filter { it.value > 0 }
                .map { "${it.key.format(DAY_FMT)}=${it.value}" }
                .toSet()
            p[Keys.HISTORY] = encoded
        }
    }

    suspend fun resetStats() {
        context.dataStore.edit { p ->
            p[Keys.TOTAL_COUNT] = 0
            p[Keys.HISTORY] = emptySet()
        }
    }

    suspend fun setFocusMinutes(v: Int) = editInt(Keys.FOCUS_MIN, v)
    suspend fun setShortBreakMinutes(v: Int) = editInt(Keys.SHORT_MIN, v)
    suspend fun setLongBreakMinutes(v: Int) = editInt(Keys.LONG_MIN, v)
    suspend fun setLongBreakEvery(v: Int) = editInt(Keys.LONG_EVERY, v)
    suspend fun setAutoStart(v: Boolean) = editBool(Keys.AUTO_START, v)
    suspend fun setVibrate(v: Boolean) = editBool(Keys.VIBRATE, v)
    suspend fun setKeepScreenOn(v: Boolean) = editBool(Keys.KEEP_SCREEN_ON, v)
    suspend fun setDynamicColor(v: Boolean) = editBool(Keys.DYNAMIC_COLOR, v)

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    private suspend fun editInt(key: Preferences.Key<Int>, v: Int) {
        context.dataStore.edit { it[key] = v }
    }

    private suspend fun editBool(key: Preferences.Key<Boolean>, v: Boolean) {
        context.dataStore.edit { it[key] = v }
    }
}
