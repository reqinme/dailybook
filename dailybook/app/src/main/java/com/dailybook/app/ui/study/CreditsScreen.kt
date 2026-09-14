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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.CreditTargetEntity
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.StudyStrings
import com.dailybook.app.ui.ChipFlow
import com.dailybook.app.ui.ConfirmDialog
import com.dailybook.app.ui.EmptyHint
import com.dailybook.app.ui.FieldLabel
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.SectionCard
import com.dailybook.app.ui.Shapes
import com.dailybook.app.ui.ThinProgressBar
import com.dailybook.app.ui.theme.expenseColor
import com.dailybook.app.ui.theme.incomeColor

/**
 * 学分进度。
 *
 * 数据来源两处，都是父级算好的：
 * - 各个类别的要求：`creditTargets`（用户设的毕业要求）；
 * - 各个类别已修到的学分：`creditsByCategory`（从成绩表按类别累加），总计 `totalCredits`。
 *
 * 所以这一页**只读成绩**：真正录入学分的地方是「GPA 计算器」，这里只管进度与要求。
 * 分类名是数据（必修 / 选修 …），不翻译。
 */

/** 可选类别：与成绩页保持同一份「数据」值 */
private val CREDIT_CATEGORIES = listOf("必修", "选修", "通识", "其他")

@Composable
fun CreditsScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CreditTargetEntity?>(null) }
    var deleting by remember { mutableStateOf<CreditTargetEntity?>(null) }

    val requiredTotal = remember(state.creditTargets) { state.creditTargets.sumOf { it.required } }
    // 设了要求、但成绩里还没有学分的类别也要显示（0 / 要求），所以以 targets 为主
    val rows = remember(state.creditTargets, state.creditsByCategory) {
        state.creditTargets.map { target ->
            target to (state.creditsByCategory[target.category] ?: 0.0)
        }
    }
    // 录了成绩但没建对应要求的类别：单独一组提示，避免「学分对不上总数」
    val untracked = remember(state.creditTargets, state.creditsByCategory) {
        val tracked = state.creditTargets.map { it.category }.toSet()
        state.creditsByCategory.filterKeys { it !in tracked }.filterValues { it > 0.0 }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "credits-header") {
                Column {
                    Text(
                        text = StudyStrings.creditsTitle(lang),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(10.dp))
                    SectionCard(title = StudyStrings.creditsTotal(lang)) {
                        val totalRatio = if (requiredTotal <= 0.0) 0f
                        else (state.totalCredits / requiredTotal).toFloat().coerceIn(0f, 1f)
                        Text(
                            text = StudyStrings.creditsSummary(
                                lang,
                                formatCredits(state.totalCredits),
                                formatCredits(requiredTotal)
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        ThinProgressBar(
                            ratio = totalRatio,
                            color = progressColor(state.totalCredits, requiredTotal),
                            height = 10.dp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (requiredTotal > 0.0 && state.totalCredits >= requiredTotal) {
                                StudyStrings.creditsReached(lang)
                            } else {
                                StudyStrings.creditsRemaining(
                                    lang,
                                    formatCredits((requiredTotal - state.totalCredits).coerceAtLeast(0.0))
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = StudyStrings.creditsHint(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.creditTargets.isEmpty()) {
                item(key = "credits-empty") {
                    Spacer(Modifier.height(20.dp))
                    EmptyHint(
                        emoji = "🎯",
                        title = StudyStrings.creditsEmpty(lang),
                        subtitle = StudyStrings.creditsEmptyHint(lang)
                    )
                }
            }

            if (rows.isNotEmpty()) {
                item(key = "credits-targets-title") {
                    Text(
                        text = StudyStrings.creditsTargets(lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            items(items = rows, key = { it.first.id }) { (target, earned) ->
                CreditTargetRow(
                    target = target,
                    earned = earned,
                    onEdit = { editing = target },
                    onDelete = { deleting = target }
                )
            }

            if (untracked.isNotEmpty()) {
                item(key = "credits-untracked") {
                    SectionCard(title = StudyStrings.creditsUntracked(lang)) {
                        untracked.entries.sortedByDescending { it.value }.forEachIndexed { index, entry ->
                            if (index > 0) Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.key,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = StudyStrings.gradesCreditValue(
                                        lang,
                                        formatCredits(entry.value)
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = StudyStrings.creditsUntrackedHint(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                contentDescription = StudyStrings.creditsAddTarget(lang)
            )
        }
    }

    if (adding) {
        CreditTargetDialog(
            existing = null,
            onDismiss = { adding = false },
            onSave = { category, required ->
                vm.addCreditTarget(category = category, required = required)
                adding = false
            }
        )
    }

    editing?.let { target ->
        CreditTargetDialog(
            existing = target,
            onDismiss = { editing = null },
            onSave = { category, required ->
                vm.updateCreditTarget(target.copy(category = category, required = required))
                editing = null
            },
            onDelete = {
                vm.deleteCreditTarget(target)
                editing = null
            }
        )
    }

    deleting?.let { target ->
        ConfirmDialog(
            title = StudyStrings.creditsDeleteTarget(lang),
            text = target.category,
            confirmText = AppStrings.delete(lang),
            onConfirm = { vm.deleteCreditTarget(target) },
            onDismiss = { deleting = null }
        )
    }
}

/** 一条学分要求：类别 + 进度条 + 已修 / 要求 */
@Composable
private fun CreditTargetRow(
    target: CreditTargetEntity,
    earned: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val lang = LocalLang.current
    val ratio = if (target.required <= 0.0) 0f
    else (earned / target.required).toFloat().coerceIn(0f, 1f)
    val reached = target.required > 0.0 && earned >= target.required

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, Shapes.card)
            .clickable { onEdit() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = target.category,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (reached) {
                Text(
                    text = StudyStrings.creditsReached(lang),
                    style = MaterialTheme.typography.labelMedium,
                    color = incomeColor()
                )
                Spacer(Modifier.width(6.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = StudyStrings.creditsDeleteTarget(lang),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ThinProgressBar(
            ratio = ratio,
            color = progressColor(earned, target.required),
            height = 8.dp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = StudyStrings.creditsRowValue(
                lang,
                formatCredits(earned),
                formatCredits(target.required)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 进度条配色：达标用收入绿，接近（80%）用次要色，其余用主色 */
@Composable
private fun progressColor(earned: Double, required: Double): Color = when {
    required <= 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
    earned >= required -> incomeColor()
    earned >= required * 0.8 -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
}

// ============================================================
// 添加 / 编辑要求
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditTargetDialog(
    existing: CreditTargetEntity?,
    onDismiss: () -> Unit,
    onSave: (category: String, required: Double) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val lang = LocalLang.current
    var category by remember { mutableStateOf(existing?.category.orEmpty()) }
    var requiredText by remember {
        mutableStateOf(if (existing == null) "" else formatCredits(existing.required))
    }
    var rejected by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) StudyStrings.creditsAddTarget(lang)
                else StudyStrings.creditsEditTarget(lang)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FieldLabel(StudyStrings.creditsCategory(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { if (it.length <= 20) category = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    CREDIT_CATEGORIES.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            // 与成绩页共用同一个翻译函数：数据里存的是「必修」这类中文，
                            // 但类别名要跟着界面语言走，不能把中文原样印给英文/日文用户。
                            label = { Text(categoryLabel(item, lang)) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.creditsRequired(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = requiredText,
                    onValueChange = {
                        requiredText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (rejected) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = StudyStrings.creditsCategory(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = expenseColor()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = category.trim()
                    val required = requiredText.trim().toDoubleOrNull() ?: 0.0
                    if (clean.isEmpty() || required <= 0.0) {
                        rejected = true
                    } else {
                        onSave(clean, required)
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
