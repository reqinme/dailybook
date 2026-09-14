package com.dailybook.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dailybook.app.data.AppDatabase
import com.dailybook.app.data.ImportantDateEntity
import com.dailybook.app.util.ImportantDateSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * 「重要日期」的提前提醒排程（妈妈生日 · 提前 3 天）。
 *
 * 做法和 [TodoReminder] / [ClassReminder] 完全一致：AlarmManager 的**非精确**闹钟
 * （`setAndAllowWhileIdle`）—— 不申请任何新权限，也不碰 Android 12+ 的「精确闹钟」授权。
 *
 * 与课表提醒一样**同时只留一个闹钟**（最近的那次提醒），响完由
 * [ImportantDateReminderReceiver] 再排下一次：两次提醒之间至少隔着好几天，
 * 一次把一整年的闹钟全排上只会制造「闹钟风暴」，改日期时还容易留下幽灵提醒。
 *
 * 「响过没有」不靠额外的记账来判断，而是靠**时间本身**：
 * 提醒时刻总在日期之*前* N 天，响完那一刻它就已经过去了，
 * [ImportantDateSchedule.fireMillis] 算出来的又是「不早于今天的那一次」，
 * 所以响过之后重排自然会滚到下一次（一年 / 一月 / 一周之后，或者干脆没有下一次）。
 * 已经排上的时刻会记在本模块自己的 SharedPreferences 里（和 [ClassReminder] 记
 * `scheduled_at` 同一个思路），便于排查「到底排上了没有」。
 *
 * 排出来的时刻由 [ImportantDateSchedule] 纯函数算：下一次发生的当天上午 9:00 往前挪
 * `remindDaysBefore` 天（0 = 当天）—— 上午 9 点这个选择的理由写在那里。
 */
object ImportantDateReminder {

    /** 闹钟带上「是哪条重要日期」；内容一律到点现查，避免响出来的是旧标题 */
    const val EXTRA_DATE_ID = "important_date_id"

    /** 闹钟的 action：和别的提醒区分开，便于撤销与排查 */
    private const val ACTION_REMIND = "com.dailybook.app.DATE_REMIND"

    /** 已经排上的触发时刻（毫秒），0 = 没排 */
    private const val PREFS_NAME = "important_date_reminder"
    private const val KEY_SCHEDULED_AT = "scheduled_at"

    /** 最近一次已经通知过的「哪条日期的哪一次发生」，形如 `3:20650` */
    private const val KEY_LAST_NOTIFIED = "last_notified"

    /**
     * 只留一个闹钟，所以 PendingIntent 的身份固定（action + 固定 requestCode）：
     * 重排时用同一个身份 cancel / 覆盖，不会堆积。
     */
    private const val REQUEST_CODE = 0

    /**
     * 非精确闹钟不能排在过去：提醒时刻已经过了（比如用户上午 10 点才把日期设成今天）
     * 就 5 秒后立刻响一次，而不是白排一个永远不会触发的时刻
     * （和 [ClassReminder.scheduleNext] 同一处理）。
     */
    private const val SOON_DELAY_MS = 5_000L

    /**
     * 「这次提醒已经算响过了」的时间余量：提醒时刻比现在早超过这个量，就不再排它。
     *
     * 接收器是「先发通知、再把下一次排上」的，排程那一刻刚响过的那次提醒时刻已经过去，
     * 不留余量的话它会被当成「还该响」而重新排成「5 秒后立刻响」，一条提醒响两遍。
     * 余量必须**小于**接收器判断「这次已经过去太久、不用再响」的阈值（那边是 30 分钟），
     * 否则刚响过的那次会被排上、却又被接收器判定成「太晚了」而不响 —— 白排一个空闹钟。
     */
    private const val MIN_DELAY_MS = 5 * 60_000L

    /**
     * 按当前数据重排：撤掉旧闹钟，再排「最近一次还没响的提醒」；没有就只撤销。
     *
     * 保持**非挂起**函数（数据变化、`MainViewModel`、[BootReceiver] 都直接调它），
     * 读库那一步丢给内部协程 —— 和 [ClassReminder.reschedule] 的用法一致。
     * 设计成随时可调用、且**绝不抛异常**：任何一处崩掉都会让用户直接看到崩溃。
     */
    fun reschedule(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduleNext(app, AppDatabase.get(app).importantDateDao().getAll())
            } catch (_: Exception) {
                // 排程失败不该让 App 崩；下次打开 App 或改日期时会再排一次
            }
        }
    }

    /**
     * 真正的排程：算出下一次该响的提醒，撤掉旧的，排上新的。
     *
     * 放在这里（私有、普通函数）是为了让 [reschedule] 保持非挂起，
     * 同时让「算 + 排」在一处完成，撤销与排程之间不会被别的调用插进来。
     */
    private fun scheduleNext(context: Context, dates: List<ImportantDateEntity>) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        // 先撤掉上一个：不撤的话「改了日期 / 删了 / 改回不提醒」之后旧闹钟还会照响。
        // 不带 id 即「没有排东西」的那个身份，和真正排程用的是同一个 PendingIntent。
        am.cancel(pendingIntent(context))

        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()

        val next = dates
            // 「不提醒」（remindDaysBefore <= 0）的日期不排任何东西
            .filter { it.remindDaysBefore > 0 }
            .mapNotNull { item ->
                val fireAt = ImportantDateSchedule.fireMillis(item, now, zone)
                    ?: return@mapNotNull null
                // 已经过去（响过了 / 那条「只过一次」的日子已经过完）的不再排
                if (fireAt <= now - MIN_DELAY_MS) return@mapNotNull null
                item to fireAt
            }
            .minByOrNull { (_, fireAt) -> fireAt }

        if (next == null) {
            forget(context)
            return
        }

        remember(context, next.second)
        am.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.second.coerceAtLeast(now + SOON_DELAY_MS),
            pendingIntent(context, next.first.id)
        )
    }

    /** 只取消，不排新的（清空重要日期时用） */
    fun cancel(context: Context) {
        try {
            val app = context.applicationContext
            app.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(app))
            forget(app)
        } catch (_: Exception) {
            // 撤销失败无伤大雅
        }
    }

    /** 已经排上的触发时刻（毫秒），0 = 没排过（界面与排查用，不影响排程） */
    fun scheduledAt(context: Context): Long =
        prefs(context).getLong(KEY_SCHEDULED_AT, 0L)

    /**
     * 这一次发生（[occurrenceEpochDay]）是不是已经通知过了。
     *
     * 只比「哪一天」而不是「几点」：排程与接收器对同一次提醒算出的是同一个日子，
     * 所以「同一天里又打开一次 App 被重排成 5 秒后立刻响」不会变成两天条通知；
     * 而每年 / 每月下一次发生时是另一个日子，照常会响。
     */
    fun alreadyNotified(context: Context, dateId: Long, occurrenceEpochDay: Long): Boolean =
        prefs(context).getString(KEY_LAST_NOTIFIED, "").orEmpty() == notifiedKey(dateId, occurrenceEpochDay)

    /** 记下「这次发生已经通知过」；只有真的发出去通知之后才该调用 */
    fun markNotified(context: Context, dateId: Long, occurrenceEpochDay: Long) {
        prefs(context).edit()
            .putString(KEY_LAST_NOTIFIED, notifiedKey(dateId, occurrenceEpochDay))
            .apply()
    }

    /** 一次通知的身份：`<日期 id>:<发生日>` */
    fun notifiedKey(dateId: Long, occurrenceEpochDay: Long): String = "$dateId:$occurrenceEpochDay"

    private fun remember(context: Context, fireAt: Long) {
        prefs(context).edit().putLong(KEY_SCHEDULED_AT, fireAt).apply()
    }

    private fun forget(context: Context) {
        prefs(context).edit().remove(KEY_SCHEDULED_AT).apply()
    }

    /** 本模块自己的偏好文件（只放排程状态，不进 SettingsStore） */
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun pendingIntent(context: Context, dateId: Long = -1L): PendingIntent {
        val intent = Intent(context, ImportantDateReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_DATE_ID, dateId)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
