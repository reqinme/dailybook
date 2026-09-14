package com.dailybook.app.util

import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private val ZONE: ZoneId = ZoneId.systemDefault()

fun LocalDate.toDayMillis(): Long = atStartOfDay(ZONE).toInstant().toEpochMilli()

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZONE).toLocalDate()

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

/** 日期分组标题：今天 / 昨天 / 前天 / 9月13日 周六 */
fun formatDateHeader(date: LocalDate, lang: Lang): String {
    val today = LocalDate.now()
    return when (date) {
        today -> AppStrings.today(lang)
        today.minusDays(1) -> AppStrings.yesterday(lang)
        today.minusDays(2) -> AppStrings.dayBeforeYesterday(lang)
        else -> {
            val day = AppStrings.monthDay(lang, date.monthValue, date.dayOfMonth)
            val week = AppStrings.weekday(lang, date.dayOfWeek.value - 1)
            if (lang == Lang.EN) "$day · $week" else "$day $week"
        }
    }
}

fun formatMonthLabel(month: YearMonth, lang: Lang): String =
    AppStrings.yearMonth(lang, month.year, month.monthValue)

/** 到期日文案：今天到期 / 明天到期 / 已逾期 3 天 / 9月20日 */
fun formatDueLabel(dueMillis: Long, lang: Lang, today: LocalDate = LocalDate.now()): String {
    val due = dueMillis.toLocalDate()
    val days = due.toEpochDay() - today.toEpochDay()
    return when {
        days < 0L -> AppStrings.dueOverdue(lang, -days)
        days == 0L -> AppStrings.dueToday(lang)
        days == 1L -> AppStrings.dueTomorrow(lang)
        else -> AppStrings.monthDay(lang, due.monthValue, due.dayOfMonth)
    }
}

fun isOverdue(dueMillis: Long, today: LocalDate = LocalDate.now()): Boolean =
    dueMillis.toLocalDate().toEpochDay() < today.toEpochDay()
