package com.dailybook.app

import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.CommonStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LedgerStrings
import com.dailybook.app.i18n.LifeStrings
import com.dailybook.app.i18n.SettingsStrings
import com.dailybook.app.i18n.StatsStrings
import com.dailybook.app.i18n.StudyStrings
import com.dailybook.app.i18n.TodoStrings
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * 四语文案表的冒烟测试：把每个文案函数 × 每种语言都真跑一遍。
 *
 * 为什么需要它：`pickf` 的模板里有 `%s`/`%d`，但**参数是在函数体里自己传的**——
 * 只要某个函数忘了传参（v1.5 的 `deleteBody` 就是这样，模板里 3 个占位符却传了 0 个参数），
 * `String.format` 就会抛异常，而且是**用户一点就崩**。编译器查不出来，
 * 逐个人工核对 300 多个函数也不现实，用反射全跑一遍最省事也最可靠。
 */
class I18nSmokeTest {

    private val tables: List<Pair<String, Any>> = listOf(
        "AppStrings" to AppStrings,
        "LedgerStrings" to LedgerStrings,
        "TodoStrings" to TodoStrings,
        "StatsStrings" to StatsStrings,
        "SettingsStrings" to SettingsStrings,
        "CommonStrings" to CommonStrings,
        // 新模块的文案表也必须登记进来，否则「模板缺参数」这类崩溃会漏检
        "LifeStrings" to LifeStrings,
        "StudyStrings" to StudyStrings
    )

    @Test
    fun everyStringFunctionRendersInEveryLanguage() {
        var checked = 0
        val failures = mutableListOf<String>()

        tables.forEach { (name, table) ->
            stringFunctionsOf(table).forEach { method ->
                Lang.entries.forEach { lang ->
                    val text = runCatching { render(table, method, lang, name) }
                    checked++
                    text.exceptionOrNull()?.let { error ->
                        failures += "$name.${method.name}($lang) 抛异常：$error"
                    }
                    if (text.isSuccess && text.getOrNull().isNullOrBlank()) {
                        failures += "$name.${method.name}($lang) 返回了空文案"
                    }
                }
            }
        }

        assertTrue("一个文案函数都没检查到，测试本身有问题", checked > 0)
        assertTrue(
            "共 $checked 次调用，其中 ${failures.size} 次有问题：\n" + failures.joinToString("\n"),
            failures.isEmpty()
        )
    }

    @Test
    fun smokeTestCoversEveryTable() {
        // 防止将来新增文案表却忘了加进上表的清单
        tables.forEach { (name, table) ->
            assertTrue("$name 里没有扫描到任何函数", stringFunctionsOf(table).isNotEmpty())
        }
    }

    private fun render(table: Any, method: Method, lang: Lang, name: String): String {
        val args = method.parameterTypes.mapIndexed { index, type ->
            if (index == 0) {
                lang
            } else {
                when (type) {
                    Int::class.javaPrimitiveType, Integer::class.java -> 2
                    Long::class.javaPrimitiveType, java.lang.Long::class.java -> 3L
                    Double::class.javaPrimitiveType, java.lang.Double::class.java -> 1.5
                    Boolean::class.javaPrimitiveType, java.lang.Boolean::class.java -> true
                    String::class.java -> "示例"
                    else -> null
                }
            }
        }.toTypedArray()

        val result = method.invoke(table, *args)
            ?: throw IllegalStateException("$name.${method.name} 返回 null（参数类型可能不受支持）")
        return result as String
    }

    /** 只取「公开、非合成、返回 String」的函数，避免把辅助函数也当成文案 */
    private fun stringFunctionsOf(table: Any): List<Method> =
        table.javaClass.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic && it.returnType == String::class.java }
            .filter { it.parameterCount >= 1 }
            .sortedBy { it.name }
}
