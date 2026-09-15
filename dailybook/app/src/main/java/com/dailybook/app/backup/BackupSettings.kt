package com.dailybook.app.backup

import android.content.Context
import android.net.Uri
import com.dailybook.app.data.CategoryStore
import com.dailybook.app.data.FocusRepository
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.data.SummaryMode
import com.dailybook.app.data.TxType
import com.dailybook.app.i18n.Lang
import com.dailybook.app.ui.theme.ThemeMode
import com.dailybook.app.ui.theme.ThemePalette
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * 备份里的「设置」那一段。
 *
 * 为什么要有它：设置页对用户的承诺是「一个 JSON 文件装下全部数据……以及**预算与各类设置**」，
 * 但以前真正写进文件的只有月度预算（`budgetCents`），恢复之后主题、语言、提醒时间、
 * 番茄钟时长、分类清单、汇率全都不跟着走 —— 承诺与事实不符。
 *
 * 覆盖范围（都是 SharedPreferences / DataStore 里的偏好，**没有**任何数据库结构改动）：
 * - 预算：月度预算（仍在顶层的 `budgetCents`，老文件也读它）、分类预算；
 * - 外观：主题模式、配色方案、跟随系统取色、自定义背景图 URI 与蒙版浓度；
 * - 语言：界面语言；
 * - 记账：每晚记账提醒（开关 + 时间）、预算预警开关、定期小结频率、每日专注目标；
 * - 专注计时：专注 / 短休息 / 长休息时长、长休息间隔、震动、自动开始下一阶段、保持常亮；
 * - 学习：学期起始日、GPA 口径、上课提醒（开关 + 提前分钟数）；
 * - 记账数据周边：用户自己增删过的分类清单、记过的币种汇率；
 * - 自动备份：开关与备份文件夹（树 URI）。
 *
 * **不包含**的东西（有意为之，都是「本机的运行状态」而不是用户在设置里做的选择）：
 * 预算预警「这个月已经提醒过哪几档」的去重记录、待办提醒的已提醒 / 已排程键、
 * 当前选中的专注目标待办、小组件的实例状态。恢复它们没有意义，反而可能让新机漏提醒。
 *
 * 与老备份的兼容：
 * - 老备份文件里**没有** `settings` 这一段，解析出来是 `null`（见 [Backup.parse]），
 *   恢复时整段跳过 —— 老备份照旧能恢复，而且不会把用户当前的设置悄悄改成默认值；
 * - 这一段是**加在同一个 FORMAT 里的新键**，所以 [Backup.FORMAT] 不用加版本号：
 *   加版本号反而会把「其实读得懂的老备份」判成不兼容（同 `courseName` / `startMinutes` 的先例）。
 */
data class BackupSettings(
    /** 分类预算：分类名 → 每月上限（分），只写大于 0 的项 */
    val categoryBudgets: Map<String, Long> = emptyMap(),
    /** 主题模式（[ThemeMode] 的枚举名）：SYSTEM / LIGHT / DARK */
    val themeMode: String = ThemeMode.SYSTEM.name,
    /** 配色方案（[ThemePalette] 的枚举名） */
    val palette: String = ThemePalette.TEAL.name,
    /** 是否跟随系统取色（Android 12+） */
    val dynamicColor: Boolean = false,
    /** 自定义背景图（SAF 授权过的 URI 字符串）；空串 = 不用背景图 */
    val backgroundUri: String = "",
    /** 背景图上的蒙版浓度（0~80） */
    val backgroundScrim: Int = 30,
    /** 界面语言（[Lang.tag]），例如 zh-CN */
    val lang: String = Lang.DEFAULT.tag,
    /** 每晚记账提醒：开关与时间 */
    val ledgerReminderEnabled: Boolean = false,
    val ledgerReminderHour: Int = 21,
    val ledgerReminderMinute: Int = 0,
    /** 预算预警（用到 80% / 超支时推一条通知） */
    val budgetAlert: Boolean = true,
    /** 定期小结频率（[SummaryMode] 的枚举名）：OFF / WEEKLY / MONTHLY */
    val summaryMode: String = SummaryMode.OFF.name,
    /** 每日专注目标（个），0 = 不设目标 */
    val focusGoal: Int = 0,
    /** 专注计时的时长与行为（DataStore 里的那一份） */
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakEvery: Int = 4,
    val autoStartNext: Boolean = false,
    val vibrate: Boolean = true,
    val keepScreenOn: Boolean = true,
    /** 学期起始日（周一，当天 00:00）；0 = 还没设 */
    val termStartMillis: Long = 0L,
    /** GPA 计算口径：4.0 或 5.0 */
    val gpaScale: Double = 4.0,
    /** 上课提醒：开关与提前分钟数 */
    val classReminder: Boolean = false,
    val classReminderMinutes: Int = 15,
    /** 用户自己增删过之后的分类清单（支出 / 收入）；空列表 = 用预置分类 */
    val expenseCategories: List<String> = emptyList(),
    val incomeCategories: List<String> = emptyList(),
    /** 记过的币种汇率：币种 → ×[com.dailybook.app.data.Currencies.RATE_SCALE] 的整数汇率 */
    val currencyRates: Map<String, Long> = emptyMap(),
    /** 自动备份开关；null = 本段设置里没写（老文件），恢复时不要动本机的选择 */
    val autoBackupEnabled: Boolean? = null,
    /** 自动备份的文件夹（树 URI 字符串）；null = 没写 */
    val autoBackupFolder: String? = null
) {

    /** 写成备份文件里的 `settings` 对象 */
    fun toJson(): JSONObject = JSONObject().apply {
        put("categoryBudgets", JSONObject().apply {
            categoryBudgets.forEach { (category, cents) -> put(category, cents) }
        })
        put("themeMode", themeMode)
        put("palette", palette)
        put("dynamicColor", dynamicColor)
        put("backgroundUri", backgroundUri)
        put("backgroundScrim", backgroundScrim)
        put("lang", lang)
        put("ledgerReminderEnabled", ledgerReminderEnabled)
        put("ledgerReminderHour", ledgerReminderHour)
        put("ledgerReminderMinute", ledgerReminderMinute)
        put("budgetAlert", budgetAlert)
        put("summaryMode", summaryMode)
        put("focusGoal", focusGoal)
        put("focusMinutes", focusMinutes)
        put("shortBreakMinutes", shortBreakMinutes)
        put("longBreakMinutes", longBreakMinutes)
        put("longBreakEvery", longBreakEvery)
        put("autoStartNext", autoStartNext)
        put("vibrate", vibrate)
        put("keepScreenOn", keepScreenOn)
        put("termStartMillis", termStartMillis)
        put("gpaScale", gpaScale)
        put("classReminder", classReminder)
        put("classReminderMinutes", classReminderMinutes)
        put("expenseCategories", JSONArray(expenseCategories))
        put("incomeCategories", JSONArray(incomeCategories))
        put("currencyRates", JSONObject().apply {
            currencyRates.forEach { (code, rate) -> put(code, rate) }
        })
        // org.json 的 put(key, null) 是「删掉这个键」，所以这里必须显式写 JSONObject.NULL
        put("autoBackupEnabled", autoBackupEnabled ?: JSONObject.NULL)
        put("autoBackupFolder", autoBackupFolder ?: JSONObject.NULL)
    }

    companion object {

        /**
         * 从 `settings` 对象里读回来。
         *
         * 每一个键都**带默认值**：这段设置将来再加键时，旧文件里的缺失键会退回默认值，
         * 而不是让整次恢复失败（和备份里其它段落的读法一致）。
         */
        fun fromJson(json: JSONObject): BackupSettings = BackupSettings(
            categoryBudgets = json.optJSONObject("categoryBudgets").toLongMap(),
            themeMode = json.optString("themeMode", ThemeMode.SYSTEM.name),
            palette = json.optString("palette", ThemePalette.TEAL.name),
            dynamicColor = json.optBoolean("dynamicColor", false),
            backgroundUri = json.optString("backgroundUri", ""),
            backgroundScrim = json.optInt("backgroundScrim", 30).coerceIn(0, 80),
            lang = json.optString("lang", Lang.DEFAULT.tag),
            ledgerReminderEnabled = json.optBoolean("ledgerReminderEnabled", false),
            ledgerReminderHour = json.optInt("ledgerReminderHour", 21).coerceIn(0, 23),
            ledgerReminderMinute = json.optInt("ledgerReminderMinute", 0).coerceIn(0, 59),
            budgetAlert = json.optBoolean("budgetAlert", true),
            summaryMode = json.optString("summaryMode", SummaryMode.OFF.name),
            focusGoal = json.optInt("focusGoal", 0).coerceIn(0, 20),
            focusMinutes = json.optInt("focusMinutes", 25),
            shortBreakMinutes = json.optInt("shortBreakMinutes", 5),
            longBreakMinutes = json.optInt("longBreakMinutes", 15),
            longBreakEvery = json.optInt("longBreakEvery", 4).coerceIn(2, 8),
            autoStartNext = json.optBoolean("autoStartNext", false),
            vibrate = json.optBoolean("vibrate", true),
            keepScreenOn = json.optBoolean("keepScreenOn", true),
            termStartMillis = json.optLong("termStartMillis", 0L).coerceAtLeast(0L),
            gpaScale = json.optDouble("gpaScale", 4.0),
            classReminder = json.optBoolean("classReminder", false),
            classReminderMinutes = json.optInt("classReminderMinutes", 15),
            expenseCategories = json.optJSONArray("expenseCategories").toStringList(),
            incomeCategories = json.optJSONArray("incomeCategories").toStringList(),
            currencyRates = json.optJSONObject("currencyRates").toLongMap(),
            autoBackupEnabled = if (json.isNull("autoBackupEnabled")) {
                null
            } else {
                json.optBoolean("autoBackupEnabled", false)
            },
            autoBackupFolder = if (json.isNull("autoBackupFolder")) {
                null
            } else {
                json.optString("autoBackupFolder", "").takeIf { it.isNotBlank() }
            }
        )

        private fun JSONObject?.toLongMap(): Map<String, Long> {
            val source = this ?: return emptyMap()
            return source.keys().asSequence()
                .associateWith { key -> source.optLong(key, 0L) }
                .filterValues { it > 0L }
        }

        private fun JSONArray?.toStringList(): List<String> {
            val source = this ?: return emptyList()
            return (0 until source.length())
                .map { source.optString(it) }
                .filter { it.isNotBlank() }
        }
    }
}

/**
 * 读出本机当前的设置，准备写进备份文件。
 *
 * 只在 IO 线程调用（要读 DataStore），[AutoBackup] 与导出备份都走它，
 * 保证「自动备份」与「手动导出」写出来的设置完全一致。
 */
suspend fun readBackupSettings(context: Context): BackupSettings {
    val app = context.applicationContext
    val store = SettingsStore.get(app)
    val categories = CategoryStore.get(app)
    val focus = FocusRepository(app).settings.first()
    val autoBackup = AutoBackup.get(app)

    return BackupSettings(
        categoryBudgets = store.categoryBudgets.value,
        themeMode = store.themeMode.value.name,
        palette = store.palette.value.name,
        dynamicColor = store.dynamicColor.value,
        backgroundUri = store.backgroundUri.value,
        backgroundScrim = store.backgroundScrim.value,
        lang = store.lang.value.tag,
        ledgerReminderEnabled = store.ledgerReminderEnabled.value,
        ledgerReminderHour = store.ledgerReminderHour.value,
        ledgerReminderMinute = store.ledgerReminderMinute.value,
        budgetAlert = store.budgetAlert.value,
        summaryMode = store.summaryMode.value.name,
        focusGoal = store.focusGoal.value,
        focusMinutes = focus.focusMinutes,
        shortBreakMinutes = focus.shortBreakMinutes,
        longBreakMinutes = focus.longBreakMinutes,
        longBreakEvery = focus.longBreakEvery,
        autoStartNext = focus.autoStartNext,
        vibrate = focus.vibrate,
        keepScreenOn = focus.keepScreenOn,
        termStartMillis = store.termStartMillis.value,
        gpaScale = store.gpaScale.value,
        classReminder = store.classReminder.value,
        classReminderMinutes = store.classReminderMinutes.value,
        expenseCategories = categories.expense.value,
        incomeCategories = categories.income.value,
        currencyRates = store.currencyRates(),
        autoBackupEnabled = autoBackup.enabled.value,
        autoBackupFolder = autoBackup.folderUri.value
    )
}

/**
 * 把备份文件里的设置写回本机（恢复备份时调）。
 *
 * 只调用已有的 setter，所以每一处的取值范围 / 兜底规则和设置页里手点一下**完全一样**：
 * 认不出来的枚举名被 `runCatching` 挡掉、越界的数值被 setter 自己夹回范围、
 * 分类清单里的空名与超长名由 [CategoryStore.setAll] 过滤。
 *
 * 提醒类设置改完不用在这里手动重排闹钟：MainViewModel 的 init 里有一组 collector
 * 盯着这些开关与时间，值一变就会重新 sync（记账提醒 / 定期小结 / 上课提醒）。
 */
suspend fun applyBackupSettings(context: Context, settings: BackupSettings) {
    val app = context.applicationContext
    val store = SettingsStore.get(app)

    // ---- 预算 ----
    store.setCategoryBudgets(settings.categoryBudgets)

    // ---- 外观 ----
    runCatching { ThemeMode.valueOf(settings.themeMode) }.getOrNull()?.let(store::setThemeMode)
    runCatching { ThemePalette.valueOf(settings.palette) }.getOrNull()?.let(store::setPalette)
    store.setDynamicColor(settings.dynamicColor)
    store.setBackgroundUri(settings.backgroundUri)
    store.setBackgroundScrim(settings.backgroundScrim)

    // ---- 语言（界面立刻跟着变，桌面小组件也会被 MainViewModel 推一次）----
    store.setLang(Lang.of(settings.lang))

    // ---- 记账 ----
    store.setLedgerReminder(settings.ledgerReminderEnabled)
    store.setLedgerReminderTime(settings.ledgerReminderHour, settings.ledgerReminderMinute)
    store.setBudgetAlert(settings.budgetAlert)
    runCatching { SummaryMode.valueOf(settings.summaryMode) }.getOrNull()?.let(store::setSummaryMode)
    store.setFocusGoal(settings.focusGoal)

    // ---- 专注计时（DataStore）----
    val focus = FocusRepository(app)
    focus.setFocusMinutes(settings.focusMinutes)
    focus.setShortBreakMinutes(settings.shortBreakMinutes)
    focus.setLongBreakMinutes(settings.longBreakMinutes)
    focus.setLongBreakEvery(settings.longBreakEvery)
    focus.setAutoStart(settings.autoStartNext)
    focus.setVibrate(settings.vibrate)
    focus.setKeepScreenOn(settings.keepScreenOn)

    // ---- 学习 ----
    store.setTermStartMillis(settings.termStartMillis)
    store.setGpaScale(settings.gpaScale)
    store.setClassReminder(settings.classReminder)
    store.setClassReminderMinutes(settings.classReminderMinutes)

    // ---- 分类清单与汇率 ----
    val categories = CategoryStore.get(app)
    // 空列表＝文件里没带（或本来就空的），按「用预置分类」处理，不覆盖本机现有的清单
    if (settings.expenseCategories.isNotEmpty()) {
        categories.setAll(TxType.EXPENSE, settings.expenseCategories)
    }
    if (settings.incomeCategories.isNotEmpty()) {
        categories.setAll(TxType.INCOME, settings.incomeCategories)
    }
    settings.currencyRates.forEach { (code, rate) -> store.setCurrencyRate(code, rate) }

    // ---- 自动备份（文件夹在别的手机上可能已经不存在，setFolder 内部自己兜住异常）----
    val autoBackup = AutoBackup.get(app)
    settings.autoBackupFolder?.let { folder ->
        runCatching { autoBackup.setFolder(Uri.parse(folder)) }
    }
    settings.autoBackupEnabled?.let(autoBackup::setEnabled)
}
