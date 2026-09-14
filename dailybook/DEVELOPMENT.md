# 日常本 DailyBook v1.4

**记账 + 待办 + 专注计时** 三合一安卓应用。
Kotlin + Jetpack Compose + Room，纯离线，只申请通知、震动与开机重排提醒三个权限。

## 功能一览

| 标签页 | 功能 |
| --- | --- |
| **记账** | 支出/收入、17 个预置分类（彩色 emoji 图标）、**多账户（现金/微信/支付宝/银行卡/其他，记录弹窗里还能自己输入账户名；非现金的记录会在流水里标出账户）**、**账户筛选胶囊（用到 2 个以上账户时才出现）**、备注、日期（快捷 + 日历）、按月分组流水、月份切换、**搜索**、**月度预算进度条（超支标红）**、**每晚记账提醒（默认 21:00，时间可改）**、**点击记录编辑**、**导出 CSV（UTF-8 BOM + CRLF，Excel 直接打开不乱码）**、删除 |
| **待办** | 快速添加、完成勾选、星标重要、到期日与逾期标红、**重复任务（每天/每周/每月，勾完自动顺延出下一条）**、**到点提醒（到期日 9:00 通知，未完成次日再提醒一次；点击通知直接打开应用）**、状态筛选、**搜索**、**设为专注目标**、**一键清除已完成** |
| **专注** | 专注/短休息/长休息三阶段圆环计时、开始/暂停/重置/跳过、每 N 个番茄长休息、结束通知 + 震动、可自动进入下一阶段、显示当前专注目标、计时中屏幕常亮 |
| **统计** | 记账：结余、分类占比、每日支出、收入来源、**账户支出分布**、**分类预算进度条（超支标红）**、**近 12 个月收支趋势图**、**年度汇总（年支出/年收入/年结余、笔数、花得最多的分类）**、**累计收支与净结余**；专注：今日/连续/累计、最近 7 天、**近 12 周专注热力图（一列一周，周一到周日，颜色越深当天专注越久）**、**本周次数 / 本周时长 / 本月时长与本月次数**、**今日专注明细（时段列表）** |
| **设置** | 主题（跟随系统/浅色/深色）、动态取色、**记账分区：月度预算、分类预算（「管理」弹窗逐项设置）、每晚记账提醒开关 + 提醒时间（TimePicker）**、各阶段时长、长休息间隔、震动、自动开始、屏幕常亮、**备份与恢复（JSON）、导出记账 CSV**、分项清除数据 |

另外还有**桌面快捷方式**：长按启动器图标可直接「记一笔 / 加待办 / 开始专注」，点进去就落在对应标签页。

## 技术栈

| 项目 | 版本 |
| --- | --- |
| Kotlin | 2.0.21 |
| AGP / Gradle / JDK | 8.7.3 / 8.13 / 21 |
| Compose | BOM 2024.10.01 + Material 3 |
| Room | 2.6.1（KSP 2.0.21-1.0.25），数据库 version 4（MIGRATION_1_2、MIGRATION_2_3、MIGRATION_3_4） |
| DataStore | 1.1.1（专注计时设置） |
| SharedPreferences | 主题 / 月度预算 / 分类预算 / 专注目标 / 每晚提醒 / 已提醒记录 |
| JUnit | 4.13.2（`testImplementation`，19 个 JVM 单元测试） |
| SDK | compileSdk 35 · targetSdk 35 · minSdk 26（Android 8.0+） |
| 版本 | versionCode 5 · versionName 1.4，正式版 APK 约 1.6 MB |

权限只有三个：`VIBRATE`、`POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`。**没有网络权限，也没有存储权限**
（备份与 CSV 都走系统文件选择器）。

## 工程结构

```
app/src/main/java/com/dailybook/app/
├── MainActivity.kt              入口 + 响应式导航（宽屏走 NavigationRail）+ 桌面快捷方式 Intent 处理（singleTop）
├── MainViewModel.kt             记账/待办/专注统计的聚合状态（UiState）
├── data/
│   ├── Entities.kt              Room 实体（交易 / 待办 + RepeatRule 重复规则与日期推算）+ 预置分类 + Accounts 账户预置
│   ├── FocusSession.kt          Room 实体 + DAO（专注记录）
│   ├── Daos.kt                  交易 DAO / 待办 DAO
│   ├── AppDatabase.kt           数据库单例 + v1→v2、v2→v3、v3→v4 迁移
│   ├── DailyRepository.kt       记账 / 待办 / 专注记录的数据入口（含整体导入导出用的快照）
│   ├── FocusRepository.kt       专注计时设置（DataStore）
│   └── SettingsStore.kt         全局单例设置（主题/月度预算/分类预算/专注目标/每晚提醒/已提醒记录）
├── backup/Backup.kt             备份 JSON 的读写 + 记账 CSV 导出（org.json，无第三方依赖）
├── timer/
│   ├── Phase.kt                 三个阶段
│   └── TimerViewModel.kt        计时状态机（时间戳驱动，跨页面不中断）
├── notify/
│   ├── Notifier.kt              通知渠道 + 震动 + 记账提醒通知（带跳转 Intent）
│   ├── TodoReminder.kt          待办提醒排程（非精确闹钟，幂等重排）
│   ├── TodoReminderReceiver.kt  提醒接收器（到点重新查库再决定是否提醒）
│   ├── LedgerReminder.kt        每晚记账提醒排程（非精确闹钟，按设置时间重排）
│   ├── LedgerReminderReceiver.kt 记账提醒到点发通知，并顺手排下一天
│   └── BootReceiver.kt          重启 / 覆盖安装后重新排程待办与记账提醒
├── ui/
│   ├── Components.kt            公共组件（卡片/指标/空状态/搜索/滑块/换行标签…）
│   ├── LedgerScreen.kt          记账页 + 记一笔/编辑弹窗（账户选择 + 自定义账户）+ 账户筛选
│   ├── TodoScreen.kt            待办页 + 编辑弹窗
│   ├── TimerScreen.kt           专注页
│   ├── StatsScreen.kt           统计页（账户支出分布 / 分类预算 / 专注热力图与周月报告）
│   ├── SettingsScreen.kt        设置页（分类预算弹窗 + 提醒时间 TimePicker）
│   ├── RingTimer.kt             圆环进度（自适应尺寸 + 渐变 + 动画）
│   └── theme/                   配色 / 主题 / 字体
└── util/Format.kt               金额与日期格式化、解析

app/src/main/res/xml/
└── shortcuts.xml                桌面快捷方式定义（记一笔 / 加待办 / 开始专注，各带一个标签页编号）
```

JVM 单元测试（`app/src/test/`）：

```
app/src/test/java/com/dailybook/app/
└── RepeatAndReminderTest.kt     RepeatRuleTest / ReminderScheduleTest / LedgerReminderTimeTest / CsvExportTest，共 19 个用例
```

## 构建

项目里没有 Gradle Wrapper，用系统安装的 `gradle` 直接执行：

```powershell
$env:JAVA_HOME   = "C:\Program Files\Java\jdk-21.0.12.1"
$env:ANDROID_HOME = "C:\Users\hjc20\AppData\Local\Android\Sdk"
cd "C:\Users\hjc20\Documents\DeepSeek Desktop\apk\github-upload\repo\dailybook"
gradle assembleRelease     # 正式签名版（R8，约 1.6 MB）
gradle assembleDebug       # 调试版
gradle testDebugUnitTest   # 19 个 JVM 单元测试（重复日期 / 待办提醒 / 记账提醒时间 / CSV 导出）
gradle lintRelease         # 静态检查
```

已构建的安装包统一收在工作区根目录的 `dist\`（即 `apk\dist\`）：

| 文件 | 说明 |
| --- | --- |
| `DailyBook-v1.4-release.apk` | 正式签名版，约 1.6 MB，**装机用这个**（签名与历史版本同源，可直接覆盖安装） |
| `DailyBook-v1.4-debug.apk` | 调试版备胎，约 17 MB，签名不同，覆盖不了正式版，需先卸载 |
| `DailyBook-v1.3-release.zip` | 上一版（v1.3）正式版的归档，留档备用 |

构建产物本身在 `app/build/outputs/apk/{release,debug}/`。

## 设计要点

- **单一数据源**：专注统计（今日/连续/累计、本周/本月、热力图）全部从 `focus_sessions` 表实时派生，
  不再另存一份计数；主题/月度预算/分类预算/专注目标只有一份全局单例设置。
- **多账户**：交易表的 `account` 列存账户名（字符串，不建外键也不建单独的表，方便用户随手加一个）。
  `Accounts.PRESETS` 是「现金 / 微信 / 支付宝 / 银行卡 / 其他」五个预置项，记录弹窗在此基础上把
  「历史用过的账户 ∪ 当前值」并进去，所以自己输入过的账户下次直接能点。
  现金是默认账户（`Accounts.DEFAULT`），列表里只在账户不是现金时才显示徽标；
  顶部筛选胶囊只有在历史数据里出现过 2 个以上账户时才显示，避免单账户用户看到无用控件。
- **分类预算**：分类上限存在设置里（`Map<分类, 分>`），和整月预算并存、互不干扰——
  月预算管「这个月总共花多少」，分类预算管「这个分类这个月花多少」。
  统计页把当月支出按分类聚合后算出进度与是否超支，超支的进度条走红色；
  分类预算只对支出分类有意义，收入不受影响。
- **数据库迁移**：v1→v2 用 `Migration` 新增 `focus_sessions` 表；v2→v3 用
  `ALTER TABLE todos ADD COLUMN repeatRule TEXT NOT NULL DEFAULT 'NONE'` 补上重复规则列；
  v3→v4 再用 `ALTER TABLE transactions ADD COLUMN account TEXT NOT NULL DEFAULT '现金'` 补上账户列。
  三次迁移都在 `addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)` 里注册，
  老用户的记账、待办、专注记录完整保留（迁移 SQL 已用 SQLite 验证结构一致），
  老数据因为带了默认值，自动全归到「现金」账户。
- **备份与恢复**：整库导出成一个 JSON 文件（记账流水 / 待办 / 专注记录 + 月度预算 + 分类预算，
  带 `app` 与 `format` 版本字段，当前 `format = 2`，v1.4 起记账多了 `account`、待办多了 `repeatRule`；
  解析时校验并拒绝不认识的结构）。`format` 比当前新会直接拒绝，比当前旧则逐字段回退到默认值
  （账户回退成「现金」、重复规则回退成 `NONE`），所以旧版本导出的备份照样能恢复。
  文件读写全部经由系统文件选择器（SAF）给的 `Uri`，所以既不需要存储权限也不碰网络；
  恢复走 `restore()`：一个事务里清空三张表再整份写入，因此先弹确认框再执行。
- **CSV 导出**：开头写 UTF-8 BOM、行尾用 CRLF、含逗号/引号/换行的单元格加引号转义，
  这样 Excel 双击打开中文不乱码，排序按日期 + id 固定。表头固定为
  `日期,类型,分类,金额,备注`——**不含账户列**，所以要按账户对账得用 JSON 备份。
- **重复任务日期推算**：`nextDueMillisOf()` 是纯函数（可直接跑 JVM 测试）——
  每天/每周/每月顺延，顺延结果落在过去时继续往后推，不补一串过期任务；
  月末夹取用 `plusMonths` 的自然夹取（1 月 31 日的下一个月是 2 月 28 日）。
- **待办提醒**：用 AlarmManager 的**非精确**闹钟 `setAndAllowWhileIdle`，不申请
  `SCHEDULE_EXACT_ALARM`，Android 12+ 也不用再引导用户去系统设置授权。排程是幂等的
  （先撤销上一轮再按需重排），提醒接收器到点重新查库确认还没完成才通知；
  「已提醒」以 `id:到期日` 为键存在设置里，最多打扰两次（到期日 9:00 + 次日 9:00），
  改期后键变了会重新提醒；通知带跳转 Intent，点一下直接回到应用。
- **每晚记账提醒**：`LedgerReminder` 和待办提醒共用同一套做法（`setAndAllowWhileIdle`、
  同一个幂等 `sync()`），只是时间由设置决定（默认 21:00，可在「设置 → 记账 → 提醒时间」用
  TimePicker 改成任意时刻），`nextTriggerMillis()` 是纯函数：今天的点还没到就排今天，过了就排明天，
  因此有 4 个 JVM 用例专门覆盖它。开关或时间一变、App 启动、重启 / 覆盖安装后、以及每次提醒触发后都会重排，
  而且每次都按**当时的系统时间**重新算触发点，所以手机时间或时区被改过之后，
  最迟到下一次重排（触发一次、重启、或打开应用）就会自动回到用户设置的钟点上，不必手动关开开关。
  `LedgerReminderReceiver` 发完通知立刻排下一天，不依赖 App 常驻。
- **桌面快捷方式**：`res/xml/shortcuts.xml` 声明「记一笔 / 加待办 / 开始专注」三个静态快捷方式，
  各自往 `MainActivity` 塞一个 `tab` 编号；Activity 是 `launchMode="singleTop"`，
  `onCreate` 与 `onNewIntent` 都走同一个 `consumeShortcut()`，并用一个自增的 `requestSeq`
  让「连续点同一个快捷方式」也能重新切回那个标签页，而不是没反应。
- **通知跳转**：待办提醒与每晚记账提醒的通知都带 `PendingIntent` 指向 `MainActivity`，
  点通知即可回到应用（`POST_NOTIFICATIONS` 之外的权限一律不申请）。
- **多尺寸适配**：分类标签走 `FlowRow` 自动换行；圆环尺寸由 `BoxWithConstraints`
  按可用空间计算；≥600dp 自动切换侧边导航并限宽 720dp；正文区域用最小高度约束，
  系统字体放大时不裁切文字。
- **计时不受重组影响**：倒计时基于 `SystemClock.elapsedRealtime()` 时间戳，
  切页面、转屏都不会走偏。计时跑在 ViewModel 中（未用前台 Service），
  进程被系统杀死会中断。

## 单元测试

`app/src/test/java/com/dailybook/app/RepeatAndReminderTest.kt` 共 19 个用例，跑在 JVM 上
（不需要设备或模拟器），用 JUnit 4 的 `org.junit.Test` + `org.junit.Assert`：

| 测试类 | 覆盖内容 |
| --- | --- |
| `RepeatRuleTest`（6） | 不重复返回 null、每天/每周/每月顺延、长期未打开只顺延出下一条、逾期的每周任务仍落在未来、月末夹取（1 月 31 日 → 2 月 28 日） |
| `ReminderScheduleTest`（6） | 未来到期排 9:00、当天 9:00 之后立即提醒、逾期补提醒一次、第二次排在次日 9:00、两次之后返回 null、已提醒过的未来任务不重复排 |
| `LedgerReminderTimeTest`（4） | 提醒点还没到排今天、已经过了排明天、正好卡在提醒时刻顺延到明天、自定义时间（7:30）按设置排 |
| `CsvExportTest`（3） | BOM 与表头、行内容、含逗号/引号的单元格转义、按日期排序 |

跑法：`gradle testDebugUnitTest`（依赖 `testImplementation(libs.junit)` = JUnit 4.13.2）。

## 已知取舍

- 未做真机运行验证（构建机没有连接安卓设备）：
  已验证编译通过、19 个单元测试全部通过、Lint 零问题、APK 结构与签名正常、
  数据库迁移 SQL 结构正确。
- 记账支持导出 CSV，但**不支持导入 CSV**（只有 JSON 备份可以整份恢复），
  而且 CSV 表头里没有账户列；账户只是一个名字标签，**没有账户间转账、没有账户余额、也没有多币种**，
  金额一律按人民币展示。
- 分类预算只对支出分类生效，且只按月计；它按分类名存在设置里，和分类数据不强绑定——
  把某个分类的记录全删了，预算条目依然留着，要去「设置 → 记账 → 分类预算 → 管理」里手动清掉。
- 待办提醒和每晚记账提醒都是**非精确**闹钟，9:00 / 21:00 的通知可能晚几分钟；
  待办只提醒两次，不做「稍后提醒」的交互；每晚提醒每天一条，不会判断「今天是不是已经记过了」。
- 桌面快捷方式只能跳到标签页，不会自动把「记一笔 / 加待办」的弹窗也一并打开。
- 从 v1.1 升级时，v1.1 若曾用 DataStore 记过专注次数，那部分历史计数不会迁移
  （v1.2 起专注统计以记录表为准）。
