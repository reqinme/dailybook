package com.dailybook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.CommonStrings
import com.dailybook.app.i18n.LedgerStrings
import com.dailybook.app.i18n.LocalLang
import java.time.YearMonth

/**
 * 年份可选范围：能改（2000 年之前的老账、很久以后的分期都算得出来），
 * 又不会因为手滑输入 99999 让年月条彻底跑飞。
 */
private const val MIN_YEAR = 1900
private const val MAX_YEAR = 2100

/**
 * 年 / 月快速切换条（记账页与统计页共用）。
 *
 * 为什么要有它：原来只有 ◀ ▶ 两颗箭头，想从本月跳到去年 3 月得点十几次。
 * 这里中间的年月本身可以点开一个「年 + 12 个月」的紧凑选择器，一次到位；
 * 箭头保留，因为「上个月 / 下个月」仍然是最常用的操作。
 *
 * 组件自己不认识 ViewModel：三个回调分别对应「上一个 / 下一个 / 直接跳到某个月」，
 * 由调用方接到真正的月份状态上（记账页与统计页共用同一份 selectedMonth）。
 *
 * @param yearMonth 当前显示的年月，用来初始化选择器里选中的年份
 * @param label 已经按当前语言拼好的年月文案（如「2026年3月」）
 * @param onPrev 上一月
 * @param onNext 下一月
 * @param onPick 从选择器里直接选中的某个月
 * @param onToday 回到本月
 */
@Composable
fun MonthYearBar(
    yearMonth: YearMonth,
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPick: (YearMonth) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    var picking by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = CommonStrings.prevMonth(lang))
        }
        // 年月本身就是「打开选择器」的按钮，比额外加一颗「选择年月」更省地方
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .clip(Shapes.badge)
                .clickable { picking = true }
                .padding(vertical = 6.dp, horizontal = 6.dp)
        )
        TextButton(onClick = onToday) { Text(CommonStrings.backToThisMonth(lang)) }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = CommonStrings.nextMonth(lang))
        }
    }

    if (picking) {
        MonthYearPickerDialog(
            yearMonth = yearMonth,
            onDismiss = { picking = false },
            onPick = { picked ->
                onPick(picked)
                picking = false
            },
            onToday = {
                onToday()
                picking = false
            }
        )
    }
}

/**
 * 年 / 月选择器：上面一行选年（箭头逐年前后翻，点年份直接输入），下面 12 个月按钮。
 * 选中的月份高亮；底部「回到本月」一键跳回，不用先选年再选月。
 */
@Composable
private fun MonthYearPickerDialog(
    yearMonth: YearMonth,
    onDismiss: () -> Unit,
    onPick: (YearMonth) -> Unit,
    onToday: () -> Unit
) {
    val lang = LocalLang.current
    // 选择器里的年份和外面的月份解耦：翻着看不同年份时，页面上的月份先不动
    var year by remember(yearMonth) { mutableStateOf(yearMonth.year) }
    var editingYear by remember { mutableStateOf(false) }
    var yearText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(LedgerStrings.monthYearTitle(lang)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { year = (year - 1).coerceAtLeast(MIN_YEAR) }) {
                        Icon(
                            Icons.Filled.ChevronLeft,
                            contentDescription = LedgerStrings.prevYear(lang)
                        )
                    }
                    Text(
                        text = AppStrings.year(lang, year),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .clip(Shapes.badge)
                            .clickable {
                                yearText = year.toString()
                                editingYear = true
                            }
                            .padding(vertical = 6.dp)
                    )
                    IconButton(onClick = { year = (year + 1).coerceAtMost(MAX_YEAR) }) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = LedgerStrings.nextYear(lang)
                        )
                    }
                }

                if (editingYear) {
                    Spacer(Modifier.height(4.dp))
                    YearInput(
                        text = yearText,
                        onTextChange = { yearText = it },
                        onConfirm = {
                            val parsed = yearText.trim().toIntOrNull()
                            if (parsed != null && parsed in MIN_YEAR..MAX_YEAR) {
                                year = parsed
                                editingYear = false
                            }
                        },
                        onCancel = { editingYear = false }
                    )
                }

                Spacer(Modifier.height(10.dp))
                MonthGrid(
                    year = year,
                    selectedMonth = yearMonth.takeIf { it.year == year }?.monthValue,
                    onPick = { month -> onPick(YearMonth.of(year, month)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onToday) { Text(LedgerStrings.monthYearThisMonth(lang)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel(lang)) }
        }
    )
}

/** 12 个月按钮：4 列换行，窄屏也放得下；选中的那个高亮 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthGrid(
    year: Int,
    selectedMonth: Int?,
    onPick: (Int) -> Unit
) {
    val lang = LocalLang.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (1..12).forEach { month ->
            FilterChip(
                selected = selectedMonth == month,
                onClick = { onPick(month) },
                label = { Text(LedgerStrings.monthShort(lang, month)) }
            )
        }
    }
}

/** 手输年份的小输入框：范围外的输入直接给提示，不会把年月条带跑偏 */
@Composable
private fun YearInput(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val lang = LocalLang.current
    val value = text.trim().toIntOrNull()
    val valid = value != null && value in MIN_YEAR..MAX_YEAR

    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) onTextChange(it) },
            label = { Text(LedgerStrings.yearFieldLabel(lang)) },
            placeholder = { Text(LedgerStrings.yearFieldHint(lang)) },
            isError = text.isNotEmpty() && !valid,
            supportingText = {
                if (text.isNotEmpty() && !valid) {
                    Text(LedgerStrings.yearFieldInvalid(lang, MIN_YEAR, MAX_YEAR))
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(min = 140.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onConfirm, enabled = valid) { Text(AppStrings.confirm(lang)) }
            TextButton(onClick = onCancel) { Text(AppStrings.cancel(lang)) }
        }
    }
}
