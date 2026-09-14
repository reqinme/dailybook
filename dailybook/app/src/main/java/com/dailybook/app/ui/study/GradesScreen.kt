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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.GradeEntity
import com.dailybook.app.data.ScoreKind
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
import com.dailybook.app.ui.StatBlock
import com.dailybook.app.ui.theme.expenseColor
import java.util.Locale

/**
 * GPA 计算器。
 *
 * 成绩只存原始分数字符串 + 计分方式 + 换算好的绩点（见 [GradeEntity]），
 * 所以三种口径（百分制 / 五级制 / 直接填绩点）可以混着录；GPA 只认绩点，
 * 由父级用「绩点 × 学分」加权算好放进 [UiState.gpa]。
 *
 * 换算规则全部集中在 [scoreToPoint] 里，界面上只负责预览与保存。
 */

// ============================================================
// 分数 → 绩点
// ============================================================

/**
 * 把用户填的分数换算成绩点。[scale] 是用户的绩点口径：4.0（默认）或 5.0。
 *
 * **百分制**（[ScoreKind.PERCENT]）按国内最常见的 4.0 分段（KDoc 里写死，便于对照）：
 *
 * | 分数 | 绩点 |
 * | --- | --- |
 * | 90 及以上 | 4.0 |
 * | 85–89 | 3.7 |
 * | 82–84 | 3.3 |
 * | 78–81 | 3.0 |
 * | 75–77 | 2.7 |
 * | 72–74 | 2.3 |
 * | 68–71 | 2.0 |
 * | 64–67 | 1.5 |
 * | 60–63 | 1.0 |
 * | 60 以下 | 0.0 |
 *
 * `scale == 5.0` 时（也就是用户在设置里选了 5 分制），把上面这套 4 分制结果**等比放大**：
 * `绩点 × scale / 4.0`。所以 90 分 = 5.0、85 分 = 4.625、75 分 = 3.375、60 分 = 1.25。
 * 其余任何 [scale]（含 0 或不合理值）都按 4.0 口径处理，不会算出乱七八糟的数。
 *
 * **五级制**（[ScoreKind.GRADE]）：优秀 4.0 / 良好 3.0 / 中等 2.0 / 及格 1.0 / 不及格 0.0，
 * 同样按 [scale] 等比放大；英文写法 excellent / good / fair / pass / fail 也认。
 * 这一档如果 [score] 填的是数字（很多人直接填 85），会**回落到百分制**去算，免得算出 0。
 *
 * **直接填绩点**（[ScoreKind.POINT]）：填多少就是多少，不做任何换算（[scale] 对它无效）。
 *
 * 认不出来的一律返回 0.0（界面上会提示「换算不出绩点」），永不抛异常。
 */
fun scoreToPoint(score: String, kind: ScoreKind, scale: Double): Double {
    val text = score.trim()
    if (text.isEmpty()) return 0.0
    val ratio = if (scale == 5.0) 5.0 / 4.0 else 1.0

    return when (kind) {
        ScoreKind.POINT -> text.toDoubleOrNull()?.coerceIn(0.0, 5.0) ?: 0.0

        ScoreKind.GRADE -> {
            val byLabel = gradePointOf(text)
            when {
                byLabel != null -> byLabel * ratio
                else -> percentPointOf(text.toDoubleOrNull())?.times(ratio) ?: 0.0
            }
        }

        ScoreKind.PERCENT -> {
            val byPercent = percentPointOf(text.toDoubleOrNull())
            when {
                byPercent != null -> byPercent * ratio
                else -> gradePointOf(text)?.times(ratio) ?: 0.0
            }
        }
    }
}

/** 百分制 → 4.0 制绩点；返回 null 表示这不是一个能认的百分数 */
private fun percentPointOf(value: Double?): Double? {
    val percent = value ?: return null
    if (percent.isNaN() || percent.isInfinite()) return null
    return when {
        percent >= 90.0 -> 4.0
        percent >= 85.0 -> 3.7
        percent >= 82.0 -> 3.3
        percent >= 78.0 -> 3.0
        percent >= 75.0 -> 2.7
        percent >= 72.0 -> 2.3
        percent >= 68.0 -> 2.0
        percent >= 64.0 -> 1.5
        percent >= 60.0 -> 1.0
        else -> 0.0
    }
}

/** 五级制 → 4.0 制绩点；返回 null 表示这不是一个能认的等级 */
private fun gradePointOf(text: String): Double? = when (text.lowercase(Locale.ROOT)) {
    "优秀", "優秀", "優", "优", "excellent", "a", "a+", "a-", "秀" -> 4.0
    "良好", "良", "good", "b", "b+", "b-" -> 3.0
    "中等", "中", "fair", "average", "c", "c+", "c-" -> 2.0
    "及格", "pass", "d", "d+", "d-" -> 1.0
    "不及格", "fail", "f", "e", "不合格" -> 0.0
    else -> null
}

// ============================================================
// 页面
// ============================================================

@Composable
fun GradesScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<GradeEntity?>(null) }
    var deleting by remember { mutableStateOf<GradeEntity?>(null) }

    val totalCredits = remember(state.grades) { state.grades.sumOf { it.credit } }
    // 换算口径：只要有一条成绩的绩点超过 4.0，说明用户用的是 5 分制（父级的 gpa 也是按这个口径算的）
    val scale = remember(state.grades) {
        if (state.grades.any { it.point > 4.0 }) 5.0 else DEFAULT_GPA_SCALE
    }
    // 按学期分组：空学期单独一组放最后（数据里 term 是数据，不翻译）
    val grouped = remember(state.grades) {
        state.grades
            .groupBy { it.term }
            .toList()
            .sortedWith(compareByDescending<Pair<String, List<GradeEntity>>> { it.first.isNotBlank() }
                .thenByDescending { it.first })
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "grades-header") {
                Column {
                    Text(
                        text = StudyStrings.gradesTitle(lang),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(10.dp))
                    SectionCard(title = StudyStrings.gradesSummary(lang)) {
                        Row(Modifier.fillMaxWidth()) {
                            StatBlock(
                                label = StudyStrings.gradesGpa(lang),
                                value = formatGpa(state.gpa),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatBlock(
                                label = StudyStrings.gradesCredits(lang),
                                value = formatCredits(totalCredits),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            StatBlock(
                                label = StudyStrings.gradesCourseCount(lang),
                                value = StudyStrings.gradesCountValue(lang, state.grades.size),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = StudyStrings.gradesScaleNote(
                                lang,
                                String.format(Locale.ROOT, "%.1f", scale)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.grades.isEmpty()) {
                item(key = "grades-empty") {
                    Spacer(Modifier.height(30.dp))
                    EmptyHint(
                        emoji = "🎓",
                        title = StudyStrings.gradesEmpty(lang),
                        subtitle = StudyStrings.gradesEmptyHint(lang)
                    )
                }
            }

            grouped.forEach { (term, items) ->
                item(key = "grades-term-" + term) {
                    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(
                            text = term.ifBlank { StudyStrings.gradesNoTerm(lang) },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
                items(items = items, key = { it.id }) { grade ->
                    GradeRow(
                        grade = grade,
                        lang = lang,
                        onEdit = { editing = grade },
                        onDelete = { deleting = grade }
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
                contentDescription = StudyStrings.gradesAddTitle(lang)
            )
        }
    }

    if (adding) {
        GradeDialog(
            existing = null,
            scale = scale,
            onDismiss = { adding = false },
            onSave = { courseName, term, credit, score, kind, category, point ->
                vm.addGrade(
                    term = term,
                    courseName = courseName,
                    credit = credit,
                    score = score,
                    scoreKind = kind,
                    point = point,
                    category = category
                )
                adding = false
            }
        )
    }

    editing?.let { grade ->
        GradeDialog(
            existing = grade,
            scale = scale,
            onDismiss = { editing = null },
            onSave = { courseName, term, credit, score, kind, category, point ->
                vm.updateGrade(
                    grade.copy(
                        term = term,
                        courseName = courseName,
                        credit = credit,
                        score = score,
                        scoreKind = kind.name,
                        point = point,
                        category = category
                    )
                )
                editing = null
            },
            onDelete = {
                vm.deleteGrade(grade)
                editing = null
            }
        )
    }

    deleting?.let { grade ->
        ConfirmDialog(
            title = StudyStrings.gradesDelete(lang),
            text = grade.courseName,
            confirmText = AppStrings.delete(lang),
            onConfirm = { vm.deleteGrade(grade) },
            onDismiss = { deleting = null }
        )
    }
}

/** 一门成绩：课程名 + 原始分数 + 学分 + 换算绩点 + 类别 */
@Composable
private fun GradeRow(
    grade: GradeEntity,
    lang: Lang,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, Shapes.card)
            .clickable { onEdit() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = grade.courseName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (grade.category.isNotBlank()) {
                    Text(
                        text = grade.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                Shapes.pill
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = StudyStrings.gradesCreditValue(lang, formatCredits(grade.credit)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = grade.score.ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium,
                color = if (grade.point <= 0.0 && grade.score.isNotBlank()) expenseColor()
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = StudyStrings.gradesPoint(
                    lang,
                    String.format(Locale.ROOT, "%.2f", grade.point)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = StudyStrings.gradesDelete(lang),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================
// 添加 / 编辑成绩
// ============================================================

/**
 * 类别是**数据**（存进 [GradeEntity.category]，也用于学分页的分类统计），
 * 所以固定用这四个常见值；界面上按语言显示对应的说法，换语言时旧数据原样显示。
 */
private val GRADE_CATEGORIES = listOf("必修", "选修", "通识", "其他")

/** 默认绩点口径：4.0；有成绩的绩点超过 4.0 时界面自动按 5.0 口径预览 */
private const val DEFAULT_GPA_SCALE = 4.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradeDialog(
    existing: GradeEntity?,
    scale: Double,
    onDismiss: () -> Unit,
    onSave: (
        courseName: String,
        term: String,
        credit: Double,
        score: String,
        kind: ScoreKind,
        category: String,
        point: Double
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val lang = LocalLang.current
    var courseName by remember { mutableStateOf(existing?.courseName.orEmpty()) }
    var term by remember { mutableStateOf(existing?.term.orEmpty()) }
    var creditText by remember {
        mutableStateOf(if (existing == null) "" else formatCredits(existing.credit))
    }
    var score by remember { mutableStateOf(existing?.score.orEmpty()) }
    var kind by remember { mutableStateOf(existing?.kind ?: ScoreKind.PERCENT) }
    var category by remember { mutableStateOf(existing?.category ?: GRADE_CATEGORIES.first()) }
    var rejected by remember { mutableStateOf(false) }

    val credit = creditText.trim().toDoubleOrNull() ?: 0.0
    val point = scoreToPoint(score, kind, scale)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) StudyStrings.gradesAddTitle(lang)
                else StudyStrings.gradesEditTitle(lang)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FieldLabel(StudyStrings.gradesCourse(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { if (it.length <= 40) courseName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.gradesKind(lang))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreKind.entries.forEach { entry ->
                        FilterChip(
                            selected = kind == entry,
                            onClick = { kind = entry },
                            label = { Text(kindLabel(entry, lang)) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.gradesScore(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = score,
                    onValueChange = { if (it.length <= 12) score = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (kind == ScoreKind.GRADE) KeyboardType.Text
                        else KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (score.isBlank()) StudyStrings.gradesPreviewUnknown(lang)
                    else StudyStrings.gradesPreview(lang, String.format(Locale.ROOT, "%.2f", point)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (score.isBlank() || (point <= 0.0 && score.isNotBlank())) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.gradesCredit(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = creditText,
                    onValueChange = { creditText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.gradesCategory(lang))
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    GRADE_CATEGORIES.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(categoryLabel(item, lang)) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.gradesTerm(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = term,
                    onValueChange = { if (it.length <= 20) term = it },
                    singleLine = true,
                    placeholder = { Text(StudyStrings.gradesTermHint(lang)) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (rejected) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = StudyStrings.gradesCourse(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = expenseColor()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = courseName.trim()
                    if (clean.isEmpty()) {
                        rejected = true
                    } else {
                        onSave(clean, term.trim(), credit, score.trim(), kind, category, point)
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

/** 计分方式的中文/英文说法 */
private fun kindLabel(kind: ScoreKind, lang: Lang): String = when (kind) {
    ScoreKind.PERCENT -> StudyStrings.gradesKindPercent(lang)
    ScoreKind.GRADE -> StudyStrings.gradesKindGrade(lang)
    ScoreKind.POINT -> StudyStrings.gradesKindPoint(lang)
}

/** 类别是数据，界面上给四个常用项的现成说法；自定义类别原样显示 */
private fun categoryLabel(category: String, lang: Lang): String = when (category) {
    "必修" -> StudyStrings.gradesCategoryRequired(lang)
    "选修" -> StudyStrings.gradesCategoryElective(lang)
    "通识" -> StudyStrings.gradesCategoryGeneral(lang)
    "其他" -> StudyStrings.gradesCategoryOther(lang)
    else -> category
}
