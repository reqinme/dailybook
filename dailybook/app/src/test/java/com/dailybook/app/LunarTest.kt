package com.dailybook.app

import com.dailybook.app.util.Lunar
import com.dailybook.app.util.Lunar.LunarDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * 农历换算。
 *
 * 这里断言的每一个日期都是公开可查的常识日期（春节 = 正月初一、中秋 = 八月十五、
 * 端午 = 五月初五），没有靠算法自己推自己 —— 否则数据表抄错一位也会「测试通过」。
 *
 * 重点是三件事：
 * 1. 春节 / 中秋 / 端午这类固定农历日期必须落在正确的公历日；
 * 2. 闰月（2023 闰二月、2025 闰六月）必须能双向换算，且没有该闰月的年份要返回 null；
 * 3. 农历生日按年重复时，闰月生日在没有该闰月的年份要退回普通月（不能空等十几年）。
 */
class LunarTest {

    // ---------------------------------------------------------------------------------------
    // 春节 = 正月初一
    // ---------------------------------------------------------------------------------------

    @Test
    fun springFestival2026IsTheFirstDayOfTheFirstMonth() {
        val lunar = Lunar.fromSolar(LocalDate.of(2026, 2, 17))
        assertEquals("春节 2026-02-17 应该是丙午年正月初一", LunarDate(2026, 1, 1, false), lunar)
    }

    @Test
    fun springFestival2025IsTheFirstDayOfTheFirstMonth() {
        val lunar = Lunar.fromSolar(LocalDate.of(2025, 1, 29))
        assertEquals("春节 2025-01-29 应该是乙巳年正月初一", LunarDate(2025, 1, 1, false), lunar)
    }

    @Test
    fun springFestival2024IsTheFirstDayOfTheFirstMonth() {
        val lunar = Lunar.fromSolar(LocalDate.of(2024, 2, 10))
        assertEquals("春节 2024-02-10 应该是甲辰年正月初一", LunarDate(2024, 1, 1, false), lunar)
    }

    /** 春节前一天一定是上一年的腊月：用来验证年与年的边界没有差一天。 */
    @Test
    fun dayBeforeSpringFestivalBelongsToThePreviousLunarYear() {
        val lunar = Lunar.fromSolar(LocalDate.of(2026, 2, 16))
        assertEquals("2026 春节前一天是 2025 年腊月", 2025, lunar.year)
        assertEquals("2026 春节前一天是腊月", 12, lunar.month)
        assertEquals("2026 春节前一天是腊月廿九", 29, lunar.day)
    }

    // ---------------------------------------------------------------------------------------
    // 固定节日
    // ---------------------------------------------------------------------------------------

    @Test
    fun midAutumn2026IsTheFifteenthOfTheEighthMonth() {
        val lunar = Lunar.fromSolar(LocalDate.of(2026, 9, 25))
        assertEquals("中秋 2026-09-25 应该是八月十五", LunarDate(2026, 8, 15, false), lunar)
    }

    @Test
    fun midAutumn2025IsTheFifteenthOfTheEighthMonth() {
        val lunar = Lunar.fromSolar(LocalDate.of(2025, 10, 6))
        assertEquals("中秋 2025-10-06 应该是八月十五", LunarDate(2025, 8, 15, false), lunar)
    }

    @Test
    fun dragonBoat2026IsTheFifthDayOfTheFifthMonth() {
        val lunar = Lunar.fromSolar(LocalDate.of(2026, 6, 19))
        assertEquals("端午 2026-06-19 应该是五月初五", LunarDate(2026, 5, 5, false), lunar)
    }

    // ---------------------------------------------------------------------------------------
    // 闰月
    // ---------------------------------------------------------------------------------------

    @Test
    fun leapMonthNumbersMatchThePublishedCalendar() {
        assertEquals("2023 年闰二月", 2, Lunar.hasLeapMonth(2023))
        assertEquals("2025 年闰六月", 6, Lunar.hasLeapMonth(2025))
        assertEquals("2026 年没有闰月", 0, Lunar.hasLeapMonth(2026))
        assertEquals("2024 年没有闰月", 0, Lunar.hasLeapMonth(2024))
    }

    /** 闰六月的第一天：2025-07-25 是当年闰六月初一（闰六月第二天即 7-26）。 */
    @Test
    fun firstDayOfLeapSixthMonth2025() {
        val lunar = Lunar.fromSolar(LocalDate.of(2025, 7, 25))
        assertEquals("2025-07-25 应该是闰六月初一", LunarDate(2025, 6, 1, true), lunar)

        // 反过来也要成立，并且普通六月初一必须还在闰月之前
        assertEquals("闰六月初一应该是 2025-07-25", LocalDate.of(2025, 7, 25), Lunar.toSolar(lunar))
        assertEquals(
            "正常六月初一是 2025-06-25，早于闰六月",
            LocalDate.of(2025, 6, 25),
            Lunar.toSolar(LunarDate(2025, 6, 1, false))
        )
    }

    /** 闰二月：2023-03-22 是当年闰二月初一（次日 3-23 即闰二月初二）。 */
    @Test
    fun firstDayOfLeapSecondMonth2023() {
        val lunar = Lunar.fromSolar(LocalDate.of(2023, 3, 22))
        assertEquals("2023-03-22 应该是闰二月初一", LunarDate(2023, 2, 1, true), lunar)
        assertEquals("闰二月初一应该是 2023-03-22", LocalDate.of(2023, 3, 22), Lunar.toSolar(lunar))

        // 闰月插在二月之后，普通二月还要在原处
        assertEquals(
            "正常二月初一是 2023-02-20",
            LocalDate.of(2023, 2, 20),
            Lunar.toSolar(LunarDate(2023, 2, 1, false))
        )
    }

    /** 闰月月份不同、位置靠后时，普通月的顺延不能算错（2020 年闰四月）。 */
    @Test
    fun leapFourthMonth2020KeepsLaterMonthsAligned() {
        assertEquals("2020 年闰四月", 4, Lunar.hasLeapMonth(2020))
        assertEquals(
            "2020 闰四月初一是 2020-05-23",
            LocalDate.of(2020, 5, 23),
            Lunar.toSolar(LunarDate(2020, 4, 1, true))
        )
        // 闰月之后的普通月要顺延：2020 年闰四月之后的「五月」初一是公历 6-21
        assertEquals(
            "2020 五月初一应该是 2020-06-21（闰四月插在四月之后）",
            LocalDate.of(2020, 6, 21),
            Lunar.toSolar(LunarDate(2020, 5, 1, false))
        )
    }

    // ---------------------------------------------------------------------------------------
    // 往返一致性
    // ---------------------------------------------------------------------------------------

    @Test
    fun roundTripHoldsAcrossTheSupportedRange() {
        // 覆盖 1900 / 1950 / 2000 / 2026 / 2100 五个跨度
        val samples = listOf(
            LocalDate.of(1900, 1, 31), // 表的起点：1900 年正月初一
            LocalDate.of(1900, 12, 31),
            LocalDate.of(1950, 6, 15),
            LocalDate.of(2000, 2, 5),  // 2000 年春节
            LocalDate.of(2026, 8, 6),
            LocalDate.of(2026, 12, 31),
            LocalDate.of(2099, 12, 31),
            LocalDate.of(2100, 1, 1)
        )
        for (d in samples) {
            val lunar = Lunar.fromSolar(d)
            assertEquals("$d 往返换算必须回到同一天（农历 $lunar）", d, Lunar.toSolar(lunar))
        }
    }

    @Test
    fun everyDayOf2025RoundTrips() {
        // 一整年（含闰六月）逐日往返：能一次性抓住数据表里任何一位月长抄错
        var d = LocalDate.of(2025, 1, 1)
        val end = LocalDate.of(2025, 12, 31)
        while (!d.isAfter(end)) {
            val lunar = Lunar.fromSolar(d)
            assertEquals("$d 往返换算必须回到同一天（农历 $lunar）", d, Lunar.toSolar(lunar))
            d = d.plusDays(1)
        }
    }

    // ---------------------------------------------------------------------------------------
    // 不存在的闰月日期
    // ---------------------------------------------------------------------------------------

    @Test
    fun leapDateInAYearWithoutThatLeapMonthIsNull() {
        assertEquals("2026 年没有闰月", 0, Lunar.hasLeapMonth(2026))
        assertNull("2026 年不存在闰六月初一", Lunar.toSolar(LunarDate(2026, 6, 1, true)))
        assertNull("2026 年不存在闰二月初一", Lunar.toSolar(LunarDate(2026, 2, 1, true)))

        assertEquals("2024 年没有闰月", 0, Lunar.hasLeapMonth(2024))
        assertNull("2024 年不存在闰六月初一", Lunar.toSolar(LunarDate(2024, 6, 1, true)))

        // 2023 年有闰月，但闰的是二月，所以闰六月同样不存在
        assertEquals("2023 年闰的是二月", 2, Lunar.hasLeapMonth(2023))
        assertNull("2023 年不存在闰六月初一", Lunar.toSolar(LunarDate(2023, 6, 1, true)))
    }

    @Test
    fun outOfRangeAndInvalidInputsReturnNull() {
        assertNull("1899 年不在表内", Lunar.toSolar(LunarDate(1899, 1, 1, false)))
        assertNull("2101 年不在表内", Lunar.toSolar(LunarDate(2101, 1, 1, false)))
        assertNull("没有十三月", Lunar.toSolar(LunarDate(2026, 13, 1, false)))
        assertNull("没有初一之前", Lunar.toSolar(LunarDate(2026, 1, 0, false)))
        assertNull("农历月最多三十天", Lunar.toSolar(LunarDate(2026, 1, 31, false)))
    }

    /** 表起点必须精确：1900-01-31 就是 1900 年正月初一。 */
    @Test
    fun tableEpochIsLunarNewYear1900() {
        assertEquals(LunarDate(1900, 1, 1, false), Lunar.fromSolar(LocalDate.of(1900, 1, 31)))
        assertEquals(
            "1900 年正月初一应该是 1900-01-31",
            LocalDate.of(1900, 1, 31),
            Lunar.toSolar(LunarDate(1900, 1, 1, false))
        )
        // 表之前的日期不崩（夹逼到第一天），只是结果没有意义
        assertEquals(
            "表范围外的日期夹逼到表起点，不抛异常",
            LunarDate(1900, 1, 1, false),
            Lunar.fromSolar(LocalDate.of(1850, 1, 1))
        )
    }

    // ---------------------------------------------------------------------------------------
    // 农历生日的年度重复
    // ---------------------------------------------------------------------------------------

    @Test
    fun birthdayAlreadyPassedThisYearGoesToNextLunarYear() {
        // 八月十五 2026 年是 9-25；今天 10-01 已经过了 → 下一次是 2027 年的八月十五（9-15）
        val next = Lunar.solarOfNextOccurrence(LunarDate(2026, 8, 15, false), LocalDate.of(2026, 10, 1))
        assertEquals("2026-10-01 之后的下一次八月十五是 2027-09-15", LocalDate.of(2027, 9, 15), next)
    }

    @Test
    fun birthdayStillAheadReturnsThisYearDate() {
        val next = Lunar.solarOfNextOccurrence(LunarDate(2026, 8, 15, false), LocalDate.of(2026, 9, 1))
        assertEquals("2026-09-01 之后的下一次八月十五是 2026-09-25", LocalDate.of(2026, 9, 25), next)
    }

    @Test
    fun birthdayTodayReturnsToday() {
        // 当天就算「下一次」（>= 今天），否则提醒会在生日当天跳过
        val today = LocalDate.of(2026, 9, 25)
        assertEquals(
            "生日当天应该返回当天",
            today,
            Lunar.solarOfNextOccurrence(LunarDate(2026, 8, 15, false), today)
        )
    }

    @Test
    fun leapMonthBirthdayFallsBackToTheOrdinaryMonth() {
        // 闰六月初一：2025 年真实存在（7-25），2026 年没有闰六月 → 退回普通六月初一 2026-07-14
        val birthday = LunarDate(2026, 6, 1, true)

        val inLeapYear = Lunar.solarOfNextOccurrence(birthday, LocalDate.of(2025, 1, 1))
        assertEquals("2025 年有闰六月，应该落在闰六月初一 2025-07-25", LocalDate.of(2025, 7, 25), inLeapYear)

        val withoutLeapMonth = Lunar.solarOfNextOccurrence(birthday, LocalDate.of(2026, 1, 1))
        assertEquals(
            "2026 年没有闰六月 → 退回普通六月初一 2026-07-14",
            LocalDate.of(2026, 7, 14),
            withoutLeapMonth
        )

        // 2026 年的六月初一已经过去（7-14 < 7-15），下一次是 2027 年的六月初一。
        // 2027-07-04 这个值是这样算出来的（不经过被测代码）：
        //   2027 年正月初一 = 2027-02-06（公开日期），前五个月的大小直接查表是 30+30+29+30+29 = 148 天，
        //   2027-02-06 + 148 天 = 2027-07-04。反向换算（07-04 → 六月初一）也一致。
        val afterThatYear = Lunar.solarOfNextOccurrence(birthday, LocalDate.of(2026, 7, 15))
        assertEquals(
            "闰六月生日在 2026-07-15 之后顺延到 2027 年的六月初一 2027-07-04",
            LocalDate.of(2027, 7, 4),
            afterThatYear
        )
    }

    @Test
    fun ordinaryBirthdayAlwaysExistsRegardlessOfLeapMonths() {
        // 正月初一（春节）每年都有，永远不该返回 null
        var today = LocalDate.of(2024, 1, 1)
        repeat(24) {
            val next = Lunar.solarOfNextOccurrence(LunarDate(2024, 1, 1, false), today)
            assertNotNull("$today 之后一定还有春节", next)
            assertEquals("春节必须落在正月初一", LunarDate(Lunar.fromSolar(next!!).year, 1, 1, false), Lunar.fromSolar(next))
            today = today.plusMonths(1)
        }
    }

    // ---------------------------------------------------------------------------------------
    // 名称
    // ---------------------------------------------------------------------------------------

    @Test
    fun monthNamesUseTheConventionalForms() {
        assertEquals("正月", Lunar.lunarMonthName(1, false))
        assertEquals("二月", Lunar.lunarMonthName(2, false))
        assertEquals("六月", Lunar.lunarMonthName(6, false))
        assertEquals("十月", Lunar.lunarMonthName(10, false))
        assertEquals("冬月", Lunar.lunarMonthName(11, false))
        assertEquals("腊月", Lunar.lunarMonthName(12, false))
        assertEquals("闰六月", Lunar.lunarMonthName(6, true))
        assertEquals("闰二月", Lunar.lunarMonthName(2, true))
    }

    @Test
    fun dayNamesUseTheConventionalForms() {
        assertEquals("初一", Lunar.lunarDayName(1))
        assertEquals("初五", Lunar.lunarDayName(5))
        assertEquals("初十", Lunar.lunarDayName(10))
        assertEquals("十五", Lunar.lunarDayName(15))
        assertEquals("二十", Lunar.lunarDayName(20))
        assertEquals("廿一", Lunar.lunarDayName(21))
        assertEquals("廿九", Lunar.lunarDayName(29))
        assertEquals("三十", Lunar.lunarDayName(30))
    }

    @Test
    fun knownHolidaysRenderWithTheExpectedNames() {
        // 中秋：八月十五
        val midAutumn = Lunar.fromSolar(LocalDate.of(2026, 9, 25))
        assertEquals(
            "中秋应该显示为八月十五",
            "八月十五",
            Lunar.lunarMonthName(midAutumn.month, midAutumn.leap) + Lunar.lunarDayName(midAutumn.day)
        )
        // 春节：正月初一
        val springFestival = Lunar.fromSolar(LocalDate.of(2026, 2, 17))
        assertEquals(
            "春节应该显示为正月初一",
            "正月初一",
            Lunar.lunarMonthName(springFestival.month, springFestival.leap) + Lunar.lunarDayName(springFestival.day)
        )
        // 闰六月初一
        val leap = Lunar.fromSolar(LocalDate.of(2025, 7, 25))
        assertEquals(
            "2025-07-25 应该显示为闰六月初一",
            "闰六月初一",
            Lunar.lunarMonthName(leap.month, leap.leap) + Lunar.lunarDayName(leap.day)
        )
    }
}
