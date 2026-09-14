package com.dailybook.app

import com.dailybook.app.data.TodoEntity
import com.dailybook.app.data.TodoPriority
import com.dailybook.app.util.toDayMillis
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 看板分组规则的边界。
 *
 * 这里最容易错的是「今天」「第 7 天」和「没日期」这三个点：
 * 今天到期要和逾期归一堆、第 7 天要算「以后」（不能两边都算或都不算），
 * 没日期不能掉进任何一堆消失掉。
 */
class TodoBucketTest {

    private val today = LocalDate.of(2026, 9, 14)

    @Test
    fun doneAlwaysWins() {
        val done = todo(done = true, due = today)
        assertEquals(TodoBucket.DONE, bucketOf(done, today))
        // 已完成的不该因为「逾期」被算进今天到期
        assertEquals(TodoBucket.DONE, bucketOf(todo(done = true, due = today.minusDays(30)), today))
    }

    @Test
    fun overdueAndTodayAreTheSameBucket() {
        assertEquals(TodoBucket.TODAY, bucketOf(todo(due = today), today))
        assertEquals(TodoBucket.TODAY, bucketOf(todo(due = today.minusDays(1)), today))
        assertEquals(TodoBucket.TODAY, bucketOf(todo(due = today.minusYears(1)), today))
    }

    @Test
    fun thisWeekIsTomorrowThroughDaySix() {
        assertEquals(TodoBucket.THIS_WEEK, bucketOf(todo(due = today.plusDays(1)), today))
        assertEquals(TodoBucket.THIS_WEEK, bucketOf(todo(due = today.plusDays(6)), today))
    }

    @Test
    fun daySevenBelongsToLater() {
        // 边界：第 7 天算「以后」，只出现在一堆里
        assertEquals(TodoBucket.LATER, bucketOf(todo(due = today.plusDays(7)), today))
        assertEquals(TodoBucket.LATER, bucketOf(todo(due = today.plusMonths(3)), today))
    }

    @Test
    fun noDueDateHasItsOwnBucket() {
        assertEquals(TodoBucket.NO_DATE, bucketOf(todo(due = null), today))
    }

    @Test
    fun everyTodoLandsInExactlyOneBucket() {
        val samples = listOf(
            todo(due = null),
            todo(due = today.minusDays(5)),
            todo(due = today),
            todo(due = today.plusDays(3)),
            todo(due = today.plusDays(7)),
            todo(due = today.plusDays(400)),
            todo(done = true, due = today.plusDays(3))
        )
        val buckets = samples.map { bucketOf(it, today) }
        // 七个样本各自的归属固定，不会出现「没归进去」的情况
        assertEquals(
            listOf(
                TodoBucket.NO_DATE,
                TodoBucket.TODAY,
                TodoBucket.TODAY,
                TodoBucket.THIS_WEEK,
                TodoBucket.LATER,
                TodoBucket.LATER,
                TodoBucket.DONE
            ),
            buckets
        )
        // 7 个样本刚好覆盖全部 5 个分堆（界面按枚举遍历，每个分堆都有样例走到）
        assertEquals("样本应覆盖全部 5 个分堆", TodoBucket.entries.size, buckets.distinct().size)
    }

    private fun todo(done: Boolean = false, due: LocalDate? = null) = TodoEntity(
        title = "示例",
        done = done,
        dueMillis = due?.toDayMillis(),
        priority = TodoPriority.NORMAL.name,
        createdAt = 1_789_000_000_000L
    )
}
