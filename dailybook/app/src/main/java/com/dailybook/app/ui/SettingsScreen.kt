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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import com.dailybook.app.data.Categories
import com.dailybook.app.timer.TimerViewModel
import com.dailybook.app.ui.theme.ThemeMode
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.parseAmountToCents

private enum class ClearTarget(val label: String, val message: String) {
    TRANSACTIONS("清除所有记账记录", "所有收支记录都会被删除，且无法恢复。"),
    TODOS("清除所有待办", "所有待办事项都会被删除，且无法恢复。"),
    FOCUS_STATS("清除专注记录", "专注次数、连续天数和时段明细都会被清零，且无法恢复。"),
    EVERYTHING("清空全部数据", "记账记录、待办和专注记录都会被删除，且无法恢复。")
}

@Composable
fun SettingsScreen(
    state: UiState,
    vm: MainViewModel,
    timerVm: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val themeMode by vm.settings.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by vm.settings.dynamicColor.collectAsStateWithLifecycle()
    val ledgerReminder by vm.settings.ledgerReminderEnabled.collectAsStateWithLifecycle()
    val reminderHour by vm.settings.ledgerReminderHour.collectAsStateWithLifecycle()
    val reminderMinute by vm.settings.ledgerReminderMinute.collectAsStateWithLifecycle()

    var clearing by remember { mutableStateOf<ClearTarget?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetText by remember { mutableStateOf("") }
    var confirmImport by remember { mutableStateOf(false) }
    var showCategoryBudget by remember { mutableStateOf(false) }
    var showReminderTime by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val message by vm.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.consumeMessage()
        }
    }

    // 三个文件选择器：导出备份 / 导出 CSV 用「新建文件」，恢复用「打开文件」。
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
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))

        SectionCard(title = "外观") {
            FieldLabel("主题")
            Spacer(Modifier.height(8.dp))
            ChipFlow {
                ThemeChip("跟随系统", ThemeMode.SYSTEM, themeMode) { vm.setThemeMode(it) }
                ThemeChip("浅色", ThemeMode.LIGHT, themeMode) { vm.setThemeMode(it) }
                ThemeChip("深色", ThemeMode.DARK, themeMode) { vm.setThemeMode(it) }
            }
            Spacer(Modifier.height(10.dp))
            LabeledSwitch("动态取色（Android 12+）", dynamicColor) { vm.setDynamicColor(it) }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "记账") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("月度预算", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (state.hasBudget) "¥${formatAmount(state.budgetCents)}"
                        else "未设置（不显示预算进度）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = {
                    budgetText = if (state.hasBudget) formatAmount(state.budgetCents) else ""
                    showBudgetDialog = true
                }) { Text("设置") }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("分类预算", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (state.hasCategoryBudget) {
                            val over = state.categoryBudgets.count { it.over }
                            "${state.categoryBudgets.size} 个分类已设置" +
                                if (over > 0) " · $over 个超支" else ""
                        } else {
                            "给常超支的分类单独设上限"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showCategoryBudget = true }) { Text("管理") }
            }

            Spacer(Modifier.height(4.dp))
            LabeledSwitch("每晚记账提醒", ledgerReminder) { vm.setLedgerReminder(it) }
            if (ledgerReminder) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("提醒时间", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { showReminderTime = true }) {
                        Text("%02d:%02d".format(reminderHour, reminderMinute))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "专注计时") {
            LabeledSlider(
                label = "专注时长",
                value = focusSettings.focusMinutes,
                range = 5..90,
                valueText = "%d 分钟",
                onChange = { timerVm.setFocusMinutes(it) }
            )
            LabeledSlider(
                label = "短休息",
                value = focusSettings.shortBreakMinutes,
                range = 1..30,
                valueText = "%d 分钟",
                onChange = { timerVm.setShortBreakMinutes(it) }
            )
            LabeledSlider(
                label = "长休息",
                value = focusSettings.longBreakMinutes,
                range = 5..45,
                valueText = "%d 分钟",
                onChange = { timerVm.setLongBreakMinutes(it) }
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("长休息间隔", style = MaterialTheme.typography.bodyMedium)
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
                            contentDescription = "减少",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "每 ${focusSettings.longBreakEvery} 个",
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
                            contentDescription = "增加",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LabeledSwitch("阶段结束震动提醒", focusSettings.vibrate) { timerVm.setVibrate(it) }
            LabeledSwitch("自动开始下一阶段", focusSettings.autoStartNext) { timerVm.setAutoStart(it) }
            LabeledSwitch("计时中保持屏幕常亮", focusSettings.keepScreenOn) { timerVm.setKeepScreenOn(it) }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard(title = "数据") {
            Text(
                text = "本月 ${state.monthCount} 笔记录 · ${state.pendingCount + state.doneCount} 条待办 · " +
                    "今日 ${state.focusStats.todayCount} 个专注",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Text("备份与导出", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { backupLauncher.launch(Backup.suggestName("日常本备份", "json")) },
                    modifier = Modifier.weight(1f)
                ) { Text("导出备份") }
                OutlinedButton(
                    onClick = { confirmImport = true },
                    modifier = Modifier.weight(1f)
                ) { Text("恢复备份") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { csvLauncher.launch(Backup.suggestName("日常本记账", "csv")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("导出记账 CSV（Excel 可打开）") }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "备份是一个 JSON 文件，装下全部记账、待办、专注记录和预算设置，" +
                    "换手机或重装后可以整份恢复。文件存到你挑的位置，恢复时会覆盖当前数据。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Text("清除数据", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            ClearTarget.entries.forEach { target ->
                OutlinedButton(
                    onClick = { clearing = target },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(target.label) }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard {
            Text(
                text = "日常本 v1.4",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "记账 + 待办 + 专注计时，三合一。数据全部存在手机本地，不联网、不上传，" +
                    "只有通知、震动和开机后排提醒需要系统权限。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(28.dp))
    }

    clearing?.let { target ->
        ConfirmDialog(
            title = "确认${target.label}？",
            text = target.message,
            confirmText = "确定清除",
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
            title = "从备份文件恢复？",
            text = "当前所有记账、待办和专注记录都会被备份文件里的内容替换，无法撤销。" +
                "建议先点「导出备份」存一份现在的数据。",
            confirmText = "选择备份文件",
            onConfirm = {
                confirmImport = false
                restoreLauncher.launch(arrayOf("application/json", "*/*"))
            },
            onDismiss = { confirmImport = false }
        )
    }

    if (showCategoryBudget) {
        CategoryBudgetDialog(
            budgets = state.categoryBudgets.associate { it.category to it.budgetCents },
            onSet = { category, cents -> vm.setCategoryBudget(category, cents) },
            onDismiss = { showCategoryBudget = false }
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
            title = { Text("设置月度预算") },
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
                        label = { Text("金额") },
                        prefix = { Text("¥") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "每月支出接近预算时，记账页会显示进度条；超支会标红。留空或填 0 表示不设置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setMonthlyBudget(parseAmountToCents(budgetText) ?: 0L)
                    showBudgetDialog = false
                }) { Text("保存") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        vm.setMonthlyBudget(0L)
                        showBudgetDialog = false
                    }) { Text("清除") }
                    TextButton(onClick = { showBudgetDialog = false }) { Text("取消") }
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
    val texts = remember(budgets) {
        mutableStateMapOf<String, String>().apply {
            Categories.EXPENSE.forEach { category ->
                put(category, budgets[category]?.let { formatAmount(it) } ?: "")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分类预算") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "给常超支的分类单独设每月上限，留空表示不限。超支的分类会在统计页标红。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Categories.EXPENSE.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
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
    val pickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("每晚提醒时间") },
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
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
