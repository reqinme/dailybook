package com.dailybook.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

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
        nowMillis: Long = System.currentTimeMillis()
    ) {
        txDao.insert(
            TransactionEntity(
                amountCents = amountCents,
                typeName = type.name,
                category = category,
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
        dateMillis: Long
    ) {
        txDao.update(
            item.copy(
                amountCents = amountCents,
                typeName = type.name,
                category = category,
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
        nowMillis: Long = System.currentTimeMillis()
    ) {
        todoDao.insert(
            TodoEntity(
                title = title,
                dueMillis = dueMillis,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateTodo(item: TodoEntity) = todoDao.update(item)

    suspend fun setTodoDone(item: TodoEntity, done: Boolean) =
        todoDao.update(item.copy(done = done))

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

    // ---- 全局 ----

    suspend fun clearAll() {
        txDao.clearAll()
        todoDao.clearAll()
        sessionDao.clearAll()
    }
}
