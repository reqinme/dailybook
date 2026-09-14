package com.dailybook.app

import com.dailybook.app.backup.Backup
import com.dailybook.app.data.Currencies
import com.dailybook.app.data.TransactionEntity
import com.dailybook.app.data.TxType
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.toDayMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 多币种换算与标签解析。
 *
 * 换算结果直接进 amountCents，所有统计都建立在它之上，所以这里的舍入必须准。
 */
class CurrencyAndTagTest {

    @Test
    fun baseCurrencyIsIdentity() {
        // CNY 记录的汇率恒为 1（RATE_SCALE），换算必须原样返回
        assertEquals(1234L, Currencies.toBaseCents(1234L, Currencies.RATE_SCALE))
        assertEquals(0L, Currencies.toBaseCents(0L, Currencies.RATE_SCALE))
    }

    @Test
    fun convertsForeignAmountWithRounding() {
        // 12.34 美元，汇率 7.10：12.34 × 7.10 = 87.614 元 → 8761 分（四舍五入）
        val usd = 1234L
        val rate = (7.10 * Currencies.RATE_SCALE).toLong()
        assertEquals(8761L, Currencies.toBaseCents(usd, rate))

        // 100 日元，汇率 0.048：4.8 元 → 480 分
        val jpy = 10_000L
        val jpyRate = (0.048 * Currencies.RATE_SCALE).toLong()
        assertEquals(480L, Currencies.toBaseCents(jpy, jpyRate))
    }

    @Test
    fun zeroOrNegativeRateNeverBreaksData() {
        // 汇率缺失时按原值处理，宁可数字不准也不能变成 0 或崩
        assertEquals(500L, Currencies.toBaseCents(500L, 0L))
        assertEquals(500L, Currencies.toBaseCents(500L, -1L))
    }

    @Test
    fun convertedAmountFormatsAsMoney() {
        val rate = (7.10 * Currencies.RATE_SCALE).toLong()
        assertEquals("87.61", formatAmount(Currencies.toBaseCents(1234L, rate)))
    }

    @Test
    fun tagsAreTrimmedDedupedAndJoined() {
        assertEquals("旅行,报销", TransactionEntity.joinTags(listOf(" 旅行 ", "报销", "旅行", "  ")))
        assertEquals("", TransactionEntity.joinTags(emptyList()))
        assertEquals("", TransactionEntity.joinTags(listOf("   ", "")))
    }

    @Test
    fun tagListSplitsStoredString() {
        val tx = tx(tags = "旅行, 报销 ,,")
        assertEquals(listOf("旅行", "报销"), tx.tagList)
        assertEquals(emptyList<String>(), tx(tags = "").tagList)
    }

    @Test
    fun foreignFlagOnlyWhenCurrencyAndAmountPresent() {
        assertFalse(tx(currency = Currencies.BASE, foreignAmountCents = 1234L).isForeign)
        assertTrue(tx(currency = "USD", foreignAmountCents = 1234L).isForeign)
        // 币种写着外币但没存原币金额（老数据 / 导入）→ 不当外币展示，避免显示 0
        assertFalse(tx(currency = "USD", foreignAmountCents = 0L).isForeign)
    }

    @Test
    fun currencySymbolsCoverPresets() {
        Currencies.PRESETS.forEach { code ->
            if (code != "CNY") assertTrue(Currencies.symbolOf(code).isNotBlank())
        }
        assertEquals("¥", Currencies.symbolOf("CNY"))
        assertEquals("$", Currencies.symbolOf("USD"))
        assertEquals("未知币种不该崩", "", Currencies.symbolOf("XYZ"))
    }

    // ---- CSV 导入去重 ----

    @Test
    fun identicalRowsCollapseToOneOnRepeatedImport() {
        val csv = Backup.toCsv(listOf(tx(category = "餐饮", note = "午饭"), tx(category = "交通", note = "地铁")))
        val first = Backup.parseCsv(csv)
        assertEquals(2, first.size)

        // 第二次导入同一份文件：指纹全部命中，没有任何新行
        val seen = first.map { Backup.fingerprint(it) }.toMutableSet()
        val second = Backup.parseCsv(csv).filter { seen.add(Backup.fingerprint(it)) }
        assertEquals(0, second.size)
    }

    @Test
    fun fingerprintIgnoresIdAndCreatedAtButNotRealChanges() {
        val a = tx(category = "餐饮", note = "午饭")
        val sameAgain = a.copy(id = 99L, createdAt = a.createdAt + 5_000L)
        assertEquals(Backup.fingerprint(a), Backup.fingerprint(sameAgain))

        assertTrue(Backup.fingerprint(a) != Backup.fingerprint(a.copy(amountCents = 1001L)))
        assertTrue(Backup.fingerprint(a) != Backup.fingerprint(a.copy(note = "晚饭")))
        assertTrue(Backup.fingerprint(a) != Backup.fingerprint(a.copy(dateMillis = a.dateMillis + 86_400_000L)))
        assertTrue(Backup.fingerprint(a) != Backup.fingerprint(a.copy(account = "微信")))
    }

    @Test
    fun importKeepsGenuinelyNewRows() {
        // 注意：CSV 往返会把日期归一化到当天零点，所以这里必须用「当天零点」构造已有数据，
        // 否则指纹天然对不上，测试会假失败
        val day = LocalDate.of(2026, 9, 14).toDayMillis()
        val csv = Backup.toCsv(
            listOf(
                tx(category = "餐饮", note = "午饭", dateMillis = day),
                tx(category = "购物", note = "杯子", dateMillis = day, amountCents = 2000L)
            )
        )
        // 库里已经有「餐饮 / 午饭」那一笔
        val existing = listOf(tx(category = "餐饮", note = "午饭", dateMillis = day))
        val seen = existing.map { Backup.fingerprint(it) }.toMutableSet()
        val fresh = Backup.parseCsv(csv).filter { seen.add(Backup.fingerprint(it)) }

        assertEquals("只有「购物 / 杯子」是新的一笔", 1, fresh.size)
        assertEquals("购物", fresh[0].category)
        assertEquals(2000L, fresh[0].amountCents)
    }

    private fun tx(
        tags: String = "",
        currency: String = Currencies.BASE,
        foreignAmountCents: Long = 0L,
        category: String = "其他",
        note: String = "",
        account: String = "现金",
        amountCents: Long = 1000L,
        dateMillis: Long = 1_789_000_000_000L
    ) = TransactionEntity(
        amountCents = amountCents,
        typeName = TxType.EXPENSE.name,
        category = category,
        account = account,
        note = note,
        tags = tags,
        currency = currency,
        foreignAmountCents = foreignAmountCents,
        dateMillis = dateMillis,
        createdAt = 1_789_000_000_000L
    )
}
