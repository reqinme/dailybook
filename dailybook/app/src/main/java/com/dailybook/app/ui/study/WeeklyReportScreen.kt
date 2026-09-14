package com.dailybook.app.ui.study

import android.content.Context
import com.dailybook.app.ui.theme.incomeColor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.R
import com.dailybook.app.data.HabitEntity
import com.dailybook.app.data.TxType
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.StatsStrings
import com.dailybook.app.i18n.StudyStrings
import com.dailybook.app.ui.EmptyHint
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.SectionCard
import com.dailybook.app.ui.Shapes
import com.dailybook.app.ui.ThinProgressBar
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 学习周报（只读）。
 *
 * 全部数据都来自 [UiState] 里已经有的东西，这一页不查库、不重新统计：
 * - 专注时长 / 次数：`focusStats`（次数取 `focusStats.recentDays`，分钟数从
 *   `focusStats.monthSessions` 里按同一个窗口 today-6 … today 自己汇总；
 *   **口径说明**：两者都和下面七根柱子出自同一段窗口，所以不会出现「总数非 0 而柱子全空」，
 *   但这 7 天要是跨了月，跨月的那几天不在 `monthSessions` 里，只能按 0 算（宁可少算不虚报），
 *   这句限制写在界面上「口径说明」那张卡片里）；
 * - 完成的待办：只用 `todos.count { it.done }`。
 *   **口径说明**：待办表里没有存「完成日期」，所以这里给的是「目前已完成的总条数」，
 *   不是「这周完成的条数」——宁可说清口径，也不假装是周数据；
 * - 背单词打卡：`wordHabits`（生活模块里单位不是「次」的定量习惯，也就是背单词 / 背书计划）
 *   加上 `habitLogs` 中落在最近 7 天的打卡记录数，明细在「单词」页面里看；
 * - 支出：`transactions` 里日期落在最近 7 天的记录（按记账日期，不是创建时间）；
 * - 作业逾期数：`overdueAssignments`。
 *
 * 「导出图片」把上面这些画成一张 1080px 宽的 PNG（Canvas + Paint，纯系统 API，
 * 不引任何图表 / 图片库），再走系统文件选择器（SAF）落盘 —— 不需要存储权限。
 */

private val WEEKLY_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd", Locale.ROOT)
private val WEEKLY_FILE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT)

/** 近 7 天的一天：界面上画柱子，导出图里也用它 */
private data class WeeklyDay(
    val date: LocalDate,
    val focusMinutes: Int,
    val focusCount: Int,
    val expenseCents: Long
)

/** 周报的全部数字，界面与导出图共用同一份 */
private data class WeeklyReportData(
    val start: LocalDate,
    val end: LocalDate,
    val today: LocalDate,
    val focusMinutes: Int,
    val focusCount: Int,
    val todoTotal: Int,
    val todoDone: Int,
    val wordPlans: List<HabitEntity>,
    /** 背单词计划近 7 天的打卡次数（habit_logs 里落在这 7 天的记录数） */
    val wordChecks: Int,
    val expenseCents: Long,
    val overdue: Int,
    val days: List<WeeklyDay>,
    val maxFocusMinutes: Int,
    val maxExpenseCents: Long
)

@Composable
fun WeeklyReportScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val report = remember(state) { weeklyReportData(state) }
    var exporting by remember { mutableStateOf(false) }
    // 点按钮时把这一份快照存下来：SAF 回调不在同一次重组里，不能依赖之后的状态
    var pendingExport by remember { mutableStateOf<WeeklyReportData?>(null) }

    val pngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val data = pendingExport ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        exporting = true
        scope.launch {
            // 生成 + 写盘都在 IO 线程；成功失败各自如实提示，失败不谎报成功
            val failure = withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = renderWeeklyBitmap(data, lang, AppStrings.appName(lang))
                    try {
                        writePng(context, uri, bitmap)
                    } finally {
                        // 1080×N 的位图，写完立刻回收
                        bitmap.recycle()
                    }
                }.exceptionOrNull()
            }
            exporting = false
            val text = if (failure == null) {
                StudyStrings.weeklyExportDone(lang)
            } else {
                StudyStrings.weeklyExportFailed(
                    lang,
                    failure.message ?: failure.javaClass.simpleName
                )
            }
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }

    /** 没有数据也允许导出（周报本来就是「这周啥也没干」的诚实版本） */
    fun startExport() {
        pendingExport = report
        val fileName = StudyStrings.weeklyFileName(
            lang,
            context.getString(R.string.app_name),
            report.end.format(WEEKLY_FILE_FORMAT)
        ) + ".png"
        pngLauncher.launch(fileName)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = StudyStrings.weeklyTitle(lang),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = StudyStrings.weeklyRange(
                lang,
                report.start.format(WEEKLY_DATE_FORMAT),
                report.end.format(WEEKLY_DATE_FORMAT)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { if (!exporting) startExport() },
            enabled = !exporting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(StudyStrings.weeklyExportImage(lang))
        }

        // ---- 专注 ----
        Spacer(Modifier.height(14.dp))
        SectionCard(title = StudyStrings.weeklyFocus(lang)) {
            Text(
                text = StudyStrings.weeklyFocusValue(lang, report.focusMinutes, report.focusCount),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = StudyStrings.weeklyDailyFocus(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // 口径说明：上面的数字和下面七根柱子是同一段窗口（today-6 … today）的同一份数据，
            // 这句只交代跨月那几天按 0 算的限制，免得用户拿它和「本月」的数字对不上时以为算错了
            Spacer(Modifier.height(2.dp))
            Text(
                text = StatsStrings.weeklyFocusScopeNote(lang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            FocusBarChart(days = report.days, maxMinutes = report.maxFocusMinutes)
        }

        // ---- 待办 / 背单词 ----
        Spacer(Modifier.height(14.dp))
        SectionCard(title = StudyStrings.weeklyTodo(lang)) {
            Text(
                text = StudyStrings.weeklyTodoValue(lang, report.todoDone, report.todoTotal),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            ThinProgressBar(
                ratio = if (report.todoTotal <= 0) 0f
                else report.todoDone.toFloat() / report.todoTotal.toFloat(),
                color = incomeColor()
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = StudyStrings.weeklyWords(lang)) {
            Text(
                text = StudyStrings.weeklyWordsValue(lang, report.wordPlans.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = StudyStrings.weeklyWordChecks(lang, report.wordChecks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            report.wordPlans.take(4).forEach { habit ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habit.emoji,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${habit.targetPerDay} ${habit.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---- 支出 ----
        Spacer(Modifier.height(14.dp))
        SectionCard(title = StudyStrings.weeklySpend(lang)) {
            Text(
                text = StudyStrings.weeklySpendValue(lang, formatAmount(report.expenseCents)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = StudyStrings.weeklyDailySpend(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            ExpenseBarChart(days = report.days, maxCents = report.maxExpenseCents)
        }

        // ---- 作业 ----
        Spacer(Modifier.height(14.dp))
        SectionCard(title = StudyStrings.weeklyAssignments(lang)) {
            if (report.overdue > 0) {
                Text(
                    text = StudyStrings.weeklyOverdueValue(lang, report.overdue),
                    style = MaterialTheme.typography.titleMedium,
                    color = expenseColor()
                )
            } else {
                Text(
                    text = StudyStrings.weeklyNoOverdue(lang),
                    style = MaterialTheme.typography.titleMedium,
                    color = incomeColor()
                )
            }
        }

        // ---- 口径说明 ----
        Spacer(Modifier.height(14.dp))
        SectionCard {
            Text(
                text = StudyStrings.weeklyNote(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (report.focusMinutes == 0 && report.expenseCents == 0L && report.todoTotal == 0) {
            Spacer(Modifier.height(16.dp))
            EmptyHint(emoji = "🗓", title = StudyStrings.weeklyEmpty(lang))
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ============================================================
// 数据拼装
// ============================================================

/**
 * 把 [UiState] 里已有的数据整理成周报。
 * 近 7 天 = 含今天在内的 7 天，所以起止是 today-6 … today。
 */
private fun weeklyReportData(state: UiState, today: LocalDate = LocalDate.now()): WeeklyReportData {
    val start = today.minusDays(6)

    // 专注：focusStats.recentDays 就是最近 7 天（含今天）的次数，这里换算成分钟与次数
    val focusByDay = state.focusStats.recentDays.associate { it.date to it.count }
    // 分钟数从选中所属月份的全部记录里按同一窗口（today-6 … today）数出来。
    // 不用 fallback 了：以前 barMinutes 为 0 时会退回 focusStats.weekMinutes（周一至今），
    // 于是「周一刚过、上周三才专注过」的那天会印出一个非 0 的近 7 天专注时长，
    // 而下面七根柱子全是空的（导出的 PNG 也带着同一个虚高的数字）。
    // 现在这个数字和柱状图出自同一份数据、同一段窗口，两者不可能不一致。
    // 用「最近 7 天」这一份，**不能用 monthSessions**：后者锚在记账/统计页选中的月份上，
    // 用户把选中月切走之后这 7 天会突然变成 0，而同一页的柱状图（来自热力图）还有数据。
    // last7Sessions 就是 today-6 … today 这一段的全部记录，和声明完全一致，跨月也不会漏。
    val last7Sessions = state.focusStats.last7Sessions
    val daySessions = last7Sessions.filter { session ->
        val date = session.startedAtMillis.toLocalDate()
        !date.isBefore(start) && !date.isAfter(today)
    }
    val minutesByDay = daySessions
        .groupBy { it.startedAtMillis.toLocalDate() }
        .mapValues { entry -> entry.value.sumOf { it.minutes } }

    // 支出：按记账日期落在最近 7 天的支出（不含收入）
    val expenseByDay = state.transactions
        .filter { it.type == TxType.EXPENSE }
        .filter { !it.dateMillis.toLocalDate().isBefore(start) && !it.dateMillis.toLocalDate().isAfter(today) }
        .groupBy { it.dateMillis.toLocalDate() }
        .mapValues { entry -> entry.value.sumOf { it.amountCents } }
    val expenseTotal = expenseByDay.values.sum()

    val days = (0..6).map { offset ->
        val date = start.plusDays(offset.toLong())
        WeeklyDay(
            date = date,
            focusMinutes = minutesByDay[date] ?: 0,
            focusCount = focusByDay[date] ?: 0,
            expenseCents = expenseByDay[date] ?: 0L
        )
    }
    // 两个数都从上面这 7 天里数：和柱状图同一份数据，所以「总数非 0 但柱子全空」不会再出现
    val focusMinutes = days.sumOf { it.focusMinutes }
    val focusCount = days.sumOf { it.focusCount }

    // 背单词打卡：只算「定量习惯」（wordHabits，单位不是「次」的那些）在最近 7 天的打卡记录数
    val wordIds = state.wordHabits.map { it.id }.toSet()
    val wordChecks = state.habitLogs.count { log ->
        log.habitId in wordIds &&
            !log.dateMillis.toLocalDate().isBefore(start) &&
            !log.dateMillis.toLocalDate().isAfter(today) &&
            log.count > 0
    }

    return WeeklyReportData(
        start = start,
        end = today,
        today = today,
        focusMinutes = focusMinutes,
        focusCount = focusCount,
        // 待办表没存完成日期，所以这里只能是「当前已完成的总条数」，界面上也这么写
        todoTotal = state.todos.size,
        todoDone = state.todos.count { it.done },
        wordPlans = state.wordHabits,
        wordChecks = wordChecks,
        expenseCents = expenseTotal,
        overdue = state.overdueAssignments,
        days = days,
        maxFocusMinutes = days.maxOfOrNull { it.focusMinutes } ?: 0,
        maxExpenseCents = days.maxOfOrNull { it.expenseCents } ?: 0L
    )
}

// ============================================================
// 界面上的柱状图（纯 Compose 排版，不引图表库）
// ============================================================

/** 近 7 天专注分钟：每根柱子的高度按最大值等比 */
@Composable
private fun FocusBarChart(days: List<WeeklyDay>, maxMinutes: Int) {
    val lang = LocalLang.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val ratio = if (maxMinutes <= 0) 0f
            else day.focusMinutes.toFloat() / maxMinutes.toFloat()
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (day.focusMinutes > 0) day.focusMinutes.toString() else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, Shapes.pill),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((64.dp * ratio).coerceAtLeast(if (day.focusMinutes > 0) 3.dp else 0.dp))
                            .background(
                                if (day.date == LocalDate.now()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                Shapes.pill
                            )
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = day.date.format(WEEKLY_DATE_FORMAT),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (day.date == LocalDate.now()) {
                    Text(
                        text = StudyStrings.coursesTodayMark(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** 近 7 天支出：柱子用导出图那套固定支出色（导出的 PNG 不跟随主题，界面这里保持一致） */
@Composable
private fun ExpenseBarChart(days: List<WeeklyDay>, maxCents: Long) {
    val lang = LocalLang.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val ratio = if (maxCents <= 0L) 0f
            else day.expenseCents.toFloat() / maxCents.toFloat()
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (day.expenseCents > 0L) formatAmount(day.expenseCents) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, Shapes.pill),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((64.dp * ratio).coerceAtLeast(if (day.expenseCents > 0L) 3.dp else 0.dp))
                            .background(expenseColor(), Shapes.pill)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = day.date.format(WEEKLY_DATE_FORMAT),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (day.date == LocalDate.now()) {
                    Text(
                        text = StudyStrings.coursesTodayMark(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ============================================================
// 导出图片（Canvas + Paint + SAF，全部自包含在本文件里）
// ============================================================

// 导出图的固定配色：和「月报导出」一样用一套不跟随主题的品牌色，
// 这样深色模式导出来的图也不会变成黑底白字。
private const val PNG_BG = "#FFFFFF"
private const val PNG_TEXT = "#1B1C1F"
private const val PNG_DIM = "#5C6069"
private const val PNG_BRAND = "#2AA79B"
private const val PNG_BAR_BG = "#EEF1F5"
private const val PNG_EXPENSE = "#D1453B"
private const val PNG_FOCUS = "#5B8DEF"

private const val PNG_WIDTH = 1080
private const val PNG_PADDING = 56f

/** 导出图里的一行：标题 / 副标题 / 一行数字 / 小标题 / 一根柱条 / 页脚 */
private sealed interface ReportRow {
    data class Title(val text: String) : ReportRow
    data class Subtitle(val text: String) : ReportRow
    data class Figure(val label: String, val value: String, val color: String?) : ReportRow
    data class Section(val text: String) : ReportRow
    data class Bar(val label: String, val value: String, val ratio: Float, val color: String) : ReportRow
    data class Footer(val text: String) : ReportRow
}

/**
 * 把周报画成一张 1080px 宽的 PNG。
 *
 * 两步走：先按字号量一遍每行占多高（顺便把长行折行），算准总高度再分配位图 ——
 * 一次性分配 + 一次绘制，不会画到一半发现高度不够。
 */
private fun renderWeeklyBitmap(
    data: WeeklyReportData,
    lang: Lang,
    appName: String
): Bitmap {
    val scale = PNG_WIDTH / 1080f
    fun px(value: Float) = value * scale

    val pad = px(PNG_PADDING)
    val wrapAt = 32
    val rows = buildList {
        add(ReportRow.Title(StudyStrings.weeklyTitle(lang)))
        add(
            ReportRow.Subtitle(
                StudyStrings.weeklyRange(
                    lang,
                    data.start.format(WEEKLY_DATE_FORMAT),
                    data.end.format(WEEKLY_DATE_FORMAT)
                )
            )
        )
        add(
            ReportRow.Figure(
                StudyStrings.weeklyFocus(lang),
                StudyStrings.weeklyFocusValue(lang, data.focusMinutes, data.focusCount),
                PNG_FOCUS
            )
        )
        add(
            ReportRow.Figure(
                StudyStrings.weeklyTodo(lang),
                StudyStrings.weeklyTodoValue(lang, data.todoDone, data.todoTotal),
                null
            )
        )
        add(
            ReportRow.Figure(
                StudyStrings.weeklyWords(lang),
                StudyStrings.weeklyWordsValue(lang, data.wordPlans.size) + " · " +
                    StudyStrings.weeklyWordChecks(lang, data.wordChecks),
                null
            )
        )
        add(
            ReportRow.Figure(
                StudyStrings.weeklySpend(lang),
                StudyStrings.weeklySpendValue(lang, formatAmount(data.expenseCents)),
                PNG_EXPENSE
            )
        )
        add(
            ReportRow.Figure(
                StudyStrings.weeklyAssignments(lang),
                if (data.overdue > 0) StudyStrings.weeklyOverdueValue(lang, data.overdue)
                else StudyStrings.weeklyNoOverdue(lang),
                if (data.overdue > 0) PNG_EXPENSE else null
            )
        )

        add(ReportRow.Section(StudyStrings.weeklyDailyFocus(lang)))
        data.days.forEach { day ->
            add(
                ReportRow.Bar(
                    label = day.date.format(WEEKLY_DATE_FORMAT),
                    value = StudyStrings.weeklyMinuteValue(lang, day.focusMinutes),
                    ratio = if (data.maxFocusMinutes <= 0) 0f
                    else day.focusMinutes.toFloat() / data.maxFocusMinutes.toFloat(),
                    color = PNG_FOCUS
                )
            )
        }

        add(ReportRow.Section(StudyStrings.weeklyDailySpend(lang)))
        data.days.forEach { day ->
            add(
                ReportRow.Bar(
                    label = day.date.format(WEEKLY_DATE_FORMAT),
                    value = formatAmount(day.expenseCents),
                    ratio = if (data.maxExpenseCents <= 0L) 0f
                    else day.expenseCents.toFloat() / data.maxExpenseCents.toFloat(),
                    color = PNG_EXPENSE
                )
            )
        }

        wrap(StudyStrings.weeklyNote(lang), wrapAt).forEach { add(ReportRow.Subtitle(it)) }
        add(ReportRow.Footer(StudyStrings.weeklyImageFooter(lang, appName)))
    }

    val titleSize = px(44f)
    val sectionSize = px(28f)
    val bodySize = px(27f)
    val figureSize = px(32f)
    val barHeight = px(22f)

    val titlePaint = textPaint(titleSize, PNG_TEXT, bold = true)
    val sectionPaint = textPaint(sectionSize, PNG_BRAND, bold = true)
    val bodyPaint = textPaint(bodySize, PNG_TEXT)
    val dimPaint = textPaint(bodySize, PNG_DIM)
    val figurePaint = textPaint(figureSize, PNG_TEXT, bold = true)

    // 第一遍：量高度
    var contentHeight = 0f
    rows.forEach { row ->
        contentHeight += when (row) {
            is ReportRow.Title -> titleSize * 1.7f
            is ReportRow.Subtitle -> bodySize * 1.5f
            is ReportRow.Figure -> figureSize * 1.9f
            is ReportRow.Section -> sectionSize * 1.9f
            is ReportRow.Bar -> barHeight * 1.9f
            is ReportRow.Footer -> bodySize * 2.2f
        }
    }

    val height = (pad * 2f + contentHeight).toInt().coerceAtLeast(400)
    val bitmap = Bitmap.createBitmap(PNG_WIDTH, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.parseColor(PNG_BG))

    // 顶部品牌条 + 柱条底色：各一个 Paint，别在循环里反复 new
    canvas.drawRect(0f, 0f, PNG_WIDTH.toFloat(), px(10f), fillPaint(PNG_BRAND))
    val barBg = fillPaint(PNG_BAR_BG)

    // 第二遍：真画
    var y = pad
    rows.forEach { row ->
        when (row) {
            is ReportRow.Title -> {
                canvas.drawText(row.text, pad, y + titleSize, titlePaint)
                y += titleSize * 1.7f
            }

            is ReportRow.Subtitle -> {
                canvas.drawText(row.text, pad, y + bodySize, dimPaint)
                y += bodySize * 1.5f
            }

            is ReportRow.Figure -> {
                canvas.drawText(row.label, pad, y + bodySize, dimPaint)
                val paint = if (row.color == null) figurePaint else {
                    textPaint(figureSize, row.color, bold = true)
                }
                canvas.drawText(row.value, pad, y + bodySize * 1.05f + figureSize, paint)
                y += figureSize * 1.9f
            }

            is ReportRow.Section -> {
                canvas.drawText(row.text, pad, y + sectionSize, sectionPaint)
                y += sectionSize * 1.9f
            }

            is ReportRow.Bar -> {
                val labelWidth = px(120f)
                val valueWidth = px(190f)
                val trackLeft = pad + labelWidth
                val trackRight = PNG_WIDTH - pad - valueWidth
                val top = y + barHeight * 0.2f
                canvas.drawText(row.label, pad, y + barHeight * 0.85f, bodyPaint)
                canvas.drawRoundRect(
                    trackLeft,
                    top,
                    trackRight,
                    top + barHeight,
                    barHeight / 2f,
                    barHeight / 2f,
                    barBg
                )
                val filled = (trackRight - trackLeft) * row.ratio.coerceIn(0f, 1f)
                if (filled > 0f) {
                    canvas.drawRoundRect(
                        trackLeft,
                        top,
                        trackLeft + filled,
                        top + barHeight,
                        barHeight / 2f,
                        barHeight / 2f,
                        fillPaint(row.color)
                    )
                }
                canvas.drawText(row.value, trackRight + px(16f), y + barHeight * 0.85f, dimPaint)
                y += barHeight * 1.9f
            }

            is ReportRow.Footer -> {
                y += bodySize * 0.6f
                canvas.drawText(row.text, pad, y, dimPaint)
                y += bodySize * 1.6f
            }
        }
    }

    return bitmap
}

/** 把 [bitmap] 写成 PNG；失败时抛出，由调用方收成提示语 */
private fun writePng(context: Context, uri: Uri, bitmap: Bitmap) {
    val stream = context.contentResolver.openOutputStream(uri, "wt")
        ?: throw IllegalStateException("openOutputStream returned null")
    stream.use { output ->
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            throw IllegalStateException("bitmap.compress failed")
        }
    }
}

private fun textPaint(size: Float, colorHex: String, bold: Boolean = false) = Paint().apply {
    isAntiAlias = true
    this.color = Color.parseColor(colorHex)
    textSize = size
    typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
}

private fun fillPaint(colorHex: String) = Paint().apply {
    isAntiAlias = true
    color = Color.parseColor(colorHex)
    style = Paint.Style.FILL
}

/**
 * 按字符数折行。
 * 导出图是固定宽度、字号也固定，只要按「一行放多少字符」粗切就够用；
 * 中英混排的最坏情况是略短或略长一点，不会溢出画布（左右都留了 padding）。
 */
private fun wrap(text: String, maxChars: Int): List<String> {
    if (text.length <= maxChars) return listOf(text)
    val lines = mutableListOf<String>()
    var rest = text
    while (rest.length > maxChars) {
        // 优先在空格处断，找不到就硬切
        val window = rest.take(maxChars)
        val breakAt = window.lastIndexOf(' ').takeIf { it > maxChars / 2 } ?: maxChars
        lines += rest.take(breakAt).trim()
        rest = rest.drop(breakAt).trim()
    }
    if (rest.isNotEmpty()) lines += rest
    return lines
}
