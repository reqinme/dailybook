package com.dailybook.app.ui.life

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailybook.app.MainViewModel
import com.dailybook.app.UiState
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.TodoScreen

/**
 * 「生活」标签页：待办 + 习惯打卡 + 备忘录 + 大事记 + 重要日期。
 *
 * 这五块都用顶部分段切换，而不是让用户先回首页再进子页面——
 * 它们都是每天会看的东西，来回跳层太费事。子页面（Route）里仍然可以单独打开同一个界面
 * （例如从统计或别处跳进来），两条路复用同一批 composable。
 */
@Composable
fun LifeScreen(
    state: UiState,
    vm: MainViewModel,
    nav: Navigator,
    modifier: Modifier = Modifier
) {
    val lang = LocalLang.current
    var section by rememberSaveable { mutableIntStateOf(0) }

    val titles = listOf(
        AppStrings.tabTodo(lang),
        AppStrings.tabHabits(lang),
        AppStrings.tabMemos(lang),
        AppStrings.tabMilestones(lang),
        AppStrings.tabImportantDates(lang)
    )

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = section,
            containerColor = MaterialTheme.colorScheme.background,
            edgePadding = 12.dp
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = section == index,
                    onClick = { section = index },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }

        when (section) {
            // 待办沿用原来的页面（它自己有 FAB 与底部弹层）
            0 -> TodoScreen(state = state, vm = vm)
            1 -> HabitsScreen(state = state, vm = vm, nav = nav)
            2 -> MemosScreen(state = state, vm = vm, nav = nav)
            3 -> MilestonesScreen(state = state, vm = vm, nav = nav)
            else -> ImportantDatesScreen(state = state, vm = vm, nav = nav)
        }
    }
}
