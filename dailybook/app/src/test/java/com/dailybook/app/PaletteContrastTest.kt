package com.dailybook.app

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.dailybook.app.ui.theme.ThemePalette
import com.dailybook.app.ui.theme.darkSchemeOf
import com.dailybook.app.ui.theme.lightSchemeOf
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 配色对比度：六套配色 × 明暗两套，逐个检查文字与背景、图标与底色是否读得清。
 *
 * 这类问题靠肉眼看截图很容易漏（尤其深色下的次色），所以直接用 WCAG 的相对亮度公式算。
 * 阈值取的是「界面控件 / 大字号」的 3:1 与「正文」的 4.5:1。
 */
class PaletteContrastTest {

    @Test
    fun everyPaletteKeepsTextReadable() {
        val problems = mutableListOf<String>()

        ThemePalette.entries.forEach { palette ->
            listOf(
                "浅色" to lightSchemeOf(palette.spec()),
                "深色" to darkSchemeOf(palette.spec())
            ).forEach { (mode, scheme) ->
                check(problems, palette, mode, scheme)
            }
        }

        assertTrue(
            "对比度不足（共 ${problems.size} 处）：\n" + problems.joinToString("\n"),
            problems.isEmpty()
        )
    }

    @Test
    fun palettesAreActuallyDifferent() {
        // 防止某个配色写错、结果和默认配色一模一样（用户切了没变化）
        val primaries = ThemePalette.entries.map { palette ->
            lightSchemeOf(palette.spec()).primary
        }
        assertTrue("六套配色的主色不应重复", primaries.distinct().size == ThemePalette.entries.size)
    }

    private fun check(
        problems: MutableList<String>,
        palette: ThemePalette,
        mode: String,
        scheme: ColorScheme
    ) {
        // 正文：底色上的正文要 4.5:1 以上
        need(problems, palette, mode, "background/onSurface", scheme.background, scheme.onSurface, 4.5)
        need(problems, palette, mode, "surface/onSurface", scheme.surface, scheme.onSurface, 4.5)
        // 次要文字（说明文字）放宽到 3:1
        need(
            problems, palette, mode, "surface/onSurfaceVariant",
            scheme.surface, scheme.onSurfaceVariant, 3.0
        )
        // 主色按钮上的文字 / 图标
        need(problems, palette, mode, "primary/onPrimary", scheme.primary, scheme.onPrimary, 3.0)
        need(
            problems, palette, mode, "secondary/onSecondary",
            scheme.secondary, scheme.onSecondary, 3.0
        )
        need(
            problems, palette, mode, "tertiary/onTertiary",
            scheme.tertiary, scheme.onTertiary, 3.0
        )
        need(
            problems, palette, mode, "primaryContainer/onPrimaryContainer",
            scheme.primaryContainer, scheme.onPrimaryContainer, 3.0
        )
        // 主色本身要和底区分得开，否则强调色看不出来
        need(problems, palette, mode, "background/primary", scheme.background, scheme.primary, 1.4)
    }

    private fun need(
        problems: MutableList<String>,
        palette: ThemePalette,
        mode: String,
        what: String,
        background: Color,
        foreground: Color,
        min: Double
    ) {
        val ratio = contrast(background, foreground)
        if (ratio < min) {
            problems += "%s/%s %s 只有 %.2f:1（需要 %.1f:1）".format(palette.name, mode, what, ratio, min)
        }
    }

    /** WCAG 对比度 */
    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }
}
