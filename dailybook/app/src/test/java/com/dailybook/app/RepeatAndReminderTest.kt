package com.dailybook.app

import com.dailybook.app.backup.Backup
import com.dailybook.app.data.RepeatRule
import com.dailybook.app.data.TransactionEntity
import com.dailybook.app.data.TxType
import com.dailybook.app.data.nextDueMillisOf
import com.dailybook.app.i18n.Lang
import com.dailybook.app.notify.LedgerReminder
import com.dailybook.app.notify.TodoReminder
import com.dailybook.app.util.toDayMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 重复任务的日期推算。
 *
 * 这段逻辑决定「勾完一条每天的任务，下一条排到哪天」，最容易出错的两种情形是
 * 「很久没打开 App」和「月末（1 月 31 日的下一个月）」。
 */
class RepeatRuleTest {

    private fun day(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).toDayMillis()

    @Test
    fun noneDoesNotRepeat() {
        assertNull(nextDueMillisOf(day(2026, 9, 14), RepeatRule.NONE))
    }

    @Test
    fun dailyGoesToTomorrow() {
        val today = LocalDate.of(2026, 9, 14)
        assertEquals(day(2026, 9, 15), nextDueMillisOf(day(2026, 9, 14), RepeatRule.DAILY, today))
    }

    @Test
    fun dailyCatchesUpToTheFuture() {
        // 每天的任务放了 10 天没打开：只顺延出「明天」那一条，不补一串过期任务
        val today = LocalDate.of(2026, 9, 14)
        assertEquals(day(2026, 9, 15), nextDueMillisOf(day(2026, 9, 4), RepeatRule.DAILY, today))
    }

    @Test
    fun weeklySkipsToNextWeek() {
        val today = LocalDate.of(2026, 9, 14)
        assertEquals(day(2026, 9, 21), nextDueMillisOf(day(2026, 9, 14), RepeatRule.WEEKLY, today))
    }

    @Test
    fun monthlyClampsToShorterMonth() {
        // 1 月 31 日的「每月」，下一个月只有 28 天
        val today = LocalDate.of(2026, 1, 31)
        assertEquals(day(2026, 2, 28), nextDueMillisOf(day(2026, 1, 31), RepeatRule.MONTHLY, today))
    }

    @Test
    fun overdueWeeklyStillLandsInTheFuture() {
        // 8 月 3 日的「每周」任务，到 9 月 14 日才勾掉 → 下一条排到 9 月 21 日
        val today = LocalDate.of(2026, 9, 14)
        assertEquals(day(2026, 9, 21), nextDueMillisOf(day(2026, 8, 3), RepeatRule.WEEKLY, today))
    }
}

/** 待办提醒的排程：同一個到期日最多打扰两次，且不会无限重排 */
class ReminderScheduleTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun atNine(date: LocalDate): Long =
        date.atTime(TodoReminder.hourOfDay(), 0).atZone(zone).toInstant().toEpochMilli()

    private fun day(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).toDayMillis()

    @Test
    fun futureDueIsScheduledAtNine() {
        val today = LocalDate.of(2026, 9, 14)
        val due = day(2026, 9, 20)
        assertEquals(
            atNine(LocalDate.of(2026, 9, 20)),
            TodoReminder.nextTrigger(1L, due, today, System.currentTimeMillis(), emptySet())
        )
    }

    @Test
    fun dueTodayAfterNineRemindsSoon() {
        val today = LocalDate.of(2026, 9, 14)
        val now = atNine(today) + 3_600_000L
        assertEquals(
            now + TodoReminder.soonDelayMillis(),
            TodoReminder.nextTrigger(1L, day(2026, 9, 14), today, now, emptySet())
        )
    }

    @Test
    fun overdueRemindsSoonOnce() {
        val today = LocalDate.of(2026, 9, 14)
        val now = System.currentTimeMillis()
        val due = day(2026, 9, 10)
        assertEquals(
            now + TodoReminder.soonDelayMillis(),
            TodoReminder.nextTrigger(1L, due, today, now, emptySet())
        )
    }

    @Test
    fun secondReminderIsNextMorning() {
        val today = LocalDate.of(2026, 9, 14)
        val due = day(2026, 9, 10)
        val reminded = setOf(TodoReminder.keyFor(1L, due))
        assertEquals(
            atNine(LocalDate.of(2026, 9, 15)),
            TodoReminder.nextTrigger(1L, due, today, System.currentTimeMillis(), reminded)
        )
    }

    @Test
    fun stopsAfterTwoReminders() {
        val today = LocalDate.of(2026, 9, 14)
        val due = day(2026, 9, 10)
        val reminded = setOf(
            TodoReminder.keyFor(1L, due),
            TodoReminder.overdueKeyFor(1L, due)
        )
        assertNull(TodoReminder.nextTrigger(1L, due, today, System.currentTimeMillis(), reminded))
    }

    @Test
    fun alreadyRemindedFutureTaskIsNotRepeated() {
        val today = LocalDate.of(2026, 9, 14)
        val due = day(2026, 9, 20)
        val reminded = setOf(TodoReminder.keyFor(1L, due))
        assertNull(TodoReminder.nextTrigger(1L, due, today, System.currentTimeMillis(), reminded))
    }
}

/** 导出 CSV：Excel 打开要能正确显示中文、逗号和引号 */
class CsvExportTest {

    private fun tx(
        amountCents: Long,
        category: String,
        note: String,
        dateMillis: Long,
        type: TxType = TxType.EXPENSE
    ) = TransactionEntity(
        amountCents = amountCents,
        typeName = type.name,
        category = category,
        note = note,
        dateMillis = dateMillis,
        createdAt = dateMillis
    )

    @Test
    fun writesBomHeaderAndRows() {
        val day = LocalDate.of(2026, 9, 14).toDayMillis()
        val csv = Backup.toCsv(
            listOf(
                tx(1234, "餐饮", "午饭", day),
                tx(50000, "工资", "", day, TxType.INCOME)
            )
        )
        val lines = csv.trim().split("\r\n")
        assertTrue("应以 UTF-8 BOM 开头，Excel 才不会乱码", csv.startsWith("\uFEFF"))
        assertEquals(
            "日期,类型,分类,账户,金额,备注,标签,币种,原币金额,汇率,报销",
            lines[0].removePrefix("\uFEFF")
        )
        // 后 4 列：没有币种 / 外币金额 / 报销标记的本位币记录 → CNY、原币金额 = 金额本身、汇率 1:1、报销留空
        assertEquals("2026-09-14,支出,餐饮,现金,12.34,午饭,,CNY,12.34,1,", lines[1])
        assertEquals("2026-09-14,收入,工资,现金,500.00,,,CNY,500.00,1,", lines[2])
    }

    @Test
    fun quotesCellsWithCommaOrQuote() {
        val day = LocalDate.of(2026, 9, 14).toDayMillis()
        val csv = Backup.toCsv(listOf(tx(100, "其他", "买书, 附\"签名\"版", day)))
        val row = csv.trim().split("\r\n")[1]
        // 带逗号和引号的备注整体加引号、内部引号翻倍；后面的币种 / 原币金额 / 汇率 / 报销照旧跟在后面
        assertTrue(row.endsWith("\"买书, 附\"\"签名\"\"版\",,CNY,1.00,1,"))
    }

    @Test
    fun sortsRowsByDate() {
        val first = LocalDate.of(2026, 9, 1).toDayMillis()
        val second = LocalDate.of(2026, 9, 20).toDayMillis()
        val csv = Backup.toCsv(listOf(tx(100, "餐饮", "晚", second), tx(200, "餐饮", "早", first)))
        val lines = csv.trim().split("\r\n")
        assertEquals("2026-09-01,支出,餐饮,现金,2.00,早,,CNY,2.00,1,", lines[1])
        assertEquals("2026-09-20,支出,餐饮,现金,1.00,晚,,CNY,1.00,1,", lines[2])
    }

    @Test
    fun csvRoundTripsThroughImport() {
        val day = LocalDate.of(2026, 9, 14).toDayMillis()
        val original = listOf(
            tx(1234, "餐饮", "午饭", day),
            tx(50000, "工资", "", day, TxType.INCOME)
        ).mapIndexed { index, item ->
            if (index == 0) item.copy(tags = "旅行,报销") else item
        }
        val parsed = Backup.parseCsv(Backup.toCsv(original), Lang.ZH_CN)

        assertEquals(2, parsed.size)
        assertEquals(1234L, parsed[0].amountCents)
        assertEquals(TxType.EXPENSE, parsed[0].type)
        assertEquals("餐饮", parsed[0].category)
        assertEquals("现金", parsed[0].account)
        assertEquals("旅行,报销", parsed[0].tags)
        assertEquals("午饭", parsed[0].note)
        assertEquals(TxType.INCOME, parsed[1].type)
        assertEquals(50_000L, parsed[1].amountCents)
    }

    @Test
    fun importsLegacyFiveColumnCsvAndSkipsBadRows() {
        // v1.3 导出的旧格式（没有账户 / 标签列），外加一行脏数据
        val csv = "\uFEFF日期,类型,分类,金额,备注\r\n" +
            "2026-09-14,支出,餐饮,12.34,午饭\r\n" +
            "不是日期,支出,餐饮,abc,坏行\r\n" +
            "\r\n"
        val parsed = Backup.parseCsv(csv, Lang.ZH_CN)
        assertEquals(1, parsed.size)
        assertEquals("现金", parsed[0].account)
        assertEquals(1234L, parsed[0].amountCents)
        assertEquals("", parsed[0].tags)
    }

    @Test
    fun importsEnglishHeadersAndTypeLabels() {
        val csv = "Date,Type,Category,Account,Amount,Note,Tags\r\n" +
            "2026-09-14,Income,工资,银行卡,500.00,,奖金\r\n"
        val parsed = Backup.parseCsv(csv, Lang.EN)
        assertEquals(1, parsed.size)
        assertEquals(TxType.INCOME, parsed[0].type)
        assertEquals("银行卡", parsed[0].account)
        assertEquals("奖金", parsed[0].tags)
    }
}

/** 每晚记账提醒的时间推算：过了点就排明天，别原地重复触发 */
class LedgerReminderTimeTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun sameDayWhenTimeIsStillAhead() {
        val now = LocalDateTime.of(2026, 9, 14, 8, 0)
        assertEquals(at(2026, 9, 14, 21, 0), LedgerReminder.nextTriggerMillis(21, 0, now))
    }

    @Test
    fun nextDayWhenTimeAlreadyPassed() {
        val now = LocalDateTime.of(2026, 9, 14, 22, 30)
        assertEquals(at(2026, 9, 15, 21, 0), LedgerReminder.nextTriggerMillis(21, 0, now))
    }

    @Test
    fun exactlyAtTheTimeRollsToTomorrow() {
        val now = LocalDateTime.of(2026, 9, 14, 21, 0)
        assertEquals(at(2026, 9, 15, 21, 0), LedgerReminder.nextTriggerMillis(21, 0, now))
    }

    @Test
    fun customTimeIsRespected() {
        val now = LocalDateTime.of(2026, 9, 14, 7, 0)
        assertEquals(at(2026, 9, 14, 7, 30), LedgerReminder.nextTriggerMillis(7, 30, now))
    }
}
