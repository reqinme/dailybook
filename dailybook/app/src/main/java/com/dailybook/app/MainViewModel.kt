package com.dailybook.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailybook.app.data.DailyRepository
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.data.TodoEntity
import com.dailybook.app.data.TransactionEntity
import com.dailybook.app.data.TxType
import com.dailybook.app.ui.theme.ThemeMode
import com.dailybook.app.util.formatMonthLabel
import com.dailybook.app.util.toDayMillis
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
data class DayGroup(
    val date: LocalDate,
    val expense: Long,
    val income: Long,
    val items: List<TransactionEntity>
)

/** 分类占比 */
data class CategorySlice(val category: String, val cents: Long, val ratio: Float)

/** 每日支出（柱状图） */
data class DayBar(val day: Int, val cents: Long)

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
    val todos: List<TodoEntity> = emptyList(),
    val visibleTodos: List<TodoEntity> = emptyList(),
    val todoFilter: TodoFilter = TodoFilter.ALL,
    val pendingCount: Int = 0,
    val doneCount: Int = 0
) {
    val balance: Long get() = monthIncome - monthExpense
    val hasMonthData: Boolean get() = monthGroups.isNotEmpty()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DailyRepository(application)
    val settings = SettingsStore(application)

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val todoFilter = MutableStateFlow(TodoFilter.ALL)

    val uiState: StateFlow<UiState> = combine(
        repo.transactions,
        repo.todos,
        selectedMonth,
        todoFilter
    ) { transactions, todos, month, filter ->
        buildState(transactions, todos, month, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    // ---- 月份切换 ----

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun goToCurrentMonth() {
        selectedMonth.value = YearMonth.now()
    }

    // ---- 待办筛选 ----

    fun setTodoFilter(filter: TodoFilter) {
        todoFilter.value = filter
    }

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
        viewModelScope.launch { repo.deleteTodo(item) }
    }

    // ---- 设置 ----

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)

    fun setDynamicColor(enabled: Boolean) = settings.setDynamicColor(enabled)

    fun clearTransactions() {
        viewModelScope.launch { repo.clearTransactions() }
    }

    fun clearTodos() {
        viewModelScope.launch { repo.clearTodos() }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearAll() }
    }

    private fun buildState(
        allTransactions: List<TransactionEntity>,
        allTodos: List<TodoEntity>,
        month: YearMonth,
        filter: TodoFilter
    ): UiState {
        val today = LocalDate.now()

        val monthTx = allTransactions.filter { YearMonth.from(it.dateMillis.toLocalDate()) == month }

        val groups = monthTx
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
        val todayExpense = allTransactions
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

        val dayBars = (1..month.lengthOfMonth()).map { day ->
            val cents = monthTx
                .filter {
                    it.type == TxType.EXPENSE &&
                        it.dateMillis.toLocalDate().dayOfMonth == day
                }
                .sumOf { it.amountCents }
            DayBar(day, cents)
        }

        val pending = allTodos.count { !it.done }
        val done = allTodos.size - pending
        val visible = when (filter) {
            TodoFilter.ALL -> allTodos
            TodoFilter.PENDING -> allTodos.filter { !it.done }
            TodoFilter.DONE -> allTodos.filter { it.done }
        }

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
            todos = allTodos,
            visibleTodos = visible,
            todoFilter = filter,
            pendingCount = pending,
            doneCount = done
        )
    }
}
