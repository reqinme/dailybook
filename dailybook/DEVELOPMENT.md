# 日常本 DailyBook v1.3

**记账 + 待办 + 专注计时** 三合一安卓应用。
Kotlin + Jetpack Compose + Room，纯离线，只申请通知、震动与开机重排提醒三个权限。

## 功能一览

| 标签页 | 功能 |
| --- | --- |
| **记账** | 支出/收入、17 个预置分类（彩色 emoji 图标）、备注、日期（快捷 + 日历）、按月分组流水、月份切换、**搜索**、**月度预算进度条（超支标红）**、**点击记录编辑**、**导出 CSV（UTF-8 BOM + CRLF，Excel 直接打开不乱码）**、删除 |
| **待办** | 快速添加、完成勾选、星标重要、到期日与逾期标红、**重复任务（每天/每周/每月，勾完自动顺延出下一条）**、**到点提醒（到期日 9:00 通知，未完成次日再提醒一次）**、状态筛选、**搜索**、**设为专注目标**、**一键清除已完成** |
| **专注** | 专注/短休息/长休息三阶段圆环计时、开始/暂停/重置/跳过、每 N 个番茄长休息、结束通知 + 震动、可自动进入下一阶段、显示当前专注目标、计时中屏幕常亮 |
| **统计** | 记账：结余、分类占比、每日支出、收入来源、**近 12 个月收支趋势图**、**年度汇总（年支出/年收入/年结余、笔数、花得最多的分类）**、**累计收支与净结余**；专注：今日/连续/累计、最近 7 天、**今日专注明细（时段列表）** |
| **设置** | 主题（跟随系统/浅色/深色）、动态取色、**月度预算**、各阶段时长、长休息间隔、震动、自动开始、屏幕常亮、**备份与恢复（JSON）、导出记账 CSV**、分项清除数据 |

## 技术栈

| 项目 | 版本 |
| --- | --- |
| Kotlin | 2.0.21 |
| AGP / Gradle / JDK | 8.7.3 / 8.13 / 21 |
| Compose | BOM 2024.10.01 + Material 3 |
| Room | 2.6.1（KSP 2.0.21-1.0.25），数据库 version 3（MIGRATION_1_2、MIGRATION_2_3） |
| DataStore | 1.1.1（专注计时设置） |
| SharedPreferences | 主题 / 预算 / 专注目标 / 已提醒记录 |
| JUnit | 4.13.2（`testImplementation`，15 个 JVM 单元测试） |
| SDK | compileSdk 35 · targetSdk 35 · minSdk 26（Android 8.0+） |
| 版本 | versionCode 4 · versionName 1.3，正式版 APK 约 1.6 MB |

## 工程结构

```
app/src/main/java/com/dailybook/app/
├── MainActivity.kt              入口 + 响应式导航（宽屏走 NavigationRail）
├── MainViewModel.kt             记账/待办/专注统计的聚合状态（UiState）
├── data/
│   ├── Entities.kt              Room 实体（交易 / 待办 + RepeatRule 重复规则与日期推算）+ 预置分类
│   ├── FocusSession.kt          Room 实体 + DAO（专注记录）
│   ├── Daos.kt                  交易 DAO / 待办 DAO
│   ├── AppDatabase.kt           数据库单例 + v1→v2、v2→v3 迁移
│   ├── DailyRepository.kt       记账 / 待办 / 专注记录的数据入口（含整体导入导出用的快照）
│   ├── FocusRepository.kt       专注计时设置（DataStore）
│   └── SettingsStore.kt         全局单例设置（主题/预算/专注目标/已提醒记录）
├── backup/Backup.kt             备份 JSON 的读写 + 记账 CSV 导出（org.json，无第三方依赖）
├── timer/
│   ├── Phase.kt                 三个阶段
│   └── TimerViewModel.kt        计时状态机（时间戳驱动，跨页面不中断）
├── notify/
│   ├── Notifier.kt              通知渠道 + 震动
│   ├── TodoReminder.kt          待办提醒排程（非精确闹钟，幂等重排）
│   ├── TodoReminderReceiver.kt  提醒接收器（到点重新查库再决定是否提醒）
│   └── BootReceiver.kt          重启 / 覆盖安装后重新排程提醒
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

JVM 单元测试（`app/src/test/`）：

```
app/src/test/java/com/dailybook/app/
└── RepeatAndReminderTest.kt     RepeatRuleTest / ReminderScheduleTest / CsvExportTest，共 15 个用例
```

## 构建

项目里没有 Gradle Wrapper，用系统安装的 `gradle` 直接执行：

```powershell
$env:JAVA_HOME   = "C:\Program Files\Java\jdk-21.0.12.1"
$env:ANDROID_HOME = "C:\Users\hjc20\AppData\Local\Android\Sdk"
cd "C:\Users\hjc20\Documents\DeepSeek Desktop\apk\github-upload\repo\dailybook"
gradle assembleRelease     # 正式签名版（R8，约 1.6 MB）
gradle assembleDebug       # 调试版
gradle testDebugUnitTest   # 15 个 JVM 单元测试（重复日期 / 提醒排程 / CSV 导出）
gradle lintRelease         # 静态检查
```

已构建的安装包统一收在工作区根目录的 `dist\`（即 `apk\dist\`）：

| 文件 | 说明 |
| --- | --- |
| `DailyBook-v1.3-release.apk` | 正式签名版，约 1.6 MB，**装机用这个**（签名与历史版本同源，可直接覆盖安装） |
| `DailyBook-v1.3-debug.apk` | 调试版备胎，约 17 MB，签名不同，覆盖不了正式版，需先卸载 |

构建产物本身在 `app/build/outputs/apk/{release,debug}/`。

## 设计要点

- **单一数据源**：专注统计（今日/连续/累计）全部从 `focus_sessions` 表实时派生，
  不再另存一份计数；主题/预算/专注目标只有一份全局单例设置。
- **数据库迁移**：v1→v2 用 `Migration` 新增 `focus_sessions` 表；v2→v3 再用
  `ALTER TABLE todos ADD COLUMN repeatRule TEXT NOT NULL DEFAULT 'NONE'` 补上重复规则列。
  两次迁移都在 `addMigrations(MIGRATION_1_2, MIGRATION_2_3)` 里注册，
  老用户的记账、待办、专注记录完整保留（迁移 SQL 已用 SQLite 验证结构一致）。
- **备份与恢复**：整库导出成一个 JSON 文件（记账流水 / 待办 / 专注记录 + 月度预算，
  带 `app` 与 `format` 版本字段，解析时校验并拒绝不认识的结构）。文件读写全部经由
  系统文件选择器（SAF）给的 `Uri`，所以既不需要存储权限也不碰网络；
  恢复走 `restore()`：一个事务里清空三张表再整份写入，因此先弹确认框再执行。
- **CSV 导出**：开头写 UTF-8 BOM、行尾用 CRLF、含逗号/引号/换行的单元格加引号转义，
  这样 Excel 双击打开中文不乱码，排序按日期 + id 固定。
- **重复任务日期推算**：`nextDueMillisOf()` 是纯函数（可直接跑 JVM 测试）——
  每天/每周/每月顺延，顺延结果落在过去时继续往后推，不补一串过期任务；
  月末夹取用 `plusMonths` 的自然夹取（1 月 31 日的下一个月是 2 月 28 日）。
- **待办提醒**：用 AlarmManager 的**非精确**闹钟 `setAndAllowWhileIdle`，不申请
  `SCHEDULE_EXACT_ALARM`，Android 12+ 也不用再引导用户去系统设置授权。排程是幂等的
  （先撤销上一轮再按需重排），提醒接收器到点重新查库确认还没完成才通知；
  「已提醒」以 `id:到期日` 为键存在设置里，最多打扰两次（到期日 9:00 + 次日 9:00），
  改期后键变了会重新提醒。重启或覆盖安装后由 `BootReceiver` 重新排一遍。
- **多尺寸适配**：分类标签走 `FlowRow` 自动换行；圆环尺寸由 `BoxWithConstraints`
  按可用空间计算；≥600dp 自动切换侧边导航并限宽 720dp；正文区域用最小高度约束，
  系统字体放大时不裁切文字。
- **计时不受重组影响**：倒计时基于 `SystemClock.elapsedRealtime()` 时间戳，
  切页面、转屏都不会走偏。计时跑在 ViewModel 中（未用前台 Service），
  进程被系统杀死会中断。

## 单元测试

`app/src/test/java/com/dailybook/app/RepeatAndReminderTest.kt` 共 15 个用例，跑在 JVM 上
（不需要设备或模拟器），用 JUnit 4 的 `org.junit.Test` + `org.junit.Assert`：

| 测试类 | 覆盖内容 |
| --- | --- |
| `RepeatRuleTest` | 不重复返回 null、每天/每周/每月顺延、长期未打开只顺延出下一条、逾期的每周任务仍落在未来、月末夹取（1 月 31 日 → 2 月 28 日） |
| `ReminderScheduleTest` | 未来到期排 9:00、当天 9:00 之后立即提醒、逾期补提醒一次、第二次排在次日 9:00、两次之后返回 null、已提醒过的未来任务不重复排 |
| `CsvExportTest` | BOM 与表头、行内容、含逗号/引号的单元格转义、按日期排序 |

跑法：`gradle testDebugUnitTest`（依赖 `testImplementation(libs.junit)` = JUnit 4.13.2）。

## 已知取舍

- 未做真机运行验证（构建机没有连接安卓设备）：
  已验证编译通过、15 个单元测试全部通过、Lint 零问题、APK 结构与签名正常、
  数据库迁移 SQL 结构正确。
- 记账支持导出 CSV，但**不支持导入 CSV**（只有 JSON 备份可以整份恢复）；
  仍不支持多账户、多币种。
- 待办提醒是**非精确**闹钟，9:00 的通知可能晚几分钟；只提醒两次，
  不做「稍后提醒」的交互。
- 从 v1.1 升级时，v1.1 若曾用 DataStore 记过专注次数，那部分历史计数不会迁移
  （v1.2 起专注统计以记录表为准）。
