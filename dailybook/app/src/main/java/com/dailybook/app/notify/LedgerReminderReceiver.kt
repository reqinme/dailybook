package com.dailybook.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailybook.app.backup.AutoBackup
import com.dailybook.app.data.AppDatabase
import com.dailybook.app.data.DailyRepository
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.util.toLocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 晚上到点：做三件事，然后把下一次排上
 * 1. 补记到期的周期记账（即使下面两个开关都关着也照做，固定支出不能漏）
 * 2. 记账提醒：提醒记录当天的收支
 * 3. 专注目标：今天番茄数没达标时提醒一次
 */
class LedgerReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = SettingsStore.get(context)
        val reminderOn = store.ledgerReminderEnabled.value
        val goal = store.focusGoal.value

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 到期的周期记账先落成真实记录，再把下次日期往后推
                DailyRepository(context).materializeRecurring()
                // 顺带做每日自动备份（用户选过文件夹且开启时才会真的写文件）
                AutoBackup.runBackupIfDue(context)

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
