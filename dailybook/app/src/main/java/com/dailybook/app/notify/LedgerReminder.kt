package com.dailybook.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dailybook.app.data.SettingsStore
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 每晚记账提醒。
 *
 * 和待办提醒用同一套做法：非精确闹钟（setAndAllowWhileIdle），不申请「精确闹钟」权限，
 * 只要求当天提醒到就行。到点后由 [LedgerReminderReceiver] 发通知并顺手排下一天。
 */
object LedgerReminder {

    private const val ACTION_REMIND = "com.dailybook.app.LEDGER_REMIND"

    /** 按当前设置重排：关掉开关就把闹钟撤掉，改时间就排到新的时间点 */
    fun sync(context: Context) {
        val store = SettingsStore.get(context)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context)
        am.cancel(pi)
        if (!store.ledgerReminderEnabled.value) return
        am.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerMillis(store.ledgerReminderHour.value, store.ledgerReminderMinute.value),
            pi
        )
    }

    /** 今天的提醒点还没到就排今天，已经过了就排明天 */
    internal fun nextTriggerMillis(
        hour: Int,
        minute: Int,
        now: LocalDateTime = LocalDateTime.now()
    ): Long {
        val at = now.toLocalDate().atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        val target = if (at.isAfter(now)) at else at.plusDays(1)
        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, LedgerReminderReceiver::class.java).apply {
            action = ACTION_REMIND
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
