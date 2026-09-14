package com.dailybook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailybook.app.R
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.security.PinCode

/**
 * 应用锁的解锁界面：4~6 位数字键盘。
 *
 * 不做「打满 4 位自动校验」——因为密码可能是 5~6 位，那样会让长密码的用户永远进不去；
 * 满 4 位后由用户点「解锁」提交。
 */
@Composable
fun LockScreen(
    hasPin: Boolean,
    onUnlock: (String) -> Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    // 没设过密码时不该走到这里（上层会直接放行），兜个底免得出现死锁界面
    if (!hasPin) {
        LaunchedUnlock(onUnlock = { onUnlock("") })
        return
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = AppStrings.appLockEnterPin(lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))
            PinDots(filled = input.length, error = error)
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (error) AppStrings.appLockWrongPin(lang) else AppStrings.appLockPinRule(lang),
                style = MaterialTheme.typography.bodySmall,
                color = if (error) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))
            Keypad(
                lang = lang,
                onDigit = { digit ->
                    if (input.length < PinCode.MAX_LENGTH) {
                        input += digit
                        error = false
                    }
                },
                onBackspace = { input = input.dropLast(1); error = false }
            )

            Spacer(Modifier.height(20.dp))
            FilledTonalButton(
                onClick = {
                    if (onUnlock(input)) {
                        input = ""
                        error = false
                    } else {
                        error = true
                        input = ""
                    }
                },
                enabled = input.length >= PinCode.MIN_LENGTH,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(AppStrings.appLockUnlock(lang))
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = AppStrings.appLockForgot(lang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = { confirmClear = true }) {
                Text(AppStrings.appLockClearPin(lang))
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(AppStrings.appLockRemovePin(lang)) },
            text = { Text(AppStrings.appLockForgot(lang)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClear()
                }) { Text(AppStrings.appLockClearPin(lang)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(AppStrings.cancel(lang))
                }
            }
        )
    }
}

/** 已输入位数的小圆点 */
@Composable
private fun PinDots(filled: Int, error: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(PinCode.MAX_LENGTH) { index ->
            val on = index < filled
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = when {
                            error && on -> MaterialTheme.colorScheme.error
                            on -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

/** 数字键盘 */
@Composable
private fun Keypad(lang: Lang, onDigit: (Char) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { digit -> KeyButton(digit.toString()) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.width(72.dp))
            KeyButton("0") { onDigit('0') }
            OutlinedButton(
                onClick = onBackspace,
                modifier = Modifier
                    .size(width = 72.dp, height = 52.dp)
                    .semantics { contentDescription = AppStrings.appLockBackspace(lang) }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun KeyButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(width = 72.dp, height = 52.dp)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * 兜底：没设过密码时立刻放行一次。
 * 放在独立 composable 里，避免在 LockScreen 主体里直接产生副作用。
 */
@Composable
private fun LaunchedUnlock(onUnlock: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { onUnlock() }
}
