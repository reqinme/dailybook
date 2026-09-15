package com.dailybook.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * 分类清单：预置分类是默认值，用户可以自己加、自己删。
 *
 * 只存名字（字符串），因为记账记录里也是按名字存的——删掉一个分类不会动老记录，
 * 老记录里的那个分类名依然能从「数据里出现过的分类」里选回来。
 * 全部删光时回落到预置列表，避免分类选择器变成空的。
 */
class CategoryStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("dailybook_categories", Context.MODE_PRIVATE)

    private val _expense = MutableStateFlow(load(KEY_EXPENSE, Categories.EXPENSE))
    val expense: StateFlow<List<String>> = _expense.asStateFlow()

    private val _income = MutableStateFlow(load(KEY_INCOME, Categories.INCOME))
    val income: StateFlow<List<String>> = _income.asStateFlow()

    fun forType(type: TxType): List<String> =
        if (type == TxType.EXPENSE) _expense.value else _income.value

    fun add(type: TxType, name: String): Boolean {
        val text = name.trim()
        if (text.isEmpty() || text.length > 8) return false
        val current = forType(type)
        if (current.contains(text)) return false
        save(type, current + text)
        return true
    }

    /** 删除只是从选择器里移除；老记录里的分类名不受影响 */
    fun remove(type: TxType, name: String) {
        val current = forType(type)
        if (current.size <= 1) return
        save(type, current.filterNot { it == name })
    }

    fun reset(type: TxType) {
        save(type, if (type == TxType.EXPENSE) Categories.EXPENSE else Categories.INCOME)
    }

    /**
     * 整体替换某一类的清单（恢复备份时用）。
     *
     * 和 [add] 同一套过滤：去掉空白、空名与超过 8 个字的名字，并去重；
     * 过滤后为空时 [save] 会回落到预置列表（和「全删光」一样，避免选择器变成空的）。
     */
    fun setAll(type: TxType, names: List<String>) {
        val safe = names.map { it.trim() }
            .filter { it.isNotEmpty() && it.length <= 8 }
            .distinct()
        save(type, safe)
    }

    private fun save(type: TxType, list: List<String>) {
        val safe = if (list.isEmpty()) {
            if (type == TxType.EXPENSE) Categories.EXPENSE else Categories.INCOME
        } else {
            list
        }
        val key = if (type == TxType.EXPENSE) KEY_EXPENSE else KEY_INCOME
        prefs.edit().putString(key, JSONArray(safe).toString()).apply()
        if (type == TxType.EXPENSE) _expense.value = safe else _income.value = safe
    }

    private fun load(key: String, fallback: List<String>): List<String> {
        val raw = prefs.getString(key, null) ?: return fallback
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(fallback).ifEmpty { fallback }
    }

    companion object {
        private const val KEY_EXPENSE = "expense_categories"
        private const val KEY_INCOME = "income_categories"

        @Volatile
        private var instance: CategoryStore? = null

        fun get(context: Context): CategoryStore =
            instance ?: synchronized(this) {
                instance ?: CategoryStore(context.applicationContext).also { instance = it }
            }
    }
}
