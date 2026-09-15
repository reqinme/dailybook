package com.dailybook.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dailybook.app.MainActivity
import com.dailybook.app.data.AppDatabase
import com.dailybook.app.data.DateRepeat
import com.dailybook.app.data.ImportantDateEntity
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LifeStrings
import com.dailybook.app.util.ImportantDateSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 重要日期提醒到点：查一下这条日期现在还在不在，是就发一条通知，然后把**下一次**排上。
 *
 * 到点重新查库（而不是把标题塞进 Intent）有三个好处，和 [ClassReminderReceiver] 一样：
 * 1. 用户在闹钟排好之后改了名字 / 日期 / 提前天数，响出来的是最新的内容；
 * 2. 日期被删了、或者提醒被改到了别的日子，就直接不响 —— 不会留下幽灵提醒
 *    （判据是 [ImportantDateSchedule.reminderDayOf] 算出来的提醒日是不是**今天**，
 *    所以「当天提醒」（0 天）也照样算数，它只是提醒日 == 发生日而已）；
 * 3. 行已经不在了就什么都不做（[ImportantDateSchedule.fireMillis] 也算不出提醒时刻）。
 *
 * 通知自己建（没有走 [Notifier]）：[Notifier] 只暴露了几个具体场景的 helper，
 * 加一个 `importantDate(...)` 需要改 `Notifier.kt`，而那个文件不在本次改动的范围内。
 * 渠道的建法与命名风格和 [Notifier.ensureChannels] 保持一致（同 id 再建一次只是改名）。
 */
class ImportantDateReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 闹钟是谁排的、排的是哪条日期：只用来确认「这条日期还有效」，内容一律现查
        val dateId = intent.getLongExtra(ImportantDateReminder.EXTRA_DATE_ID, -1L)
        if (dateId <= 0L) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext
                // 闹钟到点后重新查库：日期被删掉了就什么都不做（不发任何通知）
                val item = AppDatabase.get(app).importantDateDao().getById(dateId) ?: return@launch

                val now = System.currentTimeMillis()
                val zone = ZoneId.systemDefault()
                // 只响「提醒日就是今天」的那一次：
                // 非精确闹钟可能被系统推迟几个小时，当天补一条提醒是对的；
                // 但如果已经拖到了第二天（或者这条日期被改到了过去），就什么都不发，只把下一次排上。
                val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                val occurrence = ImportantDateSchedule.nextOccurrence(item, today) ?: return@launch
                if (ImportantDateSchedule.reminderDayOf(item, occurrence) != today) return@launch

                // 同一次发生只通知一遍：App 在同一天里被重新打开时，排程会把它排成
                // 「5 秒后立刻响」，没有这一层就会变成一天响好几次
                val occurrenceDay = occurrence.toEpochDay()
                if (ImportantDateReminder.alreadyNotified(app, dateId, occurrenceDay)) return@launch

                // 没给通知权限时不记账：等用户授权之后这次提醒还能补上
                if (!canNotify(app)) return@launch

                val lang = SettingsStore.get(app).lang.value
                ensureChannel(app, lang)
                if (post(app, lang, item, today)) {
                    ImportantDateReminder.markNotified(app, dateId, occurrenceDay)
                }
            } catch (_: Exception) {
                // 提醒失败不该让 App 崩，静默即可（和 TodoReminderReceiver 一样）
            } finally {
                // 不管响没响成，都要把下一次排上；否则响过一次之后就再也不响了
                try {
                    ImportantDateReminder.reschedule(context)
                } catch (_: Exception) {
                    // reschedule 内部已经兜住了异常，这里只是双保险
                }
                result.finish()
            }
        }
    }

    /** Android 13+ 未授权时直接返回（此处内联检查，便于 lint 静态分析） */
    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * 通知里写「就是今天」还是「还有 N 天」。
     *
     * 用的是这条日期的**下一次发生**：提前 3 天提醒时用户想看到的是「还有 3 天」，
     * 而不是「就是今天」——后者是这个日期本身，不是提醒的这一天。
     * 名字后面接备注（如果有），这样一眼能分辨是哪条日期。
     */
    private fun bodyText(lang: Lang, item: ImportantDateEntity, today: LocalDate): String {
        // 算不出下一次（只有农历日期超出年份表才会发生）时就只说日期名，不编一个假的天数
        val occurrence = ImportantDateSchedule.nextOccurrence(item, today)
            ?: return item.title
        val daysLeft = ChronoUnit.DAYS.between(today, occurrence)
        val whenText = if (daysLeft <= 0L) LifeStrings.dateToday(lang)
        else LifeStrings.dateDaysLeft(lang, daysLeft)
        val note = item.note.trim()
        return if (note.isEmpty()) whenText
        else whenText + " · " + note
    }

    private fun ensureChannel(context: Context, lang: Lang) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            LifeStrings.notifChannelDate(lang),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = LifeStrings.notifChannelDateDesc(lang)
        }
        nm.createNotificationChannel(channel)
    }

    /** 发出通知；返回 true 表示真的发出去了（没权限 / 被系统拒绝时返回 false） */
    private fun post(context: Context, lang: Lang, item: ImportantDateEntity, today: LocalDate): Boolean {
        // 标题按「这一次是提前几天提醒的」选：
        // - 当天提醒（0 / 老数据里的负数）：直说「今天：X」——套「还有 0 天」那句话不通；
        // - 「只过一次」且提前了几天：「快到了：X」，它没有「还剩几天」的说法；
        // - 其余：`X 还有 N 天`。
        val title = when {
            item.remindDaysBefore <= 0 -> LifeStrings.dateNotifTitleToday(lang, item.title)
            item.repeatRule == DateRepeat.ONCE -> LifeStrings.dateNotifTitleOnce(lang, item.title)
            else -> LifeStrings.dateNotifTitle(lang, item.title, item.remindDaysBefore)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(bodyText(lang, item, today))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFY_ID, notification)
            true
        } catch (_: SecurityException) {
            // 权限可能在运行期间被用户撤销，忽略即可
            false
        }
    }

    /** 点通知回到 App（和 [Notifier] 里的做法一致） */
    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val CHANNEL_ID = "dailybook_date_reminder"

        /** 同时只响一条（排程也只留一个闹钟），所以固定一个通知 id 就够 */
        const val NOTIFY_ID = 6000
    }
}
