package com.dailybook.app.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 应用锁的 PIN 规则与摘要计算。
 *
 * 只存「盐 + SHA-256 摘要」，**不存明文**；摘要计算是纯函数，所以可以单独测。
 * 这里不做密钥派生（PBKDF2/Argon2）——它是本地 4~6 位数字锁，
 * 目的是挡住「别人拿起你手机随手翻」，不是抗离线爆破；这一点在文档里写清楚。
 */
object PinCode {

    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 6

    /**
     * 把全角数字统一成半角。
     *
     * 中文 / 日文输入法很容易打出全角「１２３４」，如果设置时存的是全角、解锁时打的是半角，
     * 用户会被自己的锁挡在外面。所以一律先归一化再校验与计算摘要。
     */
    fun normalize(raw: String): String = buildString(raw.length) {
        raw.forEach { ch ->
            val code = ch.code
            append(if (code in 0xFF10..0xFF19) (code - 0xFF10 + '0'.code).toChar() else ch)
        }
    }

    /** 只允许 4~6 位数字（全角会被归一化后接受；字母、空格、超长一律拒绝） */
    fun isValid(pin: String): Boolean {
        val normalized = normalize(pin)
        return normalized.length in MIN_LENGTH..MAX_LENGTH && normalized.all { it in '0'..'9' }
    }

    /** 生成随机盐（十六进制） */
    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    /** pin + 盐 的 SHA-256 摘要（十六进制小写）；PIN 先归一化 */
    fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$salt:${normalize(pin)}".toByteArray(Charsets.UTF_8))
            .toHex()

    /** 校验：恒定时间比较，避免按字符提前返回带来的时间差 */
    fun verify(pin: String, salt: String, expectedHash: String): Boolean {
        val actual = hash(pin, salt)
        if (actual.length != expectedHash.length) return false
        var diff = 0
        for (i in actual.indices) {
            diff = diff or (actual[i].code xor expectedHash[i].code)
        }
        return diff == 0
    }

    private fun ByteArray.toHex(): String {
        val out = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            out.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return out.toString()
    }

    private const val HEX = "0123456789abcdef"
}
