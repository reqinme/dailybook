package com.dailybook.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailybook.app.CalendarDay
import com.dailybook.app.DayGroup
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.Accounts
import com.dailybook.app.data.Categories
import com.dailybook.app.data.Currencies
import com.dailybook.app.data.TransactionEntity
import com.dailybook.app.data.TxType
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LedgerStrings
import com.dailybook.app.i18n.LocalLang
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

/** 内容区：流水列表 / 月日历 */
private enum class LedgerView { LIST, CALENDAR }

/** 标签输入里逗号 / 空格 / 顿号 / 分号都当分隔符，跟用户的输入习惯对齐 */
private val TAG_SEPARATORS = charArrayOf(',', '，', ';', '；', '、', ' ', '\n', '\t')

/** 标签输入框文本 → 标签列表（去空白、去重、忽略空串） */
private fun parseTagInput(raw: String): List<String> =
    raw.split(*TAG_SEPARATORS)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

/** 汇率文本 → ×RATE_SCALE 的整数汇率；填不出数就按 1:1 兜底 */
private fun rateToScaled(text: String): Long {
    val raw = text.trim()
    val value = (if (raw.isEmpty()) 1.0 else raw.toDoubleOrNull() ?: 1.0) * Currencies.RATE_SCALE
    return if (value < 1.0) 1L else value.toLong()
}

/** 去掉小数末尾多余的 0（7.20 → 7.2，1.00 → 1），让数字输入框读起来自然 */
private fun trimZeros(text: String): String {
    if (!text.contains('.')) return text
    return text.trimEnd('0').trimEnd('.')
}

/** 汇率（×RATE_SCALE）→ 可编辑文本，如 7.2；没有记录过就按 1 兜底 */
private fun rateToText(rateScaled: Long): String {
    if (rateScaled <= 0L) return "1"
    val exact = trimZeros(formatAmount(rateScaled / 10L))
    return if (exact.isEmpty() || exact == "0") "1" else exact
}

/** 某个收支类型下可选的分类：用户自定义清单 ∪ 数据里用过的分类，空清单时退回内置预置 */
private fun categoryOptions(
    type: TxType,
    expense: List<String>,
    income: List<String>
): List<String> = when (type) {
    TxType.EXPENSE -> expense.ifEmpty { Categories.EXPENSE }
    TxType.INCOME -> income.ifEmpty { Categories.INCOME }
}

/**
 * 跳到指定的某个月。
 *
 * 本来最省事的做法是给 MainViewModel 加一个 `fun moveToMonth(target: YearMonth)`，
 * 但这一版约定不改 MainViewModel，所以用现成的三个月份动作推过去：
 * 差几个月就走几次 next / previous（每次都是一次状态赋值，最多几十次），差 0 就什么都不做。
 */
private fun MainViewModel.moveToMonth(target: YearMonth) {
    val current = uiState.value.month
    var delta = (target.year - current.year) * 12 + (target.monthValue - current.monthValue)
    while (delta > 0) {
        nextMonth()
        delta--
    }
    while (delta < 0) {
        previousMonth()
        delta++
    }
}

/** 日历格子里的短金额：整元不带小数，大数字用 k 收窄 */
private fun compactAmount(cents: Long): String {
    if (cents <= 0L) return ""
    val yuanExact = cents / 100.0
    if (yuanExact >= 1000.0) {
        val thousands = (yuanExact / 100.0).toLong() / 10.0
        return trimZeros(String.format(java.util.Locale.ROOT, "%.1f", thousands)) + "k"
    }
    return if (cents % 100L == 0L) (cents / 100L).toString()
    else trimZeros(String.format(java.util.Locale.ROOT, "%.1f", yuanExact))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    state: UiState,
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    var sheetOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    var view by remember { mutableStateOf(LedgerView.LIST) }
    val lang = LocalLang.current

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
                text = { Text(LedgerStrings.addEntry(lang)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MonthYearBar(
                yearMonth = state.month,
                label = AppStrings.yearMonth(lang, state.month.year, state.month.monthValue),
                onPrev = vm::previousMonth,
                onNext = vm::nextMonth,
                onPick = { picked -> vm.moveToMonth(picked) },
                onToday = vm::goToCurrentMonth
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

            if (state.hasReimbursement) {
                ReimbursementCard(state = state)
            }

            if (state.dayFilter != null) {
                DayFilterCard(state = state, vm = vm)
            }

            SearchField(
                value = state.ledgerQuery,
                onValueChange = vm::setLedgerQuery,
                placeholder = LedgerStrings.searchHint(lang),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // 用过两个以上账户才显示筛选条，只有一个账户时不必占地方
            if (state.accounts.size > 1) {
                AccountFilterRow(state = state, vm = vm, lang = lang)
            }

            if (state.hasTags) {
                TagFilterRow(state = state, vm = vm, lang = lang)
            }

            LedgerViewToggle(
                view = view,
                onSelect = { view = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            LedgerList(
                state = state,
                vm = vm,
                view = view,
                lang = lang,
                onEdit = { tx ->
                    editing = tx
                    sheetOpen = true
                }
            )
        }
    }

    if (sheetOpen) {
        TransactionSheet(
            editing = editing,
            knownAccounts = state.accounts,
            expenseCategories = state.expenseCategories,
            incomeCategories = state.incomeCategories,
            knownTags = state.allTags,
            rateOf = { code -> vm.settings.currencyRate(code) },
            onRateChange = { code, rateScaled -> vm.settings.setCurrencyRate(code, rateScaled) },
            onDismiss = {
                sheetOpen = false
                editing = null
            },
            onSave = {
                    amountCents, type, category, note, dayMillis, account,
                    tags, reimbursable, currency, foreignAmountCents, rateScaled ->
                val target = editing
                if (target == null) {
                    vm.addTransaction(
                        amountCents = amountCents,
                        type = type,
                        category = category,
                        note = note,
                        dateMillis = dayMillis,
                        account = account,
                        tags = tags,
                        reimbursable = reimbursable,
                        currency = currency,
                        foreignAmountCents = foreignAmountCents,
                        rateScaled = rateScaled
                    )
                } else {
                    vm.updateTransaction(
                        item = target,
                        amountCents = amountCents,
                        type = type,
                        category = category,
                        note = note,
                        dateMillis = dayMillis,
                        account = account,
                        tags = tags,
                        reimbursable = reimbursable,
                        currency = currency,
                        foreignAmountCents = foreignAmountCents,
                        rateScaled = rateScaled
                    )
                }
                sheetOpen = false
                editing = null
            }
        )
    }
}

/**
 * 内容区：日历视图 + 流水列表，**同一个 LazyColumn**（内容区里唯一的滚动面）。
 *
 * 这一版把日历挪进 LazyColumn 当第一个 item。
 * 之前日历是 LazyColumn 的兄弟节点，上面那几张固定高度的汇总 / 筛选卡加上日历正好占满一屏，
 * 列表就被挤成 0 高度，而外层 Column 又不会滚 —— 于是「打开日历筛选后不能上下滑动」。
 * 现在整屏只有一个滚动面：日历、当天筛选条、每天的流水都在里面，多余的内容一律靠滚动看到，
 * 没有任何一块被裁掉，也没有谁再需要 weight(1f) 去抢高度。
 */
@Composable
private fun LedgerList(
    state: UiState,
    vm: MainViewModel,
    view: LedgerView,
    lang: Lang,
    onEdit: (TransactionEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // 底部留出 FAB 的空间，最后一行不会被悬浮按钮压住
        contentPadding = PaddingValues(bottom = 104.dp)
    ) {
        if (view == LedgerView.CALENDAR) {
            item(key = "month-calendar") {
                Column(Modifier.fillMaxWidth()) {
                    MonthCalendar(
                        days = state.calendarDays,
                        maxExpense = state.maxCalendarExpense,
                        selected = state.dayFilter,
                        lang = lang,
                        onPick = { date -> vm.setDayFilter(date) }
                    )
                    HintText(
                        text = LedgerStrings.calendarScrollHint(lang),
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 6.dp)
                    )
                }
            }
        }

        // 点过日历里的某一天才出现：说明当前按哪一天筛，✕ 或再点同一天取消
        if (state.dayFilter != null) {
            item(key = "day-filter") { DayFilterCard(state = state, vm = vm) }
        }

        if (!state.hasMonthData) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyHint(
                        emoji = when {
                            state.isSearching -> "🔍"
                            state.tagFilter != null -> "🏷️"
                            state.dayFilter != null -> "📅"
                            state.accountFilter != null -> "💳"
                            else -> "🧾"
                        },
                        title = when {
                            state.isSearching -> LedgerStrings.emptySearchTitle(lang)
                            state.isLedgerFiltered -> LedgerStrings.emptyNoMatchTitle(lang)
                            else -> LedgerStrings.emptyMonthTitle(lang)
                        },
                        subtitle = when {
                            state.isSearching -> LedgerStrings.emptySearchSubtitle(lang)
                            state.isLedgerFiltered -> LedgerStrings.emptyNoMatchHint(lang)
                            else -> LedgerStrings.emptyMonthSubtitle(lang)
                        }
                    )
                }
            }
        } else {
            state.monthGroups.forEach { group ->
                item(key = "day-${group.date}") { DayHeader(group) }
                items(items = group.items, key = { it.id }) { tx ->
                    TransactionRow(
                        tx = tx,
                        onEdit = { onEdit(tx) },
                        onDelete = { vm.deleteTransaction(tx) },
                        onToggleReimbursed = { vm.toggleReimbursed(tx) }
                    )
                }
            }
        }
    }
}

/** 账户筛选条：原有行为不变，只把它挪成独立组件 */
@Composable
private fun AccountFilterRow(
    state: UiState,
    vm: MainViewModel,
    lang: Lang
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = state.accountFilter == null,
            onClick = { vm.setAccountFilter(null) },
            label = { Text(LedgerStrings.accountFilterAll(lang)) }
        )
        state.accounts.forEach { account ->
            FilterChip(
                selected = state.accountFilter == account,
                onClick = {
                    vm.setAccountFilter(
                        if (state.accountFilter == account) null else account
                    )
                },
                label = { Text("${Accounts.emojiOf(account)} $account") }
            )
        }
    }
}

/** 标签筛选条：「全部」+ 数据里用过的标签，点一下按标签筛 */
@Composable
private fun TagFilterRow(
    state: UiState,
    vm: MainViewModel,
    lang: Lang
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = state.tagFilter == null,
            onClick = { vm.setTagFilter(null) },
            label = { Text(LedgerStrings.tagFilterAll(lang)) }
        )
        state.allTags.forEach { tag ->
            FilterChip(
                selected = state.tagFilter == tag,
                onClick = { vm.setTagFilter(tag) },
                label = { Text("#$tag") }
            )
        }
        val active = state.tagFilter
        if (active != null && active !in state.allTags) {
            FilterChip(
                selected = true,
                onClick = { vm.setTagFilter(active) },
                label = { Text("#$active") }
            )
        }
    }
}

/** 列表 / 日历 切换 */
@Composable
private fun LedgerViewToggle(
    view: LedgerView,
    onSelect: (LedgerView) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = view == LedgerView.LIST,
            onClick = { onSelect(LedgerView.LIST) },
            leadingIcon = { Icon(Icons.Filled.List, contentDescription = null) },
            label = { Text(LedgerStrings.viewList(lang)) }
        )
        FilterChip(
            selected = view == LedgerView.CALENDAR,
            onClick = { onSelect(LedgerView.CALENDAR) },
            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            label = { Text(LedgerStrings.viewCalendar(lang)) }
        )
    }
}

/** 当天筛选的小卡片，点 ✕ 取消（再点日历里同一天也会取消） */
@Composable
private fun DayFilterCard(state: UiState, vm: MainViewModel) {
    val lang = LocalLang.current
    val day = state.dayFilter ?: return
    val target = state.monthGroups.firstOrNull { it.date == day }

    SectionCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = AppStrings.monthDay(lang, day.monthValue, day.dayOfMonth),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = LedgerStrings.dayFilterSummary(
                        lang,
                        target?.items?.size ?: 0,
                        formatAmount(target?.expense ?: 0L),
                        formatAmount(target?.income ?: 0L)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { vm.setDayFilter(day) }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = LedgerStrings.clearDayFilter(lang),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReimbursementCard(state: UiState) {
    val lang = LocalLang.current
    SectionCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = LedgerStrings.pendingReimbursement(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "¥${formatAmount(state.pendingReimbursementCents)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = LedgerStrings.reimbursed(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "¥${formatAmount(state.reimbursedCents)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
    val lang = LocalLang.current

    SectionCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = LedgerStrings.balanceThisMonth(lang),
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
            StatBlock(AppStrings.txExpense(lang), "¥${formatAmount(expense)}", expenseColor(), Modifier.weight(1f))
            StatBlock(AppStrings.txIncome(lang), "¥${formatAmount(income)}", incomeColor(), Modifier.weight(1f))
            StatBlock(
                LedgerStrings.statTodayExpense(lang),
                "¥${formatAmount(todayExpense)}",
                MaterialTheme.colorScheme.onSurface,
                Modifier.weight(1f)
            )
        }
        if (count > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = LedgerStrings.monthCount(lang, count),
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
    val lang = LocalLang.current
    val accent = if (overBudget) expenseColor() else MaterialTheme.colorScheme.primary

    SectionCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LedgerStrings.budgetThisMonth(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (overBudget) LedgerStrings.overBudget(lang, formatAmount(expense - budget))
                else LedgerStrings.budgetRemaining(lang, formatAmount(remaining)),
                style = MaterialTheme.typography.bodyMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(10.dp))
        ThinProgressBar(ratio = ratio, color = accent)
        Spacer(Modifier.height(8.dp))
        Text(
            text = LedgerStrings.budgetUsed(lang, formatAmount(expense), formatAmount(budget)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 日期分组标题（今天 / 昨天 / 9月13日 周六）。
 *
 * internal 而不是 private：统计详情页的「本月记录 / 每日明细」也用同一套日期文案与排版，
 * 这样两个月度列表看起来是一回事。
 */
@Composable
internal fun DayHeader(group: DayGroup) {
    val lang = LocalLang.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDateHeader(group.date, lang),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        val parts = buildList {
            if (group.expense > 0) add(LedgerStrings.statSpent(lang, formatAmount(group.expense)))
            if (group.income > 0) add(LedgerStrings.statReceived(lang, formatAmount(group.income)))
        }
        Text(
            text = parts.joinToString("  "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 流水行：记账页里可编辑 / 可删除，统计详情页里是只读的。
 * [readOnly] 为 true 时不显示删除按钮、报销徽标也不可点，只保留排版（分类徽标、账户、备注、标签、金额）。
 * 不传 [onClick] 时用 [onEdit] 作为整行的点击行为。
 */
@Composable
internal fun TransactionRow(
    tx: TransactionEntity,
    readOnly: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onToggleReimbursed: () -> Unit = {},
    onClick: () -> Unit = onEdit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val lang = LocalLang.current
    val isExpense = tx.type == TxType.EXPENSE
    val amountColor = if (isExpense) expenseColor() else incomeColor()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, Shapes.card)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(category = tx.category, emoji = Categories.emojiOf(tx.category))

        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tx.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (tx.reimbursable) {
                    Spacer(Modifier.width(6.dp))
                    ReimbursementBadge(
                        reimbursed = tx.reimbursed,
                        clickable = !readOnly,
                        onToggle = onToggleReimbursed
                    )
                }
            }
            val subtitle = buildString {
                if (tx.account != Accounts.DEFAULT) append("${Accounts.emojiOf(tx.account)} ${tx.account}")
                if (tx.note.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(tx.note)
                }
                tx.tagList.forEach { tag ->
                    if (isNotEmpty()) append(" · ")
                    append("#$tag")
                }
                if (tx.dateMillis.toLocalDate() != LocalDate.now()) {
                    if (isNotEmpty()) append(" · ")
                    append(AppStrings.monthDay(lang, tx.dateMillis.toLocalDate().monthValue, tx.dateMillis.toLocalDate().dayOfMonth))
                }
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (isExpense) "-" else "+") + "¥${formatAmount(tx.amountCents)}",
                style = MaterialTheme.typography.titleMedium,
                color = amountColor,
                maxLines = 1
            )
            if (tx.isForeign) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = LedgerStrings.convertedFrom(
                        lang,
                        Currencies.symbolOf(tx.currency),
                        formatAmount(tx.foreignAmountCents),
                        tx.currency
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        if (!readOnly) {
            IconButton(onClick = { confirmDelete = true }) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = AppStrings.delete(lang),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (confirmDelete && !readOnly) {
        ConfirmDialog(
            title = LedgerStrings.deleteTitle(lang),
            text = LedgerStrings.deleteBody(lang, tx.category, formatAmount(tx.amountCents), tx.note),
            confirmText = AppStrings.delete(lang),
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
    }
}

/**
 * 待报销 / 已报销 小徽标：在记账页自己是一个可点区域，不会触发整行的编辑；
 * 统计详情页里只做展示（clickable = false），点它不会改任何数据。
 */
@Composable
private fun ReimbursementBadge(
    reimbursed: Boolean,
    clickable: Boolean = true,
    onToggle: () -> Unit
) {
    val lang = LocalLang.current
    val text = if (reimbursed) LedgerStrings.reimbursed(lang) else LedgerStrings.pendingReimbursement(lang)
    val accent = if (reimbursed) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.primary

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = accent,
        maxLines = 1,
        modifier = Modifier
            .clip(Shapes.badge)
            .then(if (clickable) Modifier.clickable { onToggle() } else Modifier)
            .background(accent.copy(alpha = 0.12f), Shapes.badge)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** 月日历：7 列网格，颜色深浅表示当天支出，点格子按天筛 */
@Composable
private fun MonthCalendar(
    days: List<CalendarDay>,
    maxExpense: Long,
    selected: LocalDate?,
    lang: Lang,
    onPick: (LocalDate) -> Unit
) {
    if (days.isEmpty()) return

    val lead = days.first().date.dayOfWeek.value - 1
    val cells: List<CalendarDay?> = List(lead) { null } + days
    val weeks = cells.chunked(7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            for (index in 0 until 7) {
                Text(
                    text = AppStrings.weekday(lang, index),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        weeks.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        CalendarCell(
                            day = day,
                            maxExpense = maxExpense,
                            selected = selected == day.date,
                            lang = lang,
                            onPick = onPick
                        )
                    }
                }
                // 最后一周补齐空位，保证每格宽度一致
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarCell(
    day: CalendarDay,
    maxExpense: Long,
    selected: Boolean,
    lang: Lang,
    onPick: (LocalDate) -> Unit
) {
    val ratio = if (maxExpense <= 0L) 0f
    else (day.expenseCents.toFloat() / maxExpense.toFloat()).coerceIn(0f, 1f)
    val fill = when {
        day.expenseCents > 0L -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f + 0.55f * ratio)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 2.dp)
            .height(44.dp)
            .clip(Shapes.badge)
            .background(fill, Shapes.badge)
            .then(
                if (selected) Modifier.border(
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    Shapes.badge
                ) else Modifier
            )
            .clickable { onPick(day.date) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (day.date.dayOfMonth == 1 || day.date.dayOfWeek.value == 7) {
                    AppStrings.monthDay(lang, day.date.monthValue, day.date.dayOfMonth)
                } else {
                    day.date.dayOfMonth.toString()
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = compactAmount(day.expenseCents),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
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
    knownAccounts: List<String>,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    knownTags: List<String>,
    rateOf: (String) -> Long,
    onRateChange: (String, Long) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        cents: Long,
        type: TxType,
        category: String,
        note: String,
        dayMillis: Long,
        account: String,
        tags: List<String>,
        reimbursable: Boolean,
        currency: String,
        foreignCents: Long,
        rateScaled: Long
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember(editing) { mutableStateOf(editing?.type ?: TxType.EXPENSE) }
    var currency by remember(editing) { mutableStateOf(editing?.currency ?: Currencies.BASE) }
    var rateText by remember(editing) {
        val code = editing?.currency ?: Currencies.BASE
        mutableStateOf(
            if (code == Currencies.BASE) "" else rateToText(editing?.rateScaled ?: rateOf(code))
        )
    }
    var amountText by remember(editing) {
        mutableStateOf(
            editing?.let { item ->
                if (item.isForeign && item.foreignAmountCents > 0L) formatAmount(item.foreignAmountCents)
                else formatAmount(item.amountCents)
            } ?: ""
        )
    }
    var category by remember(editing) {
        mutableStateOf(editing?.category ?: Categories.EXPENSE.first())
    }
    var account by remember(editing) { mutableStateOf(editing?.account ?: Accounts.DEFAULT) }
    var note by remember(editing) { mutableStateOf(editing?.note ?: "") }
    var tagsText by remember(editing) { mutableStateOf(editing?.tagList?.joinToString(" ") ?: "") }
    var reimbursable by remember(editing) { mutableStateOf(editing?.reimbursable ?: false) }
    var selectedDate by remember(editing) {
        mutableStateOf(editing?.dateMillis?.toLocalDate() ?: LocalDate.now())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCustomAccount by remember { mutableStateOf(false) }

    // 预置账户 + 数据里出现过的账户（比如从备份恢复进来的自定义账户）
    val accountOptions = remember(knownAccounts, account) {
        (Accounts.PRESETS + knownAccounts + account).distinct()
    }

    // 分类清单来自用户自己的设置 + 数据里用过的分类，不再只用内置预置
    val categories = categoryOptions(
        type = type,
        expense = expenseCategories,
        income = incomeCategories
    )
    val tagSuggestions = remember(knownTags, tagsText) {
        val used = parseTagInput(tagsText).toSet()
        knownTags.filter { it !in used }.take(12)
    }

    val cents = parseAmountToCents(amountText)
    val isForeign = currency != Currencies.BASE
    val rateScaled = if (isForeign) rateToScaled(rateText) else Currencies.RATE_SCALE
    val baseCents = if (cents == null) null
    else if (isForeign) Currencies.toBaseCents(cents, rateScaled)
    else cents
    val today = LocalDate.now()
    val lang = LocalLang.current

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
                text = if (editing == null) LedgerStrings.addEntry(lang) else LedgerStrings.editEntry(lang),
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
                            val list = categoryOptions(t, expenseCategories, incomeCategories)
                            if (category !in list) category = list.first()
                        },
                        label = { Text(t.label(lang)) }
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
                label = {
                    Text(
                        if (isForeign) LedgerStrings.amountForeignLabel(lang, currency)
                        else LedgerStrings.amountLabel(lang)
                    )
                },
                prefix = { Text(Currencies.symbolOf(currency)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            FieldLabel(LedgerStrings.currencyLabel(lang))
            Spacer(Modifier.height(8.dp))
            ChipFlow {
                Currencies.PRESETS.forEach { code ->
                    FilterChip(
                        selected = currency == code,
                        onClick = {
                            currency = code
                            if (code != Currencies.BASE) rateText = rateToText(rateOf(code))
                        },
                        label = { Text("${Currencies.symbolOf(code)} $code") }
                    )
                }
            }

            if (isForeign) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { input ->
                        if (input.count { it == '.' } <= 1 &&
                            input.all { it.isDigit() || it == '.' } &&
                            input.length <= 9
                        ) {
                            rateText = input
                        }
                    },
                    label = { Text(LedgerStrings.rateLabel(lang, currency)) },
                    supportingText = {
                        Text(LedgerStrings.rateHint(lang, Currencies.symbolOf(currency), currency))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (cents != null && baseCents != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = LedgerStrings.rateConverted(lang, formatAmount(baseCents)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            FieldLabel(LedgerStrings.categoryLabel(lang))
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
            FieldLabel(LedgerStrings.dateLabel(lang))
            Spacer(Modifier.height(8.dp))
            ChipFlow {
                FilterChip(
                    selected = selectedDate == today,
                    onClick = { selectedDate = today },
                    label = { Text(AppStrings.today(lang)) }
                )
                FilterChip(
                    selected = selectedDate == today.minusDays(1),
                    onClick = { selectedDate = today.minusDays(1) },
                    label = { Text(AppStrings.yesterday(lang)) }
                )
                FilterChip(
                    selected = selectedDate == today.minusDays(2),
                    onClick = { selectedDate = today.minusDays(2) },
                    label = { Text(AppStrings.dayBeforeYesterday(lang)) }
                )
                FilterChip(
                    selected = selectedDate != today &&
                        selectedDate != today.minusDays(1) &&
                        selectedDate != today.minusDays(2),
                    onClick = { showDatePicker = true },
                    label = {
                        Text(
                            if (selectedDate == today) LedgerStrings.pickDate(lang)
                            else AppStrings.monthDay(lang, selectedDate.monthValue, selectedDate.dayOfMonth)
                        )
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            FieldLabel(LedgerStrings.accountLabel(lang))
            Spacer(Modifier.height(8.dp))
            ChipFlow {
                accountOptions.forEach { item ->
                    FilterChip(
                        selected = account == item,
                        onClick = { account = item },
                        label = { Text("${Accounts.emojiOf(item)} $item") }
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { showCustomAccount = true },
                    label = { Text(LedgerStrings.customAccountChip(lang)) }
                )
            }

            Spacer(Modifier.height(16.dp))
            FieldLabel(LedgerStrings.tagsLabel(lang))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = tagsText,
                onValueChange = { if (it.length <= 60) tagsText = it },
                placeholder = { Text(LedgerStrings.tagsHint(lang)) },
                supportingText = { Text(LedgerStrings.tagsTip(lang)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
            if (tagSuggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ChipFlow {
                    tagSuggestions.forEach { tag ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val merged = (parseTagInput(tagsText) + tag).distinct()
                                tagsText = merged.joinToString(" ")
                            },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 40) note = it },
                label = { Text(LedgerStrings.noteOptional(lang)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))
            LabeledSwitch(
                title = LedgerStrings.reimbursableLabel(lang),
                checked = reimbursable,
                onChange = { reimbursable = it }
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val foreignValue = parseAmountToCents(amountText) ?: return@Button
                    val rate = if (isForeign) rateToScaled(rateText) else Currencies.RATE_SCALE
                    val base = if (isForeign) Currencies.toBaseCents(foreignValue, rate) else foreignValue
                    if (isForeign) onRateChange(currency, rate)
                    onSave(
                        base,
                        type,
                        category,
                        note.trim(),
                        selectedDate.toDayMillis(),
                        account,
                        parseTagInput(tagsText),
                        reimbursable,
                        currency,
                        if (isForeign) foreignValue else base,
                        rate
                    )
                },
                enabled = baseCents != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = Shapes.pill
            ) {
                Text(
                    text = when {
                        baseCents == null -> LedgerStrings.enterAmount(lang)
                        editing == null -> LedgerStrings.saveAmount(lang, formatAmount(baseCents))
                        else -> LedgerStrings.saveChangesAmount(lang, formatAmount(baseCents))
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showCustomAccount) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomAccount = false },
            title = { Text(LedgerStrings.customAccountTitle(lang)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { if (it.length <= 10) text = it },
                        label = { Text(LedgerStrings.accountNameLabel(lang)) },
                        placeholder = { Text(LedgerStrings.accountNamePlaceholder(lang)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = LedgerStrings.accountNameHint(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = text.trim()
                    if (name.isNotEmpty()) account = name
                    showCustomAccount = false
                }) { Text(AppStrings.confirm(lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomAccount = false }) { Text(AppStrings.cancel(lang)) }
            }
        )
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
                }) { Text(AppStrings.confirm(lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(AppStrings.cancel(lang)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
