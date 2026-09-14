package com.dailybook.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 配色方案：只换品牌色（主色 / 次色 / 第三色），中性底色沿用同一套，
 * 这样每个配色都保持同一份对比度与层次，不会出现某个配色在深色下读不清的情况。
 *
 * 对比度由 `PaletteContrastTest` 逐个校验（每种配色 × 明暗两套）。
 */
enum class ThemePalette {
    TEAL,
    INDIGO,
    VIOLET,
    ROSE,
    AMBER,
    FOREST;

    internal fun spec(): PaletteSpec = when (this) {
        TEAL -> PaletteSpec(
            lightPrimary = Color(0xFF17897F),
            lightPrimaryContainer = Color(0xFFCCEDE9),
            lightOnPrimaryContainer = Color(0xFF00332E),
            darkPrimary = Color(0xFF2AA79B),
            darkPrimaryContainer = Color(0xFF1B6B63),
            darkOnPrimaryContainer = Color(0xFFD6F5F1),
            lightSecondary = Color(0xFFB8791A),
            darkSecondary = Color(0xFFF2A93B),
            lightTertiary = Color(0xFF3B66C4),
            darkTertiary = Color(0xFF5B8DEF)
        )

        INDIGO -> PaletteSpec(
            lightPrimary = Color(0xFF3B5BC4),
            lightPrimaryContainer = Color(0xFFD9E1FA),
            lightOnPrimaryContainer = Color(0xFF0B1638),
            darkPrimary = Color(0xFF7C97F0),
            darkPrimaryContainer = Color(0xFF2C3F86),
            darkOnPrimaryContainer = Color(0xFFE2E8FF),
            lightSecondary = Color(0xFF8A5A2B),
            darkSecondary = Color(0xFFE0A56A),
            lightTertiary = Color(0xFF0F8A80),
            darkTertiary = Color(0xFF4FC2B6)
        )

        VIOLET -> PaletteSpec(
            lightPrimary = Color(0xFF6A4FC4),
            lightPrimaryContainer = Color(0xFFE4DCFA),
            lightOnPrimaryContainer = Color(0xFF190B3C),
            darkPrimary = Color(0xFFAE99F0),
            darkPrimaryContainer = Color(0xFF463281),
            darkOnPrimaryContainer = Color(0xFFEBE4FF),
            lightSecondary = Color(0xFFA0436E),
            darkSecondary = Color(0xFFEE8FB4),
            lightTertiary = Color(0xFF2F6FA8),
            darkTertiary = Color(0xFF7FB6E8)
        )

        ROSE -> PaletteSpec(
            lightPrimary = Color(0xFFB23A55),
            lightPrimaryContainer = Color(0xFFFADCE3),
            lightOnPrimaryContainer = Color(0xFF3C0A18),
            darkPrimary = Color(0xFFF07E97),
            darkPrimaryContainer = Color(0xFF7E2740),
            darkOnPrimaryContainer = Color(0xFFFFE3E9),
            lightSecondary = Color(0xFF9A6A16),
            darkSecondary = Color(0xFFE9BB63),
            lightTertiary = Color(0xFF4A6BA8),
            darkTertiary = Color(0xFF9DB7E8)
        )

        AMBER -> PaletteSpec(
            lightPrimary = Color(0xFF9A6410),
            lightPrimaryContainer = Color(0xFFFBE6C4),
            lightOnPrimaryContainer = Color(0xFF321C00),
            darkPrimary = Color(0xFFE9B45C),
            darkPrimaryContainer = Color(0xFF6B4508),
            darkOnPrimaryContainer = Color(0xFFFFE8C8),
            lightSecondary = Color(0xFF2F6F63),
            darkSecondary = Color(0xFF6FC0AF),
            lightTertiary = Color(0xFF6A5AA8),
            darkTertiary = Color(0xFFB2A4E8)
        )

        FOREST -> PaletteSpec(
            lightPrimary = Color(0xFF2F6B3A),
            lightPrimaryContainer = Color(0xFFD5EBD7),
            lightOnPrimaryContainer = Color(0xFF08210D),
            darkPrimary = Color(0xFF74C183),
            darkPrimaryContainer = Color(0xFF25552E),
            darkOnPrimaryContainer = Color(0xFFDDF4E1),
            lightSecondary = Color(0xFF7A5A18),
            darkSecondary = Color(0xFFD9B267),
            lightTertiary = Color(0xFF2C6A8C),
            darkTertiary = Color(0xFF7DB6D4)
        )
    }
}

/** 一套配色在明暗两种模式下的取值 */
internal data class PaletteSpec(
    val lightPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val lightOnPrimary: Color = Color(0xFFFFFFFF),
    val darkPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val darkOnPrimary: Color = Color(0xFF10201C),
    val lightSecondary: Color,
    val darkSecondary: Color,
    val lightTertiary: Color,
    val darkTertiary: Color
)

private val LIGHT_SECONDARY_ON = Color(0xFFFFFFFF)
private val DARK_SECONDARY_ON = Color(0xFF241703)
private val LIGHT_TERTIARY_ON = Color(0xFFFFFFFF)
private val DARK_TERTIARY_ON = Color(0xFF0C1A2E)

/** 浅色方案：中性底色固定，品牌色随配色变化 */
internal fun lightSchemeOf(spec: PaletteSpec) = androidx.compose.material3.lightColorScheme(
    primary = spec.lightPrimary,
    onPrimary = spec.lightOnPrimary,
    primaryContainer = spec.lightPrimaryContainer,
    onPrimaryContainer = spec.lightOnPrimaryContainer,
    secondary = spec.lightSecondary,
    onSecondary = LIGHT_SECONDARY_ON,
    tertiary = spec.lightTertiary,
    onTertiary = LIGHT_TERTIARY_ON,
    background = LightBackground,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

/** 深色方案 */
internal fun darkSchemeOf(spec: PaletteSpec) = androidx.compose.material3.darkColorScheme(
    primary = spec.darkPrimary,
    onPrimary = spec.darkOnPrimary,
    primaryContainer = spec.darkPrimaryContainer,
    onPrimaryContainer = spec.darkOnPrimaryContainer,
    secondary = spec.darkSecondary,
    onSecondary = DARK_SECONDARY_ON,
    tertiary = spec.darkTertiary,
    onTertiary = DARK_TERTIARY_ON,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)
