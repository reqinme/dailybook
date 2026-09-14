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

    // ---- 备份 / 恢复用 ----

    @Query("SELECT * FROM transactions ORDER BY dateMillis ASC, id ASC")
    suspend fun getAll(): List<TransactionEntity>

    @Insert
    suspend fun insertAll(items: List<TransactionEntity>)
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

    /** 手工排序：把某条待办挪到新的次序 */
    @Query("UPDATE todos SET sortOrder = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Long)

    /** 优先级 */
    @Query("UPDATE todos SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: String)

    @Query("DELETE FROM todos")
    suspend fun clearAll()

    // ---- 备份 / 恢复 / 提醒用 ----

    @Query("SELECT * FROM todos ORDER BY id ASC")
    suspend fun getAll(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: Long): TodoEntity?

    /** 找一条「同名且同到期日、还没完成」的待办，用于避免重复任务被重复生成 */
    @Query(
        """
        SELECT * FROM todos
        WHERE title = :title AND dueMillis = :dueMillis AND done = 0
        LIMIT 1
        """
    )
    suspend fun findPending(title: String, dueMillis: Long): TodoEntity?

    @Insert
    suspend fun insertAll(items: List<TodoEntity>)
}
