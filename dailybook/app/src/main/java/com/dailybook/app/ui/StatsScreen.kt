package com.dailybook.app.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailybook.app.CategorySlice
import com.dailybook.app.DayBar
import com.dailybook.app.FocusDay
import com.dailybook.app.MainViewModel
import com.dailybook.app.MonthBar
import com.dailybook.app.UiState
import com.dailybook.app.data.Categories
import com.dailybook.app.data.FocusSessionEntity
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.ui.theme.incomeColor
import com.dailybook.app.util.formatAmount
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

private val WEEKDAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

private fun clockText(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
}

@Composable
fun StatsScreen(
    state: UiState,
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val focus = state.focusStats

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = "统计",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        MonthSwitcher(
            label = state.monthLabel,
            onPrev = vm::previousMonth,
            onNext = vm::nextMonth,
            onToday = vm::goToCurrentMonth,
            showToday = state.month != YearMonth.now()
        )

        // ==================== 记账 ====================
        Spacer(Modifier.height(8.dp))
        SectionCard {
            Row {
                StatBlock("支出", "¥${formatAmount(state.monthExpense)}", expenseColor(), Modifier.weight(1f))
                StatBlock("收入", "¥${formatAmount(state.monthIncome)}", incomeColor(), Modifier.weight(1f))
                StatBlock(
                    "结余",
                    "¥${formatAmount(state.balance)}",
                    if (state.balance < 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                    Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "支出分类占比") {
            if (state.expenseSlices.isEmpty()) {
                HintText("本月还没有支出记录")
            } else {
                state.expenseSlices.forEachIndexed { index, slice ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    SliceRow(slice, expenseColor())
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "每日支出") {
            if (state.maxDayCents <= 0L) {
                HintText("本月还没有支出记录")
            } else {
                Spacer(Modifier.height(4.dp))
                DailyChart(state.dayBars, state.maxDayCents)
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "近 12 个月收支趋势") {
            if (state.yearSummary.maxBarCents <= 0L) {
                HintText("最近一年还没有记账记录")
            } else {
                ChartLegend()
                Spacer(Modifier.height(8.dp))
                YearTrendChart(state.yearSummary.monthBars, state.yearSummary.maxBarCents)
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "${state.yearSummary.year} 年汇总") {
            if (!state.yearSummary.hasData) {
                HintText("今年还没有记账记录")
            } else {
                Row {
                    StatBlock(
                        "年支出",
                        "¥${formatAmount(state.yearSummary.expense)}",
                        expenseColor(),
                        Modifier.weight(1f)
                    )
                    StatBlock(
                        "年收入",
                        "¥${formatAmount(state.yearSummary.income)}",
                        incomeColor(),
                        Modifier.weight(1f)
                    )
                    StatBlock(
                        "年结余",
                        "¥${formatAmount(state.yearSummary.balance)}",
                        if (state.yearSummary.balance < 0) expenseColor()
                        else MaterialTheme.colorScheme.onSurface,
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = buildString {
                        append("全年 ${state.yearSummary.count} 笔")
                        if (state.yearSummary.topCategory.isNotBlank()) {
                            append(" · 花得最多：")
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
        SectionCard(title = "累计") {
            if (state.totalCount == 0) {
                HintText("还没有任何记账记录")
            } else {
                Row {
                    StatBlock(
                        "总收入",
                        "¥${formatAmount(state.totalIncomeCents)}",
                        incomeColor(),
                        Modifier.weight(1f)
                    )
                    StatBlock(
                        "总支出",
                        "¥${formatAmount(state.totalExpenseCents)}",
                        expenseColor(),
                        Modifier.weight(1f)
                    )
                    StatBlock(
                        "净结余",
                        "¥${formatAmount(state.totalBalance)}",
                        if (state.totalBalance < 0) expenseColor()
                        else MaterialTheme.colorScheme.onSurface,
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "从第一笔记账到现在，共 ${state.totalCount} 笔",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.incomeSlices.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            SectionCard(title = "收入来源占比") {
                state.incomeSlices.forEachIndexed { index, slice ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    SliceRow(slice, incomeColor())
                }
            }
        }

        // ==================== 专注 ====================
        Spacer(Modifier.height(24.dp))
        Text(
            text = "专注",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        SectionCard {
            Row {
                StatBlock("今日", focus.todayCount.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatBlock("连续", "${focus.streak}", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                StatBlock("累计", focus.totalCount.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            }
            if (focus.todayCount > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "今日专注 ${focus.todayMinutes} 分钟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "最近 7 天完成的专注") {
            Spacer(Modifier.height(4.dp))
            FocusWeekChart(focus.recentDays)
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "今日专注明细") {
            if (focus.todaySessions.isEmpty()) {
                HintText("今天还没有完成的专注")
            } else {
                focus.todaySessions.forEachIndexed { index, session ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    SessionRow(session)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SessionRow(session: FocusSessionEntity) {
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
            Text(
                text = "${session.minutes} 分钟",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
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
                    text = WEEKDAY_LABELS[day.date.dayOfWeek.value - 1],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartLegend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LegendDot(expenseColor())
        Spacer(Modifier.width(4.dp))
        Text(
            text = "支出",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(14.dp))
        LegendDot(incomeColor())
        Spacer(Modifier.width(4.dp))
        Text(
            text = "收入",
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
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
