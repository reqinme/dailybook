package com.dailybook.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailybook.app.backup.Backup
import com.dailybook.app.backup.AutoBackup
import com.dailybook.app.backup.BackupOutcome
import com.dailybook.app.backup.applyBackupSettings
import com.dailybook.app.backup.readBackupSettings
import com.dailybook.app.data.Accounts
import com.dailybook.app.data.CategoryStore
import com.dailybook.app.data.Currencies
import com.dailybook.app.data.DailyRepository
import com.dailybook.app.data.AwardEntity
import com.dailybook.app.data.AwardKind
import com.dailybook.app.data.CourseEntity
import com.dailybook.app.data.CreditTargetEntity
import com.dailybook.app.data.DateRepeat
import com.dailybook.app.data.ExamEntity
import com.dailybook.app.data.GradeEntity
import com.dailybook.app.data.HabitEntity
import com.dailybook.app.data.HabitLogEntity
import com.dailybook.app.data.ImportantDateEntity
import com.dailybook.app.data.MemoEntity
import com.dailybook.app.data.MilestoneEntity
import com.dailybook.app.data.ScoreKind
import com.dailybook.app.data.StudyTaskEntity
import com.dailybook.app.data.FocusSessionEntity
import com.dailybook.app.data.RepeatRule
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.data.SummaryMode
import com.dailybook.app.data.TodoEntity
import com.dailybook.app.data.TodoPriority
import com.dailybook.app.data.RecurringEntity
import com.dailybook.app.data.SubtaskEntity
import com.dailybook.app.widget.CountdownWidgetProvider
import com.dailybook.app.widget.WidgetProvider
import com.dailybook.app.data.TransactionEntity
import com.dailybook.app.data.TxType
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.notify.ClassReminder
import com.dailybook.app.notify.ImportantDateReminder
import com.dailybook.app.notify.LedgerReminder
import com.dailybook.app.notify.Notifier
import com.dailybook.app.notify.SummaryReminder
import com.dailybook.app.notify.TodoReminder
import com.dailybook.app.ui.theme.ThemeMode
// 「已修学分」口径的唯一实现（学分 > 0 且绩点 > 0）：成绩页、学分进度页与这里共用同一份，
// 界面层不再各自重算一遍
import com.dailybook.app.ui.study.earnedCredits
import com.dailybook.app.ui.study.earnedCreditsByCategory
import com.dailybook.app.util.ImportantDateSchedule
import com.dailybook.app.util.Lunar
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 「定量类习惯」的单位白名单（含繁体写法）：个 / 页。
 *
 * 只有这些单位的习惯才按「每天完成多少量」记，也就是学习周报里说的「定量计划」
 * （背单词 20 个、背书计划 3 页）。以前这里是 `unit != "次"` 的黑名单，
 * 于是「每天练琴 30 分钟」这种**按时间**记的习惯也被当成定量计划塞进周报，
 * 卡片标题却写着「背单词打卡」—— 单位不认识的就不算，宁缺勿滥；
 * 「次」本来就不在名单里，非定量的打卡习惯照旧被排除。
 */
private val COUNTABLE_HABIT_UNITS = setOf("个", "個", "页", "頁")

enum class TodoFilter {
    ALL,
    PENDING,
    DONE;

    fun label(lang: Lang): String = when (this) {
        ALL -> AppStrings.filterAll(lang)
        PENDING -> AppStrings.filterPending(lang)
        DONE -> AppStrings.filterDone(lang)
    }
}

/** 一天的分组（用于记账列表） */
@Immutable
data class DayGroup(
    val date: LocalDate,
    val expense: Long,
    val income: Long,
    val items: List<TransactionEntity>
)

/** 分类占比 */
@Immutable
data class CategorySlice(val category: String, val cents: Long, val ratio: Float)

/** 账户占比（支出按账户分布） */
@Immutable
data class AccountSlice(val account: String, val cents: Long, val ratio: Float)

/** 某个分类的预算执行情况 */
@Immutable
data class CategoryBudgetRow(
    val category: String,
    val budgetCents: Long,
    val spentCents: Long
) {
    val ratio: Float
        get() = if (budgetCents <= 0L) 0f
        else (spentCents.toFloat() / budgetCents.toFloat()).coerceIn(0f, 1f)

    val over: Boolean get() = budgetCents > 0L && spentCents > budgetCents

    val remainingCents: Long get() = (budgetCents - spentCents).coerceAtLeast(0L)

    /** 超支金额，没超就是 0 */
    val overCents: Long get() = (spentCents - budgetCents).coerceAtLeast(0L)
}

/** 专注热力图里的一格 */
@Immutable
data class HeatCell(val date: LocalDate, val minutes: Int)

/** 每日支出（柱状图） */
@Immutable
data class DayBar(val day: Int, val cents: Long)

/** 专注天数（柱状图） */
@Immutable
data class FocusDay(val date: LocalDate, val count: Int)

/** 看板分组 */
enum class TodoBucket { TODAY, THIS_WEEK, LATER, NO_DATE, DONE }

/**
 * 看板分组规则（抽成纯函数，方便直接测边界）：
 * 已完成的单独一堆；已过期和今天到期都算「今天到期」；7 天内算「本周内」；
 * 再往后算「以后」；没日期的单独一堆。
 */
fun bucketOf(todo: TodoEntity, today: LocalDate): TodoBucket = when {
    todo.done -> TodoBucket.DONE
    todo.dueMillis == null -> TodoBucket.NO_DATE
    else -> {
        val due = todo.dueMillis.toLocalDate()
        when {
            !due.isAfter(today) -> TodoBucket.TODAY
            due.isBefore(today.plusDays(7)) -> TodoBucket.THIS_WEEK
            else -> TodoBucket.LATER
        }
    }
}

@Immutable
data class TodoSection(val bucket: TodoBucket, val items: List<TodoEntity>)

/**
 * 一条重要日期的「下一次发生」。
 * [item] 是原始记录，[nextMillis] 是算出来的下一次公历日期（当天 00:00），
 * [daysLeft] 是距今天的天数（0 = 就是今天）。
 *
 * 「只过一次」（[DateRepeat.ONCE]）且已经过完的**不会**出现在 [UiState.upcomingDates] 里，
 * 而是进 [UiState.pastDates]（[daysLeft] 为负数），所以这里的 [daysLeft] 对
 * [UiState.upcomingDates] 来说总是 >= 0。
 */
@Immutable
data class UpcomingDate(val item: ImportantDateEntity, val nextMillis: Long, val daysLeft: Long)

/**
 * 一条重要日期的下一次发生日；农历与阳历的规则都收在 [ImportantDateSchedule] 里
 * （界面显示、提醒排程共用同一份实现，所以规则只留一处）。
 *
 * 返回 null 表示「算不出下一次」：农历超出 [Lunar] 的年份表，或「只过一次」已经过完
 * （过完的那种不再进倒计时列表，但会进 [UiState.pastDates]，不会凭空消失）。
 */
private fun nextOccurrenceDate(item: ImportantDateEntity, today: LocalDate): LocalDate? {
    val next = ImportantDateSchedule.nextOccurrence(item, today) ?: return null
    return next.takeIf { !it.isBefore(today) }
}

/** 环比：本月 / 上月、今年 / 去年 */
@Immutable
data class Comparison(
    val monthExpense: Long = 0L,
    val lastMonthExpense: Long = 0L,
    val monthIncome: Long = 0L,
    val lastMonthIncome: Long = 0L,
    val yearExpense: Long = 0L,
    val lastYearExpense: Long = 0L
) {
    /** 变化百分比；上月为 0 时返回 null（没有可比基数） */
    fun monthExpensePercent(): Int? = percentOf(lastMonthExpense, monthExpense)
    fun monthIncomePercent(): Int? = percentOf(lastMonthIncome, monthIncome)
    fun yearExpensePercent(): Int? = percentOf(lastYearExpense, yearExpense)

    private fun percentOf(base: Long, now: Long): Int? {
        if (base <= 0L) return null
        return (((now - base).toDouble() / base.toDouble()) * 100).toInt()
    }
}

/** 智能洞察：只给结构化数据，句子在界面层按语言拼 */
enum class InsightKind { SPENT_MORE, SPENT_LESS, NO_RECORD_DAYS, TOP_CATEGORY, BUDGET_LEFT }

@Immutable
data class Insight(
    val kind: InsightKind,
    val amountCents: Long = 0L,
    val category: String = "",
    val days: Int = 0
)

/** 日历视图里的一天 */
@Immutable
data class CalendarDay(
    val date: LocalDate,
    val expenseCents: Long,
    val incomeCents: Long,
    val count: Int
)

/** 某个待办累计投入的专注时间 */
@Immutable
data class TodoFocus(val title: String, val minutes: Int, val count: Int)

/** 月度收支（近 12 个月趋势图） */
@Immutable
data class MonthBar(val month: YearMonth, val expense: Long, val income: Long) {
    val label: String get() = "${month.monthValue}月"
}

/**
 * 年度汇总。
 *
 * [year] 是**选中月所在的那一年**（不是「今天所在的年」）：统计页上「N 年汇总」的标题、
 * 同比卡片里的「今年 / 去年」和 [monthBars] 的最后一根柱子都跟着它走，
 * 所以翻月份时这一页只讲一个年份。
 */
@Immutable
data class YearSummary(
    val year: Int = YearMonth.now().year,
    val income: Long = 0L,
    val expense: Long = 0L,
    val count: Int = 0,
    val topCategory: String = "",
    val topCategoryCents: Long = 0L,
    val monthBars: List<MonthBar> = emptyList()
) {
    val balance: Long get() = income - expense
    val hasData: Boolean get() = count > 0
    val maxBarCents: Long get() = monthBars.maxOfOrNull { maxOf(it.expense, it.income) } ?: 0L
}

/** 专注统计（全部由 focus_sessions 表派生） */
@Immutable
data class FocusStats(
    val todayCount: Int = 0,
    val todayMinutes: Int = 0,
    val totalCount: Int = 0,
    val streak: Int = 0,
    val weekCount: Int = 0,
    val weekMinutes: Int = 0,
    val monthCount: Int = 0,
    val monthMinutes: Int = 0,
    val recentDays: List<FocusDay> = emptyList(),
    val todaySessions: List<FocusSessionEntity> = emptyList(),
    /** 选中月份（[UiState.month]）的全部专注记录：统计详情页要按月列出（不只是今天）。
     *  [monthCount] / [monthMinutes] 也是同一批记录的汇总，所以切月时三个数一起变。 */
    val monthSessions: List<FocusSessionEntity> = emptyList(),
    /**
     * 最近 7 天（含今天）完成的专注记录，**与选中的月份无关**。
     *
     * 学习周报声明的是「近 7 天」，所以它必须用这一份：用 [monthSessions] 的话，
     * 用户把选中月切到别的月份时这 7 天会突然变成 0，而同一页的柱状图还有数据（数字与图打架）。
     */
    val last7Sessions: List<FocusSessionEntity> = emptyList(),
    /** 近 12 周热力图：外层是周（列），内层是周一到周日（行） */
    val heatWeeks: List<List<HeatCell>> = emptyList(),
    val heatMaxMinutes: Int = 0,
    /** 按待办汇总的投入时间（只含有标题的那些记录），从多到少 */
    val perTodo: List<TodoFocus> = emptyList()
)

@Immutable
data class UiState(
    val month: YearMonth = YearMonth.now(),
    /**
     * 记账页流水列表的分组：**带筛选**（搜索 / 账户 / 标签 / 按天），只喂记账页自己的那个列表。
     *
     * 它和 [monthExpense] / [monthIncome] / [monthCount] / [expenseSlices] / [calendarDays]
     * **不是**同一份数据 —— 后面这些都是整月口径。拿这份分组去数笔数就会和整月汇总打架
     * （「上面写本月 42 笔，下面只列 1 行」就是这么来的）。
     * 统计详情页的三份明细请用 [monthAllGroups]，**不要把这两个字段合并回去**。
     */
    val monthGroups: List<DayGroup> = emptyList(),
    /**
     * 整月分组：**不带任何筛选**，和 [monthExpense] / [monthIncome] / [monthCount] /
     * [expenseSlices] / [calendarDays] 出自同一份整月流水（同一个 `groupBy` 形状，
     * 所以界面可以在两者之间直接换用）。
     *
     * 统计详情页（本月记录 / 分类明细 / 每日明细）只讲「选中的这一个月」，
     * 跟记账页当前的搜索 / 账户 / 标签 / 某天筛选毫无关系，三份明细都读这一份；
     * 记账页的列表仍旧读 [monthGroups]（那里本来就该跟着筛选走）。
     */
    val monthAllGroups: List<DayGroup> = emptyList(),
    val monthExpense: Long = 0L,
    val monthIncome: Long = 0L,
    val monthCount: Int = 0,
    val todayExpense: Long = 0L,
    val expenseSlices: List<CategorySlice> = emptyList(),
    val incomeSlices: List<CategorySlice> = emptyList(),
    val dayBars: List<DayBar> = emptyList(),
    val maxDayCents: Long = 0L,
    val ledgerQuery: String = "",
    val isSearching: Boolean = false,
    val budgetCents: Long = 0L,
    /** 全部出现过的账户，默认账户排最前 */
    val accounts: List<String> = emptyList(),
    /** 记账页当前选中的账户筛选，null 表示全部 */
    val accountFilter: String? = null,
    /** 标签筛选，null 表示全部 */
    val tagFilter: String? = null,
    /** 日历里点选的某一天，null 表示不按天过滤 */
    val dayFilter: LocalDate? = null,
    /** 数据里出现过的全部标签（按出现次数排序） */
    val allTags: List<String> = emptyList(),
    /** 可选分类：用户自定义清单 ∪ 数据里出现过的分类 */
    val expenseCategories: List<String> = emptyList(),
    val incomeCategories: List<String> = emptyList(),
    /** 当月日历（只含当月天） */
    val calendarDays: List<CalendarDay> = emptyList(),
    val maxCalendarExpense: Long = 0L,
    /** 待报销 / 已报销汇总（全部时间口径） */
    val pendingReimbursementCents: Long = 0L,
    val pendingReimbursementCount: Int = 0,
    val reimbursedCents: Long = 0L,
    /** 每条待办下的子任务 */
    val subtasksByTodo: Map<Long, List<SubtaskEntity>> = emptyMap(),
    /** 看板分组（今天 / 本周 / 以后 / 没日期 / 已完成） */
    val kanbanSections: List<TodoSection> = emptyList(),
    /** 周期记账规则 */
    val recurring: List<RecurringEntity> = emptyList(),
    /** 环比数据 */
    val comparison: Comparison = Comparison(),
    /** 智能洞察（最多 4 条） */
    val insights: List<Insight> = emptyList(),
    /** 本月支出按账户分布 */
    val accountSlices: List<AccountSlice> = emptyList(),
    /** 设了预算的分类，按使用比例从高到低 */
    val categoryBudgets: List<CategoryBudgetRow> = emptyList(),
    val visibleTodos: List<TodoEntity> = emptyList(),
    val todoFilter: TodoFilter = TodoFilter.ALL,
    val todoQuery: String = "",
    val pendingCount: Int = 0,
    val doneCount: Int = 0,
    val focusTaskId: Long = SettingsStore.NO_TASK,
    val focusTaskTitle: String = "",
    val focusStats: FocusStats = FocusStats(),
    val yearSummary: YearSummary = YearSummary(),
    val totalIncomeCents: Long = 0L,
    val totalExpenseCents: Long = 0L,
    val totalCount: Int = 0,
    // ---- 生活模块：备忘录 / 大事记 / 重要日期 / 习惯打卡 ----
    val memos: List<MemoEntity> = emptyList(),
    val milestones: List<MilestoneEntity> = emptyList(),
    val importantDates: List<ImportantDateEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
    /**
     * 每个习惯今天的完成量：habitId → 今天所有打卡记录的**合计**（没打卡 = 0）。
     *
     * 同一「天」不保证只有一条记录（恢复备份 / 导入会带进来重复行，表里也没有唯一约束），
     * 所以这里按「同一天的多条求和」读，和 [com.dailybook.app.data.HabitDao.countOf]、
     * 习惯页的 7 格小图同一口径 —— 以前取的是 `firstOrNull`，一天两条时界面就会自我矛盾。
     */
    val habitToday: Map<Long, Int> = emptyMap(),
    /** 每个习惯的连续天数：habitId → 连续打卡天数（今天还没打卡则从昨天起算） */
    val habitStreak: Map<Long, Int> = emptyMap(),
    /** 每个习惯本周已打卡的天数：habitId → 本周（周一起）打过卡的不同日期数 */
    val habitWeekDone: Map<Long, Int> = emptyMap(),
    /** 重要日期的下一次发生，按还剩几天从近到远排；只含「今天或今天之后」的（[UpcomingDate.daysLeft] >= 0） */
    val upcomingDates: List<UpcomingDate> = emptyList(),
    /**
     * 「只过一次」且已经过完的重要日期，[UpcomingDate.daysLeft] 是负数。
     *
     * 单列一份而不是混进 [upcomingDates]：顶部大卡片与「下一个纪念日」的语义是
     * 「下一次还没发生的日子」，混进去会让倒计时卡片显示一个已经过去的日子；
     * 但这些记录也**不该**凭空消失（以前就是那样），所以在列表里单独成组、明确标成已过去。
     */
    val pastDates: List<UpcomingDate> = emptyList(),
    /**
     * 定量类习惯（单位是**可数**的 个 / 页，见 [COUNTABLE_HABIT_UNITS]），
     * 也就是学习周报里说的「定量计划」（背单词 / 背书计划）。
     *
     * 只认白名单里的单位：「每天练琴 30 分钟」这类按时间记的习惯不算定量计划，
     * 否则它会在周报里被当成一个背单词计划列出来（单位不认识的不算）。
     */
    val wordHabits: List<HabitEntity> = emptyList(),
    // ---- 学习模块：课表 / 考试 / 作业 / 成绩 / 学分 / 奖助 ----
    val courses: List<CourseEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val studyTasks: List<StudyTaskEntity> = emptyList(),
    val grades: List<GradeEntity> = emptyList(),
    val creditTargets: List<CreditTargetEntity> = emptyList(),
    val awards: List<AwardEntity> = emptyList(),
    /** 最近一场还没开考的考试，没有就是 null */
    val nextExam: ExamEntity? = null,
    /** 距那场考试还有几天（今天考 = 0，没有考试也是 0） */
    val examDaysLeft: Long = 0L,
    /** 作业 / DDL 子集：挂了课程名的那部分待办 */
    val assignmentTodos: List<TodoEntity> = emptyList(),
    /** 其中还没做完、且到期日已经过去的条数 */
    val overdueAssignments: Int = 0,
    /** 全部流水（不筛选、不按月）：学习周报这类「近 7 天」统计要用 */
    val transactions: List<TransactionEntity> = emptyList(),
    /** 全部待办（不筛选）：周报与完成率统计要用 */
    val todos: List<TodoEntity> = emptyList(),
    /** 加权平均绩点：Σ(绩点 × 学分) / Σ学分，没有学分记录时是 0 */
    val gpa: Double = 0.0,
    /**
     * GPA 计算口径：4.0（默认）或 5.0，来自设置里的 [SettingsStore.gpaScale]。
     *
     * **只决定「原始分数 / 等级怎么换算成绩点」**（成绩页录入时的预览与保存），
     * 已经存下来的 [GradeEntity.point] 是多少就是多少，GPA 永远按它加权算 ——
     * 所以切换口径不会回头改写历史成绩，界面也不会再自己猜一个口径出来。
     */
    val gpaScale: Double = 4.0,
    /**
     * 已修学分总和。
     *
     * 口径：**学分 > 0 且绩点 > 0** 才算拿到学分（和成绩页的 `earnedCredit`、
     * 学分进度页完全一致）。不及格（绩点 0.00）与「还没出分」的课都不计入 ——
     * 以前这里是 `sumOf { it.credit }`，一门 59 分的课照样算进已修学分，
     * 甚至能把某个类别顶成「已达标」，而同一批成绩的 GPA 却把它算 0。
     */
    val totalCredits: Double = 0.0,
    /**
     * 每个课程类别已修的学分：类别 → 学分。
     *
     * 口径同 [totalCredits]（学分 > 0 且绩点 > 0），和学分进度页的分类进度对得上。
     */
    val creditsByCategory: Map<String, Double> = emptyMap(),
    /** 今天的专注时长（分钟），和 focusStats.todayMinutes 同源 */
    val studyMinutesToday: Int = 0
) {
    val balance: Long get() = monthIncome - monthExpense
    val hasMonthData: Boolean get() = monthGroups.isNotEmpty()

    val hasBudget: Boolean get() = budgetCents > 0L

    /** 预算使用比例，未设置预算时为 0 */
    val budgetRatio: Float
        get() = if (budgetCents <= 0L) 0f
        else (monthExpense.toFloat() / budgetCents.toFloat()).coerceIn(0f, 1f)

    val budgetRemainingCents: Long get() = (budgetCents - monthExpense).coerceAtLeast(0L)

    val overBudget: Boolean get() = budgetCents > 0L && monthExpense > budgetCents

    /** 有分类预算超支了吗 */
    val hasOverBudgetCategory: Boolean get() = categoryBudgets.any { it.over }

    /** 有没有设过分类预算 */
    val hasCategoryBudget: Boolean get() = categoryBudgets.isNotEmpty()

    /** 有历史数据里出现过的标签 */
    val hasTags: Boolean get() = allTags.isNotEmpty()

    val hasReimbursement: Boolean get() = pendingReimbursementCount > 0 || reimbursedCents > 0L

    /** 记账页是否处于「筛选后」的状态 */
    val isLedgerFiltered: Boolean
        get() = isSearching || accountFilter != null || tagFilter != null || dayFilter != null

    /** 有史以来的净结余 */
    val totalBalance: Long get() = totalIncomeCents - totalExpenseCents
}

/** 记账侧的输入聚合，避免每次重组都重新拼装大量 flow */
private data class LedgerInputs(
    val all: List<TransactionEntity>,
    val month: YearMonth,
    val query: String,
    val budgetCents: Long,
    val accountFilter: String?,
    val tagFilter: String?,
    val dayFilter: LocalDate?,
    val categoryBudgets: Map<String, Long>
)

/** 只跟筛选有关的设置项，单独拼一层，避免 combine 超过 5 个参数 */
private data class LedgerFilters(
    val account: String?,
    val categoryBudgets: Map<String, Long>,
    val tag: String?,
    val day: LocalDate?
)

/** 用户自定义的分类清单 */
private data class CategoryInputs(val expense: List<String>, val income: List<String>)

/** 待办侧的输入聚合 */
private data class TodoInputs(
    val all: List<TodoEntity>,
    val filter: TodoFilter,
    val query: String,
    val subtasks: List<SubtaskEntity>,
    val recurring: List<RecurringEntity>
)

/** 生活模块的输入聚合（备忘录 / 大事记 / 重要日期 / 习惯与打卡） */
private data class LifeInputs(
    val memos: List<MemoEntity>,
    val milestones: List<MilestoneEntity>,
    val dates: List<ImportantDateEntity>,
    val habits: List<HabitEntity>,
    val habitLogs: List<HabitLogEntity>
)

/** 学习模块的输入聚合（课表 / 考试 / 复习计划 / 成绩 / 奖助 / 学分要求） */
private data class StudyInputs(
    val courses: List<CourseEntity>,
    val exams: List<ExamEntity>,
    val studyTasks: List<StudyTaskEntity>,
    val grades: List<GradeEntity>,
    val awards: List<AwardEntity>,
    val creditTargets: List<CreditTargetEntity>,
    /** GPA 口径（4.0 / 5.0）：界面换算分数 → 绩点时要和设置里选的一致 */
    val gpaScale: Double
)

/** 专注侧的输入聚合：记录 + 当前目标 + 分类清单，凑一层免得 combine 超过 5 个 */
private data class FocusSettings(
    val sessions: List<FocusSessionEntity>,
    val taskId: Long,
    val taskTitle: String,
    val cats: CategoryInputs
)

/**
 * 预算预警的档位（由低到高）。
 * [NEAR]：本月支出用到预算的 80%；[OVER]：已经超支。
 * 判定只在这里出现一次（[MainViewModel.budgetTierOf]），别在别处再写一遍阈值。
 */
private enum class BudgetTier { NEAR, OVER }

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repo = DailyRepository(app)

    /** 用户可自定义的分类清单 */
    val categories: CategoryStore = CategoryStore.get(app)

    /** 全局共享的设置存储（与 TimerViewModel 拿到的是同一个实例） */
    val settings: SettingsStore = SettingsStore.get(app)

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val todoFilter = MutableStateFlow(TodoFilter.ALL)
    private val ledgerQuery = MutableStateFlow("")
    private val todoQuery = MutableStateFlow("")
    private val ledgerAccount = MutableStateFlow<String?>(null)
    private val ledgerTag = MutableStateFlow<String?>(null)
    private val ledgerDay = MutableStateFlow<LocalDate?>(null)

    private val ledgerFilters = combine(
        ledgerAccount,
        settings.categoryBudgets,
        ledgerTag,
        ledgerDay
    ) { account, budgets, tag, day ->
        LedgerFilters(account, budgets, tag, day)
    }

    private val ledgerInputs = combine(
        repo.transactions,
        selectedMonth,
        ledgerQuery,
        settings.monthlyBudgetCents,
        ledgerFilters
    ) { transactions, month, query, budget, filters ->
        LedgerInputs(
            all = transactions,
            month = month,
            query = query,
            budgetCents = budget,
            accountFilter = filters.account,
            tagFilter = filters.tag,
            dayFilter = filters.day,
            categoryBudgets = filters.categoryBudgets
        )
    }

    private val categoryInputs = combine(
        categories.expense,
        categories.income
    ) { expense, income ->
        CategoryInputs(expense, income)
    }

    private val settingsInputs = combine(
        settings.focusTaskId,
        settings.focusTaskTitle
    ) { id, title ->
        id to title
    }

    private val todoInputs = combine(
        repo.todos,
        todoFilter,
        todoQuery,
        repo.subtasks,
        repo.recurring
    ) { todos, filter, query, subtasks, recurring ->
        TodoInputs(todos, filter, query, subtasks, recurring)
    }

    private val lifeInputs = combine(
        repo.memos,
        repo.milestones,
        repo.importantDates,
        repo.habits,
        repo.habitLogs
    ) { memos, milestones, dates, habits, habitLogs ->
        LifeInputs(memos, milestones, dates, habits, habitLogs)
    }

    // 奖助与学分要求先自己拼一层：这样下面那层 combine 全是 5 个流凑出来的
    private val studyExtras = combine(repo.awards, repo.creditTargets) { a, t -> a to t }

    // GPA 口径来自设置（设置页写 SettingsStore.gpaScale），成绩页只读它 ——
    // 以前界面是「有哪条绩点超过 4.0 就猜成 5.0」，用户选了 5.0 也会被自己的旧数据翻回 4.0。
    //
    // 注意：`combine` 只提供到 **5 个流**的重载，写成 6 个编译不过（会报 SuspendFunction6 与
    // SuspendFunction1 不匹配）。所以把「成绩 + 口径」先拼成一层，下面那层就还是 5 个。
    private val studyGrades = combine(repo.grades, settings.gpaScale) { grades, scale -> grades to scale }

    private val studyInputs = combine(
        repo.courses,
        repo.exams,
        repo.studyTasks,
        studyExtras,
        studyGrades
    ) { courses, exams, studyTasks, extras, grades ->
        StudyInputs(
            courses = courses,
            exams = exams,
            studyTasks = studyTasks,
            grades = grades.first,
            awards = extras.first,
            creditTargets = extras.second,
            gpaScale = grades.second
        )
    }

    private val focusSettings = combine(
        repo.focusSessions,
        settingsInputs,
        categoryInputs
    ) { s, task, cats ->
        FocusSettings(s, task.first, task.second, cats)
    }

    val uiState: StateFlow<UiState> = combine(
        ledgerInputs,
        todoInputs,
        lifeInputs,
        studyInputs,
        focusSettings
    ) { ledger, todos, life, study, focus ->
        buildState(ledger, todos, life, study, focus)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    /** 操作结果提示（导出成功、导入失败之类），界面弹完即清空 */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    init {
        // 打开 App 先把到期的周期记账补成真实记录（补完会把下次日期往后推，不会重复记）
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.materializeRecurring()
                // 补记的几笔会真的改变本月支出（长期没用 App 时一次可能跨过预算的两档）
                checkBudgetAlert()
            }
        }
        // 顺带跑一次自动备份：24 小时内已备份过会自动跳过，没配置就什么都不做
        viewModelScope.launch {
            withContext(Dispatchers.IO) { AutoBackup.runBackupIfDue(app) }
        }
        // 数据一变、或者**界面语言一换**就刷新桌面小组件（没放小组件时 refresh 内部会直接返回，
        // 开销可忽略）。语言也要在这里听着：小组件上的文字是它自己按当前语言取的资源
        // （见 WidgetProvider.localizedContext），而系统最长 30 分钟才唤起一次 onUpdate ——
        // 不主动推一把的话，换了语言后桌面上会挂着旧语言半小时。
        viewModelScope.launch {
            combine(
                repo.transactions,
                repo.todos,
                repo.focusSessions,
                repo.exams,
                settings.lang
            ) { _, _, _, _, _ -> Unit }.collect {
                WidgetProvider.refresh(app)
                // 考试倒计时小组件跟着考试数据一起刷新
                CountdownWidgetProvider.refresh(app)
            }
        }
        // 待办一变就重排提醒：完成 / 删除 / 改期都会自动撤销或顺延，不会留下幽灵提醒
        viewModelScope.launch {
            repo.todos.collect { todos ->
                withContext(Dispatchers.IO) { TodoReminder.sync(app, todos) }
            }
        }
        // 每晚记账提醒 / 每日专注目标：开关、时间、目标一变就重排闹钟
        viewModelScope.launch {
            combine(
                settings.ledgerReminderEnabled,
                settings.ledgerReminderHour,
                settings.ledgerReminderMinute,
                settings.focusGoal
            ) { _, _, _, _ -> Unit }.collect {
                withContext(Dispatchers.IO) { LedgerReminder.sync(app) }
            }
        }
        // 定期小结：模式一变就重排
        viewModelScope.launch {
            settings.summaryMode.collect {
                withContext(Dispatchers.IO) { SummaryReminder.sync(app) }
            }
        }
        // 上课提醒：课表（增删改都算）、开关、提前量、**学期起始日**任一变化就重排。
        // 订阅时 courses 会先发一次当前值，所以「打开 App 重排一次」也由它兜住了；
        // 排程内部会先撤旧闹钟，重复触发不会堆积。
        //
        // 学期起始日必须订阅：ClassSchedule.nextOccurrence 用它把「第几周」换算成真实日期
        // （`fallbackTermStartMillis = store.termStartMillis`），漏了它就会出现
        // 「改了学期起始日，已经排好的闹钟还按旧的周次响（单周的课在双周响）」。
        viewModelScope.launch {
            combine(
                repo.courses,
                settings.classReminder,
                settings.classReminderMinutes,
                settings.termStartMillis
            ) { _, _, _, _ -> Unit }.collect {
                withContext(Dispatchers.IO) { ClassReminder.reschedule(app) }
            }
        }
        // 重要日期的「提前 N 天提醒」：日期增删改（提前量、重复规则、历法都算）任一变化就重排。
        // 和上课提醒一样「同一时刻只挂一个闹钟」，订阅时先发一次当前值，
        // 所以「打开 App 补排一次 + 响过之后由接收器自己续排」这两条路都通了。
        viewModelScope.launch {
            repo.importantDates.collect {
                withContext(Dispatchers.IO) { ImportantDateReminder.reschedule(app) }
            }
        }
    }

    // ---- 月份切换 ----

    // 切月时**必须**清掉「按某一天筛选」：否则筛选还停在旧月份的那一天，
    // 新月份里查不到那一天，列表直接空掉，而那张筛选卡还会显示「0 笔 · ¥0.00」自相矛盾。
    fun previousMonth() {
        ledgerDay.value = null
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        ledgerDay.value = null
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    /** 跳到指定月份：年月快速切换器（记账页与统计页共用）用的 */
    fun moveToMonth(target: YearMonth) {
        ledgerDay.value = null
        selectedMonth.value = target
    }

    fun goToCurrentMonth() {
        ledgerDay.value = null
        selectedMonth.value = YearMonth.now()
    }

    // ---- 搜索 / 筛选 ----

    fun setLedgerQuery(query: String) { ledgerQuery.value = query }

    /** 记账页按账户筛选；传 null 表示看全部 */
    fun setAccountFilter(account: String?) { ledgerAccount.value = account }

    /** 按标签筛选 */
    fun setTagFilter(tag: String?) { ledgerTag.value = if (ledgerTag.value == tag) null else tag }

    /** 日历里点选某天（再点一次取消） */
    fun setDayFilter(day: LocalDate?) {
        ledgerDay.value = if (day != null && ledgerDay.value == day) null else day
    }

    /** 标记一笔是否已报销 */
    fun toggleReimbursed(item: TransactionEntity) {
        viewModelScope.launch { repo.setReimbursed(item, !item.reimbursed) }
    }

    // ---- 自定义分类 ----

    fun addCategory(type: TxType, name: String): Boolean = categories.add(type, name)

    fun removeCategory(type: TxType, name: String) = categories.remove(type, name)

    fun resetCategories(type: TxType) = categories.reset(type)

    // ---- 子任务 ----

    fun addSubtask(todoId: Long, title: String) {
        viewModelScope.launch { repo.addSubtask(todoId, title) }
    }

    fun toggleSubtask(item: SubtaskEntity) {
        viewModelScope.launch { repo.setSubtaskDone(item, !item.done) }
    }

    fun deleteSubtask(item: SubtaskEntity) {
        viewModelScope.launch { repo.deleteSubtask(item) }
    }

    // ---- 优先级与排序 ----

    fun setTodoPriority(item: TodoEntity, priority: TodoPriority) {
        viewModelScope.launch { repo.updateTodo(item.copy(priority = priority.name)) }
    }

    /** 列表拖动结束后按新顺序重排 */
    fun reorderTodos(ordered: List<TodoEntity>) {
        viewModelScope.launch { repo.reorderTodos(ordered) }
    }

    // ---- 周期记账 ----

    fun addRecurring(
        amountCents: Long,
        type: TxType,
        category: String,
        account: String,
        note: String,
        tags: List<String>,
        rule: RepeatRule,
        nextDueMillis: Long
    ) {
        viewModelScope.launch {
            repo.addRecurring(amountCents, type, category, account, note, tags, rule, nextDueMillis)
            withContext(Dispatchers.IO) {
                repo.materializeRecurring()
                // 补记出来的是真实的支出记录：可能一次就把本月预算跨过去
                checkBudgetAlert()
            }
        }
    }

    // ---- 自动备份 ----

    /** 设置里点「立即备份」：绕过 24 小时限制，结果按实际情况回报（失败不会说成成功） */
    fun backupNow() {
        viewModelScope.launch {
            val lang = settings.lang.value
            val outcome = withContext(Dispatchers.IO) { AutoBackup.get(app).runBackupNow() }
            _message.value = when (outcome) {
                BackupOutcome.SUCCESS -> AppStrings.autoBackupDone(lang)
                BackupOutcome.NOT_CONFIGURED -> AppStrings.autoBackupNeedsFolder(lang)
                BackupOutcome.NOT_DUE -> AppStrings.autoBackupNotNow(lang)
                BackupOutcome.FAILED ->
                    AppStrings.backupFailure(lang, AutoBackup.get(app).lastFailureReason()?.name)
            }
        }
    }

    // ---- 周期记账 ----

    fun deleteRecurring(item: RecurringEntity) {
        viewModelScope.launch { repo.deleteRecurring(item) }
    }

    /** 暂停 / 继续一条周期记账 */
    fun toggleRecurring(item: RecurringEntity) {
        viewModelScope.launch { repo.updateRecurring(item.copy(enabled = !item.enabled)) }
    }

    fun setTodoFilter(filter: TodoFilter) { todoFilter.value = filter }

    fun setTodoQuery(query: String) { todoQuery.value = query }

    // ---- 记账 ----

    fun addTransaction(
        amountCents: Long,
        type: TxType,
        category: String,
        note: String,
        dateMillis: Long,
        account: String = Accounts.DEFAULT,
        tags: List<String> = emptyList(),
        reimbursable: Boolean = false,
        currency: String = Currencies.BASE,
        foreignAmountCents: Long = 0L,
        rateScaled: Long = Currencies.RATE_SCALE
    ) {
        viewModelScope.launch {
            repo.addTransaction(
                amountCents = amountCents,
                type = type,
                category = category,
                note = note,
                dateMillis = dateMillis,
                account = account,
                tags = tags,
                reimbursable = reimbursable,
                currency = currency,
                foreignAmountCents = foreignAmountCents,
                rateScaled = rateScaled
            )
            // 记完一笔顺手看一眼预算，越过预警线就提醒一次
            // （不分收支：改/删一条收入不影响支出，但把支出改成收入、或删掉一笔支出都会变，
            //  统一在每一条「会动到本月支出」的路上都查一次，见 checkBudgetAlert）
            withContext(Dispatchers.IO) { checkBudgetAlert() }
        }
    }

    /** 修改一条已有记录 */
    fun updateTransaction(
        item: TransactionEntity,
        amountCents: Long,
        type: TxType,
        category: String,
        note: String,
        dateMillis: Long,
        account: String = item.account,
        tags: List<String> = item.tagList,
        reimbursable: Boolean = item.reimbursable,
        currency: String = item.currency,
        foreignAmountCents: Long = item.foreignAmountCents,
        rateScaled: Long = item.rateScaled
    ) {
        viewModelScope.launch {
            repo.updateTransaction(
                item = item,
                amountCents = amountCents,
                type = type,
                category = category,
                note = note,
                dateMillis = dateMillis,
                account = account,
                tags = tags,
                reimbursable = reimbursable,
                currency = currency,
                foreignAmountCents = foreignAmountCents,
                rateScaled = rateScaled
            )
            // 把金额改大（改小也一样）可能正好把某一档跨过去，所以改完也要看一眼预算
            withContext(Dispatchers.IO) { checkBudgetAlert() }
        }
    }

    fun deleteTransaction(item: TransactionEntity) {
        viewModelScope.launch {
            repo.deleteTransaction(item)
            withContext(Dispatchers.IO) { checkBudgetAlert() }
        }
    }

    // ---- 待办 ----

    fun addTodo(title: String, dueMillis: Long?, repeatRule: RepeatRule = RepeatRule.NONE) {
        val text = title.trim()
        if (text.isEmpty()) return
        viewModelScope.launch { repo.addTodo(text, dueMillis, repeatRule) }
    }

    fun toggleTodoDone(item: TodoEntity) {
        viewModelScope.launch { repo.setTodoDone(item, !item.done) }
    }

    fun toggleTodoImportant(item: TodoEntity) {
        viewModelScope.launch { repo.toggleTodoImportant(item) }
    }

    /** 作业 / DDL：一条带课程名的待办（学习模块用，省得先建后改） */
    fun addAssignment(title: String, courseName: String, dueMillis: Long?) {
        viewModelScope.launch {
            repo.addTodo(title = title, dueMillis = dueMillis, courseName = courseName)
        }
    }

    fun updateTodo(
        item: TodoEntity,
        title: String,
        important: Boolean,
        dueMillis: Long?,
        repeatRule: RepeatRule = item.repeat
    ) {
        val text = title.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repo.updateTodo(
                item.copy(
                    title = text,
                    important = important,
                    dueMillis = dueMillis,
                    repeatRule = repeatRule.name
                )
            )
        }
    }

    fun deleteTodo(item: TodoEntity) {
        viewModelScope.launch {
            repo.deleteTodo(item)
            // 删掉的正好是当前专注目标时，一并清空目标
            if (settings.focusTaskId.value == item.id) settings.clearFocusTask()
        }
    }

    fun clearCompletedTodos() {
        viewModelScope.launch { repo.clearCompletedTodos() }
    }

    /** 把某条待办设为当前专注目标；再次点击取消 */
    fun toggleFocusTask(item: TodoEntity) {
        if (settings.focusTaskId.value == item.id) {
            settings.clearFocusTask()
        } else {
            settings.setFocusTask(item.id, item.title)
        }
    }

    // ---- 设置 ----

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)

    /** 切换界面语言（简中 / 繁中 / 英 / 日） */
    fun setLang(lang: Lang) = settings.setLang(lang)

    fun setDynamicColor(enabled: Boolean) = settings.setDynamicColor(enabled)

    fun setMonthlyBudget(cents: Long) = settings.setMonthlyBudget(cents)

    /** 设置 / 取消某个分类的月度预算（传 0 表示取消） */
    fun setCategoryBudget(category: String, cents: Long) =
        settings.setCategoryBudget(category, cents)

    fun clearCategoryBudgets() = settings.clearCategoryBudgets()

    /** 每晚记账提醒 */
    fun setLedgerReminder(enabled: Boolean) = settings.setLedgerReminder(enabled)

    fun setLedgerReminderTime(hour: Int, minute: Int) =
        settings.setLedgerReminderTime(hour, minute)

    /** 每日专注目标（0 = 不设） */
    fun setFocusGoal(count: Int) = settings.setFocusGoal(count)

    /** 定期小结：关 / 每周 / 每月 */
    fun setSummaryMode(mode: SummaryMode) = settings.setSummaryMode(mode)

    /** 预算预警开关 */
    fun setBudgetAlert(enabled: Boolean) = settings.setBudgetAlert(enabled)

    // ---- 预算预警 ----

    /**
     * 这个月的支出落在哪一档（没到 80% 返回 null）。
     *
     * 一条 `when` 从高往低判，**只看「现在到了哪一档」**，不看「上一次是哪一档、这一次跳了多远」：
     * 支出从 70% 一步跳到 110% 时这里直接得到 [BudgetTier.OVER]，
     * 不会因为分支顺序把某一档判不到（旧写法是先判超支、再判 80%，看起来一样，
     * 但配合「只在记账时才查一次」就变成了「80% 那一档只有在某一次记账恰好落在 [80%,100%) 时才可能响」）。
     * 于是每月的规则是确定的：**本月到达过哪一档就报哪一档，同一档每月只报一次**；
     * 预算调大 / 删记录让支出退回低档后，没报过的低档之后仍会照常报（各档有自己的键）。
     */
    private fun budgetTierOf(spent: Long, budget: Long): BudgetTier? = when {
        budget <= 0L -> null
        spent > budget -> BudgetTier.OVER
        spent >= budget * 8 / 10 -> BudgetTier.NEAR
        else -> null
    }

    /**
     * 检查预算：月度预算的「接近预算 / 已超支」两档，以及各分类预算超支，每档每月提醒一次。
     *
     * 调用时机：**每一条会改变本月支出的路**都要调它 —— 记一笔 / 改金额 / 删除 / 导入 CSV /
     * 恢复备份 / 补记周期账 / 清空流水。以前只在 `addTransaction` 里查，所以「编辑金额改到超支」
     * 「CSV 导入一大笔」这些路根本不会预警（开着开关也一声不响）。
     *
     * 没设任何预算时直接返回，连数据库都不读。
     */
    private suspend fun checkBudgetAlert() {
        if (!settings.budgetAlert.value) return
        val budget = settings.monthlyBudgetCents.value
        val categoryBudgets = settings.categoryBudgets.value.filterValues { it > 0L }
        if (budget <= 0L && categoryBudgets.isEmpty()) return

        val month = YearMonth.now()
        val lang = settings.lang.value
        val monthKey = month.toString()

        val expenses = repo.snapshot().transactions.filter {
            it.type == TxType.EXPENSE && YearMonth.from(it.dateMillis.toLocalDate()) == month
        }
        val spent = expenses.sumOf { it.amountCents }

        when (budgetTierOf(spent, budget)) {
            BudgetTier.OVER -> warnBudgetOnce("$monthKey:over") {
                AppStrings.budgetOverTitle(lang) to
                    AppStrings.budgetOverText(lang, formatAmount(spent - budget))
            }

            BudgetTier.NEAR -> warnBudgetOnce("$monthKey:near") {
                AppStrings.budgetNearTitle(lang) to
                    AppStrings.budgetNearText(lang, formatAmount(spent), formatAmount(budget))
            }

            // 还没到 80%：两档都不该响
            null -> Unit
        }

        categoryBudgets.forEach { (category, catBudget) ->
            val catSpent = expenses.filter { it.category == category }.sumOf { it.amountCents }
            if (catSpent > catBudget) {
                warnBudgetOnce("$monthKey:cat:$category") {
                    AppStrings.notifOverBudgetTitle(lang) to
                        AppStrings.notifOverBudgetOver(
                            lang,
                            category,
                            formatAmount(catSpent - catBudget)
                        )
                }
            }
        }
    }

    /**
     * 同一档每月只提醒一次：先把通知发出去，**发得出去才记账**。
     *
     * 顺序很要紧。以前是反过来（先 `markBudgetWarned` 再发通知），于是一旦通知没发出去
     * （Android 13+ 没给通知权限时 [Notifier.notifyBudgetAlert] 会直接 return），
     * 这个月已经被标成「提醒过了」，用户从此再也收不到这一档的提醒 —— 静默失效。
     * 现在权限没给就不记账，下次记账 / 导入时还会再试。
     */
    private fun warnBudgetOnce(key: String, build: () -> Pair<String, String>) {
        if (settings.isBudgetWarned(key)) return
        // 和 Notifier 里的判断保持一致（Android 13+ 需要 POST_NOTIFICATIONS）。
        // Notifier.notifyBudgetAlert 不返回「到底发没发出去」，所以只能在这里对同样的条件。
        if (!canPostNotifications()) return
        val (title, text) = build()
        Notifier(app).notifyBudgetAlert(key, title, text)
        settings.markBudgetWarned(key)
    }

    /** 当前有没有发通知的权限：和 [Notifier] 内部那句判断同口径 */
    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    // ---- 备份 / 恢复 / 导出 ----

    /** 导出全部数据为 JSON 备份文件（换机、重装前先存一份） */
    fun exportBackup(uri: Uri) = runFileTask {
        val snapshot = repo.snapshot()
        // 除了数据库里的全部内容，还要带上预算与各类设置（外观 / 语言 / 提醒 / 番茄钟 /
        // 学习设置 / 分类清单 / 汇率 / 自动备份文件夹）—— 恢复之后才是「和原来一样」
        val settingsSnapshot = readBackupSettings(app)
        Backup.writeText(
            app,
            uri,
            Backup.toJson(snapshot, settings.monthlyBudgetCents.value, settingsSnapshot)
        )
        AppStrings.backupExported(
            settings.lang.value,
            snapshot.transactions.size,
            snapshot.todos.size,
            snapshot.focusSessions.size
        )
    }

    /** 导出记账流水为 CSV（Excel 可直接打开） */
    fun exportLedgerCsv(uri: Uri) = runFileTask {
        val transactions = repo.snapshot().transactions
        val lang = settings.lang.value
        if (transactions.isEmpty()) throw IllegalStateException(AppStrings.nothingToExport(lang))
        Backup.writeText(app, uri, Backup.toCsv(transactions, lang))
        AppStrings.csvExported(lang, transactions.size)
    }

    /** 从备份文件恢复：会覆盖当前全部数据 */
    fun importBackup(uri: Uri) = runFileTask {
        val parsed = Backup.parse(Backup.readText(app, uri))
        repo.restore(parsed.snapshot)
        settings.setMonthlyBudget(parsed.budgetCents)
        // 设置那一段是后加的：老备份里没有（parsed.settings == null）就整段跳过，
        // 只恢复数据 —— 老文件照旧能用，也不会把本机的主题 / 语言悄悄改掉
        parsed.settings?.let { applyBackupSettings(app, it) }
        // 恢复备份换掉的是整份数据，本月支出一夜之间就可能跨过某一档
        checkBudgetAlert()
        AppStrings.backupRestored(
            settings.lang.value,
            parsed.snapshot.transactions.size,
            parsed.snapshot.todos.size,
            parsed.snapshot.focusSessions.size
        )
    }

    /**
     * 导入记账 CSV。
     * 用「日期 + 金额 + 类型 + 分类 + 账户 + 备注」当指纹去重，
     * 所以同一份文件重复导入不会翻倍；解析不了的行在解析阶段就被丢掉了。
     */
    fun importLedgerCsv(uri: Uri) = runFileTask {
        val lang = settings.lang.value
        val rows = Backup.parseCsv(Backup.readText(app, uri), lang)
        if (rows.isEmpty()) throw IllegalStateException(AppStrings.nothingToImport(lang))

        val seen = repo.snapshot().transactions.map { Backup.fingerprint(it) }.toMutableSet()
        val fresh = rows.filter { seen.add(Backup.fingerprint(it)) }
        if (fresh.isEmpty()) return@runFileTask AppStrings.importNothingNew(lang)

        repo.insertTransactions(fresh)
        // 一次导入可能直接跨过 80% / 超支两档，导入完也要查一遍
        checkBudgetAlert()
        AppStrings.importedCsv(lang, fresh.size, rows.size - fresh.size)
    }

    /** 文件读写放到 IO 线程，结果统一以提示语回到界面 */
    private fun runFileTask(block: suspend () -> String) {        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { block() } }
            _message.value = result.getOrElse {
                AppStrings.actionFailed(
                    settings.lang.value,
                    it.message ?: it.javaClass.simpleName
                )
            }
        }
    }

    // ---- 清除数据 ----

    /**
     * 清除所有记账流水（设置页那个按钮）。
     *
     * **周期记账规则不在删除范围里**，所以确认文案 [AppStrings] 那一侧必须说清
     * 「规则会保留、到日子还会自动记一笔」，并指出想连规则一起清掉要用「清空全部数据」——
     * 文案和行为必须一致（见 SettingsStrings.clearAllRecordsMessage）。
     */
    fun clearTransactions() {
        viewModelScope.launch {
            repo.clearTransactions()
            // 清空会把本月支出打到 0：不会再触发新的一档，但「已经报过」的键要跟着当下这一眼重新对齐
            withContext(Dispatchers.IO) { checkBudgetAlert() }
        }
    }

    fun clearTodos() {
        viewModelScope.launch {
            repo.clearTodos()
            settings.clearFocusTask()
        }
    }

    /** 清除专注统计（即删除所有专注记录） */
    fun clearFocusStats() {
        viewModelScope.launch { repo.clearFocusSessions() }
    }

    fun clearAll() {
        viewModelScope.launch {
            repo.clearAll()
            settings.clearFocusTask()
        }
    }

    // ---- 备忘录 ----

    /** 随手记一条；标题空了直接忽略，不写进库里 */
    fun addMemo(title: String, content: String, pinned: Boolean = false) {
        val text = title.trim()
        if (text.isEmpty()) return
        viewModelScope.launch { repo.addMemo(text, content, pinned) }
    }

    /** 整体替换一条备忘录（界面自己 copy 好字段；updateMemo 会顺手刷新 updatedAt） */
    fun updateMemo(item: MemoEntity) {
        viewModelScope.launch { repo.updateMemo(item) }
    }

    fun deleteMemo(item: MemoEntity) {
        viewModelScope.launch { repo.deleteMemo(item) }
    }

    /** 置顶 / 取消置顶（置顶也算一次改动，同样刷新 updatedAt） */
    fun toggleMemoPinned(item: MemoEntity) {
        viewModelScope.launch { repo.toggleMemoPinned(item) }
    }

    // ---- 大事记 ----

    /** 记一件已经发生的事（毕业、入职、第一次旅行……） */
    fun addMilestone(title: String, note: String, dateMillis: Long, imageUri: String = "") {
        val text = title.trim()
        if (text.isEmpty()) return
        viewModelScope.launch { repo.addMilestone(text, note, dateMillis, imageUri) }
    }

    fun updateMilestone(item: MilestoneEntity) {
        viewModelScope.launch { repo.updateMilestone(item) }
    }

    fun deleteMilestone(item: MilestoneEntity) {
        viewModelScope.launch { repo.deleteMilestone(item) }
    }

    // ---- 重要日期 ----

    /**
     * 新增一个重要日期（生日 / 纪念日 / 倒计时）。
     * 农历的按「农历月 + 农历日」存，[dateMillis] 只是首次换算出的阳历日，方便排序。
     */
    fun addImportantDate(
        title: String,
        dateMillis: Long,
        lunar: Boolean = false,
        lunarMonth: Int = 1,
        lunarDay: Int = 1,
        lunarLeap: Boolean = false,
        repeat: DateRepeat = DateRepeat.YEARLY,
        remindDaysBefore: Int = 0,
        note: String = ""
    ) {
        val text = title.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repo.addImportantDate(
                title = text,
                dateMillis = dateMillis,
                lunar = lunar,
                lunarMonth = lunarMonth,
                lunarDay = lunarDay,
                lunarLeap = lunarLeap,
                repeat = repeat,
                remindDaysBefore = remindDaysBefore,
                note = note
            )
        }
    }

    fun updateImportantDate(item: ImportantDateEntity) {
        viewModelScope.launch { repo.updateImportantDate(item) }
    }

    fun deleteImportantDate(item: ImportantDateEntity) {
        viewModelScope.launch { repo.deleteImportantDate(item) }
    }

    // ---- 习惯打卡 ----

    /** 新建一个习惯；「背单词计划」这类也用它建（单位填 个 / 页） */
    fun addHabit(
        name: String,
        emoji: String = "✅",
        targetPerDay: Int = 1,
        unit: String = "次",
        daysPerWeek: Int = 7
    ) {
        val text = name.trim()
        if (text.isEmpty()) return
        viewModelScope.launch { repo.addHabit(text, emoji, targetPerDay, unit, daysPerWeek) }
    }

    fun updateHabit(item: HabitEntity) {
        viewModelScope.launch { repo.updateHabit(item) }
    }

    /** 删习惯会连它的打卡记录一起删 */
    fun deleteHabit(item: HabitEntity) {
        viewModelScope.launch { repo.deleteHabit(item) }
    }

    /** 记一次打卡：把某一天的完成量直接改成 [count]（一天一条，不会重复记） */
    fun logHabit(habitId: Long, dayMillis: Long, count: Int) {
        viewModelScope.launch { repo.logHabit(habitId, dayMillis, count) }
    }

    /** 勾选 / 取消勾选「今天」：今天就是本地零点的那一天，不用调用方传日期 */
    fun toggleHabitToday(habit: HabitEntity) {
        val todayMillis = LocalDate.now().toDayMillis()
        viewModelScope.launch { repo.toggleHabitDone(habit.id, todayMillis, habit.targetPerDay) }
    }

    // ---- 课表 ----

    /**
     * 新增一门课（课表里的一格：星期几 + 第几节到第几节 + 起止周）。
     *
     * [startMinutes] / [endMinutes] 是当天 00:00 起的分钟数（-1 = 没填），上课提醒靠它算准点时刻。
     */
    fun addCourse(
        name: String,
        teacher: String = "",
        location: String = "",
        dayOfWeek: Int = 1,
        startPeriod: Int = 1,
        endPeriod: Int = 2,
        weeks: String = "",
        termStartMillis: Long,
        startMinutes: Int = -1,
        endMinutes: Int = -1,
        colorIndex: Int = 0,
        note: String = ""
    ) {
        val text = name.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repo.addCourse(
                name = text,
                teacher = teacher,
                location = location,
                dayOfWeek = dayOfWeek,
                startPeriod = startPeriod,
                endPeriod = endPeriod,
                weeks = weeks,
                termStartMillis = termStartMillis,
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                colorIndex = colorIndex,
                note = note
            )
        }
    }

    fun updateCourse(item: CourseEntity) {
        viewModelScope.launch { repo.updateCourse(item) }
    }

    fun deleteCourse(item: CourseEntity) {
        viewModelScope.launch { repo.deleteCourse(item) }
    }

    // ---- 考试与复习计划 ----

    /** 新增一场考试（倒计时的主体） */
    fun addExam(
        name: String,
        courseName: String = "",
        examMillis: Long,
        location: String = "",
        note: String = ""
    ) {
        val text = name.trim()
        if (text.isEmpty()) return
        viewModelScope.launch { repo.addExam(text, courseName, examMillis, location, note) }
    }

    fun updateExam(item: ExamEntity) {
        viewModelScope.launch { repo.updateExam(item) }
    }

    /** 删考试会连它的复习计划一起删 */
    fun deleteExam(item: ExamEntity) {
        viewModelScope.launch { repo.deleteExam(item) }
    }

    /** 加一条复习计划；[examId] 传 0 表示不挂考试的独立任务 */
    fun addStudyTask(examId: Long, title: String, dateMillis: Long) {
        val text = title.trim()
        if (text.isEmpty()) return
        viewModelScope.launch { repo.addStudyTask(examId, text, dateMillis) }
    }

    /** 整体替换一条复习计划（界面自己 copy 好标题 / 日期 / 排序） */
    fun updateStudyTask(item: StudyTaskEntity) {
        viewModelScope.launch { repo.updateStudyTask(item) }
    }

    fun toggleStudyTask(item: StudyTaskEntity) {
        viewModelScope.launch { repo.toggleStudyTask(item) }
    }

    fun deleteStudyTask(item: StudyTaskEntity) {
        viewModelScope.launch { repo.deleteStudyTask(item) }
    }

    // ---- 成绩 / 学分 / 奖助 ----

    /** 录入一门课的成绩；[point] 是换算好的绩点，算 GPA 只认它 */
    fun addGrade(
        term: String,
        courseName: String,
        credit: Double = 0.0,
        score: String = "",
        scoreKind: ScoreKind = ScoreKind.PERCENT,
        point: Double = 0.0,
        category: String = "必修",
        note: String = ""
    ) {
        val text = courseName.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repo.addGrade(term, text, credit, score, scoreKind, point, category, note)
        }
    }

    fun updateGrade(item: GradeEntity) {
        viewModelScope.launch { repo.updateGrade(item) }
    }

    fun deleteGrade(item: GradeEntity) {
        viewModelScope.launch { repo.deleteGrade(item) }
    }

    /** 某个课程类别要修满多少学分 */
    fun addCreditTarget(category: String, required: Double) {
        val text = category.trim()
        if (text.isEmpty()) return
        viewModelScope.launch { repo.addCreditTarget(text, required) }
    }

    fun updateCreditTarget(item: CreditTargetEntity) {
        viewModelScope.launch { repo.updateCreditTarget(item) }
    }

    fun deleteCreditTarget(item: CreditTargetEntity) {
        viewModelScope.launch { repo.deleteCreditTarget(item) }
    }

    /** 记一条奖助 / 竞赛 / 证书 */
    fun addAward(
        title: String,
        kind: AwardKind = AwardKind.SCHOLARSHIP,
        dateMillis: Long,
        level: String = "",
        note: String = "",
        imageUri: String = ""
    ) {
        val text = title.trim()
        if (text.isEmpty()) return
        viewModelScope.launch { repo.addAward(text, kind, dateMillis, level, note, imageUri) }
    }

    fun updateAward(item: AwardEntity) {
        viewModelScope.launch { repo.updateAward(item) }
    }

    fun deleteAward(item: AwardEntity) {
        viewModelScope.launch { repo.deleteAward(item) }
    }

    // ---- 状态拼装 ----

    private fun buildState(
        ledger: LedgerInputs,
        todo: TodoInputs,
        life: LifeInputs,
        study: StudyInputs,
        focus: FocusSettings
    ): UiState {
        val today = LocalDate.now()
        val month = ledger.month

        val monthTx = ledger.all.filter { YearMonth.from(it.dateMillis.toLocalDate()) == month }
        val query = ledger.query.trim()
        // 搜索匹配分类 / 备注 / 标签三者：流水行上把标签显示出来了，搜索框也写着「分类、备注或标签」，
        // 只查前两项会让「搜标签」永远搜不到（用户以为记录丢了）。
        val searched = if (query.isEmpty()) monthTx else monthTx.filter { tx ->
            tx.category.contains(query, ignoreCase = true) ||
                tx.note.contains(query, ignoreCase = true) ||
                tx.tagList.any { it.contains(query, ignoreCase = true) }
        }
        // 账户 / 标签 / 某一天这三个筛选只影响列表（和搜索一样），上方汇总是整月口径
        val accountFilter = ledger.accountFilter
        val tagFilter = ledger.tagFilter
        val dayFilter = ledger.dayFilter
        val listed = searched
            .filter { accountFilter == null || it.account == accountFilter }
            .filter { tagFilter == null || it.tagList.contains(tagFilter) }
            .filter { dayFilter == null || it.dateMillis.toLocalDate() == dayFilter }

        // 两份分组，`groupBy` 形状完全一样，区别只在数据范围：
        // - listedGroups（UiState.monthGroups）：带搜索 / 账户 / 标签 / 按天筛选，喂记账页的流水列表；
        // - allGroups（UiState.monthAllGroups）：整月不筛选，喂统计详情页的三份明细，
        //   和下面的 monthExpense / monthIncome / monthCount / expenseSlices / calendarDays 同一份口径。
        //
        // 以前统计详情页读的是带筛选的那一份，于是「本月 42 笔」的标题下面可能只列着当前筛选出的 1 条，
        // 分类明细里的「餐饮 1 笔 ¥860.00」也和整月那一栏的 30 笔互相矛盾。
        fun groupsOf(source: List<TransactionEntity>): List<DayGroup> = source
            .groupBy { it.dateMillis.toLocalDate() }
            .toSortedMap(compareByDescending<LocalDate> { it })
            .map { (date, items) ->
                DayGroup(
                    date = date,
                    expense = items.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents },
                    income = items.filter { it.type == TxType.INCOME }.sumOf { it.amountCents },
                    items = items
                )
            }

        val groups = groupsOf(listed)
        val allGroups = groupsOf(monthTx)

        val monthExpense = monthTx.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
        val monthIncome = monthTx.filter { it.type == TxType.INCOME }.sumOf { it.amountCents }
        val todayExpense = ledger.all
            .filter { it.type == TxType.EXPENSE && it.dateMillis.toLocalDate() == today }
            .sumOf { it.amountCents }

        fun slicesOf(type: TxType, total: Long): List<CategorySlice> =
            if (total <= 0L) emptyList()
            else monthTx
                .filter { it.type == type }
                .groupBy { it.category }
                .map { (category, items) ->
                    val cents = items.sumOf { it.amountCents }
                    CategorySlice(category, cents, cents.toFloat() / total.toFloat())
                }
                .sortedByDescending { it.cents }

        val expenseByDay = monthTx
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.dateMillis.toLocalDate().dayOfMonth }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }

        // ---- 账户 ----
        val accounts = ledger.all
            .map { it.account }
            .distinct()
            .sortedWith(compareBy({ it != Accounts.DEFAULT }, { it }))

        val accountSlices = if (monthExpense <= 0L) emptyList()
        else monthTx
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.account }
            .map { (account, items) ->
                val cents = items.sumOf { it.amountCents }
                AccountSlice(account, cents, cents.toFloat() / monthExpense.toFloat())
            }
            .sortedByDescending { it.cents }

        // ---- 分类预算执行情况 ----
        val spentByCategory = monthTx
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }

        val categoryBudgetRows = ledger.categoryBudgets
            .map { (category, budget) ->
                CategoryBudgetRow(
                    category = category,
                    budgetCents = budget,
                    spentCents = spentByCategory[category] ?: 0L
                )
            }
            .sortedWith(compareByDescending<CategoryBudgetRow> { it.over }.thenByDescending { it.ratio })

        val dayBars = (1..month.lengthOfMonth()).map { day ->
            DayBar(day, expenseByDay[day] ?: 0L)
        }

        // ---- 待办 ----
        val todoQueryText = todo.query.trim()
        val matched = if (todoQueryText.isEmpty()) todo.all else todo.all.filter {
            it.title.contains(todoQueryText, ignoreCase = true)
        }
        val pending = todo.all.count { !it.done }
        val visible = when (todo.filter) {
            TodoFilter.ALL -> matched
            TodoFilter.PENDING -> matched.filter { !it.done }
            TodoFilter.DONE -> matched.filter { it.done }
        }

        // ---- 专注（从记录表派生）----
        val sessionsByDay = focus.sessions.groupBy { it.startedAtMillis.toLocalDate() }
        val todaySessions = sessionsByDay[today].orEmpty().sortedByDescending { it.startedAtMillis }
        val recentDays = (6 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            FocusDay(day, sessionsByDay[day]?.size ?: 0)
        }
        // 连续天数：从今天（今天还没有则从昨天）往前数
        var streak = 0
        var cursor = if (sessionsByDay[today].isNullOrEmpty()) today.minusDays(1) else today
        while (!sessionsByDay[cursor].isNullOrEmpty()) {
            streak++
            cursor = cursor.minusDays(1)
        }

        // ---- 本周 / 本月汇总（周一为一周之始）----
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekSessions = focus.sessions.filter {
            !it.startedAtMillis.toLocalDate().isBefore(monday)
        }
        // 「本月」锚的是**选中月**（上面 `val month = ledger.month`），不是「今天所在的月」：
        // monthSessions / monthCount / monthMinutes 是统计页与月报里「选中月」那一栏的数字。
        // 以前这里用的是 YearMonth.from(today)，于是把月份切到 5 月、标题写着「5 月」，
        // 专注那一栏却还是当前月的数 —— 标题与数字互相打架。
        // （周与热力图仍然锚在当前周 / 今天附近，这是刻意保留的：它们本来就是「最近」的意思。）
        val selectedMonth = month
        val monthSessions = focus.sessions.filter {
            YearMonth.from(it.startedAtMillis.toLocalDate()) == selectedMonth
        }

        // 最近 7 天（含今天）的专注记录，**与选中的月份无关**。
        // 学习周报声明的是「近 7 天」，它不能跟着记账/统计页选的月份走：
        // 否则用户把选中月切到别处时，那 7 天会突然变成 0，而同一页的柱状图（来自热力图）还是有数 ——
        // 数字和柱子又打架了。
        val last7 = today.minusDays(6)..today
        val last7Sessions = focus.sessions.filter {
            it.startedAtMillis.toLocalDate() in last7
        }

        // ---- 近 12 周热力图：列是周，行是周一到周日；未来日期记 -1，界面画成空格 ----
        val minutesByDay = focus.sessions
            .groupBy { it.startedAtMillis.toLocalDate() }
            .mapValues { entry -> entry.value.sumOf { it.minutes } }
        val heatWeeks = (11 downTo 0).map { back ->
            val weekStart = monday.minusWeeks(back.toLong())
            (0..6).map { offset ->
                val date = weekStart.plusDays(offset.toLong())
                HeatCell(date, if (date.isAfter(today)) -1 else (minutesByDay[date] ?: 0))
            }
        }
        val heatMaxMinutes = heatWeeks.flatten().maxOfOrNull { it.minutes }?.coerceAtLeast(0) ?: 0

        // ---- 按待办汇总投入时间（从「专注目标」发起的那些记录）----
        val perTodo = focus.sessions
            .filter { it.taskTitle.isNotBlank() }
            .groupBy { it.taskTitle }
            .map { (title, items) ->
                TodoFocus(title, items.sumOf { it.minutes }, items.size)
            }
            .sortedByDescending { it.minutes }
            .take(10)

        val focusStats = FocusStats(
            todayCount = todaySessions.size,
            todayMinutes = todaySessions.sumOf { it.minutes },
            totalCount = focus.sessions.size,
            streak = streak,
            weekCount = weekSessions.size,
            weekMinutes = weekSessions.sumOf { it.minutes },
            monthCount = monthSessions.size,
            monthMinutes = monthSessions.sumOf { it.minutes },
            recentDays = recentDays,
            todaySessions = todaySessions,
            monthSessions = monthSessions.sortedByDescending { it.startedAtMillis },
            last7Sessions = last7Sessions,
            heatWeeks = heatWeeks,
            heatMaxMinutes = heatMaxMinutes,
            perTodo = perTodo
        )

        // ---- 生活模块：备忘录 / 大事记 / 重要日期 / 习惯打卡 ----
        // 打卡记录存的就是「当天零点」，所以这里的「今天」也用本地零点比
        val todayMillis = today.toDayMillis()
        // 一天**不一定**只有一条记录（导入 / 恢复备份会带进来重复行，见 HabitLogEntity 的说明），
        // 所以按 habitId 分组之后还要按「同一天求和」读
        val habitLogsOf = life.habitLogs.groupBy { it.habitId }

        // 每个习惯今天的完成量：把今天那几条**加起来**；没记录就是 0。
        // 和习惯页（HabitsScreen）的今日数量、7 格小图同一口径 ——
        // 以前这里取 `firstOrNull`，一天两条时标题就会比格子少
        val habitToday = life.habits.associate { habit ->
            habit.id to habitLogsOf[habit.id].orEmpty()
                .filter { it.dateMillis == todayMillis }
                .sumOf { it.count }
        }

        // 连续天数：从今天往前一天天数；今天还没打卡就从昨天起算
        // —— 今天还没过完，不该把「昨天还在坚持」判成断掉（和上面专注 streak 一个口径）
        val habitStreak = life.habits.associate { habit ->
            val doneDays = habitLogsOf[habit.id].orEmpty()
                .filter { it.count > 0 }
                .map { it.dateMillis.toLocalDate() }
                .toSet()
            var day = if (doneDays.contains(today)) today else today.minusDays(1)
            var run = 0
            while (doneDays.contains(day)) {
                run++
                day = day.minusDays(1)
            }
            habit.id to run
        }

        // 本周打过卡的天数：和上面 monday 同口径（周一为一周之始），用来算「每周 N 天」的完成度
        val habitWeekDone = life.habits.associate { habit ->
            val days = habitLogsOf[habit.id].orEmpty()
                .filter { it.count > 0 && !it.dateMillis.toLocalDate().isBefore(monday) }
                .map { it.dateMillis }
                .distinct()
                .size
            habit.id to days
        }

        // 定量类习惯：只认**可数的单位**（个 / 页，见 COUNTABLE_HABIT_UNITS）。
        // 以前这里是 `unit != "次"` 的黑名单，于是「每天练琴 30 分钟」也被当成
        // 背单词计划塞进学习周报的定量计划那一栏 —— 单位不认识的不算
        val wordHabits = life.habits.filter { it.unit in COUNTABLE_HABIT_UNITS }

        // 重要日期的下一次发生：农历交给 Lunar 换算，阳历按重复规则往后滚 ——
        // 规则都在 ImportantDateSchedule 里（和提醒排程共用一份）。
        // 「只过一次」那天过去之后算不出「下一次」，以前就这么凭空消失了；
        // 现在它不会进 upcomingDates（那是「下一次还没发生的」，顶部大卡片只认它），
        // 而是进 pastDates，界面上明确标成「已过去 N 天」。
        val upcomingDates = life.dates.mapNotNull { item ->
            val next = nextOccurrenceDate(item, today) ?: return@mapNotNull null
            UpcomingDate(item, next.toDayMillis(), ChronoUnit.DAYS.between(today, next))
        }.sortedBy { it.daysLeft }

        // 已经过完的「只过一次」：按离今天从近到远排（daysLeft 是负数，越近越靠前）
        val pastDates = life.dates.mapNotNull { item ->
            val next = ImportantDateSchedule.nextOccurrence(item, today) ?: return@mapNotNull null
            if (!next.isBefore(today)) return@mapNotNull null
            UpcomingDate(item, next.toDayMillis(), ChronoUnit.DAYS.between(today, next))
        }.sortedByDescending { it.daysLeft }

        // ---- 学习模块：课表 / 考试 / 作业 / 成绩 / 学分 / 奖助 ----
        // 下一场考试：已经开考的不算；examDaysLeft 是整天数（今天考 = 0）
        val nowMillis = System.currentTimeMillis()
        val nextExam = study.exams
            .filter { it.examMillis >= nowMillis }
            .minByOrNull { it.examMillis }
        val examDaysLeft = nextExam?.let {
            ChronoUnit.DAYS.between(today, it.examMillis.toLocalDate())
        } ?: 0L

        // 作业 / DDL 复用待办表：挂了课程名的那些就是作业（见 Study.kt 的说明）
        val assignmentTodos = todo.all.filter { it.courseName.isNotBlank() }
        val overdueAssignments = assignmentTodos.count { item ->
            val due = item.dueMillis
            !item.done && due != null && due.toLocalDate().isBefore(today)
        }

        // 平均绩点：只按有学分的课加权（point 在录入时已经按计分方式换算好了）
        val graded = study.grades.filter { it.credit > 0.0 }
        val gradedCredits = graded.sumOf { it.credit }
        val gpa = if (gradedCredits > 0.0) {
            graded.sumOf { it.point * it.credit } / gradedCredits
        } else {
            0.0
        }
        // 已修学分：**学分 > 0 且绩点 > 0** 才算拿到。口径的唯一实现在成绩页的 earnedCredit，
        // 学分进度页与学习首页也用它自己重算 —— 这里直接复用同一个函数，
        // 于是「三处各自算一遍」不会再分叉（以前这里是 sumOf { it.credit }，
        // 不及格的课也算进已修学分，甚至能把某个类别顶成「已达标」）。
        val totalCredits = earnedCredits(study.grades)
        // 按课程类别（必修 / 选修 / 通识）汇总已修学分，和 creditTargets 对照着看还差多少
        val creditsByCategory = earnedCreditsByCategory(study.grades)

        // 今天的专注时长：和 focusStats.todayMinutes 同一个数，单独给学习页一个好读的名字
        val studyMinutesToday = todaySessions.sumOf { it.minutes }

        // ---- 年度报表（近 12 个月趋势 + 年度汇总）----
        // 「年」锚的是**选中月所在的那一年**（上面 `val month = ledger.month`），不是「今天的年份」：
        // 以前这里和下面的趋势柱都按 today.year / YearMonth.from(today) 算，
        // 于是把月份翻到 2024 年时，同一页上「2024 年汇总」的标题下面摆着 2026 年的数字，
        // 趋势图也还是最近 12 个月 —— 一页两个「年」。同比那张卡片本来就用 month.year，
        // 现在两边是同一个年份，标题与数字终于对得上。
        val thisYear = month.year
        val yearTx = ledger.all.filter { it.dateMillis.toLocalDate().year == thisYear }
        val yearExpense = yearTx.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
        val yearIncome = yearTx.filter { it.type == TxType.INCOME }.sumOf { it.amountCents }
        val topExpense = yearTx
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .map { (category, items) -> category to items.sumOf { it.amountCents } }
            .maxByOrNull { it.second }
        val byMonth = ledger.all.groupBy { YearMonth.from(it.dateMillis.toLocalDate()) }
        // 近 12 个月的趋势也锚在**选中月**上（最后一根柱子就是选中的那个月），
        // 和上面的年度汇总、下面的同比卡片讲的是同一段时间
        val monthBars = (11 downTo 0).map { back ->
            val m = month.minusMonths(back.toLong())
            val items = byMonth[m].orEmpty()
            MonthBar(
                month = m,
                expense = items.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents },
                income = items.filter { it.type == TxType.INCOME }.sumOf { it.amountCents }
            )
        }
        val yearSummary = YearSummary(
            year = thisYear,
            income = yearIncome,
            expense = yearExpense,
            count = yearTx.size,
            topCategory = topExpense?.first.orEmpty(),
            topCategoryCents = topExpense?.second ?: 0L,
            monthBars = monthBars
        )

        val allIncome = ledger.all.filter { it.type == TxType.INCOME }.sumOf { it.amountCents }
        val allExpense = ledger.all.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }

        // ---- 标签 / 分类清单 ----
        val allTags = ledger.all
            .flatMap { it.tagList }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

        val usedCategories = ledger.all.groupBy { it.type }.mapValues { entry ->
            entry.value.map { it.category }.distinct()
        }
        val expenseCategories = (focus.cats.expense + usedCategories[TxType.EXPENSE].orEmpty()).distinct()
        val incomeCategories = (focus.cats.income + usedCategories[TxType.INCOME].orEmpty()).distinct()

        // ---- 当月日历 ----
        val byDay = monthTx.groupBy { it.dateMillis.toLocalDate() }
        val calendarDays = (1..month.lengthOfMonth()).map { dayOfMonth ->
            val date = month.atDay(dayOfMonth)
            val items = byDay[date].orEmpty()
            CalendarDay(
                date = date,
                expenseCents = items.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents },
                incomeCents = items.filter { it.type == TxType.INCOME }.sumOf { it.amountCents },
                count = items.size
            )
        }
        val maxCalendarExpense = calendarDays.maxOfOrNull { it.expenseCents } ?: 0L

        // ---- 待报销 ----
        val reimbursable = ledger.all.filter { it.reimbursable }
        val pendingReimbursement = reimbursable.filter { !it.reimbursed }
        val pendingReimbursementCents = pendingReimbursement.sumOf { it.amountCents }
        val reimbursedCents = reimbursable.filter { it.reimbursed }.sumOf { it.amountCents }

        // ---- 环 比：本月 / 上月、今年 / 去年 ----
        val lastMonth = month.minusMonths(1)
        val lastMonthTx = ledger.all.filter {
            YearMonth.from(it.dateMillis.toLocalDate()) == lastMonth
        }
        val lastMonthExpense = lastMonthTx.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
        val lastMonthIncome = lastMonthTx.filter { it.type == TxType.INCOME }.sumOf { it.amountCents }
        val thisYearForComparison = month.year
        val comparisonYearExpense = ledger.all
            .filter { it.type == TxType.EXPENSE && it.dateMillis.toLocalDate().year == thisYearForComparison }
            .sumOf { it.amountCents }
        val lastYearExpense = ledger.all.filter {
            it.type == TxType.EXPENSE && it.dateMillis.toLocalDate().year == thisYearForComparison - 1
        }.sumOf { it.amountCents }

        val comparison = Comparison(
            monthExpense = monthExpense,
            lastMonthExpense = lastMonthExpense,
            monthIncome = monthIncome,
            lastMonthIncome = lastMonthIncome,
            yearExpense = comparisonYearExpense,
            lastYearExpense = lastYearExpense
        )

        // ---- 智能洞察：先说「断更」，再说钱的趋势 ----
        val insights = buildList {
            val today = LocalDate.now()
            val lastDate = ledger.all.maxOfOrNull { it.dateMillis.toLocalDate() }
            if (lastDate != null) {
                val gap = ChronoUnit.DAYS.between(lastDate, today).toInt()
                if (gap >= 3) add(Insight(InsightKind.NO_RECORD_DAYS, days = gap))
            }

            val diff = monthExpense - lastMonthExpense
            if (lastMonthExpense > 0L && diff != 0L) {
                if (diff > 0L) {
                    // 找出「这个月比上个月多花最多」的分类，指出钱花哪了
                    val nowByCategory = monthTx.filter { it.type == TxType.EXPENSE }
                        .groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amountCents } }
                    val beforeByCategory = lastMonthTx.filter { it.type == TxType.EXPENSE }
                        .groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amountCents } }
                    val topGrowth = nowByCategory.entries
                        .map { (category, cents) -> category to (cents - (beforeByCategory[category] ?: 0L)) }
                        .maxByOrNull { it.second }
                    add(
                        Insight(
                            kind = InsightKind.SPENT_MORE,
                            amountCents = diff,
                            category = topGrowth?.takeIf { it.second > 0L }?.first.orEmpty()
                        )
                    )
                } else {
                    add(Insight(InsightKind.SPENT_LESS, amountCents = -diff))
                }
            }

            val topCategory = monthTx.filter { it.type == TxType.EXPENSE }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amountCents } }
                .maxByOrNull { it.value }
            if (topCategory != null && topCategory.value > 0L) {
                add(
                    Insight(
                        kind = InsightKind.TOP_CATEGORY,
                        amountCents = topCategory.value,
                        category = topCategory.key
                    )
                )
            }

            val left = ledger.budgetCents - monthExpense
            if (ledger.budgetCents > 0L && left > 0L) {
                add(Insight(InsightKind.BUDGET_LEFT, amountCents = left))
            }
        }.take(4)

        // ---- 子任务与看板 ----
        val subtasksByTodo = todo.subtasks.groupBy { it.todoId }
        val todayDate = LocalDate.now()
        val kanbanSections = TodoBucket.entries
            .map { bucket -> bucket to todo.all.filter { bucketOf(it, todayDate) == bucket } }
            .filter { it.second.isNotEmpty() }
            .map { (bucket, items) ->
                TodoSection(
            bucket,
            // 排序：重要的在前，其次按用户拖拽出来的 sortOrder。
            // 以前只按 important 排，而 reorderTodos 写进去的 sortOrder 没有任何读取路径 ——
            // 于是「拖拽排序」在界面上永远看不到效果（拖了等于没拖）。
            items.sortedWith(
                compareByDescending<TodoEntity> { it.important }.thenBy { it.sortOrder }
            )
        )
            }

        return UiState(
            month = month,
            monthGroups = groups,
            monthAllGroups = allGroups,
            monthExpense = monthExpense,
            monthIncome = monthIncome,
            monthCount = monthTx.size,
            todayExpense = todayExpense,
            expenseSlices = slicesOf(TxType.EXPENSE, monthExpense),
            incomeSlices = slicesOf(TxType.INCOME, monthIncome),
            dayBars = dayBars,
            maxDayCents = dayBars.maxOfOrNull { it.cents } ?: 0L,
            ledgerQuery = ledger.query,
            isSearching = query.isNotEmpty(),
            budgetCents = ledger.budgetCents,
            accounts = accounts,
            accountFilter = accountFilter,
            tagFilter = tagFilter,
            dayFilter = dayFilter,
            allTags = allTags,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            calendarDays = calendarDays,
            maxCalendarExpense = maxCalendarExpense,
            pendingReimbursementCents = pendingReimbursementCents,
            pendingReimbursementCount = pendingReimbursement.size,
            reimbursedCents = reimbursedCents,
            subtasksByTodo = subtasksByTodo,
            kanbanSections = kanbanSections,
            recurring = todo.recurring,
            comparison = comparison,
            insights = insights,
            accountSlices = accountSlices,
            categoryBudgets = categoryBudgetRows,
            visibleTodos = visible,
            todoFilter = todo.filter,
            todoQuery = todo.query,
            pendingCount = pending,
            doneCount = todo.all.size - pending,
            focusTaskId = focus.taskId,
            focusTaskTitle = focus.taskTitle,
            focusStats = focusStats,
            yearSummary = yearSummary,
            totalIncomeCents = allIncome,
            totalExpenseCents = allExpense,
            totalCount = ledger.all.size,
            // 生活模块
            memos = life.memos,
            milestones = life.milestones,
            importantDates = life.dates,
            habits = life.habits,
            habitLogs = life.habitLogs,
            habitToday = habitToday,
            habitStreak = habitStreak,
            habitWeekDone = habitWeekDone,
            upcomingDates = upcomingDates,
            pastDates = pastDates,
            wordHabits = wordHabits,
            // 学习模块
            courses = study.courses,
            exams = study.exams,
            studyTasks = study.studyTasks,
            grades = study.grades,
            creditTargets = study.creditTargets,
            awards = study.awards,
            nextExam = nextExam,
            examDaysLeft = examDaysLeft,
            assignmentTodos = assignmentTodos,
            overdueAssignments = overdueAssignments,
            transactions = ledger.all,
            todos = todo.all,
            gpa = gpa,
            gpaScale = study.gpaScale,
            totalCredits = totalCredits,
            creditsByCategory = creditsByCategory,
            studyMinutesToday = studyMinutesToday
        )
    }
}
