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
// 学习模块（校园）：课表 / 考试与复习计划 / 成绩与学分 / 奖助记录
// 作业与 DDL 复用 todos（多了 courseName 字段），不另建表
// =====================================================================

/**
 * 一门课（课表里的一格）。
 *
 * 一节课就是一条记录：星期几 + 第几节到第几节 + 起止周。
 * `weeks` 存成「周次表达式」文本（例如 `1-16`、`1-16单`、`3,5,7-9`），
 * 由 `CourseWeeks` 解析，这样单双周、跳周都能表达，且导出/备份都是纯文本。
 *
 * [startMinutes] / [endMinutes] 是**真实钟点**（当天 00:00 起的分钟数），
 * 和「第几节」是两回事：节次是教学安排（第 1 节可能 8:00 也可能 8:30），
 * 只有钟点才能算出「提前多少分钟提醒」的准确时刻，所以上课提醒认的是这两个字段。
 * 老数据（v7 及更早）和没填时间的课都是 -1，此时排不出提醒，课表本身照常显示。
 */
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val teacher: String = "",
    val location: String = "",
    /** 1 = 周一 … 7 = 周日 */
    val dayOfWeek: Int = 1,
    /** 第几节开始 / 结束（1 起） */
    val startPeriod: Int = 1,
    val endPeriod: Int = 2,
    /** 周次表达式，空 = 每周都有 */
    val weeks: String = "",
    /** 学期起始日（当天 00:00），用来把「第几周」换算成真实日期 */
    val termStartMillis: Long,
    /** 上课开始时间：当天 00:00 起的分钟数；-1 = 还没填 */
    val startMinutes: Int = -1,
    /** 下课时间：同上；-1 = 还没填 */
    val endMinutes: Int = -1,
    val colorIndex: Int = 0,
    val note: String = "",
    val createdAt: Long
)

@Dao
interface CourseDao {

    @Query("SELECT * FROM courses ORDER BY dayOfWeek ASC, startPeriod ASC, id ASC")
    fun observeAll(): Flow<List<CourseEntity>>

    @Insert
    suspend fun insert(item: CourseEntity): Long

    @Update
    suspend fun update(item: CourseEntity)

    @Delete
    suspend fun delete(item: CourseEntity)

    @Query("DELETE FROM courses")
    suspend fun clearAll()

    @Query("SELECT * FROM courses ORDER BY id ASC")
    suspend fun getAll(): List<CourseEntity>

    @Insert
    suspend fun insertAll(items: List<CourseEntity>)
}

/** 考试：倒计时的主体 */
@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val courseName: String = "",
    /** 考试开始时间（精确到分钟） */
    val examMillis: Long,
    val location: String = "",
    val note: String = "",
    val createdAt: Long
)

/** 复习计划里的一条（挂在某场考试下） */
@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 属于哪场考试；0 = 不挂考试的独立复习任务 */
    val examId: Long = 0L,
    val title: String,
    val done: Boolean = false,
    /** 计划做这件事的日期（当天 00:00） */
    val dateMillis: Long,
    val sortOrder: Long = 0L,
    val createdAt: Long
)

@Dao
interface ExamDao {

    @Query("SELECT * FROM exams ORDER BY examMillis ASC, id ASC")
    fun observeAll(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM study_tasks ORDER BY dateMillis ASC, sortOrder ASC, id ASC")
    fun observeTasks(): Flow<List<StudyTaskEntity>>

    @Insert
    suspend fun insert(item: ExamEntity): Long

    @Update
    suspend fun update(item: ExamEntity)

    @Delete
    suspend fun delete(item: ExamEntity)

    @Insert
    suspend fun insertTask(item: StudyTaskEntity): Long

    @Update
    suspend fun updateTask(item: StudyTaskEntity)

    @Delete
    suspend fun deleteTask(item: StudyTaskEntity)

    @Query("DELETE FROM study_tasks WHERE examId = :examId")
    suspend fun deleteTasksOf(examId: Long)

    @Query("DELETE FROM exams")
    suspend fun clearAll()

    @Query("DELETE FROM study_tasks")
    suspend fun clearAllTasks()

    @Query("SELECT * FROM exams ORDER BY id ASC")
    suspend fun getAll(): List<ExamEntity>

    @Query("SELECT * FROM study_tasks ORDER BY id ASC")
    suspend fun getAllTasks(): List<StudyTaskEntity>

    @Insert
    suspend fun insertAll(items: List<ExamEntity>)

    @Insert
    suspend fun insertAllTasks(items: List<StudyTaskEntity>)
}

/** 成绩计分方式 */
enum class ScoreKind {
    /** 百分制，例如 92 */
    PERCENT,

    /** 五级制：优秀 / 良好 / 中等 / 及格 / 不及格 */
    GRADE,

    /** 直接给绩点，例如 4.0 */
    POINT
}

/**
 * 一门课的成绩。
 *
 * `credit` 用 Double 存学分（有 0.5 学分）；`score` 存原始字符串便于展示，
 * `point` 存换算后的绩点，算 GPA 只认 point —— 这样三种计分方式可以混着录。
 */
@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 学期标签，例如 2026春 / 2026-2027-1 */
    val term: String = "",
    val courseName: String,
    val credit: Double = 0.0,
    val score: String = "",
    val scoreKind: String = ScoreKind.PERCENT.name,
    /** 换算出的绩点（5 分制或 4 分制由用户在设置里选口径） */
    val point: Double = 0.0,
    /** 课程类别：必修 / 选修 / 通识（数据，不翻译） */
    val category: String = "必修",
    val note: String = "",
    val createdAt: Long
) {
    val kind: ScoreKind
        get() = runCatching { ScoreKind.valueOf(scoreKind) }.getOrDefault(ScoreKind.PERCENT)
}

/** 学分要求：按类别记录毕业需要的学分 */
@Entity(tableName = "credit_targets")
data class CreditTargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String,
    val required: Double = 0.0,
    val createdAt: Long
)

/** 奖助 / 竞赛 / 证书 */
enum class AwardKind { SCHOLARSHIP, CONTEST, CERTIFICATE, OTHER }

@Entity(tableName = "awards")
data class AwardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val kind: String = AwardKind.SCHOLARSHIP.name,
    val dateMillis: Long,
    /** 级别：国家级 / 省级 / 校级 / 院级（数据，不翻译） */
    val level: String = "",
    val note: String = "",
    val imageUri: String = "",
    val createdAt: Long
) {
    val awardKind: AwardKind
        get() = runCatching { AwardKind.valueOf(kind) }.getOrDefault(AwardKind.OTHER)
}

@Dao
interface StudyDao {

    @Query("SELECT * FROM grades ORDER BY term DESC, id DESC")
    fun observeGrades(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM credit_targets ORDER BY id ASC")
    fun observeTargets(): Flow<List<CreditTargetEntity>>

    @Query("SELECT * FROM awards ORDER BY dateMillis DESC, id DESC")
    fun observeAwards(): Flow<List<AwardEntity>>

    @Insert
    suspend fun insertGrade(item: GradeEntity): Long

    @Update
    suspend fun updateGrade(item: GradeEntity)

    @Delete
    suspend fun deleteGrade(item: GradeEntity)

    @Insert
    suspend fun insertTarget(item: CreditTargetEntity): Long

    @Update
    suspend fun updateTarget(item: CreditTargetEntity)

    @Delete
    suspend fun deleteTarget(item: CreditTargetEntity)

    @Insert
    suspend fun insertAward(item: AwardEntity): Long

    @Update
    suspend fun updateAward(item: AwardEntity)

    @Delete
    suspend fun deleteAward(item: AwardEntity)

    @Query("DELETE FROM grades")
    suspend fun clearGrades()

    @Query("DELETE FROM credit_targets")
    suspend fun clearTargets()

    @Query("DELETE FROM awards")
    suspend fun clearAwards()

    @Query("SELECT * FROM grades ORDER BY id ASC")
    suspend fun getAllGrades(): List<GradeEntity>

    @Query("SELECT * FROM credit_targets ORDER BY id ASC")
    suspend fun getAllTargets(): List<CreditTargetEntity>

    @Query("SELECT * FROM awards ORDER BY id ASC")
    suspend fun getAllAwards(): List<AwardEntity>

    @Insert
    suspend fun insertAllGrades(items: List<GradeEntity>)

    @Insert
    suspend fun insertAllTargets(items: List<CreditTargetEntity>)

    @Insert
    suspend fun insertAllAwards(items: List<AwardEntity>)
}
