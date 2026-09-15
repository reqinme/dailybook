package com.dailybook.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.backup.AutoBackup
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
import com.dailybook.app.i18n.StudyStrings
import com.dailybook.app.i18n.TodoStrings
import com.dailybook.app.notify.ClassReminder
import com.dailybook.app.timer.TimerViewModel
import com.dailybook.app.ui.ConfirmDialog
import com.dailybook.app.ui.ChipFlow
import com.dailybook.app.ui.FieldLabel
import com.dailybook.app.ui.LabeledSlider
import com.dailybook.app.ui.LabeledSwitch
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.Route
import com.dailybook.app.ui.SectionCard
import com.dailybook.app.ui.SettingsCategory
import com.dailybook.app.ui.Shapes
import com.dailybook.app.ui.rememberBackgroundBitmap
import com.dailybook.app.ui.theme.ThemeMode
import com.dailybook.app.ui.theme.ThemePalette
import com.dailybook.app.ui.theme.lightSchemeOf
import com.dailybook.app.util.ClassSchedule
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.formatDueLabel
import com.dailybook.app.util.parseAmountToCents
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// =====================================================================
// 设置分类子页面
//
// 这一层只做两件事：把「当前分类」翻译成对应的一屏设置，以及把每个开关 / 按钮
// 接到原来就有的 VM 方法上。行的排版和对话框都是从旧的单页设置里原样搬过来的，
// 行为不变，只是各自回到了自己的分类页面。
// =====================================================================

/** 自动备份「上次成功」的时间写法：本地时区的 2026-09-14 21:05，四语都用同一套数字格式 */
private val BACKUP_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/** 上课提醒可选的提前分钟数（分钟数是数据，界面上的「分钟」由文案表出） */
private val CLASS_REMINDER_MINUTES = listOf(5, 10, 15, 30)

/** 清除项的按钮文案 / 二次确认说明 */
private enum class ClearTarget {
    TRANSACTIONS,
    TODOS,
    FOCUS_STATS,
    EVERYTHING
}

private fun ClearTarget.label(lang: Lang): String = when (this) {
    ClearTarget.TRANSACTIONS -> SettingsStrings.clearAllRecords(lang)
    ClearTarget.TODOS -> SettingsStrings.clearAllTodos(lang)
    ClearTarget.FOCUS_STATS -> SettingsStrings.clearFocusStats(lang)
    ClearTarget.EVERYTHING -> SettingsStrings.clearEverything(lang)
}

private fun ClearTarget.message(lang: Lang): String = when (this) {
    ClearTarget.TRANSACTIONS -> SettingsStrings.clearAllRecordsMessage(lang)
    ClearTarget.TODOS -> SettingsStrings.clearAllTodosMessage(lang)
    ClearTarget.FOCUS_STATS -> SettingsStrings.clearFocusStatsMessage(lang)
    ClearTarget.EVERYTHING -> SettingsStrings.clearEverythingMessage(lang)
}

/**
 * 一个设置分类的子页面：只画这个分类的设置项。
 *
 * 每个分类都是「一整屏 + 自己的滚动」，并且**不嵌套**滚动容器：
 * 外层 [Column] 负责滚，卡片里只放静态内容，对话框内部各自管自己的滚动。
 */
@Composable
fun SettingsCategoryScreen(
    category: SettingsCategory,
    state: UiState,
    vm: MainViewModel,
    timerVm: TimerViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = LocalLang.current
    val message by vm.message.collectAsStateWithLifecycle()

    // 导出 / 恢复 / 备份的结果由 VM 用 Toast 说（AppStrings 里那几句），这里只负责显示
    LaunchedEffect(message) {
        message?.let {
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

        when (category) {
            SettingsCategory.APPEARANCE -> AppearanceSettings(vm)
            SettingsCategory.LANGUAGE -> LanguageSettings(vm)
            SettingsCategory.LEDGER -> LedgerSettings(state, vm)
            SettingsCategory.FOCUS -> FocusSettingsSection(timerVm, vm)
            SettingsCategory.STUDY -> StudySettings(state, vm)
            SettingsCategory.DATA -> DataSettings(state, vm)
            SettingsCategory.ABOUT -> AboutCategorySection(nav)
        }

        Spacer(Modifier.height(28.dp))
    }
}

// =====================================================================
// 外观
// =====================================================================

@Composable
private fun AppearanceSettings(vm: MainViewModel) {
    val lang = LocalLang.current
    val themeMode by vm.settings.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by vm.settings.dynamicColor.collectAsStateWithLifecycle()
    // 配色方案（跟随系统取色打开时会被系统取色盖掉，见 paletteDynamicHint）
    val palette by vm.settings.palette.collectAsStateWithLifecycle()
    // 自定义背景图：URI 与蒙版浓度都存在 SettingsStore 里，由 MainActivity 铺在最底层
    val backgroundUri by vm.settings.backgroundUri.collectAsStateWithLifecycle()
    val backgroundScrim by vm.settings.backgroundScrim.collectAsStateWithLifecycle()

    // 挑图用系统文件选择器（OpenDocument），拿到的读权限跟着 URI 持久化 ——
    // 下次启动 MainActivity 还要能解码这张图，所以不自己复制文件进 App 目录。
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.settings.setBackgroundUri(it.toString()) } }

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

        Spacer(Modifier.height(12.dp))
        FieldLabel(AppStrings.paletteTitle(lang))
        Spacer(Modifier.height(6.dp))
        ChipFlow {
            ThemePalette.entries.forEach { entry ->
                PaletteChip(entry, palette) { vm.settings.setPalette(it) }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 只在动态取色开着的时候提示：配色会被系统取色盖掉；不禁用芯片（用户可以先选好）
        if (dynamicColor) {
            Text(
                text = SettingsStrings.paletteDynamicHint(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(16.dp))
        // 背景图（SettingsStore.backgroundUri / backgroundScrim，MainActivity 用 AppBackground 铺底）：
        // 选图 → setBackgroundUri(uri)；「清除」→ setBackgroundUri(null)；
        // 蒙版浓度 → setBackgroundScrim(percent)。图片本身不进 App 私有目录，只记 URI。
        FieldLabel(SettingsStrings.backgroundImage(lang))
        Spacer(Modifier.height(6.dp))
        SettingsRow(
            title = if (backgroundUri.isBlank()) {
                AppStrings.autoBackupNone(lang)
            } else {
                SettingsStrings.backgroundChosen(lang)
            },
            subtitle = SettingsStrings.backgroundHint(lang),
            trailing = SettingsStrings.backgroundPick(lang),
            onClick = { imageLauncher.launch(arrayOf("image/*")) }
        )
        if (backgroundUri.isNotBlank()) {
            val preview = rememberBackgroundBitmap(backgroundUri)
            if (preview != null) {
                // 预览按 16:9 裁：和真正铺在界面上的 Crop 表现一致，看得见大概效果
                Image(
                    bitmap = preview,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(Shapes.card)
                )
                Spacer(Modifier.height(10.dp))
            }
            LabeledSlider(
                label = SettingsStrings.backgroundScrim(lang),
                value = backgroundScrim,
                range = 0..80,
                valueText = SettingsStrings.percentTemplate(lang),
                onChange = { vm.settings.setBackgroundScrim(it) }
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { vm.settings.setBackgroundUri(null) }) {
                    Text(AppStrings.clear(lang))
                }
            }
        }
    }
}

// =====================================================================
// 语言
// =====================================================================

@Composable
private fun LanguageSettings(vm: MainViewModel) {
    val lang = LocalLang.current
    val currentLang by vm.settings.lang.collectAsStateWithLifecycle()

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
}

// =====================================================================
// 记账：预算 / 分类 / 周期记账 / 提醒 / 定期小结
// =====================================================================

@Composable
private fun LedgerSettings(state: UiState, vm: MainViewModel) {
    val lang = LocalLang.current

    val ledgerReminder by vm.settings.ledgerReminderEnabled.collectAsStateWithLifecycle()
    val reminderHour by vm.settings.ledgerReminderHour.collectAsStateWithLifecycle()
    val reminderMinute by vm.settings.ledgerReminderMinute.collectAsStateWithLifecycle()
    val summaryMode by vm.settings.summaryMode.collectAsStateWithLifecycle()
    val budgetAlert by vm.settings.budgetAlert.collectAsStateWithLifecycle()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetText by remember { mutableStateOf("") }
    var showCategoryBudget by remember { mutableStateOf(false) }
    var showCategoryManage by remember { mutableStateOf(false) }
    var showRecurring by remember { mutableStateOf(false) }
    var showReminderTime by remember { mutableStateOf(false) }

    SectionCard(title = SettingsStrings.sectionLedger(lang)) {
        // 月度预算
        SettingsRow(
            title = SettingsStrings.monthlyBudget(lang),
            subtitle = if (state.hasBudget) "¥${formatAmount(state.budgetCents)}"
            else SettingsStrings.budgetNotSet(lang),
            trailing = SettingsStrings.setMonthlyBudget(lang),
            onClick = {
                budgetText = if (state.hasBudget) formatAmount(state.budgetCents) else ""
                showBudgetDialog = true
            }
        )

        // 分类预算
        SettingsRow(
            title = SettingsStrings.categoryBudget(lang),
            subtitle = if (state.hasCategoryBudget) {
                val over = state.categoryBudgets.count { it.over }
                SettingsStrings.categoryBudgetCount(lang, state.categoryBudgets.size) +
                    if (over > 0) " · " + SettingsStrings.categoryBudgetOver(lang, over) else ""
            } else {
                SettingsStrings.categoryBudgetHint(lang)
            },
            trailing = SettingsStrings.categoryBudgetAction(lang),
            onClick = { showCategoryBudget = true }
        )

        // 分类管理
        SettingsRow(
            title = SettingsStrings.categoryManage(lang),
            subtitle = SettingsStrings.categoryManageSubtitle(
                lang,
                state.expenseCategories.size,
                state.incomeCategories.size
            ),
            // 这两行原来都写「管理」，用户看不出区别：一个改额度、一个改分类本身
            trailing = SettingsStrings.categoryManageAction(lang),
            onClick = { showCategoryManage = true }
        )

        // 周期记账：房租、订阅这类固定支出，到日子自动记一笔
        SettingsRow(
            title = AppStrings.recurringTitle(lang),
            subtitle = if (state.recurring.isEmpty()) {
                AppStrings.recurringEmpty(lang)
            } else {
                SettingsStrings.recurringCount(lang, state.recurring.size)
            },
            trailing = AppStrings.manage(lang),
            onClick = { showRecurring = true }
        )

        Spacer(Modifier.height(4.dp))
        LabeledSwitch(SettingsStrings.nightlyLedgerReminder(lang), ledgerReminder) { vm.setLedgerReminder(it) }
        if (ledgerReminder) {
            Spacer(Modifier.height(4.dp))
            SettingsRow(
                title = SettingsStrings.reminderTime(lang),
                trailing = "%02d:%02d".format(reminderHour, reminderMinute),
                onClick = { showReminderTime = true }
            )
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
            // 自建分类 / 账本里用过的分类也要出现在对话框里，否则保存会把它们的预算抹掉
            expenseCategories = state.expenseCategories,
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
}

// =====================================================================
// 专注：时长 / 间隔 / 开关 / 每日目标
// =====================================================================

@Composable
private fun FocusSettingsSection(timerVm: TimerViewModel, vm: MainViewModel) {
    val lang = LocalLang.current
    val focusGoal by vm.settings.focusGoal.collectAsStateWithLifecycle()
    val timerState by timerVm.state.collectAsStateWithLifecycle()
    val focusSettings = timerState.settings

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
}

// =====================================================================
// 学习：学期起始日 / GPA 口径 / 上课提醒
// 三个偏好键都在 SettingsStore 里（termStartMillis / gpaScale / classReminder + classReminderMinutes）
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudySettings(state: UiState, vm: MainViewModel) {
    val lang = LocalLang.current
    val termStart by vm.settings.termStartMillis.collectAsStateWithLifecycle()
    val gpaScale by vm.settings.gpaScale.collectAsStateWithLifecycle()
    val classReminder by vm.settings.classReminder.collectAsStateWithLifecycle()
    val remindMinutes by vm.settings.classReminderMinutes.collectAsStateWithLifecycle()
    var showTermPicker by remember { mutableStateOf(false) }
    // 有没有任何一门课填了上课时间：决定提示该说「提醒能响」还是「还没课有填时间」
    val anyCourseHasTime = remember(state.courses) {
        state.courses.any { ClassSchedule.isValidTime(it.startMinutes) }
    }

    SectionCard(title = AppStrings.settingsStudy(lang)) {
        Text(
            text = AppStrings.settingsStudyHint(lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        // ---- 学期起始日：用来把课表的「第几周」换算成真实日期 ----
        SettingsRow(
            title = SettingsStrings.termStart(lang),
            subtitle = SettingsStrings.termStartHint(lang),
            trailing = if (termStart > 0L) {
                // 存的是**本地零点**的时间戳，必须按本地时区取日期。
                // 之前用 `LocalDate.ofEpochDay(termStart / 86_400_000L)`（按 UTC 算），
                // 在 UTC+8 会把用户选的 9 月 1 日显示成 8 月 31 日。
                val d = termStart.toLocalDate()
                AppStrings.monthDay(lang, d.monthValue, d.dayOfMonth)
            } else {
                AppStrings.notSet(lang)
            },
            onClick = { showTermPicker = true }
        )
        if (termStart > 0L) {
            TextButton(onClick = { vm.settings.setTermStartMillis(0L) }) {
                Text(AppStrings.clear(lang))
            }
        }

        Spacer(Modifier.height(10.dp))

        // ---- GPA 计算口径 ----
        FieldLabel(SettingsStrings.gpaScale(lang))
        Spacer(Modifier.height(6.dp))
        ChipFlow {
            listOf(4.0, 5.0).forEach { scale ->
                FilterChip(
                    selected = kotlin.math.abs(gpaScale - scale) < 0.01,
                    onClick = { vm.settings.setGpaScale(scale) },
                    label = { Text(SettingsStrings.gpaScaleValue(lang, if (scale >= 4.5) "5.0" else "4.0")) }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- 上课提醒 ----
        // 这个开关现在是真的：课程里能填「开始 / 结束时间」之后，提醒时刻 = 上课时刻 - 提前量，
        // 由 ClassReminder 用非精确闹钟排下一次（不申请新权限）。
        // 仍然有一件事必须说清楚：只有**填了开始时间**的课才排得出提醒，
        // 所以下面那行提示按「有没有课填过时间」分成两句，两句都是真的。
        val context = LocalContext.current

        LabeledSwitch(
            SettingsStrings.classReminder(lang),
            classReminder
        ) { enabled ->
            vm.settings.setClassReminder(enabled)
            // 开 / 关都要重排：开了排下一节，关了把已排的闹钟撤掉
            ClassReminder.reschedule(context)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (!anyCourseHasTime) {
                SettingsStrings.classReminderNoTimeHint(lang)
            } else {
                SettingsStrings.classReminderHint(lang)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (classReminder) {
            Spacer(Modifier.height(12.dp))
            FieldLabel(SettingsStrings.minutesBefore(lang))
            Spacer(Modifier.height(6.dp))
            ChipFlow {
                CLASS_REMINDER_MINUTES.forEach { minutes ->
                    FilterChip(
                        selected = remindMinutes == minutes,
                        onClick = {
                            vm.settings.setClassReminderMinutes(minutes)
                            // 提前量一变，旧闹钟的时刻就不对了，立刻按新提前量重排
                            ClassReminder.reschedule(context)
                        },
                        label = { Text(SettingsStrings.minutesBeforeValue(lang, minutes)) }
                    )
                }
            }
        }
    }

    if (showTermPicker) {
        val initial = if (termStart > 0L) termStart else System.currentTimeMillis()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showTermPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // DatePicker 给的是 UTC 当天零点，换成本地日期再存本地零点
                    pickerState.selectedDateMillis?.let { millis ->
                        val local = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        vm.settings.setTermStartMillis(
                            local.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        )
                    }
                    showTermPicker = false
                }) { Text(AppStrings.save(lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showTermPicker = false }) { Text(AppStrings.cancel(lang)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// =====================================================================
// 数据与备份
// =====================================================================

@Composable
private fun DataSettings(state: UiState, vm: MainViewModel) {
    val lang = LocalLang.current
    val context = LocalContext.current

    // 自动备份是本机偏好，单例直接拿，不走 VM
    val autoBackup = remember { AutoBackup.get(context) }
    val autoBackupEnabled by autoBackup.enabled.collectAsStateWithLifecycle()
    val autoBackupFolder by autoBackup.folderUri.collectAsStateWithLifecycle()
    val lastBackupAt by autoBackup.lastBackupAt.collectAsStateWithLifecycle()
    val lastBackupFailure by autoBackup.lastFailure.collectAsStateWithLifecycle()

    var clearing by remember { mutableStateOf<ClearTarget?>(null) }
    var confirmImport by remember { mutableStateOf(false) }
    var confirmCsvImport by remember { mutableStateOf(false) }
    // 自动备份：想在没选文件夹时就打开开关时给出的行内提示
    var needsFolder by remember { mutableStateOf(false) }

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

    // 自动备份的文件夹：OpenDocumentTree 选一个目录，授权由 AutoBackup.setFolder 持久化。
    // 用户在系统界面里挑目录，所以同样不需要任何存储权限。
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            autoBackup.setFolder(it)
            // 刚才是「想开但没文件夹」才被拦下来的话，选完文件夹就直接打开开关
            if (needsFolder) {
                autoBackup.setEnabled(true)
                needsFolder = false
            }
        }
    }

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

        // ---- 自动备份：挑一个文件夹，开 App / 每晚提醒时自动写一份进去 ----
        Spacer(Modifier.height(16.dp))
        LabeledSwitch(AppStrings.autoBackupTitle(lang), autoBackupEnabled) { wanted ->
            if (wanted && autoBackupFolder == null) {
                // 没有文件夹就没有备份目标：不打开开关，只把「先选文件夹」提示留在卡片上
                needsFolder = true
            } else {
                needsFolder = false
                autoBackup.setEnabled(wanted)
            }
        }
        if (needsFolder && autoBackupFolder == null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = AppStrings.autoBackupNeedsFolder(lang) + " · " + SettingsStrings.autoBackupNeedsFolderHint(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = SettingsStrings.autoBackupFolderLabel(lang),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = autoBackup.folderLabel() ?: AppStrings.autoBackupNone(lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { folderLauncher.launch(null) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (autoBackupFolder == null) AppStrings.autoBackupPickFolder(lang)
                    else AppStrings.autoBackupChangeFolder(lang)
                )
            }
            OutlinedButton(
                onClick = {
                    // 结果（成功 / 没配好 / 失败原因）由 VM 用 Toast 说，这里不自己报成功
                    vm.backupNow()
                },
                modifier = Modifier.weight(1f)
            ) { Text(AppStrings.autoBackupNow(lang)) }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = if (lastBackupAt > 0L) {
                AppStrings.autoBackupLast(
                    lang,
                    Instant.ofEpochMilli(lastBackupAt)
                        .atZone(ZoneId.systemDefault())
                        .format(BACKUP_TIME_FORMAT)
                )
            } else {
                AppStrings.autoBackupNever(lang)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        lastBackupFailure?.let { code ->
            // 失败时间也从 AutoBackup 里读出来：只写「上次失败」时，用户分不清是刚才那次
            // 还是上个礼拜留下的（这个 getter 以前一直没人读）。
            // 重组由 code / 上次成功时间的变化触发，所以「失败了一次新的」界面一定会刷新。
            val failedAt = remember(code, lastBackupAt) { autoBackup.lastFailureMillis() }
            Spacer(Modifier.height(4.dp))
            Text(
                // 原因码翻成人话再套进「上次失败 <时间>：…」；失败不会伪装成成功
                text = if (failedAt > 0L) {
                    AppStrings.autoBackupFailedAt(
                        lang,
                        Instant.ofEpochMilli(failedAt)
                            .atZone(ZoneId.systemDefault())
                            .format(BACKUP_TIME_FORMAT),
                        AppStrings.backupFailure(lang, code.name)
                    )
                } else {
                    AppStrings.autoBackupFailed(lang, AppStrings.backupFailure(lang, code.name))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = AppStrings.autoBackupHint(lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (autoBackupEnabled && autoBackupFolder == null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = SettingsStrings.autoBackupNeedsFolderHint(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
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
}

// =====================================================================
// 关于与更新（分类页里的那一段，真正的详情在 Route.About / Route.Update）
// =====================================================================

@Composable
private fun AboutCategorySection(nav: Navigator) {
    val lang = LocalLang.current
    val version = rememberAppVersion()

    SectionCard(title = AppStrings.settingsAbout(lang)) {
        // 关于分类页只放一行版本 + 两个入口：详细内容在各自的页面上，见 AboutScreen / UpdateScreen
        SettingsRow(
            title = SettingsStrings.aboutAppLine(lang, AppStrings.appName(lang), version.display(lang)),
            subtitle = SettingsStrings.aboutDescription(lang),
            onClick = { nav.push(Route.About) }
        )
        SettingsRow(
            title = AppStrings.checkUpdate(lang),
            subtitle = SettingsStrings.updateNoNetworkNote(lang),
            onClick = { nav.push(Route.Update) }
        )
        SettingsRow(
            title = SettingsStrings.licenseTitle(lang),
            // 只留一行摘要：许可全文在「关于」页，别在列表和详情页各印一遍同一段话
            subtitle = SettingsStrings.licenseRowSubtitle(lang),
            onClick = { nav.push(Route.About) }
        )
    }
}

// =====================================================================
// 外观：芯片 / 色卡（原 SettingsScreen.kt 里的私有组件，跟着外观页搬过来）
// =====================================================================

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

/**
 * 配色芯片：前面一颗小圆点是这套配色**自己的**主色，选的不是当前这一套也能看出颜色。
 *
 * 点色取自 `lightSchemeOf(spec()).primary`（浅色方案的主色）。spec()/lightSchemeOf 都是
 * internal，和本文件同一个模块，所以能直接调；深色模式下这几颗点仍然用浅色主色 ——
 * 它们是「色卡」而不是主题色，固定用浅色那支反而六颗都好认。名字用 AppStrings.paletteXxx。
 */
@Composable
private fun PaletteChip(
    palette: ThemePalette,
    current: ThemePalette,
    onSelect: (ThemePalette) -> Unit
) {
    val lang = LocalLang.current
    FilterChip(
        selected = current == palette,
        onClick = { onSelect(palette) },
        label = { Text(palette.label(lang)) },
        leadingIcon = {
            Box(
                Modifier
                    .size(12.dp)
                    .background(paletteDotColor(palette), CircleShape)
            )
        }
    )
}

/** 配色的展示名（AppStrings 里的十二个） */
private fun ThemePalette.label(lang: Lang): String = when (this) {
    ThemePalette.TEAL -> AppStrings.paletteTeal(lang)
    ThemePalette.INDIGO -> AppStrings.paletteIndigo(lang)
    ThemePalette.VIOLET -> AppStrings.paletteViolet(lang)
    ThemePalette.ROSE -> AppStrings.paletteRose(lang)
    ThemePalette.AMBER -> AppStrings.paletteAmber(lang)
    ThemePalette.FOREST -> AppStrings.paletteForest(lang)
    ThemePalette.SKY -> AppStrings.paletteSky(lang)
    ThemePalette.MINT -> AppStrings.paletteMint(lang)
    ThemePalette.CORAL -> AppStrings.paletteCoral(lang)
    ThemePalette.COFFEE -> AppStrings.paletteCoffee(lang)
    ThemePalette.GRAPHITE -> AppStrings.paletteGraphite(lang)
    ThemePalette.SAKURA -> AppStrings.paletteSakura(lang)
}

/** 色卡上的那颗点：这套配色的浅色主色 */
private fun paletteDotColor(palette: ThemePalette): Color = lightSchemeOf(palette.spec()).primary

// =====================================================================
// 记账页的对话框（原 SettingsScreen.kt 里的私有对话框，原样搬过来）
// =====================================================================

/**
 * 分类预算：逐个支出分类填上限，留空表示不限。
 *
 * 这里列的是**并集**：预置支出分类 + 已经在用 / 已经设过预算的分类
 * （[expenseCategories] 来自 `UiState.expenseCategories`，含用户自建分类和账本里用过的分类）。
 *
 * 为什么必须是并集：`SettingsStore.setCategoryBudget` 在金额 `<= 0` 时是**删除**这个键，
 * 而保存会把对话框里每个分类都写一遍。所以「只列预置分类」就等于给自建分类（比如「宠物」）
 * 传 0——用户一按保存，那个分类的预算连同它的超支提醒就被悄悄删掉了。
 * 反过来，凡是写出去的行都必须在对话框里露过面：下面 [rows] 同时喂给渲染和保存，
 * 既不会删掉没露面的预算，也不会写出用户没看见的值。
 */
@Composable
private fun CategoryBudgetDialog(
    budgets: Map<String, Long>,
    expenseCategories: List<String>,
    onSet: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalLang.current
    // 预置分类按原顺序排在前面，其余（自建 / 用过的）按名字追加在后，重复的去掉
    val rows = remember(expenseCategories) {
        (Categories.EXPENSE + expenseCategories).distinct().sortedBy { category ->
            Categories.EXPENSE.indexOf(category).let { if (it >= 0) it else Categories.EXPENSE.size }
        }
    }
    val texts = remember(rows, budgets) {
        mutableStateMapOf<String, String>().apply {
            rows.forEach { category ->
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
                rows.forEach { category ->
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
                // 只写对话框里真的出现过的行（rows 与渲染用的是同一份），
                // 留空 = 解析成 0 = 清掉预算，这是用户自己看得见、改得动的
                rows.forEach { category ->
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
                // 「类型」这个字段标签和奖助弹窗里那个是同一个词，共用 StudyStrings.awardsKind
                // （原来本表里另有一个一字不差的 recurringKindLabel，已合并掉）
                FieldLabel(StudyStrings.awardsKind(lang))
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
                    // 金额必须大于 0：空着、填「.」解析不出来、或者填 0 都不建规则。
                    // 以前这里把 null 当 0 用，于是会建出一条「支出 · 0.00」的规则：
                    // 到日子就往账本里插一笔 ¥0.00，而且 Backup 导出时按 amountCents > 0 过滤，
                    // 这种规则连备份都存不下来（备份 / 恢复一圈回来就消失了）。
                    val cents = amountCents
                    if (cents == null || cents <= 0L) {
                        // 复用已有的「请先填写大于 0 的金额」：
                        // 空着、填「.」解析不出来、或者填 0，用户需要看到的都是这一句
                        rejected = true
                    } else if (selectedCategory.isNotEmpty()) {
                        onAdd(
                            cents,
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
