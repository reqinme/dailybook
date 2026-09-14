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

    val transactions: Flow<List<TransactionEntity>> = txDao.observeAll()
    val todos: Flow<List<TodoEntity>> = todoDao.observeAll()
    val focusSessions: Flow<List<FocusSessionEntity>> = sessionDao.observeAll()

    // ---- 记账 ----

    suspend fun addTransaction(
        amountCents: Long,
        type: TxType,
        category: String,
        note: String,
        dateMillis: Long,
        account: String = Accounts.DEFAULT,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        txDao.insert(
            TransactionEntity(
                amountCents = amountCents,
                typeName = type.name,
                category = category,
                account = account.ifBlank { Accounts.DEFAULT },
                note = note,
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
        account: String = item.account
    ) {
        txDao.update(
            item.copy(
                amountCents = amountCents,
                typeName = type.name,
                category = category,
                account = account.ifBlank { Accounts.DEFAULT },
                note = note,
                dateMillis = dateMillis
            )
        )
    }

    suspend fun deleteTransaction(item: TransactionEntity) = txDao.delete(item)

    suspend fun clearTransactions() = txDao.clearAll()

    // ---- 待办 ----

    suspend fun addTodo(
        title: String,
        dueMillis: Long?,
        repeatRule: RepeatRule = RepeatRule.NONE,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        todoDao.insert(
            TodoEntity(
                title = title,
                dueMillis = dueMillis,
                repeatRule = repeatRule.name,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateTodo(item: TodoEntity) = todoDao.update(item)

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

    suspend fun deleteTodo(item: TodoEntity) = todoDao.delete(item)

    suspend fun clearCompletedTodos() = todoDao.clearCompleted()

    suspend fun clearTodos() = todoDao.clearAll()

    // ---- 专注记录 ----

    suspend fun recordFocusSession(
        startedAtMillis: Long,
        endedAtMillis: Long,
        minutes: Int,
        taskTitle: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        sessionDao.insert(
            FocusSessionEntity(
                startedAtMillis = startedAtMillis,
                endedAtMillis = endedAtMillis,
                minutes = minutes,
                taskTitle = taskTitle,
                createdAt = nowMillis
            )
        )
    }

    suspend fun clearFocusSessions() = sessionDao.clearAll()

    // ---- 备份 / 恢复 ----

    /** 读出全部数据，用于导出备份 */
    suspend fun snapshot(): DbSnapshot = DbSnapshot(
        transactions = txDao.getAll(),
        todos = todoDao.getAll(),
        focusSessions = sessionDao.getAll()
    )

    /** 用备份内容整体替换现有数据：先清空再写入，同一个事务内完成 */
    suspend fun restore(data: DbSnapshot) {
        db.withTransaction {
            txDao.clearAll()
            todoDao.clearAll()
            sessionDao.clearAll()
            txDao.insertAll(data.transactions)
            todoDao.insertAll(data.todos)
            sessionDao.insertAll(data.focusSessions)
        }
    }

    // ---- 全局 ----

    suspend fun clearAll() {
        txDao.clearAll()
        todoDao.clearAll()
        sessionDao.clearAll()
    }
}

/** 一份完整的数据库快照（备份文件的内容） */
data class DbSnapshot(
    val transactions: List<TransactionEntity>,
    val todos: List<TodoEntity>,
    val focusSessions: List<FocusSessionEntity>
)
