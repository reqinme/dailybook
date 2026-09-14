package com.dailybook.app.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.SettingsStrings
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.Route
import com.dailybook.app.ui.SectionCard

/**
 * 关于：应用名、包名、版本号 / 版本代码、最低 Android 版本、一句话说明和开源许可。
 *
 * 版本号来自 [rememberAppVersion]（读安装包信息，见那个函数的注释），
 * 和「检查更新」页用的是同一份数据，不会出现两处版本号对不上的情况。
 * 这一页没有任何网络调用。
 */
@Composable
fun AboutScreen(nav: Navigator, modifier: Modifier = Modifier) {
    val lang = LocalLang.current
    val context = LocalContext.current
    val version = rememberAppVersion()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        // ---- 这是什么 ----
        SectionCard {
            Text(
                text = SettingsStrings.aboutAppLine(lang, AppStrings.appName(lang), version.display(lang)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = SettingsStrings.aboutDescription(lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        // ---- 版本信息 ----
        SectionCard(title = SettingsStrings.aboutVersionSection(lang)) {
            InfoRow(SettingsStrings.aboutPackage(lang), context.packageName)
            Spacer(Modifier.height(8.dp))
            InfoRow(SettingsStrings.aboutVersionName(lang), version.display(lang))
            Spacer(Modifier.height(8.dp))
            InfoRow(SettingsStrings.aboutVersionCode(lang), version.codeText)
            Spacer(Modifier.height(10.dp))
            Text(
                text = SettingsStrings.aboutVersionSource(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        // ---- 最低系统版本 ----
        SectionCard {
            InfoRow(
                SettingsStrings.aboutMinAndroid(lang),
                SettingsStrings.aboutMinAndroidValue(lang, minSdkRelease(), Build.VERSION_CODES.O)
            )
        }

        Spacer(Modifier.height(14.dp))

        // ---- 数据在哪：不联网 ----
        SectionCard(title = SettingsStrings.aboutDataTitle(lang)) {
            Text(
                text = SettingsStrings.aboutAllLocal(lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(14.dp))

        // ---- 开源许可 ----
        SectionCard(title = SettingsStrings.licenseTitle(lang)) {
            Text(
                text = SettingsStrings.licenseText(lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

/**
 * 「标签 —— 值」两栏一行。
 *
 * 包名和版本号是数据（不翻译），所以值一律等宽靠右显示，长包名会自动换行。
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 支持的最低 Android 版本。
 *
 * `minSdk = 26` 写在 `app/build.gradle.kts` 里，而本模块没开 BuildConfig 生成
 * （见 [rememberAppVersion] 的注释），所以这里用 **API 级别常量** `Build.VERSION_CODES.O`
 * 作为唯一事实来源：写成 `API 26` 的那半边由编译器算出来，版本名「8.0」只是给 API 26
 * 配一个人类可读的名字。以后改 minSdk 时，改 `MIN_SDK_API` 一处即可，
 * 名字对不上就显示级别数字，不会硬编一个错的版本名。
 */
private val MIN_SDK_API: Int = Build.VERSION_CODES.O

private fun minSdkRelease(): String = when (MIN_SDK_API) {
    Build.VERSION_CODES.O -> "8.0"
    Build.VERSION_CODES.O_MR1 -> "8.1"
    Build.VERSION_CODES.P -> "9"
    Build.VERSION_CODES.Q -> "10"
    Build.VERSION_CODES.R -> "11"
    Build.VERSION_CODES.S -> "12"
    Build.VERSION_CODES.TIRAMISU -> "13"
    Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "14"
    else -> MIN_SDK_API.toString()
}
