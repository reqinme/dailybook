package com.dailybook.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailybook.app.backup.Backup
import com.dailybook.app.data.Accounts
import com.dailybook.app.data.DailyRepository
import com.dailybook.app.data.FocusSessionEntity
import com.dailybook.app.data.RepeatRule
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.data.SummaryMode
import com.dailybook.app.data.TodoEntity
import com.dailybook.app.data.TransactionEntity
import com.dailybook.app.data.TxType
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.notify.LedgerReminder
import com.dailybook.app.notify.Notifier
import com.dailybook.app.notify.SummaryReminder
import com.dailybook.app.notify.TodoReminder
import com.dailybook.app.ui.theme.ThemeMode
import com.dailybook.app.util.formatAmount
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

/** 某个待办累计投入的专注时间 */
@Immutable
data class TodoFocus(val title: String, val minutes: Int, val count: Int)

/** 月度收支（近 12 个月趋势图） */
@Immutable
data class MonthBar(val month: YearMonth, val expense: Long, val income: Long) {
    val label: String get() = "${month.monthValue}月"
}

/** 年度汇总 */
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
    /** 近 12 周热力图：外层是周（列），内层是周一到周日（行） */
    val heatWeeks: List<List<HeatCell>> = emptyList(),
    val heatMaxMinutes: Int = 0,
    /** 按待办汇总的投入时间（只含有标题的那些记录），从多到少 */
    val perTodo: List<TodoFocus> = emptyList()
)

@Immutable
data class UiState(
    val month: YearMonth = YearMonth.now(),
    val monthGroups: List<DayGroup> = emptyList(),
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
    val totalCount: Int = 0
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
    val categoryBudgets: Map<String, Long>
)

/** 只跟筛选有关的两个设置项，单独拼一层，避免 combine 超过 5 个参数 */
private data class LedgerFilters(
    val account: String?,
    val categoryBudgets: Map<String, Long>
)

/** 待办侧的输入聚合 */
private data class TodoInputs(
    val all: List<TodoEntity>,
    val filter: TodoFilter,
    val query: String
)

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repo = DailyRepository(app)

    /** 全局共享的设置存储（与 TimerViewModel 拿到的是同一个实例） */
    val settings: SettingsStore = SettingsStore.get(app)

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val todoFilter = MutableStateFlow(TodoFilter.ALL)
    private val ledgerQuery = MutableStateFlow("")
    private val todoQuery = MutableStateFlow("")
    private val ledgerAccount = MutableStateFlow<String?>(null)

    private val ledgerFilters = combine(
        ledgerAccount,
        settings.categoryBudgets
    ) { account, budgets ->
        LedgerFilters(account, budgets)
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
            categoryBudgets = filters.categoryBudgets
        )
    }

    private val todoInputs = combine(
        repo.todos,
        todoFilter,
        todoQuery
    ) { todos, filter, query ->
        TodoInputs(todos, filter, query)
    }

    val uiState: StateFlow<UiState> = combine(
        ledgerInputs,
        todoInputs,
        repo.focusSessions,
        settings.focusTaskId,
        settings.focusTaskTitle
    ) { ledger, todos, sessions, taskId, taskTitle ->
        buildState(ledger, todos, sessions, taskId, taskTitle)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    /** 操作结果提示（导出成功、导入失败之类），界面弹完即清空 */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    init {
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
    }

    // ---- 月份切换 ----

    fun previousMonth() { selectedMonth.value = selectedMonth.value.minusMonths(1) }

    fun nextMonth() { selectedMonth.value = selectedMonth.value.plusMonths(1) }

    fun goToCurrentMonth() { selectedMonth.value = YearMonth.now() }

    // ---- 搜索 / 筛选 ----

    fun setLedgerQuery(query: String) { ledgerQuery.value = query }

    /** 记账页按账户筛选；传 null 表示看全部 */
    fun setAccountFilter(account: String?) { ledgerAccount.value = account }

    fun setTodoFilter(filter: TodoFilter) { todoFilter.value = filter }

    fun setTodoQuery(query: String) { todoQuery.value = query }

    // ---- 记账 ----

    fun addTransaction(
        amountCents: Long,
        type: TxType,
        category: String,
        note: String,
        dateMillis: Long,
        account: String = Accounts.DEFAULT
    ) {
        viewModelScope.launch {
            repo.addTransaction(amountCents, type, category, note, dateMillis, account)
            // 记完一笔顺手看一眼预算，越过预警线就提醒一次
            if (type == TxType.EXPENSE) withContext(Dispatchers.IO) { checkBudgetAlert() }
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
        account: String = item.account
    ) {
        viewModelScope.launch {
            repo.updateTransaction(item, amountCents, type, category, note, dateMillis, account)
        }
    }

    fun deleteTransaction(item: TransactionEntity) {
        viewModelScope.launch { repo.deleteTransaction(item) }
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
     * 记完一笔后检查：月度预算用到 80%、超支，以及各分类预算超支，各提醒一次。
     * 用「月份 + 阈值」做键，所以同一个月里不会反复打扰；换月后自动重新计。
     */
    private suspend fun checkBudgetAlert() {
        if (!settings.budgetAlert.value) return
        val month = YearMonth.now()
        val lang = settings.lang.value
        val monthKey = month.toString()

        val expenses = repo.snapshot().transactions.filter {
            it.type == TxType.EXPENSE && YearMonth.from(it.dateMillis.toLocalDate()) == month
        }
        val spent = expenses.sumOf { it.amountCents }

        val budget = settings.monthlyBudgetCents.value
        if (budget > 0L) {
            when {
                spent > budget -> warnBudgetOnce("$monthKey:over") {
                    AppStrings.budgetOverTitle(lang) to
                        AppStrings.budgetOverText(lang, formatAmount(spent - budget))
                }

                spent >= budget * 8 / 10 -> warnBudgetOnce("$monthKey:near") {
                    AppStrings.budgetNearTitle(lang) to
                        AppStrings.budgetNearText(lang, formatAmount(spent), formatAmount(budget))
                }
            }
        }

        settings.categoryBudgets.value.forEach { (category, catBudget) ->
            if (catBudget <= 0L) return@forEach
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

    private fun warnBudgetOnce(key: String, build: () -> Pair<String, String>) {
        if (settings.isBudgetWarned(key)) return
        settings.markBudgetWarned(key)
        val (title, text) = build()
        Notifier(app).notifyBudgetAlert(key, title, text)
    }

    // ---- 备份 / 恢复 / 导出 ----

    /** 导出全部数据为 JSON 备份文件（换机、重装前先存一份） */
    fun exportBackup(uri: Uri) = runFileTask {
        val snapshot = repo.snapshot()
        Backup.writeText(app, uri, Backup.toJson(snapshot, settings.monthlyBudgetCents.value))
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
        AppStrings.backupRestored(
            settings.lang.value,
            parsed.snapshot.transactions.size,
            parsed.snapshot.todos.size,
            parsed.snapshot.focusSessions.size
        )
    }

    /** 文件读写放到 IO 线程，结果统一以提示语回到界面 */
    private fun runFileTask(block: suspend () -> String) {
        viewModelScope.launch {
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

    fun clearTransactions() {
        viewModelScope.launch { repo.clearTransactions() }
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

    // ---- 状态拼装 ----

    private fun buildState(
        ledger: LedgerInputs,
        todo: TodoInputs,
        sessions: List<FocusSessionEntity>,
        focusTaskId: Long,
        focusTaskTitle: String
    ): UiState {
        val today = LocalDate.now()
        val month = ledger.month

        val monthTx = ledger.all.filter { YearMonth.from(it.dateMillis.toLocalDate()) == month }
        val query = ledger.query.trim()
        val searched = if (query.isEmpty()) monthTx else monthTx.filter { tx ->
            tx.category.contains(query, ignoreCase = true) ||
                tx.note.contains(query, ignoreCase = true)
        }
        // 账户筛选只影响列表（和搜索一样），上方的月度汇总是整月的口径
        val accountFilter = ledger.accountFilter
        val listed = if (accountFilter == null) searched
        else searched.filter { it.account == accountFilter }

        val groups = listed
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
        val sessionsByDay = sessions.groupBy { it.startedAtMillis.toLocalDate() }
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
        val weekSessions = sessions.filter {
            !it.startedAtMillis.toLocalDate().isBefore(monday)
        }
        val currentMonth = YearMonth.from(today)
        val monthSessions = sessions.filter {
            YearMonth.from(it.startedAtMillis.toLocalDate()) == currentMonth
        }

        // ---- 近 12 周热力图：列是周，行是周一到周日；未来日期记 -1，界面画成空格 ----
        val minutesByDay = sessions
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
        val perTodo = sessions
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
            totalCount = sessions.size,
            streak = streak,
            weekCount = weekSessions.size,
            weekMinutes = weekSessions.sumOf { it.minutes },
            monthCount = monthSessions.size,
            monthMinutes = monthSessions.sumOf { it.minutes },
            recentDays = recentDays,
            todaySessions = todaySessions,
            heatWeeks = heatWeeks,
            heatMaxMinutes = heatMaxMinutes,
            perTodo = perTodo
        )

        // ---- 年度报表（近 12 个月趋势 + 当年汇总）----
        val thisYear = today.year
        val yearTx = ledger.all.filter { it.dateMillis.toLocalDate().year == thisYear }
        val yearExpense = yearTx.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
        val yearIncome = yearTx.filter { it.type == TxType.INCOME }.sumOf { it.amountCents }
        val topExpense = yearTx
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .map { (category, items) -> category to items.sumOf { it.amountCents } }
            .maxByOrNull { it.second }
        val byMonth = ledger.all.groupBy { YearMonth.from(it.dateMillis.toLocalDate()) }
        val monthBars = (11 downTo 0).map { back ->
            val m = YearMonth.from(today).minusMonths(back.toLong())
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

        return UiState(
            month = month,
            monthGroups = groups,
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
            accountSlices = accountSlices,
            categoryBudgets = categoryBudgetRows,
            visibleTodos = visible,
            todoFilter = todo.filter,
            todoQuery = todo.query,
            pendingCount = pending,
            doneCount = todo.all.size - pending,
            focusTaskId = focusTaskId,
            focusTaskTitle = focusTaskTitle,
            focusStats = focusStats,
            yearSummary = yearSummary,
            totalIncomeCents = allIncome,
            totalExpenseCents = allExpense,
            totalCount = ledger.all.size
        )
    }
}
