package com.dailybook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.TodoFilter
import com.dailybook.app.UiState
import com.dailybook.app.data.TodoEntity
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.util.formatDueLabel
import com.dailybook.app.util.isOverdue
import com.dailybook.app.util.toDayMillis
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun TodoScreen(
    state: UiState,
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    var newTitle by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<TodoEntity?>(null) }
    val focusManager = LocalFocusManager.current

    fun submit() {
        if (newTitle.isBlank()) return
        vm.addTodo(newTitle, null)
        newTitle = ""
        focusManager.clearFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = "待办",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "待完成 ${state.pendingCount} 项 · 已完成 ${state.doneCount} 项",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { if (it.length <= 60) newTitle = it },
                placeholder = { Text("添加待办…") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() })
            )
            Spacer(Modifier.width(10.dp))
            FilledIconButton(
                onClick = { submit() },
                enabled = newTitle.isNotBlank()
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加")
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TodoFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.todoFilter == filter,
                    onClick = { vm.setTodoFilter(filter) },
                    label = { Text(filter.label) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        if (state.visibleTodos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "✅", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = when (state.todoFilter) {
                            TodoFilter.ALL -> "还没有待办，添加一条试试"
                            TodoFilter.PENDING -> "没有未完成的待办，很棒！"
                            TodoFilter.DONE -> "还没有完成任何待办"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(items = state.visibleTodos, key = { it.id }) { todo ->
                    TodoRow(
                        todo = todo,
                        onToggle = { vm.toggleTodoDone(todo) },
                        onStar = { vm.toggleTodoImportant(todo) },
                        onDelete = { vm.deleteTodo(todo) },
                        onEdit = { editing = todo }
                    )
                }
            }
        }
    }

    editing?.let { item ->
        EditTodoDialog(
            item = item,
            onDismiss = { editing = null },
            onSave = { title, important, dueMillis ->
                vm.updateTodo(item, title, important, dueMillis)
                editing = null
            },
            onDelete = {
                vm.deleteTodo(item)
                editing = null
            }
        )
    }
}

@Composable
private fun TodoRow(
    todo: TodoEntity,
    onToggle: () -> Unit,
    onStar: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
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
            todo.dueMillis?.let { due ->
                val overdue = !todo.done && isOverdue(due)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatDueLabel(due),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) expenseColor() else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onStar) {
            Icon(
                imageVector = if (todo.important) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "重要",
                tint = if (todo.important) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTodoDialog(
    item: TodoEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, important: Boolean, dueMillis: Long?) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var important by remember { mutableStateOf(item.important) }
    var dueDate by remember {
        mutableStateOf(item.dueMillis?.let { Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() })
    }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑待办") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text("内容") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("标记为重要", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = important, onCheckedChange = { important = it })
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("到期日", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dueDate?.let { "${it.monthValue}月${it.dayOfMonth}日" } ?: "未设置",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { showPicker = true }) { Text("选择") }
                        if (dueDate != null) {
                            TextButton(onClick = { dueDate = null }) { Text("清除") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), important, dueDate?.toDayMillis())
                    }
                }
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("删除", color = expenseColor())
                }
                TextButton(onClick = onDismiss) { Text("取消") }
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
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
