package com.dailybook.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dailybook.app.MainActivity
import com.dailybook.app.R
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.i18n.AppStrings

/** 专注阶段结束时的通知与震动提醒 */
class Notifier(private val context: Context) {

    init {
        ensureChannels()
    }

    private fun ensureChannels() {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        // 渠道名跟随 App 内语言；同一个 id 再建一次就是改名
        val lang = SettingsStore.get(context).lang.value
        val focus = NotificationChannel(
            CHANNEL_ID,
            AppStrings.notifChannelFocus(lang),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = AppStrings.notifChannelFocusDesc(lang)
        }
        val todo = NotificationChannel(
            TODO_CHANNEL_ID,
            AppStrings.notifChannelTodo(lang),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = AppStrings.notifChannelTodoDesc(lang)
        }
        val ledger = NotificationChannel(
            LEDGER_CHANNEL_ID,
            AppStrings.notifChannelLedger(lang),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = AppStrings.notifChannelLedgerDesc(lang)
        }
        val summary = NotificationChannel(
            SUMMARY_CHANNEL_ID,
            AppStrings.summaryChannel(lang),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = AppStrings.summaryChannelDesc(lang)
        }
        nm.createNotificationChannel(focus)
        nm.createNotificationChannel(todo)
        nm.createNotificationChannel(ledger)
        nm.createNotificationChannel(summary)
    }

    fun notifyPhaseFinished(title: String, text: String) {
        // Android 13+ 未授权时直接返回（此处内联检查，便于 lint 静态分析）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFY_ID, notification)
        } catch (_: SecurityException) {
            // 权限可能在运行期间被用户撤销，忽略即可
        }
    }

    /** 待办到点提醒（每个待办一条，互不覆盖），带「稍后提醒」两个按钮 */
    fun notifyTodoDue(todoId: Long, title: String, text: String) {
        if (!canNotify()) return
        val lang = SettingsStore.get(context).lang.value
        val builder = NotificationCompat.Builder(context, TODO_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .addAction(
                0,
                AppStrings.snoozeOneHour(lang),
                TodoReminder.snoozeIntent(context, todoId, 60 * 60 * 1000L)
            )
            .addAction(
                0,
                AppStrings.snoozeTomorrowMorning(lang),
                TodoReminder.snoozeIntent(context, todoId, TodoReminder.millisUntilTomorrowMorning())
            )
        try {
            NotificationManagerCompat.from(context)
                .notify(TodoReminder.todoNotifyId(todoId), builder.build())
        } catch (_: SecurityException) {
            // 权限可能在运行期间被用户撤销，忽略即可
        }
    }

    /** 每周 / 每月小结 */
    fun notifySummary(weekly: Boolean, expense: String, income: String, minutes: Int, count: Int) {
        if (!canNotify()) return
        val lang = SettingsStore.get(context).lang.value
        notifyOn(
            channelId = SUMMARY_CHANNEL_ID,
            id = SUMMARY_NOTIFY_ID,
            title = AppStrings.summaryTitle(lang, weekly),
            text = AppStrings.summaryBody(lang, expense, income, minutes, count)
        )
    }

    /** 今天专注目标没达成 */
    fun notifyFocusGoal(done: Int, goal: Int) {
        if (!canNotify()) return
        val lang = SettingsStore.get(context).lang.value
        notifyOn(
            channelId = SUMMARY_CHANNEL_ID,
            id = FOCUS_GOAL_NOTIFY_ID,
            title = AppStrings.focusGoalTitle(lang),
            text = AppStrings.focusGoalBody(lang, done, goal)
        )
    }

    /** 预算预警：文案由调用方按当前语言组好（月度与分类两种情况） */
    fun notifyBudgetAlert(tag: String, title: String, text: String) {
        if (!canNotify()) return
        notifyOn(
            channelId = SUMMARY_CHANNEL_ID,
            id = BUDGET_NOTIFY_BASE + (tag.hashCode() and 0xFF),
            title = title,
            text = text
        )
    }

    private fun notifyOn(channelId: String, id: Int, title: String, text: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // 权限被撤销时忽略
        }
    }

    /** 每晚记账提醒 */
    fun notifyLedgerReminder() {
        if (!canNotify()) return
        val lang = SettingsStore.get(context).lang.value
        val notification = NotificationCompat.Builder(context, LEDGER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(AppStrings.notifLedgerTitle(lang))
            .setContentText(AppStrings.notifLedgerText(lang))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(LEDGER_NOTIFY_ID, notification)
        } catch (_: SecurityException) {
            // 同上
        }
    }

    /** 点通知直接回到 App */
    private fun openAppIntent(): PendingIntent {
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

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun vibrate() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (vibrator == null || !vibrator.hasVibrator()) return
        try {
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 180, 130, 180, 130, 340), -1)
            )
        } catch (_: Exception) {
            // 部分设备或省电模式下会被拒绝，忽略即可
        }
    }

    private companion object {
        const val CHANNEL_ID = "dailybook_focus_timer"
        const val TODO_CHANNEL_ID = "dailybook_todo_reminder"
        const val LEDGER_CHANNEL_ID = "dailybook_ledger_reminder"
        const val SUMMARY_CHANNEL_ID = "dailybook_summary"
        const val NOTIFY_ID = 1001
        const val LEDGER_NOTIFY_ID = 3000
        const val SUMMARY_NOTIFY_ID = 4000
        const val FOCUS_GOAL_NOTIFY_ID = 4001
        const val BUDGET_NOTIFY_BASE = 4100
    }
}
