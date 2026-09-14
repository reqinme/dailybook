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

/** 专注阶段结束时的通知与震动提醒 */
class Notifier(private val context: Context) {

    init {
        ensureChannels()
    }

    private fun ensureChannels() {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val focus = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_description)
        }
        val todo = NotificationChannel(
            TODO_CHANNEL_ID,
            context.getString(R.string.todo_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.todo_channel_description)
        }
        val ledger = NotificationChannel(
            LEDGER_CHANNEL_ID,
            context.getString(R.string.ledger_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.ledger_channel_description)
        }
        nm.createNotificationChannel(focus)
        nm.createNotificationChannel(todo)
        nm.createNotificationChannel(ledger)
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

    /** 待办到点提醒（每个待办一条，互不覆盖） */
    fun notifyTodoDue(todoId: Long, title: String, text: String) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, TODO_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context)
                .notify(TODO_NOTIFY_BASE + (todoId % 100_000L).toInt(), notification)
        } catch (_: SecurityException) {
            // 权限可能在运行期间被用户撤销，忽略即可
        }
    }

    /** 每晚记账提醒 */
    fun notifyLedgerReminder() {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, LEDGER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("今天的账记了吗？")
            .setContentText("花一分钟记一下，月底就不会糊里糊涂")
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
        const val NOTIFY_ID = 1001
        const val TODO_NOTIFY_BASE = 2000
        const val LEDGER_NOTIFY_ID = 3000
    }
}
