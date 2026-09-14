package com.dailybook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.CommonStrings
import com.dailybook.app.i18n.LocalLang
import kotlin.math.roundToInt

// ============================================================
// 通用视觉常量
// ============================================================

/** 全局统一的圆角，避免每个页面各写一个数字 */
object Shapes {
    val card = RoundedCornerShape(20.dp)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val pill = RoundedCornerShape(50)
    val badge = RoundedCornerShape(8.dp)
}

/** 分类配色板 */
private val CATEGORY_COLORS = listOf(
    Color(0xFFEF7C6B), Color(0xFFF2A93B), Color(0xFF5FBF96), Color(0xFF5B8DEF),
    Color(0xFF9B8CFF), Color(0xFFE86FA9), Color(0xFF4EC5C1), Color(0xFFD98E5F),
    Color(0xFF8FBF4E), Color(0xFF7A8CA8)
)

/** 按分类名稳定取色（同名永远同色） */
fun categoryColor(category: String): Color {
    val index = (category.hashCode() % CATEGORY_COLORS.size + CATEGORY_COLORS.size) %
        CATEGORY_COLORS.size
    return CATEGORY_COLORS[index]
}

// ============================================================
// 基础组件
// ============================================================

/** 月份切换条（记账页与统计页共用） */
@Composable
fun MonthSwitcher(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    showToday: Boolean
) {
    val lang = LocalLang.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = CommonStrings.prevMonth(lang))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        if (showToday) {
            TextButton(onClick = onToday) { Text(CommonStrings.backToThisMonth(lang)) }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = CommonStrings.nextMonth(lang))
        }
    }
}

/** 统一的卡片容器 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor
                )
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

/** 数字指标块（统计页大量复用） */
@Composable
fun StatBlock(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            maxLines = 1
        )
    }
}

/** 空状态提示 */
@Composable
fun EmptyHint(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = emoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** 搜索框（带清除按钮） */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    val hint = placeholder ?: AppStrings.search(lang)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(hint) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = CommonStrings.clearSearch(lang))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = Shapes.pill,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    )
}

/** 带数值标签的滑块 */
@Composable
fun LabeledSlider(
    label: String,
    value: Int,
    range: IntRange,
    valueText: String,
    onChange: (Int) -> Unit
) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueText.replace("%d", local.roundToInt().toString()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            onValueChangeFinished = { onChange(local.roundToInt()) }
        )
    }
}

/** 设置项：标题 + 开关 */
@Composable
fun LabeledSwitch(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** 会换行的标签流（小屏/大字模式下不会把分类挤出屏幕） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        content()
    }
}

/** 二次确认弹窗（清除数据等危险操作统一走这里） */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalLang.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirmText ?: AppStrings.confirm(lang)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel(lang)) }
        }
    )
}

/** 圆形分类图标（emoji + 该分类的主题色底） */
@Composable
fun CategoryBadge(
    category: String,
    emoji: String,
    size: Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(categoryColor(category).copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
    }
}

/** 细进度条（预算、占比都用它） */
@Composable
fun ThinProgressBar(
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(trackColor, Shapes.pill)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .height(height)
                .background(color, Shapes.pill)
        )
    }
}

/** 小标题 */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}
