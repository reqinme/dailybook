package com.dailybook.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        TodoEntity::class,
        FocusSessionEntity::class,
        SubtaskEntity::class,
        RecurringEntity::class,
        MemoEntity::class,
        MilestoneEntity::class,
        ImportantDateEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        CourseEntity::class,
        ExamEntity::class,
        StudyTaskEntity::class,
        GradeEntity::class,
        CreditTargetEntity::class,
        AwardEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun todoDao(): TodoDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun recurringDao(): RecurringDao
    abstract fun memoDao(): MemoDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun importantDateDao(): ImportantDateDao
    abstract fun habitDao(): HabitDao
    abstract fun courseDao(): CourseDao
    abstract fun examDao(): ExamDao
    abstract fun studyDao(): StudyDao

    companion object {
        /** v1.1 → v1.2：新增专注记录表（老用户的记账/待办数据完整保留） */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `focus_sessions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`startedAtMillis` INTEGER NOT NULL, " +
                        "`endedAtMillis` INTEGER NOT NULL, " +
                        "`minutes` INTEGER NOT NULL, " +
                        "`taskTitle` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        /** v1.2 → v1.3：待办新增「重复规则」列（老数据默认不重复） */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `todos` ADD COLUMN `repeatRule` TEXT NOT NULL DEFAULT 'NONE'"
                )
            }
        }

        /** v1.3 → v1.4：记账新增「账户」列（老数据一律算作现金） */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `transactions` ADD COLUMN `account` TEXT NOT NULL DEFAULT '现金'"
                )
            }
        }

        /** v1.4 → v1.5：记账新增标签 / 待报销 / 多币种列，专注记录新增「中断」列 */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "ALTER TABLE `transactions` ADD COLUMN `reimbursable` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `transactions` ADD COLUMN `reimbursed` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `transactions` ADD COLUMN `currency` TEXT NOT NULL DEFAULT 'CNY'"
                )
                db.execSQL(
                    "ALTER TABLE `transactions` ADD COLUMN `foreignAmountCents` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `transactions` ADD COLUMN `rateScaled` INTEGER NOT NULL DEFAULT 10000"
                )
                db.execSQL(
                    "ALTER TABLE `focus_sessions` ADD COLUMN `interrupted` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** v1.5 → v1.6：待办新增优先级与手动排序，并新建子任务表、周期记账表 */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `todos` ADD COLUMN `priority` TEXT NOT NULL DEFAULT 'NORMAL'"
                )
                db.execSQL("ALTER TABLE `todos` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `subtasks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`todoId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`done` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recurring_tx` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`amountCents` INTEGER NOT NULL, " +
                        "`typeName` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`account` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`tags` TEXT NOT NULL DEFAULT '', " +
                        "`rule` TEXT NOT NULL, " +
                        "`nextDueMillis` INTEGER NOT NULL, " +
                        "`enabled` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        /** v1.7 → v1.8：待办新增「课程名」列（作业/DDL 复用待办，老数据为空串） */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `todos` ADD COLUMN `courseName` TEXT NOT NULL DEFAULT ''"
                )

                // ---- 生活模块 ----
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `memos` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`content` TEXT NOT NULL, " +
                        "`pinned` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `milestones` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`dateMillis` INTEGER NOT NULL, " +
                        "`imageUri` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `important_dates` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`dateMillis` INTEGER NOT NULL, " +
                        "`lunar` INTEGER NOT NULL, " +
                        "`lunarMonth` INTEGER NOT NULL, " +
                        "`lunarDay` INTEGER NOT NULL, " +
                        "`lunarLeap` INTEGER NOT NULL, " +
                        "`repeat` TEXT NOT NULL, " +
                        "`remindDaysBefore` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `habits` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`emoji` TEXT NOT NULL, " +
                        "`targetPerDay` INTEGER NOT NULL, " +
                        "`unit` TEXT NOT NULL, " +
                        "`daysPerWeek` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "`archived` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `habit_logs` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`habitId` INTEGER NOT NULL, " +
                        "`dateMillis` INTEGER NOT NULL, " +
                        "`count` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )

                // ---- 学习模块 ----
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `courses` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`teacher` TEXT NOT NULL, " +
                        "`location` TEXT NOT NULL, " +
                        "`dayOfWeek` INTEGER NOT NULL, " +
                        "`startPeriod` INTEGER NOT NULL, " +
                        "`endPeriod` INTEGER NOT NULL, " +
                        "`weeks` TEXT NOT NULL, " +
                        "`termStartMillis` INTEGER NOT NULL, " +
                        "`colorIndex` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `exams` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`courseName` TEXT NOT NULL, " +
                        "`examMillis` INTEGER NOT NULL, " +
                        "`location` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `study_tasks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`examId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`done` INTEGER NOT NULL, " +
                        "`dateMillis` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `grades` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`term` TEXT NOT NULL, " +
                        "`courseName` TEXT NOT NULL, " +
                        "`credit` REAL NOT NULL, " +
                        "`score` TEXT NOT NULL, " +
                        "`scoreKind` TEXT NOT NULL, " +
                        "`point` REAL NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `credit_targets` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`required` REAL NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `awards` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, " +
                        "`dateMillis` INTEGER NOT NULL, " +
                        "`level` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`imageUri` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dailybook.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build()
                    .also { instance = it }
            }
    }
}
