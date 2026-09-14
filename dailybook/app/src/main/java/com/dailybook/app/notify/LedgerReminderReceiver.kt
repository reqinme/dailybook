package com.dailybook.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailybook.app.data.AppDatabase
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.util.toLocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 晚上到点：按开关分别做两件事，然后把下一次排上
 * 1. 记账提醒：提醒记录当天的收支
 * 2. 专注目标：今天番茄数没达标时提醒一次
 */
class LedgerReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = SettingsStore.get(context)
        val reminderOn = store.ledgerReminderEnabled.value
        val goal = store.focusGoal.value
        if (!reminderOn && goal <= 0) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notifier = Notifier(context)
                if (reminderOn) notifier.notifyLedgerReminder()
                if (goal > 0) {
                    val today = LocalDate.now()
                    val done = AppDatabase.get(context).focusSessionDao().getAll()
                        .count { it.startedAtMillis.toLocalDate() == today }
                    if (done < goal) notifier.notifyFocusGoal(done, goal)
                }
            } catch (_: Exception) {
                // 提醒失败不该让 App 崩
            } finally {
                LedgerReminder.sync(context)
                result.finish()
            }
        }
    }
}
