package com.dailybook.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import com.dailybook.app.MainActivity
import com.dailybook.app.R
import com.dailybook.app.data.AppDatabase
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.data.TxType
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.toLocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/**
 * 桌面小组件：今天花了多少、收了多少、还剩几件待办、专注了多久。
 *
 * 只用系统框架 API（AppWidgetProvider + RemoteViews），没有引入任何新依赖。
 *
 * 数据刷新有三条路：
 * 1. 系统按 widget_info.xml 里的 updatePeriodMillis（30 分钟，系统允许的最小值）唤起 onUpdate；
 * 2. App 写完成数据后调用 [WidgetProvider.refresh] 主动推一次；
 * 3. 用户把小组件拖到桌面时系统发一次 APPWIDGET_UPDATE。
 */
class WidgetProvider : AppWidgetProvider() {

    /**
     * onUpdate 跑在主线程上，查库必须挪到 IO；goAsync() 让系统知道「这次广播还没处理完」，
     * 处理完在 finally 里 result.finish()——跟 LedgerReminderReceiver 是同一套写法。
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val result = goAsync()
        // 挂起函数用的协程作用域不合适：onUpdate 返回后进程随时可能被回收，
        // 这里必须靠 goAsync() 把「还没结束」告诉系统。
        CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context)
            } catch (_: Exception) {
                // 小组件崩了会弹 ANR 对话框，这里绝不能让异常冒出去
            } finally {
                result.finish()
            }
        }
    }

    /** 自定义刷新广播（App 改完数据后发），只是把上面那套重跑一遍 */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_REFRESH) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context)
            } catch (_: Exception) {
                // 同上，异常不出接收器
            } finally {
                result.finish()
            }
        }
    }

    companion object {

        /** App 数据变化后广播这个 action，让小组件立刻刷新 */
        const val ACTION_REFRESH = "com.dailybook.app.WIDGET_REFRESH"

        /**
         * 给 App 用的刷新入口：广播一次 [ACTION_REFRESH]。
         *
         * 广播只投给自己 App（setPackage），不需要任何权限；没有小组件在桌面上时
         * 系统会直接丢掉，不会报错。
         */
        fun refresh(context: Context) {
            try {
                val intent = Intent(context, WidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                    setPackage(context.packageName)
                }
                context.sendBroadcast(intent)
            } catch (_: Exception) {
                // 刷新失败不该影响正在记账的界面
            }
        }

        /** 查库 + 画所有小组件实例 */
        private suspend fun render(context: Context) {
            val today = LocalDate.now()
            val db = AppDatabase.get(context)

            val transactions = db.transactionDao().getAll()
            val todos = db.todoDao().getAll()
            val sessions = db.focusSessionDao().getAll()

            var expenseCents = 0L
            var incomeCents = 0L
            for (tx in transactions) {
                if (tx.dateMillis.toLocalDate() != today) continue
                when (tx.typeName) {
                    TxType.EXPENSE.name -> expenseCents += tx.amountCents
                    TxType.INCOME.name -> incomeCents += tx.amountCents
                }
            }

            val todoCount = todos.count { !it.done }

            var focusMinutes = 0
            for (session in sessions) {
                if (session.startedAtMillis.toLocalDate() == today) focusMinutes += session.minutes
            }

            val views = buildViews(
                context = context,
                expenseCents = expenseCents,
                incomeCents = incomeCents,
                todoCount = todoCount,
                focusMinutes = focusMinutes
            )

            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, WidgetProvider::class.java)
            )
            for (id in ids) {
                try {
                    manager.updateAppWidget(id, views)
                } catch (_: Exception) {
                    // 单个实例更新失败就跳过，剩下的继续
                }
            }
        }

        /** 组装 RemoteViews：标题行 + 收支两列 + 待办/专注一行 */
        private fun buildViews(
            context: Context,
            expenseCents: Long,
            incomeCents: Long,
            todoCount: Int,
            focusMinutes: Int
        ): RemoteViews {
            // 桌面上的文字跟随**系统**语言（App 内语言设置管不到系统 UI）。
            // 若 App 内选的语言是系统认识的（中/日文），一并带上，尽量贴近 App 内所见。
            val strings = localizedContext(context)
            val views = RemoteViews(context.packageName, R.layout.widget_dailybook)

            views.setTextViewText(
                R.id.widget_expense_value,
                strings.getString(R.string.widget_money, formatAmount(expenseCents))
            )
            views.setTextViewText(
                R.id.widget_income_value,
                strings.getString(R.string.widget_money, formatAmount(incomeCents))
            )
            views.setTextViewText(
                R.id.widget_footer,
                strings.getString(R.string.widget_todo_focus, todoCount, focusMinutes)
            )

            // 点小组件任意位置都打开 App。FLAG_IMMUTABLE 是 Android 12+ 的硬要求，
            // FLAG_UPDATE_CURRENT 保证每次都用最新构造的 Intent。
            val launch = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            return views
        }

        /**
         * 用 App 内设置的语言重取一次资源（只在本机认识这个语言时才用），
         * 让小组件和通知一样能跟着 App 内语言走；否则回落到系统语言。
         */
        private fun localizedContext(context: Context): Context {
            return try {
                val tag = SettingsStore.get(context).lang.value.tag
                Locale.forLanguageTag(tag).let { locale ->
                    val config = Configuration(context.resources.configuration).apply {
                        setLocale(locale)
                    }
                    context.createConfigurationContext(config)
                }
            } catch (_: Exception) {
                context
            }
        }
    }
}
