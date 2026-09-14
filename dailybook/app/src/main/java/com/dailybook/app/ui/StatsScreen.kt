package com.dailybook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.Categories
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.ui.theme.incomeColor
import com.dailybook.app.util.formatAmount
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    state: UiState,
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
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
        Spacer(Modifier.height(6.dp))

        MonthSwitcher(
            label = state.monthLabel,
            onPrev = vm::previousMonth,
            onNext = vm::nextMonth,
            onToday = vm::goToCurrentMonth,
            showToday = state.month != YearMonth.now()
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.padding(vertical = 18.dp)) {
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "支出分类占比",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(14.dp))
                if (state.expenseSlices.isEmpty()) {
                    HintText("本月还没有支出记录")
                } else {
                    state.expenseSlices.forEachIndexed { index, slice ->
                        if (index > 0) Spacer(Modifier.height(12.dp))
                        SliceRow(slice, expenseColor())
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "每日支出",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                if (state.maxDayCents <= 0L) {
                    HintText("本月还没有支出记录")
                } else {
                    DailyChart(state.dayBars, state.maxDayCents)
                }
            }
        }

        if (state.incomeSlices.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        text = "收入来源占比",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(14.dp))
                    state.incomeSlices.forEachIndexed { index, slice ->
                        if (index > 0) Spacer(Modifier.height(12.dp))
                        SliceRow(slice, incomeColor())
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun StatBlock(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = color)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(slice.ratio.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(color, RoundedCornerShape(3.dp))
                )
            }
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
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
