package com.dailybook.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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

/** 一条待办 */
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val done: Boolean = false,
    val important: Boolean = false,
    /** 可选到期日（当天 00:00），null 表示不设置 */
    val dueMillis: Long? = null,
    val createdAt: Long
)

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
