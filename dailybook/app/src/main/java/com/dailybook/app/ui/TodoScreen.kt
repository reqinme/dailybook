package com.dailybook.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dailybook.app.MainViewModel
import com.dailybook.app.TodoBucket
import com.dailybook.app.TodoFilter
import com.dailybook.app.TodoSection
import com.dailybook.app.UiState
import com.dailybook.app.data.RepeatRule
import com.dailybook.app.data.SubtaskEntity
import com.dailybook.app.data.TodoEntity
import com.dailybook.app.data.TodoPriority
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.TodoStrings
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.util.formatDueLabel
import com.dailybook.app.util.isOverdue
import com.dailybook.app.util.toDayMillis
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt

/** 拖拽排序的阈值：一行大约 56dp，稍微压低一点，拖过一半就换位，手感更跟手 */
private const val ROW_HEIGHT_DP = 56

/** 列表 / 看板两种视图 */
private enum class TodoViewMode { LIST, KANBAN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    state: UiState,
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    var newTitle by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<TodoEntity?>(null) }
    var confirmClearDone by remember { mutableStateOf(false) }
    // 编辑弹窗里点了「删除」之后先停在这里：等用户在二次确认里真点了删除才落库。
    // 删待办会**连它的子任务一起删**（DailyRepository.deleteTodo），一条误触就整份清单没了。
    var pendingDelete by remember { mutableStateOf<TodoEntity?>(null) }
    // 视图选择要能扛住旋转 / 进程重建
    var viewMode by rememberSaveable { mutableStateOf(TodoViewMode.LIST) }
    val focusManager = LocalFocusManager.current
    val lang = LocalLang.current

    fun submit() {
        if (newTitle.isBlank()) return
        vm.addTodo(newTitle, null)
        newTitle = ""
        focusManager.clearFocus()
    }

    val searching = state.todoQuery.isNotBlank()
    val showKanban = viewMode == TodoViewMode.KANBAN
    // 拖拽只在「简单列表」里可用：搜索、已完成筛选、看板视图下顺序不是用户在拖的那份
    val reorderEnabled = !searching && state.todoFilter != TodoFilter.DONE && !showKanban
    val dragUnavailableHint = when {
        searching -> TodoStrings.dragHintNoSearch(lang)
        showKanban -> TodoStrings.viewKanban(lang)
        else -> TodoStrings.dragHintNoFilter(lang)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = AppStrings.tabTodo(lang),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = TodoStrings.counts(lang, state.pendingCount, state.doneCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.doneCount > 0) {
                TextButton(onClick = { confirmClearDone = true }) {
                    Text(TodoStrings.clearDone(lang))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { if (it.length <= 60) newTitle = it },
                placeholder = { Text(TodoStrings.addPlaceholder(lang)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() })
            )
            Spacer(Modifier.width(10.dp))
            FilledIconButton(
                onClick = { submit() },
                enabled = newTitle.isNotBlank(),
                modifier = Modifier.heightIn(min = 52.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = TodoStrings.addTodo(lang))
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = TodoStrings.hint(lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))
        SearchField(
            value = state.todoQuery,
            onValueChange = vm::setTodoQuery,
            placeholder = TodoStrings.searchPlaceholder(lang)
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TodoFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.todoFilter == filter,
                    onClick = { vm.setTodoFilter(filter) },
                    label = { Text(filter.label(lang)) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TodoViewMode.entries.forEach { mode ->
                FilterChip(
                    selected = viewMode == mode,
                    onClick = { viewMode = mode },
                    label = {
                        Text(
                            if (mode == TodoViewMode.LIST) TodoStrings.viewList(lang)
                            else TodoStrings.viewKanban(lang)
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        if (state.visibleTodos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyHint(
                    emoji = "✅",
                    title = when {
                        searching -> TodoStrings.emptyNoMatch(lang)
                        state.todoFilter == TodoFilter.PENDING -> TodoStrings.emptyNoPending(lang)
                        state.todoFilter == TodoFilter.DONE -> TodoStrings.emptyNoDone(lang)
                        else -> TodoStrings.emptyNoTodos(lang)
                    },
                    subtitle = if (searching) TodoStrings.emptyTryAnotherKeyword(lang) else null
                )
            }
        } else {
            // 提示只在「本来可以拖、现在拖不了」时说，免得平时占地方
            if (!reorderEnabled) {
                Text(
                    text = dragUnavailableHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
            } else {
                Text(
                    text = TodoStrings.dragHintList(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
            }

            if (showKanban) {
                KanbanContent(
                    state = state,
                    vm = vm,
                    onEdit = { editing = it }
                )
            } else {
                TodoListContent(
                    state = state,
                    vm = vm,
                    dragEnabled = reorderEnabled,
                    onEdit = { editing = it }
                )
            }
        }
    }

    if (confirmClearDone) {
        ConfirmDialog(
            title = TodoStrings.confirmClearDoneTitle(lang, state.doneCount),
            text = TodoStrings.confirmClearDoneText(lang),
            confirmText = TodoStrings.confirmClearDoneAction(lang),
            onConfirm = { vm.clearCompletedTodos() },
            onDismiss = { confirmClearDone = false }
        )
    }

    editing?.let { item ->
        EditTodoDialog(
            item = item,
            subtasks = state.subtasksByTodo[item.id].orEmpty(),
            onDismiss = { editing = null },
            onSave = { title, important, dueMillis, repeat, priority ->
                // VM 的 updateTodo 没有 priority 参数，所以把优先级折进实体一起写回
                vm.updateTodo(item.copy(priority = priority.name), title, important, dueMillis, repeat)
                editing = null
            },
            onDelete = {
                // 先关掉编辑弹窗、把待删的那条挂起来，走下面那次二次确认。
                // 以前这里直接 vm.deleteTodo(item)：按钮和「保存」挨着，点错就永久删掉待办 + 全部子任务。
                pendingDelete = item
                editing = null
            },
            onAddSubtask = { vm.addSubtask(item.id, it) },
            onToggleSubtask = { vm.toggleSubtask(it) },
            onDeleteSubtask = { vm.deleteSubtask(it) }
        )
    }

    // 删除待办的二次确认：和「清除已完成」用同一个 ConfirmDialog。
    // 文案里带上子任务条数 —— 那些子任务也会一起没，不说清楚就等于静默连坐。
    pendingDelete?.let { item ->
        val subtaskCount = state.subtasksByTodo[item.id].orEmpty().size
        ConfirmDialog(
            title = TodoStrings.confirmDeleteTodoTitle(lang),
            text = TodoStrings.confirmDeleteTodoText(lang, subtaskCount),
            confirmText = AppStrings.delete(lang),
            onConfirm = { vm.deleteTodo(item) },
            onDismiss = { pendingDelete = null }
        )
    }
}

// ============================================================
// 简单列表 + 拖拽排序
// ============================================================

/** 在父级 Column 里占满剩余高度，所以接收者是 [ColumnScope] */
@Composable
private fun ColumnScope.TodoListContent(
    state: UiState,
    vm: MainViewModel,
    dragEnabled: Boolean,
    onEdit: (TodoEntity) -> Unit
) {
    val items = state.visibleTodos
    // 拖动过程中只挪「画面」，真实的待办列表等松手后一次性提交，
    // 这样 LazyColumn 的 item 身份不会在手势中途被换掉，手势不会被重组打断。
    val shifts = remember { mutableStateMapOf<Long, Float>() }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }
    var rawDrag by remember { mutableFloatStateOf(0f) }
    var dropIndex by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current

    // 列表内容变了（删除、改筛选）就把残留的位移清掉，避免错位
    LaunchedEffect(items) {
        val alive = items.mapTo(HashSet()) { it.id }
        shifts.keys.toList().forEach { key -> if (key !in alive) shifts.remove(key) }
    }

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(items = items, key = { it.id }) { todo ->
            val shift = shifts[todo.id] ?: 0f
            val dragging = draggingId == todo.id
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = shift }
                    .onSizeChanged { if (it.height > 0) rowHeightPx = it.height.toFloat() }
                    .then(
                        if (!dragEnabled) Modifier
                        else Modifier.pointerInput(items) {
                            // 长按（不是点击）才进入拖拽：点击仍旧是打开编辑。
                            // 注意：detectDragGesturesAfterLongPress 的四个回调是**同一次调用的具名参数**，
                            // 不是嵌套的 onDrag {} / onDragEnd {}（那样并不存在）。
                            var dragId: Long? = null
                            detectDragGesturesAfterLongPress(
                                onDragStart = { _ ->
                                    val startIndex = items.indexOfFirst { it.id == todo.id }
                                    if (startIndex >= 0) {
                                        dragId = todo.id
                                        draggingId = todo.id
                                        dropIndex = startIndex
                                        rawDrag = 0f
                                        shifts.clear()
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val id = dragId
                                    // 手指可能已经拖到别的行上，那时每一行都会收到 onDrag；
                                    // 只让被拖的那一行真正处理，否则位移会被算两遍。
                                    if (id == null || id != todo.id) {
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    val reference = if (rowHeightPx > 0f) {
                                        rowHeightPx
                                    } else {
                                        ROW_HEIGHT_DP.dp.toPx()
                                    }
                                    rawDrag += dragAmount.y
                                    val from = items.indexOfFirst { it.id == id }
                                    if (from < 0) return@detectDragGesturesAfterLongPress
                                    val to = (from + (rawDrag / reference).roundToInt())
                                        .coerceIn(0, items.lastIndex)
                                    shifts.clear()
                                    items.forEachIndexed { i, other ->
                                        if (other.id == id) return@forEachIndexed
                                        val moved = when {
                                            to > from && i in (from + 1)..to -> -1
                                            to < from && i in to until from -> 1
                                            else -> 0
                                        }
                                        if (moved != 0) shifts[other.id] = moved * reference
                                    }
                                    // 被拖的那行：手里的位移换算成「目标槽位」下的局部位移，
                                    // 换槽的瞬间正好抵消，手指底下的那一行看起来是连续移动的。
                                    shifts[id] = rawDrag + (from - to) * reference
                                    dropIndex = to
                                },
                                onDragEnd = {
                                    val id = dragId
                                    dragId = null
                                    draggingId = null
                                    shifts.clear()
                                    rawDrag = 0f
                                    if (id != null) {
                                        val from = items.indexOfFirst { it.id == id }
                                        if (from >= 0 && dropIndex != from) {
                                            vm.reorderTodos(insertAt(items, from, dropIndex))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    dragId = null
                                    draggingId = null
                                    shifts.clear()
                                    rawDrag = 0f
                                }
                            )
                        }
                    )
            ) {
                TodoRow(
                    todo = todo,
                    progress = subtaskProgress(state.subtasksByTodo[todo.id]),
                    isFocusTarget = state.focusTaskId == todo.id,
                    onToggle = { vm.toggleTodoDone(todo) },
                    onStar = { vm.toggleTodoImportant(todo) },
                    onFocusTarget = { vm.toggleFocusTask(todo) },
                    onEdit = { onEdit(todo) },
                    onPriority = { vm.setTodoPriority(todo, it) }
                )
            }
        }
    }
}

/** 把 [from] 位置的元素挪到 [to] 位置，其余保持原有顺序 */
private fun insertAt(items: List<TodoEntity>, from: Int, to: Int): List<TodoEntity> {
    if (from == to || from !in items.indices) return items
    val mutable = items.toMutableList()
    val moved = mutable.removeAt(from)
    mutable.add(to.coerceIn(0, mutable.size), moved)
    return mutable
}

// ============================================================
// 看板视图
// ============================================================

/**
 * 看板：按 [UiState.kanbanSections] 分组。
 * 搜索时不用 state.kanbanSections（它不带搜索词），而是把过滤后的可见待办按同一套规则重新分桶，
 * 这样搜索结果的视图切换不会突然跳回列表。空分组直接不画。
 */
@Composable
private fun ColumnScope.KanbanContent(
    state: UiState,
    vm: MainViewModel,
    onEdit: (TodoEntity) -> Unit
) {
    val lang = LocalLang.current
    val searching = state.todoQuery.isNotBlank()
    val sections = remember(searching, state.kanbanSections, state.visibleTodos) {
        if (searching) groupForKanban(state.visibleTodos) else state.kanbanSections
    }

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        sections.forEach { section ->
            item(key = "kanban-header-" + section.bucket.name) {
                KanbanHeader(label = bucketLabel(section.bucket, lang), count = section.items.size, lang = lang)
            }
            items(items = section.items, key = { it.id }) { todo ->
                TodoRow(
                    todo = todo,
                    progress = subtaskProgress(state.subtasksByTodo[todo.id]),
                    isFocusTarget = state.focusTaskId == todo.id,
                    onToggle = { vm.toggleTodoDone(todo) },
                    onStar = { vm.toggleTodoImportant(todo) },
                    onFocusTarget = { vm.toggleFocusTask(todo) },
                    onEdit = { onEdit(todo) },
                    onPriority = { vm.setTodoPriority(todo, it) }
                )
            }
        }
    }
}

@Composable
private fun KanbanHeader(label: String, count: Int, lang: Lang) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TodoStrings.kanbanSectionTitle(lang, label, count),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

/** 与 VM 的看板分桶规则保持一致（今天 / 本周 / 以后 / 没日期 / 已完成） */
private fun bucketOf(todo: TodoEntity, today: LocalDate): TodoBucket {
    if (todo.done) return TodoBucket.DONE
    val due = todo.dueMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        ?: return TodoBucket.NO_DATE
    return when {
        !due.isAfter(today) -> TodoBucket.TODAY
        due.isBefore(today.plusDays(7)) -> TodoBucket.THIS_WEEK
        else -> TodoBucket.LATER
    }
}

private fun groupForKanban(
    todos: List<TodoEntity>,
    today: LocalDate = LocalDate.now()
): List<TodoSection> = TodoBucket.entries
    .map { bucket ->
        TodoSection(
            bucket = bucket,
            items = todos.filter { bucketOf(it, today) == bucket }
                .sortedWith(compareByDescending<TodoEntity> { it.important })
        )
    }
    .filter { it.items.isNotEmpty() }

private fun bucketLabel(bucket: TodoBucket, lang: Lang): String = when (bucket) {
    TodoBucket.TODAY -> AppStrings.kanbanToday(lang)
    TodoBucket.THIS_WEEK -> AppStrings.kanbanThisWeek(lang)
    TodoBucket.LATER -> AppStrings.kanbanLater(lang)
    TodoBucket.NO_DATE -> AppStrings.kanbanNoDate(lang)
    TodoBucket.DONE -> AppStrings.kanbanDone(lang)
}

// ============================================================
// 列表行
// ============================================================

@Composable
private fun TodoRow(
    todo: TodoEntity,
    progress: Pair<Int, Int>?,
    isFocusTarget: Boolean,
    onToggle: () -> Unit,
    onStar: () -> Unit,
    onFocusTarget: () -> Unit,
    onEdit: () -> Unit,
    onPriority: (TodoPriority) -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (isFocusTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surface,
        label = "todoContainer"
    )
    val lang = LocalLang.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(container, Shapes.card)
            .clickable { onEdit() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = todo.done, onCheckedChange = { onToggle() })

        Column(Modifier.weight(1f)) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None
            )
            if (progress != null) {
                Spacer(Modifier.height(4.dp))
                SubtaskProgress(done = progress.first, total = progress.second, lang = lang)
            }
            if (isFocusTarget || todo.dueMillis != null || todo.repeats) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFocusTarget) {
                        Text(
                            text = TodoStrings.focusTarget(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (todo.dueMillis != null || todo.repeats) Spacer(Modifier.width(8.dp))
                    }
                    todo.dueMillis?.let { due ->
                        val overdue = !todo.done && isOverdue(due)
                        Text(
                            text = formatDueLabel(due, lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overdue) expenseColor()
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (todo.repeats) {
                        if (todo.dueMillis != null) Spacer(Modifier.width(8.dp))
                        Text(
                            text = TodoStrings.repeatPrefix(lang, todo.repeat.label(lang)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        TodoPriorityMenu(
            current = todo.priorityLevel,
            onPick = onPriority
        )
        IconButton(onClick = onFocusTarget) {
            Icon(
                imageVector = Icons.Filled.CenterFocusStrong,
                contentDescription = if (isFocusTarget) TodoStrings.unsetFocusTarget(lang) else TodoStrings.setFocusTarget(lang),
                tint = if (isFocusTarget) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onStar) {
            Icon(
                imageVector = if (todo.important) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = TodoStrings.important(lang),
                tint = if (todo.important) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 行上的优先级角标：点开一个小下拉直接改。
 * 「普通」不画符号（角标空着），低 / 高 / 紧急分别带 ▽ / ▲ / 🔥。
 */
@Composable
private fun TodoPriorityMenu(
    current: TodoPriority,
    onPick: (TodoPriority) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val lang = LocalLang.current
    val marker = current.emoji()

    Box {
        Row(
            modifier = Modifier
                .background(
                    if (current == TodoPriority.NORMAL) MaterialTheme.colorScheme.surfaceVariant
                        .copy(alpha = 0.35f)
                    else priorityColor(current).copy(alpha = 0.16f),
                    Shapes.pill
                )
                .clickable { open = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (marker.isEmpty()) TodoStrings.prioritySet(lang)
                else TodoStrings.priorityBadge(lang, marker, current.label(lang)),
                style = MaterialTheme.typography.labelSmall,
                color = if (current == TodoPriority.NORMAL) MaterialTheme.colorScheme.onSurfaceVariant
                else priorityColor(current)
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            TodoPriority.entries.forEach { level ->
                val leading = level.emoji()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (leading.isEmpty()) level.label(lang)
                            else leading + " " + level.label(lang),
                            color = if (level == current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        open = false
                        if (level != current) onPick(level)
                    }
                )
            }
        }
    }
}

@Composable
private fun SubtaskProgress(done: Int, total: Int, lang: Lang) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = TodoStrings.subtaskProgress(lang, done, total),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        ThinProgressBar(
            ratio = if (total <= 0) 0f else done.toFloat() / total.toFloat(),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(64.dp),
            height = 4.dp
        )
    }
}

/** 有子任务就返回 (已完成数, 总数)，否则 null（行上不显示进度） */
private fun subtaskProgress(list: List<SubtaskEntity>?): Pair<Int, Int>? {
    if (list.isNullOrEmpty()) return null
    return list.count { it.done } to list.size
}

/** 优先级配色：低 / 普通用中性色（普通本来就低调），高用次要色，紧急用支出红 */
@Composable
private fun priorityColor(level: TodoPriority) = when (level) {
    TodoPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    TodoPriority.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    TodoPriority.HIGH -> MaterialTheme.colorScheme.secondary
    TodoPriority.URGENT -> expenseColor()
}

/** 子任务行：勾选 / 划线 / 删除 */
@Composable
private fun SubtaskRow(
    item: SubtaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.done, onCheckedChange = { onToggle() })
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = TodoStrings.subtaskDelete(LocalLang.current),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================
// 编辑弹窗
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTodoDialog(
    item: TodoEntity,
    subtasks: List<SubtaskEntity>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        important: Boolean,
        dueMillis: Long?,
        repeatRule: RepeatRule,
        priority: TodoPriority
    ) -> Unit,
    onDelete: () -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onDeleteSubtask: (SubtaskEntity) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var important by remember { mutableStateOf(item.important) }
    var repeat by remember { mutableStateOf(item.repeat) }
    var priority by remember { mutableStateOf(item.priorityLevel) }
    var newSubtask by remember { mutableStateOf("") }
    var dueDate by remember {
        mutableStateOf(
            item.dueMillis?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        )
    }
    var showPicker by remember { mutableStateOf(false) }
    val lang = LocalLang.current
    val focusManager = LocalFocusManager.current

    fun submitSubtask() {
        val text = newSubtask.trim()
        if (text.isEmpty()) return
        onAddSubtask(text)
        newSubtask = ""
        focusManager.clearFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(TodoStrings.editTitle(lang)) },
        text = {
            // Column 自带纵向滚动，内容多了（子任务长）也不会把按钮顶出屏幕
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text(TodoStrings.fieldContent(lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(TodoStrings.importantMarker(lang), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = important, onCheckedChange = { important = it })
                }

                // ---- 优先级 ----
                Spacer(Modifier.height(12.dp))
                Text(TodoStrings.priorityLabel(lang), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                ChipFlow {
                    TodoPriority.entries.forEach { level ->
                        val leading = level.emoji()
                        FilterChip(
                            selected = priority == level,
                            onClick = { priority = level },
                            label = {
                                Text(
                                    if (leading.isEmpty()) level.label(lang)
                                    else leading + " " + level.label(lang)
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(TodoStrings.dueDateLabel(lang), style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dueDate?.let { AppStrings.monthDay(lang, it.monthValue, it.dayOfMonth) }
                                ?: AppStrings.notSet(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { showPicker = true }) { Text(AppStrings.select(lang)) }
                        if (dueDate != null) {
                            TextButton(onClick = { dueDate = null }) { Text(AppStrings.clear(lang)) }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 逾期任务最常用的一步：直接推到明天，不用再翻日历
                    OutlinedButton(onClick = { dueDate = LocalDate.now().plusDays(1) }) {
                        Text(TodoStrings.postponeToTomorrow(lang))
                    }
                    val overdueDate = dueDate
                    if (overdueDate != null && isOverdue(overdueDate.toDayMillis())) {
                        Text(
                            text = formatDueLabel(overdueDate.toDayMillis(), lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = expenseColor(),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(TodoStrings.repeatLabel(lang), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                ChipFlow {
                    RepeatRule.entries.forEach { rule ->
                        FilterChip(
                            selected = repeat == rule,
                            onClick = { repeat = rule },
                            label = { Text(rule.label(lang)) }
                        )
                    }
                }
                if (repeat != RepeatRule.NONE) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (dueDate == null) TodoStrings.autoNextHintNoDue(lang)
                        else TodoStrings.autoNextHintDue(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ---- 子任务 / 清单 ----
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = TodoStrings.subtaskSection(lang, subtasks.count { it.done }, subtasks.size),
                    style = MaterialTheme.typography.bodyMedium
                )
                subtasks.forEach { subtask ->
                    SubtaskRow(
                        item = subtask,
                        onToggle = { onToggleSubtask(subtask) },
                        onDelete = { onDeleteSubtask(subtask) }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSubtask,
                        onValueChange = { if (it.length <= 60) newSubtask = it },
                        placeholder = { Text(TodoStrings.subtaskPlaceholder(lang)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitSubtask() })
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { submitSubtask() },
                        enabled = newSubtask.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = TodoStrings.subtaskAdd(lang))
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = TodoStrings.reminderNotice(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), important, dueDate?.toDayMillis(), repeat, priority)
                    }
                }
            ) { Text(AppStrings.save(lang)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(AppStrings.delete(lang), color = expenseColor())
                }
                TextButton(onClick = onDismiss) { Text(AppStrings.cancel(lang)) }
            }
        }
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate?.toDayMillis() ?: LocalDate.now().toDayMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = pickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
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
