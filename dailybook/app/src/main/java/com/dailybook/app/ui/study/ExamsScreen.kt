package com.dailybook.app.ui.study

import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.ExamEntity
import com.dailybook.app.data.StudyTaskEntity
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.StudyStrings
import com.dailybook.app.ui.ChipFlow
import com.dailybook.app.ui.ConfirmDialog
import com.dailybook.app.ui.EmptyHint
import com.dailybook.app.ui.FieldLabel
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.SectionCard
import com.dailybook.app.ui.Shapes
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 考试倒计时 + 复习计划。
 *
 * 每场考试一张卡：大号倒计时（N 天 / 今天 / 已结束）、课程、考场与时间；
 * 展开后是这场考试的复习任务（勾选 / 删除 / 内联加一条）。
 *
 * 「自动排复习计划」只是把几条常见任务**平均铺在剩余天数上**的建议，
 * 排出来的是普通任务，用户随时能删 —— 界面上也明说了这一点。
 */

/** 考试日期与时间的显示格式：四语统一数字写法 */
private val EXAM_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
private val EXAM_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

/** 自动排计划最多排 5 条（剩余天数少时更少） */
private const val MAX_PLAN_TASKS = 5

@Composable
fun ExamsScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val context = LocalContext.current
    val today = LocalDate.now()

    var editing by remember { mutableStateOf<ExamEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ExamEntity?>(null) }
    var expandedId by remember { mutableLongStateOf(0L) }
    /** 上一条「自动排复习计划」排给了哪场考试：提示只在那一张卡下面显示 */
    var plannedExamId by remember { mutableLongStateOf(0L) }

    // 按日期排序（DAO 已经排过，这里再排一次，保证与倒计时文案一致）
    val exams = remember(state.exams) { state.exams.sortedBy { it.examMillis } }

    Box(modifier = modifier.fillMaxSize()) {
        if (exams.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = StudyStrings.examsTitle(lang),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(40.dp))
                EmptyHint(
                    emoji = "📝",
                    title = StudyStrings.examsEmpty(lang),
                    subtitle = StudyStrings.examsEmptyHint(lang)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 14.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "exams-header") {
                    Column {
                        Text(
                            text = StudyStrings.examsTitle(lang),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = StudyStrings.examsAutoPlanHint(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }

                items(items = exams, key = { it.id }) { exam ->
                    ExamCard(
                        exam = exam,
                        tasks = state.studyTasks.filter { it.examId == exam.id },
                        today = today,
                        lang = lang,
                        expanded = expandedId == exam.id,
                        planned = plannedExamId == exam.id,
                        onToggleExpand = {
                            expandedId = if (expandedId == exam.id) 0L else exam.id
                        },
                        onEdit = { editing = exam },
                        onDelete = { deleting = exam },
                        onToggleTask = { vm.toggleStudyTask(it) },
                        onDeleteTask = { vm.deleteStudyTask(it) },
                        onAddTask = { vm.addStudyTask(exam.id, it, today.toDayMillis()) },
                        onAutoPlan = {
                            val created = autoPlanTasks(
                                daysLeft = ChronoUnit.DAYS.between(today, exam.examMillis.toLocalDate()),
                                lang = lang
                            )
                            if (created.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    StudyStrings.examsPlanTooLate(lang),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                created.forEach { (title, date) ->
                                    vm.addStudyTask(exam.id, title, date.toDayMillis())
                                }
                                plannedExamId = exam.id
                                expandedId = exam.id
                                Toast.makeText(
                                    context,
                                    StudyStrings.examsPlanDone(lang, created.size),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
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
                contentDescription = StudyStrings.examsAddTitle(lang)
            )
        }
    }

    if (adding) {
        ExamDialog(
            existing = null,
            courses = state.courses.map { it.name },
            onDismiss = { adding = false },
            onSave = { name, course, millis, location, note ->
                vm.addExam(
                    name = name,
                    courseName = course,
                    examMillis = millis,
                    location = location,
                    note = note
                )
                adding = false
            }
        )
    }

    editing?.let { exam ->
        ExamDialog(
            existing = exam,
            courses = state.courses.map { it.name },
            onDismiss = { editing = null },
            onSave = { name, course, millis, location, note ->
                vm.updateExam(
                    exam.copy(
                        name = name,
                        courseName = course,
                        examMillis = millis,
                        location = location,
                        note = note
                    )
                )
                editing = null
            },
            onDelete = {
                vm.deleteExam(exam)
                editing = null
            }
        )
    }

    deleting?.let { exam ->
        ConfirmDialog(
            title = StudyStrings.examsDelete(lang),
            text = StudyStrings.examsDeleteConfirm(lang, exam.name),
            confirmText = AppStrings.delete(lang),
            onConfirm = { vm.deleteExam(exam) },
            onDismiss = { deleting = null }
        )
    }
}

// ============================================================
// 自动排复习计划
// ============================================================

/**
 * 把几条常见的复习任务铺在剩余天数上，返回 (任务标题, 日期) 列表。
 *
 * 规则：
 * - 考试已结束（[daysLeft] < 0）→ 返回空列表（调用方提示「已经结束」）；
 * - 只剩 0~2 天 → 只排 1 条（就是今天），这时候排 4 条没意义；
 * - 否则按天数决定条数（3~5 条），再把这些条**铺满「今天 ~ 考试前一天」这段窗口**：
 *   第 i 条的日期取窗口里的第 (2i-1)/(2×条数) 个等分点，
 *   条数先按可用天数封顶，所以
 *   ① 一天最多一条（相邻偏移量的步长 = 窗口/条数 ≥ 1，严格递增），
 *   ② 最后一条落在考试前一天或更早，**不会排到考试当天**，
 *   ③ 窗口短（还剩 3 天）时排今天 / +1 / +2，而不是被挤到考试当天。
 *
 * 以前用的是固定间隔 `天数 / (条数 + 1)`、再 `coerceAtLeast(1)`：剩余天数一少，
 * 间隔就被顶成 1 天，几条任务连排到今天+1、+2、+3 —— 最后一条正好是考试当天。
 *
 * 这些只是**建议**：排出来的就是普通复习任务，用户勾掉或删掉都行。
 * 抽成纯函数是为了不依赖 Compose，也方便日后单独调间距规则。
 */
internal fun autoPlanTasks(daysLeft: Long, lang: Lang): List<Pair<String, LocalDate>> {
    if (daysLeft < 0L) return emptyList()
    val today = LocalDate.now()
    if (daysLeft <= 2L) {
        return listOf(StudyStrings.examPlanTaskFinal(lang) to today)
    }

    // 可用窗口 = 今天（偏移 0）到考试前一天（偏移 daysLeft - 1），一共 daysLeft 天
    val window = daysLeft.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val wanted = when {
        daysLeft <= 5L -> 3
        daysLeft <= 10L -> 4
        else -> MAX_PLAN_TASKS
    }
    // 条数不超过可用天数：窗口越短排得越少，绝不一天堆两条
    val count = wanted.coerceAtMost(window)
    val templates = listOf(
        StudyStrings.examPlanTaskRead(lang),
        StudyStrings.examPlanTaskPastPapers(lang),
        StudyStrings.examPlanTaskKeyPoints(lang),
        StudyStrings.examPlanTaskMock(lang),
        StudyStrings.examPlanTaskReview(lang)
    )
    return (1..count).map { index ->
        val title = templates[(index - 1) % templates.size]
        // 等分中点：index 递增 ⇒ 偏移严格递增（步长 = 窗口 / 条数 ≥ 1），
        // 最后一个偏移 = 窗口 - ceil(窗口 / (2×条数)) ≤ 窗口 - 1，所以到不了考试当天
        val offset = ((2 * index - 1) * window) / (2 * count)
        title to today.plusDays(offset.toLong())
    }
}

// ============================================================
// 考试卡
// ============================================================

@Composable
private fun ExamCard(
    exam: ExamEntity,
    tasks: List<StudyTaskEntity>,
    today: LocalDate,
    lang: Lang,
    expanded: Boolean,
    planned: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleTask: (StudyTaskEntity) -> Unit,
    onDeleteTask: (StudyTaskEntity) -> Unit,
    onAddTask: (String) -> Unit,
    onAutoPlan: () -> Unit
) {
    val days = ChronoUnit.DAYS.between(today, exam.examMillis.toLocalDate())
    val doneCount = tasks.count { it.done }

    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 倒计时：N 天 / 今天 / 已结束
            Column(
                modifier = Modifier.width(84.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        days < 0L -> StudyStrings.examsFinished(lang)
                        days == 0L -> StudyStrings.examsToday(lang)
                        else -> StudyStrings.examsDaysLeft(lang, days)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        days < 0L -> MaterialTheme.colorScheme.onSurfaceVariant
                        days <= 7L -> expenseColor()
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        days < 0L -> StudyStrings.examsPassedNote(lang, -days)
                        days == 0L -> StudyStrings.examsToday(lang)
                        else -> StudyStrings.examsCountdownNote(lang, days)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = exam.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (exam.courseName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = exam.courseName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = StudyStrings.examsWhen(
                        lang,
                        exam.examMillis.toLocalDate().format(EXAM_DATE_FORMAT),
                        Instant.ofEpochMilli(exam.examMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()
                            .format(EXAM_TIME_FORMAT)
                    ) + if (exam.location.isBlank()) "" else " · ${exam.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tasks.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = StudyStrings.examsTaskCount(lang, doneCount, tasks.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (doneCount == tasks.size) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = StudyStrings.examsDelete(lang),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleExpand) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = StudyStrings.examsTasksSection(lang)
                )
            }
        }

        if (expanded) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(10.dp))
            FieldLabel(StudyStrings.examsTasksSection(lang))
            Spacer(Modifier.height(6.dp))

            if (tasks.isEmpty()) {
                Text(
                    text = StudyStrings.examsNoTask(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                tasks.forEach { task ->
                    StudyTaskRow(
                        task = task,
                        lang = lang,
                        onToggle = { onToggleTask(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            InlineTaskField(lang = lang, onAdd = onAddTask)

            if (days >= 0L) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onAutoPlan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(StudyStrings.examsAutoPlan(lang))
                }
                if (planned) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = StudyStrings.examsAutoPlanHint(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (exam.note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = exam.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 一条复习任务：勾选 / 划线 / 删除 */
@Composable
private fun StudyTaskRow(
    task: StudyTaskEntity,
    lang: Lang,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.done, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
            )
            Text(
                text = AppStrings.monthDay(
                    lang,
                    task.dateMillis.toLocalDate().monthValue,
                    task.dateMillis.toLocalDate().dayOfMonth
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = StudyStrings.examsDeleteTask(lang),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 内联「加一条复习任务」：输入框 + 加号，回车也能提交 */
@Composable
private fun InlineTaskField(lang: Lang, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    fun submit() {
        val clean = text.trim()
        if (clean.isEmpty()) return
        onAdd(clean)
        text = ""
        focusManager.clearFocus()
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 40) text = it },
            placeholder = { Text(StudyStrings.examsAddTaskPlaceholder(lang)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() })
        )
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = { submit() },
            enabled = text.isNotBlank(),
            modifier = Modifier.heightIn(min = 52.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = StudyStrings.examsAddTaskPlaceholder(lang)
            )
        }
    }
}

// ============================================================
// 添加 / 编辑考试
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamDialog(
    existing: ExamEntity?,
    courses: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        courseName: String,
        examMillis: Long,
        location: String,
        note: String
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val lang = LocalLang.current
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var course by remember { mutableStateOf(existing?.courseName.orEmpty()) }
    var location by remember { mutableStateOf(existing?.location.orEmpty()) }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }
    // 默认：今天上午 9 点（多数考试就在这个时段）
    var examDate by remember {
        mutableStateOf(
            existing?.examMillis?.toLocalDate() ?: LocalDate.now()
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var rejected by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) StudyStrings.examsAddTitle(lang)
                else StudyStrings.examsEditTitle(lang)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FieldLabel(StudyStrings.examsName(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.examsCourse(lang))
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

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.examsDate(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = examDate.format(EXAM_DATE_FORMAT),
                    onValueChange = { },
                    readOnly = true,
                    singleLine = true,
                    placeholder = { Text(StudyStrings.examsDateHint(lang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.examsLocation(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { if (it.length <= 24) location = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.examsNote(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 80) note = it },
                    modifier = Modifier.fillMaxWidth()
                )

                if (rejected) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = StudyStrings.examsName(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = expenseColor()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = name.trim()
                    if (clean.isEmpty()) {
                        rejected = true
                    } else {
                        // 保留原有时间（时 / 分），只换日期；新建时默认上午 9:00
                        val base = existing?.examMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
                        }
                        val hour = base?.hour ?: 9
                        val minute = base?.minute ?: 0
                        val millis = examDate
                            .atStartOfDay(ZoneId.systemDefault())
                            .withHour(hour)
                            .withMinute(minute)
                            .toInstant()
                            .toEpochMilli()
                        onSave(clean, course.trim(), millis, location.trim(), note.trim())
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

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = examDate.toDayMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            examDate = Instant.ofEpochMilli(it)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
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
