package com.dailybook.app.util

import com.dailybook.app.data.CourseEntity
import com.dailybook.app.ui.study.weekNumberFor
import com.dailybook.app.ui.study.weeksMatch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** 一次即将到来的课 */
data class ClassOccurrence(val course: CourseEntity, val startMillis: Long, val notifyAtMillis: Long)

/**
 * 上课提醒的**纯时间计算**：不碰 AlarmManager、不碰 Context，所以能直接写单元测试
 * （这个文件里没有任何 `android.*` 导入，`ClassScheduleTest` 在 JVM 上就能跑）。
 *
 * 为什么要单独一层：课表原本只存「周几 + 第几节」，第几节是教学安排
 * （第 1 节可能 8:00 也可能 8:30），换算不出真实钟点，也就排不出提醒。
 * 现在每门课可以填 [CourseEntity.startMinutes] 等真实时间，
 * 「下一次该提醒哪节课」就退化成一个纯日期问题，全部收在这里。
 *
 * 只认填了 [CourseEntity.startMinutes] 的课（-1 = 没填 = 跳过）；
 * 没填时间的课在课表里照旧显示，只是不排提醒。
 */
object ClassSchedule {

    /** 一天有多少分钟：合法钟点的上界 */
    const val MINUTES_OF_DAY: Int = 1440

    /** 没填时间的哨兵值（和 [CourseEntity.startMinutes] 的默认值一致） */
    const val NO_TIME: Int = -1

    /** 没填下课时间时，按一节课 45 分钟估 */
    const val DEFAULT_LESSON_MINUTES: Int = 45

    /**
     * 最多往后找多少天。
     *
     * 14 天 = 覆盖两个完整周：单双周、`3,5,7-9` 这种跳周的课即使这周不上，
     * 也能在窗口里找到下一次；再远就没意义了（用户迟早会再打开 App，那时会重排一次）。
     */
    private const val SEARCH_DAYS: Int = 14

    /** 课表里给用户看的钟点写法：固定 24 小时制 `08:00`（是数据不是文案，四语一致） */
    private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * 下一次该提醒的课；没有（没课、没填时间、已过今天最后一节且无后续）时返回 null。
     *
     * 同一天有多节课时取最早的一节；提醒时刻 = 上课时刻 - [leadMinutes]，且必须晚于 [nowMillis]。
     *
     * - **周次**：用课程自己的 [CourseEntity.termStartMillis] 算「第几周」（它 <= 0 时退回
     *   [fallbackTermStartMillis]，也就是设置里的学期起始日），再用 [weeksMatch] 判断这周上不上 ——
     *   所以只在单周开的课，双周不会响。
     * - **窗口**：从 [nowMillis] 所在的那天起，往后最多 [SEARCH_DAYS] 天。
     * - **今天已经过了提醒时刻、但课还没结束**的课照样返回（宁可晚一点响，也别整整一节课不吭声）：
     *   此时 [ClassOccurrence.notifyAtMillis] 会早于 [nowMillis]，排程那边会把它提到「现在」立刻响。
     *   这一条**只对今天**成立，判据是「这节课的下课时刻还晚于现在」（[endOfCourse]）：
     *   - 课已经上完（下课时刻 <= [nowMillis]）→ 跳过今天，顺延到下一次（通常是下周）；
     *   - 没填下课时间、或填得比上课还早 → 按 [DEFAULT_LESSON_MINUTES] 估一节课，
     *     所以「还在上没上」是按这个估值判断的：开局 45 分钟之后就当这节课过去了；
     *   - 窗口里更靠后的那些天（`offset >= 1`）不存在「已经上完」，只会被周次 / 星期几筛掉。
     *
     * @param leadMinutes 提前多少分钟提醒（负数按 0 处理）
     * @param fallbackTermStartMillis 课程自己没填学期起始日时的兜底值（设置里的那个），0 = 没有
     */
    fun nextOccurrence(
        courses: List<CourseEntity>,
        leadMinutes: Int,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        fallbackTermStartMillis: Long = 0L
    ): ClassOccurrence? {
        if (courses.isEmpty()) return null

        val lead = leadMinutes.coerceAtLeast(0)
        val today: LocalDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val now = nowMillis

        // 每门课只留「真能排提醒」的：填了时间，且钟点落在一整天之内
        val usable = courses.filter { it.startMinutes in 0 until MINUTES_OF_DAY }
        if (usable.isEmpty()) return null

        // 从今天起一天天往后看：第一次命中那天，就是最早的那天，
        // 所以在那天取最早的一节课即可收工（不需要遍历满 14 天）
        for (offset in 0..SEARCH_DAYS) {
            val date = today.plusDays(offset.toLong())
            val dayOfWeek = date.dayOfWeek.value // 1 = 周一 … 7 = 周日

            var best: ClassOccurrence? = null
            usable.forEach { course ->
                if (course.dayOfWeek != dayOfWeek) return@forEach

                // 周次按课算：每门课的学期起始日可能不一样
                val termStart =
                    if (course.termStartMillis > 0L) course.termStartMillis else fallbackTermStartMillis
                if (!weeksMatch(course.weeks, weekNumberFor(termStart, date))) return@forEach

                val startMillis = date.atTime(
                    course.startMinutes / 60,
                    course.startMinutes % 60
                ).atZone(zone).toInstant().toEpochMilli()

                // 这节课已经上完了：这个时刻之后再提醒没有意义
                if (endOfCourse(date, course, startMillis, zone) <= now) return@forEach

                if (best == null || startMillis < best!!.startMillis) {
                    best = ClassOccurrence(
                        course = course,
                        startMillis = startMillis,
                        notifyAtMillis = startMillis - lead * 60_000L
                    )
                }
            }

            if (best != null) return best
        }

        return null
    }

    /**
     * 这节课什么时候下课，用来判断「还值不值得提醒」。
     *
     * 没填下课时间、或填得比上课还早时，按 [DEFAULT_LESSON_MINUTES] 估一节 ——
     * 免得刚上课一分钟就把这节课当成已经过去、直接跳到下周去了。
     */
    private fun endOfCourse(
        date: LocalDate,
        course: CourseEntity,
        startMillis: Long,
        zone: ZoneId
    ): Long {
        val end = course.endMinutes
        return if (isValidTime(end) && end > course.startMinutes) {
            date.atTime(end / 60, end % 60).atZone(zone).toInstant().toEpochMilli()
        } else {
            startMillis + DEFAULT_LESSON_MINUTES * 60_000L
        }
    }

    /** 这个分钟数是不是一个合法的钟点（-1 = 没填，不算合法也不算错误） */
    fun isValidTime(minutes: Int): Boolean = minutes in 0 until MINUTES_OF_DAY

    /** 分钟数 → 本地时间；没填或非法时返回 null */
    fun minutesToTime(minutes: Int): LocalTime? =
        if (isValidTime(minutes)) LocalTime.of(minutes / 60, minutes % 60) else null

    /** 分钟数 → `08:00`；没填（-1 之类）返回空串，由界面决定显示什么 */
    fun formatMinutes(minutes: Int): String = minutesToTime(minutes)?.format(CLOCK).orEmpty()

    /** `08:00`，但按当前语言的时间写法（通知里用；界面里用 [formatMinutes] 更稳） */
    fun formatMinutesLocalized(minutes: Int): String =
        minutesToTime(minutes)
            ?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
            .orEmpty()

    /** `08:00–09:40`；只填了一头就只显示那一头，两头都没填返回空串 */
    fun formatRange(startMinutes: Int, endMinutes: Int): String {
        val start = formatMinutes(startMinutes)
        val end = formatMinutes(endMinutes)
        return when {
            start.isNotEmpty() && end.isNotEmpty() -> "$start–$end"
            start.isNotEmpty() -> start
            else -> end
        }
    }
}
