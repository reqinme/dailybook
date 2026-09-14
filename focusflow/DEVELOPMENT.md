# 专注时钟 FocusFlow

一个用 **Kotlin + Jetpack Compose** 写的安卓番茄钟应用。纯离线运行，不联网、不上传任何数据。

## 功能

| 模块 | 说明 |
| --- | --- |
| 计时 | 专注 / 短休息 / 长休息三阶段，圆环进度 + 渐变色外发光，开始 / 暂停 / 重置 / 跳过 |
| 番茄循环 | 每完成 N 个专注（默认 4 个）自动安排一次长休息，N 可调 |
| 统计 | 今日专注数、连续专注天数、累计专注数、最近 7 天柱状图 |
| 提醒 | 阶段结束时发通知 + 震动；可选自动开始下一阶段 |
| 设置 | 各阶段时长、长休息间隔、震动、屏幕常亮、主题（跟随系统/浅色/深色）、动态取色 |
| 持久化 | 使用 Jetpack DataStore 保存设置与统计数据，卸载前一直保留 |

## 技术栈

- Kotlin 2.0.21 / JDK 21
- Android Gradle Plugin 8.7.3 / Gradle 8.13
- Jetpack Compose（BOM 2024.10.01）+ Material 3
- DataStore Preferences 1.1.1
- compileSdk 35 / targetSdk 35 / minSdk 26（Android 8.0+）

## 工程结构

```
app/src/main/java/com/focusflow/timer/
├── MainActivity.kt              # 入口：主题、底部导航、通知权限申请、屏幕常亮
├── data/FocusRepository.kt      # DataStore 读写：设置 + 统计
├── timer/
│   ├── Phase.kt                 # 三个阶段枚举
│   └── TimerViewModel.kt        # 计时状态机（基于时间戳，不受重组/切页面影响）
├── notify/Notifier.kt           # 通知渠道 + 震动
└── ui/
    ├── RingTimer.kt             # 圆环进度组件（Canvas 绘制）
    ├── TimerScreen.kt           # 计时页
    ├── StatsScreen.kt           # 统计页
    └── SettingsScreen.kt        # 设置页
```

## 构建

### 方式一：命令行（本机已配好环境）

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"
$env:ANDROID_HOME = "C:\Users\hjc20\AppData\Local\Android\Sdk"
cd "C:\Users\hjc20\Documents\DeepSeek Desktop\apk"
.\gradlew.bat assembleRelease     # 正式签名版（R8 压缩，约 1 MB）
.\gradlew.bat assembleDebug       # 调试版（约 16 MB）
```

产物与已构建好的安装包：

| 文件 | 大小 | 说明 |
| --- | --- | --- |
| `dist/FocusFlow-v1.0-release.apk` | 1.06 MB | **推荐安装这个**，已用 `keystore/focusflow.jks` 签名，R8 压缩 |
| `dist/FocusFlow-v1.0-debug.apk` | 16.3 MB | 调试版备胎，未压缩，安装包名为同一个 `com.focusflow.timer` |

安装到手机：

```powershell
# USB 连接并开启「USB 调试」后
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r dist\FocusFlow-v1.0-release.apk
```

或者直接把 apk 拷到手机里点击安装（需要在系统设置里允许「安装未知来源应用」）。

> 两个 APK 包名相同、签名不同，**同一台手机上不要混装**（会提示签名冲突，需先卸载）。

### 方式二：Android Studio

直接 `Open` 本目录即可，Android Studio 会自行同步 Gradle。

### 签名说明

签名密钥库位于 `keystore/focusflow.jks`，口令写在 `keystore.properties`（两者都已被 `.gitignore` 排除）。
这是自用签名，后续升级安装必须继续用同一个密钥库，否则无法覆盖安装。若要上架应用商店，请换成自己的正式密钥。


## 本机环境说明

由于国内直连 `dl.google.com` 会被限速到几十 KB/s，本工程已做如下配置：

- `settings.gradle.kts`：依赖仓库优先使用阿里云镜像
  （`maven.aliyun.com/repository/google`、`/public`、`/gradle-plugin`），官方源作为兜底。
- Gradle 分发包：使用腾讯云镜像
  `https://mirrors.cloud.tencent.com/gradle/gradle-8.13-bin.zip`
- Android SDK 组件：
  - `platforms/android-35`、`platform-tools` 来自腾讯云镜像 `mirrors.cloud.tencent.com/AndroidSDK/`
  - `build-tools/35.0.0` 来自同一镜像（注意真实文件名是 `build-tools_r35_windows.zip`，下划线）
- 若用 `sdkmanager` 直接装组件，会走 `dl.google.com`，速度很慢；建议继续用镜像直链。

工具链安装位置：

| 组件 | 路径 |
| --- | --- |
| JDK 21 | `C:\Program Files\Java\jdk-21.0.12.1`（本机原有） |
| Gradle 8.13 | `C:\Users\hjc20\AndroidToolchain\gradle-8.13` |
| Android SDK | `C:\Users\hjc20\AppData\Local\Android\Sdk` |

## 已知取舍

- 计时逻辑跑在 ViewModel 中（基于 `SystemClock.elapsedRealtime()` 时间戳），
  没有做前台 Service。因此**应用进程被系统杀掉时计时会中断**，普通使用场景（切后台、锁屏）不受影响。
  如需绝对可靠（例如半小时以上不打开应用），可以再补一个前台 Service，我可以继续加。
- `minSdk 26`：为了只使用自适应图标、简化资源，未提供 Android 7 及以下的兼容图标。
