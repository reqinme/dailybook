package com.dailybook.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.SettingsStrings

/**
 * 底部五个标签页。
 *
 * 这一版把「设置」从标签里拿出来（改成右上角齿轮），因为要腾位置给「生活」与「学习」两个新模块。
 * 没有引入 navigation 库：页面栈就是 [Route] 的列表，用 BackHandler 处理返回，够用且省一个依赖。
 */
enum class Tab {
    LEDGER,
    LIFE,
    STUDY,
    FOCUS,
    STATS;

    fun label(lang: Lang): String = when (this) {
        LEDGER -> AppStrings.tabLedger(lang)
        LIFE -> AppStrings.tabLife(lang)
        STUDY -> AppStrings.tabStudy(lang)
        FOCUS -> AppStrings.tabFocus(lang)
        STATS -> AppStrings.tabStats(lang)
    }

    val icon: ImageVector
        get() = when (this) {
            LEDGER -> Icons.Filled.AccountBalanceWallet
            LIFE -> Icons.Filled.Checklist
            STUDY -> Icons.Filled.School
            FOCUS -> Icons.Filled.Timer
            STATS -> Icons.Filled.BarChart
        }
}

/**
 * 设置里的大分类（系统设置式的第一层）。
 * 点进来是一个独立子页面，只放这个分类下的设置项。
 */
enum class SettingsCategory {
    APPEARANCE,
    LANGUAGE,
    LEDGER,
    FOCUS,
    STUDY,
    DATA,
    ABOUT;

    fun label(lang: Lang): String = when (this) {
        // 「外观」「记账」两行和分类子页面上的分区标题是同一句，统一取 SettingsStrings 那一份
        APPEARANCE -> SettingsStrings.sectionAppearance(lang)
        LANGUAGE -> AppStrings.settingsLanguage(lang)
        LEDGER -> SettingsStrings.sectionLedger(lang)
        FOCUS -> AppStrings.settingsFocus(lang)
        STUDY -> AppStrings.settingsStudy(lang)
        DATA -> AppStrings.settingsData(lang)
        ABOUT -> AppStrings.settingsAbout(lang)
    }
}

/** 二级页面。用字符串路由 + 可选参数，避免为一个简单页面栈引入导航库。 */
sealed interface Route {

    /** 设置首页（分类列表） */
    data object SettingsHome : Route

    /** 某个设置分类的子页面 */
    data class SettingsPage(val category: SettingsCategory) : Route

    /** 关于 */
    data object About : Route

    /** 检查更新 */
    data object Update : Route

    // ---- 生活模块 ----
    data object Habits : Route
    data object Memos : Route
    data object Milestones : Route
    data object ImportantDates : Route

    // ---- 学习模块 ----
    data object Courses : Route
    data object Exams : Route
    data object Assignments : Route
    data object Grades : Route
    data object Credits : Route
    data object Awards : Route
    data object WeeklyReport : Route

    // ---- 统计里的记录详情（点标题才打开的页面） ----
    data class StatsDetail(val kind: StatsDetailKind) : Route
}

/** 统计页里「点标题打开」的记录类型 */
enum class StatsDetailKind {
    /** 本月全部流水 */
    MONTH_ENTRIES,

    /** 分类明细 */
    CATEGORY_ENTRIES,

    /** 每日明细 */
    DAILY_ENTRIES,

    /** 专注记录 */
    FOCUS_SESSIONS,

    /** 待办完成情况 */
    TODO_SUMMARY
}

/** 页面栈操作，交给各个子页面用 */
class Navigator(
    private val stack: MutableList<Route>
) {
    fun push(route: Route) {
        stack.add(route)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun popToTop() {
        stack.clear()
    }

    val canGoBack: Boolean get() = stack.isNotEmpty()
}
