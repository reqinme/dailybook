package com.dailybook.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailybook.app.AccountSlice
import com.dailybook.app.CategoryBudgetRow
import com.dailybook.app.CategorySlice
import com.dailybook.app.Comparison
import com.dailybook.app.DayBar
import com.dailybook.app.FocusDay
import com.dailybook.app.HeatCell
import com.dailybook.app.Insight
import com.dailybook.app.InsightKind
import com.dailybook.app.MainViewModel
import com.dailybook.app.MonthBar
import com.dailybook.app.R
import com.dailybook.app.UiState
import com.dailybook.app.data.Accounts
import com.dailybook.app.data.Categories
import com.dailybook.app.data.FocusSessionEntity
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.StatsStrings
import com.dailybook.app.report.CategoryData
import com.dailybook.app.report.CompareData
import com.dailybook.app.report.DailyBarData
import com.dailybook.app.report.MonthlyReportData
import com.dailybook.app.report.buildHtml
import com.dailybook.app.report.renderBitmap
import com.dailybook.app.report.writePdf
import com.dailybook.app.report.writePng
import com.dailybook.app.report.writeText
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.ui.theme.incomeColor
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.formatMonthLabel
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 月报导出的三种格式 */
private enum class ExportFormat(val mime: String, val extension: String) {
    IMAGE("image/png", "png"),
    HTML("text/html", "html"),
    PDF("application/pdf", "pdf")
}

private fun clockText(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
}

// ==================== v1.7：智能洞察 / 月报的数据拼装 ====================

/**
 * 把结构化洞察翻成一句话。
 *
 * SPENT_MORE 在数据层只有「能落到某个分类」时才带 category，
 * 所以没有分类时用 [StatsStrings.insightSpentMoreNoCategory] 这个不带分类的版本，
 * 避免拼出「主要在」这种半截话。渲染不出来（比如金额为 0）就返回空串，由调用方丢掉。
 */
private fun insightLine(insight: Insight, lang: Lang): String = when (insight.kind) {
    InsightKind.SPENT_MORE -> if (insight.category.isBlank()) {
        StatsStrings.insightSpentMoreNoCategory(lang, formatAmount(insight.amountCents))
    } else {
        AppStrings.insightSpentMore(
            lang,
            formatAmount(insight.amountCents),
            "${Categories.emojiOf(insight.category)}${insight.category}"
        )
    }

    InsightKind.SPENT_LESS ->
        AppStrings.insightSpentLess(lang, formatAmount(insight.amountCents))

    InsightKind.NO_RECORD_DAYS -> AppStrings.insightNoRecord(lang, insight.days)

    InsightKind.TOP_CATEGORY -> AppStrings.insightTopCategory(
        lang,
        "${Categories.emojiOf(insight.category)}${insight.category}",
        formatAmount(insight.amountCents)
    )

    InsightKind.BUDGET_LEFT ->
        AppStrings.insightBudgetLeft(lang, formatAmount(insight.amountCents))
}

private fun insightsToLines(insights: List<Insight>, lang: Lang): List<String> =
    insights.map { insightLine(it, lang) }.filter { it.isNotBlank() }

/** 环比卡片里那一块「金额 + 涨跌」 */
private data class CompareStat(
    val label: String,
    val amountText: String,
    val percent: Int?,
    val amountColor: Color
)

/**
 * 这个月有没有值得出月报的数据。
 *
 * 只看「有金额或有分类」这三件事，故意不去读 UiState 里别的标志位：
 * 月报的全部内容都由这三样派生，所以它们都是空的就一定没什么可导出。
 */
private val UiState.isReportable: Boolean
    get() = monthExpense > 0L || monthIncome > 0L || expenseSlices.isNotEmpty()

/**
 * 从 UiState 拼出月报需要的一切。
 *
 * 模块（[com.dailybook.app.report]）不认识 UiState、也不查字符串资源，
 * 所以所有文案都在这里用 pick/pickf 系列先拼好，包括洞察句子。
 */
private fun monthlyReportData(state: UiState, lang: Lang): MonthlyReportData {
    val monthLabel = formatMonthLabel(state.month, lang)
    val comparison = state.comparison

    // 分类行：金额由大到小，占比以支出最高的那类为基准，柱条才好比较
    val topCents = state.expenseSlices.maxOfOrNull { it.cents } ?: 0L
    val categories = state.expenseSlices.take(8).map { slice ->
        CategoryData(
            name = "${Categories.emojiOf(slice.category)}${slice.category}",
            amountText = formatAmount(slice.cents),
            ratio = if (topCents <= 0L) 0f else (slice.cents.toFloat() / topCents.toFloat())
        )
    }
    val maxDayCents = state.maxDayCents.coerceAtLeast(1L)

    return MonthlyReportData(
        title = AppStrings.reportMonthlyTitle(lang, monthLabel),
        monthLabel = monthLabel,
        appName = "${AppStrings.appName(lang)} DailyBook",
        summarySectionTitle = StatsStrings.reportSummarySection(lang),
        expenseLabel = AppStrings.txExpense(lang),
        incomeLabel = AppStrings.txIncome(lang),
        balanceLabel = AppStrings.txBalance(lang),
        expenseCents = state.monthExpense,
        incomeCents = state.monthIncome,
        balanceCents = state.balance,
        summaryNote = StatsStrings.reportSummaryLine(lang, monthLabel),
        dailySectionTitle = StatsStrings.reportDailySection(lang),
        dailyBars = state.dayBars.map { bar ->
            DailyBarData(
                label = StatsStrings.reportDailyBar(lang, bar.day, formatAmount(bar.cents)),
                ratio = (bar.cents.toFloat() / maxDayCents.toFloat()).coerceIn(0f, 1f)
            )
        },
        categorySectionTitle = StatsStrings.reportCategorySection(lang),
        categoryTopLabel = StatsStrings.reportTopCategoryLabel(lang),
        categories = categories,
        focusSectionTitle = StatsStrings.reportFocusSection(lang),
        focusLine = StatsStrings.reportFocusLine(
            lang,
            state.focusStats.monthCount,
            state.focusStats.monthMinutes
        ),
        compareSectionTitle = StatsStrings.reportCompareSection(lang),
        comparisons = listOf(
            CompareData(
                AppStrings.txExpense(lang),
                formatAmount(state.monthExpense),
                StatsStrings.compareDelta(lang, comparison.monthExpensePercent())
            ),
            CompareData(
                AppStrings.txIncome(lang),
                formatAmount(state.monthIncome),
                StatsStrings.compareDelta(lang, comparison.monthIncomePercent())
            ),
            CompareData(
                StatsStrings.yearExpense(lang),
                formatAmount(state.yearSummary.expense),
                StatsStrings.compareDelta(lang, comparison.yearExpensePercent())
            )
        ),
        insightSectionTitle = StatsStrings.reportInsightSection(lang),
        insights = insightsToLines(state.insights, lang),
        emptyText = StatsStrings.reportEmptySection(lang)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    state: UiState,
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val focus = state.focusStats
    val focusGoal by vm.settings.focusGoal.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ---- v1.7 月报导出 ----
    // 三个按钮都走系统文件选择器（SAF）：用户在系统界面里挑好位置，App 直接把内容写过去，
    // 所以既不需要存储权限，也不用先落一份临时文件。生成 + 写盘都在 IO 线程上，不卡界面。
    // 点按钮时把当时的月报数据快照存下来，SAF 回调里直接用这一份（回调不在同一次重组里）。
    var reportData by remember { mutableStateOf<MonthlyReportData?>(null) }
    var exporting by remember { mutableStateOf(false) }

    /** 生成 + 写盘放 IO 线程；成功失败各用一个 Toast 收尾 */
    fun runExport(generate: suspend () -> Unit) {
        if (exporting) return
        exporting = true
        scope.launch {
            val failure = withContext(Dispatchers.IO) {
                runCatching { generate() }.exceptionOrNull()
            }
            exporting = false
            val text = if (failure == null) {
                AppStrings.reportSaved(lang)
            } else {
                StatsStrings.reportExportFailed(
                    lang,
                    failure.message ?: failure.javaClass.simpleName
                )
            }
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }

    val htmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.HTML.mime)
    ) { uri ->
        val data = reportData ?: return@rememberLauncherForActivityResult
        if (uri != null) runExport { writeText(context, uri, buildHtml(data, lang)) }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.PDF.mime)
    ) { uri ->
        val data = reportData ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            runExport {
                if (!writePdf(context, uri, data, lang)) error("writePdf returned false")
            }
        }
    }

    val pngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.IMAGE.mime)
    ) { uri ->
        val data = reportData ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            runExport {
                val bitmap = renderBitmap(data, lang)
                try {
                    writePng(context, uri, bitmap)
                } finally {
                    // 1080×N 的位图，写完立刻回收，不留在内存里等 GC
                    bitmap.recycle()
                }
            }
        }
    }

    /** 没有数据就不弹文件选择器，直接提示 */
    fun startExport(format: ExportFormat) {
        if (state.isReportable) {
            reportData = monthlyReportData(state, lang)
            val fileName = "${StatsStrings.reportFileName(
                lang,
                context.getString(R.string.app_name),
                state.month.toString()
            )}.${format.extension}"
            when (format) {
                ExportFormat.HTML -> htmlLauncher.launch(fileName)
                ExportFormat.PDF -> pdfLauncher.launch(fileName)
                ExportFormat.IMAGE -> pngLauncher.launch(fileName)
            }
        } else {
            Toast.makeText(context, AppStrings.reportNoData(lang), Toast.LENGTH_LONG).show()
        }
    }

    // vm.message 里的提示（别处操作留下的）在这一页也消费掉并弹成 Toast；
    // 本页自己的导出结果直接弹 Toast，不往 vm.message 里写，避免两条消息互相覆盖。
    val vmMessage = vm.message.collectAsStateWithLifecycle()
    LaunchedEffect(vmMessage.value) {
        vmMessage.value?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.consumeMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = StatsStrings.title(lang),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        MonthSwitcher(
            label = AppStrings.yearMonth(lang, state.month.year, state.month.monthValue),
            onPrev = vm::previousMonth,
            onNext = vm::nextMonth,
            onToday = vm::goToCurrentMonth,
            showToday = state.month != YearMonth.now()
        )

        // ==================== 记账 ====================
        Spacer(Modifier.height(8.dp))
        SectionCard {
            Row {
                StatBlock(
                    AppStrings.txExpense(lang),
                    "¥${formatAmount(state.monthExpense)}",
                    expenseColor(),
                    Modifier.weight(1f)
                )
                StatBlock(
                    AppStrings.txIncome(lang),
                    "¥${formatAmount(state.monthIncome)}",
                    incomeColor(),
                    Modifier.weight(1f)
                )
                StatBlock(
                    AppStrings.txBalance(lang),
                    "¥${formatAmount(state.balance)}",
                    if (state.balance < 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                    Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = StatsStrings.expenseCategoryTitle(lang)) {
            if (state.expenseSlices.isEmpty()) {
                HintText(StatsStrings.noExpenseThisMonth(lang))
            } else {
                state.expenseSlices.forEachIndexed { index, slice ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    SliceRow(slice, expenseColor())
                }
            }
        }

        if (state.accountSlices.size > 1) {
            Spacer(Modifier.height(14.dp))
            SectionCard(title = StatsStrings.accountTitle(lang)) {
                state.accountSlices.forEachIndexed { index, slice ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    AccountSliceRow(slice)
                }
            }
        }

        if (state.hasCategoryBudget) {
            Spacer(Modifier.height(14.dp))
            SectionCard(title = StatsStrings.categoryBudgetTitle(lang)) {
                state.categoryBudgets.forEachIndexed { index, row ->
                    if (index > 0) Spacer(Modifier.height(14.dp))
                    CategoryBudgetView(row)
                }
                if (state.hasOverBudgetCategory) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = StatsStrings.overBudgetNote(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = expenseColor()
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = StatsStrings.dailyExpenseTitle(lang)) {
            if (state.maxDayCents <= 0L) {
                HintText(StatsStrings.noExpenseThisMonth(lang))
            } else {
                Spacer(Modifier.height(4.dp))
                DailyChart(state.dayBars, state.maxDayCents)
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = StatsStrings.yearTrendTitle(lang)) {
            if (state.yearSummary.maxBarCents <= 0L) {
                HintText(StatsStrings.noYearRecords(lang))
            } else {
                ChartLegend()
                Spacer(Modifier.height(8.dp))
                YearTrendChart(state.yearSummary.monthBars, state.yearSummary.maxBarCents)
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = StatsStrings.yearSummaryTitle(lang, state.yearSummary.year)) {
            if (!state.yearSummary.hasData) {
                HintText(StatsStrings.noThisYearRecords(lang))
            } else {
                Row {
                    StatBlock(
                        StatsStrings.yearExpense(lang),
                        "¥${formatAmount(state.yearSummary.expense)}",
                        expenseColor(),
                        Modifier.weight(1f)
                    )
                    StatBlock(
                        StatsStrings.yearIncome(lang),
                        "¥${formatAmount(state.yearSummary.income)}",
                        incomeColor(),
                        Modifier.weight(1f)
                    )
                    StatBlock(
                        StatsStrings.yearBalance(lang),
                        "¥${formatAmount(state.yearSummary.balance)}",
                        if (state.yearSummary.balance < 0) expenseColor()
                        else MaterialTheme.colorScheme.onSurface,
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = buildString {
                        append(StatsStrings.yearCount(lang, state.yearSummary.count))
                        if (state.yearSummary.topCategory.isNotBlank()) {
                            append(StatsStrings.topCategoryPrefix(lang))
                            append(Categories.emojiOf(state.yearSummary.topCategory))
                            append(state.yearSummary.topCategory)
                            append(" ¥${formatAmount(state.yearSummary.topCategoryCents)}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = StatsStrings.cumulative(lang)) {
            if (state.totalCount == 0) {
                HintText(StatsStrings.noRecordsAtAll(lang))
            } else {
                Row {
                    StatBlock(
                        StatsStrings.totalIncome(lang),
                        "¥${formatAmount(state.totalIncomeCents)}",
                        incomeColor(),
                        Modifier.weight(1f)
                    )
                    StatBlock(
                        StatsStrings.totalExpense(lang),
                        "¥${formatAmount(state.totalExpenseCents)}",
                        expenseColor(),
                        Modifier.weight(1f)
                    )
                    StatBlock(
                        StatsStrings.netBalance(lang),
                        "¥${formatAmount(state.totalBalance)}",
                        if (state.totalBalance < 0) expenseColor()
                        else MaterialTheme.colorScheme.onSurface,
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = StatsStrings.sinceFirstRecord(lang, state.totalCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 待报销：统计全部时间里标记为「待报销」的记录，与上面选的月份无关
        if (state.hasReimbursement) {
            Spacer(Modifier.height(14.dp))
            SectionCard(title = StatsStrings.reimbursementTitle(lang)) {
                AmountRow(
                    label = StatsStrings.pendingReimbursementLabel(lang),
                    value = StatsStrings.reimbursementAmountWithCount(
                        lang,
                        formatAmount(state.pendingReimbursementCents),
                        state.pendingReimbursementCount
                    ),
                    color = expenseColor()
                )
                Spacer(Modifier.height(8.dp))
                AmountRow(
                    label = StatsStrings.reimbursedLabel(lang),
                    value = StatsStrings.reimbursementAmount(
                        lang,
                        formatAmount(state.reimbursedCents)
                    ),
                    color = incomeColor()
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = StatsStrings.reimbursementNote(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.incomeSlices.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            SectionCard(title = StatsStrings.incomeSourceTitle(lang)) {
                state.incomeSlices.forEachIndexed { index, slice ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    SliceRow(slice, incomeColor())
                }
            }
        }

        // ==================== v1.7 环比对比 + 智能洞察 ====================
        Spacer(Modifier.height(14.dp))
        ComparisonCard(comparison = state.comparison, year = state.yearSummary.year)

        if (state.insights.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            InsightCard(insights = state.insights)
        }

        // ==================== 专注 ====================
        Spacer(Modifier.height(24.dp))
        Text(
            text = AppStrings.phaseFocus(lang),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        SectionCard {
            Row {
                StatBlock(
                    StatsStrings.focusToday(lang),
                    focus.todayCount.toString(),
                    MaterialTheme.colorScheme.primary,
                    Modifier.weight(1f)
                )
                StatBlock(
                    StatsStrings.focusStreak(lang),
                    "${focus.streak}",
                    MaterialTheme.colorScheme.secondary,
                    Modifier.weight(1f)
                )
                StatBlock(
                    StatsStrings.cumulative(lang),
                    focus.totalCount.toString(),
                    MaterialTheme.colorScheme.onSurface,
                    Modifier.weight(1f)
                )
            }
            if (focus.todayCount > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = StatsStrings.todayFocusMinutes(lang, focus.todayMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))
            Row {
                StatBlock(
                    StatsStrings.focusWeekCount(lang),
                    focus.weekCount.toString(),
                    MaterialTheme.colorScheme.primary,
                    Modifier.weight(1f)
                )
                StatBlock(
                    StatsStrings.focusWeekDuration(lang),
                    StatsStrings.focusMinutesShort(lang, focus.weekMinutes),
                    MaterialTheme.colorScheme.secondary,
                    Modifier.weight(1f)
                )
                StatBlock(
                    StatsStrings.focusMonthDuration(lang),
                    StatsStrings.focusMinutesShort(lang, focus.monthMinutes),
                    MaterialTheme.colorScheme.onSurface,
                    Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = StatsStrings.focusMonthSummary(
                    lang,
                    focus.monthCount,
                    focus.heatWeeks.flatten().sumOf { it.minutes.coerceAtLeast(0) }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (focusGoal > 0) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = StatsStrings.focusGoalTitle(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (focus.todayCount >= focusGoal) StatsStrings.focusGoalReached(lang)
                        else StatsStrings.focusGoalProgress(lang, focus.todayCount, focusGoal),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (focus.todayCount >= focusGoal) incomeColor()
                        else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                ThinProgressBar(
                    ratio = (focus.todayCount.toFloat() / focusGoal.toFloat()).coerceIn(0f, 1f),
                    color = if (focus.todayCount >= focusGoal) incomeColor()
                    else MaterialTheme.colorScheme.primary
                )
            }
        }

        if (focus.perTodo.isNotEmpty()) {
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
                Spacer(Modifier.height(10.dp))
                Text(
                    text = StatsStrings.perTodoHint(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = StatsStrings.heatmapTitle(lang)) {
            Spacer(Modifier.height(4.dp))
            FocusHeatmap(focus.heatWeeks, focus.heatMaxMinutes)
            Spacer(Modifier.height(10.dp))
            Text(
                text = StatsStrings.heatmapHint(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = StatsStrings.last7DaysTitle(lang)) {
            Spacer(Modifier.height(4.dp))
            FocusWeekChart(focus.recentDays)
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = StatsStrings.todayDetailTitle(lang)) {
            if (focus.todaySessions.isEmpty()) {
                HintText(StatsStrings.noFocusToday(lang))
            } else {
                focus.todaySessions.forEachIndexed { index, session ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    SessionRow(session)
                }
            }
        }

        // ==================== v1.7 月报导出 ====================
        // 三份导出各自走一次系统文件选择器；这个月没数据时不弹选择器，只提示
        Spacer(Modifier.height(14.dp))
        SectionCard(title = AppStrings.reportMonthlyTitle(lang, formatMonthLabel(state.month, lang))) {
            Text(
                text = StatsStrings.reportExportHint(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { startExport(ExportFormat.IMAGE) },
                    enabled = !exporting
                ) { Text(AppStrings.reportExportImage(lang)) }
                OutlinedButton(
                    onClick = { startExport(ExportFormat.HTML) },
                    enabled = !exporting
                ) { Text(AppStrings.reportExportHtml(lang)) }
                OutlinedButton(
                    onClick = { startExport(ExportFormat.PDF) },
                    enabled = !exporting
                ) { Text(AppStrings.reportExportPdf(lang)) }
            }
            if (exporting) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = AppStrings.reportGenerating(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SessionRow(session: FocusSessionEntity) {
    val lang = LocalLang.current
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
                    InterruptedTag(StatsStrings.sessionInterrupted(lang))
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

/** 淡色的「中断」小标签 */
@Composable
private fun InterruptedTag(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

private fun insightEmoji(kind: InsightKind): String = when (kind) {
    InsightKind.SPENT_MORE -> "🔥"
    InsightKind.SPENT_LESS -> "🍃"
    InsightKind.NO_RECORD_DAYS -> "📝"
    InsightKind.TOP_CATEGORY -> "🏆"
    InsightKind.BUDGET_LEFT -> "🎯"
}

/**
 * 涨跌小标签：支出变多偏暖（琥珀），变少偏冷（品牌绿），持平用弱色。
 * 没有可比基数（percent 为 null）时干脆不画，金额本身照样显示。
 */
@Composable
private fun ChangeChip(percent: Int?) {
    val lang = LocalLang.current
    if (percent == null) return
    val color = when {
        percent > 0 -> MaterialTheme.colorScheme.secondary
        percent < 0 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = StatsStrings.compareDelta(lang, percent),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

/**
 * 环比对比：本月 vs 上月（支出 / 收入），今年 vs 去年（支出）。
 *
 * 上面三行是「这个数是多少、比之前涨跌多少」，涨跌标签由 [ChangeChip] 画；
 * 下面三行是三个对比基准本身，用弱色显示，方便看清涨跌是从哪个数算出来的。
 * 上月和去年都还是空的时候没什么可比的，只给一句说明。
 */
@Composable
private fun ComparisonCard(comparison: Comparison, year: Int) {
    val lang = LocalLang.current
    val hasBase = comparison.lastMonthExpense > 0L ||
        comparison.lastMonthIncome > 0L ||
        comparison.lastYearExpense > 0L

    SectionCard(title = StatsStrings.comparisonTitle(lang)) {
        if (!hasBase) {
            HintText(StatsStrings.comparisonNoBase(lang))
        } else {
            val stats = listOf(
                CompareStat(
                    "${StatsStrings.thisMonthExpense(lang)} · ${AppStrings.vsLastMonth(lang)}",
                    "¥${formatAmount(comparison.monthExpense)}",
                    comparison.monthExpensePercent(),
                    expenseColor()
                ),
                CompareStat(
                    "${StatsStrings.thisMonthIncome(lang)} · ${AppStrings.vsLastMonth(lang)}",
                    "¥${formatAmount(comparison.monthIncome)}",
                    comparison.monthIncomePercent(),
                    incomeColor()
                ),
                CompareStat(
                    "${StatsStrings.yearSummaryTitle(lang, year)} · ${AppStrings.vsLastYear(lang)}",
                    "¥${formatAmount(comparison.yearExpense)}",
                    comparison.yearExpensePercent(),
                    expenseColor()
                )
            )
            stats.forEachIndexed { index, stat ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                AmountRow(
                    label = stat.label,
                    value = stat.amountText,
                    color = stat.amountColor,
                    suffix = { ChangeChip(stat.percent) }
                )
            }
            Spacer(Modifier.height(12.dp))
            AmountRow(
                label = StatsStrings.lastMonthExpense(lang),
                value = "¥${formatAmount(comparison.lastMonthExpense)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            AmountRow(
                label = StatsStrings.lastMonthIncome(lang),
                value = "¥${formatAmount(comparison.lastMonthIncome)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            AmountRow(
                label = StatsStrings.lastYearExpense(lang),
                value = "¥${formatAmount(comparison.lastYearExpense)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 智能洞察：最多 4 条（数据层已经截断），每条前面挂一个小图标 */
@Composable
private fun InsightCard(insights: List<Insight>) {
    val lang = LocalLang.current
    val lines = insights
        .map { insightEmoji(it.kind) to insightLine(it, lang) }
        .filter { it.second.isNotBlank() }

    SectionCard(title = StatsStrings.insightTitle(lang)) {
        if (lines.isEmpty()) {
            HintText(StatsStrings.insightNoData(lang))
        } else {
            lines.forEachIndexed { index, (emoji, text) ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/** 「标签 —— 金额」一行：金额靠右，长金额省略而不是撑破卡片；[suffix] 用来挂涨跌小标签 */
@Composable
private fun AmountRow(
    label: String,
    value: String,
    color: Color,
    suffix: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            if (suffix != null) {
                suffix()
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun SliceRow(slice: CategorySlice, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = Categories.emojiOf(slice.category),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = slice.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "¥${formatAmount(slice.cents)}  ${(slice.ratio * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            ThinProgressBar(ratio = slice.ratio, color = color, height = 6.dp)
        }
    }
}

@Composable
private fun DailyChart(bars: List<DayBar>, maxCents: Long) {
    val maxBarHeight = 108.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        bars.forEach { bar ->
            val fraction = if (maxCents <= 0L) 0f else bar.cents.toFloat() / maxCents.toFloat()
            val height = (maxBarHeight * fraction).coerceAtLeast(if (bar.cents > 0L) 4.dp else 2.dp)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .background(
                            if (bar.cents > 0L) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (bar.day == 1 || bar.day % 5 == 0) bar.day.toString() else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FocusWeekChart(days: List<FocusDay>) {
    val lang = LocalLang.current
    val maxCount = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val maxBarHeight = 108.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { day ->
            val fraction = day.count.toFloat() / maxCount.toFloat()
            val height = (maxBarHeight * fraction).coerceAtLeast(if (day.count > 0) 4.dp else 2.dp)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (day.count > 0) day.count.toString() else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(height)
                        .background(
                            if (day.count > 0) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = AppStrings.weekday(lang, day.date.dayOfWeek.value - 1),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartLegend() {
    val lang = LocalLang.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        LegendDot(expenseColor())
        Spacer(Modifier.width(4.dp))
        Text(
            text = AppStrings.txExpense(lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(14.dp))
        LegendDot(incomeColor())
        Spacer(Modifier.width(4.dp))
        Text(
            text = AppStrings.txIncome(lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .width(8.dp)
            .height(8.dp)
            .background(color, RoundedCornerShape(2.dp))
    )
}

/** 近 12 个月收支双柱图：每个月两根细柱（左支出、右收入） */
@Composable
private fun YearTrendChart(bars: List<MonthBar>, maxCents: Long) {
    val maxBarHeight = 104.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        bars.forEach { bar ->
            val expenseFraction =
                if (maxCents <= 0L) 0f else bar.expense.toFloat() / maxCents.toFloat()
            val incomeFraction =
                if (maxCents <= 0L) 0f else bar.income.toFloat() / maxCents.toFloat()
            val expenseHeight = (maxBarHeight * expenseFraction)
                .coerceAtLeast(if (bar.expense > 0L) 4.dp else 2.dp)
            val incomeHeight = (maxBarHeight * incomeFraction)
                .coerceAtLeast(if (bar.income > 0L) 4.dp else 2.dp)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier.height(maxBarHeight),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(expenseHeight)
                            .background(
                                if (bar.expense > 0L) expenseColor()
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(incomeHeight)
                            .background(
                                if (bar.income > 0L) incomeColor()
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (bar.month.monthValue == 1) "${bar.month.year}" else "${bar.month.monthValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AccountSliceRow(slice: AccountSlice) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = Accounts.emojiOf(slice.account),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = slice.account,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "¥${formatAmount(slice.cents)}  ${(slice.ratio * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            ThinProgressBar(
                ratio = slice.ratio,
                color = MaterialTheme.colorScheme.primary,
                height = 6.dp
            )
        }
    }
}

@Composable
private fun CategoryBudgetView(row: CategoryBudgetRow) {
    val lang = LocalLang.current
    val accent = if (row.over) expenseColor() else MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${Categories.emojiOf(row.category)} ${row.category}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (row.over) StatsStrings.overBudget(lang, formatAmount(row.overCents))
                else StatsStrings.remainingBudget(lang, formatAmount(row.remainingCents)),
                style = MaterialTheme.typography.bodySmall,
                color = accent
            )
        }
        Spacer(Modifier.height(6.dp))
        ThinProgressBar(ratio = row.ratio, color = accent, height = 6.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = StatsStrings.usedOfBudget(
                lang,
                formatAmount(row.spentCents),
                formatAmount(row.budgetCents)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 近 12 周专注热力图：一列一周，7 行（周一到周日） */
@Composable
private fun FocusHeatmap(weeks: List<List<HeatCell>>, maxMinutes: Int) {
    if (weeks.isEmpty()) return
    val lang = LocalLang.current
    val cell = 13.dp
    val gap = 3.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                week.forEach { day ->
                    val color = when {
                        // 还没到的日子留空
                        day.minutes < 0 -> Color.Transparent
                        day.minutes == 0 -> MaterialTheme.colorScheme.surfaceVariant
                        else -> {
                            val ratio =
                                if (maxMinutes <= 0) 1f
                                else day.minutes.toFloat() / maxMinutes.toFloat()
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f + 0.7f * ratio)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(cell)
                            .background(color, RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = StatsStrings.heatmapLess(lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        listOf(0.3f, 0.5f, 0.75f, 1f).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(2.dp))
        Text(
            text = StatsStrings.heatmapMore(lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
