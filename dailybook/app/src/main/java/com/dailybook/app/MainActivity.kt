package com.dailybook.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailybook.app.data.SettingsStore
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.LocalLang
import com.dailybook.app.timer.TimerViewModel
import com.dailybook.app.ui.LedgerScreen
import com.dailybook.app.ui.settings.AboutScreen
import com.dailybook.app.ui.settings.SettingsCategoryScreen
import com.dailybook.app.ui.settings.SettingsHomeScreen
import com.dailybook.app.ui.settings.UpdateScreen
import com.dailybook.app.ui.StatsScreen
import com.dailybook.app.ui.TimerScreen
import com.dailybook.app.ui.TodoScreen
import androidx.compose.ui.graphics.Color
import com.dailybook.app.i18n.Lang
import com.dailybook.app.ui.AppBackground
import com.dailybook.app.ui.Navigator
import com.dailybook.app.ui.Route
import com.dailybook.app.ui.StatsDetailKind
import com.dailybook.app.ui.StatsDetailScreen
import com.dailybook.app.ui.Tab
import com.dailybook.app.ui.life.HabitsScreen
import com.dailybook.app.ui.life.ImportantDatesScreen
import com.dailybook.app.ui.life.LifeScreen
import com.dailybook.app.ui.life.MemosScreen
import com.dailybook.app.ui.life.MilestonesScreen
import com.dailybook.app.ui.settings.AboutScreen
import com.dailybook.app.ui.settings.SettingsCategoryScreen
import com.dailybook.app.ui.settings.SettingsHomeScreen
import com.dailybook.app.ui.settings.UpdateScreen
import com.dailybook.app.ui.study.AssignmentsScreen
import com.dailybook.app.ui.study.AwardsScreen
import com.dailybook.app.ui.study.CoursesScreen
import com.dailybook.app.ui.study.CreditsScreen
import com.dailybook.app.ui.study.ExamsScreen
import com.dailybook.app.ui.study.GradesScreen
import com.dailybook.app.ui.study.StudyScreen
import com.dailybook.app.ui.study.WeeklyReportScreen
import com.dailybook.app.ui.theme.DailyBookTheme

class MainActivity : ComponentActivity() {

    /** 桌面快捷方式带来的目标标签页；requestSeq 让「连续点同一个快捷方式」也能重新切过去 */
    private var requestedTab by mutableIntStateOf(0)
    private var requestSeq by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeShortcut(intent)
        val settings = SettingsStore.get(this)
        setContent {
            // 语言在这里统一提供，下面所有界面和组件都通过 LocalLang 读当前语言
            val lang by settings.lang.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalLang provides lang) {
                DailyBookApp(initialTab = requestedTab, tabRequestSeq = requestSeq)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeShortcut(intent)
    }

    private fun consumeShortcut(intent: Intent?) {
        val tab = intent?.getStringExtra(EXTRA_TAB)?.toIntOrNull() ?: return
        if (tab !in 0..4) return
        requestedTab = tab
        requestSeq++
    }

    companion object {
        /** shortcuts.xml 里写进 Intent 的标签页编号 */
        const val EXTRA_TAB = "tab"
    }
}

// 标签页与页面路由的定义都在 ui/Nav.kt（Tab 枚举 / Route / Navigator），这里不再重复定义

/** 宽屏阈值：>= 600dp（平板、横屏手机、折叠屏展开）改用侧边导航栏 */
private const val WIDE_SCREEN_DP = 600

/** 内容区域最大宽度，避免平板上文字被拉得过于松散 */
private val MAX_CONTENT_WIDTH = 720.dp

@Composable
fun DailyBookApp(
    initialTab: Int = 0,
    tabRequestSeq: Int = 0,
    vm: MainViewModel = viewModel(),
    timerVm: TimerViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val timerState by timerVm.state.collectAsStateWithLifecycle()
    val themeMode by vm.settings.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by vm.settings.dynamicColor.collectAsStateWithLifecycle()
    val palette by vm.settings.palette.collectAsStateWithLifecycle()
    val backgroundUri by vm.settings.backgroundUri.collectAsStateWithLifecycle()
    val backgroundScrim by vm.settings.backgroundScrim.collectAsStateWithLifecycle()

    val lang = LocalLang.current

    val tabs = Tab.entries
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    // 页面栈：设置与各模块子页面都推到这里，返回键逐层退回（不引 navigation 库）
    val backStack = remember { mutableStateListOf<Route>() }
    val navigator = remember(backStack) { Navigator(backStack) }
    val currentRoute = backStack.lastOrNull()
    BackHandler(enabled = backStack.isNotEmpty()) { navigator.pop() }
    val wideScreen = LocalConfiguration.current.screenWidthDp >= WIDE_SCREEN_DP

    // 从桌面快捷方式进来时切到对应标签页（连续点同一个也会重新切）
    LaunchedEffect(tabRequestSeq) {
        if (tabRequestSeq > 0 && initialTab in tabs.indices) selectedTab = initialTab
    }

    DailyBookTheme(
        themeMode = themeMode,
        palette = palette,
        dynamicColor = dynamicColor,
        // 有背景图时底与卡片半透明，图才透得出来
        seeThrough = backgroundUri.isNotBlank()
    ) {
        // Android 13+ 需要授权才能弹出「专注结束」通知
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { }
        )
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 专注计时中保持屏幕常亮
        KeepScreenOn(enabled = timerState.isRunning && timerState.settings.keepScreenOn)

        val title = currentRoute?.title(lang) ?: tabs[selectedTab].label(lang)

        Scaffold(
            // 有背景图时不要用实色底，否则会把图挡住
            containerColor = if (backgroundUri.isBlank()) {
                MaterialTheme.colorScheme.background
            } else {
                Color.Transparent
            },
            topBar = {
                AppTopBar(
                    title = title,
                    showBack = currentRoute != null,
                    onBack = { navigator.pop() },
                    onSettings = { navigator.push(Route.SettingsHome) }
                )
            },
            bottomBar = {
                // 子页面打开时收起底部栏，把整屏让给内容
                if (!wideScreen && currentRoute == null) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(tab.icon, contentDescription = tab.label(lang)) },
                                label = { Text(tab.label(lang)) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            AppBackground(uriString = backgroundUri, scrimPercent = backgroundScrim) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 宽屏用侧栏；窄屏靠底部栏（已在上面按需隐藏）
                if (wideScreen) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationRailItem(
                                selected = selectedTab == index && currentRoute == null,
                                onClick = {
                                    backStack.clear()
                                    selectedTab = index
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label(lang)) },
                                label = { Text(tab.label(lang)) }
                            )
                        }
                    }
                }
                AppContent(
                    route = currentRoute,
                    selectedTab = selectedTab,
                    state = state,
                    timerState = timerState,
                    vm = vm,
                    timerVm = timerVm,
                    navigator = navigator,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                )
            }
            }
        }
    }
}

/** 顶部栏：子页面显示返回箭头，首页显示设置齿轮 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    val lang = LocalLang.current
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = AppStrings.back(lang)
                    )
                }
            }
        },
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        actions = {
            if (!showBack) {
                IconButton(onClick = onSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = AppStrings.tabSettings(lang)
                    )
                }
            }
        }
    )
}

/** 子页面标题（顶部栏用） */
private fun Route.title(lang: Lang): String = when (this) {
    Route.SettingsHome -> AppStrings.settingsTitle(lang)
    is Route.SettingsPage -> category.label(lang)
    Route.About -> AppStrings.settingsAbout(lang)
    Route.Update -> AppStrings.checkUpdate(lang)
    Route.Habits -> AppStrings.tabHabits(lang)
    Route.Memos -> AppStrings.tabMemos(lang)
    is Route.MemoDetail -> AppStrings.tabMemos(lang)
    Route.Milestones -> AppStrings.tabMilestones(lang)
    Route.ImportantDates -> AppStrings.tabImportantDates(lang)
    Route.Courses -> AppStrings.tabCourses(lang)
    Route.Exams -> AppStrings.tabExams(lang)
    Route.Assignments -> AppStrings.tabAssignments(lang)
    Route.Grades -> AppStrings.tabGrades(lang)
    Route.Credits -> AppStrings.tabCredits(lang)
    Route.Awards -> AppStrings.tabAwards(lang)
    Route.Words -> AppStrings.tabWords(lang)
    Route.WeeklyReport -> AppStrings.tabWeeklyReport(lang)
    is Route.StatsDetail -> kind.title(lang)
}

private fun StatsDetailKind.title(lang: Lang): String = when (this) {
    StatsDetailKind.MONTH_ENTRIES -> AppStrings.statsMonthEntries(lang)
    StatsDetailKind.CATEGORY_ENTRIES -> AppStrings.statsCategoryEntries(lang)
    StatsDetailKind.DAILY_ENTRIES -> AppStrings.statsDailyEntries(lang)
    StatsDetailKind.FOCUS_SESSIONS -> AppStrings.statsFocusSessions(lang)
    StatsDetailKind.TODO_SUMMARY -> AppStrings.statsTodoSummary(lang)
}

/**
 * 子页面路由表：集中一处，方便一眼看清有哪些页面；各页面自己不管导航。
 */
@Composable
private fun ScreenHost(
    route: Route,
    state: UiState,
    vm: MainViewModel,
    timerVm: TimerViewModel,
    nav: Navigator
) {
    when (route) {
        Route.SettingsHome -> SettingsHomeScreen(state, vm, timerVm, nav)
        is Route.SettingsPage -> SettingsCategoryScreen(route.category, state, vm, timerVm, nav)
        Route.About -> AboutScreen(nav)
        Route.Update -> UpdateScreen(nav)

        Route.Habits -> HabitsScreen(state, vm, nav)
        Route.Memos -> MemosScreen(state, vm, nav)
        is Route.MemoDetail -> MemosScreen(state, vm, nav)
        Route.Milestones -> MilestonesScreen(state, vm, nav)
        Route.ImportantDates -> ImportantDatesScreen(state, vm, nav)

        Route.Courses -> CoursesScreen(state, vm, nav)
        Route.Exams -> ExamsScreen(state, vm, nav)
        Route.Assignments -> AssignmentsScreen(state, vm, nav)
        Route.Grades -> GradesScreen(state, vm, nav)
        Route.Credits -> CreditsScreen(state, vm, nav)
        Route.Awards -> AwardsScreen(state, vm, nav)
        Route.Words -> HabitsScreen(state, vm, nav)
        Route.WeeklyReport -> WeeklyReportScreen(state, vm, nav)

        is Route.StatsDetail -> StatsDetailScreen(route.kind, state, vm, nav)
    }
}

/** 内容区：子页面优先，否则渲染当前标签页；宽屏时限宽居中 */
@Composable
private fun AppContent(
    route: Route?,
    selectedTab: Int,
    state: UiState,
    timerState: com.dailybook.app.timer.TimerUiState,
    vm: MainViewModel,
    timerVm: TimerViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = MAX_CONTENT_WIDTH)
        ) {
            // 用「路由或标签」当动画 key：从标签进入子页面也有同一套淡入淡出
            AnimatedContent(
                targetState = route ?: Tab.entries[selectedTab],
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                        fadeOut(animationSpec = tween(120))
                },
                label = "screenContent"
            ) { target ->
                when (target) {
                    is Route -> ScreenHost(
                        route = target,
                        state = state,
                        vm = vm,
                        timerVm = timerVm,
                        nav = navigator
                    )

                    is Tab -> when (target) {
                        Tab.LEDGER -> LedgerScreen(state = state, vm = vm)
                        Tab.LIFE -> LifeScreen(state = state, vm = vm, nav = navigator)
                        Tab.STUDY -> StudyScreen(state = state, vm = vm, nav = navigator)
                        Tab.FOCUS -> TimerScreen(
                            state = timerState,
                            stats = state.focusStats,
                            vm = timerVm
                        )

                        Tab.STATS -> StatsScreen(state = state, vm = vm, nav = navigator)
                    }
                }
            }
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
