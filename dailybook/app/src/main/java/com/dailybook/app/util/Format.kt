package com.dailybook.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private val ZONE: ZoneId = ZoneId.systemDefault()
private val WEEKDAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

fun LocalDate.toDayMillis(): Long = atStartOfDay(ZONE).toInstant().toEpochMilli()

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZONE).toLocalDate()

fun todayMillis(): Long = LocalDate.now().toDayMillis()

/** 分 → "1234.56" */
fun formatAmount(cents: Long): String {
    val negative = cents < 0
    val abs = if (negative) -cents else cents
    val text = "${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    return if (negative) "-$text" else text
}

/** 用户输入（元）→ 分；非法或非正数返回 null */
fun parseAmountToCents(input: String): Long? {
    val text = input.trim().replace(",", "").replace("，", "").replace("¥", "")
    if (text.isEmpty()) return null
    return try {
        val value = BigDecimal(text)
        if (value.signum() <= 0) return null
        val cents = value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        if (cents <= 0) null else cents
    } catch (_: Exception) {
        null
    }
}

/** 日期分组标题：今天 / 昨天 / 9月13日 周六 */
fun formatDateHeader(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        today.minusDays(2) -> "前天"
        else -> "${date.monthValue}月${date.dayOfMonth}日 ${WEEKDAYS[date.dayOfWeek.value - 1]}"
    }
}

fun formatMonthLabel(month: YearMonth): String = "${month.year}年${month.monthValue}月"

fun formatDayShort(date: LocalDate): String = "${date.monthValue}/${date.dayOfMonth}"

/** 到期日文案：今天到期 / 明天到期 / 已逾期 3 天 / 9月20日 */
fun formatDueLabel(dueMillis: Long, today: LocalDate = LocalDate.now()): String {
    val due = dueMillis.toLocalDate()
    val days = due.toEpochDay() - today.toEpochDay()
    return when {
        days < 0L -> "已逾期 ${-days} 天"
        days == 0L -> "今天到期"
        days == 1L -> "明天到期"
        else -> "${due.monthValue}月${due.dayOfMonth}日"
    }
}

fun isOverdue(dueMillis: Long, today: LocalDate = LocalDate.now()): Boolean =
    dueMillis.toLocalDate().toEpochDay() < today.toEpochDay()
