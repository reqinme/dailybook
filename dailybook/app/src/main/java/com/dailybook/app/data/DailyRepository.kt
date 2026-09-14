package com.dailybook.app.data

import android.content.Context
import androidx.room.withTransaction
import com.dailybook.app.util.toDayMillis
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** 记账 + 待办 + 专注记录的数据入口 */
class DailyRepository(context: Context) {

    private val db = AppDatabase.get(context)
    private val txDao = db.transactionDao()
    private val todoDao = db.todoDao()
    private val sessionDao = db.focusSessionDao()
    private val subtaskDao = db.subtaskDao()
    private val recurringDao = db.recurringDao()

    val transactions: Flow<List<TransactionEntity>> = txDao.observeAll()
    val todos: Flow<List<TodoEntity>> = todoDao.observeAll()
    val focusSessions: Flow<List<FocusSessionEntity>> = sessionDao.observeAll()
    val subtasks: Flow<List<SubtaskEntity>> = subtaskDao.observeAll()
    val recurring: Flow<List<RecurringEntity>> = recurringDao.observeAll()

    // ---- 记账 ----

    suspend fun addTransaction(
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
        rateScaled: Long = Currencies.RATE_SCALE,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        txDao.insert(
            TransactionEntity(
                amountCents = amountCents,
                typeName = type.name,
                category = category,
                account = account.ifBlank { Accounts.DEFAULT },
                note = note,
                tags = TransactionEntity.joinTags(tags),
                reimbursable = reimbursable,
                currency = currency,
                foreignAmountCents = if (foreignAmountCents > 0L) foreignAmountCents else amountCents,
                rateScaled = rateScaled,
                dateMillis = dateMillis,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateTransaction(
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
        txDao.update(
            item.copy(
                amountCents = amountCents,
                typeName = type.name,
                category = category,
                account = account.ifBlank { Accounts.DEFAULT },
                note = note,
                tags = TransactionEntity.joinTags(tags),
                reimbursable = reimbursable,
                currency = currency,
                foreignAmountCents = if (foreignAmountCents > 0L) foreignAmountCents else amountCents,
                rateScaled = rateScaled,
                dateMillis = dateMillis
            )
        )
    }

    /** 标记一笔是否已经报销 */
    suspend fun setReimbursed(item: TransactionEntity, reimbursed: Boolean) =
        txDao.update(item.copy(reimbursed = reimbursed))

    suspend fun deleteTransaction(item: TransactionEntity) = txDao.delete(item)

    /** 批量写入（CSV 导入用） */
    suspend fun insertTransactions(items: List<TransactionEntity>) {
        if (items.isEmpty()) return
        txDao.insertAll(items)
    }

    suspend fun clearTransactions() = txDao.clearAll()

    // ---- 待办 ----

    suspend fun addTodo(
        title: String,
        dueMillis: Long?,
        repeatRule: RepeatRule = RepeatRule.NONE,
        priority: TodoPriority = TodoPriority.NORMAL,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        // 新待办排在最前面（sortOrder 越小越靠前）
        val minOrder = todoDao.getAll().minOfOrNull { it.sortOrder } ?: 0L
        todoDao.insert(
            TodoEntity(
                title = title,
                dueMillis = dueMillis,
                repeatRule = repeatRule.name,
                priority = priority.name,
                sortOrder = minOrder - 1L,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateTodo(item: TodoEntity) = todoDao.update(item)

    /** 手工排序：按传入顺序重排（列表拖动后调用） */
    suspend fun reorderTodos(ordered: List<TodoEntity>) {
        db.withTransaction {
            ordered.forEachIndexed { index, todo ->
                if (todo.sortOrder != index.toLong()) todoDao.updateSortOrder(todo.id, index.toLong())
            }
        }
    }

    // ---- 子任务 ----

    suspend fun subtasksOf(todoId: Long): List<SubtaskEntity> = subtaskDao.forTodo(todoId)

    suspend fun addSubtask(todoId: Long, title: String, nowMillis: Long = System.currentTimeMillis()) {
        val text = title.trim()
        if (text.isEmpty()) return
        val maxOrder = subtaskDao.forTodo(todoId).maxOfOrNull { it.sortOrder } ?: 0L
        subtaskDao.insert(
            SubtaskEntity(
                todoId = todoId,
                title = text,
                sortOrder = maxOrder + 1L,
                createdAt = nowMillis
            )
        )
    }

    suspend fun setSubtaskDone(item: SubtaskEntity, done: Boolean) =
        subtaskDao.update(item.copy(done = done))

    suspend fun deleteSubtask(item: SubtaskEntity) = subtaskDao.delete(item)

    /**
     * 勾选 / 取消勾选待办。
     * 勾上一条「重复任务」时，自动生成下一次的那条（未完成、到期日顺延到未来）；
     * 取消勾选只改状态，不生成新任务。
     */
    suspend fun setTodoDone(item: TodoEntity, done: Boolean) {
        if (!done || !item.repeats) {
            todoDao.update(item.copy(done = done))
            return
        }
        val base = item.dueMillis ?: LocalDate.now().toDayMillis()
        val next = nextDueMillisOf(base, item.repeat)
        db.withTransaction {
            todoDao.update(item.copy(done = true))
            // 同一个到期日只生成一条：连点两下勾选框（或先勾后取消再勾）都不会冒出重复的下一次
            if (next != null && todoDao.findPending(item.title, next) == null) {
                todoDao.insert(item.copy(id = 0L, done = false, dueMillis = next))
            }
        }
    }

    suspend fun toggleTodoImportant(item: TodoEntity) =
        todoDao.update(item.copy(important = !item.important))

    /** 删除待办时连它的子任务一起删，不留孤儿数据 */
    suspend fun deleteTodo(item: TodoEntity) {
        db.withTransaction {
            subtaskDao.deleteForTodo(item.id)
            todoDao.delete(item)
        }
    }

    suspend fun clearCompletedTodos() = todoDao.clearCompleted()

    suspend fun clearTodos() = todoDao.clearAll()

    // ---- 专注记录 ----

    suspend fun recordFocusSession(
        startedAtMillis: Long,
        endedAtMillis: Long,
        minutes: Int,
        taskTitle: String,
        interrupted: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        sessionDao.insert(
            FocusSessionEntity(
                startedAtMillis = startedAtMillis,
                endedAtMillis = endedAtMillis,
                minutes = minutes,
                taskTitle = taskTitle,
                interrupted = interrupted,
                createdAt = nowMillis
            )
        )
    }

    suspend fun clearFocusSessions() = sessionDao.clearAll()

    // ---- 周期记账 / 固定支出 ----

    suspend fun addRecurring(
        amountCents: Long,
        type: TxType,
        category: String,
        account: String,
        note: String,
        tags: List<String>,
        rule: RepeatRule,
        nextDueMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        recurringDao.insert(
            RecurringEntity(
                amountCents = amountCents,
                typeName = type.name,
                category = category,
                account = account.ifBlank { Accounts.DEFAULT },
                note = note,
                tags = TransactionEntity.joinTags(tags),
                rule = if (rule == RepeatRule.NONE) RepeatRule.MONTHLY.name else rule.name,
                nextDueMillis = nextDueMillis,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateRecurring(item: RecurringEntity) = recurringDao.update(item)

    suspend fun deleteRecurring(item: RecurringEntity) = recurringDao.delete(item)

    /**
     * 把到期的周期记账补成真实记录，并把下一次往后推。
     * 只补「到期的那一次」，长期没打开 App 也不会一次刷出一堆；
     * 返回这次补记了几笔（0 表示没有到期的）。
     */
    suspend fun materializeRecurring(today: LocalDate = LocalDate.now()): Int {
        val todayMillis = today.toDayMillis()
        val due = recurringDao.getAll().filter { it.enabled && it.nextDueMillis <= todayMillis }
        if (due.isEmpty()) return 0

        db.withTransaction {
            due.forEach { rule ->
                txDao.insert(
                    TransactionEntity(
                        amountCents = rule.amountCents,
                        typeName = rule.typeName,
                        category = rule.category,
                        account = rule.account,
                        note = rule.note,
                        tags = rule.tags,
                        dateMillis = rule.nextDueMillis,
                        createdAt = System.currentTimeMillis()
                    )
                )
                val next = nextDueMillisOf(rule.nextDueMillis, rule.repeat, today)
                    ?: rule.nextDueMillis
                recurringDao.update(rule.copy(nextDueMillis = next))
            }
        }
        return due.size
    }

    // ---- 备份 / 恢复 ----

    /** 读出全部数据，用于导出备份 */
    suspend fun snapshot(): DbSnapshot = DbSnapshot(
        transactions = txDao.getAll(),
        todos = todoDao.getAll(),
        focusSessions = sessionDao.getAll(),
        subtasks = subtaskDao.getAll(),
        recurring = recurringDao.getAll()
    )

    /** 用备份内容整体替换现有数据：先清空再写入，同一个事务内完成 */
    suspend fun restore(data: DbSnapshot) {
        db.withTransaction {
            txDao.clearAll()
            todoDao.clearAll()
            sessionDao.clearAll()
            subtaskDao.clearAll()
            recurringDao.clearAll()
            txDao.insertAll(data.transactions)
            todoDao.insertAll(data.todos)
            sessionDao.insertAll(data.focusSessions)
            subtaskDao.insertAll(data.subtasks)
            recurringDao.insertAll(data.recurring)
        }
    }

    // ---- 全局 ----

    suspend fun clearAll() {
        txDao.clearAll()
        todoDao.clearAll()
        sessionDao.clearAll()
        subtaskDao.clearAll()
        recurringDao.clearAll()
    }
}

/** 一份完整的数据库快照（备份文件的内容） */
data class DbSnapshot(
    val transactions: List<TransactionEntity>,
    val todos: List<TodoEntity>,
    val focusSessions: List<FocusSessionEntity>,
    val subtasks: List<SubtaskEntity> = emptyList(),
    val recurring: List<RecurringEntity> = emptyList()
)
