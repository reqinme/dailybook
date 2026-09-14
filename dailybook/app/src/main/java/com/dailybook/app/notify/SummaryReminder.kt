package com.dailybook.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.data.SummaryMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 每周 / 每月小结。
 *
 * 同样用非精确闹钟：每周日 20:00、每月 1 号 10:00。
 * 到点后由 [SummaryReceiver] 现算一遍数据再发通知，并把下一次排上。
 */
object SummaryReminder {

    private const val WEEKLY_HOUR = 20
    private const val MONTHLY_HOUR = 10
    private val ZONE: ZoneId = ZoneId.systemDefault()

    fun sync(context: Context) {
        val store = SettingsStore.get(context)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context)
        am.cancel(pi)
        val mode = store.summaryMode.value
        if (mode == SummaryMode.OFF) return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger(mode), pi)
    }

    /** 下一次该在什么时候发 */
    internal fun nextTrigger(mode: SummaryMode, now: LocalDateTime = LocalDateTime.now()): Long =
        when (mode) {
            SummaryMode.OFF -> 0L
            SummaryMode.WEEKLY -> {
                // 下一个周日 20:00；今天就是周日且还没到点，就用今天
                var days = (7 - now.dayOfWeek.value) % 7
                var target = now.toLocalDate().plusDays(days.toLong()).atTime(WEEKLY_HOUR, 0)
                if (!target.isAfter(now)) {
                    days += 7
                    target = now.toLocalDate().plusDays(days.toLong()).atTime(WEEKLY_HOUR, 0)
                }
                target.atZone(ZONE).toInstant().toEpochMilli()
            }
            SummaryMode.MONTHLY -> {
                // 下一个 1 号 10:00
                var target = now.toLocalDate().withDayOfMonth(1).atTime(MONTHLY_HOUR, 0)
                if (!target.isAfter(now)) {
                    target = now.toLocalDate().withDayOfMonth(1).plusMonths(1)
                        .atTime(MONTHLY_HOUR, 0)
                }
                target.atZone(ZONE).toInstant().toEpochMilli()
            }
        }

    /** 小结覆盖的日期区间（闭区间） */
    internal fun rangeFor(mode: SummaryMode, today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> =
        when (mode) {
            // 周日 20:00 发的「本周」= 刚过去的周一到今天
            SummaryMode.WEEKLY -> today.minusDays(6) to today
            // 1 号发的「上月」= 上一个自然月
            SummaryMode.MONTHLY -> today.minusMonths(1).withDayOfMonth(1) to
                today.withDayOfMonth(1).minusDays(1)
            SummaryMode.OFF -> today to today
        }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SummaryReceiver::class.java).apply {
            action = "com.dailybook.app.SUMMARY"
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
