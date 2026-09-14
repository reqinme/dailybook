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
import com.dailybook.app.R
import com.dailybook.app.data.AppDatabase
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.SettingsStrings
import com.dailybook.app.util.ClassSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 上课提醒到点：查一下这节课现在还在不在，是就发一条通知，然后把**下一节**排上。
 *
 * 到点重新查库（而不是把课程名塞进 Intent）有两个好处：
 * 1. 用户在闹钟排好之后改了课名 / 地点 / 时间，响出来的是最新的内容；
 * 2. 课被删了、或者提醒开关被关了，就直接不响 —— 不会留下幽灵提醒。
 *
 * 通知自己建（没有走 [Notifier]）：[Notifier] 只暴露了几个具体场景的 helper，
 * 加一个 `classReminder(...)` 需要改 `Notifier.kt`，而那个文件不在本次改动的范围内。
 * 渠道的建法与命名风格和 [Notifier.ensureChannels] 保持一致（同 id 再建一次只是改名）。
 */
class ClassReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 闹钟是谁排的、排的是哪节课：只用来确认「这节课还有效」，内容一律现查
        val courseId = intent.getLongExtra(ClassReminder.EXTRA_COURSE_ID, -1L)
        if (courseId <= 0L) return
        val startMinutes = intent.getIntExtra(ClassReminder.EXTRA_START_MINUTES, ClassSchedule.NO_TIME)

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext
                val store = SettingsStore.get(app)
                // 到点时开关已经关了就什么都不做（用户可能刚关掉，闹钟还没来得及撤）
                if (!store.classReminder.value) return@launch

                val course = AppDatabase.get(app).courseDao().getById(courseId) ?: return@launch
                val lang = store.lang.value
                val minutes =
                    if (ClassSchedule.isValidTime(course.startMinutes)) course.startMinutes else startMinutes

                // 到点时课已经上完了就不响：闹钟可能被系统推迟（非精确闹钟允许），
                // 或者触发时刻被顶到了「现在」；这时候再提醒是纯打扰。
                // 用 CourseEntity 的估计下课时间判断，和排程那边同一套规则。
                if (isLessonOver(course)) return@launch

                ensureChannel(app, lang)
                post(app, lang, course.name, course.location, minutes)
            } catch (_: Exception) {
                // 提醒失败不该让 App 崩，静默即可（和 TodoReminderReceiver 一样）
            } finally {
                // 不管响没响成，都要把下一节排上；否则响过一次之后就再也不响了
                try {
                    ClassReminder.reschedule(context)
                } catch (_: Exception) {
                    // reschedule 内部已经兜住了异常，这里只是双保险
                }
                result.finish()
            }
        }
    }

    /**
     * 这节课现在是不是已经上完了。
     *
     * 用「最近一次该上课的日子」算它的起止时刻：今天是这节课的星期几就用今天，
     * 否则用上周同一天。没填下课时间就按开始 + 45 分钟估（与 [ClassSchedule] 一致）。
     * 判断只需要精确到「这节课有没有彻底过去」，所以这里用系统本地日期就够了。
     */
    private fun isLessonOver(course: com.dailybook.app.data.CourseEntity): Boolean {
        if (!ClassSchedule.isValidTime(course.startMinutes)) return false
        val zone = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zone)
        val daysSince = (today.dayOfWeek.value - course.dayOfWeek + 7) % 7
        val date = today.minusDays(daysSince.toLong())

        val start = date.atTime(course.startMinutes / 60, course.startMinutes % 60)
        val end = if (ClassSchedule.isValidTime(course.endMinutes) && course.endMinutes > course.startMinutes) {
            date.atTime(course.endMinutes / 60, course.endMinutes % 60)
        } else {
            start.plusMinutes(ClassSchedule.DEFAULT_LESSON_MINUTES.toLong())
        }
        val now = java.time.LocalDateTime.now(zone)
        return !now.isBefore(end)
    }

    private fun ensureChannel(context: Context, lang: com.dailybook.app.i18n.Lang) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            AppStrings.notifChannelClass(lang),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = AppStrings.notifChannelClassDesc(lang)
        }
        nm.createNotificationChannel(channel)
    }

    private fun post(
        context: Context,
        lang: com.dailybook.app.i18n.Lang,
        courseName: String,
        location: String,
        startMinutes: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val clock = ClassSchedule.formatMinutes(startMinutes)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(SettingsStrings.classReminderNotifyTitle(lang, clock, courseName))
            .setContentText(SettingsStrings.classReminderNotifyBody(lang, location))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFY_ID, notification)
        } catch (_: SecurityException) {
            // 权限可能在运行期间被用户撤销，忽略即可
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
        const val CHANNEL_ID = "dailybook_class_reminder"

        /** 同时只响一节课（排程也只留一个闹钟），所以固定一个通知 id 就够 */
        const val NOTIFY_ID = 5000
    }
}
