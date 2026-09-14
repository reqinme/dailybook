package com.dailybook.app.ui.life

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.HabitEntity
import com.dailybook.app.data.HabitLogEntity
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.LifeStrings
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.ui.ChipFlow
import com.dailybook.app.ui.ConfirmDialog
import com.dailybook.app.ui.EmptyHint
import com.dailybook.app.ui.FieldLabel
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.Shapes
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import java.time.LocalDate

/**
 * 习惯打卡。
 *
 * 一屏一件事：今天有没有做到。
 * - 每行左边是习惯本体，点主体部分 = 打卡开关（交给 [MainViewModel.toggleHabitToday]）；
 * - 右边的 ＋ / － 是「今天的量再加减一次」，走 [MainViewModel.logHabit]；
 * - 下方的 7 个小圆点是最近一周，有记录的那天填实心。
 *
 * 连续天数 / 本周天数都来自 [UiState.habitStreak] 与 [UiState.habitWeekDone]，
 * 这里不再自己算一遍（算法只有一处，界面和提醒才不会有分歧）。
 *
 * 本页不发任何导航（没有子页面），但签名保持和父级统一调用的形式一致。
 */
@Composable
fun HabitsScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val todayMillis = remember { LocalDate.now().toDayMillis() }
    val logsByDay = remember(state.habitLogs) { logsByHabitDay(state.habitLogs) }

    var editing by remember { mutableStateOf<HabitEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<HabitEntity?>(null) }

    val doneToday = state.habits.count { (state.habitToday[it.id] ?: 0) >= it.targetPerDay }

    // 根容器用 Box：内容一列，FAB 贴在右下角叠在上面。
    // FAB 在自己的 Box 里只占按钮大小，不会挡住列表的触摸事件。
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = LifeStrings.habitsTitle(lang),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = LifeStrings.habitsCounts(lang, doneToday, state.habits.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            if (state.habits.isNotEmpty()) {
                Text(
                    text = LifeStrings.habitsRowHint(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                HabitsContent(
                    habits = state.habits,
                    logsByDay = logsByDay,
                    todayMillis = todayMillis,
                    state = state,
                    onToggle = { vm.toggleHabitToday(it) },
                    onLog = { habit, count -> vm.logHabit(habit.id, todayMillis, count) },
                    onEdit = { editing = it }
                )
            } else {
                // 空状态：这里没有 LazyColumn，所以直接居中一块提示（不套滚动容器）
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyHint(
                        emoji = "✅",
                        title = LifeStrings.habitsEmpty(lang),
                        subtitle = LifeStrings.habitsEmptyHint(lang),
                        modifier = Modifier.padding(bottom = 40.dp)
                    )
                }
            }
        }

        FilledIconButton(
            onClick = { adding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = LifeStrings.habitAdd(lang))
        }
    }

    if (adding) {
        HabitDialog(
            habit = null,
            onDismiss = { adding = false },
            onSave = { name, emoji, target, unit, days ->
                vm.addHabit(name, emoji, target, unit, days)
                adding = false
            },
            onDelete = null
        )
    }

    editing?.let { habit ->
        HabitDialog(
            habit = habit,
            onDismiss = { editing = null },
            onSave = { name, emoji, target, unit, days ->
                vm.updateHabit(
                    habit.copy(
                        name = name,
                        emoji = emoji,
                        targetPerDay = target,
                        unit = unit,
                        daysPerWeek = days
                    )
                )
                editing = null
            },
            onDelete = {
                deleting = habit
                editing = null
            }
        )
    }

    deleting?.let { habit ->
        ConfirmDialog(
            title = LifeStrings.habitDeleteTitle(lang, habit.name),
            text = LifeStrings.habitDeleteText(lang),
            confirmText = AppStrings.delete(lang),
            onConfirm = { vm.deleteHabit(habit) },
            onDismiss = { deleting = null }
        )
    }
}

/**
 * 打卡记录按「习惯 → 日期」聚合。
 * 表里一天只有一条（DAO 的 logOf 保证），但导入 / 恢复备份可能带来重复，
 * 所以这里按日期累加：同一天两条只会显示成一个点，不会多画。
 */
private fun logsByHabitDay(logs: List<HabitLogEntity>): Map<Long, Map<Long, Int>> =
    logs.groupBy { it.habitId }
        .mapValues { (_, items) ->
            val byDay = HashMap<Long, Int>()
            items.forEach { log ->
                byDay[log.dateMillis] = (byDay[log.dateMillis] ?: 0) + log.count
            }
            byDay
        }

/** 在父级 Column 里占满剩余高度，所以接收者是 [ColumnScope] */
@Composable
private fun ColumnScope.HabitsContent(
    habits: List<HabitEntity>,
    logsByDay: Map<Long, Map<Long, Int>>,
    todayMillis: Long,
    state: UiState,
    onToggle: (HabitEntity) -> Unit,
    onLog: (HabitEntity, Int) -> Unit,
    onEdit: (HabitEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(items = habits, key = { it.id }) { habit ->
            HabitRow(
                habit = habit,
                todayCount = state.habitToday[habit.id] ?: 0,
                streak = state.habitStreak[habit.id] ?: 0,
                weekDone = state.habitWeekDone[habit.id] ?: 0,
                days = logsByDay[habit.id].orEmpty(),
                todayMillis = todayMillis,
                onToggle = { onToggle(habit) },
                onLog = { onLog(habit, it) },
                onEdit = { onEdit(habit) }
            )
        }
    }
}

@Composable
private fun HabitRow(
    habit: HabitEntity,
    todayCount: Int,
    streak: Int,
    weekDone: Int,
    days: Map<Long, Int>,
    todayMillis: Long,
    onToggle: () -> Unit,
    onLog: (Int) -> Unit,
    onEdit: () -> Unit
) {
    val lang = LocalLang.current
    val done = todayCount >= habit.targetPerDay

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, Shapes.card)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 主体：点一下就算今天打卡（再点一下取消）
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onToggle() }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (done) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = habit.emoji, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (done) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = LifeStrings.habitProgress(lang, todayCount, habit.targetPerDay, habit.unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (done) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = LifeStrings.habitStreak(lang, streak),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = LifeStrings.habitWeekDone(lang, weekDone),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                MiniWeekGrid(days = days, todayMillis = todayMillis, target = habit.targetPerDay)
            }
        }

        // 右侧：今天的量 ±1（和打卡开关分开，避免误触把数量清零）
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalIconButton(
                onClick = { onLog(todayCount + 1) },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = LifeStrings.habitIncrease(lang),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = { onLog(todayCount - 1) },
                enabled = todayCount > 0,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = LifeStrings.habitDecrease(lang),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = LifeStrings.habitEdit(lang),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 最近 7 天的小格子：有记录的那天画实心圆，达到目标的再深一档。
 * 数据来自 [UiState.habitLogs]，按天聚合后由 [logsByHabitDay] 传进来。
 */
@Composable
private fun MiniWeekGrid(days: Map<Long, Int>, todayMillis: Long, target: Int) {
    val today = todayMillis.toLocalDate()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (6 downTo 0).forEach { back ->
            val date = today.minusDays(back.toLong())
            val count = days[date.toDayMillis()] ?: 0
            val filled = count > 0
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        when {
                            count >= target.coerceAtLeast(1) -> MaterialTheme.colorScheme.primary
                            filled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        CircleShape
                    )
            )
        }
    }
}

/**
 * 新建 / 编辑习惯用同一个弹窗（[habit] 为 null 就是新建）。
 * 名称和单位是数据，不翻译；单位给 次 / 个 / 页 / 分钟 四个常用值，也可以自己填。
 */
@Composable
private fun HabitDialog(
    habit: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, target: Int, unit: String, daysPerWeek: Int) -> Unit,
    onDelete: (() -> Unit)?
) {
    val lang = LocalLang.current
    var name by remember { mutableStateOf(habit?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(habit?.emoji ?: EMOJI_PRESETS.first()) }
    var targetText by remember { mutableStateOf((habit?.targetPerDay ?: 1).toString()) }
    var unit by remember { mutableStateOf(habit?.unit ?: UNITS.first()) }
    var daysPerWeek by remember { mutableStateOf(habit?.daysPerWeek ?: 7) }

    val target = targetText.toIntOrNull()
    val canSave = name.isNotBlank() && target != null && target > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (habit == null) LifeStrings.habitAdd(lang) else LifeStrings.habitEdit(lang))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 24) name = it },
                    label = { Text(LifeStrings.habitFieldName(lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                FieldLabel(LifeStrings.habitFieldEmoji(lang))
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    // emoji 是数据，不翻译
                    EMOJI_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = emoji == preset,
                            onClick = { emoji = preset },
                            label = { Text(preset) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) targetText = input
                        },
                        label = { Text(LifeStrings.habitFieldTarget(lang)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.width(150.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(12.dp))
                FieldLabel(LifeStrings.habitFieldUnit(lang))
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    UNITS.forEach { option ->
                        FilterChip(
                            selected = unit == option,
                            onClick = { unit = option },
                            label = { Text(option) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                FieldLabel(LifeStrings.habitFieldDaysPerWeek(lang))
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    (1..7).forEach { option ->
                        FilterChip(
                            selected = daysPerWeek == option,
                            onClick = { daysPerWeek = option },
                            label = { Text(option.toString()) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = LifeStrings.habitsRowHint(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val days = target
                    if (canSave && days != null) {
                        onSave(name.trim(), emoji, days, unit, daysPerWeek)
                    }
                }
            ) { Text(AppStrings.save(lang)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(AppStrings.delete(lang)) }
                }
                TextButton(onClick = onDismiss) { Text(AppStrings.cancel(lang)) }
            }
        }
    )
}

/** 常用图标；emoji 属于数据，不参与翻译 */
private val EMOJI_PRESETS = listOf(
    "✅", "💪", "📖", "💧", "🏃", "🧘", "🌙", "🍎", "✍️", "🎯"
)

/** 常用单位；同样属于数据 */
private val UNITS = listOf("次", "个", "页", "分钟")
