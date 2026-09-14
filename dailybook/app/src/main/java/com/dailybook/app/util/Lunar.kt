package com.dailybook.app.util

import java.time.LocalDate

/**
 * 农历换算（1900–2100）。
 *
 * 纯 Kotlin 实现，不依赖 `android.icu.util.ChineseCalendar` ——
 * 那个类在 JVM 单测里根本跑不起来（android.jar 里全是抛异常的桩），
 * 而农历这种「算错一天用户就会在某天过错生日」的逻辑必须能被测试钉住。
 * 实现上只用 `java.time.LocalDate`，无任何 android.* 依赖，零新增依赖。
 *
 * ## 数据表与位布局
 * [LUNAR_INFO] 每年一个整数（索引 = 农历年 - 1900，共 201 项），含义（低位到高位）：
 * ```
 *   bit 0..3   闰月月号：0 = 该年无闰月；否则 1~12 表示「闰几月」，
 *              也就是闰月排在该普通月之后（如 6 = 闰六月排在六月后面）
 *   bit 4..15  12 个普通月的大小：1 = 30 天，0 = 29 天。
 *              第 m 月的位是 `info ushr (16 - m) and 1`，
 *              即 正月 = bit 15、二月 = bit 14 …… 十二月 = bit 4。
 *   bit 16     闰月大小：1 = 闰月 30 天，0 = 闰月 29 天（该年无闰月时此位无意义）
 * ```
 * **注意**：bit 16（闰月大小）与 bit 4..15（普通月大小）是两份互相独立的信息，
 * 不能拿 bit 16 去推普通月的长短。另外 bit 4..15 是「一年存 12 位」，与相邻年份没有错位 ——
 * 第 n 位就对应本年度的第 n 个月。这两点都容易写错，写错后表现是「整年日期整体平移」。
 *
 * 校验样例（均可在公开万年历上逐日核对）：
 * ```
 *   1900 项 0x04bd8：闰 8 月；正月 30 天；bit16=0 → 闰八月 29 天；全年 384 天
 *   2024 项 0x0ad50：无闰月；全年 354 天；正月初一 = 2024-02-10
 *   2025 项 0x0a6e6：闰 6 月；全年 384 天；正月初一 = 2025-01-29，
 *                    六月初一 = 06-25、闰六月初一 = 07-25、八月初一 = 09-22、八月十五 = 10-06
 *   2026 项 0x0a4e0：无闰月；全年 354 天；正月初一 = 2026-02-17
 *   2027 项 0x0d260：无闰月；全年 354 天；正月初一 = 2027-02-06、六月初一 = 2027-07-04
 * ```
 * 表中「闰月月号」与 bit16 并不总是一致（例如 1900 / 2020 / 2023 / 2025 的 bit16 都是 0），
 * 这说明两者确实独立存储 —— 只要按上面的规则各取各的，全表 353~385 天的年长都在合法区间内。
 *
 * ## 纪元
 * 1900 年正月初一 = 公历 **1900-01-31**，这是整张表的零点（[EPOCH_EPOCH_DAY]）。
 * 注意是 -25537（1900-01-31），不是 -25567（那是 1900-01-01）—— 两者差 30 天，
 * 写错会让所有日期整整偏一个月。
 *
 * 支持范围之外（早于 1900）按表起点夹逼处理，不抛异常 ——
 * 提醒、控件、统计里到处都在调它，宁可结果不精确也不能崩。
 * 判断「某个农历日期是否真实存在」请用 [toSolar] 的 null 返回，不要靠范围判断。
 */
object Lunar {

    /** 农历日期。`leap = true` 表示闰月（例如闰六月初一）。 */
    data class LunarDate(val year: Int, val month: Int, val day: Int, val leap: Boolean)

    private const val MIN_YEAR = 1900
    private const val MAX_YEAR = 2100

    private const val MONTHS_PER_YEAR = 12
    private const val SHORT_MONTH_DAYS = 29
    private const val LONG_MONTH_DAYS = 30

    /**
     * 1900-01-31（1900 年正月初一）的 epochDay；表里所有天数都相对它算。
     * 注意是 **-25537**（1900-01-31），不是 -25567（那是 1900-01-01）——
     * 这两个值差 30 天，写错会让所有日期整整偏一个月。
     */
    private const val EPOCH_EPOCH_DAY = -25537L

    // ---------------------------------------------------------------
    // 基础查表
    // ---------------------------------------------------------------

    private fun info(year: Int): Int = LUNAR_INFO[year.coerceIn(MIN_YEAR, MAX_YEAR) - MIN_YEAR]

    /** 该农历年的闰月月份，0 表示没有闰月。 */
    fun hasLeapMonth(lunarYear: Int): Int = info(lunarYear) and LEAP_MONTH_MASK

    /** 该农历年第 month 个**普通**月的天数（29 或 30）。 */
    private fun monthDays(year: Int, month: Int): Int =
        if (info(year) and (MONTH_BITS_BASE ushr month) != 0) LONG_MONTH_DAYS else SHORT_MONTH_DAYS

    /** 该农历年闰月的天数；没有闰月时是 0。 */
    private fun leapDays(year: Int): Int =
        if (hasLeapMonth(year) == 0) {
            0
        } else if (info(year) and LEAP_MONTH_DAYS_BIT != 0) {
            LONG_MONTH_DAYS
        } else {
            SHORT_MONTH_DAYS
        }

    /** 该农历年的总天数（353~385）。 */
    private fun yearDays(year: Int): Int {
        var sum = 0
        for (m in 1..MONTHS_PER_YEAR) sum += monthDays(year, m)
        return sum + leapDays(year)
    }

    /**
     * 该农历年的全部月份，按真实的日历顺序排列：[月号, 是否闰月, 天数]。
     * 闰月插在它所重复的普通月之后。
     *
     * [fromSolar] 与 [toSolar] 共用这一个月份序列，因此两者严格互逆 ——
     * 不会出现「正向算得进去、反向算不回来」的偏差。
     */
    private fun monthsOfYear(year: Int): List<IntArray> {
        val leap = hasLeapMonth(year)
        val months = ArrayList<IntArray>(if (leap == 0) MONTHS_PER_YEAR else MONTHS_PER_YEAR + 1)
        for (m in 1..MONTHS_PER_YEAR) {
            months.add(intArrayOf(m, 0, monthDays(year, m)))
            if (leap == m) months.add(intArrayOf(m, 1, leapDays(year)))
        }
        return months
    }

    // ---------------------------------------------------------------
    // 换算
    // ---------------------------------------------------------------

    /**
     * 公历 → 农历。
     *
     * 早于 1900-01-31 的日期夹逼到表起点（返回 1900 年正月初一），不抛异常。
     */
    fun fromSolar(date: LocalDate): LunarDate {
        // 1) 先算「从 1900 年正月初一到现在过了多少天」——整张表的唯一坐标轴
        var offset = date.toEpochDay() - EPOCH_EPOCH_DAY
        if (offset < 0L) offset = 0L

        // 2) 一年一年地减掉整年的天数，直到剩下的 offset 落在某一年之内
        var year = MIN_YEAR
        while (year < MAX_YEAR && offset >= yearDays(year)) {
            offset -= yearDays(year)
            year++
        }

        // 3) 在这个农历年里按真实月份顺序逐月往后走。offset 落在哪个月内，
        //    就是那个月的第 (offset + 1) 天。闰月紧跟在它所重复的普通月之后。
        for (month in monthsOfYear(year)) {
            if (offset < month[2]) {
                return LunarDate(year, month[0], offset.toInt() + 1, month[1] != 0)
            }
            offset -= month[2]
        }

        // 数据表自相矛盾时才会走到这里（正常永远不该发生）：兜底成腊月三十，
        // 免得抛异常把提醒流程打断。
        return LunarDate(year, MONTHS_PER_YEAR, LONG_MONTH_DAYS, false)
    }

    /**
     * 农历 → 公历。
     *
     * 该农历日期在当年**不存在**时返回 null，主要是两种：
     * 1. 「闰月日期遇到没有该闰月的年份」（如闰六月初一在 2024 / 2026 年不存在）；
     * 2. 农历日超出该月天数（某月只有 29 天却写三十），以及月份 / 日子越界（十三月、三十二）。
     * 农历年超出 1900–2100 时同样返回 null —— 这是判断「该农历日期是否存在」的唯一入口。
     */
    fun toSolar(lunar: LunarDate): LocalDate? {
        if (lunar.year !in MIN_YEAR..MAX_YEAR) return null
        if (lunar.month !in 1..MONTHS_PER_YEAR) return null
        if (lunar.day !in 1..LONG_MONTH_DAYS) return null

        // 一个农历年最多只有一个闰月，所以「闰 X 月」只有在该年闰的正好是 X 月时才存在
        if (lunar.leap && hasLeapMonth(lunar.year) != lunar.month) return null

        // 该年正月初一距 1900-01-31 的天数
        var offset = 0L
        for (y in MIN_YEAR until lunar.year) offset += yearDays(y)

        // 走到目标月：普通月按（月号，非闰）匹配，闰月按（月号，闰）匹配；
        // 该月长度决定「三十」是否存在。
        for (month in monthsOfYear(lunar.year)) {
            if (month[0] == lunar.month && (month[1] != 0) == lunar.leap) {
                if (lunar.day > month[2]) return null
                return LocalDate.ofEpochDay(EPOCH_EPOCH_DAY + offset + (lunar.day - 1))
            }
            offset += month[2]
        }

        // monthsOfYear 一定含有 1..12 的全部普通月，所以只有 lunar.leap 为 true 时才可能到这
        return null
    }

    /**
     * 某个农历日期从今天起的下一次出现（「重要日期」的倒计时就靠它）。
     *
     * - 先按今天的农历年算，不够就往后试两年。
     * - **闰月日期遇到没有该闰月的年份，回落到当月同一天** ——
     *   这是用户实际期待的行为：闰六月生日在平年也要过，而不是空等十几年。
     * - 返回「今天或今天之后」最近的一次（当天算当天），候选日期早于今天就继续往后找。
     * - 找不到时返回 null（只有农历年超过 2100 才会发生）。
     */
    fun solarOfNextOccurrence(lunar: LunarDate, today: LocalDate): LocalDate? {
        val startYear = fromSolar(today).year
        for (candidateYear in startYear..(startYear + 2)) {
            if (candidateYear > MAX_YEAR) break
            // 该年确实有这个闰月就按闰月过，否则退回同名普通月
            val useLeap = lunar.leap && hasLeapMonth(candidateYear) == lunar.month
            val candidate = toSolar(LunarDate(candidateYear, lunar.month, lunar.day, useLeap)) ?: continue
            if (!candidate.isBefore(today)) return candidate
        }
        return null
    }

    // ---------------------------------------------------------------
    // 名字（中文数字属于数据，不跟着界面语言翻译）
    // ---------------------------------------------------------------

    private val DIGITS = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")

    /** 月份名：正月 / 二月 / … / 十月 / 冬月 / 腊月；闰月前面加「闰」。 */
    fun lunarMonthName(month: Int, leap: Boolean): String {
        val safe = month.coerceIn(1, MONTHS_PER_YEAR)
        val name = when (safe) {
            1 -> "正月"
            11 -> "冬月"
            12 -> "腊月"
            else -> DIGITS[safe - 1] + "月"
        }
        return if (leap) "闰$name" else name
    }

    /** 日子名：初一…初十 / 十一…十九 / 二十 / 廿一…廿九 / 三十。 */
    fun lunarDayName(day: Int): String {
        val d = day.coerceIn(1, LONG_MONTH_DAYS)
        return when {
            d <= 10 -> "初" + DIGITS[d - 1]
            d < 20 -> "十" + DIGITS[d - 11]
            d == 20 -> "二十"
            d < 30 -> "廿" + DIGITS[d - 21]
            else -> "三十"
        }
    }

    // ---------------------------------------------------------------
    // 数据表（1900–2100，见文件头的位布局说明）
    // ---------------------------------------------------------------

    /** bit 0..3：闰月月号（0 = 无闰月）。 */
    private const val LEAP_MONTH_MASK = 0xF

    /** bit 16：闰月大小（1 = 30 天）。 */
    private const val LEAP_MONTH_DAYS_BIT = 0x10000

    /** 普通月大小位的基准：`MONTH_BITS_BASE ushr m` 取第 m 月（m = 1..12）的位。 */
    private const val MONTH_BITS_BASE = 0x10000

    private val LUNAR_INFO = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2, // 1900-1909
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977, // 1910-1919
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970, // 1920-1929
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950, // 1930-1939
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557, // 1940-1949
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0, // 1950-1959
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0, // 1960-1969
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6, // 1970-1979
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570, // 1980-1989
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0, // 1990-1999
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5, // 2000-2009
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930, // 2010-2019
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530, // 2020-2029
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45, // 2030-2039
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0, // 2040-2049
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0, // 2050-2059
        0x092e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4, // 2060-2069
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0, // 2070-2079
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160, // 2080-2089
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a4d0, 0x0d150, 0x0f252, // 2090-2099
        0x0d520                                                                                    // 2100
    )
}
