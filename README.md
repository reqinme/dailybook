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
| 📒 **Ledger** | Income & expense, preset categories, notes, date picker, monthly flow grouped by day, month switcher, category breakdown, daily spending bar chart, **monthly budget with a progress bar**, **search**, **tap any record to edit it**, and **CSV export that Excel opens correctly** |
| ✅ **Todos** | Quick add, check off, star as important, due date with overdue highlighting, **repeating tasks (every day / week / month) that create the next occurrence when you tick one off**, **a reminder on the due date**, status filter, **search**, clear all completed |
| ⏱️ **Focus** | Pomodoro timer: focus / short break / long break, ring progress with gradient, notification + vibration when a phase ends, optional auto-advance, long-break every N pomodoros, **today's session log** |
| 📊 **Stats** | Pomodoros today, focus streak, last 7 days chart, ledger totals, **a 12-month income / expense trend chart**, **this year's summary** (year expense, income, balance, record count, top spending category) and **all-time totals with a net balance** |
| ⚙️ **Settings** | Timer lengths, vibration, keep screen on, theme, **backup & restore**, **CSV export**, and data clearing |

Also included: category badges with colour and emoji, and a tablet / landscape layout with a
side navigation rail.

**Backup & restore:** export everything — transactions, todos, focus sessions and the monthly
budget — into a single JSON file, and load it back later (for example after changing phones).
The file is written to a location you pick through the system file picker, so the app still
needs no storage permission. Restoring replaces the current data, and asks for confirmation first.

**FocusFlow** is the earlier standalone pomodoro app, kept for anyone who only wants a timer.

---

## Installation requirements

| Item | Requirement |
| --- | --- |
| **Android version** | **8.0 (API 26) or newer.** Built against Android 15 (API 35) |
| **APK size** | About 1.6 MB |
| **Installing** | The app is self-signed (not distributed through a store), so you must allow *"install apps from unknown sources"* in system settings |
| **Permissions** | Notifications, vibration, and rescheduling to-do reminders after a reboot — that is all. No storage permission: backups and CSV files are written through the system file picker |
| **Network** | **No internet permission at all.** Nothing is uploaded anywhere |
| **Your data** | Stored only on your device. Uninstalling the app deletes it. Upgrading keeps it — the database migrates automatically (now version 3) |
| **Upgrading** | All versions share the same package name and signing key, so you can install a newer APK straight over an older one. Existing records, todos and focus sessions are preserved by the automatic database migration |
| **Known limits** | Only release APKs are signed; debug builds use a different signature and cannot be installed over a release build. The timer stops counting if the system kills the app in the background. To-do reminders are deliberately inexact (no exact-alarm permission), so the 09:00 notification may arrive a few minutes late |
