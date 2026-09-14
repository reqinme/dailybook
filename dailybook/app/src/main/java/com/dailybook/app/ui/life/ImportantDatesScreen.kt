package com.dailybook.app.ui.life

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.UpcomingDate
import com.dailybook.app.data.DateRepeat
import com.dailybook.app.data.ImportantDateEntity
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LifeStrings
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.ui.ChipFlow
import com.dailybook.app.ui.ConfirmDialog
import com.dailybook.app.ui.EmptyHint
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.Shapes
import com.dailybook.app.util.Lunar
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 重要日期 / 倒计时。
 *
 * 顶部是「最近的那个日子」的大字倒计时（[UiState.upcomingDates] 的第一个），
 * 下面按最近程度列出其余日子；每行可以单独导出 .ics 到系统日历里。
 *
 * 「只过一次」又已经过完的日子单列一组（[UiState.pastDates]），明确写成「已过去 N 天」：
 * 它们以前会凭空消失（算不出「下一次」就从列表里掉出去，还让整页显示成「还没有重要日期」），
 * 而顶部那张大字卡片仍然是「下一次还没发生的日子」，不会被已经过去的日期顶掉。
 *
 * 农历日期的展示：月名 / 日名（正月初一、闰六月十五）直接取 [Lunar] 的数据，
 * 属于农历本身的数据，不进 [LifeStrings]，四语都显示同样的字样。
 */
@Composable
fun ImportantDatesScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val context = LocalContext.current

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ImportantDateEntity?>(null) }
    var deleting by remember { mutableStateOf<ImportantDateEntity?>(null) }
    // 点「导出 .ics」时先记住要导出的那一条，用户挑完位置再真正写文件
    var pendingExport by remember { mutableStateOf<Pair<ImportantDateEntity, Long>?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        val payload = pendingExport
        pendingExport = null
        if (uri == null || payload == null) return@rememberLauncherForActivityResult
        // 写文件的结果用本页自己的 Toast 汇报（不走 VM 的 message，免得和记账的导入导出提示串台）
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(buildIcs(payload.first, payload.second, lang).toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("openOutputStream returned null")
        }.isSuccess
        Toast.makeText(
            context,
            if (ok) LifeStrings.dateExportDone(lang, uri.lastPathSegment.orEmpty())
            else LifeStrings.dateExportFailed(lang),
            Toast.LENGTH_LONG
        ).show()
    }

    // 行上的「导出 .ics」只负责选文件：选中后回调里才生成文本并写进去
    fun startExport(item: ImportantDateEntity, nextMillis: Long) {
        pendingExport = item to nextMillis
        exportLauncher.launch(icsFileName(item.title, nextMillis))
    }

    // 顶部大卡片只认「下一次还没发生的日子」；已经过完的「只过一次」在下面单列一组
    val hero = state.upcomingDates.firstOrNull()
    val rest = state.upcomingDates.drop(1)
    val past = state.pastDates

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = LifeStrings.datesTitle(lang),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))

            if (state.upcomingDates.isEmpty() && past.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyHint(
                        emoji = "🎂",
                        title = LifeStrings.datesEmpty(lang),
                        subtitle = LifeStrings.datesEmptyHint(lang),
                        modifier = Modifier.padding(bottom = 40.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    if (hero != null) {
                        item(key = "hero") {
                            HeroCard(
                                upcoming = hero,
                                onEdit = { editing = hero.item },
                                onExport = { startExport(hero.item, hero.nextMillis) }
                            )
                        }
                    }
                    if (rest.isNotEmpty()) {
                        item(key = "others") {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = LifeStrings.datesOtherTitle(lang),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(items = rest, key = { the -> "up-" + the.item.id }) { upcoming ->
                            DateRow(
                                upcoming = upcoming,
                                onEdit = { editing = upcoming.item },
                                onExport = { startExport(upcoming.item, upcoming.nextMillis) }
                            )
                        }
                    }
                    if (past.isNotEmpty()) {
                        item(key = "past-title") {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = LifeStrings.datesPastTitle(lang),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(items = past, key = { the -> "past-" + the.item.id }) { passed ->
                            DateRow(
                                upcoming = passed,
                                onEdit = { editing = passed.item },
                                onExport = { startExport(passed.item, passed.nextMillis) }
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
            Icon(Icons.Filled.Add, contentDescription = LifeStrings.dateAdd(lang))
        }
    }

    if (adding) {
        DateDialog(
            item = null,
            onDismiss = { adding = false },
            onSave = { title, dateMillis, lunar, lunarMonth, lunarDay, lunarLeap, repeat, remind, note ->
                vm.addImportantDate(
                    title,
                    dateMillis,
                    lunar,
                    lunarMonth,
                    lunarDay,
                    lunarLeap,
                    repeat,
                    remind,
                    note
                )
                adding = false
            },
            onDelete = null
        )
    }

    editing?.let { item ->
        DateDialog(
            item = item,
            onDismiss = { editing = null },
            onSave = { title, dateMillis, lunar, lunarMonth, lunarDay, lunarLeap, repeat, remind, note ->
                vm.updateImportantDate(
                    item.copy(
                        title = title,
                        dateMillis = dateMillis,
                        lunar = lunar,
                        lunarMonth = lunarMonth,
                        lunarDay = lunarDay,
                        lunarLeap = lunarLeap,
                        repeat = repeat.name,
                        remindDaysBefore = remind,
                        note = note
                    )
                )
                editing = null
            },
            onDelete = {
                deleting = item
                editing = null
            }
        )
    }

    deleting?.let { item ->
        ConfirmDialog(
            title = LifeStrings.dateDeleteTitle(lang, item.title),
            text = LifeStrings.dateDeleteText(lang),
            confirmText = AppStrings.delete(lang),
            onDismiss = { deleting = null },
            onConfirm = { vm.deleteImportantDate(item) }
        )
    }
}

/** 顶部大卡片：N 天 + 名称 + 日期（农历日期额外给一行农历写法） */
@Composable
private fun HeroCard(
    upcoming: UpcomingDate,
    onEdit: () -> Unit,
    onExport: () -> Unit
) {
    val lang = LocalLang.current
    val item = upcoming.item

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), Shapes.card)
            .padding(18.dp)
    ) {
        Text(
            text = LifeStrings.datesHeroLabel(lang),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = countdownText(lang, upcoming.daysLeft),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = solarDateText(lang, upcoming.nextMillis),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (item.lunar) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = lunarText(lang, item),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        if (item.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RepeatChip(repeat = item.repeatRule, lang = lang)
            Spacer(Modifier.width(8.dp))
            Text(
                text = remindText(lang, item.remindDaysBefore),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onEdit) { Text(AppStrings.edit(lang)) }
            IconButton(onClick = onExport) {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = LifeStrings.dateExportIcs(lang),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** 列表行：名称、下一次的阳历日期、N 天后、农历写法、重复与提醒 */
@Composable
private fun DateRow(
    upcoming: UpcomingDate,
    onEdit: () -> Unit,
    onExport: () -> Unit
) {
    val lang = LocalLang.current
    val item = upcoming.item

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, Shapes.card)
            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = solarDateText(lang, upcoming.nextMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = countdownText(lang, upcoming.daysLeft),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (item.lunar) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = lunarText(lang, item),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RepeatChip(repeat = item.repeatRule, lang = lang)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = remindText(lang, item.remindDaysBefore),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = AppStrings.edit(lang),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onExport) {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = LifeStrings.dateExportIcs(lang),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RepeatChip(repeat: DateRepeat, lang: Lang) {
    // 重复规则是「选项」，套一层淡淡的底色当胶囊，不改 FilterChip 的选中态语义
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), Shapes.pill)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = repeatLabel(repeat, lang),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// 展示辅助（供本文件与同一包内的其它生活页复用）
// ============================================================

/**
 * 倒计时文案：就是今天 / 还有 N 天 / 已过去 N 天。
 *
 * 最后一种只对「只过一次」又已经过完的日子出现（[UiState.pastDates]）：
 * 那时候 [daysLeft] 是负数，以前和 0 一起被写成「就是今天」，
 * 于是唯一还能看到它的那一天显示的是一句不实的话。
 */
internal fun countdownText(lang: Lang, daysLeft: Long): String = when {
    daysLeft > 0L -> LifeStrings.dateDaysLeft(lang, daysLeft)
    daysLeft == 0L -> LifeStrings.dateToday(lang)
    else -> LifeStrings.dateDaysPassed(lang, -daysLeft)
}

/** 阳历日期：9月20日 周六 */
internal fun solarDateText(lang: Lang, millis: Long): String {
    val date = millis.toLocalDate()
    return AppStrings.monthDay(lang, date.monthValue, date.dayOfMonth) + " " +
        AppStrings.weekday(lang, date.dayOfWeek.value - 1)
}

/** 农历写法：农历闰六月十五（月名 / 日名是 [Lunar] 的数据） */
internal fun lunarText(lang: Lang, item: ImportantDateEntity): String {
    var leap = item.lunarLeap
    // 存的是闰月但换算到今年并不存在闰月时，VM 会按普通月算，这里也跟着去掉「闰」字
    if (leap) {
        val current = Lunar.fromSolar(LocalDate.now())
        if (Lunar.hasLeapMonth(current.year) != item.lunarMonth &&
            Lunar.hasLeapMonth(current.year + 1) != item.lunarMonth
        ) {
            leap = false
        }
    }
    return LifeStrings.dateLunar(
        lang,
        Lunar.lunarMonthName(item.lunarMonth, leap),
        Lunar.lunarDayName(item.lunarDay)
    )
}

/** 提醒文案：当天提醒 / 提前 N 天 */
internal fun remindText(lang: Lang, daysBefore: Int): String =
    if (daysBefore <= 0) LifeStrings.dateRemindOnDay(lang)
    else LifeStrings.dateRemindBefore(lang, daysBefore)

internal fun repeatLabel(repeat: DateRepeat, lang: Lang): String = when (repeat) {
    DateRepeat.ONCE -> LifeStrings.dateRepatOnce(lang)
    DateRepeat.YEARLY -> LifeStrings.dateRepeatYearly(lang)
    DateRepeat.MONTHLY -> LifeStrings.dateRepeatMonthly(lang)
    DateRepeat.WEEKLY -> LifeStrings.dateRepeatWeekly(lang)
}

// ============================================================
// 导出 .ics
// ============================================================

/** 导出文件名：important-date-<标题>-20260920.ics，去掉系统不认的字符 */
private fun icsFileName(title: String, nextMillis: Long): String {
    val safe = title.map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .trim('-')
        .take(24)
        .ifEmpty { "date" }
    return "important-$safe-${ICS_DATE.format(nextMillis.toLocalDate())}.ics"
}

/**
 * 生成一条 iCalendar（RFC 5545）事件文本。
 *
 * 公开的顶层函数：父级以后想接「导出全部 / 分享」也能直接复用，不必再抄一份。
 * 规则要点：
 * - 行尾统一 CRLF；文本字段里的 `\` `;` `,` 和换行都要转义；
 * - 全天事件用 `DTSTART;VALUE=DATE:yyyyMMdd`（不带时区）；
 * - 重复：每年 / 每月 / 每周对应 RRULE 的 YEARLY / MONTHLY / WEEKLY；只过一次不写 RRULE；
 * - 提前提醒不为 0 时补一个 VALARM（TRIGGER 用负的天数）。
 */
fun buildIcs(item: ImportantDateEntity, nextMillis: Long, lang: Lang): String {
    val day = nextMillis.toLocalDate()
    val lunar = if (item.lunar) lunarText(lang, item) else ""
    val description = if (lunar.isEmpty()) {
        LifeStrings.dateIcsDescription(lang, item.note.ifBlank { "-" })
    } else {
        LifeStrings.dateIcsDescriptionLunar(lang, lunar, item.note.ifBlank { "-" })
    }
    val rule = when (item.repeatRule) {
        DateRepeat.YEARLY -> "RRULE:FREQ=YEARLY"
        DateRepeat.MONTHLY -> "RRULE:FREQ=MONTHLY"
        DateRepeat.WEEKLY -> "RRULE:FREQ=WEEKLY"
        DateRepeat.ONCE -> null
    }

    val lines = mutableListOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//DailyBook//Important Dates//" + lang.tag,
        "CALSCALE:GREGORIAN",
        "BEGIN:VEVENT",
        "UID:dailybook-date-" + item.id + "-" + ICS_DATE.format(day) + "@dailybook.app",
        "DTSTAMP:" + ICS_STAMP.format(Instant.now()),
        "DTSTART;VALUE=DATE:" + ICS_DATE.format(day),
        "SUMMARY:" + icsEscape(LifeStrings.dateIcsSummary(lang, item.title)),
        "DESCRIPTION:" + icsEscape(description)
    )
    rule?.let { lines += it }
    if (item.remindDaysBefore > 0) {
        lines += "BEGIN:VALARM"
        lines += "ACTION:DISPLAY"
        lines += "DESCRIPTION:" + icsEscape(LifeStrings.dateIcsAlarm(lang, item.title))
        lines += "TRIGGER:-P" + item.remindDaysBefore + "D"
        lines += "END:VALARM"
    }
    lines += "END:VEVENT"
    lines += "END:VCALENDAR"

    // CRLF 收尾（RFC 5545 要求，也是各家日历 App 最稳的写法）
    return lines.joinToString("\r\n") + "\r\n"
}

/** RFC 5545 文本转义：反斜杠、分号、逗号要转义，换行写成字面量 \n */
private fun icsEscape(text: String): String = text
    .replace("\\", "\\\\")
    .replace(";", "\\;")
    .replace(",", "\\,")
    .replace("\r\n", "\\n")
    .replace("\n", "\\n")
    .replace("\r", "\\n")

/** 日期（UTC，全天事件用的就是这一天），固定格式不随语言变 */
private val ICS_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT)

/** DTSTAMP：UTC 时间戳，形如 20260914T120000Z */
private val ICS_STAMP: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC)

// ============================================================
// 新增 / 编辑弹窗
// ============================================================

/** 提前提醒的可选天数（0 = 当天） */
private val REMIND_OPTIONS = listOf(0, 1, 3, 7)

/** 农历日的可选值：1 / 5 / 10 / 15 / 20 / 25 / 30（常用日子里挑，避免 30 个芯片铺满屏幕） */
private val LUNAR_DAY_PRESETS = listOf(1, 5, 10, 15, 20, 25, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(
    item: ImportantDateEntity?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        dateMillis: Long,
        lunar: Boolean,
        lunarMonth: Int,
        lunarDay: Int,
        lunarLeap: Boolean,
        repeat: DateRepeat,
        remindDaysBefore: Int,
        note: String
    ) -> Unit,
    onDelete: (() -> Unit)?
) {
    val lang = LocalLang.current

    var title by remember { mutableStateOf(item?.title.orEmpty()) }
    var lunar by remember { mutableStateOf(item?.lunar ?: false) }
    var date by remember {
        mutableStateOf(item?.dateMillis?.toLocalDate() ?: LocalDate.now())
    }
    var lunarMonth by remember { mutableStateOf(item?.lunarMonth ?: 1) }
    var lunarDay by remember { mutableStateOf(item?.lunarDay ?: 1) }
    var lunarLeap by remember { mutableStateOf(item?.lunarLeap ?: false) }
    var repeat by remember { mutableStateOf(item?.repeatRule ?: DateRepeat.YEARLY) }
    var remind by remember { mutableStateOf(item?.remindDaysBefore ?: 0) }
    var note by remember { mutableStateOf(item?.note.orEmpty()) }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) LifeStrings.dateAdd(lang) else LifeStrings.dateEdit(lang)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "title") {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 40) title = it },
                        label = { Text(LifeStrings.dateFieldTitle(lang)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item(key = "calendar") {
                    Column {
                        Text(LifeStrings.dateFieldCalendar(lang), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        ChipFlow {
                            FilterChip(
                                selected = !lunar,
                                onClick = { lunar = false },
                                label = { Text(LifeStrings.dateSolar(lang)) }
                            )
                            FilterChip(
                                selected = lunar,
                                onClick = { lunar = true },
                                label = { Text(LifeStrings.dateLunarLabel(lang)) }
                            )
                        }
                    }
                }

                if (lunar) {
                    item(key = "lunar-month") {
                        Column {
                            Text(
                                text = LifeStrings.dateFieldLunarMonth(lang),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            ChipFlow {
                                (1..12).forEach { month ->
                                    FilterChip(
                                        selected = lunarMonth == month,
                                        onClick = { lunarMonth = month },
                                        // 月名是农历数据，不翻译
                                        label = { Text(Lunar.lunarMonthName(month, false)) }
                                    )
                                }
                            }
                        }
                    }
                    item(key = "lunar-day") {
                        Column {
                            Text(
                                text = LifeStrings.dateFieldLunarDay(lang),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            ChipFlow {
                                LUNAR_DAY_PRESETS.forEach { day ->
                                    FilterChip(
                                        selected = lunarDay == day,
                                        onClick = { lunarDay = day },
                                        label = { Text(Lunar.lunarDayName(day)) }
                                    )
                                }
                            }
                        }
                    }
                    item(key = "lunar-leap") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = lunarLeap,
                                onClick = { lunarLeap = !lunarLeap },
                                label = { Text(LifeStrings.dateLunarLeap(lang)) }
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = LifeStrings.dateLunarHint(lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    item(key = "solar-date") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(LifeStrings.dateFieldDate(lang), style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = AppStrings.monthDay(lang, date.monthValue, date.dayOfMonth),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(onClick = { showPicker = true }) {
                                    Text(AppStrings.select(lang))
                                }
                            }
                        }
                    }
                }

                item(key = "repeat") {
                    Column {
                        Text(LifeStrings.dateFieldRepeat(lang), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        ChipFlow {
                            DateRepeat.entries.forEach { option ->
                                FilterChip(
                                    selected = repeat == option,
                                    onClick = { repeat = option },
                                    label = { Text(repeatLabel(option, lang)) }
                                )
                            }
                        }
                    }
                }

                item(key = "remind") {
                    Column {
                        Text(LifeStrings.dateFieldRemind(lang), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        ChipFlow {
                            REMIND_OPTIONS.forEach { option ->
                                FilterChip(
                                    selected = remind == option,
                                    onClick = { remind = option },
                                    label = { Text(remindText(lang, option)) }
                                )
                            }
                        }
                    }
                }

                item(key = "note") {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= 300) note = it },
                        label = {
                            Text(LifeStrings.dateFieldNote(lang) + " · " + AppStrings.optional(lang))
                        },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item(key = "hint") {
                    Text(
                        text = LifeStrings.dateDialogHint(lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 农历模式下也要落一个阳历日期（排序和 ICS 导出都用它）：
                    // 能换算就换成换算结果，换不出来（比如今年没有这个闰月）就先按今天存。
                    val dateMillis = if (lunar) {
                        val today = LocalDate.now()
                        Lunar.toSolar(Lunar.LunarDate(today.year, lunarMonth, lunarDay, lunarLeap))
                            ?.toDayMillis()
                            ?: date.toDayMillis()
                    } else {
                        date.toDayMillis()
                    }
                    if (title.isNotBlank()) {
                        onSave(
                            title.trim(),
                            dateMillis,
                            lunar,
                            lunarMonth,
                            lunarDay,
                            lunarLeap,
                            repeat,
                            remind,
                            note.trim()
                        )
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
                    // 选择器返回 UTC 当天零点，按 UTC 解出「选中的那一天」
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
