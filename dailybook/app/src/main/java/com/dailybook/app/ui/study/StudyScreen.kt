package com.dailybook.app.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.CourseEntity
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.StudyStrings
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.Route
import com.dailybook.app.ui.SectionCard
import com.dailybook.app.ui.Shapes
import com.dailybook.app.ui.StatBlock
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 学习首页（底部「学习」标签的内容）。
 *
 * 定位：先给「今天要干什么、最急的是什么」一个答案，再给通往七个子页面的入口。
 * 所以最上面是今天的课程（不用点进课表就能看到），中间是四个数字块，
 * 下面才是入口卡片网格 —— 每张卡片把 [Route] 推给父级的页面栈。
 *
 * 数据全部来自 [UiState]（父级算好的），这一页只做展示与跳转，不重新统计。
 */

/** 日期短格式：四语统一用数字，避免各语言月份名带来的排版差异 */
private val HUB_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)

@Composable
fun StudyScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val today = LocalDate.now()
    val todayIndex = today.dayOfWeek.value - 1

    // 本周 / 今日课程：周次用课表那套算法（见 CoursesScreen.kt 里 weeksMatch 的 KDoc）。
    // 学期起始日的来源与上课提醒一致：课自己的优先，都没填就用设置里那个（见 termStartOf）。
    // 一门课都没有时，weekNumberFor 会退化成「忽略周次」。
    val settingTermStart by vm.settings.termStartMillis.collectAsStateWithLifecycle()
    val termStart = remember(state.courses, settingTermStart) {
        termStartOf(state.courses, settingTermStart)
    }
    val weekNumber = remember(termStart, today) { weekNumberFor(termStart, today) }
    val todayCourses = remember(state.courses, weekNumber, todayIndex) {
        state.courses
            .filter { it.dayOfWeek == todayIndex + 1 && weeksMatch(it.weeks, weekNumber) }
            .sortedBy { it.startPeriod }
    }
    val weekCourses = remember(state.courses, weekNumber) {
        state.courses.filter { weeksMatch(it.weeks, weekNumber) }
    }
    val openAssignments = state.assignmentTodos.count { !it.done }

    val entries = listOf(
        HubEntry(
            icon = Icons.Filled.CalendarMonth,
            title = AppStrings.tabCourses(lang),
            hint = StudyStrings.hubHintCourses(lang, todayCourses.size),
            route = Route.Courses
        ),
        HubEntry(
            icon = Icons.Filled.Assignment,
            title = AppStrings.tabAssignments(lang),
            hint = StudyStrings.hubHintAssignments(lang, openAssignments, state.overdueAssignments),
            route = Route.Assignments
        ),
        HubEntry(
            icon = Icons.Filled.Quiz,
            title = AppStrings.tabExams(lang),
            hint = StudyStrings.hubHintExams(lang, state.exams.size),
            route = Route.Exams
        ),
        HubEntry(
            icon = Icons.Filled.Calculate,
            title = AppStrings.tabGrades(lang),
            hint = StudyStrings.hubHintGrades(lang, state.grades.size),
            route = Route.Grades
        ),
        HubEntry(
            icon = Icons.Filled.School,
            title = AppStrings.tabCredits(lang),
            hint = StudyStrings.hubHintCredits(
                lang,
                // 和成绩页 / 学分进度页同一个口径（只算拿到学分的课，见 GradesScreen.earnedCredit 的 KDoc）。
                // 父级的 state.totalCredits 目前是「所有成绩的学分之和」，两边对不上，
                // 所以这里按同一口径重算一遍，三处显示的数字才不会各说各话。
                formatCredits(earnedCredits(state.grades)),
                formatCredits(state.creditTargets.sumOf { it.required })
            ),
            route = Route.Credits
        ),
        HubEntry(
            icon = Icons.Filled.EmojiEvents,
            title = AppStrings.tabAwards(lang),
            hint = StudyStrings.hubHintAwards(lang, state.awards.size),
            route = Route.Awards
        ),
        // 这里原来还有一张「背单词」卡片，点进去挂的却是**习惯打卡**界面
        // （顶栏标题还写着「背单词」），等于同一个界面在「生活 → 习惯打卡」之外
        // 又开了一个名不副实的入口。背单词/背书计划本来就是「单位不是『次』的习惯」，
        // 在习惯打卡里把单位设成「个/页」即可，所以这里不再重复放入口。
        HubEntry(
            icon = Icons.Filled.Description,
            title = AppStrings.tabWeeklyReport(lang),
            hint = StudyStrings.hubHintWeekly(lang),
            route = Route.WeeklyReport
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = StudyStrings.hubTitle(lang),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = StudyStrings.hubToday(
                lang,
                today.format(HUB_DATE_FORMAT),
                AppStrings.weekday(lang, todayIndex)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ---- 四个数字块：今天 / 本周课程、最近的考试、未完成作业、GPA ----
        Spacer(Modifier.height(12.dp))
        SectionCard {
            Row(Modifier.fillMaxWidth()) {
                StatBlock(
                    label = StudyStrings.hubTodaySection(lang),
                    value = StudyStrings.hubCountValue(lang, todayCourses.size),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = StudyStrings.coursesViewWeek(lang),
                    value = StudyStrings.hubCountValue(lang, weekCourses.size),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = StudyStrings.gradesGpa(lang),
                    value = formatGpa(state.gpa),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = StudyStrings.hubExamLabel(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = examCountdownText(state, lang),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (state.examDaysLeft in 0..7) expenseColor()
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = state.nextExam?.name.orEmpty().ifBlank { StudyStrings.hubNoExam(lang) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = StudyStrings.hubAssignmentLabel(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = StudyStrings.hubCountValue(lang, openAssignments),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (state.overdueAssignments > 0) {
                            StudyStrings.hubOverdueValue(lang, state.overdueAssignments)
                        } else {
                            StudyStrings.hubHintAssignments(lang, openAssignments, 0)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.overdueAssignments > 0) expenseColor()
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            if (state.courses.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = StudyStrings.coursesWeekNumber(lang, weekNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- 今日课程：不打开课表也能用 ----
        Spacer(Modifier.height(14.dp))
        SectionCard(title = StudyStrings.hubTodaySection(lang)) {
            if (todayCourses.isEmpty()) {
                Text(
                    text = StudyStrings.hubNoCourseToday(lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                todayCourses.forEachIndexed { index, course ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    TodayCourseRow(course, lang)
                }
            }
        }

        // ---- 入口卡片网格 ----
        Spacer(Modifier.height(14.dp))
        Text(
            text = StudyStrings.hubEntries(lang),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(10.dp))
        entries.chunked(2).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { entry ->
                    EntryCard(
                        entry = entry,
                        modifier = Modifier.weight(1f),
                        onClick = { nav.push(entry.route) }
                    )
                }
                // 入口是奇数个时补一个空位，最后一行不会只剩一张卡拉到全宽
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

/** 首页入口卡片：图标 + 标题 + 一行提示 */
private data class HubEntry(
    val icon: ImageVector,
    val title: String,
    val hint: String,
    val route: Route
)

@Composable
private fun EntryCard(
    entry: HubEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, Shapes.card)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), Shapes.badge),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = entry.hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
    }
}

/** 今日课程的一行：左侧色条 + 课程名 + 节次与地点 */
@Composable
private fun TodayCourseRow(course: CourseEntity, lang: Lang) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 6.dp, height = 42.dp)
                .background(courseColor(course.colorIndex), Shapes.pill)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (course.location.isBlank()) {
                    periodText(lang, course.startPeriod, course.endPeriod)
                } else {
                    StudyStrings.coursesRowSubtitle(
                        lang,
                        "",
                        periodText(lang, course.startPeriod, course.endPeriod),
                        course.location
                    ).trim()
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 概览里考试那一格：有考试显示天数，没有就显示破折号 */
private fun examCountdownText(state: UiState, lang: Lang): String = when {
    state.nextExam == null -> "—"
    state.examDaysLeft == 0L -> StudyStrings.examsToday(lang)
    state.examDaysLeft < 0L -> StudyStrings.examsFinished(lang)
    else -> StudyStrings.hubDays(lang, state.examDaysLeft)
}

/** GPA 保留两位小数（四语都用同一个数字写法） */
internal fun formatGpa(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

/** 学分去掉没意义的小数尾巴：24.0 → 24，24.5 → 24.5 */
internal fun formatCredits(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded == Math.floor(rounded)) {
        rounded.toLong().toString()
    } else {
        String.format(Locale.ROOT, "%.1f", rounded)
    }
}

/** 「第 1-2 节」/「第 3 节」，课表、首页、考试页共用 */
internal fun periodText(lang: Lang, start: Int, end: Int): String =
    if (start == end) StudyStrings.coursesPeriodOne(lang, start)
    else StudyStrings.coursesPeriodRange(lang, start, end)

/**
 * 课表用的「学期起始日」（第 1 周的锚点）。
 *
 * **优先级和上课提醒完全一致**（[com.dailybook.app.util.ClassSchedule.nextOccurrence]）：
 * 1. 各门课自己的 [CourseEntity.termStartMillis]：填了的课各自以自己为准，
 *    这里取其中最早的（= 本学期从哪天开始），课表 / 首页 / 新建课程的默认值都用它；
 * 2. 一门课都没填（全是 0）→ 设置里的「学期起始日」（[settingMillis]）；
 * 3. 设置里也没设 → 今天往前一周（纯兜底，只是为了有个能算周次的锚点）。
 *
 * 以前第 2 步是缺的：上课提醒拿设置兜底、课表却只看课程自身的字段，
 * 于是「设置 → 学期起始日」改了以后闹钟按新周次响、课表上的「第 N 周」纹丝不动，
 * 同一门没填起始日的课在两处显示的周次可以完全不一样。
 */
internal fun termStartOf(courses: List<CourseEntity>, settingMillis: Long = 0L): Long =
    courses.filter { it.termStartMillis > 0L }.minOfOrNull { it.termStartMillis }
        ?: settingMillis.takeIf { it > 0L }
        ?: LocalDate.now().minusWeeks(1).toDayMillis()

/**
 * 把某一天归到它所在那一周的周一（当天 00:00）。
 *
 * [weekNumberFor] 内部就是这么算的（「第 1 周」以周一为始），所以存进
 * [CourseEntity.termStartMillis] 的值也归一下，界面上写的日期、设置里的日期
 * 和「第几周」的算法才是同一个锚点：字段标签写着「学期起始日（第 1 周的周一）」，
 * 用户选了周三时如果不归位，标签说的和实际算的就不是一回事。
 */
internal fun weekStartOf(millis: Long): Long {
    if (millis <= 0L) return 0L
    val date = millis.toLocalDate()
    return date.minusDays((date.dayOfWeek.value - 1).toLong()).toDayMillis()
}
