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
| 📒 **Ledger** | Income & expense, preset categories, notes, date picker, **every record belongs to an account — cash / WeChat / Alipay / bank card / other, or a name you type yourself — with an account filter chip row and the account shown on each row**, monthly flow grouped by day, month switcher, category breakdown, daily spending bar chart, **monthly budget with a progress bar**, **a monthly budget per category**, **search**, **tap any record to edit it**, **a nightly reminder to write down the day's records**, and **CSV export that Excel opens correctly** |
| ✅ **Todos** | Quick add, check off, star as important, due date with overdue highlighting, **repeating tasks (every day / week / month) that create the next occurrence when you tick one off**, **a reminder on the due date that opens the app when you tap it**, status filter, **search**, clear all completed |
| ⏱️ **Focus** | Pomodoro timer: focus / short break / long break, ring progress with gradient, notification + vibration when a phase ends, optional auto-advance, long-break every N pomodoros, **today's session log** |
| 📊 **Stats** | Pomodoros today, focus streak, last 7 days chart, **a 12-week focus heatmap (one column per week, Monday to Sunday, deeper colour for more focus minutes) with this week's count and minutes and this month's count and minutes**, ledger totals, **a 12-month income / expense trend chart**, **this year's summary** (year expense, income, balance, record count, top spending category), **an account spending breakdown**, **per-category budget progress bars with over-budget categories in red** and **all-time totals with a net balance** |
| ⚙️ **Settings** | Timer lengths, vibration, keep screen on, theme, **a monthly budget per category**, **the nightly ledger reminder with a time you pick**, **backup & restore**, **CSV export**, and data clearing |

Also included: category badges with colour and emoji, **launcher shortcuts (long-press the app
icon to jump straight into recording an entry, adding a to-do or starting a focus session)**,
and a tablet / landscape layout with a side navigation rail.

**Backup & restore:** export everything — transactions (with their account), todos, focus sessions
and the monthly budget, including per-category budgets — into a single JSON file, and load it back
later (for example after changing phones). The file is written to a location you pick through the
system file picker, so the app still needs no storage permission. Restoring replaces the current
data, and asks for confirmation first. Backups written by older versions still restore fine —
fields they never stored fall back to their defaults.

**FocusFlow** is the earlier standalone pomodoro app, kept for anyone who only wants a timer.

---

## Installation requirements

| Item | Requirement |
| --- | --- |
| **Android version** | **8.0 (API 26) or newer.** Built against Android 15 (API 35) |
| **APK size** | About 1.6 MB |
| **Installing** | The app is self-signed (not distributed through a store), so you must allow *"install apps from unknown sources"* in system settings |
| **Permissions** | Notifications, vibration, and rescheduling reminders after a reboot (to-dos and the nightly ledger reminder) — that is all. No storage permission: backups and CSV files are written through the system file picker |
| **Network** | **No internet permission at all.** Nothing is uploaded anywhere |
| **Your data** | Stored only on your device. Uninstalling the app deletes it. Upgrading keeps it — the database migrates automatically (now version 4) |
| **Upgrading** | All versions share the same package name and signing key, so you can install a newer APK straight over an older one. Existing records, todos and focus sessions are preserved by the automatic database migration |
| **Known limits** | Only release APKs are signed; debug builds use a different signature and cannot be installed over a release build. The timer stops counting if the system kills the app in the background. To-do reminders and the nightly ledger reminder are deliberately inexact (no exact-alarm permission), so a 09:00 or 21:00 notification may arrive a few minutes late |
