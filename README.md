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
| 📒 **Ledger** | Income & expense, preset categories, notes, date picker, **every record belongs to an account — cash / WeChat / Alipay / bank card / other, or a name you type yourself — with an account filter chip row and the account shown on each row**, monthly flow grouped by day, month switcher, category breakdown, daily spending bar chart, **monthly budget with a progress bar and an alert notification when it reaches 80% or is exceeded**, **a monthly budget per category (also with an alert when one goes over)**, **search**, **tap any record to edit it**, **a nightly reminder to write down the day's records**, **CSV export that Excel opens correctly**, **multi-currency — besides CNY you can record USD / JPY / EUR / HKD / GBP: pick a currency, enter its rate, and the app stores the converted CNY amount, with the rate remembered per currency and foreign rows showing both figures (e.g. `$12.34 ≈ ¥87.61`)**, **tags — one record can carry several (travel / reimbursement / renovation …), with a tag filter row on the ledger page**, **reimbursement — mark a record pending, then settled once it is paid back**, **custom categories (Settings → Ledger → Categories) where you can add and remove expense and income categories and one tap restores the presets**, and **CSV import that also accepts the app's own exports** |
| ✅ **Todos** | Quick add, check off, star as important, due date with overdue highlighting, **subtasks — a to-do can carry a checklist of small steps that you tick off, add and delete right in the edit sheet, with the row showing progress (e.g. `2/5`) and a thin progress bar; deleting a to-do takes its subtasks with it, so no orphan rows are left behind**, **four priority levels (low / normal / high / urgent) — set them in the edit sheet or straight from a to-do row, rows carry a marker while normal stays unmarked, and older to-dos default to normal**, **long-press a to-do and drag to reorder the list — the new order is saved and a click still opens the editor; reordering is off while a search is active, in the completed filter and in the kanban view, and the UI says so instead of silently doing nothing**, **a list / kanban view toggle — the kanban groups the same to-dos into due today / this week / later / no date / completed sections with their counts, and empty sections are hidden**, **repeating tasks (every day / week / month) that create the next occurrence when you tick one off**, **a reminder on the due date that opens the app when you tap it, with "1 hour later" and "tomorrow morning" snooze buttons on the notification**, **a "move to tomorrow" button in the edit sheet that pushes a due date one day on in one tap**, status filter, **search**, clear all completed |
| ⏱️ **Focus** | Pomodoro timer: focus / short break / long break, ring progress with gradient, notification + vibration when a phase ends, optional auto-advance, long-break every N pomodoros, **a daily pomodoro goal (1 / 2 / 3 / 4 / 6 / 8 or none), with one nudge if it is still unmet at the evening reminder time**, **today's session log**, **a count-up (stopwatch) mode where you focus as long as you like and tap Done to record that stretch**, and **sessions stopped or skipped part-way are kept and flagged as interrupted** |
| 📊 **Stats** | Pomodoros today, focus streak, last 7 days chart, **a 12-week focus heatmap (one column per week, Monday to Sunday, deeper colour for more focus minutes) with this week's count and minutes and this month's count and minutes**, ledger totals, **a 12-month income / expense trend chart**, **this year's summary** (year expense, income, balance, record count, top spending category), **an account spending breakdown**, **per-category budget progress bars with over-budget categories in red**, **today's progress towards the daily focus goal with a progress bar**, **time invested per to-do — focused minutes and session count for each to-do you started from, top 10** and **all-time totals with a net balance**, and **a reimbursement summary card with the pending total and count plus the all-time reimbursed total**, **a period comparison card — this month against last month for spending and income, and this year against last year for spending, each with a signed percentage; when there is no comparable base (nothing at all recorded last month, say) it says the amounts cannot be compared instead of showing a fake 0% or +100%**, **up to four plain-language insights — how much more or less you spent than last month and which category drove it, the biggest category this month, how many days since your last entry, and how much budget is left; a month with no data shows none of them**, **monthly report export — the current month as a shareable PNG image, a standalone HTML file (inline CSS only, no scripts and no external resources, so it reads fine offline) and a PDF (A4, as many pages as it needs), all three through the system file picker so the app still needs no storage permission; the report carries the month's figures, a bar chart, the category breakdown, focus stats and the insight lines, and a month with no data says there is nothing to export rather than writing an empty file**, with interrupted sessions marked in the log |
| ⚙️ **Settings** | **App language — 简体中文 / 繁體中文 / English / 日本語, four chips, applied instantly with no restart**, timer lengths, vibration, keep screen on, theme, **six colour schemes — 青 (Teal, the default) / 靛蓝 (Indigo) / 紫罗兰 (Violet) / 玫红 (Rose) / 琥珀 (Amber) / 森林 (Forest), each working in light and dark mode, and every one checked for contrast so none becomes unreadable in dark mode; dynamic colour still wins over the chosen scheme when it is on**, **a daily focus goal**, **a monthly budget per category**, **budget alerts on or off**, **the nightly ledger reminder with a time you pick**, **recurring entries for fixed costs — rent, subscriptions and other regular costs are set up once with an amount, type, category, account, note, a weekly or monthly rule and a first due date; due entries are written automatically, filled in when you open the app or at the evening reminder, each one pushing the next date forward so nothing is missed and nothing is recorded twice, and a long absence only back-fills the most recent occurrence instead of flooding the ledger; each rule can be paused, resumed or deleted, and the settings row shows how many rules exist and when the next one is due**, **a periodic recap — off / weekly / monthly, sent on Sunday at 20:00 or on the 1st at 10:00**, **backup & restore**, **automatic backup — pick a folder through the system file picker and the app writes the complete data as one JSON file whenever you open it or at the evening reminder, at most one file per day (same day overwrites, never a duplicate copy) and keeping the newest 7 while cleaning up older ones; it only ever deletes files it created itself (the `DailyBook-backup-` prefix), failures are reported honestly by cause (no permission / cannot create / cannot write), and a failure is never recorded as a successful backup and never shown as success**, **a 安全 (Security) card with the app lock — off until you turn it on with a 4–6 digit passcode, which is stored only as a random salt plus a SHA-256 digest, never as plaintext; full-width digits are normalised to half-width so a passcode set with full-width １２３４ still unlocks with 1234, and because the passcode lives only on this device a forgotten one cannot be recovered — clearing the app's data is the only way out, which the settings screen says outright; there is no biometric or device-credential path**, **CSV export**, **category management (add / remove expense and income categories, or restore the presets)**, **CSV import**, and data clearing |

Also included: category badges with colour and emoji, **launcher shortcuts (long-press the app
icon to jump straight into recording an entry, adding a to-do or starting a focus session)**,
and a tablet / landscape layout with a side navigation rail.

**Home-screen widget:** shows today's spending and income, how many to-dos are still open and
today's focus minutes; tapping it opens the app. It is built on the framework's `RemoteViews`,
refreshes when the app's data changes and otherwise at the 30-minute interval the system allows as a
minimum, and its text follows the system language (with the in-app language applied where the system
supports it).

**App lock, if you want one:** off by default, so nothing changes unless you turn it on. When it is
on, opening the app asks for a 4–6 digit passcode, and coming back from the background locks it
again. The passcode is stored only as a random salt plus a SHA-256 digest — never as plaintext.
Full-width digits are normalised to half-width, so a passcode set with full-width １２３４ still
unlocks with 1234 and you cannot lock yourself out with your own IME. There is no biometric or
device-credential path: it is the app's own passcode, and the lock screen draws nothing of your data
before the passcode is accepted.

**Four languages:** every screen, every notification and the CSV export come in
**简体中文 / 繁體中文 / English / 日本語**. Pick one in **Settings → Language** and it takes effect
immediately, with no restart. The launcher name and the home-screen shortcut labels follow the
**system** language instead, because they are drawn by the system rather than by the app.

**Backup & restore:** export everything — transactions (with their account, tags, currency and
reimbursement state), todos (with their subtask checklists, priority and manual order), recurring
rules, focus sessions and the monthly budget, including per-category budgets — into a single JSON
file, and load it back later (for example after changing phones).
The file is written to a location you pick through the system file picker, so the app still needs
no storage permission. Restoring replaces the current data, and asks for confirmation first.
Backups written by older versions still restore fine — fields they never stored fall back to their
defaults (empty tags, not reimbursable, CNY at rate 10000, normal priority, no subtasks).

**Automatic backup:** pick a folder through the system file picker — so still **no storage
permission** — and the app writes the complete data as one JSON file when you open it or at the
evening reminder. It keeps **at most one file per day** (the same day overwrites, so you never get a
duplicate copy such as `… (1).json`) and **keeps the newest 7**, cleaning up older ones; it only ever
deletes files it created itself (the `DailyBook-backup-` prefix), so anything else you keep in that
folder is left alone. If something goes wrong the app says so by cause (no permission / cannot
create / cannot write) instead of pretending: a failure never overwrites the "last successful
backup" record and is never shown as success.

**One CNY basis:** although you can record foreign currencies, the app stores the converted CNY
amount (the original figure is kept too and shown in the list), so **every statistic, budget and
report stays on a single CNY basis**. Importing is equally careful: **CSV import skips rows
identical to an existing record (same date, amount, type, category, account and note)**, so
importing the same file twice adds nothing, and rows it cannot parse are ignored instead of failing
the whole import.

**Fixed costs, filled in for you:** rent, subscriptions and other regular costs are set up once
(amount, type, category, account, note, a weekly or monthly rule and the first due date), and the app
writes them the moment they come due — when you open the app, or at the evening reminder. Every entry
pushes that rule's next date forward, so **nothing is missed and nothing is recorded twice**, and
coming back after a long absence back-fills only the most recent occurrence instead of flooding the
ledger. Rules can be paused, resumed or deleted at any time.

**Monthly report:** the stats page exports the current month as a **shareable PNG image**, a
**standalone HTML file** (inline CSS only — no scripts and no external resources, so it reads fine
offline) and a **PDF** (A4, as many pages as it needs). All three go through the system file picker,
so the app still needs **no storage permission**, and a month with no data says there is nothing to
export instead of writing an empty file.

**FocusFlow** is the earlier standalone pomodoro app, kept for anyone who only wants a timer.

---

## Installation requirements

| Item | Requirement |
| --- | --- |
| **Android version** | **8.0 (API 26) or newer.** Built against Android 15 (API 35) |
| **APK size** | About 1.8 MB (v1.8 is roughly the same size as v1.7: the widget uses the framework's `RemoteViews`, so nothing new was bundled) |
| **Installing** | The app is self-signed (not distributed through a store), so you must allow *"install apps from unknown sources"* in system settings |
| **Permissions** | Exactly three: notifications, vibration, and rescheduling reminders after a reboot (to-dos, the nightly ledger reminder and the periodic recap). No storage permission: backups, CSV files and monthly reports are written through the system file picker. v1.8 adds no permission at all — the app lock uses built-in APIs and its own passcode, the widget uses the framework's `RemoteViews`, and automatic backup goes through the system file picker |
| **Network** | **No internet permission at all.** Nothing is uploaded anywhere |
| **Your data** | Stored only on your device. Uninstalling the app deletes it. Upgrading keeps it — the database migrates automatically (now version 6), and rows written by older versions get safe defaults (no tags, not reimbursable, CNY at rate 10000, priority normal, no subtasks). The app lock's passcode is stored as a random salt plus a SHA-256 digest, never as plaintext, and it too lives only on your device — which is why a forgotten passcode cannot be recovered and the only way out is clearing the app's data |
| **Upgrading** | All versions share the same package name and signing key, so you can install a newer APK straight over an older one. Existing records, todos and focus sessions are preserved by the automatic database migration. Backups from v1.5 and earlier still restore, and fields they never stored fall back to their defaults |
| **Known limits** | Only release APKs are signed; debug builds use a different signature and cannot be installed over a release build. The timer stops counting if the system kills the app in the background. To-do reminders and the nightly ledger reminder are deliberately inexact (no exact-alarm permission), so a 09:00 or 21:00 notification may arrive a few minutes late. Statistics always stay on one CNY basis — foreign amounts are converted with the rate you enter. Deleting a category never touches existing records: the name stays on them. A focus stretch shorter than 1 minute is not recorded as an interrupted session, and breaks are never recorded. The in-app language does not rename the launcher icon or its shortcuts — those follow the system language. Dragging to reorder works in the plain list only: while a search is active, in the completed filter and in the kanban view the app says reordering is unavailable. Recurring entries are not written by a background job — they are filled in when you open the app or at the evening reminder, so a due entry lands on your next launch rather than at the exact due moment, and a long absence back-fills only the most recent occurrence. The app lock has no biometric or device-credential path and no recovery: a forgotten passcode means clearing the app's data. The widget shows today's figures only. Automatic backup happens only when you open the app or the daily alarm fires, so a user who never opens the app gets no fresh backup, and the JSON it writes is plaintext in the folder you chose, which may be a cloud-synced one |
