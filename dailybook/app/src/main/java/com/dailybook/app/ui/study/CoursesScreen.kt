package com.dailybook.app.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.CourseEntity
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.StudyStrings
import com.dailybook.app.ui.ChipFlow
import com.dailybook.app.ui.ConfirmDialog
import com.dailybook.app.ui.EmptyHint
import com.dailybook.app.ui.FieldLabel
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.SectionCard
import com.dailybook.app.ui.Shapes
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.util.ClassSchedule
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * 课表。
 *
 * 一节课 = 一条 [CourseEntity]（星期几 + 第几节到第几节 + 周次表达式），
 * 所以「一格跨 2 节」「单双周」「跳周」都不用另建表：周次语法见 [weeksMatch]。
 *
 * 布局：手机宽度放不下 7 天 × 12 节，所以整张表做成**横向可滚动**的固定尺寸网格
 * （宽 7 × 每列 64dp），外面套一层纵向滚动 —— 两轴各自一个滚动容器，
 * 没有任何 LazyColumn 嵌在 verticalScroll 里。
 * 另外给一个「今天 / 本周」开关：平时只看今天，周末排课 / 临时调课时再切本周。
 */

// ============================================================
// 周次表达式
// ============================================================

/**
 * 判断「第 [week] 周」是否命中周次表达式 [expression]（从 1 开始计数）。
 *
 * 语法（分隔符支持 `,`、`，`、空格；单个数字与区间可混用）：
 * - `""`（空）或全空白 —— **每周都有**；
 * - `1-16` —— 第 1 到第 16 周；
 * - `1,3,5-9` —— 第 1、3、5、6、7、8、9 周；
 * - `1-16单` —— 先按 `1-16` 命中，再只保留**奇数周**；
 * - `2-16双` —— 先命中，再只保留**偶数周**；
 * - `单` / `双` 也可以写在**开头**（`单1-16`），等价于后缀写法；
 * - `9` —— 只上第 9 周（单周次补课常用）。
 *
 * 解析不了的片段会被**忽略**（例如 `abc`、`0`、`20-10` 这种倒序区间），
 * 全部片段都无效时按「每周都有」处理 —— 宁可多显示一节课，也不要让用户以为课没了。
 * `week <= 0`（学期还没开始、或日期算不出来）同样返回 true，理由相同。
 *
 * 抽成顶层纯函数是为了能直接测边界，不依赖 Compose 与时间。
 */
fun weeksMatch(expression: String, week: Int): Boolean {
    if (week <= 0) return true
    val text = expression.trim()
    if (text.isEmpty()) return true

    var body = text
    var parity = 0 // 0 = 不限，1 = 单周，-1 = 双周
    // 后缀写法：1-16单 / 1-16双
    val last = body.last()
    if (last == '单' || last == '單') {
        parity = 1
        body = body.dropLast(1)
    } else if (last == '双' || last == '雙') {
        parity = -1
        body = body.dropLast(1)
    }
    // 前缀写法：单1-16 / 双2-16
    val first = body.firstOrNull()
    if (first == '单' || first == '單') {
        parity = 1
        body = body.drop(1)
    } else if (first == '双' || first == '雙') {
        parity = -1
        body = body.drop(1)
    }

    val pieces = body.split(',', '，', ' ', '、').map { it.trim() }.filter { it.isNotEmpty() }
    if (pieces.isEmpty()) return true

    var matched = false
    var anyValid = false
    pieces.forEach { piece ->
        val dash = piece.indexOf('-')
        if (dash > 0) {
            // 区间：a-b（a > b 的倒序区间按无效处理）
            val start = piece.substring(0, dash).trim().toIntOrNull()
            val end = piece.substring(dash + 1).trim().toIntOrNull()
            if (start != null && end != null && start in 1..60 && end in start..60) {
                anyValid = true
                if (week in start..end) matched = true
            }
        } else {
            val single = piece.toIntOrNull()
            if (single != null && single in 1..60) {
                anyValid = true
                if (week == single) matched = true
            }
        }
    }

    // 一段都解析不出来（用户随手写错了）时按「每周都有」，避免课表整个空掉
    if (!anyValid) return true
    if (!matched) return false

    return when (parity) {
        1 -> week % 2 == 1
        -1 -> week % 2 == 0
        else -> true
    }
}

/**
 * 第几周（周一为一周之始）：`((今天 - 学期起始日) / 7) + 1`。
 *
 * [termStartMillis] 先归到它所在那一周的周一，所以用户填了周三也不会整体偏一天。
 * 还没开学（差值为负）时返回 0 —— [weeksMatch] 对 0 一律放行，课表照样能看。
 */
fun weekNumberFor(termStartMillis: Long, today: LocalDate = LocalDate.now()): Int {
    if (termStartMillis <= 0L) return 0
    val start = termStartMillis.toLocalDate().let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
    val days = ChronoUnit.DAYS.between(start, today)
    if (days < 0L) return 0
    return (days / 7L).toInt() + 1
}

// ============================================================
// 课表配色
// ============================================================

/**
 * 固定 6 色课程板（与分类色板一样是「数据色」，不跟随主题，课表看起来才稳定）。
 * 取的是中等明度，配 [courseTextColor] 选出的黑白文字，深浅主题下都清楚。
 */
private val COURSE_COLORS = listOf(
    Color(0xFF5B8DEF), Color(0xFF5FBF96), Color(0xFFF2A93B),
    Color(0xFF9B8CFF), Color(0xFFE86FA9), Color(0xFF4EC5C1)
)

/** 课表配色：按 [colorIndex] 稳定取色，越界自动回绕 */
internal fun courseColor(colorIndex: Int): Color {
    val index = ((colorIndex % COURSE_COLORS.size) + COURSE_COLORS.size) % COURSE_COLORS.size
    return COURSE_COLORS[index]
}

/** 按底色明暗自动选黑字或白字，保证任何时候都读得清 */
internal fun courseTextColor(background: Color): Color =
    if (background.luminance() > 0.55f) Color(0xFF1B1C1F) else Color(0xFFFFFFFF)

// ---- 网格尺寸：列宽固定，整张表横向滚动 ----
private val CELL_WIDTH = 64.dp
private val CELL_HEIGHT = 54.dp

@Composable
fun CoursesScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val today = LocalDate.now()
    val todayIndex = today.dayOfWeek.value - 1 // 0 = 周一

    var showTodayOnly by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<CourseEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CourseEntity?>(null) }

    // 学期起始日：所有课程里最早的那个（新课程默认沿用，免得每次重填）
    val defaultTermStart = remember(state.courses) { termStartOf(state.courses) }
    val weekNumber = remember(defaultTermStart, today) { weekNumberFor(defaultTermStart, today) }

    // 本周视图用的列表：当前周次命中的全部课程（与「今天」开关无关，切回来时不用重算）
    val weekCourses = remember(state.courses, weekNumber) {
        state.courses.filter { weeksMatch(it.weeks, weekNumber) }
    }
    val todayCourses = remember(state.courses, weekNumber, todayIndex) {
        state.courses
            .filter { it.dayOfWeek == todayIndex + 1 && weeksMatch(it.weeks, weekNumber) }
            .sortedBy { it.startPeriod }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = StudyStrings.coursesTitle(lang),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (weekNumber > 0) {
                    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
                    StudyStrings.coursesWeekRange(
                        lang,
                        weekNumber,
                        AppStrings.monthDay(lang, monday.monthValue, monday.dayOfMonth),
                        AppStrings.monthDay(
                            lang,
                            monday.plusDays(6).monthValue,
                            monday.plusDays(6).dayOfMonth
                        )
                    )
                } else {
                    StudyStrings.coursesTermStart(lang)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 今天 / 本周
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = showTodayOnly,
                    onClick = { showTodayOnly = true },
                    label = { Text(StudyStrings.coursesViewToday(lang)) }
                )
                FilterChip(
                    selected = !showTodayOnly,
                    onClick = { showTodayOnly = false },
                    label = { Text(StudyStrings.coursesViewWeek(lang)) }
                )
            }

            if (state.courses.isEmpty()) {
                Spacer(Modifier.height(40.dp))
                EmptyHint(
                    emoji = "📚",
                    title = StudyStrings.coursesEmpty(lang),
                    subtitle = StudyStrings.coursesEmptyHint(lang)
                )
            } else if (showTodayOnly) {
                Spacer(Modifier.height(12.dp))
                SectionCard(
                    title = StudyStrings.coursesTodayHeader(
                        lang,
                        AppStrings.weekday(lang, todayIndex),
                        AppStrings.monthDay(lang, today.monthValue, today.dayOfMonth)
                    )
                ) {
                    if (todayCourses.isEmpty()) {
                        Text(
                            text = StudyStrings.coursesNoCourseToday(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        todayCourses.forEachIndexed { index, course ->
                            if (index > 0) Spacer(Modifier.height(10.dp))
                            TodayCourseBlock(course, lang)
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                WeekGrid(
                    courses = weekCourses,
                    todayIndex = todayIndex,
                    lang = lang,
                    onPick = { editing = it }
                )
            }

            // 全部课程：编辑 / 删除的入口（课表格子太小，点了直接开编辑）
            if (state.courses.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionCard(title = StudyStrings.coursesListTitle(lang)) {
                    state.courses.forEachIndexed { index, course ->
                        if (index > 0) Spacer(Modifier.height(6.dp))
                        CourseRow(
                            course = course,
                            lang = lang,
                            onEdit = { editing = course },
                            onDelete = { deleting = course }
                        )
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
        }

        FloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = StudyStrings.coursesAddTitle(lang)
            )
        }
    }

    if (adding) {
        CourseDialog(
            existing = null,
            defaultTermStart = defaultTermStart,
            onDismiss = { adding = false },
            onSave = { name, teacher, location, day, start, end, weeks, termStart, startMinutes, endMinutes, color ->
                vm.addCourse(
                    name = name,
                    teacher = teacher,
                    location = location,
                    dayOfWeek = day,
                    startPeriod = start,
                    endPeriod = end,
                    weeks = weeks,
                    termStartMillis = termStart,
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                    colorIndex = color
                )
                adding = false
            }
        )
    }

    editing?.let { course ->
        CourseDialog(
            existing = course,
            defaultTermStart = defaultTermStart,
            onDismiss = { editing = null },
            onSave = { name, teacher, location, day, start, end, weeks, termStart, startMinutes, endMinutes, color ->
                vm.updateCourse(
                    course.copy(
                        name = name,
                        teacher = teacher,
                        location = location,
                        dayOfWeek = day,
                        startPeriod = start,
                        endPeriod = end,
                        weeks = weeks,
                        termStartMillis = termStart,
                        startMinutes = startMinutes,
                        endMinutes = endMinutes,
                        colorIndex = color
                    )
                )
                editing = null
            },
            onDelete = {
                vm.deleteCourse(course)
                editing = null
            }
        )
    }

    deleting?.let { course ->
        ConfirmDialog(
            title = StudyStrings.coursesDelete(lang),
            text = StudyStrings.coursesDeleteConfirm(lang, course.name),
            confirmText = AppStrings.delete(lang),
            onConfirm = { vm.deleteCourse(course) },
            onDismiss = { deleting = null }
        )
    }
}

// ============================================================
// 周网格
// ============================================================

/** 网格的行数上限：12 节 */
private const val MAX_PERIOD = 12

/**
 * 固定尺寸的周课表：左列是节次，右侧 7 天。
 * 整块横向滚动（7 × 64dp = 448dp，比多数手机窄屏宽），纵向由外层 Column 负责。
 * [courses] 只放**当前周次命中的课**（今天 / 本周开关不影响网格本身）。
 */
@Composable
private fun WeekGrid(
    courses: List<CourseEntity>,
    todayIndex: Int,
    lang: Lang,
    onPick: (CourseEntity) -> Unit
) {
    val labelWidth = 34.dp
    val contentWidth = CELL_WIDTH * 7
    // 表头与网格**共用同一个滚动状态**：否则横向滑动网格时表头不动，
    // 「周六」会飘到别的列上面去，看着就不像一张表。
    val horizontal = rememberScrollState()
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    Column(Modifier.fillMaxWidth()) {
        // 表头：与下面的网格用同样的「外层 weight + 内层滚动」结构，保证两行对齐
        Row(Modifier.fillMaxWidth()) {
            // 左上角留白，与节次列对齐
            Spacer(Modifier.width(labelWidth))
            Row(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.horizontalScroll(horizontal)) {
                    (0..6).forEach { day ->
                        Box(
                            modifier = Modifier
                                .width(CELL_WIDTH)
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = AppStrings.weekday(lang, day),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (day == todayIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (day == todayIndex) FontWeight.Bold
                                    else FontWeight.Normal
                                )
                                if (day == todayIndex) {
                                    Text(
                                        text = StudyStrings.coursesTodayMark(lang),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 表头与内容之间的分隔线，表格的「表头」才立得住
        HorizontalDivider(color = lineColor)

        Row(Modifier.fillMaxWidth()) {
            // 节次列：每格底边也画一条线，和右侧网格的行线接上，整张表才是「格子」而不是悬浮色块
            Column(Modifier.width(labelWidth)) {
                (1..MAX_PERIOD).forEach { period ->
                    Box(
                        modifier = Modifier
                            .height(CELL_HEIGHT)
                            .fillMaxWidth()
                            .drawBehind {
                                val stroke = 1.dp.toPx()
                                drawLine(
                                    lineColor,
                                    Offset(0f, size.height - stroke / 2),
                                    Offset(size.width, size.height - stroke / 2),
                                    stroke
                                )
                            },
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            text = period.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 7 天 × 12 节的格子 + 叠在上面的课程块
            // 注意：横向滚动放在**外层** Box 上，里面的 Box 才是固定宽度 ——
            // 如果两者写在一起（weight + horizontalScroll 同时修饰一个节点），
            // 滚动容器会被塞进固定宽度里，表就滚不动了。
            Box(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.horizontalScroll(horizontal)) {
                    Box(
                        modifier = Modifier
                            .width(contentWidth)
                            .height(CELL_HEIGHT * MAX_PERIOD)
                            // 表格线：横线 13 条（12 节的上下沿 + 底边）、竖线 8 条（7 天分界 + 右边沿）。
                            // 用 drawBehind 画而不是堆分隔组件，格子尺寸就还是精确的 64×54，
                            // 课程块的 offset 定位也不会因为多出来的分隔条错位。
                            .drawBehind {
                                val stroke = 1.dp.toPx()
                                val w = size.width
                                val h = size.height
                                for (row in 0..MAX_PERIOD) {
                                    val y = (CELL_HEIGHT.toPx() * row).coerceAtMost(h)
                                    drawLine(lineColor, Offset(0f, y), Offset(w, y), stroke)
                                }
                                for (col in 0..7) {
                                    val x = (CELL_WIDTH.toPx() * col).coerceAtMost(w)
                                    drawLine(lineColor, Offset(x, 0f), Offset(x, h), stroke)
                                }
                            }
                    ) {
                        // 底格：只画很淡的分隔背景，不放任何可点内容
                        Column {
                            (1..MAX_PERIOD).forEach { _ ->
                                Row {
                                    (0..6).forEach { _ ->
                                        Box(
                                            modifier = Modifier
                                                .width(CELL_WIDTH)
                                                .height(CELL_HEIGHT)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                        .copy(alpha = 0.35f)
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        // 课程块：按「星期几 / 第几节」绝对定位（offset 在固定尺寸的父级里就是绝对定位）
                        courses.forEach { course ->
                            val dayIndex = (course.dayOfWeek - 1).coerceIn(0, 6)
                            val start = course.startPeriod.coerceIn(1, MAX_PERIOD)
                            val end = course.endPeriod.coerceIn(start, MAX_PERIOD)
                            val span = end - start + 1
                            CourseBlock(
                                course = course,
                                span = span,
                                modifier = Modifier
                                    .offset(x = CELL_WIDTH * dayIndex, y = CELL_HEIGHT * (start - 1))
                                    .width(CELL_WIDTH)
                                    .height(CELL_HEIGHT * span),
                                onClick = { onPick(course) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 网格里的一格课：底色取自 colorIndex，文字用自动对比色 */
@Composable
private fun CourseBlock(
    course: CourseEntity,
    span: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = courseColor(course.colorIndex)
    val textColor = courseTextColor(background)
    val range = ClassSchedule.formatRange(course.startMinutes, course.endMinutes)
    Box(
        modifier = modifier
            .padding(2.dp)
            .background(background, Shapes.badge)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column {
            Text(
                text = course.name,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                maxLines = if (span >= 3) 4 else 2,
                overflow = TextOverflow.Ellipsis
            )
            // 格子只有 64dp 宽，钟点单独一行、字号最小；没填时间的课就少这一行
            if (range.isNotEmpty()) {
                Text(
                    text = range,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (course.location.isNotBlank()) {
                Text(
                    text = course.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 今天视图里的一条课：色条 + 课程名 + 节次 / 教师 / 地点 */
@Composable
private fun TodayCourseBlock(course: CourseEntity, lang: Lang) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(46.dp)
                .background(courseColor(course.colorIndex), Shapes.pill)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = StudyStrings.coursesRowSubtitle(
                    lang,
                    AppStrings.weekday(lang, (course.dayOfWeek - 1).coerceIn(0, 6)),
                    periodText(lang, course.startPeriod, course.endPeriod),
                    course.location
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (course.teacher.isNotBlank()) {
                Text(
                    text = course.teacher,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 课程行：点行编辑，右侧 ✕ 删除 */
@Composable
private fun CourseRow(
    course: CourseEntity,
    lang: Lang,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
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
                text = buildString {
                    append(
                        StudyStrings.coursesRowSubtitle(
                            lang,
                            AppStrings.weekday(lang, (course.dayOfWeek - 1).coerceIn(0, 6)),
                            periodText(lang, course.startPeriod, course.endPeriod),
                            course.location
                        )
                    )
                    // 填了钟点就把 `08:00–09:40` 挂在节次后面；没填的课显示和以前一模一样
                    val range = ClassSchedule.formatRange(course.startMinutes, course.endMinutes)
                    if (range.isNotEmpty()) append(" · ").append(range)
                    if (course.weeks.isNotBlank()) append(" · ").append(course.weeks)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = StudyStrings.coursesDelete(lang),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================
// 添加 / 编辑弹窗
// ============================================================

/**
 * 「开始时间」的一键预设：常见的整点 / 半点上课时刻。
 * 只是省得在小屏上敲数字，用户照样可以自己填任意时间。
 */
private val START_PRESETS = listOf(8 * 60, 8 * 60 + 30, 9 * 60, 10 * 60, 14 * 60, 16 * 60, 18 * 60, 19 * 60)

/** 「结束时间」的预设：比开始时间常见的那几档晚一两节 */
private val END_PRESETS = listOf(9 * 60 + 40, 10 * 60 + 30, 11 * 60 + 30, 12 * 60, 15 * 60 + 40, 17 * 60 + 30, 19 * 60 + 30, 21 * 60)

private val TIME_PRESET_FORMAT = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

/** [parseClock] 的「没填」标记：和「填得不合法」区分开 */
private const val NO_CLOCK = Int.MIN_VALUE

/**
 * 把输入框里的 `8:00` / `08:00` / `8` 读成「当天 00:00 起的分钟数」。
 *
 * - 返回 [NO_CLOCK]：**没填**（空串，或者才写到 `8:` 这种半截状态）—— 存 -1，这门课不排提醒；
 * - 返回 null：**填得不合法**（小时不在 0..23、分钟不在 0..59）—— 拦下不让保存。
 */
private fun parseClock(text: String): Int? {
    val clean = text.trim()
    if (clean.isEmpty()) return NO_CLOCK
    val parts = clean.split(':')
    val hour = parts.getOrNull(0)?.trim().orEmpty()
    val minute = parts.getOrNull(1)?.trim().orEmpty()
    // 还没写到分钟（`8:`）不算填错，只当没填完
    if (hour.isEmpty()) return NO_CLOCK
    val h = hour.toIntOrNull() ?: return null
    val m = if (minute.isEmpty()) 0 else minute.toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

/** 把 [parseClock] 的结果收成「能存进数据库的分钟数」（没填 / 不合法都算 -1） */
private fun Int?.toStoredMinutes(): Int =
    if ((this == null) || (this == NO_CLOCK)) ClassSchedule.NO_TIME else this

/**
 * 一个钟点输入行：常用时间快选 chips + 时 / 分两个小输入框。
 *
 * 沿用设置页 `ReminderTimeDialog` 的「chip 快选」思路，但换成手填时 / 分两个格子 ——
 * 弹窗里要放两组时间，Material 的 `TimePicker` 表盘太大，两个就撑爆了。
 */
@Composable
private fun CourseTimeRow(
    value: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    presets: List<Int>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val current = parseClock(value)

    Column(modifier) {
        ChipFlow {
            presets.forEach { minutes ->
                val label = java.time.LocalTime.of(minutes / 60, minutes % 60)
                    .format(TIME_PRESET_FORMAT)
                FilterChip(
                    selected = current == minutes,
                    onClick = {
                        // 再点一次已选中的预设 = 取消选择，等于「这节课不填时间」
                        if (current == minutes) onClear() else onChange(label)
                    },
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        CourseTimeFields(
            value = value,
            onChange = onChange,
            accent = accent
        )
        if (value.isNotBlank()) {
            TextButton(onClick = onClear) { Text(StudyStrings.coursesTimeClear(lang)) }
        }
    }
}

/** 时 / 分两个输入框：只收数字，各有两位上限 */
@Composable
private fun CourseTimeFields(value: String, onChange: (String) -> Unit, accent: Color) {
    val parts = value.split(':')
    val hourText = parts.getOrNull(0).orEmpty()
    val minuteText = parts.getOrNull(1).orEmpty()
    val hour = hourText.toIntOrNull()
    val minute = minuteText.toIntOrNull()

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = hourText,
            // 越界直接打不进去：这样保存时几乎不会撞上校验（校验仍然保留，防手滑）
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(2)
                if (digits.isEmpty() || (digits.toIntOrNull() ?: 0) <= 23) {
                    onChange("$digits:$minuteText")
                }
            },
            singleLine = true,
            isError = hourText.isNotEmpty() && (hour == null || hour > 23),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.bodyLarge,
            color = if (minuteText.isNotEmpty() && (minute == null || minute > 59)) accent
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        OutlinedTextField(
            value = minuteText,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(2)
                if (digits.isEmpty() || (digits.toIntOrNull() ?: 0) <= 59) {
                    onChange("$hourText:$digits")
                }
            },
            singleLine = true,
            isError = minuteText.isNotEmpty() && (minute == null || minute > 59),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(72.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseDialog(
    existing: CourseEntity?,
    defaultTermStart: Long,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        teacher: String,
        location: String,
        dayOfWeek: Int,
        startPeriod: Int,
        endPeriod: Int,
        weeks: String,
        termStartMillis: Long,
        startMinutes: Int,
        endMinutes: Int,
        colorIndex: Int
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val lang = LocalLang.current
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var teacher by remember { mutableStateOf(existing?.teacher.orEmpty()) }
    var location by remember { mutableStateOf(existing?.location.orEmpty()) }
    var day by remember { mutableStateOf(existing?.dayOfWeek ?: 1) }
    var startText by remember { mutableStateOf((existing?.startPeriod ?: 1).toString()) }
    var endText by remember { mutableStateOf((existing?.endPeriod ?: 2).toString()) }
    var weeks by remember { mutableStateOf(existing?.weeks.orEmpty()) }
    var termStart by remember { mutableStateOf(existing?.termStartMillis ?: defaultTermStart) }
    var colorIndex by remember { mutableStateOf(existing?.colorIndex ?: 0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var rejected by remember { mutableStateOf(false) }
    // 上课 / 下课钟点：空串 = 没填（存 -1）。老课程和老备份读出来就是 -1，这里也就显示为空
    var startTimeText by remember { mutableStateOf(ClassSchedule.formatMinutes(existing?.startMinutes ?: -1)) }
    var endTimeText by remember { mutableStateOf(ClassSchedule.formatMinutes(existing?.endMinutes ?: -1)) }

    val start = startText.trim().toIntOrNull()?.coerceIn(1, MAX_PERIOD) ?: 1
    val end = endText.trim().toIntOrNull()?.coerceIn(start, MAX_PERIOD) ?: start

    val startMinutes = parseClock(startTimeText)
    val endMinutes = parseClock(endTimeText)
    val startFilled = startMinutes != null && startMinutes != NO_CLOCK
    val endFilled = endMinutes != null && endMinutes != NO_CLOCK
    // 空 / 写了一半 -> -1（不排提醒）；越界的数字 -> null（拦下不让保存）
    val timeInvalid = startMinutes == null || endMinutes == null
    // 只填了一头就按 -1 存另一头，不硬凑一个假的结束时间
    val timeOrderInvalid = startFilled && endFilled && endMinutes < startMinutes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) StudyStrings.coursesAddTitle(lang)
                else StudyStrings.coursesEditTitle(lang)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FieldLabel(StudyStrings.coursesName(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 30) name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.coursesTeacher(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { if (it.length <= 20) teacher = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.coursesLocation(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { if (it.length <= 24) location = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.coursesDay(lang))
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    (0..6).forEach { index ->
                        FilterChip(
                            selected = day == index + 1,
                            onClick = { day = index + 1 },
                            label = { Text(AppStrings.weekday(lang, index)) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.coursesPeriods(lang))
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = { startText = it.filter { ch -> ch.isDigit() }.take(2) },
                        singleLine = true,
                        modifier = Modifier.width(78.dp)
                    )
                    Text(
                        text = "–",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    OutlinedTextField(
                        value = endText,
                        onValueChange = { endText = it.filter { ch -> ch.isDigit() }.take(2) },
                        singleLine = true,
                        modifier = Modifier.width(78.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = periodText(lang, start, end),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ---- 上课 / 下课钟点：上课提醒就靠这两个时间 ----
                // 第几节是教学安排，换算不出钟点，所以提醒必须单独填真实时间；
                // 不填也能存，只是这门课不会有提醒（下面的小字说清楚了）。
                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.coursesBoundaryLabel(lang, start = true))
                Spacer(Modifier.height(6.dp))
                CourseTimeRow(
                    value = startTimeText,
                    onChange = { startTimeText = it },
                    onClear = { startTimeText = "" },
                    presets = START_PRESETS,
                    accent = expenseColor()
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.coursesBoundaryLabel(lang, start = false))
                Spacer(Modifier.height(6.dp))
                CourseTimeRow(
                    value = endTimeText,
                    onChange = { endTimeText = it },
                    onClear = { endTimeText = "" },
                    presets = END_PRESETS,
                    accent = expenseColor()
                )

                Spacer(Modifier.height(6.dp))
                // 填了开始时间就回显一遍范围，让用户确认排出来的提醒是几点
                if (startFilled) {
                    Text(
                        text = StudyStrings.coursesTimeRange(
                            lang,
                            ClassSchedule.formatRange(
                                startMinutes!!,
                                if (endFilled) endMinutes!! else ClassSchedule.NO_TIME
                            )
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = when {
                        timeInvalid -> StudyStrings.coursesTimeInvalid(lang)
                        timeOrderInvalid -> StudyStrings.coursesTimeOrder(lang)
                        else -> StudyStrings.coursesTimeEmptyHint(lang)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (timeInvalid || timeOrderInvalid) expenseColor()
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.coursesWeeks(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = weeks,
                    onValueChange = { if (it.length <= 24) weeks = it },
                    singleLine = true,
                    placeholder = { Text("1-16") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = StudyStrings.coursesWeeksHint(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.coursesTermStart(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = AppStrings.monthDay(
                        lang,
                        termStart.toLocalDate().monthValue,
                        termStart.toLocalDate().dayOfMonth
                    ),
                    onValueChange = { },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.coursesColor(lang))
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    (0 until COURSE_COLORS.size).forEach { index ->
                        FilterChip(
                            selected = colorIndex == index,
                            onClick = { colorIndex = index },
                            label = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(courseColor(index), Shapes.pill)
                                )
                            }
                        )
                    }
                }

                if (rejected) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = StudyStrings.coursesName(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = expenseColor()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = name.trim()
                    if (clean.isEmpty()) {
                        rejected = true
                    } else if (timeInvalid || timeOrderInvalid) {
                        // 时间不合法就不保存：存进去也排不出提醒，不如当场说清楚
                    } else {
                        onSave(
                            clean,
                            teacher.trim(),
                            location.trim(),
                            day,
                            start,
                            end,
                            weeks.trim(),
                            termStart,
                            startMinutes.toStoredMinutes(),
                            endMinutes.toStoredMinutes(),
                            colorIndex
                        )
                    }
                }
            ) { Text(AppStrings.save(lang)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(AppStrings.delete(lang)) }
                }
                TextButton(onClick = onDismiss) { Text(AppStrings.cancel(lang)) }
            }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = termStart)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // DatePicker 给的是 UTC 当天 00:00，先按 UTC 取回日期，再本地化到当天 00:00
                        pickerState.selectedDateMillis?.let {
                            termStart = Instant.ofEpochMilli(it)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toDayMillis()
                        }
                        showDatePicker = false
                    }
                ) { Text(AppStrings.confirm(lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(AppStrings.cancel(lang)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
