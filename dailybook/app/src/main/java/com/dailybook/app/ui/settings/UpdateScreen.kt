package com.dailybook.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.i18n.SettingsStrings
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.SectionCard

/** 发布页地址（仓库的 remote：https://github.com/reqinme/dailybook.git） */
private const val RELEASES_URL = "https://github.com/reqinme/dailybook/releases"

/**
 * 检查更新。
 *
 * ⚠️ 这个 App **没有申请联网权限**，所以不可能在应用内自己去查最新版本 ——
 * 硬要做就得加 `INTERNET` 权限，那是这次改造明确不要的。于是这一页做的是
 * 「把发布页交给系统浏览器」：点按钮 → `ACTION_VIEW` → 浏览器打开 GitHub releases，
 * 新版本号和 APK 都在那边，用户自己比对。
 *
 * 版本号来自 [rememberAppVersion]，和关于页同一个来源，两处不会打架。
 * 没有任何网络调用；`startActivity` 包在 runCatching 里，设备上没有浏览器时
 * 只会弹一句提示，不会崩。
 *
 * [nav] 目前用不到（这一页没有下级页面），保留是为了四个设置页面签名一致，
 * 路由那边不用为它写特例。
 */
@Composable
fun UpdateScreen(nav: Navigator, modifier: Modifier = Modifier) {
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

        SectionCard {
            Text(
                text = SettingsStrings.updateCurrentVersion(lang, version.full(lang)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = SettingsStrings.updateNoNetworkNote(lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = {
                    // 只是把网址交给浏览器，App 自己不联网；没有浏览器就提示一下，不崩
                    val opened = runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL))
                        )
                    }.isSuccess
                    if (!opened) {
                        Toast.makeText(
                            context,
                            SettingsStrings.updateNoBrowser(lang) + " " + RELEASES_URL,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(SettingsStrings.updateOpenReleases(lang)) }

            Spacer(Modifier.height(10.dp))
            Text(
                text = SettingsStrings.updateUrl(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            // 网址是数据，不翻译，原样显示出来，方便用户在没有浏览器时手抄
            Text(
                text = RELEASES_URL,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = SettingsStrings.updateApkNote(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        // 版本是从哪来的、SP 上那份限制，跟关于页说的是同一件事
        SectionCard {
            Text(
                text = SettingsStrings.aboutVersionSource(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}
