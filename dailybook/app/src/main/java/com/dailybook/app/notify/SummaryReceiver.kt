package com.dailybook.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailybook.app.data.DailyRepository
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.data.SummaryMode
import com.dailybook.app.data.TxType
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.toLocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 每周 / 每月小结到点：现算这段时间的收支与专注，发一条通知，再排下一次 */
class SummaryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mode = SettingsStore.get(context).summaryMode.value
        if (mode == SummaryMode.OFF) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = DailyRepository(context).snapshot()
                val (from, to) = SummaryReminder.rangeFor(mode, java.time.LocalDate.now())

                val inRange = snapshot.transactions.filter { tx ->
                    val day = tx.dateMillis.toLocalDate()
                    !day.isBefore(from) && !day.isAfter(to)
                }
                val expense = inRange.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
                val income = inRange.filter { it.type == TxType.INCOME }.sumOf { it.amountCents }

                val sessions = snapshot.focusSessions.filter { session ->
                    val day = session.startedAtMillis.toLocalDate()
                    !day.isBefore(from) && !day.isAfter(to)
                }

                // 完全没数据就不打扰
                if (inRange.isNotEmpty() || sessions.isNotEmpty()) {
                    Notifier(context).notifySummary(
                        weekly = mode == SummaryMode.WEEKLY,
                        expense = formatAmount(expense),
                        income = formatAmount(income),
                        minutes = sessions.sumOf { it.minutes },
                        count = sessions.size
                    )
                }
            } catch (_: Exception) {
                // 算不出来就算了，静默
            } finally {
                // 无论如何都排下一次，避免汇总从此断掉
                SummaryReminder.sync(context)
                result.finish()
            }
        }
    }
}
