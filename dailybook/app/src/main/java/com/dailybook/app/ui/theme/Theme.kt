package com.dailybook.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { SYSTEM, LIGHT, DARK }

// 品牌色与配色方案在 Palette.kt（ThemePalette），这里只负责把方案套进 MaterialTheme

/** 当前配色是否为深色底（用于挑选收支颜色） */
val isDarkScheme: Boolean
    @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun expenseColor(): Color = if (isDarkScheme) ExpenseDark else ExpenseLight

@Composable
fun incomeColor(): Color = if (isDarkScheme) IncomeDark else IncomeLight

@Composable
fun DailyBookTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: ThemePalette = ThemePalette.TEAL,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        // 跟随系统取色时不套自己的配色（系统色优先），否则用用户选的配色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> darkSchemeOf(palette.spec())
        else -> lightSchemeOf(palette.spec())
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DailyBookTypography,
        content = content
    )
}
