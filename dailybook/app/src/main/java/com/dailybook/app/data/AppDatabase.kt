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
        FocusSessionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun todoDao(): TodoDao
    abstract fun focusSessionDao(): FocusSessionDao

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

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dailybook.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
