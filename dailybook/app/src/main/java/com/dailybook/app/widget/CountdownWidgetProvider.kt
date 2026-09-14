package com.dailybook.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.View
import android.widget.RemoteViews
import com.dailybook.app.MainActivity
import com.dailybook.app.R
import com.dailybook.app.data.AppDatabase
import com.dailybook.app.data.ExamEntity
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.util.toLocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 桌面小组件（第二个）：考试倒计时 —— 四六级、考研这类「就盯着那一天」的考试。
 *
 * 只用系统框架 API（AppWidgetProvider + RemoteViews），没有引入任何新依赖，也没有新增权限。
 * 和 [WidgetProvider] 是同一套写法：goAsync() + IO 协程查库，最后 result.finish()。
 *
 * 一个实例只显示**一场**考试：考试名 + 日期 · 课程 · 地点 + 大号「N 天」（今天 / 已结束）。
 * 默认显示「最近一场还没开考的考试」；实例上有个「切换」热区，
 * 点一下换成下一场，并把选择记在这个实例自己的偏好里（[PREFS_NAME] 下的 exam_id_<appWidgetId>）。
 *
 * 刷新有三条路：
 * 1. 系统按 widget_countdown_info.xml 里的 updatePeriodMillis（30 分钟，系统允许的最小值）唤起 onUpdate；
 * 2. App 里考试数据变了，调 [CountdownWidgetProvider.refresh] 主动推一次；
 * 3. 用户点「切换」热区（[ACTION_NEXT]）。
 */
class CountdownWidgetProvider : AppWidgetProvider() {

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

    /**
     * 两条自定义广播都在这里收：
     * - [ACTION_REFRESH]：App 改完考试数据后发来，重画一遍；
     * - [ACTION_NEXT]：「切换」热区发来，带着 EXTRA_APPWIDGET_ID，让**这个实例**换下一场考试。
     *
     * 切换特意不做成跳 Activity：桌面上点一下还要拉起界面太打扰，
     * 直接换掉小组件上的内容最轻。所以这里用 getBroadcast + 显式组件（见 [switchIntent]）。
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action != ACTION_REFRESH && action != ACTION_NEXT) return

        // 只有「切换」才需要知道是哪个实例；刷新对所有实例生效。
        val cycleWidgetId = if (action == ACTION_NEXT) {
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        } else {
            AppWidgetManager.INVALID_APPWIDGET_ID
        }

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context, cycleWidgetId)
            } catch (_: Exception) {
                // 同上，异常不出接收器
            } finally {
                result.finish()
            }
        }
    }

    /**
     * 实例被从桌面删掉：把它记住的「当前考试」偏好一起清掉，别在 SharedPreferences 里留垃圾。
     * （不清也不影响显示，但删多了会一直攒无用的 exam_id_<id> 键。）
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        try {
            val editor = prefs(context).edit()
            for (id in appWidgetIds) editor.remove(keyFor(id))
            editor.apply()
        } catch (_: Exception) {
            // 清偏好失败无所谓，不能因为删小组件让 App 崩
        }
    }

    companion object {

        /** App 里考试数据变化后广播这个 action，让倒计时立刻刷新 */
        const val ACTION_REFRESH = "com.dailybook.app.COUNTDOWN_REFRESH"

        /** 桌面「切换」热区广播这个 action（携带 EXTRA_APPWIDGET_ID），换下一场考试 */
        const val ACTION_NEXT = "com.dailybook.app.COUNTDOWN_NEXT"

        /** 小组件自己的偏好文件：每个实例选了哪场考试都记在这里 */
        private const val PREFS_NAME = "dailybook_widget_countdown"

        /** 偏好键前缀，完整键名 = exam_id_<appWidgetId> */
        private const val KEY_PREFIX = "exam_id_"

        /** 「没存过 / 存的已经失效」的哨兵值：考试 id 是自增主键，从 1 起，不会有 0 或负数 */
        private const val NO_EXAM_ID = -1L

        /**
         * 给 App 用的刷新入口：广播一次 [ACTION_REFRESH]。
         *
         * 广播只投给自己 App（setPackage），不需要任何权限；桌面上没有这个小组件时
         * 系统会直接丢掉，不会报错。
         */
        fun refresh(context: Context) {
            try {
                val intent = Intent(context, CountdownWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                    setPackage(context.packageName)
                }
                context.sendBroadcast(intent)
            } catch (_: Exception) {
                // 刷新失败不该影响正在编辑考试的用户
            }
        }

        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private fun keyFor(appWidgetId: Int): String = KEY_PREFIX + appWidgetId

        /**
         * 查库 + 画所有小组件实例。
         *
         * [cycleWidgetId] 是刚被点了「切换」的那个实例；其余实例保持各自记住的选择。
         */
        private suspend fun render(
            context: Context,
            cycleWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, CountdownWidgetProvider::class.java)
            )
            // 桌面上一个实例都没有，连库都不用查
            if (ids.isEmpty()) return

            val today = LocalDate.now()
            val nowMillis = System.currentTimeMillis()
            // 只查一次库，然后按 / 画所有实例（每个实例选的考试可能不一样）
            val upcoming = AppDatabase.get(context).examDao().getAll()
                .filter { it.examMillis >= nowMillis }
                .sortedWith(compareBy({ it.examMillis }, { it.id }))

            // 桌面上的文字跟随**系统**语言（App 内语言设置管不到系统 UI）。
            // 若 App 内选的语言是系统认识的（中/日文），一并带上，尽量贴近 App 内所见。
            val localized = localizedContext(context)
            val store = prefs(context)

            for (id in ids) {
                try {
                    val exam = if (id == cycleWidgetId) {
                        cycleToNext(store, id, upcoming)
                    } else {
                        resolve(store, id, upcoming)
                    }
                    manager.updateAppWidget(
                        id,
                        buildViews(context, localized, id, exam, today, upcoming.size)
                    )
                } catch (_: Exception) {
                    // 单个实例更新失败就跳过，剩下的继续
                }
            }
        }

        /**
         * 这个实例该显示哪场考试：存过且**还没开考**就用存的，否则回落到最近的一场。
         *
         * 存的那场已经考完 / 被删掉时顺手清掉这条偏好，免得偏好里一直是失效的 id。
         */
        private fun resolve(
            store: SharedPreferences,
            appWidgetId: Int,
            upcoming: List<ExamEntity>
        ): ExamEntity? {
            if (upcoming.isEmpty()) return null
            val storedId = store.getLong(keyFor(appWidgetId), NO_EXAM_ID)
            val stored = upcoming.firstOrNull { it.id == storedId }
            if (stored != null) return stored
            if (storedId != NO_EXAM_ID) {
                store.edit().remove(keyFor(appWidgetId)).apply()
            }
            return upcoming.first()
        }

        /**
         * 「切换」：换到这个实例的下一场考试，并记住它。
         *
         * 从最后一场再点就绕回第一场（环形），这样无论几场考试都不用别的按钮。
         * 没存过（-1）时 (index + 1) % size == 0，正好落到最近的一场。
         */
        private fun cycleToNext(
            store: SharedPreferences,
            appWidgetId: Int,
            upcoming: List<ExamEntity>
        ): ExamEntity? {
            if (upcoming.isEmpty()) {
                store.edit().remove(keyFor(appWidgetId)).apply()
                return null
            }
            val storedId = store.getLong(keyFor(appWidgetId), NO_EXAM_ID)
            val index = upcoming.indexOfFirst { it.id == storedId }
            val next = upcoming[(index + 1) % upcoming.size]
            store.edit().putLong(keyFor(appWidgetId), next.id).apply()
            return next
        }

        /**
         * 组装 RemoteViews：标题行（含「切换」热区）+ 分隔线 + 考试名 / 大号天数 + 日期一行。
         *
         * [upcomingCount] 只有一场考试时用不到「切换」（点了也是原地打转），把热区藏起来。
         */
        private fun buildViews(
            context: Context,
            localized: Context,
            appWidgetId: Int,
            exam: ExamEntity?,
            today: LocalDate,
            upcomingCount: Int
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)
            views.setTextViewText(
                R.id.widget_countdown_title,
                localized.getString(R.string.widget_countdown_title)
            )

            if (exam == null) {
                // 一场没添加 / 全考完了：给一句人话，然后让整个小组件点一下打开 App。
                // App 的路由会落在默认标签页（首页），并没有直达「学习」页；
                // 这是可以接受的：用户进来自己切一下就行，比在这里猜路由、猜 tab 下标稳得多。
                views.setTextViewText(
                    R.id.widget_countdown_name,
                    localized.getString(R.string.widget_countdown_empty)
                )
                views.setTextViewText(
                    R.id.widget_countdown_meta,
                    localized.getString(R.string.widget_countdown_empty_hint)
                )
                views.setViewVisibility(R.id.widget_countdown_days, View.GONE)
                views.setViewVisibility(R.id.widget_countdown_switch, View.GONE)
            } else {
                // 整天数：和首页卡片一个口径（按本地日期算差，今天考 = 0）
                val days = ChronoUnit.DAYS.between(today, exam.examMillis.toLocalDate())
                val daysText = when {
                    days > 0L -> localized.getString(R.string.widget_countdown_days, days)
                    days == 0L -> localized.getString(R.string.widget_countdown_today)
                    else -> localized.getString(R.string.widget_countdown_passed)
                }

                views.setTextViewText(R.id.widget_countdown_name, exam.name)
                views.setTextViewText(R.id.widget_countdown_days, daysText)
                views.setTextViewText(R.id.widget_countdown_meta, metaLine(localized, exam))
                views.setViewVisibility(R.id.widget_countdown_days, View.VISIBLE)
                views.setViewVisibility(
                    R.id.widget_countdown_switch,
                    if (upcomingCount > 1) View.VISIBLE else View.GONE
                )
                views.setOnClickPendingIntent(
                    R.id.widget_countdown_switch,
                    switchIntent(context, appWidgetId)
                )
            }

            // 点小组件任意位置（除了「切换」热区）都打开 App。
            // FLAG_IMMUTABLE 是 Android 12+ 的硬要求，FLAG_UPDATE_CURRENT 保证每次都用最新构造的 Intent。
            views.setOnClickPendingIntent(
                R.id.widget_countdown_root,
                openIntent(context, appWidgetId)
            )
            return views
        }

        /** 小字那行：日期 · 课程 · 地点，空的自动省掉，不留多余的分隔点 */
        private fun metaLine(localized: Context, exam: ExamEntity): String {
            val parts = mutableListOf(formatExamDate(localized, exam.examMillis))
            if (exam.courseName.isNotBlank()) parts += exam.courseName.trim()
            if (exam.location.isNotBlank()) parts += exam.location.trim()
            return parts.joinToString(" · ")
        }

        /** 考试日期：交给系统按当前语言排版（中文 2026年9月13日 / 英文 Sep 13, 2026 / 日文 2026/09/13） */
        private fun formatExamDate(localized: Context, examMillis: Long): String {
            return try {
                val locale = localized.resources.configuration.locales[0]
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(locale)
                    .format(Instant.ofEpochMilli(examMillis).atZone(ZoneId.systemDefault()).toLocalDate())
            } catch (_: Exception) {
                // 兜底：ISO 日期，难看但不会让小组件空着
                examMillis.toLocalDate().toString()
            }
        }

        /** 整个小组件 → 打开 App（和 [WidgetProvider] 用同一套 Intent 写法） */
        private fun openIntent(context: Context, appWidgetId: Int): PendingIntent {
            val launch = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                appWidgetId,
                launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        /**
         * 「切换」热区 → 给**自己**发一条 [ACTION_NEXT] 广播。
         *
         * requestCode 必须带 appWidgetId：extras 不参与 PendingIntent 的身份判断，
         * 不区分 requestCode 的话多个实例会共用一个 PendingIntent，
         * 点谁都是同一个 id，切换就错位了。
         */
        private fun switchIntent(context: Context, appWidgetId: Int): PendingIntent {
            val cycle = Intent(context, CountdownWidgetProvider::class.java).apply {
                action = ACTION_NEXT
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setPackage(context.packageName)
            }
            return PendingIntent.getBroadcast(
                context,
                appWidgetId,
                cycle,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        /**
         * 用 App 内设置的语言重取一次资源（只在本机认识这个语言时才用），
         * 让小组件和通知一样能跟着 App 内语言走；否则回落到系统语言。
         * 跟 [WidgetProvider] 里那个私有实现是同一套（那边是 private，这里照抄一份）。
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
