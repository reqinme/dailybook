package com.dailybook.app

import com.dailybook.app.security.PinCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 应用锁的 PIN 规则与摘要。
 *
 * 重点：合法长度边界、摘要不含明文、同样的 PIN 配不同盐得到不同摘要、校验对错分明。
 */
class PinCodeTest {

    @Test
    fun acceptsFourToSixDigitsOnly() {
        assertTrue(PinCode.isValid("1234"))
        assertTrue(PinCode.isValid("123456"))
        assertFalse("3 位太短", PinCode.isValid("123"))
        assertFalse("7 位太长", PinCode.isValid("1234567"))
        assertFalse("不能有字母", PinCode.isValid("12a4"))
        assertFalse("不能为空", PinCode.isValid(""))
        assertFalse("不能有空格", PinCode.isValid("12 4"))
    }

    @Test
    fun fullWidthDigitsAreNormalized() {
        // 中文 / 日文输入法打出的全角数字必须能用，否则用户会被自己的锁挡在外面
        assertEquals("1234", PinCode.normalize("１２３４"))
        assertEquals("1234", PinCode.normalize("1234"))
        assertEquals("12", PinCode.normalize("1２"))
        assertTrue(PinCode.isValid("１２３４"))
        assertFalse("归一化后仍是 3 位，太短", PinCode.isValid("１２３"))
        // 关键：设置时全角、解锁时半角，也要能进得去
        val salt = PinCode.newSalt()
        val stored = PinCode.hash("１２３４", salt)
        assertTrue(PinCode.verify("1234", salt, stored))
        assertTrue(PinCode.verify("１２３４", salt, stored))
    }

    @Test
    fun hashIsDeterministicAndNotPlaintext() {
        val salt = "abc123"
        val hash = PinCode.hash("1234", salt)
        assertEquals("同样的 PIN + 同样的盐必须得到同样的摘要", hash, PinCode.hash("1234", salt))
        assertEquals("SHA-256 十六进制长度固定", 64, hash.length)
        assertFalse("摘要里不能出现明文 PIN", hash.contains("1234"))
        assertTrue("只应含十六进制字符", hash.all { it in "0123456789abcdef" })
    }

    @Test
    fun samePinDifferentSaltGivesDifferentHash() {
        // 盐的作用：两个人用同样的 PIN，摘要也不能一样
        assertNotEquals(PinCode.hash("1234", "salt-a"), PinCode.hash("1234", "salt-b"))
        assertNotEquals(PinCode.newSalt(), PinCode.newSalt())
    }

    @Test
    fun differentPinsGiveDifferentHashes() {
        val salt = PinCode.newSalt()
        assertNotEquals(PinCode.hash("1234", salt), PinCode.hash("1235", salt))
    }

    @Test
    fun verifyAcceptsOnlyTheRightPin() {
        val salt = PinCode.newSalt()
        val hash = PinCode.hash("4321", salt)
        assertTrue(PinCode.verify("4321", salt, hash))
        assertFalse(PinCode.verify("4322", salt, hash))
        assertFalse(PinCode.verify("43210", salt, hash))
        assertFalse(PinCode.verify("", salt, hash))
        // 盐换了，旧摘要就不再匹配（改了盐等于改锁）
        assertFalse(PinCode.verify("4321", PinCode.newSalt(), hash))
    }

    @Test
    fun verifyRejectsMalformedStoredHash() {
        // 存坏了（长度不对）不能崩，也不能放行
        val salt = "salt"
        assertFalse(PinCode.verify("1234", salt, ""))
        assertFalse(PinCode.verify("1234", salt, "not-a-hash"))
    }

    @Test
    fun newSaltIsRandomHexOfFixedLength() {
        val salt = PinCode.newSalt()
        assertEquals("16 字节 = 32 个十六进制字符", 32, salt.length)
        assertTrue(salt.all { it in "0123456789abcdef" })
    }
}
