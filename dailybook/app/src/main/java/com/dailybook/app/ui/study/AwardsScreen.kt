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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.AwardEntity
import com.dailybook.app.data.AwardKind
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
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 奖助 / 竞赛 / 证书。
 *
 * 按 [AwardKind] 分组展示（奖助学金 / 竞赛 / 证书 / 其他），每条显示名称、级别与日期。
 *
 * **没有图片加载库**：项目不引任何图片依赖，所以 `imageUri` 非空时这里只显示一个
 * 「已附图片」的胶囊（见 [StudyStrings.awardsHasImage]），不会去解码位图 ——
 * 按需加载 + 内存缓存是图片库该干的事，为一张缩略图引一个库不划算。
 *
 * 级别（国家级 / 省级 / 校级 / 院级）是**数据**，存原文；界面上用固定四个芯片填，
 * 但已有记录里的自定义级别照原样显示。
 */

/** 级别的四个常用值（数据，不翻译；芯片文案按语言显示） */
private val AWARD_LEVELS = listOf("国家级", "省级", "校级", "院级")

private val AWARD_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)

@Composable
fun AwardsScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AwardEntity?>(null) }
    var deleting by remember { mutableStateOf<AwardEntity?>(null) }

    // 按类型分组：顺序固定为 奖助学金 / 竞赛 / 证书 / 其他，空组不显示
    val grouped = remember(state.awards) {
        AwardKind.entries
            .map { kind -> kind to state.awards.filter { it.awardKind == kind } }
            .filter { it.second.isNotEmpty() }
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "awards-header") {
                Column {
                    Text(
                        text = StudyStrings.awardsTitle(lang),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = StudyStrings.awardsCount(lang, state.awards.size) +
                            " · " + StudyStrings.awardsImageHint(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            if (state.awards.isEmpty()) {
                item(key = "awards-empty") {
                    Spacer(Modifier.height(30.dp))
                    EmptyHint(
                        emoji = "🏆",
                        title = StudyStrings.awardsEmpty(lang),
                        subtitle = StudyStrings.awardsEmptyHint(lang)
                    )
                }
            }

            grouped.forEach { (kind, items) ->
                item(key = "awards-kind-" + kind.name) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = kindLabel(kind, lang) + " · " + items.size,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
                items(items = items, key = { it.id }) { award ->
                    AwardRow(
                        award = award,
                        lang = lang,
                        onEdit = { editing = award },
                        onDelete = { deleting = award }
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
                contentDescription = StudyStrings.awardsAddTitle(lang)
            )
        }
    }

    if (adding) {
        AwardDialog(
            existing = null,
            onDismiss = { adding = false },
            onSave = { title, kind, level, date, note ->
                vm.addAward(
                    title = title,
                    kind = kind,
                    dateMillis = date,
                    level = level,
                    note = note
                )
                adding = false
            }
        )
    }

    editing?.let { award ->
        AwardDialog(
            existing = award,
            onDismiss = { editing = null },
            onSave = { title, kind, level, date, note ->
                vm.updateAward(
                    award.copy(
                        title = title,
                        kind = kind.name,
                        dateMillis = date,
                        level = level,
                        note = note
                    )
                )
                editing = null
            },
            onDelete = {
                vm.deleteAward(award)
                editing = null
            }
        )
    }

    deleting?.let { award ->
        ConfirmDialog(
            title = StudyStrings.awardsDelete(lang),
            text = award.title,
            confirmText = AppStrings.delete(lang),
            onConfirm = { vm.deleteAward(award) },
            onDismiss = { deleting = null }
        )
    }
}

/** 一条记录：名称 + 级别 + 日期（+ 有图片时的提示胶囊） */
@Composable
private fun AwardRow(
    award: AwardEntity,
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
                text = award.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (award.level.isNotBlank()) {
                    Text(
                        text = award.level,
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
                    text = award.dateMillis.toLocalDate().format(AWARD_DATE_FORMAT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 有图片附件：不加载位图，只给一个提示（本项目没有图片库）
                if (award.imageUri.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = StudyStrings.awardsHasImage(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                                Shapes.pill
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            if (award.note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = award.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = StudyStrings.awardsDelete(lang),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun kindLabel(kind: AwardKind, lang: Lang): String = when (kind) {
    AwardKind.SCHOLARSHIP -> StudyStrings.awardsKindScholarship(lang)
    AwardKind.CONTEST -> StudyStrings.awardsKindContest(lang)
    AwardKind.CERTIFICATE -> StudyStrings.awardsKindCertificate(lang)
    AwardKind.OTHER -> StudyStrings.awardsKindOther(lang)
}

/** 级别是数据：四个常用值按语言显示成一句话，自定义级别原样返回 */
private fun levelLabel(level: String, lang: Lang): String = when (level) {
    "国家级" -> StudyStrings.awardsLevelNational(lang)
    "省级" -> StudyStrings.awardsLevelProvincial(lang)
    "校级" -> StudyStrings.awardsLevelSchool(lang)
    "院级" -> StudyStrings.awardsLevelCollege(lang)
    else -> level
}

// ============================================================
// 添加 / 编辑记录
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AwardDialog(
    existing: AwardEntity?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        kind: AwardKind,
        level: String,
        dateMillis: Long,
        note: String
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val lang = LocalLang.current
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var kind by remember { mutableStateOf(existing?.awardKind ?: AwardKind.SCHOLARSHIP) }
    var level by remember { mutableStateOf(existing?.level.orEmpty()) }
    var date by remember { mutableStateOf(existing?.dateMillis?.toLocalDate() ?: LocalDate.now()) }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var rejected by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) StudyStrings.awardsAddTitle(lang)
                else StudyStrings.awardsEditTitle(lang)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FieldLabel(StudyStrings.awardsName(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 50) title = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.awardsKind(lang))
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    AwardKind.entries.forEach { entry ->
                        FilterChip(
                            selected = kind == entry,
                            onClick = { kind = entry },
                            label = { Text(kindLabel(entry, lang)) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.awardsLevel(lang))
                Spacer(Modifier.height(6.dp))
                ChipFlow {
                    AWARD_LEVELS.forEach { item ->
                        FilterChip(
                            selected = level == item,
                            onClick = { level = if (level == item) "" else item },
                            label = { Text(levelLabel(item, lang)) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = StudyStrings.awardsLevelNone(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { level = "" }
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.awardsDate(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = date.format(AWARD_DATE_FORMAT),
                    onValueChange = { },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )

                Spacer(Modifier.height(10.dp))
                FieldLabel(StudyStrings.awardsNote(lang))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 80) note = it },
                    modifier = Modifier.fillMaxWidth()
                )

                if (rejected) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = StudyStrings.awardsName(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = title.trim()
                    if (clean.isEmpty()) {
                        rejected = true
                    } else {
                        onSave(clean, kind, level.trim(), date.toDayMillis(), note.trim())
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
            initialSelectedDateMillis = date.toDayMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // DatePicker 给的是 UTC 当天 00:00，先按 UTC 取回日期，再本地化到当天 00:00
                        pickerState.selectedDateMillis?.let {
                            date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
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
