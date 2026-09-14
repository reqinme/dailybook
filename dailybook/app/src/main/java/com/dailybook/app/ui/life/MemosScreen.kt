package com.dailybook.app.ui.life

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.data.MemoEntity
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.i18n.LifeStrings
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.ui.ConfirmDialog
import com.dailybook.app.ui.EmptyHint
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.Shapes
import com.dailybook.app.util.toLocalDate

/**
 * 备忘录。
 *
 * 两列瀑布流（[LazyVerticalStaggeredGrid]）：卡片高度跟着内容走，
 * 两列各自排自己的，短卡片下面不会留一大片空白，320dp 窄屏也够放两列。
 *
 * ⚠️ 点卡片**没有** push `Route.MemoDetail`：父级那个 `MemoDetailScreen` 还没写，
 * push 过去会是一屏空白。所以这里就地打开一个「全文弹窗」看完整内容，
 * 本页自给自足，不依赖父级补齐页面。等 MemoDetailScreen 就位后，
 * 把卡片的 clickable 换成 `nav.push(Route.MemoDetail(memo.id))` 即可。
 */
@Composable
fun MemosScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current

    var composing by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MemoEntity?>(null) }
    var reading by remember { mutableStateOf<MemoEntity?>(null) }
    var deleting by remember { mutableStateOf<MemoEntity?>(null) }

    val pinned = state.memos.count { it.pinned }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = LifeStrings.memosTitle(lang),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = LifeStrings.memosCounts(lang, state.memos.size, pinned),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            if (state.memos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyHint(
                        emoji = "📝",
                        title = LifeStrings.memosEmpty(lang),
                        subtitle = LifeStrings.memosEmptyHint(lang),
                        modifier = Modifier.padding(bottom = 40.dp)
                    )
                }
            } else {
                Text(
                    text = LifeStrings.memosHint(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                // 置顶排在前面这件事由 DAO 的排序保证（pinned DESC, updatedAt DESC），
                // 这里不再自己排一遍。
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    verticalItemSpacing = 10.dp,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(items = state.memos, key = { it.id }) { memo ->
                        MemoCard(
                            memo = memo,
                            onOpen = { reading = memo },
                            onPin = { vm.toggleMemoPinned(memo) },
                            onEdit = { editing = memo },
                            onDelete = { deleting = memo }
                        )
                    }
                }
            }
        }

        FilledIconButton(
            onClick = { composing = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = LifeStrings.memoAdd(lang))
        }
    }

    if (composing) {
        MemoDialog(
            memo = null,
            onDismiss = { composing = false },
            onSave = { title, content ->
                vm.addMemo(title, content)
                composing = false
            }
        )
    }

    editing?.let { memo ->
        MemoDialog(
            memo = memo,
            onDismiss = { editing = null },
            onSave = { title, content ->
                vm.updateMemo(memo.copy(title = title, content = content))
                editing = null
            }
        )
    }

    reading?.let { memo ->
        MemoReaderDialog(memo = memo, onDismiss = { reading = null })
    }

    deleting?.let { memo ->
        ConfirmDialog(
            title = LifeStrings.memoDeleteTitle(lang, memo.title),
            text = LifeStrings.memoDeleteText(lang),
            confirmText = AppStrings.delete(lang),
            onConfirm = { vm.deleteMemo(memo) },
            onDismiss = { deleting = null }
        )
    }
}

/** 一张备忘卡片：标题、两行预览、更新时间；右上角 ⋮ 里是置顶 / 编辑 / 删除 */
@Composable
private fun MemoCard(
    memo: MemoEntity,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val lang = LocalLang.current
    var menu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .background(MaterialTheme.colorScheme.surface, Shapes.card)
            .clickable { onOpen() }
            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = memo.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            )
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = AppStrings.manage(lang),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(if (memo.pinned) LifeStrings.memoUnpin(lang) else LifeStrings.memoPin(lang))
                        },
                        onClick = {
                            menu = false
                            onPin()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(AppStrings.edit(lang)) },
                        onClick = {
                            menu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(AppStrings.delete(lang)) },
                        onClick = {
                            menu = false
                            onDelete()
                        }
                    )
                }
            }
        }

        if (memo.pinned) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = LifeStrings.memoPinnedBadge(lang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (memo.content.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = memo.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 10.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = memoFooter(memo, lang),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 10.dp)
        )
    }
}

/** 「更新于 9月13日 周六」——日期格式复用 AppStrings，四语自动切换 */
private fun memoFooter(memo: MemoEntity, lang: Lang): String {
    val date = memo.updatedAt.toLocalDate()
    val day = AppStrings.monthDay(lang, date.monthValue, date.dayOfMonth)
    val week = AppStrings.weekday(lang, date.dayOfWeek.value - 1)
    return LifeStrings.memoUpdatedAt(lang) + " " + day + " " + week
}

/** 新建 / 编辑备忘：标题 + 多行正文 */
@Composable
private fun MemoDialog(
    memo: MemoEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit
) {
    val lang = LocalLang.current
    var title by remember { mutableStateOf(memo?.title.orEmpty()) }
    var content by remember { mutableStateOf(memo?.content.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (memo == null) LifeStrings.memoAdd(lang) else LifeStrings.memoEdit(lang)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text(LifeStrings.memoFieldTitle(lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 4000) content = it },
                    label = { Text(LifeStrings.memoFieldContent(lang) + " · " + AppStrings.optional(lang)) },
                    minLines = 4,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onSave(title.trim(), content.trim()) }
            ) { Text(AppStrings.save(lang)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel(lang)) }
        }
    )
}

/**
 * 就地看全文（替代还没实现的 MemoDetailScreen）。
 * 不做滚动容器嵌套：正文整块放在可滚动列里，标题在弹窗头部不跟着滚。
 */
@Composable
private fun MemoReaderDialog(memo: MemoEntity, onDismiss: () -> Unit) {
    val lang = LocalLang.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(memo.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = memoFooter(memo, lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Text(
                text = memo.content.ifBlank { AppStrings.notSet(lang) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.confirm(lang)) }
        }
    )
}
