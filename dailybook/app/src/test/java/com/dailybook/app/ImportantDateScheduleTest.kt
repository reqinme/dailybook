package com.dailybook.app

import com.dailybook.app.data.DateRepeat
import com.dailybook.app.data.ImportantDateEntity
import com.dailybook.app.util.ImportantDateSchedule
import com.dailybook.app.util.toDayMillis
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * 重要日期的「下一次是哪天 / 提醒什么时候响」。
 *
 * 为什么必须钉住：
 * - 界面上的倒计时（还剩 N 天）和通知实际响的时刻必须是**同一套规则**算出来的，
 *   否则会出现「列表说还剩 3 天、通知却在别的时候响」这种自相矛盾；
 * - 重复规则里最容易错的是**月末对齐**（每月 31 日遇到 2 月）与「只过一次」过期后怎么办。
 *
 * 全程用固定时区 Asia/Shanghai 和写死的日期，换机器结果一致。
 */
class ImportantDateScheduleTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun item(
        date: LocalDate,
        repeat: DateRepeat = DateRepeat.YEARLY,
        remindDaysBefore: Int = 0,
        lunar: Boolean = false,
        lunarMonth: Int = 1,
        lunarDay: Int = 1,
        lunarLeap: Boolean = false
    ) = ImportantDateEntity(
        title = "测试",
        dateMillis = date.toDayMillis(),
        lunar = lunar,
        lunarMonth = lunarMonth,
        lunarDay = lunarDay,
        lunarLeap = lunarLeap,
        // 注意：实体里存的是枚举的名字（字符串），`repeatRule` 是它的只读换算属性
        repeat = repeat.name,
        remindDaysBefore = remindDaysBefore,
        note = "",
        createdAt = 0L
    )

    // ============================================================
    // 只过一次
    // ============================================================

    @Test
    fun onceAlwaysReturnsItsOwnDay_EvenWhenItHasPassed() {
        val day = LocalDate.of(2026, 5, 20)
        // 已经过去了也照样返回原来那天 —— 由界面决定显示成「已过去 N 天」，
        // 而不是把它从列表里抹掉（这是 v1.10 修过的问题）。
        assertEquals(day, ImportantDateSchedule.nextOccurrence(item(day, DateRepeat.ONCE), LocalDate.of(2026, 5, 25)))
        assertEquals(day, ImportantDateSchedule.nextOccurrence(item(day, DateRepeat.ONCE), day))
        assertEquals(day, ImportantDateSchedule.nextOccurrence(item(day, DateRepeat.ONCE), LocalDate.of(2027, 1, 1)))
    }

    // ============================================================
    // 每年 / 每月 / 每周
    // ============================================================

    @Test
    fun yearlyRollsToThisYearAndStaysOnTheSameMonthAndDay() {
        val born = LocalDate.of(1990, 9, 25)
        assertEquals(LocalDate.of(2026, 9, 25), ImportantDateSchedule.nextOccurrence(item(born), LocalDate.of(2026, 1, 1)))
        // 今天就是那天 → 还是今天（不推到明年）
        assertEquals(LocalDate.of(2026, 9, 25), ImportantDateSchedule.nextOccurrence(item(born), LocalDate.of(2026, 9, 25)))
        // 已经过了 → 推到明年
        assertEquals(LocalDate.of(2027, 9, 25), ImportantDateSchedule.nextOccurrence(item(born), LocalDate.of(2026, 9, 26)))
    }

    @Test
    fun weeklyRollsByWholeWeeks() {
        val start = LocalDate.of(2026, 9, 1) // 周二
        val today = LocalDate.of(2026, 9, 14) // 周一，落后 13 天 = 1 周 + 6 天
        assertEquals(
            "应该落在不早于今天的那个周二",
            LocalDate.of(2026, 9, 15),
            ImportantDateSchedule.nextOccurrence(item(start, DateRepeat.WEEKLY), today)
        )
    }

    @Test
    fun monthlyDoesNotDriftAfterAShortMonth() {
        // 「每月 31 日」：2 月只能落在 28/29 日，但**不能因此把以后都变成 28 日**。
        // 之前的实现用「被夹取过的日期」继续加月份，于是过了 2 月之后永远是 28 日。
        val start = LocalDate.of(2026, 1, 31)
        assertEquals(
            "2 月没有 31 日，夹到 28 日",
            LocalDate.of(2026, 2, 28),
            ImportantDateSchedule.nextOccurrence(item(start, DateRepeat.MONTHLY), LocalDate.of(2026, 2, 1))
        )
        assertEquals(
            "3 月要回到 31 日，不能漂移成 28 日",
            LocalDate.of(2026, 3, 31),
            ImportantDateSchedule.nextOccurrence(item(start, DateRepeat.MONTHLY), LocalDate.of(2026, 3, 1))
        )
        assertEquals(
            "4 月是 30 天，同样不能被夹成 28 日",
            LocalDate.of(2026, 4, 30),
            ImportantDateSchedule.nextOccurrence(item(start, DateRepeat.MONTHLY), LocalDate.of(2026, 4, 1))
        )
        assertEquals(
            "5 月仍然回到 31 日",
            LocalDate.of(2026, 5, 31),
            ImportantDateSchedule.nextOccurrence(item(start, DateRepeat.MONTHLY), LocalDate.of(2026, 5, 2))
        )
    }

    @Test
    fun monthlyOnTheTwentyNinthSurvivesFebruary() {
        val start = LocalDate.of(2025, 1, 29)
        // 2026 不是闰年，2 月没有 29 日
        assertEquals(
            LocalDate.of(2026, 2, 28),
            ImportantDateSchedule.nextOccurrence(item(start, DateRepeat.MONTHLY), LocalDate.of(2026, 2, 1))
        )
        assertEquals(
            "3 月要回到 29 日",
            LocalDate.of(2026, 3, 29),
            ImportantDateSchedule.nextOccurrence(item(start, DateRepeat.MONTHLY), LocalDate.of(2026, 3, 1))
        )
    }

    @Test
    fun leapDayYearlyLandsOnTheTwentyEighthInCommonYears() {
        val start = LocalDate.of(2024, 2, 29)
        assertEquals(
            LocalDate.of(2025, 2, 28),
            ImportantDateSchedule.nextOccurrence(item(start), LocalDate.of(2025, 1, 1))
        )
        assertEquals(
            "闰年仍然回到 29 日",
            LocalDate.of(2028, 2, 29),
            ImportantDateSchedule.nextOccurrence(item(start), LocalDate.of(2028, 1, 1))
        )
    }

    @Test
    fun todayCountsAsTheNextOccurrence() {
        val start = LocalDate.of(2026, 6, 10)
        assertEquals(start, ImportantDateSchedule.nextOccurrence(item(start, DateRepeat.WEEKLY), start))
        assertEquals(start, ImportantDateSchedule.nextOccurrence(item(start, DateRepeat.MONTHLY), start))
    }

    // ============================================================
    // 提醒时刻
    // ============================================================

    @Test
    fun reminderFiresAtNineLocalOnTheDayItself() {
        val day = LocalDate.of(2026, 9, 20)
        val expected = day.atTime(ImportantDateSchedule.FIRE_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(
            expected,
            ImportantDateSchedule.fireMillisForOccurrence(item(day, DateRepeat.ONCE), day, zone)
        )
    }

    @Test
    fun reminderMovesBackByTheConfiguredDays() {
        val day = LocalDate.of(2026, 9, 20)
        val threeDaysEarly = ImportantDateSchedule.fireMillisForOccurrence(
            item(day, DateRepeat.ONCE, remindDaysBefore = 3), day, zone
        )
        val expected = LocalDate.of(2026, 9, 17).atTime(ImportantDateSchedule.FIRE_HOUR, 0)
            .atZone(zone).toInstant().toEpochMilli()
        assertEquals("提前 3 天 = 9 月 17 日上午 9 点", expected, threeDaysEarly)
    }

    @Test
    fun negativeOrZeroReminderDaysMeanTheDayItself() {
        val day = LocalDate.of(2026, 9, 20)
        val onTheDay = day.atTime(ImportantDateSchedule.FIRE_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(onTheDay, ImportantDateSchedule.fireMillisForOccurrence(item(day, DateRepeat.ONCE, remindDaysBefore = 0), day, zone))
        assertEquals(
            "老数据里的负数也按当天算，不能往前翻",
            onTheDay,
            ImportantDateSchedule.fireMillisForOccurrence(item(day, DateRepeat.ONCE, remindDaysBefore = -5), day, zone)
        )
    }

    @Test
    fun reminderDayIsTheDayTheReceiverComparesAgainst() {
        val day = LocalDate.of(2026, 9, 20)
        assertEquals(
            LocalDate.of(2026, 9, 18),
            ImportantDateSchedule.reminderDayOf(item(day, DateRepeat.ONCE, remindDaysBefore = 2), day)
        )
        assertEquals(null, ImportantDateSchedule.reminderDayOf(item(day, DateRepeat.ONCE), null))
    }

    @Test
    fun fireMillisUsesTodaysDateWhenTheOccurrenceHasPassed() {
        // 传入「已经过去」的 now：ONCE 会照原样返回发生日，所以算出来的是那天的 9:00
        // （调用方据此判断「已经响过了」，从而不会排第二遍）
        val day = LocalDate.of(2026, 9, 20)
        val now = LocalDate.of(2026, 10, 1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val fired = ImportantDateSchedule.fireMillis(item(day, DateRepeat.ONCE), now, zone)
        assertEquals(
            day.atTime(ImportantDateSchedule.FIRE_HOUR, 0).atZone(zone).toInstant().toEpochMilli(),
            fired
        )
    }

    // ============================================================
    // 农历（与界面同一套规则）
    // ============================================================

    @Test
    fun lunarDateUsesTheSameNextOccurrenceAsTheList() {
        // 农历八月十五 = 2026-09-25（LunarTest 里也钉过这一天）
        val midAutumn = item(
            LocalDate.now(), DateRepeat.YEARLY, lunar = true, lunarMonth = 8, lunarDay = 15
        )
        assertEquals(
            LocalDate.of(2026, 9, 25),
            ImportantDateSchedule.nextOccurrence(midAutumn, LocalDate.of(2026, 1, 1))
        )
        assertEquals(
            "过了中秋就顺延到次年八月十五",
            LocalDate.of(2027, 9, 15),
            ImportantDateSchedule.nextOccurrence(midAutumn, LocalDate.of(2026, 10, 1))
        )
    }

    @Test
    fun leapMonthDateFallsBackToTheOrdinaryMonth() {
        // 闰六月：2025 年有（7-25），2026 年没有 → 落在普通六月初一 2026-07-14
        val leapSixth = item(
            LocalDate.now(), DateRepeat.YEARLY, lunar = true, lunarMonth = 6, lunarDay = 1, lunarLeap = true
        )
        assertEquals(
            LocalDate.of(2025, 7, 25),
            ImportantDateSchedule.nextOccurrence(leapSixth, LocalDate.of(2025, 1, 1))
        )
        assertEquals(
            LocalDate.of(2026, 7, 14),
            ImportantDateSchedule.nextOccurrence(leapSixth, LocalDate.of(2026, 1, 1))
        )
    }
}
