package com.dailybook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.dailybook.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    state: UiState,
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    var sheetOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = null
                    sheetOpen = true
                },
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
                showToday = state.month != YearMonth.now()
            )

            MonthSummaryCard(
                expense = state.monthExpense,
                income = state.monthIncome,
                balance = state.balance,
                todayExpense = state.todayExpense,
                count = state.monthCount
            )

            if (state.hasBudget) {
                BudgetCard(
                    expense = state.monthExpense,
                    budget = state.budgetCents,
                    ratio = state.budgetRatio,
                    remaining = state.budgetRemainingCents,
                    overBudget = state.overBudget
                )
            }

            SearchField(
                value = state.ledgerQuery,
                onValueChange = vm::setLedgerQuery,
                placeholder = "搜索分类或备注",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (!state.hasMonthData) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyHint(
                        emoji = if (state.isSearching) "🔍" else "🧾",
                        title = if (state.isSearching) "没有匹配的记录" else "这个月还没有记账",
                        subtitle = if (state.isSearching) "换个关键词试试" else "点右下角「记一笔」开始"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 104.dp)
                ) {
                    state.monthGroups.forEach { group ->
                        item(key = "day-${group.date}") { DayHeader(group) }
                        items(items = group.items, key = { it.id }) { tx ->
                            TransactionRow(
                                tx = tx,
                                onEdit = {
                                    editing = tx
                                    sheetOpen = true
                                },
                                onDelete = { vm.deleteTransaction(tx) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (sheetOpen) {
        TransactionSheet(
            editing = editing,
            onDismiss = {
                sheetOpen = false
                editing = null
            },
            onSave = { amountCents, type, category, note, dayMillis ->
                val target = editing
                if (target == null) {
                    vm.addTransaction(amountCents, type, category, note, dayMillis)
                } else {
                    vm.updateTransaction(target, amountCents, type, category, note, dayMillis)
                }
                sheetOpen = false
                editing = null
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
    SectionCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
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
            StatBlock("支出", "¥${formatAmount(expense)}", expenseColor(), Modifier.weight(1f))
            StatBlock("收入", "¥${formatAmount(income)}", incomeColor(), Modifier.weight(1f))
            StatBlock(
                "今日支出",
                "¥${formatAmount(todayExpense)}",
                MaterialTheme.colorScheme.onSurface,
                Modifier.weight(1f)
            )
        }
        if (count > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "本月共 $count 笔记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetCard(
    expense: Long,
    budget: Long,
    ratio: Float,
    remaining: Long,
    overBudget: Boolean
) {
    val accent = if (overBudget) expenseColor() else MaterialTheme.colorScheme.primary

    SectionCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "本月预算",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (overBudget) "已超支 ¥${formatAmount(expense - budget)}"
                else "剩余 ¥${formatAmount(remaining)}",
                style = MaterialTheme.typography.bodyMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(10.dp))
        ThinProgressBar(ratio = ratio, color = accent)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "已用 ¥${formatAmount(expense)} / ¥${formatAmount(budget)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun TransactionRow(
    tx: TransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val isExpense = tx.type == TxType.EXPENSE
    val amountColor = if (isExpense) expenseColor() else incomeColor()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, Shapes.card)
            .clickable { onEdit() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(category = tx.category, emoji = Categories.emojiOf(tx.category))

        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = tx.category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val subtitle = buildString {
                if (tx.note.isNotBlank()) append(tx.note)
                if (tx.dateMillis.toLocalDate() != LocalDate.now()) {
                    if (isNotEmpty()) append(" · ")
                    append("${tx.dateMillis.toLocalDate().monthValue}月${tx.dateMillis.toLocalDate().dayOfMonth}日")
                }
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
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
        ConfirmDialog(
            title = "删除这条记录？",
            text = "${tx.category} ¥${formatAmount(tx.amountCents)}" +
                if (tx.note.isBlank()) "" else "（${tx.note}）",
            confirmText = "删除",
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
    }
}

/**
 * 记一笔 / 编辑记录 共用的底部弹窗。
 * 内容可滚动 + imePadding，保证小屏或键盘弹出时「保存」按钮依然够得到。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionSheet(
    editing: TransactionEntity?,
    onDismiss: () -> Unit,
    onSave: (cents: Long, type: TxType, category: String, note: String, dayMillis: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember(editing) { mutableStateOf(editing?.type ?: TxType.EXPENSE) }
    var amountText by remember(editing) {
        mutableStateOf(editing?.let { formatAmount(it.amountCents) } ?: "")
    }
    var category by remember(editing) {
        mutableStateOf(editing?.category ?: Categories.EXPENSE.first())
    }
    var note by remember(editing) { mutableStateOf(editing?.note ?: "") }
    var selectedDate by remember(editing) {
        mutableStateOf(editing?.dateMillis?.toLocalDate() ?: LocalDate.now())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val cents = parseAmountToCents(amountText)
    val categories = Categories.forType(type)
    val today = LocalDate.now()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = Shapes.sheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (editing == null) "记一笔" else "编辑记录",
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
                            val list = Categories.forType(t)
                            if (category !in list) category = list.first()
                        },
                        label = { Text(t.label) }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 &&
                        input.all { it.isDigit() || it == '.' } &&
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
            FieldLabel("分类")
            Spacer(Modifier.height(8.dp))
            // 会换行，小屏 / 大字模式不会把分类挤到屏幕外
            ChipFlow {
                categories.forEach { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text("${Categories.emojiOf(item)} $item") }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            FieldLabel("日期")
            Spacer(Modifier.height(8.dp))
            ChipFlow {
                FilterChip(
                    selected = selectedDate == today,
                    onClick = { selectedDate = today },
                    label = { Text("今天") }
                )
                FilterChip(
                    selected = selectedDate == today.minusDays(1),
                    onClick = { selectedDate = today.minusDays(1) },
                    label = { Text("昨天") }
                )
                FilterChip(
                    selected = selectedDate == today.minusDays(2),
                    onClick = { selectedDate = today.minusDays(2) },
                    label = { Text("前天") }
                )
                FilterChip(
                    selected = selectedDate != today &&
                        selectedDate != today.minusDays(1) &&
                        selectedDate != today.minusDays(2),
                    onClick = { showDatePicker = true },
                    label = {
                        Text(
                            if (selectedDate == today) "选日期"
                            else "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日"
                        )
                    }
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
                    .heightIn(min = 50.dp),
                shape = Shapes.pill
            ) {
                Text(
                    text = when {
                        cents == null -> "请输入金额"
                        editing == null -> "保存  ¥${formatAmount(cents)}"
                        else -> "保存修改  ¥${formatAmount(cents)}"
                    },
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
                        // DatePicker 返回 UTC 零点，按 UTC 还原日期再转成本地零点
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
