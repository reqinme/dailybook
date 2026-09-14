package com.dailybook.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dailybook.app.data.AppDatabase
import com.dailybook.app.data.CourseEntity
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.util.ClassSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * 上课提醒的排程。
 *
 * 做法和 [TodoReminder] / [LedgerReminder] 一致：AlarmManager 的**非精确**闹钟
 * （`setAndAllowWhileIdle`），不申请任何新权限 —— 不碰「精确闹钟」权限，
 * 也不需要在 Android 12+ 上再让用户去系统设置里单独授权。
 *
 * 与它们不同的是：**同时只留一个闹钟**（下一次要提醒的那节课）。
 * 课表是周期性的、能自我延续（响完由 [ClassReminderReceiver] 立刻排下一节），
 * 一次排一学期的闹钟既没必要，也容易变成「闹钟风暴」，还会在改课表时留下一堆幽灵提醒。
 *
 * 排出来的时刻认的是课程的 [com.dailybook.app.data.CourseEntity.startMinutes]：
 * 没填时间的课排不出提醒（见 [ClassSchedule]），这是「课表只有节次」时无法回避的事实。
 */
object ClassReminder {

    /** 闹钟带上「是哪节课」：接收器据此确认这节课还在不在（内容一律现查） */
    const val EXTRA_COURSE_ID = "course_id"

    /** 闹钟排的时候这节课的开始钟点；课程改了时间就以库里的为准，这个只是兜底 */
    const val EXTRA_START_MINUTES = "start_minutes"

    /** 闹钟的 action：和别的提醒区分开，便于撤销与排查 */
    private const val ACTION_REMIND = "com.dailybook.app.CLASS_REMIND"

    /** 已经排过的触发时刻，用于快速判断「还是同一节课」、不必重复排 */
    private const val PREFS_NAME = "class_reminder"
    private const val KEY_SCHEDULED_AT = "scheduled_at"

    private const val REQUEST_CODE = 0

    /**
     * 取消旧闹钟，然后按当前数据排下一个；没有课、课程都没填时间、
     * 或者开关关着时**只取消**，不排任何东西。
     *
     * 保持**非挂起**函数（设置页的点击回调、[BootReceiver] 都直接调用它），
     * 读库那一步丢给内部协程去做 —— 和 [TodoReminder.sync] 的用法一致。
     *
     * 设计成随时可调用、且**绝不抛异常**：数据加载、闹钟撤销、系统服务缺失全都在内部兜住 ——
     * 它会被界面回调、数据变化、闹钟到点等多个入口调用，任何一处崩掉都会让用户直接看到崩溃。
     */
    fun reschedule(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = SettingsStore.get(app)
                // 开关关着时连库都不用查：只撤闹钟
                val courses = if (store.classReminder.value) {
                    AppDatabase.get(app).courseDao().getAll()
                } else {
                    emptyList()
                }
                scheduleNext(app, courses)
            } catch (_: Exception) {
                // 排程失败不该让 App 崩；下次打开 App 或改课表时会再排一次
            }
        }
    }

    /**
     * 真正的排程：算出下一次要提醒哪节课，撤掉旧的，排上新的。
     *
     * 放在这里（私有、普通函数）是为了让 [reschedule] 保持非挂起，
     * 同时让「算 + 排」这一段在一处完成，撤销与排程之间不会被别的调用插进来。
     */
    private fun scheduleNext(context: Context, courses: List<CourseEntity>) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val store = SettingsStore.get(context)

        val occurrence = if (store.classReminder.value) {
            ClassSchedule.nextOccurrence(
                courses = courses,
                leadMinutes = store.classReminderMinutes.value,
                nowMillis = System.currentTimeMillis(),
                zone = ZoneId.systemDefault(),
                // 课程自己没填学期起始日时，退回设置里的那个
                fallbackTermStartMillis = store.termStartMillis.value
            )
        } else {
            null
        }

        // 先撤掉上一个：不撤的话「改了时间 / 关了开关」之后旧闹钟还会照响。
        // 不带参数即「没有排东西」的那个身份，和真正排程用的是同一个 PendingIntent。
        am.cancel(pendingIntent(context))
        if (occurrence == null) {
            forget(context)
            return
        }

        remember(context, occurrence.startMillis)

        // 非精确闹钟不能排在过去：提醒时刻已经过了（打开 App 时已经快到上课点）
        // 就让它 5 秒后立刻响一次，而不是白排一个永远不会触发的时刻
        val triggerAt = occurrence.notifyAtMillis
            .coerceAtLeast(System.currentTimeMillis() + 5_000L)
        am.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(context, occurrence.course.id, occurrence.course.startMinutes)
        )
    }

    /** 只取消，不排新的（关开关、清空课表时用） */
    fun cancel(context: Context) {
        try {
            val app = context.applicationContext
            app.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(app))
            forget(app)
        } catch (_: Exception) {
            // 同上：撤销失败无伤大雅
        }
    }

    /** 下次闹钟的触发时刻；没排过返回 0（界面 / 排查用，不影响排程） */
    fun scheduledAt(context: Context): Long =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_SCHEDULED_AT, 0L)

    private fun remember(context: Context, startMillis: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_SCHEDULED_AT, startMillis).apply()
    }

    private fun forget(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_SCHEDULED_AT).apply()
    }

    /**
     * 只留一个闹钟，所以 PendingIntent 的身份是固定的（action + 固定 requestCode）：
     * 重排时用同一个身份去 cancel / 覆盖，不会堆积。
     */
    private fun pendingIntent(context: Context, courseId: Long = -1L, startMinutes: Int = -1): PendingIntent {
        val intent = Intent(context, ClassReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_COURSE_ID, courseId)
            putExtra(EXTRA_START_MINUTES, startMinutes)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
