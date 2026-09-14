package com.dailybook.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 待办下面的一个小步骤（子任务 / 清单） */
@Entity(tableName = "subtasks")
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 挂在哪条待办下 */
    val todoId: Long,
    val title: String,
    val done: Boolean = false,
    val sortOrder: Long = 0L,
    val createdAt: Long
)

@Dao
interface SubtaskDao {

    @Query("SELECT * FROM subtasks ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE todoId = :todoId ORDER BY sortOrder ASC, id ASC")
    suspend fun forTodo(todoId: Long): List<SubtaskEntity>

    @Insert
    suspend fun insert(item: SubtaskEntity): Long

    @Update
    suspend fun update(item: SubtaskEntity)

    @Delete
    suspend fun delete(item: SubtaskEntity)

    /** 删待办时把它的子任务一起带走，避免留下孤儿数据 */
    @Query("DELETE FROM subtasks WHERE todoId = :todoId")
    suspend fun deleteForTodo(todoId: Long)

    @Query("DELETE FROM subtasks")
    suspend fun clearAll()

    // ---- 备份 / 恢复 ----

    @Query("SELECT * FROM subtasks ORDER BY id ASC")
    suspend fun getAll(): List<SubtaskEntity>

    @Insert
    suspend fun insertAll(items: List<SubtaskEntity>)
}
