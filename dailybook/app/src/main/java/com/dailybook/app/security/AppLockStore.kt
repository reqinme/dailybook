package com.dailybook.app.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用锁：是否开启 + 自设 PIN。
 *
 * 开关由用户自己决定（默认关闭）；没设过 PIN 时不会「假装开着」——
 * 只有既开了开关又有摘要才算真的启用，否则锁屏会把用户挡在外面进不去。
 */
class AppLockStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("dailybook_applock", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(loadEnabled())
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** 是否设过 PIN（用于界面上区分「设置密码」和「修改密码」） */
    fun hasPin(): Boolean = prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    /** 指纹变化（设置 / 取消 / 清空）时重算 */
    private fun loadEnabled(): Boolean =
        prefs.getBoolean(KEY_ENABLED, false) && prefs.contains(KEY_HASH)

    /** 设置或修改 PIN；顺带把开关打开（用户刚设完密码，显然是想用它） */
    fun setPin(pin: String): Boolean {
        if (!PinCode.isValid(pin)) return false
        val salt = PinCode.newSalt()
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, PinCode.hash(pin, salt))
            .putBoolean(KEY_ENABLED, true)
            .apply()
        _enabled.value = true
        return true
    }

    /** 校验 PIN；没设过密码时一律返回 false */
    fun verify(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val hash = prefs.getString(KEY_HASH, null) ?: return false
        return PinCode.verify(pin, salt, hash)
    }

    /** 开关：只有在设过 PIN 的前提下才允许打开 */
    fun setEnabled(enabled: Boolean): Boolean {
        if (enabled && !hasPin()) return false
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled && hasPin()
        return true
    }

    /** 关掉并清掉 PIN（忘记密码时只能走这里，见文档的取舍说明） */
    fun clear() {
        prefs.edit().clear().apply()
        _enabled.value = false
    }

    companion object {
        private const val KEY_ENABLED = "app_lock_enabled"
        private const val KEY_SALT = "app_lock_salt"
        private const val KEY_HASH = "app_lock_hash"

        @Volatile
        private var instance: AppLockStore? = null

        fun get(context: Context): AppLockStore =
            instance ?: synchronized(this) {
                instance ?: AppLockStore(context.applicationContext).also { instance = it }
            }
    }
}
