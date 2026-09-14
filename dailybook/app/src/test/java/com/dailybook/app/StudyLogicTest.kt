package com.dailybook.app

import com.dailybook.app.data.ScoreKind
import com.dailybook.app.ui.study.scoreToPoint
import com.dailybook.app.ui.study.weekNumberFor
import com.dailybook.app.ui.study.weeksMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * 学习模块里两个「纯逻辑但直接决定用户看到什么」的函数。
 *
 * 为什么必须钉住它们：
 * - `weeksMatch` 决定某节课在这一周到底上不上。写错一位用户就会看到课表少课/多课，
 *   而且这种错很难被发现（课表看起来「就是这样」）。
 * - `scoreToPoint` 决定绩点。换算错一位用户算出来的 GPA 就是错的，还会拿去算奖学金。
 *
 * 两个函数都是纯的、不依赖 Android，所以能在这里跑真值断言。
 */
class StudyLogicTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun millisOf(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    // ============================================================
    // 周次表达式
    // ============================================================

    @Test
    fun emptyExpressionMeansEveryWeek() {
        assertTrue("空表达式应该每周都上", weeksMatch("", 1))
        assertTrue(weeksMatch("", 17))
        assertTrue("只有空格也算空", weeksMatch("   ", 3))
    }

    @Test
    fun zeroOrNegativeWeekAlwaysMatches() {
        // 周次算不出来时（比如没填学期起始日）不能把课藏起来
        assertTrue("周次 0 应该照常显示", weeksMatch("1-16", 0))
        assertTrue(weeksMatch("1-16", -5))
    }

    @Test
    fun rangeExpressionMatchesInsideOnly() {
        assertTrue(weeksMatch("1-16", 1))
        assertTrue(weeksMatch("1-16", 9))
        assertTrue(weeksMatch("1-16", 16))
        assertFalse("第 17 周不在 1-16 之内", weeksMatch("1-16", 17))
        assertFalse("第 0 周以外", weeksMatch("1-16", 18))
    }

    @Test
    fun singleWeekExpression() {
        assertTrue(weeksMatch("9", 9))
        assertFalse(weeksMatch("9", 8))
        assertFalse(weeksMatch("9", 10))
    }

    @Test
    fun commaSeparatedPicks() {
        // 1,3,5-9 → 1 / 3 / 5 / 6 / 7 / 8 / 9
        val expression = "1,3,5-9"
        for (week in listOf(1, 3, 5, 6, 7, 8, 9)) {
            assertTrue("第 $week 周应该上（$expression）", weeksMatch(expression, week))
        }
        for (week in listOf(2, 4, 10, 11)) {
            assertFalse("第 $week 周不应该上（$expression）", weeksMatch(expression, week))
        }
    }

    @Test
    fun oddAndEvenSuffix() {
        // 单周
        val odd = "1-16单"
        assertTrue(weeksMatch(odd, 1))
        assertTrue(weeksMatch(odd, 15))
        assertFalse("第 2 周是双周", weeksMatch(odd, 2))
        assertFalse("第 16 周是双周", weeksMatch(odd, 16))

        // 双周
        val even = "2-16双"
        assertTrue(weeksMatch(even, 2))
        assertTrue(weeksMatch(even, 16))
        assertFalse("第 3 周是单周", weeksMatch(even, 3))
    }

    @Test
    fun oddAndEvenPrefix() {
        // 前缀写法也应认（文档里写了这种写法）
        assertTrue("单1-16 的第 3 周", weeksMatch("单1-16", 3))
        assertFalse("单1-16 的第 4 周", weeksMatch("单1-16", 4))
    }

    @Test
    fun chineseAndSpaceSeparators() {
        // 分隔符支持 , ， 空格 、
        for (expression in listOf("1,3,5", "1，3，5", "1 3 5", "1、3、5")) {
            assertTrue("「$expression」应认第 1 周", weeksMatch(expression, 1))
            assertTrue("「$expression」应认第 5 周", weeksMatch(expression, 5))
            assertFalse("「$expression」不该认第 2 周", weeksMatch(expression, 2))
        }
    }

    @Test
    fun unparsableExpressionFallsBackToEveryWeek() {
        // 全看不懂时宁可全部显示，也不要把课藏起来
        assertTrue(weeksMatch("下学期", 1))
        assertTrue(weeksMatch("???", 7))
        // 部分看得懂：看得懂的部分仍然生效
        assertTrue("1-16 认得出，乱七八糟的部分忽略", weeksMatch("1-16 乱七八糟", 3))
        assertFalse(weeksMatch("1-16 乱七八糟", 20))
    }

    // ============================================================
    // 学期周次换算
    // ============================================================

    @Test
    fun weekNumberStartsAtOneOnTheTermWeek() {
        // 学期起始日所在那一周算第 1 周（周一起算）
        val termStart = LocalDate.of(2026, 9, 2) // 周三
        val monday = LocalDate.of(2026, 8, 31)
        assertEquals("起始日当天是第 1 周", 1, weekNumberFor(millisOf(termStart), termStart))
        assertEquals("同一周的周一是第 1 周", 1, weekNumberFor(millisOf(termStart), monday))
        assertEquals("同一周的周日还是第 1 周", 1, weekNumberFor(millisOf(termStart), LocalDate.of(2026, 9, 6)))
    }

    @Test
    fun weekNumberAdvancesEverySevenDays() {
        val termStart = LocalDate.of(2026, 9, 2)
        assertEquals(2, weekNumberFor(millisOf(termStart), termStart.plusWeeks(1)))
        assertEquals(5, weekNumberFor(millisOf(termStart), termStart.plusWeeks(4)))
        assertEquals(17, weekNumberFor(millisOf(termStart), termStart.plusWeeks(16)))
    }

    @Test
    fun weekNumberIsZeroBeforeTermStartsOrWithoutTermStart() {
        val termStart = LocalDate.of(2026, 9, 2)
        assertEquals("学期开始前没有周次", 0, weekNumberFor(millisOf(termStart), LocalDate.of(2026, 8, 1)))
        assertEquals("没填起始日时是 0", 0, weekNumberFor(0L, LocalDate.of(2026, 9, 2)))
    }

    @Test
    fun termStartWeekIsBasedOnMonday() {
        // 起始日与目标日同一周，即使中间跨月也应同周
        val termStart = LocalDate.of(2026, 10, 30) // 周五
        val nextMonday = LocalDate.of(2026, 11, 2)
        assertEquals("跨到下一周的周一是第 2 周", 2, weekNumberFor(millisOf(termStart), nextMonday))
        assertEquals("同周的周日仍是第 1 周", 1, weekNumberFor(millisOf(termStart), LocalDate.of(2026, 11, 1)))
    }

    // ============================================================
    // 分数 → 绩点
    // ============================================================

    @Test
    fun percentBandsMapToTheDocumentedPoints() {
        val expected = listOf(
            100 to 4.0, 90 to 4.0,
            89 to 3.7, 85 to 3.7,
            84 to 3.3, 82 to 3.3,
            81 to 3.0, 78 to 3.0,
            77 to 2.7, 75 to 2.7,
            74 to 2.3, 72 to 2.3,
            71 to 2.0, 68 to 2.0,
            67 to 1.5, 64 to 1.5,
            63 to 1.0, 60 to 1.0,
            59 to 0.0, 0 to 0.0
        )
        for ((score, point) in expected) {
            assertEquals("百分制 $score 应该换算成 $point", point, scoreToPoint("$score", ScoreKind.PERCENT, 4.0), 0.001)
        }
    }

    @Test
    fun fiveLevelGradeNamesMapToPoints() {
        assertEquals(4.0, scoreToPoint("优秀", ScoreKind.GRADE, 4.0), 0.001)
        assertEquals(3.0, scoreToPoint("良好", ScoreKind.GRADE, 4.0), 0.001)
        assertEquals(2.0, scoreToPoint("中等", ScoreKind.GRADE, 4.0), 0.001)
        assertEquals(1.0, scoreToPoint("及格", ScoreKind.GRADE, 4.0), 0.001)
        assertEquals(0.0, scoreToPoint("不及格", ScoreKind.GRADE, 4.0), 0.001)
    }

    @Test
    fun letterGradesAreAccepted() {
        assertEquals(4.0, scoreToPoint("A", ScoreKind.GRADE, 4.0), 0.001)
        assertEquals(3.0, scoreToPoint("B", ScoreKind.GRADE, 4.0), 0.001)
        assertEquals(2.0, scoreToPoint("C", ScoreKind.GRADE, 4.0), 0.001)
        assertEquals(1.0, scoreToPoint("D", ScoreKind.GRADE, 4.0), 0.001)
        assertEquals(0.0, scoreToPoint("F", ScoreKind.GRADE, 4.0), 0.001)
    }

    @Test
    fun directPointInputIsUsedAsIsAndClamped() {
        assertEquals(3.5, scoreToPoint("3.5", ScoreKind.POINT, 4.0), 0.001)
        assertEquals(0.0, scoreToPoint("0", ScoreKind.POINT, 4.0), 0.001)
        assertEquals("超过 5 的绩点夹到 5", 5.0, scoreToPoint("9", ScoreKind.POINT, 4.0), 0.001)
        assertEquals("负数夹到 0", 0.0, scoreToPoint("-2", ScoreKind.POINT, 4.0), 0.001)
    }

    @Test
    fun fivePointScaleRescalesTheFourPointResult() {
        // 90 → 4.0 → ×5/4 = 5.0
        assertEquals(5.0, scoreToPoint("90", ScoreKind.PERCENT, 5.0), 0.001)
        // 75 → 2.7 → ×1.25 = 3.375
        assertEquals(3.375, scoreToPoint("75", ScoreKind.PERCENT, 5.0), 0.001)
        // 60 → 1.0 → ×1.25 = 1.25
        assertEquals(1.25, scoreToPoint("60", ScoreKind.PERCENT, 5.0), 0.001)
        // 五级制也应按口径放大
        assertEquals(5.0, scoreToPoint("优秀", ScoreKind.GRADE, 5.0), 0.001)
    }

    @Test
    fun unrecognisedInputBecomesZeroInsteadOfThrowing() {
        // 用户可能填「缓考」「免修」之类，不能崩
        assertEquals(0.0, scoreToPoint("", ScoreKind.PERCENT, 4.0), 0.001)
        assertEquals(0.0, scoreToPoint("   ", ScoreKind.PERCENT, 4.0), 0.001)
        assertEquals(0.0, scoreToPoint("缓考", ScoreKind.PERCENT, 4.0), 0.001)
        assertEquals(0.0, scoreToPoint("abc", ScoreKind.GRADE, 4.0), 0.001)
        assertEquals(0.0, scoreToPoint("不知道", ScoreKind.POINT, 4.0), 0.001)
    }

    @Test
    fun percentWithDecimalPointIsHandled() {
        // 89.5 应落在 85~89 这一档
        assertEquals(3.7, scoreToPoint("89.5", ScoreKind.PERCENT, 4.0), 0.001)
        assertEquals("90.5 也在最高档", 4.0, scoreToPoint("90.5", ScoreKind.PERCENT, 4.0), 0.001)
    }

    @Test
    fun weekdayHelperSanity() {
        // 保证测试自己没写错：2026-09-02 是周三
        assertEquals(DayOfWeek.WEDNESDAY, LocalDate.of(2026, 9, 2).dayOfWeek)
    }
}
