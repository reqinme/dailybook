# 日常本 DailyBook

**记账 + 待办**二合一安卓应用。Kotlin + Jetpack Compose + Room，纯离线，不申请任何权限。

## 功能

### 记账
- 「记一笔」底部弹窗：支出/收入切换、金额、分类（预置 10 个支出 + 7 个收入分类，带 emoji）、备注、日期（今天/昨天/前天快捷选择 + 完整日历选择器）
- 首页按**日期分组**展示流水，每天显示当日收支小计
- 月份切换（‹ ›），支持「回本月」
- 删除单条记录（带二次确认）
- 金额以**分**为单位存储（`Long`），避免浮点误差

### 待办
- 快速添加（输入框回车即添加）
- 勾选完成、标记重要（星标）、删除
- **到期日**：可设置/清除，逾期显示红色「已逾期 N 天」
- 编辑弹窗：改内容、重要标记、到期日
- 筛选：全部 / 待完成 / 已完成，并显示统计数量
- 排序：未完成优先 → 重要优先 → 有到期日的优先且按时间升序

### 统计
- 本月支出 / 收入 / 结余
- **支出分类占比**：横向占比条 + 金额 + 百分比
- **每日支出柱状图**：按当月天数展示（每 5 天标注日期）
- 收入来源占比
- 月份可切换，与记账页联动

### 设置
- 主题：跟随系统 / 浅色 / 深色
- 动态取色（Android 12+）
- 清除记账记录 / 清除待办 / 清空全部（均带二次确认）
- 关于信息

## 技术栈

| 项目 | 版本 |
| --- | --- |
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.7.3（Gradle 8.13，JDK 21） |
| Jetpack Compose | BOM 2024.10.01 + Material 3 |
| Room | 2.6.1（KSP 2.0.21-1.0.25） |
| 设置存储 | SharedPreferences（轻量，无需额外依赖） |
| SDK | compileSdk 35 / targetSdk 35 / minSdk 26 |

## 工程结构

```
app/src/main/java/com/dailybook/app/
├── MainActivity.kt            # 入口 + 底部导航（记账/待办/统计/设置）
├── MainViewModel.kt           # 唯一 ViewModel：UiState 聚合 + 所有统计计算
├── data/
│   ├── Entities.kt            # Room 实体（TransactionEntity / TodoEntity）+ 预置分类
│   ├── Daos.kt                # TransactionDao / TodoDao
│   ├── AppDatabase.kt         # Room 数据库单例
│   ├── DailyRepository.kt     # 数据操作入口
│   └── SettingsStore.kt       # SharedPreferences 设置
├── ui/
│   ├── Components.kt          # 共用组件（月份切换条）
│   ├── LedgerScreen.kt        # 记账页 + 记一笔弹窗
│   ├── TodoScreen.kt          # 待办页 + 编辑弹窗
│   ├── StatsScreen.kt         # 统计页
│   ├── SettingsScreen.kt      # 设置页
│   └── theme/                 # 配色 / 主题 / 字体
└── util/Format.kt             # 金额、日期格式化与解析
```

## 构建

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"
$env:ANDROID_HOME = "C:\Users\hjc20\AppData\Local\Android\Sdk"
cd "C:\Users\hjc20\Documents\DeepSeek Desktop\apk\dailybook"
.\gradlew.bat assembleRelease     # 正式签名版（R8 压缩）
.\gradlew.bat assembleDebug
```

已构建好的安装包在 `..\dist\`：

| 文件 | 说明 |
| --- | --- |
| `dist/DailyBook-v1.0-release.apk` | **推荐安装**，正式签名 + R8 |
| `dist/DailyBook-v1.0-debug.apk` | 调试版备胎 |

> 两个包名相同、签名不同，**同一台手机不要混装**。

## 数据说明

- 数据库文件：`/data/data/com.dailybook.app/databases/dailybook.db`（应用私有目录）
- 卸载应用会连同数据一起删除；应用内提供了清除数据的入口
- 没有任何网络权限，数据不会离开手机

## 依赖仓库

与仓库内另一个工程（FocusFlow）相同，为规避国内直连限速：

- Maven 依赖：优先 `maven.aliyun.com/repository/{google,public,gradle-plugin}`
- Gradle 分发：`mirrors.cloud.tencent.com/gradle/`

## 已知取舍

- 没有做真机运行验证（构建机未连接设备）。已验证：编译通过、Lint 零问题、APK 结构/签名/图标/入口 Activity 正常。
- 未做数据库迁移（`version = 1`）。后续若修改实体结构，需要补 `Migration` 或开启 `fallbackToDestructiveMigration`。
- 待办不支持重复任务、提醒通知；记账不支持预算、账户、导出。
