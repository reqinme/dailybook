package com.dailybook.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 收支类型 */
enum class TxType {
    EXPENSE,
    INCOME;

    fun label(lang: Lang): String = when (this) {
        EXPENSE -> AppStrings.txExpense(lang)
        INCOME -> AppStrings.txIncome(lang)
    }
}

/** 一条记账记录（金额以「分」为单位存储，避免浮点误差） */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 折成人民币后的金额（分）。所有统计口径都用它，多币种只是多存一份原始信息 */
    val amountCents: Long,
    /** 存枚举名，避免依赖 Room 的枚举转换 */
    val typeName: String,
    val category: String,
    /** 账户（现金 / 微信 / 支付宝 / 银行卡，或自定义），老数据默认「现金」 */
    val account: String = Accounts.DEFAULT,
    val note: String = "",
    /** 标签，逗号分隔（存字符串，方便导出与旧数据兼容） */
    val tags: String = "",
    /** 待报销 / 已报销：两笔独立的标记，统计里能把待报销的钱单拎出来 */
    val reimbursable: Boolean = false,
    val reimbursed: Boolean = false,
    /** 币种；CNY 为本位币 */
    val currency: String = Currencies.BASE,
    /** 原币金额（分）；本位币记录等于 amountCents */
    val foreignAmountCents: Long = 0L,
    /** 汇率 ×[Currencies.RATE_SCALE]（1 外币 = rate / RATE_SCALE 元）；本位币为 RATE_SCALE */
    val rateScaled: Long = Currencies.RATE_SCALE,
    /** 记账日期（当天 00:00 的毫秒时间戳） */
    val dateMillis: Long,
    val createdAt: Long
) {
    val type: TxType
        get() = runCatching { TxType.valueOf(typeName) }.getOrDefault(TxType.EXPENSE)

    val tagList: List<String>
        get() = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    /** 是否是外币记录（需要用原币展示） */
    val isForeign: Boolean get() = currency != Currencies.BASE && foreignAmountCents > 0L

    companion object {
        fun joinTags(tags: List<String>): String =
            tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")
    }
}

/** 币种：本位币是人民币，其余按用户填的汇率折算 */
object Currencies {
    const val BASE = "CNY"

    /** 汇率放大倍数：汇率存整数，1 外币 = rate / RATE_SCALE 元 */
    const val RATE_SCALE = 10_000L

    val PRESETS = listOf("CNY", "USD", "JPY", "EUR", "HKD", "GBP")

    fun symbolOf(code: String): String = when (code) {
        "CNY" -> "¥"
        "USD" -> "$"
        "JPY" -> "¥"
        "EUR" -> "€"
        "HKD" -> "HK$"
        "GBP" -> "£"
        else -> ""
    }

    /** 用原币金额与汇率算人民币金额（分），四舍五入 */
    fun toBaseCents(foreignCents: Long, rateScaled: Long): Long {
        if (rateScaled <= 0L) return foreignCents
        return (foreignCents * rateScaled + RATE_SCALE / 2) / RATE_SCALE
    }
}

/** 账户：预置几个常用渠道，也允许用户自己写 */
object Accounts {
    const val DEFAULT = "现金"

    val PRESETS = listOf("现金", "微信", "支付宝", "银行卡", "其他")

    fun emojiOf(account: String): String = when (account) {
        "现金" -> "💵"
        "微信" -> "💬"
        "支付宝" -> "🅰️"
        "银行卡" -> "💳"
        "信用卡" -> "💳"
        "其他" -> "📦"
        else -> "🏷️"
    }
}

/** 待办的重复规则 */
enum class RepeatRule {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY;

    fun label(lang: Lang): String = when (this) {
        NONE -> AppStrings.repeatNone(lang)
        DAILY -> AppStrings.repeatDaily(lang)
        WEEKLY -> AppStrings.repeatWeekly(lang)
        MONTHLY -> AppStrings.repeatMonthly(lang)
    }
}

/** 待办优先级 */
enum class TodoPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT;

    fun label(lang: Lang): String = when (this) {
        LOW -> AppStrings.priorityLow(lang)
        NORMAL -> AppStrings.priorityNormal(lang)
        HIGH -> AppStrings.priorityHigh(lang)
        URGENT -> AppStrings.priorityUrgent(lang)
    }

    fun emoji(): String = when (this) {
        LOW -> "▽"
        NORMAL -> ""
        HIGH -> "▲"
        URGENT -> "🔥"
    }
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
    /** 优先级，老数据默认普通 */
    val priority: String = TodoPriority.NORMAL.name,
    /** 手动排序用的次序（越小越靠前），0 表示还没排过 */
    val sortOrder: Long = 0L,
    /** 属于哪门课（作业 / DDL 用；普通待办为空串） */
    val courseName: String = "",
    val createdAt: Long
) {
    val repeat: RepeatRule
        get() = runCatching { RepeatRule.valueOf(repeatRule) }.getOrDefault(RepeatRule.NONE)

    val repeats: Boolean get() = repeat != RepeatRule.NONE

    val priorityLevel: TodoPriority
        get() = runCatching { TodoPriority.valueOf(priority) }.getOrDefault(TodoPriority.NORMAL)
}

/**
 * 依据重复规则算出下一次到期日。
 * 长期没打开 App 时（例如「每天」的任务放了 10 天），会一路顺延到未来，
 * 不会补出一串过期任务。
 *
 * 实现上先按天 / 周 / 月**大步快进**到今天附近，再用小循环校正到「严格晚于今天」。
 * 这一点很关键：早期版本只用一个 400 次的循环上限保护，遇到很多年前的「每天」规则
 * 会在 400 次后停下，把结果留在**过去**——那会让周期记账每次打开 App 都判定为到期而重复记账。
 */
fun nextDueMillisOf(
    baseMillis: Long,
    rule: RepeatRule,
    today: LocalDate = LocalDate.now()
): Long? {
    if (rule == RepeatRule.NONE) return null
    var date = baseMillis.toLocalDate()

    // 第一步：大步跳到今天附近（最多差一个周期），避免从很多年前一天天推进
    when (rule) {
        RepeatRule.DAILY -> {
            val days = ChronoUnit.DAYS.between(date, today)
            if (days > 0) date = date.plusDays(days)
        }
        RepeatRule.WEEKLY -> {
            val weeks = ChronoUnit.WEEKS.between(date, today)
            if (weeks > 0) date = date.plusWeeks(weeks)
        }
        RepeatRule.MONTHLY -> {
            val months = ChronoUnit.MONTHS.between(date, today)
            if (months > 0) date = date.plusMonths(months)
        }
        RepeatRule.NONE -> return null
    }

    // 第二步：小步校正到严格晚于今天（月末对齐等情况下需要多走一两步）
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
