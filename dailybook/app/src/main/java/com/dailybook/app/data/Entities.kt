package com.dailybook.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import java.time.LocalDate

/** 收支类型 */
enum class TxType(val label: String) {
    EXPENSE("支出"),
    INCOME("收入")
}

/** 一条记账记录（金额以「分」为单位存储，避免浮点误差） */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amountCents: Long,
    /** 存枚举名，避免依赖 Room 的枚举转换 */
    val typeName: String,
    val category: String,
    val note: String = "",
    /** 记账日期（当天 00:00 的毫秒时间戳） */
    val dateMillis: Long,
    val createdAt: Long
) {
    val type: TxType
        get() = runCatching { TxType.valueOf(typeName) }.getOrDefault(TxType.EXPENSE)
}

/** 待办的重复规则 */
enum class RepeatRule(val label: String) {
    NONE("不重复"),
    DAILY("每天"),
    WEEKLY("每周"),
    MONTHLY("每月")
}

/** 一条待办 */
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val done: Boolean = false,
    val important: Boolean = false,
    /** 可选到期日（当天 00:00），null 表示不设置 */
    val dueMillis: Long? = null,
    /** 存枚举名，避免依赖 Room 的枚举转换 */
    val repeatRule: String = RepeatRule.NONE.name,
    val createdAt: Long
) {
    val repeat: RepeatRule
        get() = runCatching { RepeatRule.valueOf(repeatRule) }.getOrDefault(RepeatRule.NONE)

    val repeats: Boolean get() = repeat != RepeatRule.NONE
}

/**
 * 依据重复规则算出下一次到期日。
 * 长期没打开 App 时（例如「每天」的任务放了 10 天），会一路顺延到未来，
 * 不会补出一串过期任务；guard 只是防止极端参数下的死循环。
 */
fun nextDueMillisOf(
    baseMillis: Long,
    rule: RepeatRule,
    today: LocalDate = LocalDate.now()
): Long? {
    if (rule == RepeatRule.NONE) return null
    var date = baseMillis.toLocalDate()
    var guard = 0
    do {
        date = when (rule) {
            RepeatRule.DAILY -> date.plusDays(1)
            RepeatRule.WEEKLY -> date.plusWeeks(1)
            RepeatRule.MONTHLY -> date.plusMonths(1)
            RepeatRule.NONE -> date
        }
        guard++
    } while (!date.isAfter(today) && guard < 400)
    return date.toDayMillis()
}

/** 预置分类 */
object Categories {
    val EXPENSE = listOf(
        "餐饮", "交通", "购物", "居住", "娱乐",
        "医疗", "学习", "人情", "通讯", "其他"
    )
    val INCOME = listOf(
        "工资", "奖金", "理财", "兼职", "红包", "报销", "其他"
    )

    fun forType(type: TxType): List<String> = if (type == TxType.EXPENSE) EXPENSE else INCOME

    /** 分类对应的 emoji，用于列表左侧的小图标 */
    fun emojiOf(category: String): String = when (category) {
        "餐饮" -> "🍜"
        "交通" -> "🚇"
        "购物" -> "🛍️"
        "居住" -> "🏠"
        "娱乐" -> "🎬"
        "医疗" -> "💊"
        "学习" -> "📚"
        "人情" -> "🎁"
        "通讯" -> "📱"
        "工资" -> "💰"
        "奖金" -> "🏆"
        "理财" -> "📈"
        "兼职" -> "💼"
        "红包" -> "🧧"
        "报销" -> "🧾"
        else -> "📌"
    }
}
