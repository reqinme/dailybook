package com.dailybook.app.ui.settings

import android.content.Context
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.SettingsStrings
import com.dailybook.app.timer.TimerViewModel
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.Route
import com.dailybook.app.ui.SectionCard
import com.dailybook.app.ui.SettingsCategory

// =====================================================================
// 设置首页：像系统设置那样，一行一个分类，点进去才是具体设置项
// =====================================================================

/**
 * 设置首页（第一层）：应用名 + 版本号的小抬头，下面是一串分类行。
 *
 * 首页**不放任何对话框、也不放任何开关** —— 每一项都推到
 * [Route.SettingsPage] 的子页面里，这样设置项再多也不会挤成一长条。
 *
 * [state] / [vm] / [timerVm] 目前首页用不到（它只是一张列表），但签名和子页面保持一致，
 * 路由那边四个页面就能用同一种调用方式，将来看板式摘要想放到首页也不用再改签名。
 */
@Composable
fun SettingsHomeScreen(
    state: UiState,
    vm: MainViewModel,
    timerVm: TimerViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val version = rememberAppVersion()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        // 抬头：应用名 + 版本号（版本号从安装包里读，见 rememberAppVersion）
        Text(
            text = SettingsStrings.homeHeader(lang, AppStrings.appName(lang), version.display(lang)),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = SettingsStrings.homeCategoryHint(lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))
        SectionCard {
            SettingsCategory.entries.forEachIndexed { index, category ->
                if (index > 0) Spacer(Modifier.height(6.dp))
                SettingsRow(
                    title = category.label(lang),
                    subtitle = category.hint(lang),
                    icon = category.icon,
                    onClick = { nav.push(Route.SettingsPage(category)) }
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

/** 分类行的图标（和 [SettingsCategory] 一样的顺序，七个分类各一个） */
private val SettingsCategory.icon: ImageVector
    get() = when (this) {
        SettingsCategory.APPEARANCE -> Icons.Filled.Palette
        SettingsCategory.LANGUAGE -> Icons.Filled.Translate
        SettingsCategory.LEDGER -> Icons.Filled.AccountBalanceWallet
        SettingsCategory.FOCUS -> Icons.Filled.Timer
        SettingsCategory.STUDY -> Icons.Filled.School
        SettingsCategory.DATA -> Icons.Filled.Storage
        SettingsCategory.ABOUT -> Icons.Filled.Info
    }

/** 分类行的副标题：一句「这个分类里有什么」 */
private fun SettingsCategory.hint(lang: Lang): String = when (this) {
    SettingsCategory.APPEARANCE -> AppStrings.settingsAppearanceHint(lang)
    SettingsCategory.LANGUAGE -> AppStrings.settingsLanguageHint(lang)
    SettingsCategory.LEDGER -> AppStrings.settingsLedgerHint(lang)
    SettingsCategory.FOCUS -> AppStrings.settingsFocusHint(lang)
    SettingsCategory.STUDY -> AppStrings.settingsStudyHint(lang)
    SettingsCategory.DATA -> AppStrings.settingsDataHint(lang)
    SettingsCategory.ABOUT -> AppStrings.settingsAboutHint(lang)
}

/**
 * 首页与子页面共用的「可点一行」：图标 + 标题 + 一行副标题 + 右侧箭头。
 *
 * 整行都可点（不是只有箭头可点），并且拆成一个公共组件，
 * 免得七个分类各写一遍一样的 Row。
 */
@Composable
internal fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailing: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =====================================================================
// 版本号：关于页与检查更新页共用同一份来源
// =====================================================================

/**
 * 版本号 / 版本代码。
 *
 * 为什么不用 `BuildConfig`：本模块的 `buildFeatures` 里没开 `buildConfig = true`
 * （只有 `compose = true`），生成不了 `com.dailybook.app.BuildConfig`；
 * 而 `build.gradle.kts` 不在这次改动范围里，所以改成从 PackageManager 读安装包信息 ——
 * 和系统设置里看到的版本号永远一致，也不需要在代码里写死一个会过期的数字。
 */
internal data class AppVersion(val name: String?, val code: Long?) {

    /** 有名字显示名字，查不到就退回 [SettingsStrings.versionUnknown]，绝不编一个假版本号 */
    fun display(lang: Lang): String = name ?: SettingsStrings.versionUnknown(lang)

    /** 「1.8（9）」；只有代码没有名字时退化成「9」 */
    fun full(lang: Lang): String = when {
        name != null && code != null -> "$name（$code）"
        name != null -> name
        code != null -> code.toString()
        else -> SettingsStrings.versionUnknown(lang)
    }

    val codeText: String get() = code?.toString() ?: "—"
}

/**
 * 读一次安装包信息并记住。
 *
 * `packageManager` 与 `getPackageInfo` 都包在 runCatching 里：设备上真出意外时
 * 关于页显示「未知」，而不是把整个设置页崩掉。
 */
@Composable
internal fun rememberAppVersion(): AppVersion {
    val context = LocalContext.current
    return remember(context) { queryAppVersion(context) }
}

/** 见 [rememberAppVersion]：读不到就给 null，由 UI 决定怎么显示 */
private fun queryAppVersion(context: Context): AppVersion {
    val info = runCatching {
        val manager = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            manager.getPackageInfo(context.packageName, 0)
        }
    }.getOrNull() ?: return AppVersion(null, null)

    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    val name = info.versionName?.takeIf { it.isNotBlank() }
    return AppVersion(name, code)
}
