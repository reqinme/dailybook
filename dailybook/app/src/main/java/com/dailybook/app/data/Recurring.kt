package com.dailybook.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 周期记账 / 固定支出：房租、订阅这类每月固定的一笔。
 *
 * 不跑后台服务，靠「打开 App 或每晚提醒时补记」把到期的规则落成真实记录，
 * 所以不会因为 App 没运行就漏账，也不会重复记（记完就把 nextDueMillis 往后推）。
 */
@Entity(tableName = "recurring_tx")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amountCents: Long,
    val typeName: String,
    val category: String,
    val account: String = Accounts.DEFAULT,
    val note: String = "",
    val tags: String = "",
    /** 重复规则，复用待办那套：每天 / 每周 / 每月 */
    val rule: String = RepeatRule.MONTHLY.name,
    /** 下一次该记的日期（当天 00:00） */
    val nextDueMillis: Long,
    val enabled: Boolean = true,
    val createdAt: Long
) {
    val repeat: RepeatRule
        get() = runCatching { RepeatRule.valueOf(rule) }.getOrDefault(RepeatRule.MONTHLY)

    val type: TxType
        get() = runCatching { TxType.valueOf(typeName) }.getOrDefault(TxType.EXPENSE)
}

@Dao
interface RecurringDao {

    @Query("SELECT * FROM recurring_tx ORDER BY nextDueMillis ASC, id ASC")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Insert
    suspend fun insert(item: RecurringEntity): Long

    @Update
    suspend fun update(item: RecurringEntity)

    @Delete
    suspend fun delete(item: RecurringEntity)

    @Query("DELETE FROM recurring_tx")
    suspend fun clearAll()

    // ---- 备份 / 恢复 ----

    @Query("SELECT * FROM recurring_tx ORDER BY id ASC")
    suspend fun getAll(): List<RecurringEntity>

    @Insert
    suspend fun insertAll(items: List<RecurringEntity>)
}
