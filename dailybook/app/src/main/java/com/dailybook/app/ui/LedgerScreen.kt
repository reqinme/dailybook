package com.dailybook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailybook.app.DayGroup
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.Categories
import com.dailybook.app.data.TransactionEntity
import com.dailybook.app.data.TxType
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.ui.theme.incomeColor
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.formatDateHeader
import com.dailybook.app.util.parseAmountToCents
import com.dailybook.app.util.toDayMillis
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    state: UiState,
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("记一笔") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MonthSwitcher(
                label = state.monthLabel,
                onPrev = vm::previousMonth,
                onNext = vm::nextMonth,
                onToday = vm::goToCurrentMonth,
                showToday = state.month != java.time.YearMonth.now()
            )

            MonthSummaryCard(
                expense = state.monthExpense,
                income = state.monthIncome,
                balance = state.balance,
                todayExpense = state.todayExpense,
                count = state.monthCount
            )

            if (!state.hasMonthData) {
                EmptyLedger()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 104.dp)
                ) {
                    state.monthGroups.forEach { group ->
                        item(key = "day-${group.date}") {
                            DayHeader(group)
                        }
                        items(items = group.items, key = { it.id }) { tx ->
                            TransactionRow(
                                tx = tx,
                                onDelete = { vm.deleteTransaction(tx) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            onDismiss = { showAddSheet = false },
            onSave = { cents, type, category, note, dayMillis ->
                vm.addTransaction(cents, type, category, note, dayMillis)
                showAddSheet = false
            }
        )
    }
}

@Composable
private fun MonthSummaryCard(
    expense: Long,
    income: Long,
    balance: Long,
    todayExpense: Long,
    count: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "本月结余",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "¥${formatAmount(balance)}",
                style = MaterialTheme.typography.displaySmall,
                color = if (balance < 0) expenseColor() else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                SummaryItem("支出", "¥${formatAmount(expense)}", expenseColor(), Modifier.weight(1f))
                SummaryItem("收入", "¥${formatAmount(income)}", incomeColor(), Modifier.weight(1f))
                SummaryItem("今日支出", "¥${formatAmount(todayExpense)}", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            }
            if (count > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "本月共 $count 笔记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun DayHeader(group: DayGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDateHeader(group.date),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        val parts = buildList {
            if (group.expense > 0) add("支 ¥${formatAmount(group.expense)}")
            if (group.income > 0) add("收 ¥${formatAmount(group.income)}")
        }
        Text(
            text = parts.joinToString("  "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TransactionRow(tx: TransactionEntity, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    val isExpense = tx.type == TxType.EXPENSE
    val amountColor = if (isExpense) expenseColor() else incomeColor()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = Categories.emojiOf(tx.category), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = tx.category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (tx.note.isNotBlank()) {
                Text(
                    text = tx.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = (if (isExpense) "-" else "+") + "¥${formatAmount(tx.amountCents)}",
            style = MaterialTheme.typography.titleMedium,
            color = amountColor
        )
        IconButton(onClick = { confirmDelete = true }) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这条记录？") },
            text = { Text("${tx.category} ¥${formatAmount(tx.amountCents)}${if (tx.note.isBlank()) "" else "（${tx.note}）"}") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    confirmDelete = false
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun EmptyLedger() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🧾", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "这个月还没有记账",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "点右下角「记一笔」开始",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
    onDismiss: () -> Unit,
    onSave: (cents: Long, type: TxType, category: String, note: String, dayMillis: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(TxType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Categories.EXPENSE.first()) }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val cents = parseAmountToCents(amountText)
    val categories = Categories.forType(type)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "记一笔",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(14.dp))

            // 支出 / 收入
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = {
                            type = t
                            category = Categories.forType(t).first()
                        },
                        label = { Text(t.label) }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    // 只允许数字和一个小数点，最多两位小数
                    if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' } &&
                        input.length <= 12
                    ) {
                        amountText = input
                    }
                },
                label = { Text("金额") },
                prefix = { Text("¥") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            SectionLabel("分类")
            Spacer(Modifier.height(8.dp))
            // 分类按钮按每行 4 个排布（避免使用实验性的 FlowRow）
            categories.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text("${Categories.emojiOf(item)} $item") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel("日期")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val today = LocalDate.now()
                QuickDateChip("今天", selectedDate == today) { selectedDate = today }
                QuickDateChip("昨天", selectedDate == today.minusDays(1)) { selectedDate = today.minusDays(1) }
                QuickDateChip("前天", selectedDate == today.minusDays(2)) { selectedDate = today.minusDays(2) }
                FilterChip(
                    selected = selectedDate != today &&
                        selectedDate != today.minusDays(1) &&
                        selectedDate != today.minusDays(2),
                    onClick = { showDatePicker = true },
                    label = { Text(if (selectedDate == today) "选日期" else "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日") }
                )
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 40) note = it },
                label = { Text("备注（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val value = parseAmountToCents(amountText) ?: return@Button
                    onSave(value, type, category, note.trim(), selectedDate.toDayMillis())
                },
                enabled = cents != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text(
                    text = if (cents == null) "请输入金额" else "保存  ¥${formatAmount(cents)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toDayMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        // DatePicker 返回的是 UTC 零点，需按 UTC 还原成日期再转本地零点
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun QuickDateChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
