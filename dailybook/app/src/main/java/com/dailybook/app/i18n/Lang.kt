package com.dailybook.app.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale
/**
 * 界面语言。
 *
 * 语言是「App 内设置」而不是跟随系统：设置页里可以随时切，通知和提醒也跟着变。
 * tag 用来存进偏好设置，label 是设置页里显示的原文名。
 */
enum class Lang(val tag: String, val label: String) {
    ZH_CN("zh-CN", "简体中文"),
    ZH_TW("zh-TW", "繁體中文"),
    EN("en", "English"),
    JA("ja", "日本語");

    companion object {
        val DEFAULT = ZH_CN

        fun of(tag: String?): Lang = entries.firstOrNull { it.tag == tag } ?: DEFAULT
    }
}

/**
 * 四语取值。
 *
 * 四个参数在编译期就必须全给，所以「漏翻一种语言」是编译错误而不是运行时空字符串——
 * 这是这套 i18n 方案唯一的强制手段，也是最有效的一条。
 */
fun pick(lang: Lang, zhCn: String, zhTw: String, en: String, ja: String): String = when (lang) {
    Lang.ZH_CN -> zhCn
    Lang.ZH_TW -> zhTw
    Lang.EN -> en
    Lang.JA -> ja
}

/**
 * 带参数的取值，占位符同 [String.format]（%s / %d / %1$s …）。
 * 用 Locale.ROOT 格式化，避免小数点变成逗号之类的地域差异。
 */
fun pickf(
    lang: Lang,
    zhCn: String,
    zhTw: String,
    en: String,
    ja: String,
    vararg args: Any
): String = String.format(Locale.ROOT, pick(lang, zhCn, zhTw, en, ja), *args)

/** 当前语言。Compose 里读它，语言一变整棵树重组 */
val LocalLang = staticCompositionLocalOf { Lang.DEFAULT }
