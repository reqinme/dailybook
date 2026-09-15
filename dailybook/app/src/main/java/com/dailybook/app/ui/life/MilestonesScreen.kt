package com.dailybook.app.ui.life

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.MilestoneEntity
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.LifeStrings
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.ui.ConfirmDialog
import com.dailybook.app.ui.EmptyHint
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.Shapes
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 大事记：一条竖着的时间线。
 *
 * 左边是时间轴（圆点 + 连线），右边是卡片；按年份分组，年份用标题分隔，最新的排最前。
 *
 * 关于配图：`imageUri` 有值时只显示「🖼 已附图片」胶囊，**不加载位图**。
 * 本项目没有引入任何图片加载库（Coil / Glide 都没有，也不允许为这一个界面新增依赖），
 * 而 SAF 给出的 `content://` URI 用 `BitmapFactory` 直接解也拿不到数据、还会踩权限与内存问题；
 * 所以这里只如实告诉用户「这条有图」，等以后有了图片加载库再把胶囊换成缩略图。
 *
 * 选图入口在新增 / 编辑弹窗里（[MilestoneDialog]）：系统文件选择器（`OpenDocument` + `image/＊`），
 * 拿到的是可持久化的读权限 + 一条 URI 字符串，**只存 URI**，不复制文件、不新增任何依赖。
 * 以前只能从备份里带进来一个 imageUri、界面上却没有地方能选，胶囊等于一句无法兑现的承诺。
 */
@Composable
fun MilestonesScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MilestoneEntity?>(null) }
    var deleting by remember { mutableStateOf<MilestoneEntity?>(null) }

    // 年份分组：最新的年份在最上面（DAO 已按 dateMillis DESC 排序，这里按出现顺序收年）
    val years = remember(state.milestones) {
        state.milestones
            .groupBy { it.dateMillis.toLocalDate().year }
            .entries
            .sortedByDescending { it.key }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = LifeStrings.milestonesTitle(lang),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = LifeStrings.milestonesCounts(lang, state.milestones.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            if (state.milestones.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyHint(
                        emoji = "🏆",
                        title = LifeStrings.milestonesEmpty(lang),
                        subtitle = LifeStrings.milestonesEmptyHint(lang),
                        modifier = Modifier.padding(bottom = 40.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    years.forEach { (year, items) ->
                        item(key = "year-" + year) {
                            YearHeader(year = year)
                        }
                        // 首尾标记按**这一年这一组**的位置算（index 0 / lastIndex）：
                        // 只有这一组的第一张卡不画上截线（否则线会悬在年份标题上方）、
                        // 最后一张卡不画下截线（否则线会拖到时间线尽头之外）。
                        itemsIndexed(items = items, key = { _, item -> item.id }) { index, milestone ->
                            TimelineRow(
                                milestone = milestone,
                                firstOfYear = index == 0,
                                lastOfYear = index == items.lastIndex,
                                onEdit = { editing = milestone }
                            )
                        }
                    }
                }
            }
        }

        FilledIconButton(
            onClick = { adding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = LifeStrings.milestoneAdd(lang))
        }
    }

    if (adding) {
        MilestoneDialog(
            milestone = null,
            onDismiss = { adding = false },
            onSave = { title, note, dateMillis, imageUri ->
                vm.addMilestone(title, note, dateMillis, imageUri)
                adding = false
            },
            onDelete = null
        )
    }

    editing?.let { milestone ->
        MilestoneDialog(
            milestone = milestone,
            onDismiss = { editing = null },
            onSave = { title, note, dateMillis, imageUri ->
                // 存的时候按 id 取**当前**这一条：弹窗开着的时候这条可能被改过
                // （比如从别处改了配图），拿打开弹窗时的旧快照整份写回去会把那次改动抹掉。
                // 只有这条已经不在了（被删掉）才退回打开弹窗时的那份。
                val current = state.milestones.firstOrNull { it.id == milestone.id } ?: milestone
                vm.updateMilestone(
                    current.copy(
                        title = title,
                        note = note,
                        dateMillis = dateMillis,
                        imageUri = imageUri
                    )
                )
                editing = null
            },
            onDelete = {
                deleting = milestone
                editing = null
            }
        )
    }

    deleting?.let { milestone ->
        ConfirmDialog(
            title = LifeStrings.milestoneDeleteTitle(lang, milestone.title),
            text = LifeStrings.milestoneDeleteText(lang),
            confirmText = AppStrings.delete(lang),
            onConfirm = { vm.deleteMilestone(milestone) },
            onDismiss = { deleting = null }
        )
    }
}

/** 年份分隔线：左边留出时间轴的宽度，年份和横线对齐 */
@Composable
private fun YearHeader(year: Int) {
    val lang = LocalLang.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(RAIL_WIDTH))
        Text(
            text = AppStrings.year(lang, year),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

/** 一条大事记：左边轴上的圆点 + 连线，右边卡片 */
@Composable
private fun TimelineRow(
    milestone: MilestoneEntity,
    firstOfYear: Boolean,
    lastOfYear: Boolean,
    onEdit: () -> Unit
) {
    TimelineRail(
        firstOfYear = firstOfYear,
        lastOfYear = lastOfYear
    ) {
        MilestoneCard(milestone = milestone, onEdit = onEdit)
    }
}

/**
 * 时间轴的排布：圆点永远对齐卡片上沿往下 18dp（视觉上和标题同一行），
 * 连线从圆点往上下延伸，所以圆点不会随卡片变高而跑偏。
 *
 * 用 [Layout] 而不是 Row + IntrinsicSize：LazyColumn 里每一行的自然高度未知，
 * 这里先量卡片，再按卡片高度决定轴的长度，省掉一次内在测量。
 */
@Composable
private fun TimelineRail(
    firstOfYear: Boolean,
    lastOfYear: Boolean,
    content: @Composable () -> Unit
) {
    val dotColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.surfaceVariant

    Layout(
        content = {
            // 轴的画布：圆点 + 上 / 下两截连线都画在这里。
            // 高度由下面 measure 时给出的 maxHeight 决定（= 卡片高度），所以不用 fillMaxSize。
            Box(Modifier.fillMaxWidth()) {
                if (!firstOfYear) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .width(2.dp)
                            .height(DOT_CENTER_Y - DOT_SIZE / 2)
                            .background(lineColor)
                    )
                }
                if (!lastOfYear) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .width(2.dp)
                            .height(DOT_CENTER_Y - DOT_SIZE / 2)
                            .background(lineColor)
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = DOT_CENTER_Y - DOT_SIZE / 2)
                        .size(DOT_SIZE)
                        .background(dotColor, CircleShape)
                )
            }
            content()
        }
    ) { measurables, constraints ->
        // 先量卡片，再按卡片高度给轴一个固定高度：
        // 这样圆点 / 连线的对齐完全由卡片高度决定，不受 LazyColumn 给的宽松约束影响。
        //
        // 注意：自定义 Layout 的 constraints 与 placeRelative 都用**像素**，
        // 而常量是 dp，必须先 roundToPx() 转换（MeasureScope 本身是 Density，可直接调用）。
        val dotSizePx = RAIL_DOT_SIZE.roundToPx()
        val dotXPx = RAIL_DOT_X.roundToPx()
        val railWidthPx = RAIL_WIDTH.roundToPx()

        val cardPlaceable = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val railPlaceable = measurables[0].measure(
            constraints.copy(
                minWidth = dotSizePx,
                maxWidth = dotSizePx,
                minHeight = cardPlaceable.height,
                maxHeight = cardPlaceable.height
            )
        )
        layout(constraints.maxWidth, cardPlaceable.height) {
            railPlaceable.placeRelative(dotXPx, 0)
            cardPlaceable.placeRelative(railWidthPx, 0)
        }
    }
}

/** 大事记卡片：日期、标题、备注、有没有配图 */
@Composable
private fun MilestoneCard(milestone: MilestoneEntity, onEdit: () -> Unit) {
    val lang = LocalLang.current
    val date = milestone.dateMillis.toLocalDate()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, bottom = 10.dp)
            .background(MaterialTheme.colorScheme.surface, Shapes.card)
            .padding(14.dp)
    ) {
        Text(
            text = AppStrings.monthDay(lang, date.monthValue, date.dayOfMonth),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = milestone.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (milestone.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = milestone.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (milestone.imageUri.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                // 只标注「有图」，不解码位图：项目里没有图片加载库（见文件头注释）
                text = LifeStrings.milestoneHasImage(lang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onEdit) { Text(AppStrings.edit(lang)) }
        }
    }
}

/** 新建 / 编辑大事记：标题、备注、日期（日期用和待办一样的 DatePickerDialog）、配图 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilestoneDialog(
    milestone: MilestoneEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, dateMillis: Long, imageUri: String) -> Unit,
    onDelete: (() -> Unit)?
) {
    val lang = LocalLang.current
    val context = LocalContext.current
    var title by remember { mutableStateOf(milestone?.title.orEmpty()) }
    var note by remember { mutableStateOf(milestone?.note.orEmpty()) }
    var date by remember {
        mutableStateOf(milestone?.dateMillis?.toLocalDate() ?: LocalDate.now())
    }
    var imageUri by remember { mutableStateOf(milestone?.imageUri.orEmpty()) }
    var showPicker by remember { mutableStateOf(false) }

    // 配图：系统文件选择器（OpenDocument + image/＊），只把 URI 存进数据库 ——
    // 和设置页的背景图是同一个做法：不复制文件进 App 目录、不申请存储权限、不引任何图片库。
    // 读权限尽量持久化（备份文件夹在 AutoBackup 里也是这样做的）：不持久化的话，
    // 这条 URI 出了这次会话就可能读不到了；个别提供方不允许持久化，失败也不该让 App 崩。
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            imageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (milestone == null) LifeStrings.milestoneAdd(lang) else LifeStrings.milestoneEdit(lang))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text(LifeStrings.milestoneFieldTitle(lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 500) note = it },
                    label = { Text(LifeStrings.milestoneFieldNote(lang) + " · " + AppStrings.optional(lang)) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(LifeStrings.milestoneFieldDate(lang), style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = AppStrings.monthDay(lang, date.monthValue, date.dayOfMonth),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { showPicker = true }) { Text(AppStrings.select(lang)) }
                    }
                }

                // ---- 配图：选一张（只记路径）/ 清掉 ----
                // 胶囊在卡片上只说明「有图」，这里才是真正能选的地方：
                // 以前 imageUri 只能从备份带进来，界面上一句「已附图片」无从兑现。
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(LifeStrings.milestoneFieldImage(lang), style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (imageUri.isBlank()) AppStrings.notSet(lang)
                            else LifeStrings.milestoneImageChosen(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (imageUri.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.secondary
                        )
                        TextButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                            Text(AppStrings.select(lang))
                        }
                    }
                }
                if (imageUri.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            // 只记路径、本页不解码位图（见文件头注释），但选过之后要能一眼看见、也能撤销
                            text = LifeStrings.milestoneHasImage(lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(6.dp))
                        TextButton(onClick = { imageUri = "" }) { Text(AppStrings.clear(lang)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), note.trim(), date.toDayMillis(), imageUri)
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

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.toDayMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // 日期选择器给的是 UTC 当天零点，按 UTC 解出来才是「选中的那一天」
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
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

/** 时间轴总宽（轴 24dp + 卡片左边距 12dp） */
private val RAIL_WIDTH = 36.dp

/** 轴画布宽度，圆点在正中间 */
private val RAIL_DOT_SIZE = 24.dp

/** 轴的画布离屏幕左边的距离（正好让轴心落在 24dp 处） */
private val RAIL_DOT_X = 11.dp

/** 圆点直径 */
private val DOT_SIZE = 12.dp

/** 圆点中心距卡片上沿的距离：和日期 + 标题这一行对齐 */
private val DOT_CENTER_Y = 18.dp
