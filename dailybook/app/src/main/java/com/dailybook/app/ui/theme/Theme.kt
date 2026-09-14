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

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF04201D),
    primaryContainer = TealDim,
    onPrimaryContainer = Color(0xFFD6F5F1),
    secondary = Amber,
    onSecondary = Color(0xFF2A1A00),
    tertiary = Blue,
    onTertiary = Color(0xFF0A1330),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val LightColors = lightColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCEDE9),
    onPrimaryContainer = Color(0xFF00332E),
    secondary = AmberLight,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = BlueLight,
    onTertiary = Color(0xFFFFFFFF),
    background = LightBackground,
    onSurface = LightOnSurface,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

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
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DailyBookTypography,
        content = content
    )
}
