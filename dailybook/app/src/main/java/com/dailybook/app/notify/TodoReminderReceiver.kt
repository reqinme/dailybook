package com.dailybook.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.dailybook.app.data.AppDatabase
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.util.formatDueLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 待办到点提醒的接收器，同时负责三件事：
 * 1. 常规到点提醒（[TodoReminder.EXTRA_TODO_ID]）
 * 2. 用户在通知上点了「稍后提醒」：撤掉当前通知，改排一个延迟闹钟
 * 3. 那个延迟闹钟到点：再提醒一次（不参与「每个到期日两次」的去重记账）
 */
class TodoReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(TodoReminder.EXTRA_TODO_ID, -1L)
        if (todoId <= 0L) return

        val snoozeDelay = intent.getLongExtra(TodoReminder.EXTRA_SNOOZE_DELAY, 0L)
        if (snoozeDelay > 0L) {
            NotificationManagerCompat.from(context).cancel(TodoReminder.todoNotifyId(todoId))
            TodoReminder.scheduleSnooze(context, todoId, snoozeDelay)
            return
        }

        val isSnoozeFire = intent.getBooleanExtra(TodoReminder.EXTRA_SNOOZE_FIRE, false)
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 闹钟到点后重新查库，确认这条待办现在还没完成
                val todo = AppDatabase.get(context).todoDao().getById(todoId)
                val due = todo?.dueMillis
                if (todo == null || todo.done || due == null) return@launch

                val store = SettingsStore.get(context)
                val key = TodoReminder.keyFor(todoId, due)
                val alreadyReminded = store.remindedKeys().contains(key)

                Notifier(context).notifyTodoDue(
                    todoId = todoId,
                    title = todo.title,
                    text = formatDueLabel(due, store.lang.value)
                )

                // 「稍后提醒」不记账，免得把正常的两档提醒次数吃掉
                if (!isSnoozeFire) {
                    store.addRemindedKey(
                        if (alreadyReminded) TodoReminder.overdueKeyFor(todoId, due) else key
                    )
                }
            } catch (_: Exception) {
                // 提醒失败不该让 App 崩，静默即可
            } finally {
                result.finish()
            }
        }
    }
}
