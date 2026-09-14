package com.dailybook.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailybook.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 重启或 App 更新后，系统会清掉所有闹钟，这里重新排一遍待办提醒与上课提醒。
 * 即使这段没跑成，下次打开 App 时也会重新排程，所以提醒不会永久失效。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val todos = AppDatabase.get(context).todoDao().getAll()
                TodoReminder.sync(context, todos)
                LedgerReminder.sync(context)
                // 上课提醒同一时刻只挂「下一节」一个闹钟，重启后补排
                ClassReminder.reschedule(context)
            } catch (_: Exception) {
                // 排程失败无伤大雅，下次打开 App 会再排
            } finally {
                result.finish()
            }
        }
    }
}
