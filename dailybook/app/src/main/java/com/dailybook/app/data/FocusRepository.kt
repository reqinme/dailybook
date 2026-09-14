package com.dailybook.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dailybook.app.timer.Phase
import com.dailybook.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_flow_prefs")

/** 专注计时的可配置项 */
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

/** 专注统计 */
data class FocusStats(
    val todayCount: Int = 0,
    val totalCount: Int = 0,
    val recentDays: List<DayCount> = emptyList()
) {
    /** 连续专注天数（从最近一天往前数） */
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

private object FocusKeys {
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

private val FOCUS_DAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** 专注计时的设置与统计（DataStore） */
class FocusRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            focusMinutes = p[FocusKeys.FOCUS_MIN] ?: 25,
            shortBreakMinutes = p[FocusKeys.SHORT_MIN] ?: 5,
            longBreakMinutes = p[FocusKeys.LONG_MIN] ?: 15,
            longBreakEvery = p[FocusKeys.LONG_EVERY] ?: 4,
            autoStartNext = p[FocusKeys.AUTO_START] ?: false,
            vibrate = p[FocusKeys.VIBRATE] ?: true,
            keepScreenOn = p[FocusKeys.KEEP_SCREEN_ON] ?: true,
            themeMode = runCatching {
                ThemeMode.valueOf(p[FocusKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = p[FocusKeys.DYNAMIC_COLOR] ?: false
        )
    }

    val stats: Flow<FocusStats> = context.dataStore.data.map { p ->
        val map = parseHistory(p[FocusKeys.HISTORY] ?: emptySet())
        val today = LocalDate.now()
        val days = (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            DayCount(d, map[d] ?: 0)
        }
        FocusStats(
            todayCount = map[today] ?: 0,
            totalCount = p[FocusKeys.TOTAL_COUNT] ?: 0,
            recentDays = days
        )
    }

    private fun parseHistory(raw: Set<String>): Map<LocalDate, Int> = raw.mapNotNull { entry ->
        val idx = entry.lastIndexOf('=')
        if (idx <= 0) return@mapNotNull null
        val date = runCatching { LocalDate.parse(entry.substring(0, idx), FOCUS_DAY_FMT) }.getOrNull()
        val count = entry.substring(idx + 1).toIntOrNull()
        if (date == null || count == null) null else date to count
    }.toMap()

    suspend fun recordCompletedFocus() {
        val today = LocalDate.now()
        context.dataStore.edit { p ->
            val map = parseHistory(p[FocusKeys.HISTORY] ?: emptySet()).toMutableMap()
            map[today] = (map[today] ?: 0) + 1
            p[FocusKeys.TOTAL_COUNT] = (p[FocusKeys.TOTAL_COUNT] ?: 0) + 1
            p[FocusKeys.HISTORY] = map.entries
                .filter { it.value > 0 }
                .map { "${it.key.format(FOCUS_DAY_FMT)}=${it.value}" }
                .toSet()
        }
    }

    suspend fun resetStats() {
        context.dataStore.edit { p ->
            p[FocusKeys.TOTAL_COUNT] = 0
            p[FocusKeys.HISTORY] = emptySet()
        }
    }

    suspend fun setFocusMinutes(v: Int) = editInt(FocusKeys.FOCUS_MIN, v)

    suspend fun setShortBreakMinutes(v: Int) = editInt(FocusKeys.SHORT_MIN, v)

    suspend fun setLongBreakMinutes(v: Int) = editInt(FocusKeys.LONG_MIN, v)

    suspend fun setLongBreakEvery(v: Int) = editInt(FocusKeys.LONG_EVERY, v)

    suspend fun setAutoStart(v: Boolean) = editBool(FocusKeys.AUTO_START, v)

    suspend fun setVibrate(v: Boolean) = editBool(FocusKeys.VIBRATE, v)

    suspend fun setKeepScreenOn(v: Boolean) = editBool(FocusKeys.KEEP_SCREEN_ON, v)

    suspend fun setDynamicColor(v: Boolean) = editBool(FocusKeys.DYNAMIC_COLOR, v)

    private suspend fun editInt(key: Preferences.Key<Int>, v: Int) {
        context.dataStore.edit { it[key] = v }
    }

    private suspend fun editBool(key: Preferences.Key<Boolean>, v: Boolean) {
        context.dataStore.edit { it[key] = v }
    }
}
