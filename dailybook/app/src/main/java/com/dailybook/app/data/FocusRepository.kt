package com.dailybook.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dailybook.app.timer.Phase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_flow_prefs")

/**
 * 专注计时的可配置项。
 * 注意：专注的「次数统计」不在这里 —— 它由数据库中的 focus_sessions 表派生，
 * 保证同一份数据只有一处来源。
 */
data class AppSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakEvery: Int = 4,
    val autoStartNext: Boolean = false,
    val vibrate: Boolean = true,
    val keepScreenOn: Boolean = true
) {
    fun durationMillisFor(phase: Phase): Long = when (phase) {
        Phase.FOCUS -> focusMinutes
        Phase.SHORT_BREAK -> shortBreakMinutes
        Phase.LONG_BREAK -> longBreakMinutes
    } * 60_000L
}

private object FocusKeys {
    val FOCUS_MIN = intPreferencesKey("focus_minutes")
    val SHORT_MIN = intPreferencesKey("short_break_minutes")
    val LONG_MIN = intPreferencesKey("long_break_minutes")
    val LONG_EVERY = intPreferencesKey("long_break_every")
    val AUTO_START = booleanPreferencesKey("auto_start_next")
    val VIBRATE = booleanPreferencesKey("vibrate")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
}

/** 专注计时的设置（DataStore 持久化） */
class FocusRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            focusMinutes = p[FocusKeys.FOCUS_MIN] ?: 25,
            shortBreakMinutes = p[FocusKeys.SHORT_MIN] ?: 5,
            longBreakMinutes = p[FocusKeys.LONG_MIN] ?: 15,
            longBreakEvery = p[FocusKeys.LONG_EVERY] ?: 4,
            autoStartNext = p[FocusKeys.AUTO_START] ?: false,
            vibrate = p[FocusKeys.VIBRATE] ?: true,
            keepScreenOn = p[FocusKeys.KEEP_SCREEN_ON] ?: true
        )
    }

    suspend fun setFocusMinutes(v: Int) = editInt(FocusKeys.FOCUS_MIN, v)

    suspend fun setShortBreakMinutes(v: Int) = editInt(FocusKeys.SHORT_MIN, v)

    suspend fun setLongBreakMinutes(v: Int) = editInt(FocusKeys.LONG_MIN, v)

    suspend fun setLongBreakEvery(v: Int) = editInt(FocusKeys.LONG_EVERY, v)

    suspend fun setAutoStart(v: Boolean) = editBool(FocusKeys.AUTO_START, v)

    suspend fun setVibrate(v: Boolean) = editBool(FocusKeys.VIBRATE, v)

    suspend fun setKeepScreenOn(v: Boolean) = editBool(FocusKeys.KEEP_SCREEN_ON, v)

    private suspend fun editInt(key: Preferences.Key<Int>, v: Int) {
        context.dataStore.edit { it[key] = v }
    }

    private suspend fun editBool(key: Preferences.Key<Boolean>, v: Boolean) {
        context.dataStore.edit { it[key] = v }
    }
}
