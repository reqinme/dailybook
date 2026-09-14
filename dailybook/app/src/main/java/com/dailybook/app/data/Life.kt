package com.dailybook.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// =====================================================================
// 生活模块：备忘录 / 大事记 / 重要日期 / 习惯打卡
// =====================================================================

/** 备忘录：随手记一条，可以置顶 */
@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val content: String = "",
    val pinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface MemoDao {

    @Query("SELECT * FROM memos ORDER BY pinned DESC, updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<MemoEntity>>

    @Insert
    suspend fun insert(item: MemoEntity): Long

    @Update
    suspend fun update(item: MemoEntity)

    @Delete
    suspend fun delete(item: MemoEntity)

    @Query("DELETE FROM memos")
    suspend fun clearAll()

    @Query("SELECT * FROM memos ORDER BY id ASC")
    suspend fun getAll(): List<MemoEntity>

    @Insert
    suspend fun insertAll(items: List<MemoEntity>)
}

/**
 * 大事记：一条一条的时间线。
 * 和「重要日期」的区别：大事记记的是**已经发生**的事（毕业、入职、第一次旅行），
 * 重要日期记的是**每年会回来**的日子（生日、纪念日）。
 */
@Entity(tableName = "milestones")
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val note: String = "",
    /** 事件发生的那一天（当天 00:00） */
    val dateMillis: Long,
    /** 可选的配图（SAF 授权后的 URI 字符串），没图就是空 */
    val imageUri: String = "",
    val createdAt: Long
)

@Dao
interface MilestoneDao {

    @Query("SELECT * FROM milestones ORDER BY dateMillis DESC, id DESC")
    fun observeAll(): Flow<List<MilestoneEntity>>

    @Insert
    suspend fun insert(item: MilestoneEntity): Long

    @Update
    suspend fun update(item: MilestoneEntity)

    @Delete
    suspend fun delete(item: MilestoneEntity)

    @Query("DELETE FROM milestones")
    suspend fun clearAll()

    @Query("SELECT * FROM milestones ORDER BY id ASC")
    suspend fun getAll(): List<MilestoneEntity>

    @Insert
    suspend fun insertAll(items: List<MilestoneEntity>)
}

/** 重要日期的重复方式（比待办那套多「每年」和「只过一次」） */
enum class DateRepeat {
    /** 只过一次（例如某个具体的考试、纪念活动） */
    ONCE,

    /** 每年同一天（生日、纪念日）——农历日期也走这里，只是换算方式不同 */
    YEARLY,

    /** 每月同一天 */
    MONTHLY,

    /** 每周同一天 */
    WEEKLY
}

/**
 * 重要日期 / 倒计时。
 *
 * 农历用 `lunar = true` 表示，并按「农历月 + 农历日」存（`lunarMonth` / `lunarDay`），
 * 阳历则用 `dateMillis`。`lunarLeapMonth` 记录是不是闰月——
 * 闰月日期每年不一定出现，转换不出来时按「当月同一天」处理并在界面上标注。
 */
@Entity(tableName = "important_dates")
data class ImportantDateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    /** 阳历日期（当天 00:00）；农历日期也存一份「首次换算出的阳历日」，方便排序 */
    val dateMillis: Long,
    /** true = 按农历算 */
    val lunar: Boolean = false,
    /** 农历月 1~12 */
    val lunarMonth: Int = 1,
    /** 农历日 1~30 */
    val lunarDay: Int = 1,
    /** 是否闰月 */
    val lunarLeap: Boolean = false,
    val repeat: String = DateRepeat.YEARLY.name,
    /** 提前几天提醒，0 = 当天 */
    val remindDaysBefore: Int = 0,
    val note: String = "",
    val createdAt: Long
) {
    val repeatRule: DateRepeat
        get() = runCatching { DateRepeat.valueOf(repeat) }.getOrDefault(DateRepeat.YEARLY)
}

@Dao
interface ImportantDateDao {

    @Query("SELECT * FROM important_dates ORDER BY dateMillis ASC, id ASC")
    fun observeAll(): Flow<List<ImportantDateEntity>>

    @Insert
    suspend fun insert(item: ImportantDateEntity): Long

    @Update
    suspend fun update(item: ImportantDateEntity)

    @Delete
    suspend fun delete(item: ImportantDateEntity)

    @Query("DELETE FROM important_dates")
    suspend fun clearAll()

    @Query("SELECT * FROM important_dates ORDER BY id ASC")
    suspend fun getAll(): List<ImportantDateEntity>

    /**
     * 按 id 取一条；没有就返回 null。
     *
     * 提醒接收器要用它：闹钟到点时必须**重新读一次库**，因为那条日期可能已经被删或改过了 ——
     * 不能用闹钟排程时缓存下来的旧对象发通知。
     */
    @Query("SELECT * FROM important_dates WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ImportantDateEntity?

    @Insert
    suspend fun insertAll(items: List<ImportantDateEntity>)
}

/**
 * 习惯（也用来承载「背单词计划」这类每日定量任务）。
 *
 * `targetPerDay` 是每天的目标次数 / 数量（背单词就是每天多少个），
 * `unit` 是单位文字（次 / 个 / 页），显示成「12 / 20 个」。
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val emoji: String = "✅",
    /** 每天目标量，至少 1 */
    val targetPerDay: Int = 1,
    /** 单位（数据，不翻译）：次 / 个 / 页 */
    val unit: String = "次",
    /** 每周目标天数，1~7；用来算「本周完成度」 */
    val daysPerWeek: Int = 7,
    /** 排序用 */
    val sortOrder: Long = 0L,
    val archived: Boolean = false,
    val createdAt: Long
)

/** 习惯打卡记录：一天一条，累计当天完成量 */
@Entity(tableName = "habit_logs")
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val habitId: Long,
    /** 打卡日期（当天 00:00） */
    val dateMillis: Long,
    val count: Int = 1,
    val note: String = "",
    val createdAt: Long
)

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_logs ORDER BY dateMillis DESC, id DESC")
    fun observeLogs(): Flow<List<HabitLogEntity>>

    @Insert
    suspend fun insert(item: HabitEntity): Long

    @Update
    suspend fun update(item: HabitEntity)

    @Delete
    suspend fun delete(item: HabitEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsOf(habitId: Long)

    @Insert
    suspend fun insertLog(item: HabitLogEntity): Long

    @Update
    suspend fun updateLog(item: HabitLogEntity)

    @Delete
    suspend fun deleteLog(item: HabitLogEntity)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND dateMillis = :dayMillis LIMIT 1")
    suspend fun logOf(habitId: Long, dayMillis: Long): HabitLogEntity?

    @Query("DELETE FROM habits")
    suspend fun clearAll()

    @Query("DELETE FROM habit_logs")
    suspend fun clearAllLogs()

    @Query("SELECT * FROM habits ORDER BY id ASC")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habit_logs ORDER BY id ASC")
    suspend fun getAllLogs(): List<HabitLogEntity>

    @Insert
    suspend fun insertAll(items: List<HabitEntity>)

    @Insert
    suspend fun insertAllLogs(items: List<HabitLogEntity>)
}
