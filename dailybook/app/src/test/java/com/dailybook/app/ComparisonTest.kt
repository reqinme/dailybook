package com.dailybook.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 环比的百分比计算。
 *
 * 这里最容易出错的是「没有可比基数」的情况（上月一笔都没记），
 * 那时候不能显示 0% 或 +100%，必须返回 null 让界面别说瞎话。
 */
class ComparisonTest {

    @Test
    fun percentIsNullWithoutBase() {
        assertNull(Comparison(monthExpense = 50_000L, lastMonthExpense = 0L).monthExpensePercent())
        assertNull(Comparison(monthIncome = 50_000L, lastMonthIncome = 0L).monthIncomePercent())
        assertNull(Comparison(yearExpense = 50_000L, lastYearExpense = 0L).yearExpensePercent())
    }

    @Test
    fun percentIsSignedAndRounded() {
        assertEquals(
            50,
            Comparison(monthExpense = 15_000L, lastMonthExpense = 10_000L).monthExpensePercent()
        )
        assertEquals(
            -25,
            Comparison(monthExpense = 7_500L, lastMonthExpense = 10_000L).monthExpensePercent()
        )
        assertEquals(
            0,
            Comparison(monthExpense = 10_000L, lastMonthExpense = 10_000L).monthExpensePercent()
        )
    }

    @Test
    fun fullDropIsMinusHundred() {
        assertEquals(
            -100,
            Comparison(monthExpense = 0L, lastMonthExpense = 10_000L).monthExpensePercent()
        )
    }

    @Test
    fun roundingGoesToWholePercent() {
        // 10000 → 10333 是 +3.33%，取整后 3
        assertEquals(
            3,
            Comparison(monthExpense = 10_333L, lastMonthExpense = 10_000L).monthExpensePercent()
        )
        // 10000 → 10666 是 +6.66%，取整后 6
        assertEquals(
            6,
            Comparison(monthExpense = 10_666L, lastMonthExpense = 10_000L).monthExpensePercent()
        )
    }

    @Test
    fun eachPairUsesItsOwnNumbers() {
        val comparison = Comparison(
            monthExpense = 20_000L,
            lastMonthExpense = 10_000L,
            monthIncome = 5_000L,
            lastMonthIncome = 10_000L,
            yearExpense = 90_000L,
            lastYearExpense = 60_000L
        )
        assertEquals(100, comparison.monthExpensePercent())
        assertEquals(-50, comparison.monthIncomePercent())
        assertEquals(50, comparison.yearExpensePercent())
    }
}
