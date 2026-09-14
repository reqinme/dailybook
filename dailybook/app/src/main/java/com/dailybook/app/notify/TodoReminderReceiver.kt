package com.dailybook.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailybook.app.data.AppDatabase
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.util.formatDueLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 待办到点提醒的接收器。
 *
 * 闹钟到点时**重新查一次数据库**，而不是把内容塞在 Intent 里：
 * 这样即使闹钟是「过期未完成」补发的，也能确认这条待办现在还没完成。
 */
class TodoReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(TodoReminder.EXTRA_TODO_ID, -1L)
        if (todoId <= 0L) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val todo = AppDatabase.get(context).todoDao().getById(todoId)
                val due = todo?.dueMillis
                if (todo == null || todo.done || due == null) return@launch

                val store = SettingsStore.get(context)
                val key = TodoReminder.keyFor(todoId, due)
                val alreadyReminded = store.remindedKeys().contains(key)

                Notifier(context).notifyTodoDue(
                    todoId = todoId,
                    title = todo.title,
                    text = formatDueLabel(due)
                )

                // 第一档提醒 → 记 key；已经是第二档（逾期补提醒）→ 记 overdue key
                store.addRemindedKey(
                    if (alreadyReminded) TodoReminder.overdueKeyFor(todoId, due) else key
                )
            } catch (_: Exception) {
                // 提醒失败不该让 App 崩，静默即可
            } finally {
                result.finish()
            }
        }
    }
}
