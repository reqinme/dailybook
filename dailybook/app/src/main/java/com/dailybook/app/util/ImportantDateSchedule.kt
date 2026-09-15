package com.dailybook.app.util

import com.dailybook.app.data.DateRepeat
import com.dailybook.app.data.ImportantDateEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 重要日期的**纯时间计算**：不碰 AlarmManager、不碰 Context，也不导入任何 `android.*`，
 * 所以能在 JVM 上直接跑单元测试（和 [ClassSchedule] 同一个套路）。
 *
 * 为什么要把「下一次是哪天」搬到这里：界面（`MainViewModel.buildState` 的倒计时列表）
 * 和提醒排程（`notify/ImportantDateReminder`）必须用**同一套**规则算日子，
 * 否则会出现「列表说还剩 3 天、通知却在别的时候响」这种自相矛盾。
 *
 * 排提醒的时刻：下一次日期的**当天上午 09:00**，再往前挪 `remindDaysBefore` 天
 * （0 = 当天）。上午 9 点是和待办提醒（[TodoReminder] 的 HOUR）一致的一个「白天开始时」的
 * 固定本地点，用户不会在半夜被叫醒，也不用为它单独做一个设置项。
 *
 * 「只过一次」（[DateRepeat.ONCE]）的日期算出来的下一次就是原来那天，
 * 哪怕它已经过去了 —— 由调用方决定是显示成「已过去 N 天」还是干脆不排提醒。
 * 这一条对**农历**日期同样成立（见 [nextOccurrence]）：ONCE 的农历日期不会年年复发。
 */
object ImportantDateSchedule {

    /** 提醒在**当地几点**响：上午 9 点（和待办提醒同一个钟点，理由见类注释） */
    const val FIRE_HOUR: Int = 9

    /**
     * 下一次发生（公历日）。返回 null 只有一种情况：农历日期超出了 [Lunar] 的年份表
     * （阳历那套无论什么规则都算得出一天，「只过一次」也照样把原来那天返回）。
     *
     * ⚠️ 农历日期**也要看重复规则**：[DateRepeat.ONCE] 的农历日期只发生一次，
     * 就是当初存下来的那一天（[ImportantDateEntity.dateMillis] 是首次换算出的阳历日），
     * 过了也停在原地 —— 于是它会像阳历的「只过一次」那样进「已经过去」那一组，
     * 而不是年年复发。其余规则（每年 / 每月 / 每周）的农历日期仍按**每年**算
     * （农历没有「每月同一天」的说法，界面上也是这么写的）。
     */
    fun nextOccurrence(item: ImportantDateEntity, today: LocalDate): LocalDate? {
        if (item.lunar) {
            // 「只过一次」：认存下来的那一天，重复规则优先于农历换算。
            // 以前这一步被跳过，标了「只过一次」的农历日期会一直往后滚到明年，
            // 和阳历 ONCE（过期后停在原地、显示「已过去 N 天」）自相矛盾。
            if (item.repeatRule == DateRepeat.ONCE) return item.dateMillis.toLocalDate()
            // 第一个参数的年只是占位：solarOfNextOccurrence 内部按「今天的农历年」往后找
            return Lunar.solarOfNextOccurrence(
                Lunar.LunarDate(today.year, item.lunarMonth, item.lunarDay, item.lunarLeap),
                today
            )
        }
        return nextSolarOccurrence(item.dateMillis.toLocalDate(), item.repeatRule, today)
    }

    /**
     * 阳历日期的下一次发生。
     *
     * [DateRepeat.ONCE] 就认原来那一天（**已经过去也照原样返回**，交由调用方决定怎么展示）；
     * YEARLY / MONTHLY / WEEKLY 一律往后滚到「不早于 [today]」的最近一次：
     * 先按年 / 月 / 周大步快进到今天附近，再用一个小循环校正（月末对齐时可能要再多走一两步）。
     */
    fun nextSolarOccurrence(start: LocalDate, rule: DateRepeat, today: LocalDate): LocalDate {
        if (rule == DateRepeat.ONCE) return start
        val unit = when (rule) {
            DateRepeat.YEARLY -> ChronoUnit.YEARS
            DateRepeat.MONTHLY -> ChronoUnit.MONTHS
            DateRepeat.WEEKLY -> ChronoUnit.WEEKS
            DateRepeat.ONCE -> return start
        }

        // 先按「整 N 个单位」大步快进到今天附近
        var step = unit.between(start, today).coerceAtLeast(0)
        var date = start.plus(step, unit)

        // 再小步校正。关键：**每一步都从 start 重新推**（start + step 个单位），
        // 不要拿上一步算出来的 date 继续加 —— 因为月末会被 Java 的 plusMonths 夹取
        // （1/31 + 1 个月 = 2/28），一旦用夹取过的日期继续推，
        // 「每月 31 日」过了 2 月就永远变成 28 日了。
        var guard = 0
        while (date.isBefore(today) && guard++ < 8) {
            step++
            date = start.plus(step, unit)
        }
        return date
    }

    /**
     * 这一次的提醒时刻（毫秒），null = 这个日期算不出日子（农历超出年份表）。
     *
     * 已经过去的提醒时刻也**照原样返回**（不会自作主张往后滚）：调用方据此判断
     * 「这条已经响过了」，才不会把同一次提醒排第二遍。
     */
    fun fireMillis(item: ImportantDateEntity, nowMillis: Long, zone: ZoneId): Long? =
        fireMillisForOccurrence(item, occurrenceDay(item, nowMillis, zone), zone)

    /** 某一次发生日 [occurrence] 对应的提醒时刻（[fireMillis] 的后半段，单独留出来便于测试） */
    fun fireMillisForOccurrence(
        item: ImportantDateEntity,
        occurrence: LocalDate?,
        zone: ZoneId
    ): Long? {
        val day = reminderDayOf(item, occurrence) ?: return null
        return day.atTime(FIRE_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * 该在哪一天提醒：发生日往前挪 `remindDaysBefore` 天（0 = 当天）。
     *
     * 接收器判断「这次提醒算不算数」时只比**日期**（是不是今天），不比时刻 ——
     * 非精确闹钟可能被系统推迟几个小时，那天补一条提醒是对的，
     * 但拖到第二天再响就纯属打扰（也容易被当成「怎么又提醒一遍」）。
     */
    fun reminderDayOf(item: ImportantDateEntity, occurrence: LocalDate?): LocalDate? {
        if (occurrence == null) return null
        // remindDaysBefore <= 0（含老数据里的负数）都当「当天提醒」，
        // 和界面上的 `remindText` 同一个口径。
        return occurrence.minusDays(item.remindDaysBefore.coerceAtLeast(0).toLong())
    }

    /** [nowMillis] 那一刻所属的本地日期；排程与接收器都用它把时间戳换成「哪一天」 */
    private fun occurrenceDay(
        item: ImportantDateEntity,
        nowMillis: Long,
        zone: ZoneId
    ): LocalDate? = nextOccurrence(item, Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate())
}
