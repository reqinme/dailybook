package com.dailybook.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** 记账 + 待办的数据入口 */
class DailyRepository(context: Context) {

    private val db = AppDatabase.get(context)
    private val txDao = db.transactionDao()
    private val todoDao = db.todoDao()

    val transactions: Flow<List<TransactionEntity>> = txDao.observeAll()
    val todos: Flow<List<TodoEntity>> = todoDao.observeAll()

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

    suspend fun deleteTransaction(item: TransactionEntity) = txDao.delete(item)

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

    suspend fun clearTransactions() = txDao.clearAll()

    suspend fun clearTodos() = todoDao.clearAll()

    suspend fun clearAll() {
        txDao.clearAll()
        todoDao.clearAll()
    }
}
