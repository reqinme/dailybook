package com.dailybook.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailybook.app.data.SettingsStore

/** 每晚记账提醒到点：发一条通知，并把下一次排上 */
class LedgerReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = SettingsStore.get(context)
        if (store.ledgerReminderEnabled.value) {
            Notifier(context).notifyLedgerReminder()
        }
        // 排下一天；如果这段时间里用户把开关关了，sync 会把闹钟撤掉
        LedgerReminder.sync(context)
    }
}
