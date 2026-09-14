package com.dailybook.app

import com.dailybook.app.data.RepeatRule
import com.dailybook.app.data.nextDueMillisOf
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 周期记账的「补记」语义。
 *
 * 关键性质：算出来的下一次日期**一定在今天之后**。
 * 这保证了「打开 App 补记一次 → 把下次往后推」不会立刻又到期，
 * 也就是长期没打开也只会补最近一次，而不是把过去几个月的都刷出来。
 */
class RecurringScheduleTest {

    private val today = LocalDate.of(2026, 9, 14)

    @Test
    fun noneHasNoNextDate() {
        assertNull(nextDueMillisOf(today.toDayMillis(), RepeatRule.NONE, today))
    }

    @Test
    fun monthlyCatchesUpToTheNextFutureMonth() {
        // 三个月前设的每月房租：下次应该是未来的同一天，而不是三个月里落下的三次
        val base = LocalDate.of(2026, 6, 10).toDayMillis()
        val next = nextDueMillisOf(base, RepeatRule.MONTHLY, today)!!.toLocalDate()
        assertTrue("下一次必须在今天之后：$next", next.isAfter(today))
        assertEquals(LocalDate.of(2026, 10, 10), next)
    }

    @Test
    fun weeklyNeverReturnsAPastDate() {
        val base = today.minusDays(10).toDayMillis()
        val next = nextDueMillisOf(base, RepeatRule.WEEKLY, today)!!.toLocalDate()
        assertTrue(next.isAfter(today))
        // 每周一次，所以下一次距今不会超过一周
        assertTrue("下一次不该超过一周：$next", next.isBefore(today.plusDays(8)))
    }

    @Test
    fun dailyGoesToTomorrowWhenAlreadyDueToday() {
        // 今天就是到期日 → 算出来的是明天，所以明天打开 App 不会重复记这一笔
        assertEquals(
            today.plusDays(1),
            nextDueMillisOf(today.toDayMillis(), RepeatRule.DAILY, today)!!.toLocalDate()
        )
    }

    @Test
    fun veryOldBaseStillTerminates() {
        // 十年前的每天任务：不能死循环，也不能返回过去
        val base = today.minusYears(10).toDayMillis()
        val next = nextDueMillisOf(base, RepeatRule.DAILY, today)!!.toLocalDate()
        assertTrue(next.isAfter(today))
    }

    @Test
    fun monthlyKeepsDayOfMonthAcrossMonthLengths() {
        // 1 月 31 日的每月任务：2 月没有 31 号，应该落在月末而不是溢出到 3 月
        val jan31 = LocalDate.of(2026, 1, 31).toDayMillis()
        val next = nextDueMillisOf(jan31, RepeatRule.MONTHLY, LocalDate.of(2026, 1, 31))!!.toLocalDate()
        assertEquals(LocalDate.of(2026, 2, 28), next)
    }
}
