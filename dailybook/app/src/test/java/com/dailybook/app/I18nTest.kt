package com.dailybook.app

import com.dailybook.app.backup.Backup
import com.dailybook.app.data.TxType
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.pick
import com.dailybook.app.i18n.pickf
import com.dailybook.app.util.formatDateHeader
import com.dailybook.app.util.formatDueLabel
import com.dailybook.app.util.formatMonthLabel
import com.dailybook.app.util.toDayMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * 四语言界面的基础校验。
 *
 * 「四种语言都给全」由编译器保证（[pick] 要求四个参数），
 * 这里补的是编译器管不到的部分：语言解析、日期格式、CSV 表头列数、占位符替换。
 */
class I18nTest {

    private val allLangs = Lang.entries.toList()

    @Test
    fun parsesLanguageTags() {
        assertEquals(Lang.ZH_CN, Lang.of("zh-CN"))
        assertEquals(Lang.ZH_TW, Lang.of("zh-TW"))
        assertEquals(Lang.EN, Lang.of("en"))
        assertEquals(Lang.JA, Lang.of("ja"))
        // 没存过 / 存了不认识的值都回到默认语言，而不是崩
        assertEquals(Lang.DEFAULT, Lang.of(null))
        assertEquals(Lang.DEFAULT, Lang.of("fr"))
    }

    @Test
    fun languageLabelsAreDistinctAndNative() {
        val labels = allLangs.map { it.label }
        assertEquals(labels.size, labels.distinct().size)
        assertTrue(labels.none { it.isBlank() })
        assertEquals("简体中文", Lang.ZH_CN.label)
        assertEquals("English", Lang.EN.label)
    }

    @Test
    fun everyLanguageHasCoreStrings() {
        allLangs.forEach { lang ->
            listOf(
                AppStrings.appName(lang),
                AppStrings.tabLedger(lang),
                AppStrings.tabTodo(lang),
                AppStrings.tabFocus(lang),
                AppStrings.tabStats(lang),
                AppStrings.tabSettings(lang),
                AppStrings.txExpense(lang),
                AppStrings.txIncome(lang),
                AppStrings.filterAll(lang),
                AppStrings.repeatDaily(lang),
                AppStrings.phaseFocus(lang),
                AppStrings.save(lang),
                AppStrings.cancel(lang)
            ).forEach { text ->
                assertTrue("语言 $lang 出现空文案", text.isNotBlank())
            }
            // 四种语言的界面文案不该互相抄（除极少数如 “OK”）
            assertNotEquals(AppStrings.tabLedger(Lang.ZH_CN), AppStrings.tabLedger(Lang.EN))
            assertNotEquals(AppStrings.tabLedger(Lang.ZH_CN), AppStrings.tabLedger(Lang.JA))
        }
    }

    @Test
    fun txTypeLabelsFollowLanguage() {
        assertEquals("支出", TxType.EXPENSE.label(Lang.ZH_CN))
        assertEquals("Expense", TxType.EXPENSE.label(Lang.EN))
        assertEquals("収入", TxType.INCOME.label(Lang.JA))
    }

    @Test
    fun monthLabelPerLanguage() {
        val month = YearMonth.of(2026, 9)
        assertEquals("2026年9月", formatMonthLabel(month, Lang.ZH_CN))
        assertEquals("2026年9月", formatMonthLabel(month, Lang.ZH_TW))
        assertEquals("Sep 2026", formatMonthLabel(month, Lang.EN))
        assertEquals("2026年9月", formatMonthLabel(month, Lang.JA))
    }

    @Test
    fun dateHeaderUsesRelativeWordsPerLanguage() {
        val today = LocalDate.now()
        assertEquals("今天", formatDateHeader(today, Lang.ZH_CN))
        assertEquals("Today", formatDateHeader(today, Lang.EN))
        assertEquals("今日", formatDateHeader(today, Lang.JA))
        assertEquals("昨天", formatDateHeader(today.minusDays(1), Lang.ZH_CN))
        assertEquals("Yesterday", formatDateHeader(today.minusDays(1), Lang.EN))
    }

    @Test
    fun dueLabelPerLanguage() {
        val today = LocalDate.now()
        assertEquals("今天到期", formatDueLabel(today.toDayMillis(), Lang.ZH_CN))
        assertEquals("Due today", formatDueLabel(today.toDayMillis(), Lang.EN))
        val overdue = formatDueLabel(today.minusDays(3).toDayMillis(), Lang.EN)
        assertTrue("逾期文案要带上天数：$overdue", overdue.contains("3"))
        assertTrue(formatDueLabel(today.minusDays(3).toDayMillis(), Lang.ZH_CN).contains("3"))
    }

    @Test
    fun csvHeaderHasSixColumnsInEveryLanguage() {
        val day = LocalDate.of(2026, 9, 14).toDayMillis()
        val tx = com.dailybook.app.data.TransactionEntity(
            amountCents = 1234,
            typeName = TxType.EXPENSE.name,
            category = "餐饮",
            note = "午饭",
            dateMillis = day,
            createdAt = day
        )
        allLangs.forEach { lang ->
            val csv = Backup.toCsv(listOf(tx), lang)
            val header = csv.trim().split("\r\n")[0].removePrefix("\uFEFF")
            assertEquals("语言 $lang 的 CSV 表头列数不对：$header", 7, header.split(",").size)
            val row = csv.trim().split("\r\n")[1]
            assertEquals("语言 $lang 的数据行列数不对：$row", 7, row.split(",").size)
            // 类型列必须是该语言的译文
            assertTrue(row.contains(TxType.EXPENSE.label(lang)))
        }
    }

    @Test
    fun overBudgetNotificationFormatsPlaceholders() {
        allLangs.forEach { lang ->
            val text = AppStrings.notifOverBudgetText(lang, "餐饮", "320.00", "300.00")
            assertTrue("语言 $lang 的占位符没替换：$text", text.contains("320.00") && text.contains("300.00"))
            assertTrue("语言 $lang 丢了分类名：$text", text.contains("餐饮"))
            val over = AppStrings.notifOverBudgetOver(lang, "餐饮", "20.00")
            assertTrue(over.contains("餐饮") && over.contains("20.00"))
        }
    }

    @Test
    fun pickAndPickfRespectRequestedLanguage() {
        assertEquals("A", pick(Lang.EN, "甲", "乙", "A", "丙"))
        assertEquals("甲", pick(Lang.ZH_CN, "甲", "乙", "A", "丙"))
        assertEquals("乙", pick(Lang.ZH_TW, "甲", "乙", "A", "丙"))
        assertEquals("丙", pick(Lang.JA, "甲", "乙", "A", "丙"))
        assertEquals("共 7 条", pickf(Lang.ZH_CN, "共 %d 条", "共 %d 條", "%d items", "%d 件", 7))
    }
}
