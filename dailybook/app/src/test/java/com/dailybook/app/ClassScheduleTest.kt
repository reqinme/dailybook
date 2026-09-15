package com.dailybook.app

import com.dailybook.app.data.CourseEntity
import com.dailybook.app.util.ClassSchedule
import com.dailybook.app.util.ClassOccurrence
import com.dailybook.app.util.toDayMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 上课提醒的排程计算（[ClassSchedule.nextOccurrence]）。
 *
 * 全程用**固定时区** Asia/Shanghai 和写死的日期，所以换台机器、换个系统时区结果都一样：
 * 上课提醒最怕的就是「在我机器上对、在用户手机上偏一天」这类问题。
 *
 * 覆盖的关键性质：
 * - 今天的课在提醒时刻之前 → 今天就该响；
 * - 今天的课**已经上完**（下课时刻已经过去）→ 顺延到下一次（通常是下周），而不是当场补一声；
 *   只是**已经开始、还没下课**的那一节仍然会返回（排程会把它提到「现在」立刻响），
 *   判据是「下课时刻还晚于现在」，精确口径见 util/ClassSchedule.kt 的 nextOccurrence；
 * - 提醒时刻越过午夜（周一 00:20 的课，提前 30 分钟 = 周日 23:50）→ 日期得回退一天；
 * - 只在单周开的课，双周必须跳过；
 * - 没填时间的课（-1）一律忽略；
 * - 同一天多节课取最早的一节；
 * - 没有任何可用课程时返回 null。
 */
class ClassScheduleTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    /** 2026-09-14 是周一（用 [mondayIsMonday] 钉住这个前提，免得以后改日期改出幻觉） */
    private val monday: LocalDate = LocalDate.of(2026, 9, 14)

    /** 学期起始日：2026-08-31（周一）；2026-09-14 就是第 3 周 */
    private val termStart: LocalDate = LocalDate.of(2026, 8, 31)

    private fun at(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    /** 同上的 `HH:mm` 写法，测试里写起来更贴近界面上的样子 */
    private fun at(date: LocalDate, time: String): Long {
        val parts = time.split(':')
        return at(date, parts[0].toInt(), parts[1].toInt())
    }

    private fun notifyAt(occurrence: ClassOccurrence): LocalDateTime =
        java.time.Instant.ofEpochMilli(occurrence.notifyAtMillis).atZone(zone).toLocalDateTime()

    private fun startAt(occurrence: ClassOccurrence): LocalDateTime =
        java.time.Instant.ofEpochMilli(occurrence.startMillis).atZone(zone).toLocalDateTime()

    private fun course(
        name: String = "高等数学",
        dayOfWeek: Int = 1,
        startMinutes: Int = 8 * 60,
        endMinutes: Int = 9 * 60 + 40,
        weeks: String = "",
        termStartMillis: Long = termStart.toDayMillis()
    ) = CourseEntity(
        name = name,
        dayOfWeek = dayOfWeek,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        weeks = weeks,
        termStartMillis = termStartMillis,
        createdAt = 0L
    )

    @Test
    fun mondayIsMonday() {
        // 后面所有断言都建在「2026-09-14 是周一」之上
        assertEquals(java.time.DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(3, com.dailybook.app.ui.study.weekNumberFor(termStart.toDayMillis(), monday))
    }

    @Test
    fun todaysClassBeforeNotifyMomentFiresToday() {
        // 周一 07:00：8:00 的课、提前 15 分钟 → 应该在**今天** 07:45 提醒
        val now = at(monday, 7, 0)
        val occurrence = ClassSchedule.nextOccurrence(
            courses = listOf(course()),
            leadMinutes = 15,
            nowMillis = now,
            zone = zone
        )

        assertNotNull("今天的课应该排得出来", occurrence)
        assertEquals("高等数学", occurrence!!.course.name)
        assertEquals(monday.atTime(8, 0), startAt(occurrence))
        assertEquals(monday.atTime(7, 45), notifyAt(occurrence))
        assertTrue("提醒时刻必须晚于 now", occurrence.notifyAtMillis > now)
    }

    @Test
    fun sameDayAfterClassFinishedGoesToNextWeek() {
        // 周一 09:00：这门课 8:00 上、8:30 就**下课了**（课程的 endMinutes 传的是 8*60+30），
        // 现在已经上完 → 今天不再补响，下一次是**下周一**同一时刻。
        // （只是已经开始、还没下课的那一节不在此列：那种仍然会返回，见
        // notifyMomentAlreadyPassedStillKeepsTheOccurrence。）
        val now = at(monday, 9, 0)
        val occurrence = ClassSchedule.nextOccurrence(
            courses = listOf(course(endMinutes = 8 * 60 + 30)),
            leadMinutes = 15,
            nowMillis = now,
            zone = zone
        )

        assertNotNull(occurrence)
        assertEquals(monday.plusWeeks(1).atTime(8, 0), startAt(occurrence!!))
        assertEquals(monday.plusWeeks(1).atTime(7, 45), notifyAt(occurrence))
        assertTrue("下一次一定在未来", occurrence.startMillis > now)
    }

    @Test
    fun notifyMomentCrossingMidnightLandsOnThePreviousDay() {
        // 周一 00:20 的课、提前 30 分钟 → 提醒时刻是**周日 23:50**，日期得往前退一天
        val now = at(monday.minusDays(1), 20, 0) // 周日 20:00
        val occurrence = ClassSchedule.nextOccurrence(
            courses = listOf(course(startMinutes = 20, endMinutes = 95)),
            leadMinutes = 30,
            nowMillis = now,
            zone = zone
        )

        assertNotNull(occurrence)
        assertEquals(monday.atTime(0, 20), startAt(occurrence!!))
        assertEquals(monday.minusDays(1).atTime(23, 50), notifyAt(occurrence))
        assertEquals(
            "提醒时刻落在前一天（周日）",
            java.time.DayOfWeek.SUNDAY,
            notifyAt(occurrence).dayOfWeek
        )
        assertTrue(occurrence.notifyAtMillis > now)
    }

    @Test
    fun oddWeekCourseIsSkippedInEvenWeekAndFoundInTheNextOddWeek() {
        // 只在 1-16 单周上的周一 8:00 课
        val oddOnly = course(weeks = "1-16单", termStartMillis = termStart.toDayMillis())

        // 2026-09-14 是第 3 周（单周）→ 今天就有
        val inOddWeek = ClassSchedule.nextOccurrence(
            courses = listOf(oddOnly),
            leadMinutes = 10,
            nowMillis = at(monday, 7, 0),
            zone = zone
        )
        assertNotNull("单周应该命中", inOddWeek)
        assertEquals(monday.atTime(8, 0), startAt(inOddWeek!!))

        // 2026-10-12 是第 7 周（2026-08-31 起算的第 7 周，仍是单周）……
        // 这里故意用 10-19 那一周（第 8 周，双周）验证「跳过」，再确认下一个单周 10-26（第 9 周）能被找到
        val evenWeekNow = at(LocalDate.of(2026, 10, 19), 7, 0)
        assertEquals(
            8,
            com.dailybook.app.ui.study.weekNumberFor(
                termStart.toDayMillis(),
                LocalDate.of(2026, 10, 19)
            )
        )
        val skipped = ClassSchedule.nextOccurrence(
            courses = listOf(oddOnly),
            leadMinutes = 10,
            nowMillis = evenWeekNow,
            zone = zone
        )
        assertNotNull("双周不该空手而归：要顺延到下一个单周", skipped)
        assertEquals("双周必须跳过这一周", LocalDate.of(2026, 10, 26), startAt(skipped!!).toLocalDate())
        assertEquals(9, com.dailybook.app.ui.study.weekNumberFor(termStart.toDayMillis(), LocalDate.of(2026, 10, 26)))
    }

    @Test
    fun courseWithoutStartTimeIsIgnored() {
        val noTime = CourseEntity(
            name = "没填时间的课",
            dayOfWeek = 1,
            startMinutes = -1,
            endMinutes = -1,
            weeks = "",
            termStartMillis = termStart.toDayMillis(),
            createdAt = 0L
        )
        assertNull(
            "没填时间的课排不出提醒",
            ClassSchedule.nextOccurrence(
                courses = listOf(noTime),
                leadMinutes = 15,
                nowMillis = at(monday, 7, 0),
                zone = zone
            )
        )

        // 和一门填了时间的课放在一起时，只挑填了时间的那一门
        val withTime = course(name = "填了时间的课", startMinutes = 10 * 60, endMinutes = 11 * 60 + 40)
        val picked = ClassSchedule.nextOccurrence(
            courses = listOf(noTime, withTime),
            leadMinutes = 15,
            nowMillis = at(monday, 7, 0),
            zone = zone
        )
        assertNotNull(picked)
        assertEquals("填了时间的课", picked!!.course.name)
        assertEquals(monday.atTime(10, 0), startAt(picked))
    }

    @Test
    fun outOfRangeStartMinutesIsIgnored() {
        // 1440 是「第二天 00:00」，不是合法钟点；瞎填的数据不该算出乱七八糟的提醒
        val broken = course(startMinutes = 1440, endMinutes = 1500)
        assertNull(
            ClassSchedule.nextOccurrence(
                courses = listOf(broken),
                leadMinutes = 15,
                nowMillis = at(monday, 7, 0),
                zone = zone
            )
        )
    }

    @Test
    fun earliestCourseOfTheDayWins() {
        val early = course(name = "早课", startMinutes = 8 * 60, endMinutes = 9 * 60 + 40)
        val late = course(name = "晚课", startMinutes = 14 * 60, endMinutes = 15 * 60 + 40)
        val occurrence = ClassSchedule.nextOccurrence(
            // 故意把晚课放在前面：结果不该受列表顺序影响
            courses = listOf(late, early),
            leadMinutes = 30,
            nowMillis = at(monday, 7, 0),
            zone = zone
        )

        assertNotNull(occurrence)
        assertEquals("同一天取最早的一节，与列表顺序无关", "早课", occurrence!!.course.name)
        assertEquals(monday.atTime(7, 30), notifyAt(occurrence))
    }

    @Test
    fun notifyMomentAlreadyPassedStillKeepsTheOccurrence() {
        // 周一 07:50：8:00 的课已经过了「提前 15 分钟」（07:45）这个点，
        // 但课还没开始 —— 宁可晚一点响，也不能整整一节课不吭声
        val now = at(monday, 7, 50)
        val occurrence = ClassSchedule.nextOccurrence(
            courses = listOf(course()),
            leadMinutes = 15,
            nowMillis = now,
            zone = zone
        )

        assertNotNull("课还没开始，就该返回它", occurrence)
        assertEquals(monday.atTime(8, 0), startAt(occurrence!!))
        assertEquals(
            "提醒时刻仍然按「上课时刻 - 提前量」算，由排程那边提前到「现在」响",
            monday.atTime(7, 45),
            notifyAt(occurrence)
        )
        assertTrue("排程会把过去时刻顶到当前", occurrence.notifyAtMillis < now)
    }

    @Test
    fun emptyCourseListGivesNull() {
        assertNull(
            ClassSchedule.nextOccurrence(
                courses = emptyList(),
                leadMinutes = 15,
                nowMillis = at(monday, 7, 0),
                zone = zone
            )
        )
    }

    @Test
    fun searchStopsAfterTwoWeeksWhenNothingMatches() {
        // 构造「真的找不到」得靠周次，不能靠星期几：
        // 从周一 07:00 起 14 天的窗口里一定会包含下一个周六（5 天后），
        // 空 weeks 又等于「每周都上」（这条语义在 StudyLogicTest 里也钉住了），
        // 所以「周六的课」在窗口内本来就该被找到 —— 原来的期望写错了。
        // 现在的构造：课程只在第 20~30 周上，而今天是第 3 周 → 窗口内一次都命不中，
        // 应该返回 null（而不是无限往后找）。
        val farWeeksOnly = course(dayOfWeek = 1, weeks = "20-30")
        assertNull(
            ClassSchedule.nextOccurrence(
                courses = listOf(farWeeksOnly),
                leadMinutes = 15,
                nowMillis = at(monday, 7, 0),
                zone = zone
            )
        )

        // 反例对照：把周次放开，同一天就能命中，说明上面的 null 确实来自周次过滤
        val anyWeek = course(dayOfWeek = 1, weeks = "")
        assertNotNull(
            ClassSchedule.nextOccurrence(
                courses = listOf(anyWeek),
                leadMinutes = 15,
                nowMillis = at(monday, 7, 0),
                zone = zone
            )
        )

        // 换成周一的课就能找到，且正好是 7 天后
        val occurrence = ClassSchedule.nextOccurrence(
            courses = listOf(course(dayOfWeek = 1)),
            leadMinutes = 15,
            nowMillis = at(monday, 7, 0),
            zone = zone
        )
        assertEquals(monday.atTime(8, 0), startAt(occurrence!!))
    }

    @Test
    fun courseTimeCanFallBackToTheSettingsTermStart() {
        // 课程自己没填学期起始日（0）→ 用设置里的那个；否则算不出周次，单双周就废了
        val oddOnly = course(weeks = "1-16单", termStartMillis = 0L)

        // 双周（第 8 周）：兜底学期起始日生效 → 跳过
        val skipped = ClassSchedule.nextOccurrence(
            courses = listOf(oddOnly),
            leadMinutes = 10,
            nowMillis = at(LocalDate.of(2026, 10, 19), 7, 0),
            zone = zone,
            fallbackTermStartMillis = termStart.toDayMillis()
        )
        assertNotNull(skipped)
        assertEquals(LocalDate.of(2026, 10, 26), startAt(skipped!!).toLocalDate())

        // 不兜底（0）时周次算成 0，weeksMatch 对 0 一律放行 → 双周也会排，语义不同、结果不同
        val withoutFallback = ClassSchedule.nextOccurrence(
            courses = listOf(oddOnly),
            leadMinutes = 10,
            nowMillis = at(LocalDate.of(2026, 10, 19), 7, 0),
            zone = zone
        )
        assertNotNull(withoutFallback)
        assertEquals(LocalDate.of(2026, 10, 19), startAt(withoutFallback!!).toLocalDate())
    }

    @Test
    fun formattingHelpersAreUsedByTheUi() {
        // 界面上的 `08:00–09:40` 就是这两个函数拼出来的，顺手钉住
        assertEquals("08:00", ClassSchedule.formatMinutes(8 * 60))
        assertEquals("09:40", ClassSchedule.formatMinutes(9 * 60 + 40))
        assertEquals("08:00–09:40", ClassSchedule.formatRange(8 * 60, 9 * 60 + 40))
        assertEquals("08:00", ClassSchedule.formatRange(8 * 60, -1))
        assertEquals("", ClassSchedule.formatRange(-1, -1))
        assertEquals("", ClassSchedule.formatMinutes(-1))
        assertTrue(ClassSchedule.isValidTime(0))
        assertTrue(ClassSchedule.isValidTime(1439))
        assertTrue(!ClassSchedule.isValidTime(-1))
        assertTrue(!ClassSchedule.isValidTime(1440))
    }
}
