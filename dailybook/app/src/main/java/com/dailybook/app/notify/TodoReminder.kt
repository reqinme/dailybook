package com.dailybook.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.data.TodoEntity
import com.dailybook.app.util.toLocalDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 待办到点提醒的排程。
 *
 * 用的是 AlarmManager 的**非精确**闹钟（setAndAllowWhileIdle）：不申请任何特殊权限，
 * 也不会像精确闹钟那样在 Android 12+ 上要求用户再去系统设置里单独授权，对「某天的提醒」完全够用。
 *
 * 提醒策略（同一个到期日最多打扰两次）：
 *  1. 到期日上午 9:00 提醒一次；
 *     若这个点已经过去（比如今天下午才把日期设成今天，或者任务已经逾期），
 *     则在下次同步时（打开 App）立刻提醒，不必等到明天；
 *  2. 仍然没完成的逾期任务，次日 9:00 再提醒一次，之后不再打扰。
 *
 * 完成、改期、删除后都会重新排程，且「提醒过」的记录以「id:到期日」为键，
 * 改期后键会变，所以新日期会重新提醒。
 */
object TodoReminder {

    const val EXTRA_TODO_ID = "todo_id"

    /** 用户点了通知上的「稍后提醒」，带着延迟时长 */
    const val EXTRA_SNOOZE_DELAY = "snooze_delay"

    /** 稍后提醒的闹钟真的到点了 */
    const val EXTRA_SNOOZE_FIRE = "snooze_fire"

    private const val HOUR = 9
    private const val SOON_DELAY_MS = 15_000L
    private const val ACTION_REMIND = "com.dailybook.app.TODO_REMIND"
    private const val ACTION_SNOOZE = "com.dailybook.app.TODO_SNOOZE"
    private const val ACTION_SNOOZE_FIRE = "com.dailybook.app.TODO_SNOOZE_FIRE"
    private val ZONE: ZoneId = ZoneId.systemDefault()

    /** 通知 id：Notifier 与「稍后提醒」都按同一个算法取，避免对不上而撤不掉通知 */
    fun todoNotifyId(todoId: Long): Int = 2000 + (todoId % 100_000L).toInt()

    /** 「明天早上」= 明天 9 点 */
    fun millisUntilTomorrowMorning(now: LocalDateTime = LocalDateTime.now()): Long {
        val target = now.toLocalDate().plusDays(1).atTime(HOUR, 0)
            .atZone(ZONE).toInstant().toEpochMilli()
        return (target - System.currentTimeMillis()).coerceAtLeast(60_000L)
    }

    /** 通知里「稍后提醒」按钮的 PendingIntent：立刻回到接收器，由它去排延迟闹钟 */
    fun snoozeIntent(context: Context, todoId: Long, delayMillis: Long): PendingIntent {
        val intent = Intent(context, TodoReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            data = Uri.parse("dailybook://todo-snooze/$todoId/$delayMillis")
            putExtra(EXTRA_TODO_ID, todoId)
            putExtra(EXTRA_SNOOZE_DELAY, delayMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 排一个「稍后提醒」闹钟。
     * data 里带上触发时刻，所以它和常 규排程的闹钟是两个不同的 PendingIntent，
     * [sync] 重排时不会把它误撤掉。
     */
    fun scheduleSnooze(context: Context, todoId: Long, delayMillis: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val at = System.currentTimeMillis() + delayMillis.coerceAtLeast(60_000L)
        val intent = Intent(context, TodoReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_FIRE
            data = Uri.parse("dailybook://todo-snooze-fire/$todoId/$at")
            putExtra(EXTRA_TODO_ID, todoId)
            putExtra(EXTRA_SNOOZE_FIRE, true)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    fun keyFor(id: Long, dueMillis: Long): String = "$id:$dueMillis"

    fun overdueKeyFor(id: Long, dueMillis: Long): String = "${keyFor(id, dueMillis)}:overdue"

    /**
     * 按当前待办列表重排全部提醒。幂等：先撤掉上一轮排过的闹钟，再按需重排，
     * 所以删除、完成、改期之后都不会留下幽灵提醒。
     */
    fun sync(context: Context, todos: List<TodoEntity>) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val store = SettingsStore.get(context)
        val reminded = store.remindedKeys()
        val today = LocalDate.now()
        val now = System.currentTimeMillis()

        (store.scheduledTodoIds() + todos.map { it.id.toString() })
            .mapNotNull { it.toLongOrNull() }
            .distinct()
            .forEach { am.cancel(pendingIntent(context, it)) }

        val scheduled = mutableSetOf<String>()
        todos.forEach { item ->
            val due = item.dueMillis ?: return@forEach
            if (item.done) return@forEach
            val triggerAt = nextTrigger(item.id, due, today, now, reminded) ?: return@forEach
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent(context, item.id)
            )
            scheduled += item.id.toString()
        }
        store.setScheduledTodoIds(scheduled)
    }

    /** 某个待办下一次该在什么时候提醒；返回 null 表示这个到期日不用再提醒了 */
    internal fun nextTrigger(
        id: Long,
        dueMillis: Long,
        today: LocalDate,
        now: Long,
        reminded: Set<String>
    ): Long? {
        val due = dueMillis.toLocalDate()
        val key = keyFor(id, dueMillis)
        if (due.isAfter(today)) {
            return if (key in reminded) null else atHour(due, HOUR)
        }
        if (key !in reminded) return now + SOON_DELAY_MS
        if (overdueKeyFor(id, dueMillis) !in reminded) return atHour(today.plusDays(1), HOUR)
        return null
    }

    private fun atHour(date: LocalDate, hour: Int): Long =
        date.atTime(hour, 0).atZone(ZONE).toInstant().toEpochMilli()

    /** 供单元测试使用：把「几点提醒」也暴露出来 */
    internal fun hourOfDay(): Int = HOUR

    internal fun soonDelayMillis(): Long = SOON_DELAY_MS

    fun cancel(context: Context, todoId: Long) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context, todoId))
    }

    /**
     * 每个待办一个 PendingIntent：data 里的待办 id 参与身份判定，
     * 所以不会因为 requestCode 相同而互相覆盖。
     */
    private fun pendingIntent(context: Context, todoId: Long): PendingIntent {
        val intent = Intent(context, TodoReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            data = Uri.parse("dailybook://todo/$todoId")
            putExtra(EXTRA_TODO_ID, todoId)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
