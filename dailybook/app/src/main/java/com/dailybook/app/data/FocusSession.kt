package com.dailybook.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 一次完成的专注记录（用于统计页的时段明细） */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    /** 实际专注分钟数 */
    val minutes: Int,
    /** 这次专注关联的待办标题，没有则为空 */
    val taskTitle: String = "",
    val createdAt: Long
)

@Dao
interface FocusSessionDao {

    @Query("SELECT * FROM focus_sessions ORDER BY startedAtMillis DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Insert
    suspend fun insert(item: FocusSessionEntity): Long

    @Delete
    suspend fun delete(item: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAll()
}
