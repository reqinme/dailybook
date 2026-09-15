package com.dailybook.app.ui.study

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.TodoEntity
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.StudyStrings
import com.dailybook.app.ui.ChipFlow
import com.dailybook.app.ui.ConfirmDialog
import com.dailybook.app.ui.EmptyHint
import com.dailybook.app.ui.FieldLabel
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.Shapes
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.util.formatDueLabel
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 作业 / DDL。
 *
 * 作业不是新表：它就是**带课程名的待办**（[UiState.assignmentTodos] = courseName 非空的 todos），
 * 所以这里勾选用的就是 vm.toggleTodoDone，和「待办」标签页看到的是同一份数据。
 *
 * 分组：已逾期 / 今天到期 / 本周内 / 以后 / 没有日期 / 已完成。
 * 逾期那一组的标题与日期会用错误色标出来 —— 那是整页最该被一眼看到的东西。
 *
 * 「课程」这个字段：作业的判定就是 `courseName` 非空（见上），所以**新建时必填**
 * （不填就只会进待办，点了保存却在这一页看不到，像是保存失败），
 * **编辑时清空要先确认**（这条会离开作业页回到待办）—— 以前两种情况都没有任何提示，
 * 用户只看到自己的作业「凭空消失」。标签因此**不再**写「（可选）」（[StudyStrings.assignmentsCourseField]）：
 * 一个写着「可选」的字段，行为却是「不填就离开这一页」，标签和行为必须一致。
 * 清空本身仍然允许，只是要先确认，并把后果说清楚
 * （[StudyStrings.assignmentsCourseRule] 常驻在字段下面）。
 */

/** 分组枚举：ordinal 就是显示顺序 */
private enum class AssignmentBucket { OVERDUE, TODAY, THIS_WEEK, LATER, NO_DATE, DONE }

@Composable
fun AssignmentsScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val today = LocalDate.now()

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TodoEntity?>(null) }
    var deleting by remember { mutableStateOf<TodoEntity?>(null) }

    // 分桶：已完成单独一组，逾期排最前
    val sections = remember(state.assignmentTodos, today) {
        state.assignmentTodos
            .groupBy { bucketOf(it, today) }
            .toList()
            .sortedBy { (bucket, _) -> bucket.ordinal }
            .filter { (_, items) -> items.isNotEmpty() }
    }
    val openCount = state.assignmentTodos.count { !it.done }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "assignments-header") {
                Column {
                    Text(
                        text = StudyStrings.assignmentsTitle(lang),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = StudyStrings.assignmentsSummary(
                            lang,
                            openCount,
                            state.overdueAssignments
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.overdueAssignments > 0) expenseColor()
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = StudyStrings.assignmentsHint(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            if (state.assignmentTodos.isEmpty()) {
                item(key = "assignments-empty") {
                    Spacer(Modifier.height(30.dp))
                    EmptyHint(
                        emoji = "📄",
                        title = StudyStrings.assignmentsEmpty(lang),
                        subtitle = StudyStrings.assignmentsEmptyHint(lang)
                    )
                }
            }

            sections.forEach { (bucket, items) ->
                item(key = "assignments-section-" + bucket.name) {
                    SectionHeader(
                        label = bucketLabel(bucket, lang),
                        count = items.size,
                        overdue = bucket == AssignmentBucket.OVERDUE
                    )
                }
                items(items = items, key = { it.id }) { todo ->
                    AssignmentRow(
                        todo = todo,
                        lang = lang,
                        onToggle = { vm.toggleTodoDone(todo) },
                        onEdit = { editing = todo },
                        onDelete = { deleting = todo }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = StudyStrings.assignmentsAddTitle(lang)
            )
        }
    }

    if (adding) {
        AssignmentDialog(
            existing = null,
            courses = state.courses.map { it.name },
            onDismiss = { adding = false },
            onSave = { title, course, due ->
                // 作业 = 带课程名的待办。走专门的那个入口，课程名跟待办一起落库，
                // 不需要先建后改（MainViewModel.addAssignment 内部就是 repo.addTodo(courseName = …)）。
                vm.addAssignment(title, course, due)
                adding = false
            }
        )
    }

    editing?.let { todo ->
        AssignmentDialog(
            existing = todo,
            courses = state.courses.map { it.name },
            onDismiss = { editing = null },
            onSave = { title, course, due ->
                vm.updateTodo(
                    item = todo.copy(courseName = course),
                    title = title,
                    important = todo.important,
                    dueMillis = due
                )
                editing = null
            },
            onDelete = {
                vm.deleteTodo(todo)
                editing = null
            }
        )
    }

    deleting?.let { todo ->
        ConfirmDialog(
            title = StudyStrings.assignmentsDelete(lang),
            text = todo.title,
            confirmText = AppStrings.delete(lang),
            onConfirm = { vm.deleteTodo(todo) },
            onDismiss = { deleting = null }
        )
    }
}

/** 分组标题 + 条数 */
@Composable
private fun SectionHeader(label: String, count: Int, overdue: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = "$label · $count",
            style = MaterialTheme.typography.titleSmall,
            color = if (overdue) expenseColor() else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            color = if (overdue) expenseColor().copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/** 一行作业：勾选框 + 标题 + 课程胶囊与到期日 */
@Composable
private fun AssignmentRow(
    todo: TodoEntity,
    lang: Lang,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val overdue = !todo.done && todo.dueMillis != null &&
        todo.dueMillis.toLocalDate().isBefore(LocalDate.now())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (overdue) expenseColor().copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surface,
                Shapes.card
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = todo.done, onCheckedChange = { onToggle() })
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onEdit() }
                .padding(vertical = 6.dp)
        ) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    todo.done -> MaterialTheme.colorScheme.onSurfaceVariant
                    overdue -> expenseColor()
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None
            )
            if (todo.courseName.isNotBlank() || todo.dueMillis != null) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (todo.courseName.isNotBlank()) {
                        Text(
                            text = todo.courseName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    Shapes.pill
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    todo.dueMillis?.let { due ->
                        Text(
                            text = formatDueLabel(due, lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overdue) expenseColor()
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = StudyStrings.assignmentsDelete(lang),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 分桶：与「待办」看板的规则一致，但把逾期单独拎出来 */
private fun bucketOf(todo: TodoEntity, today: LocalDate): AssignmentBucket {
    if (todo.done) return AssignmentBucket.DONE
    val due = todo.dueMillis?.toLocalDate() ?: return AssignmentBucket.NO_DATE
    return when {
        due.isBefore(today) -> AssignmentBucket.OVERDUE
        due == today -> AssignmentBucket.TODAY
        due.isBefore(today.plusDays(7)) -> AssignmentBucket.THIS_WEEK
        else -> AssignmentBucket.LATER
    }
}

private fun bucketLabel(bucket: AssignmentBucket, lang: Lang): String = when (bucket) {
    AssignmentBucket.OVERDUE -> StudyStrings.assignmentsOverdue(lang)
    AssignmentBucket.TODAY -> StudyStrings.assignmentsToday(lang)
    AssignmentBucket.THIS_WEEK -> StudyStrings.assignmentsThisWeek(lang)
    AssignmentBucket.LATER -> StudyStrings.assignmentsLater(lang)
    AssignmentBucket.NO_DATE -> StudyStrings.assignmentsNoDate(lang)
    AssignmentBucket.DONE -> StudyStrings.assignmentsDone(lang)
}

// ============================================================
// 添加 / 编辑作业
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignmentDialog(
    existing: TodoEntity?,
    courses: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, courseName: String, dueMillis: Long?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val lang = LocalLang.current
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var course by remember { mutableStateOf(existing?.courseName.orEmpty()) }
    var due by remember { mutableStateOf(existing?.dueMillis?.toLocalDate()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var rejected by remember { mutableStateOf(false) }
    // 课程名空着：新建时是「没填完」（不填这条不会出现在作业页），保存前拦下；
    // 编辑时是「要把它从作业页拿走」，保存前用二次确认说清楚（见文件头注释）
    var courseRejected by remember { mutableStateOf(false) }
    var confirmUnlink by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) StudyStrings.assignmentsAddTitle(lang)
                else StudyStrings.assignmentsEditTitle(lang)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FieldLabel(StudyStrings.assignmentsTitleField(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.assignmentsCourseField(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = course,
                    onValueChange = { if (it.length <= 30) course = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (courses.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    ChipFlow {
                        courses.forEach { item ->
                            FilterChip(
                                selected = course == item,
                                onClick = { course = item },
                                label = { Text(item) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    // 常驻说明「填 / 不填分别会发生什么」，新建时没填转成错误色 ——
                    // 和课表页「时间不合法」是同一个做法
                    text = StudyStrings.assignmentsCourseRule(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (courseRejected) expenseColor()
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.assignmentsDueField(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = due?.let { AppStrings.monthDay(lang, it.monthValue, it.dayOfMonth) }
                        ?: StudyStrings.assignmentsNoDue(lang),
                    onValueChange = { },
                    readOnly = true,
                    singleLine = true,
                    placeholder = { Text(StudyStrings.assignmentsPickDue(lang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )
                if (due != null) {
                    TextButton(onClick = { due = null }) {
                        Text(StudyStrings.assignmentsClearDue(lang))
                    }
                }

                if (rejected) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = StudyStrings.assignmentsTitleField(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = expenseColor()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = title.trim()
                    val cleanCourse = course.trim()
                    rejected = clean.isEmpty()
                    if (clean.isEmpty()) {
                        // 没写作业内容，先让用户补上
                    } else if (cleanCourse.isEmpty() && existing == null) {
                        // 新建：课程空着就会变成一条普通待办，在这一页根本看不到 —— 拦住
                        courseRejected = true
                    } else if (cleanCourse.isEmpty()) {
                        // 编辑时清空：这条会离开作业页，先确认（不做静默搬家）
                        confirmUnlink = true
                    } else {
                        onSave(clean, cleanCourse, due?.toDayMillis())
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

    // 清空课程的二次确认：说清楚这条将离开作业页（[StudyStrings.assignmentsCourseRule] 就是
    // 那句说明），并把是哪条作业写在正文里。确认后照常保存（课程名存空）。
    if (confirmUnlink) {
        ConfirmDialog(
            title = StudyStrings.assignmentsCourseRule(lang),
            text = title.trim(),
            confirmText = AppStrings.save(lang),
            onConfirm = { onSave(title.trim(), "", due?.toDayMillis()) },
            onDismiss = { confirmUnlink = false }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (due ?: LocalDate.now()).toDayMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // DatePicker 给的是 UTC 当天 00:00，先按 UTC 取回日期，再本地化到当天 00:00
                        pickerState.selectedDateMillis?.let {
                            due = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text(AppStrings.confirm(lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(AppStrings.cancel(lang)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
