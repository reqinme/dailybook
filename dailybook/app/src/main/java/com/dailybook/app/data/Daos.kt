package com.dailybook.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(item: TransactionEntity): Long

    @Update
    suspend fun update(item: TransactionEntity)

    @Delete
    suspend fun delete(item: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface TodoDao {

    @Query(
        """
        SELECT * FROM todos
        ORDER BY done ASC,
                 important DESC,
                 CASE WHEN dueMillis IS NULL THEN 1 ELSE 0 END ASC,
                 dueMillis ASC,
                 id DESC
        """
    )
    fun observeAll(): Flow<List<TodoEntity>>

    @Insert
    suspend fun insert(item: TodoEntity): Long

    @Update
    suspend fun update(item: TodoEntity)

    @Delete
    suspend fun delete(item: TodoEntity)

    /** 批量清除已完成 */
    @Query("DELETE FROM todos WHERE done = 1")
    suspend fun clearCompleted()

    @Query("DELETE FROM todos")
    suspend fun clearAll()
}
