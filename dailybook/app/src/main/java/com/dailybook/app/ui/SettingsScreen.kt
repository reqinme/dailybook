package com.dailybook.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailybook.app.CategoryBudgetRow
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.backup.Backup
import com.dailybook.app.data.Accounts
import com.dailybook.app.data.Categories
import com.dailybook.app.data.RecurringEntity
import com.dailybook.app.data.RepeatRule
import com.dailybook.app.data.SummaryMode
import com.dailybook.app.data.TxType
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LedgerStrings
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.SettingsStrings
import com.dailybook.app.i18n.TodoStrings
import com.dailybook.app.timer.TimerViewModel
import com.dailybook.app.ui.theme.ThemeMode
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.formatDueLabel
import com.dailybook.app.util.parseAmountToCents
import com.dailybook.app.util.toDayMillis
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** 关于卡片里的版本号，「日常本 v1.4」里的 1.4 由它拼出来 */
private const val APP_VERSION = "1.4"

private enum class ClearTarget {
    TRANSACTIONS,
    TODOS,
    FOCUS_STATS,
    EVERYTHING
}

/** 清除项的按钮文案 */
private fun ClearTarget.label(lang: Lang): String = when (this) {
    ClearTarget.TRANSACTIONS -> SettingsStrings.clearAllRecords(lang)
    ClearTarget.TODOS -> SettingsStrings.clearAllTodos(lang)
    ClearTarget.FOCUS_STATS -> SettingsStrings.clearFocusStats(lang)
    ClearTarget.EVERYTHING -> SettingsStrings.clearEverything(lang)
}

/** 清除项的二次确认说明 */
private fun ClearTarget.message(lang: Lang): String = when (this) {
    ClearTarget.TRANSACTIONS -> SettingsStrings.clearAllRecordsMessage(lang)
    ClearTarget.TODOS -> SettingsStrings.clearAllTodosMessage(lang)
    ClearTarget.FOCUS_STATS -> SettingsStrings.clearFocusStatsMessage(lang)
    ClearTarget.EVERYTHING -> SettingsStrings.clearEverythingMessage(lang)
}

@Composable
fun SettingsScreen(
    state: UiState,
    vm: MainViewModel,
    timerVm: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val themeMode by vm.settings.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by vm.settings.dynamicColor.collectAsStateWithLifecycle()
    val currentLang by vm.settings.lang.collectAsStateWithLifecycle()
    val ledgerReminder by vm.settings.ledgerReminderEnabled.collectAsStateWithLifecycle()
    val reminderHour by vm.settings.ledgerReminderHour.collectAsStateWithLifecycle()
    val reminderMinute by vm.settings.ledgerReminderMinute.collectAsStateWithLifecycle()
    val summaryMode by vm.settings.summaryMode.collectAsStateWithLifecycle()
    val budgetAlert by vm.settings.budgetAlert.collectAsStateWithLifecycle()
    val focusGoal by vm.settings.focusGoal.collectAsStateWithLifecycle()

    var clearing by remember { mutableStateOf<ClearTarget?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetText by remember { mutableStateOf("") }
    var confirmImport by remember { mutableStateOf(false) }
    var confirmCsvImport by remember { mutableStateOf(false) }
    var showCategoryBudget by remember { mutableStateOf(false) }
    var showCategoryManage by remember { mutableStateOf(false) }
    var showRecurring by remember { mutableStateOf(false) }
    var showReminderTime by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val message by vm.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.consumeMessage()
        }
    }

    // 文件选择器：导出备份 / 导出 CSV 用「新建文件」，恢复备份 / 导入 CSV 用「打开文件」。
    // 位置由用户在系统界面里挑，所以 App 不需要任何存储权限。
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportBackup(it) } }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { vm.exportLedgerCsv(it) } }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importBackup(it) } }

    // CSV 导入跟恢复备份一样用「打开文件」：先弹二次确认，再让用户挑 csv。
    // 文件里的记录与现有记录完全相同时会跳过，重复导入不会翻倍。
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importLedgerCsv(it) } }

    val timerState by timerVm.state.collectAsStateWithLifecycle()
    val focusSettings = timerState.settings

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = SettingsStrings.title(lang),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))

        SectionCard(title = SettingsStrings.sectionAppearance(lang)) {
            FieldLabel(SettingsStrings.theme(lang))
            Spacer(Modifier.height(8.dp))
            ChipFlow {
                ThemeChip(SettingsStrings.themeSystem(lang), ThemeMode.SYSTEM, themeMode) { vm.setThemeMode(it) }
                ThemeChip(SettingsStrings.themeLight(lang), ThemeMode.LIGHT, themeMode) { vm.setThemeMode(it) }
                ThemeChip(SettingsStrings.themeDark(lang), ThemeMode.DARK, themeMode) { vm.setThemeMode(it) }
            }
            Spacer(Modifier.height(10.dp))
            LabeledSwitch(SettingsStrings.dynamicColor(lang), dynamicColor) { vm.setDynamicColor(it) }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = AppStrings.language(lang)) {
            ChipFlow {
                Lang.entries.forEach { entry ->
                    FilterChip(
                        selected = currentLang == entry,
                        onClick = { vm.setLang(entry) },
                        // 语言名是原文名（Lang.label），属于数据，不翻译
                        label = { Text(entry.label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = AppStrings.languageHint(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = SettingsStrings.sectionLedger(lang)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(SettingsStrings.monthlyBudget(lang), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (state.hasBudget) "¥${formatAmount(state.budgetCents)}"
                        else SettingsStrings.budgetNotSet(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = {
                    budgetText = if (state.hasBudget) formatAmount(state.budgetCents) else ""
                    showBudgetDialog = true
                }) { Text(SettingsStrings.setMonthlyBudget(lang)) }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(SettingsStrings.categoryBudget(lang), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (state.hasCategoryBudget) {
                            val over = state.categoryBudgets.count { it.over }
                            SettingsStrings.categoryBudgetCount(lang, state.categoryBudgets.size) +
                                if (over > 0) " · " + SettingsStrings.categoryBudgetOver(lang, over) else ""
                        } else {
                            SettingsStrings.categoryBudgetHint(lang)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showCategoryBudget = true }) { Text(AppStrings.manage(lang)) }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(SettingsStrings.categoryManage(lang), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = SettingsStrings.categoryManageSubtitle(
                            lang,
                            state.expenseCategories.size,
                            state.incomeCategories.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showCategoryManage = true }) { Text(AppStrings.manage(lang)) }
            }

            Spacer(Modifier.height(4.dp))
            // 周期记账：房租、订阅这类固定支出，到日子自动记一笔
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(AppStrings.recurringTitle(lang), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (state.recurring.isEmpty()) {
                            AppStrings.recurringEmpty(lang)
                        } else {
                            SettingsStrings.recurringCount(lang, state.recurring.size)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showRecurring = true }) { Text(AppStrings.manage(lang)) }
            }

            Spacer(Modifier.height(4.dp))
            LabeledSwitch(SettingsStrings.nightlyLedgerReminder(lang), ledgerReminder) { vm.setLedgerReminder(it) }
            if (ledgerReminder) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(SettingsStrings.reminderTime(lang), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { showReminderTime = true }) {
                        Text("%02d:%02d".format(reminderHour, reminderMinute))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            LabeledSwitch(AppStrings.budgetAlert(lang), budgetAlert) { vm.setBudgetAlert(it) }

            Spacer(Modifier.height(10.dp))
            Text(AppStrings.summaryLabel(lang), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            ChipFlow {
                SummaryMode.entries.forEach { mode ->
                    FilterChip(
                        selected = summaryMode == mode,
                        onClick = { vm.setSummaryMode(mode) },
                        label = { Text(mode.label(lang)) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = AppStrings.summaryHint(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = SettingsStrings.sectionFocus(lang)) {
            LabeledSlider(
                label = SettingsStrings.focusDuration(lang),
                value = focusSettings.focusMinutes,
                range = 5..90,
                valueText = SettingsStrings.minutesTemplate(lang),
                onChange = { timerVm.setFocusMinutes(it) }
            )
            LabeledSlider(
                label = SettingsStrings.shortBreak(lang),
                value = focusSettings.shortBreakMinutes,
                range = 1..30,
                valueText = SettingsStrings.minutesTemplate(lang),
                onChange = { timerVm.setShortBreakMinutes(it) }
            )
            LabeledSlider(
                label = SettingsStrings.longBreak(lang),
                value = focusSettings.longBreakMinutes,
                range = 5..45,
                valueText = SettingsStrings.minutesTemplate(lang),
                onChange = { timerVm.setLongBreakMinutes(it) }
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(SettingsStrings.longBreakEveryLabel(lang), style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = {
                            timerVm.setLongBreakEvery(
                                (focusSettings.longBreakEvery - 1).coerceAtLeast(2)
                            )
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Filled.Remove,
                            contentDescription = SettingsStrings.decrease(lang),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = SettingsStrings.longBreakEveryValue(lang, focusSettings.longBreakEvery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    FilledTonalIconButton(
                        onClick = {
                            timerVm.setLongBreakEvery(
                                (focusSettings.longBreakEvery + 1).coerceAtMost(8)
                            )
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = SettingsStrings.increase(lang),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LabeledSwitch(SettingsStrings.vibrateOnPhaseEnd(lang), focusSettings.vibrate) { timerVm.setVibrate(it) }
            LabeledSwitch(SettingsStrings.autoStartNext(lang), focusSettings.autoStartNext) { timerVm.setAutoStart(it) }
            LabeledSwitch(SettingsStrings.keepScreenOn(lang), focusSettings.keepScreenOn) { timerVm.setKeepScreenOn(it) }

            Spacer(Modifier.height(10.dp))
            Text(AppStrings.focusGoalLabel(lang), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            ChipFlow {
                listOf(0, 1, 2, 3, 4, 6, 8).forEach { count ->
                    FilterChip(
                        selected = focusGoal == count,
                        onClick = { vm.setFocusGoal(count) },
                        label = {
                            Text(
                                if (count == 0) AppStrings.focusGoalOff(lang)
                                else AppStrings.focusGoalValue(lang, count)
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (focusGoal > 0) {
                    AppStrings.focusGoalHint(lang)
                } else {
                    AppStrings.focusGoalOff(lang)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = SettingsStrings.sectionData(lang)) {
            Text(
                text = SettingsStrings.ledgerSummary(
                    lang,
                    state.monthCount,
                    state.pendingCount + state.doneCount,
                    state.focusStats.todayCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Text(SettingsStrings.backupAndExport(lang), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        backupLauncher.launch(
                            Backup.suggestName(AppStrings.backupFileName(lang), "json")
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(SettingsStrings.exportBackup(lang)) }
                OutlinedButton(
                    onClick = { confirmImport = true },
                    modifier = Modifier.weight(1f)
                ) { Text(SettingsStrings.restoreBackup(lang)) }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    csvLauncher.launch(
                        Backup.suggestName(AppStrings.ledgerFileName(lang), "csv")
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(SettingsStrings.exportCsv(lang)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { confirmCsvImport = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(AppStrings.importCsv(lang)) }
            Spacer(Modifier.height(6.dp))
            Text(
                text = AppStrings.importCsvHint(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = SettingsStrings.backupExplain1(lang) + SettingsStrings.backupExplain2(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Text(SettingsStrings.clearData(lang), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            ClearTarget.entries.forEach { target ->
                OutlinedButton(
                    onClick = { clearing = target },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(target.label(lang)) }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard {
            Text(
                text = SettingsStrings.aboutVersion(lang, APP_VERSION),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = SettingsStrings.aboutText(lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(28.dp))
    }

    clearing?.let { target ->
        ConfirmDialog(
            title = SettingsStrings.confirmClearTitle(lang, target.label(lang)),
            text = target.message(lang),
            confirmText = SettingsStrings.confirmClear(lang),
            onConfirm = {
                when (target) {
                    ClearTarget.TRANSACTIONS -> vm.clearTransactions()
                    ClearTarget.TODOS -> vm.clearTodos()
                    ClearTarget.FOCUS_STATS -> vm.clearFocusStats()
                    ClearTarget.EVERYTHING -> vm.clearAll()
                }
            },
            onDismiss = { clearing = null }
        )
    }

    if (confirmImport) {
        ConfirmDialog(
            title = SettingsStrings.restoreFromBackupTitle(lang),
            text = SettingsStrings.restoreConfirmText(lang),
            confirmText = SettingsStrings.pickBackupFile(lang),
            onConfirm = {
                confirmImport = false
                restoreLauncher.launch(arrayOf("application/json", "*/*"))
            },
            onDismiss = { confirmImport = false }
        )
    }

    if (confirmCsvImport) {
        ConfirmDialog(
            title = SettingsStrings.importCsvTitle(lang),
            text = SettingsStrings.importCsvConfirmText(lang),
            confirmText = SettingsStrings.pickCsvFile(lang),
            onConfirm = {
                confirmCsvImport = false
                csvImportLauncher.launch(
                    arrayOf(
                        "text/csv",
                        "text/comma-separated-values",
                        "application/vnd.ms-excel",
                        "*/*"
                    )
                )
            },
            onDismiss = { confirmCsvImport = false }
        )
    }

    if (showCategoryManage) {
        CategoryManageDialog(
            expenseCategories = state.expenseCategories,
            incomeCategories = state.incomeCategories,
            onAdd = { type, name -> vm.addCategory(type, name) },
            onRemove = { type, name -> vm.removeCategory(type, name) },
            onReset = { type -> vm.resetCategories(type) },
            onDismiss = { showCategoryManage = false }
        )
    }

    if (showCategoryBudget) {
        CategoryBudgetDialog(
            budgets = state.categoryBudgets.associate { it.category to it.budgetCents },
            onSet = { category, cents -> vm.setCategoryBudget(category, cents) },
            onDismiss = { showCategoryBudget = false }
        )
    }

    if (showRecurring) {
        RecurringDialog(
            items = state.recurring,
            expenseCategories = state.expenseCategories,
            incomeCategories = state.incomeCategories,
            onAdd = { amountCents, type, category, account, note, rule, nextDueMillis ->
                vm.addRecurring(
                    amountCents = amountCents,
                    type = type,
                    category = category,
                    account = account,
                    note = note,
                    tags = emptyList(),
                    rule = rule,
                    nextDueMillis = nextDueMillis
                )
            },
            onToggle = { vm.toggleRecurring(it) },
            onDelete = { vm.deleteRecurring(it) },
            onDismiss = { showRecurring = false }
        )
    }

    if (showReminderTime) {
        ReminderTimeDialog(
            hour = reminderHour,
            minute = reminderMinute,
            onConfirm = { h, m -> vm.setLedgerReminderTime(h, m) },
            onDismiss = { showReminderTime = false }
        )
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text(SettingsStrings.setMonthlyBudget(lang)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { input ->
                            if (input.count { it == '.' } <= 1 &&
                                input.all { it.isDigit() || it == '.' } &&
                                input.length <= 10
                            ) {
                                budgetText = input
                            }
                        },
                        label = { Text(SettingsStrings.amount(lang)) },
                        prefix = { Text("¥") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = SettingsStrings.monthlyBudgetExplain(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setMonthlyBudget(parseAmountToCents(budgetText) ?: 0L)
                    showBudgetDialog = false
                }) { Text(AppStrings.save(lang)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        vm.setMonthlyBudget(0L)
                        showBudgetDialog = false
                    }) { Text(AppStrings.clear(lang)) }
                    TextButton(onClick = { showBudgetDialog = false }) { Text(AppStrings.cancel(lang)) }
                }
            }
        )
    }
}

@Composable
private fun ThemeChip(
    label: String,
    mode: ThemeMode,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    FilterChip(
        selected = current == mode,
        onClick = { onSelect(mode) },
        label = { Text(label) }
    )
}

/** 分类预算：逐个支出分类填上限，留空表示不限 */
@Composable
private fun CategoryBudgetDialog(
    budgets: Map<String, Long>,
    onSet: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalLang.current
    val texts = remember(budgets) {
        mutableStateMapOf<String, String>().apply {
            Categories.EXPENSE.forEach { category ->
                put(category, budgets[category]?.let { formatAmount(it) } ?: "")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(SettingsStrings.categoryBudget(lang)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = SettingsStrings.categoryBudgetExplain(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Categories.EXPENSE.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 分类名是数据，不翻译
                        Text(
                            text = "${Categories.emojiOf(category)} $category",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = texts[category].orEmpty(),
                            onValueChange = { input ->
                                if (input.length <= 8 &&
                                    input.count { it == '.' } <= 1 &&
                                    input.all { it.isDigit() || it == '.' }
                                ) {
                                    texts[category] = input
                                }
                            },
                            prefix = { Text("¥") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(124.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Categories.EXPENSE.forEach { category ->
                    onSet(category, parseAmountToCents(texts[category].orEmpty()) ?: 0L)
                }
                onDismiss()
            }) { Text(AppStrings.save(lang)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel(lang)) }
        }
    )
}

/** 记账提醒时间选择 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    hour: Int,
    minute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalLang.current
    val pickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(SettingsStrings.reminderTimeTitle(lang)) },
        text = {
            Column {
                // 常用时段一键选，省得在小屏上拨表盘
                ChipFlow {
                    listOf(20 to 0, 21 to 0, 22 to 0).forEach { (h, m) ->
                        FilterChip(
                            selected = pickerState.hour == h && pickerState.minute == m,
                            onClick = {
                                pickerState.hour = h
                                pickerState.minute = m
                            },
                            label = { Text("%02d:%02d".format(h, m)) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                TimePicker(state = pickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(pickerState.hour, pickerState.minute)
                onDismiss()
            }) { Text(AppStrings.confirm(lang)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel(lang)) }
        }
    )
}

/**
 * 分类管理：支出 / 收入各一节，可以逐个删、逐个加，也可以一键回到预置分类。
 *
 * 删掉的只是「选择器里的一项」：老记录上的分类名照旧保留，也还能从记录里选回来，
 * 所以这里不需要任何「确认删除」的二次弹窗。
 */
@Composable
private fun CategoryManageDialog(
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    onAdd: (TxType, String) -> Boolean,
    onRemove: (TxType, String) -> Unit,
    onReset: (TxType) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalLang.current
    val drafts = remember { mutableStateMapOf<TxType, String>() }
    val rejected = remember { mutableStateMapOf<TxType, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(SettingsStrings.categoryManage(lang)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // 分类名是数据（要么预置、要么用户自己起的），不翻译
                TxType.entries.forEach { type ->
                    val categories = if (type == TxType.EXPENSE) expenseCategories else incomeCategories

                    Text(type.label(lang), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    ChipFlow {
                        categories.forEach { category ->
                            FilterChip(
                                selected = false,
                                // 点标签本身或点末尾的 ✕ 都是删除
                                onClick = { onRemove(type, category) },
                                label = { Text(category) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = SettingsStrings.removeCategoryLabel(lang, category),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = drafts[type].orEmpty(),
                            onValueChange = { input ->
                                if (input.length <= 8) {
                                    drafts[type] = input
                                    rejected[type] = false
                                }
                            },
                            label = { Text(SettingsStrings.newCategoryName(lang)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            val name = drafts[type].orEmpty()
                            // addCategory 返回 false 说明是空名 / 重名 / 超过 8 个字，就只给一行提示，不新增
                            if (onAdd(type, name)) {
                                drafts[type] = ""
                                rejected[type] = false
                            } else {
                                rejected[type] = true
                            }
                        }) { Text(AppStrings.add(lang)) }
                    }
                    if (rejected[type] == true) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = SettingsStrings.addCategoryFailed(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { onReset(type) }) {
                        Text(SettingsStrings.resetCategories(lang))
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Text(
                    text = SettingsStrings.categoryManageHint(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(SettingsStrings.categoryManageDone(lang)) }
        }
    )
}

/**
 * 周期记账管理：上半是已有的规则（可以暂停 / 删除），下半是新增。
 *
 * 新增只用「打开对话框时」的状态，加完就清空；首次记账日期默认今天，
 * 需要改就点日期胶囊翻日历（复用待办那边的 DatePickerDialog 写法）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringDialog(
    items: List<RecurringEntity>,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    onAdd: (
        amountCents: Long,
        type: TxType,
        category: String,
        account: String,
        note: String,
        rule: RepeatRule,
        nextDueMillis: Long
    ) -> Unit,
    onToggle: (RecurringEntity) -> Unit,
    onDelete: (RecurringEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalLang.current

    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TxType.EXPENSE) }
    var category by remember { mutableStateOf("") }
    var account by remember { mutableStateOf(Accounts.DEFAULT) }
    var note by remember { mutableStateOf("") }
    var rule by remember { mutableStateOf(RepeatRule.MONTHLY) }
    var firstDue by remember { mutableStateOf(LocalDate.now()) }
    var rejected by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    // 分类列表跟着收 / 支切换；当前分类在新列表里就保留，换类型时退回第一个
    val categories = if (type == TxType.EXPENSE) expenseCategories else incomeCategories
    val selectedCategory = if (categories.contains(category)) category else categories.firstOrNull().orEmpty()
    val amountCents = parseAmountToCents(amountText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.recurringTitle(lang)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // ---- 已有规则 ----
                if (items.isEmpty()) {
                    Text(
                        text = AppStrings.recurringEmpty(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                } else {
                    Text(
                        text = SettingsStrings.recurringCount(lang, items.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    items.forEach { item ->
                        val itemType = item.type
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                // 类型名是 AppStrings / TxType 里的文案
                                Text(
                                    text = itemType.label(lang) + " · " + formatAmount(item.amountCents),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (item.enabled) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(Modifier.height(2.dp))
                                // 分类名和账户名是数据，不翻译；规则名用 RepeatRule.label
                                Text(
                                    text = item.category + " · " + item.account + " · " + item.repeat.label(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    // 日期用项目自己的格式化（今天 / 明天 / 9月20日），不打印时间戳
                                    text = AppStrings.recurringNext(
                                        lang,
                                        formatDueLabel(item.nextDueMillis, lang)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (item.enabled) SettingsStrings.recurringEnabled(lang) else SettingsStrings.recurringPaused(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = item.enabled, onCheckedChange = { onToggle(item) })
                            IconButton(onClick = { onDelete(item) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = SettingsStrings.removeCategoryLabel(lang, item.category),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = AppStrings.recurringHint(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ---- 新增 ----
                Spacer(Modifier.height(14.dp))
                Text(SettingsStrings.recurringAdd(lang), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.count { it == '.' } <= 1 &&
                            input.all { it.isDigit() || it == '.' } &&
                            input.length <= 10
                        ) {
                            amountText = input
                            rejected = false
                        }
                    },
                    label = { Text(SettingsStrings.amount(lang)) },
                    prefix = { Text("¥") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (rejected) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = SettingsStrings.recurringAmountRequired(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(10.dp))
                FieldLabel(SettingsStrings.recurringKindLabel(lang))
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    TxType.entries.forEach { entry ->
                        FilterChip(
                            selected = type == entry,
                            onClick = { type = entry },
                            label = { Text(entry.label(lang)) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                FieldLabel(LedgerStrings.categoryLabel(lang))
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    categories.forEach { item ->
                        FilterChip(
                            selected = selectedCategory == item,
                            onClick = { category = item },
                            // 分类名是数据（预置或用户自己起的），不翻译
                            label = { Text(item) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                FieldLabel(LedgerStrings.accountLabel(lang))
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    Accounts.PRESETS.forEach { item ->
                        FilterChip(
                            selected = account == item,
                            onClick = { account = item },
                            // 账户名是数据，不翻译
                            label = { Text("${Accounts.emojiOf(item)} $item") }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 40) note = it },
                    label = { Text(LedgerStrings.noteOptional(lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(TodoStrings.repeatLabel(lang))
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    // 周期记账不用「不重复」，所以只给每周 / 每月
                    listOf(RepeatRule.WEEKLY, RepeatRule.MONTHLY).forEach { entry ->
                        FilterChip(
                            selected = rule == entry,
                            onClick = { rule = entry },
                            label = { Text(entry.label(lang)) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                // 日期靠一行的「选择」按钮翻日历；小屏放得下，不用挤成两行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SettingsStrings.recurringFirstDue(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = AppStrings.monthDay(lang, firstDue.monthValue, firstDue.dayOfMonth),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { showPicker = true }) { Text(AppStrings.select(lang)) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedCategory.isNotEmpty()) {
                        onAdd(
                            amountCents ?: 0L,
                            type,
                            selectedCategory,
                            account,
                            note.trim(),
                            rule,
                            firstDue.toDayMillis()
                        )
                        // 加完清空，方便连着加第二条
                        amountText = ""
                        note = ""
                        rejected = false
                    }
                }
            ) { Text(AppStrings.add(lang)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel(lang)) }
        }
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = firstDue.toDayMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // DatePicker 给的是 UTC 当天 00:00，先按 UTC 取回日期，再本地化到当天 00:00
                    pickerState.selectedDateMillis?.let {
                        firstDue = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showPicker = false
                }) { Text(AppStrings.confirm(lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(AppStrings.cancel(lang)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
