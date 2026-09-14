package com.dailybook.app

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailybook.app.data.DailyRepository
import com.dailybook.app.data.FocusSessionEntity
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.data.TodoEntity
import com.dailybook.app.data.TransactionEntity
import com.dailybook.app.data.TxType
import com.dailybook.app.ui.theme.ThemeMode
import com.dailybook.app.util.formatMonthLabel
import com.dailybook.app.util.toLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class TodoFilter(val label: String) {
    ALL("全部"),
    PENDING("待完成"),
    DONE("已完成")
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

/** 每日支出（柱状图） */
@Immutable
data class DayBar(val day: Int, val cents: Long)

/** 专注天数（柱状图） */
@Immutable
data class FocusDay(val date: LocalDate, val count: Int)

/** 专注统计（全部由 focus_sessions 表派生） */
@Immutable
data class FocusStats(
    val todayCount: Int = 0,
    val todayMinutes: Int = 0,
    val totalCount: Int = 0,
    val streak: Int = 0,
    val recentDays: List<FocusDay> = emptyList(),
    val todaySessions: List<FocusSessionEntity> = emptyList()
)

@Immutable
data class UiState(
    val month: YearMonth = YearMonth.now(),
    val monthLabel: String = formatMonthLabel(YearMonth.now()),
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
    val visibleTodos: List<TodoEntity> = emptyList(),
    val todoFilter: TodoFilter = TodoFilter.ALL,
    val todoQuery: String = "",
    val pendingCount: Int = 0,
    val doneCount: Int = 0,
    val focusTaskId: Long = SettingsStore.NO_TASK,
    val focusTaskTitle: String = "",
    val focusStats: FocusStats = FocusStats()
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
}

/** 记账侧的输入聚合，避免每次重组都重新拼装大量 flow */
private data class LedgerInputs(
    val all: List<TransactionEntity>,
    val month: YearMonth,
    val query: String,
    val budgetCents: Long
)

/** 待办侧的输入聚合 */
private data class TodoInputs(
    val all: List<TodoEntity>,
    val filter: TodoFilter,
    val query: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DailyRepository(application)

    /** 全局共享的设置存储（与 TimerViewModel 拿到的是同一个实例） */
    val settings: SettingsStore = SettingsStore.get(application)

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val todoFilter = MutableStateFlow(TodoFilter.ALL)
    private val ledgerQuery = MutableStateFlow("")
    private val todoQuery = MutableStateFlow("")

    private val ledgerInputs = combine(
        repo.transactions,
        selectedMonth,
        ledgerQuery,
        settings.monthlyBudgetCents
    ) { transactions, month, query, budget ->
        LedgerInputs(transactions, month, query, budget)
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

    // ---- 月份切换 ----

    fun previousMonth() { selectedMonth.value = selectedMonth.value.minusMonths(1) }

    fun nextMonth() { selectedMonth.value = selectedMonth.value.plusMonths(1) }

    fun goToCurrentMonth() { selectedMonth.value = YearMonth.now() }

    // ---- 搜索 / 筛选 ----

    fun setLedgerQuery(query: String) { ledgerQuery.value = query }

    fun setTodoFilter(filter: TodoFilter) { todoFilter.value = filter }

    fun setTodoQuery(query: String) { todoQuery.value = query }

    // ---- 记账 ----

    fun addTransaction(
        amountCents: Long,
        type: TxType,
        category: String,
        note: String,
        dateMillis: Long
    ) {
        viewModelScope.launch {
            repo.addTransaction(amountCents, type, category, note, dateMillis)
        }
    }

    /** 修改一条已有记录 */
    fun updateTransaction(
        item: TransactionEntity,
        amountCents: Long,
        type: TxType,
        category: String,
        note: String,
        dateMillis: Long
    ) {
        viewModelScope.launch {
            repo.updateTransaction(item, amountCents, type, category, note, dateMillis)
        }
    }

    fun deleteTransaction(item: TransactionEntity) {
        viewModelScope.launch { repo.deleteTransaction(item) }
    }

    // ---- 待办 ----

    fun addTodo(title: String, dueMillis: Long?) {
        val text = title.trim()
        if (text.isEmpty()) return
        viewModelScope.launch { repo.addTodo(text, dueMillis) }
    }

    fun toggleTodoDone(item: TodoEntity) {
        viewModelScope.launch { repo.setTodoDone(item, !item.done) }
    }

    fun toggleTodoImportant(item: TodoEntity) {
        viewModelScope.launch { repo.toggleTodoImportant(item) }
    }

    fun updateTodo(item: TodoEntity, title: String, important: Boolean, dueMillis: Long?) {
        val text = title.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repo.updateTodo(item.copy(title = text, important = important, dueMillis = dueMillis))
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

    fun setDynamicColor(enabled: Boolean) = settings.setDynamicColor(enabled)

    fun setMonthlyBudget(cents: Long) = settings.setMonthlyBudget(cents)

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

        val groups = searched
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

        val focusStats = FocusStats(
            todayCount = todaySessions.size,
            todayMinutes = todaySessions.sumOf { it.minutes },
            totalCount = sessions.size,
            streak = streak,
            recentDays = recentDays,
            todaySessions = todaySessions
        )

        return UiState(
            month = month,
            monthLabel = formatMonthLabel(month),
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
            visibleTodos = visible,
            todoFilter = todo.filter,
            todoQuery = todo.query,
            pendingCount = pending,
            doneCount = todo.all.size - pending,
            focusTaskId = focusTaskId,
            focusTaskTitle = focusTaskTitle,
            focusStats = focusStats
        )
    }
}
