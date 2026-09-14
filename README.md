# DailyBook · FocusFlow

**English** | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md)

Offline Android apps built with Kotlin + Jetpack Compose.
**No internet permission — your data never leaves your phone.**

---

## What it is

**DailyBook** puts three everyday tools into one app: a ledger, a to-do list and a pomodoro
timer. Five tabs at the bottom: **Ledger · Todos · Focus · Stats · Settings**.

| Tab | What it does |
| --- | --- |
| 📒 **Ledger** | Income & expense, preset categories, notes, date picker, monthly flow grouped by day, month switcher, category breakdown, daily spending bar chart, **monthly budget with a progress bar**, **search**, and **tap any record to edit it** |
| ✅ **Todos** | Quick add, check off, star as important, due date with overdue highlighting, status filter, **search**, clear all completed |
| ⏱️ **Focus** | Pomodoro timer: focus / short break / long break, ring progress with gradient, notification + vibration when a phase ends, optional auto-advance, long-break every N pomodoros, **today's session log** |
| 📊 **Stats** | Pomodoros today, focus streak, last 7 days chart, ledger totals |
| ⚙️ **Settings** | Timer lengths, vibration, keep screen on, theme, and data clearing |

Also included: category badges with colour and emoji, tablet / landscape layout with a side
navigation rail, and a Chinese / English interface.

**FocusFlow** is the earlier standalone pomodoro app, kept for anyone who only wants a timer.

---

## Installation requirements

| Item | Requirement |
| --- | --- |
| **Android version** | **8.0 (API 26) or newer.** Built against Android 15 (API 35) |
| **APK size** | About 1.6 MB |
| **Installing** | The app is self-signed (not distributed through a store), so you must allow *"install apps from unknown sources"* in system settings |
| **Permissions** | Notifications and vibration — that is all |
| **Network** | **No internet permission at all.** Nothing is uploaded anywhere |
| **Your data** | Stored only on your device. Uninstalling the app deletes it. Upgrading keeps it — the database migrates automatically |
| **Upgrading** | All versions share the same package name and signing key, so you can install a newer APK straight over an older one |
| **Known limits** | Only release APKs are signed; debug builds use a different signature and cannot be installed over a release build. The timer stops counting if the system kills the app in the background |
