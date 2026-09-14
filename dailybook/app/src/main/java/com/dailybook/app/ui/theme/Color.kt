package com.dailybook.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---- 深色 ----
val DarkBackground = Color(0xFF0F1115)
val DarkSurface = Color(0xFF171A21)
val DarkSurfaceVariant = Color(0xFF21252F)
val DarkOnSurface = Color(0xFFE6E8EE)
val DarkOnSurfaceVariant = Color(0xFF9AA1B2)
val DarkOutline = Color(0xFF2C313D)

// ---- 浅色 ----
val LightBackground = Color(0xFFF7F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF1F5)
val LightOnSurface = Color(0xFF1B1C1F)
val LightOnSurfaceVariant = Color(0xFF5C6069)
val LightOutline = Color(0xFFDCE0E6)

// 品牌色不在这里：v1.8 起改成 12 套可切换配色，统一放在 Palette.kt 的 PaletteSpec 里。
// （这里原来还有一组写死的品牌色 val，早已无人引用，v1.10 删除，免得下次有人改了它却发现界面没变。）

// ---- 收支颜色（深色底） ----
val ExpenseDark = Color(0xFFFF7A6E)
val IncomeDark = Color(0xFF4DD6A0)

// ---- 收支颜色（浅色底） ----
val ExpenseLight = Color(0xFFD1453B)
val IncomeLight = Color(0xFF1F8F68)
