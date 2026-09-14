# 日常本 DailyBook v1.2

**记账 + 待办 + 专注计时** 三合一安卓应用。
Kotlin + Jetpack Compose + Room，纯离线，只申请通知与震动两个权限。

## 功能一览

| 标签页 | 功能 |
| --- | --- |
| **记账** | 支出/收入、17 个预置分类（彩色 emoji 图标）、备注、日期（快捷 + 日历）、按月分组流水、月份切换、**搜索**、**月度预算进度条（超支标红）**、**点击记录编辑**、删除 |
| **待办** | 快速添加、完成勾选、星标重要、到期日与逾期标红、状态筛选、**搜索**、**设为专注目标**、**一键清除已完成** |
| **专注** | 专注/短休息/长休息三阶段圆环计时、开始/暂停/重置/跳过、每 N 个番茄长休息、结束通知 + 震动、可自动进入下一阶段、显示当前专注目标、计时中屏幕常亮 |
| **统计** | 记账：结余、分类占比、每日支出、收入来源；专注：今日/连续/累计、最近 7 天、**今日专注明细（时段列表）** |
| **设置** | 主题（跟随系统/浅色/深色）、动态取色、**月度预算**、各阶段时长、长休息间隔、震动、自动开始、屏幕常亮、分项清除数据 |

## 技术栈

| 项目 | 版本 |
| --- | --- |
| Kotlin | 2.0.21 |
| AGP / Gradle / JDK | 8.7.3 / 8.13 / 21 |
| Compose | BOM 2024.10.01 + Material 3 |
| Room | 2.6.1（KSP 2.0.21-1.0.25），数据库 version 2 |
| DataStore | 1.1.1（专注计时设置） |
| SharedPreferences | 主题 / 预算 / 专注目标 |
| SDK | compileSdk 35 · targetSdk 35 · minSdk 26（Android 8.0+） |

## 工程结构

```
app/src/main/java/com/dailybook/app/
├── MainActivity.kt              入口 + 响应式导航（宽屏走 NavigationRail）
├── MainViewModel.kt             记账/待办/专注统计的聚合状态（UiState）
├── data/
│   ├── Entities.kt              Room 实体（交易 / 待办）+ 预置分类
│   ├── FocusSession.kt          Room 实体 + DAO（专注记录）
│   ├── Daos.kt                  交易 DAO / 待办 DAO
│   ├── AppDatabase.kt           数据库单例 + v1→v2 迁移
│   ├── DailyRepository.kt       记账 / 待办 / 专注记录的数据入口
│   ├── FocusRepository.kt       专注计时设置（DataStore）
│   └── SettingsStore.kt         全局单例设置（主题/预算/专注目标）
├── timer/
│   ├── Phase.kt                 三个阶段
│   └── TimerViewModel.kt        计时状态机（时间戳驱动，跨页面不中断）
├── notify/Notifier.kt           通知渠道 + 震动
├── ui/
│   ├── Components.kt            公共组件（卡片/指标/空状态/搜索/滑块/换行标签…）
│   ├── LedgerScreen.kt          记账页 + 记一笔/编辑弹窗
│   ├── TodoScreen.kt            待办页 + 编辑弹窗
│   ├── TimerScreen.kt           专注页
│   ├── StatsScreen.kt           统计页
│   ├── SettingsScreen.kt        设置页
│   ├── RingTimer.kt             圆环进度（自适应尺寸 + 渐变 + 动画）
│   └── theme/                   配色 / 主题 / 字体
└── util/Format.kt               金额与日期格式化、解析
```

## 构建

```powershell
$env:JAVA_HOME   = "C:\Program Files\Java\jdk-21.0.12.1"
$env:ANDROID_HOME = "C:\Users\hjc20\AppData\Local\Android\Sdk"
cd "C:\Users\hjc20\Documents\DeepSeek Desktop\apk\dailybook"
.\gradlew.bat assembleRelease     # 正式签名版（R8，约 1.6 MB）
.\gradlew.bat assembleDebug       # 调试版
.\gradlew.bat lintRelease         # 静态检查
```

已构建产物在 `..\dist\`，打包件 `日常本v1.2-三合一.zip`。

## 设计要点

- **单一数据源**：专注统计（今日/连续/累计）全部从 `focus_sessions` 表实时派生，
  不再另存一份计数；主题/预算/专注目标只有一份全局单例设置。
- **数据库迁移**：v1→v2 用 `Migration` 新增 `focus_sessions` 表，
  老用户的记账与待办数据完整保留（迁移 SQL 已用 SQLite 验证结构一致）。
- **多尺寸适配**：分类标签走 `FlowRow` 自动换行；圆环尺寸由 `BoxWithConstraints`
  按可用空间计算；≥600dp 自动切换侧边导航并限宽 720dp；正文区域用最小高度约束，
  系统字体放大时不裁切文字。
- **计时不受重组影响**：倒计时基于 `SystemClock.elapsedRealtime()` 时间戳，
  切页面、转屏都不会走偏。计时跑在 ViewModel 中（未用前台 Service），
  进程被系统杀死会中断。

## 已知取舍

- 未做真机运行验证（构建机没有连接安卓设备）：
  已验证编译通过、Lint 零问题、APK 结构与签名正常、数据库迁移 SQL 结构正确。
- 待办不支持重复任务与到点提醒通知；记账不支持多账户、导出 CSV。
- 从 v1.1 升级时，v1.1 若曾用 DataStore 记过专注次数，那部分历史计数不会迁移
  （v1.2 起专注统计以记录表为准）。
