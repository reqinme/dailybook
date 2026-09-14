package com.dailybook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dailybook.app.CalendarDay
import com.dailybook.app.CategorySlice
import com.dailybook.app.DayBar
import com.dailybook.app.DayGroup
import com.dailybook.app.MainViewModel
import com.dailybook.app.TodoSection
import com.dailybook.app.UiState
import com.dailybook.app.data.Categories
import com.dailybook.app.data.FocusSessionEntity
import com.dailybook.app.data.TodoEntity
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.StatsStrings
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.ui.theme.incomeColor
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.formatDateHeader
import com.dailybook.app.util.formatDueLabel
import com.dailybook.app.util.formatMonthLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

// ============================================================
// 统计详情页：统计页上「点标题才打开」的整份记录列表
// ============================================================
//
// 为什么不留在统计页里：五份原始列表（本月记录 / 分类明细 / 每日明细 / 专注记录 / 待办完成情况）
// 会把统计页拖得很长，而它们平时并不需要看。现在统计页只留「标题按钮 + 汇总可视化」，
// 想看细节就点进来，这一页自己一个滚动面，空的、满的都不会挤坏别的卡片。
//
// 数据来源只有 UiState，不新增任何 ViewModel 接口。

/** 专注记录的时间点：HH:mm（与统计页上的时段明细同一套格式） */
private fun clockText(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
}

/** 占比文案：「50%」——% 不进 pickf 模板（那是格式符），所以在界面层拼好再传 */
private fun percentText(ratio: Float): String = "${(ratio * 100).roundToInt()}%"

// ============================================================
// 统计页上的入口按钮
// ============================================================

/**
 * 记录入口行：一行标题 + 右箭头，点一下推进详情页。
 *
 * 放在这个文件里是因为「入口」和「详情页」是一对：统计页只管把 kind 传进来，
 * 排版、箭头、点击范围都收在这里，两个页面不会各写一套。
 *
 * 左右留白默认 14dp，刚好和记账页的流水行对齐；
 * 放进 [SectionCard] 里时卡片自己已经有 18dp 内边距，那里传 0 就行（不然会缩进两次）。
 */
@Composable
fun StatsDetailRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 14.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// 详情页
// ============================================================

/**
 * 一种记录一份列表。
 *
 * 顶部保留年 / 月条：从统计页点进来时看到的是同一个月，
 * 想顺手看上个月的同类明细也不用退回去切。
 *
 * [nav] 这一版用不到：返回由外面顶栏的箭头和系统返回键处理（BackHandler 在 MainActivity），
 * 签名保留是为了和其它子页面一致，父级路由可以直接照抄。
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun StatsDetailScreen(
    kind: StatsDetailKind,
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        MonthYearBar(
            yearMonth = state.month,
            label = formatMonthLabel(state.month, lang),
            onPrev = vm::previousMonth,
            onNext = vm::nextMonth,
            onPick = { picked -> vm.moveToMonth(picked) },
            onToday = vm::goToCurrentMonth
        )
        Spacer(Modifier.height(6.dp))

        when (kind) {
            StatsDetailKind.MONTH_ENTRIES -> MonthEntriesList(state, lang)
            StatsDetailKind.CATEGORY_ENTRIES -> CategoryEntriesList(state, lang)
            StatsDetailKind.DAILY_ENTRIES -> DailyEntriesList(state, lang)
            StatsDetailKind.FOCUS_SESSIONS -> FocusSessionsList(state, lang)
            StatsDetailKind.TODO_SUMMARY -> TodoSummaryList(state, lang)
        }
    }
}

// ---- 本月记录 ----

/**
 * 一个月的全部流水，按日期从近到远。
 *
 * 用记账页同一套流水行排版（分类徽标、账户、备注、标签、金额），只是这里只读：
 * 不改数据、不给删除按钮，避免在统计里误删。日期分组用记账页的日期标题（今天 / 昨天 / 9月13日 周六）。
 */
@Composable
private fun MonthEntriesList(state: UiState, lang: Lang) {
    val days = state.monthGroups.map { group ->
        group.copy(items = group.items.sortedByDescending { it.createdAt })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "month-summary") {
            SectionCard {
                Text(
                    text = StatsStrings.monthEntriesSummary(
                        lang,
                        state.monthCount,
                        formatAmount(state.monthExpense),
                        formatAmount(state.monthIncome)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (days.isEmpty()) {
            item(key = "empty") {
                EmptyBlock(
                    emoji = "🧾",
                    title = StatsStrings.monthEntriesEmpty(lang),
                    subtitle = StatsStrings.monthEntriesEmptyHint(lang)
                )
            }
        } else {
            days.forEach { group ->
                item(key = "day-${group.date}") { DayHeader(group) }
                items(items = group.items, key = { it.id }) { tx ->
                    TransactionRow(tx = tx, readOnly = true)
                }
            }
        }
    }
}

// ---- 分类明细 ----

/**
 * 分类维度的明细：一行一个分类，笔数、金额、占比（进度条）。
 *
 * 笔数从 [UiState.monthGroups] 里数出来（同一份月份数据，和占比的口径一致）；
 * 支出按支出总额算占比、收入按收入总额算，进度条各自用各自的总额当基准。
 */
@Composable
private fun CategoryEntriesList(state: UiState, lang: Lang) {
    val countOf = state.monthGroups
        .flatMap { it.items }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        if (state.expenseSlices.isEmpty()) {
            item(key = "empty") {
                EmptyBlock(
                    emoji = "💸",
                    title = StatsStrings.categoryEntriesEmpty(lang),
                    subtitle = StatsStrings.monthEntriesEmptyHint(lang)
                )
            }
        } else {
            item(key = "expense-title") {
                SectionCard(title = StatsStrings.expenseCategoryTitle(lang)) {
                    state.expenseSlices.forEachIndexed { index, slice ->
                        if (index > 0) Spacer(Modifier.height(14.dp))
                        CategoryDetailRow(slice, countOf[slice.category] ?: 0, expenseColor(), lang)
                    }
                }
            }
        }

        if (state.incomeSlices.isNotEmpty()) {
            item(key = "income-title") {
                Spacer(Modifier.height(14.dp))
                SectionCard(title = StatsStrings.incomeByCategoryTitle(lang)) {
                    state.incomeSlices.forEachIndexed { index, slice ->
                        if (index > 0) Spacer(Modifier.height(14.dp))
                        CategoryDetailRow(slice, countOf[slice.category] ?: 0, incomeColor(), lang)
                    }
                }
            }
        }
    }
}

/** 分类明细的一行：分类名 + 笔数 + 金额 + 占比，下面一根进度条 */
@Composable
private fun CategoryDetailRow(
    slice: CategorySlice,
    count: Int,
    color: Color,
    lang: Lang
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Categories.emojiOf(slice.category),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = slice.category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "¥${formatAmount(slice.cents)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    maxLines = 1
                )
                Text(
                    text = "${StatsStrings.categoryEntryCount(lang, count)} · " +
                        StatsStrings.categoryShare(lang, percentText(slice.ratio)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        ThinProgressBar(ratio = slice.ratio, color = color, height = 6.dp)
    }
}

// ---- 每日明细 ----

/** 一天一行：笔数、支出、收入、净额，按日期从近到远 */
@Composable
private fun DailyEntriesList(state: UiState, lang: Lang) {
    val days = dailyRows(state)
    // 只要有任意一天有记录（笔数 / 收支）就出整表，空白天照样一行行列出来，
    // 这样「哪几天断了」一眼能看出来；整月一条记录都没有才走空态。
    val hasAny = days.any { it.count > 0 || it.expense > 0L || it.income > 0L }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        if (!hasAny) {
            item(key = "empty") {
                EmptyBlock(
                    emoji = "📅",
                    title = StatsStrings.dailyEntriesEmpty(lang),
                    subtitle = StatsStrings.monthEntriesEmptyHint(lang)
                )
            }
        } else {
            items(items = days, key = { it.date.toString() }) { row ->
                DailyDetailRow(row, lang)
            }
        }
    }
}

/** 每日明细里的一行数据 */
private data class DailyRow(
    val date: LocalDate,
    val count: Int,
    val expense: Long,
    val income: Long
) {
    val net: Long get() = income - expense
}

/**
 * 把「本月每一天」拼出来。
 *
 * 日期清单以 [UiState.calendarDays] 为准（当月每天一条，含没有记录的空白天），
 * 它和 [UiState.dayBars] 一样是**整月口径**（不受记账页的搜索 / 账户 / 标签 / 某天筛选影响），
 * 所以空白天、整月合计都不会被筛掉。
 * 当天有流水时再用 [UiState.monthGroups] 补上更细的笔数与收入；那份列表带筛选，
 * 所以只在对应日期确实存在时采用，取不到就退回整月口径的日历数据。
 */
private fun dailyRows(state: UiState): List<DailyRow> {
    val groups = state.monthGroups.associateBy { it.date }
    val bars = state.dayBars.associateBy { it.day }
    val calendars: List<CalendarDay> = state.calendarDays.ifEmpty {
        (1..state.month.lengthOfMonth()).map { day ->
            val date = state.month.atDay(day)
            CalendarDay(
                date = date,
                expenseCents = bars[day]?.cents ?: 0L,
                incomeCents = 0L,
                count = 0
            )
        }
    }

    return calendars
        .map { day ->
            val group: DayGroup? = groups[day.date]
            val bar: DayBar? = bars[day.date.dayOfMonth]
            DailyRow(
                date = day.date,
                count = group?.items?.size ?: day.count,
                expense = group?.expense ?: bar?.cents ?: day.expenseCents,
                income = group?.income ?: day.incomeCents
            )
        }
        .sortedByDescending { it.date }
}

@Composable
private fun DailyDetailRow(row: DailyRow, lang: Lang) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surface, Shapes.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = formatDateHeader(row.date, lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = StatsStrings.dayEntrySummary(
                    lang,
                    AppStrings.monthDay(lang, row.date.monthValue, row.date.dayOfMonth),
                    row.count,
                    formatAmount(row.expense)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            val netColor = if (row.net < 0L) expenseColor() else incomeColor()
            Text(
                text = StatsStrings.dayNetLine(lang, formatAmount(row.net)),
                style = MaterialTheme.typography.titleMedium,
                color = netColor,
                maxLines = 1
            )
            if (row.income > 0L) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = StatsStrings.dayIncomeLine(lang, formatAmount(row.income)),
                    style = MaterialTheme.typography.bodySmall,
                    color = incomeColor(),
                    maxLines = 1
                )
            }
        }
    }
}

// ---- 专注记录 ----

/**
 * 专注记录：本月汇总 + 今天的逐条明细 + 按待办汇总的投入时间。
 *
 * 能给的都给了：次数、总时长、平均每次、按待办的投入、今天每条时段（含「中断」标记）。
 * 逐日的全部明细要读专注记录表，而 UiState 只开放了「本月汇总」和「今天的记录」，
 * 所以这里不硬编每一天的数字，用 [StatsStrings.focusSessionsScopeNote] 说清口径。
 */
@Composable
private fun FocusSessionsList(state: UiState, lang: Lang) {
    val focus = state.focusStats
    val average = if (focus.monthCount <= 0) 0 else focus.monthMinutes / focus.monthCount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "focus-summary") {
            SectionCard {
                Text(
                    text = StatsStrings.focusSessionCount(lang, focus.monthCount, focus.monthMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (focus.monthCount > 0) {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) {
                        StatBlock(
                            label = StatsStrings.focusToday(lang),
                            value = focus.todayCount.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatBlock(
                            label = StatsStrings.focusWeekCount(lang),
                            value = focus.weekCount.toString(),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        StatBlock(
                            label = StatsStrings.focusMonthDuration(lang),
                            value = StatsStrings.focusMinutesShort(lang, average),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (focus.monthCount <= 0) {
            item(key = "empty") {
                EmptyBlock(
                    emoji = "🍅",
                    title = StatsStrings.focusSessionsEmpty(lang),
                    subtitle = StatsStrings.focusSessionsEmptyHint(lang)
                )
            }
        } else {
            if (focus.todaySessions.isNotEmpty()) {
                item(key = "today-sessions") {
                    Spacer(Modifier.height(14.dp))
                    SectionCard(title = StatsStrings.todayDetailTitle(lang)) {
                        focus.todaySessions.forEachIndexed { index, session ->
                            if (index > 0) Spacer(Modifier.height(10.dp))
                            FocusSessionRow(session, lang)
                        }
                    }
                }
            }

            if (focus.perTodo.isNotEmpty()) {
                item(key = "per-todo") {
                    Spacer(Modifier.height(14.dp))
                    SectionCard(title = StatsStrings.perTodoTitle(lang)) {
                        focus.perTodo.forEachIndexed { index, item ->
                            if (index > 0) Spacer(Modifier.height(12.dp))
                            Column(Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "🎯 ${item.title}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = StatsStrings.perTodoMinutes(lang, item.minutes, item.count),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                ThinProgressBar(
                                    ratio = item.minutes.toFloat() /
                                        (focus.perTodo.first().minutes.coerceAtLeast(1)).toFloat(),
                                    color = MaterialTheme.colorScheme.secondary,
                                    height = 6.dp
                                )
                            }
                        }
                    }
                }
            }

            item(key = "scope-note") {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = StatsStrings.focusSessionsScopeNote(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 一条专注记录：起止时间、分钟数、中断标记、关联的待办 */
@Composable
private fun FocusSessionRow(session: FocusSessionEntity, lang: Lang) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${clockText(session.startedAtMillis)} - ${clockText(session.endedAtMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = StatsStrings.sessionMinutes(lang, session.minutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                // 中途停止 / 跳过的记录挂一个淡色小标签，正常走完的什么都不加
                if (session.interrupted) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = StatsStrings.sessionInterrupted(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, Shapes.badge)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
        if (session.taskTitle.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "🎯 ${session.taskTitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---- 待办完成情况 ----

/**
 * 待办完成情况：还没完成的 / 已经完成的分两组。
 *
 * 数据取 [UiState.kanbanSections]（里面是**全部**待办，不受待办页当前的筛选影响），
 * 所以统计页的「完成情况」不会因为待办页正筛着「已完成」而少算。
 */
@Composable
private fun TodoSummaryList(state: UiState, lang: Lang) {
    // 口径以 kanbanSections 为准：它装的是**全部**待办（不受待办页当前筛选影响），
    // 计数与下面两张名单都从同一份数据里数，不会出现「写着 3 条、下面只列 1 条」。
    val all = state.kanbanSections.flatMap { it.items }
    val pending = sectionItems(state.kanbanSections, done = false)
    val done = sectionItems(state.kanbanSections, done = true)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "todo-summary") {
            SectionCard {
                Row(Modifier.fillMaxWidth()) {
                    StatBlock(
                        label = AppStrings.filterPending(lang),
                        value = pending.size.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatBlock(
                        label = AppStrings.filterDone(lang),
                        value = done.size.toString(),
                        color = incomeColor(),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (all.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = StatsStrings.todoSummaryEmpty(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = StatsStrings.todoCountLine(lang, AppStrings.filterPending(lang), pending.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = StatsStrings.todoCountLine(lang, AppStrings.filterDone(lang), done.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (all.isEmpty()) {
            item(key = "empty") {
                EmptyBlock(
                    emoji = "✅",
                    title = StatsStrings.todoSummaryEmpty(lang),
                    subtitle = StatsStrings.todoSummaryEmptyHint(lang)
                )
            }
        } else {
            item(key = "pending-title") {
                Spacer(Modifier.height(14.dp))
                SectionCard(title = StatsStrings.todoPendingTitle(lang)) {
                    if (pending.isEmpty()) {
                        Text(
                            text = StatsStrings.todoCountLine(
                                lang,
                                AppStrings.filterPending(lang),
                                0
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        pending.forEachIndexed { index, todo ->
                            if (index > 0) Spacer(Modifier.height(10.dp))
                            TodoSummaryRow(todo, lang)
                        }
                    }
                }
            }

            item(key = "done-title") {
                Spacer(Modifier.height(14.dp))
                SectionCard(title = StatsStrings.todoDoneTitle(lang)) {
                    if (done.isEmpty()) {
                        Text(
                            text = StatsStrings.todoCountLine(
                                lang,
                                AppStrings.filterDone(lang),
                                0
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        done.forEachIndexed { index, todo ->
                            if (index > 0) Spacer(Modifier.height(10.dp))
                            TodoSummaryRow(todo, lang)
                        }
                    }
                }
            }
        }
    }
}

/** 看板分组里筛出「未完成」或「已完成」的那些，保持原来的排序（重要的在前） */
private fun sectionItems(sections: List<TodoSection>, done: Boolean): List<TodoEntity> =
    sections.flatMap { section -> section.items.filter { it.done == done } }

/** 待办的一行：状态圆点（完成了是实心，没完成是空心）+ 标题 + 到期 / 重复 / 重要 */
@Composable
private fun TodoSummaryRow(todo: TodoEntity, lang: Lang) {
    val accent = if (todo.done) incomeColor() else MaterialTheme.colorScheme.primary
    val dotColor = if (todo.done) accent else Color.Transparent

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(dotColor, CircleShape)
                .border(1.5.dp, accent, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = todoMeta(todo, lang)
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // 优先级符号是数据（▽ / ▲ / 🔥 之类），不是界面文案
        val mark = todo.priorityLevel.emoji()
        if (mark.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = mark,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 待办行的副标题：到期日 · 重复规则 · 重要标记，都没有就是空串 */
@Composable
private fun todoMeta(todo: TodoEntity, lang: Lang): String = buildString {
    val due = todo.dueMillis
    if (due != null) append(formatDueLabel(due, lang))
    if (todo.repeats) {
        if (isNotEmpty()) append(" · ")
        append(todo.repeat.label(lang))
    }
    if (todo.important) {
        if (isNotEmpty()) append(" · ")
        append(AppStrings.priorityHigh(lang))
    }
}

// ---- 共用：空态 ----

/**
 * 详情页的空态：上方是年 / 月条，下面留一块比较高的居中区域，
 * 不然空列表看起来像「页面没加载出来」。
 */
@Composable
private fun EmptyBlock(emoji: String, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = emoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
