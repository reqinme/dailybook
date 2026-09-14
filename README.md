# DailyBook · FocusFlow

**English** | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md)

Offline Android apps built with Kotlin + Jetpack Compose.
**No internet permission — your data never leaves your phone.**

---

## What it is

**DailyBook** puts five everyday tools into one app: a ledger, life notes, campus study, a to-do
list and a pomodoro timer. Five tabs at the bottom: **Ledger · Life · Study · Focus · Stats**;
Settings moved out of the tab bar into a slim top bar, where a sub-page shows a back arrow instead.

| Tab | What it does |
| --- | --- |
| 📒 **Ledger** | Income & expense, preset categories, notes, date picker, **every record belongs to an account — cash / WeChat / Alipay / bank card / other, or a name you type yourself — with an account filter chip row and the account shown on each row**, monthly flow grouped by day, month switcher, category breakdown, daily spending bar chart, **monthly budget with a progress bar and an alert notification when it reaches 80% or is exceeded**, **a monthly budget per category (also with an alert when one goes over)**, **search**, **tap any record to edit it**, **a nightly reminder to write down the day's records**, **CSV export that Excel opens correctly**, **multi-currency — besides CNY you can record USD / JPY / EUR / HKD / GBP: pick a currency, enter its rate, and the app stores the converted CNY amount, with the rate remembered per currency and foreign rows showing both figures (e.g. `$12.34 ≈ ¥87.61`)**, **tags — one record can carry several (travel / reimbursement / renovation …), with a tag filter row on the ledger page**, **reimbursement — mark a record pending, then settled once it is paid back**, **custom categories (Settings → Ledger → Categories) where you can add and remove expense and income categories and one tap restores the presets**, and **CSV import that also accepts the app's own exports** |
| ✅ **Todos** | Quick add, check off, star as important, due date with overdue highlighting, **subtasks — a to-do can carry a checklist of small steps that you tick off, add and delete right in the edit sheet, with the row showing progress (e.g. `2/5`) and a thin progress bar; deleting a to-do takes its subtasks with it, so no orphan rows are left behind**, **four priority levels (low / normal / high / urgent) — set them in the edit sheet or straight from a to-do row, rows carry a marker while normal stays unmarked, and older to-dos default to normal**, **long-press a to-do and drag to reorder the list — the new order is saved and a click still opens the editor; reordering is off while a search is active, in the completed filter and in the kanban view, and the UI says so instead of silently doing nothing**, **a list / kanban view toggle — the kanban groups the same to-dos into due today / this week / later / no date / completed sections with their counts, and empty sections are hidden**, **repeating tasks (every day / week / month) that create the next occurrence when you tick one off**, **a reminder on the due date that opens the app when you tap it, with "1 hour later" and "tomorrow morning" snooze buttons on the notification**, **a "move to tomorrow" button in the edit sheet that pushes a due date one day on in one tap**, status filter, **search**, clear all completed |
| ⏱️ **Focus** | Pomodoro timer: focus / short break / long break, ring progress with gradient, notification + vibration when a phase ends, optional auto-advance, long-break every N pomodoros, **a daily pomodoro goal (1 / 2 / 3 / 4 / 6 / 8 or none), with one nudge if it is still unmet at the evening reminder time**, **today's session log**, **a count-up (stopwatch) mode where you focus as long as you like and tap Done to record that stretch**, and **sessions stopped or skipped part-way are kept and flagged as interrupted** |
| 📊 **Stats** | Pomodoros today, focus streak, last 7 days chart, **a 12-week focus heatmap (one column per week, Monday to Sunday, deeper colour for more focus minutes) with this week's count and minutes and this month's count and minutes**, **an 年 / 月 quick-switch bar — ◀ ▶, plus a tappable centre that opens a year and 12-month picker with 回到本月, so jumping months is one tap instead of many (the ledger page has the same bar)** — ledger totals, **a 12-month income / expense trend chart**, **this year's summary** (year expense, income, balance, record count, top spending category), **an account spending breakdown**, **per-category budget progress bars with over-budget categories in red**, **today's progress towards the daily focus goal with a progress bar**, **time invested per to-do — focused minutes and session count for each to-do you started from, top 10** and **all-time totals with a net balance**, and **a reimbursement summary card with the pending total and count plus the all-time reimbursed total**, **a period comparison card — this month against last month for spending and income, and this year against last year for spending, each with a signed percentage; when there is no comparable base (nothing at all recorded last month, say) it says the amounts cannot be compared instead of showing a fake 0% or +100%**, **up to four plain-language insights — how much more or less you spent than last month and which category drove it, the biggest category this month, how many days since your last entry, and how much budget is left; a month with no data shows none of them**, **monthly report export — the current month as a shareable PNG image, a standalone HTML file (inline CSS only, no scripts and no external resources, so it reads fine offline) and a PDF (A4, as many pages as it needs), all three through the system file picker so the app still needs no storage permission; the report carries the month's figures, a bar chart, the category breakdown, focus stats and the insight lines, and a month with no data says there is nothing to export rather than writing an empty file**, **the raw record lists are gone: 本月记录 / 分类明细 / 每日明细 / 专注记录 / 待办完成情况 are title rows that open dedicated detail pages, while every aggregate card, chart, comparison, insight and the monthly-report export stay on the stats page**, with interrupted sessions marked in the log |
| 🧺 **Life** | Five views behind one segmented control — **待办 / 习惯打卡 / 备忘录 / 大事记 / 重要日期**. **Habit check-ins: a row per habit with an emoji, its name, today's progress against a target and a unit (次 / 个 / 页 / 分钟), the current streak (连续 N 天), how many days it is done this week, a 7-day mini grid, +/− to log a count and a tap on the row to check in; the add / edit dialog carries emoji, target, unit and a weekly goal — and the same mechanism is how a 背单词 / 背书 plan works, as a habit with a unit and a daily target**. **Memos: two-column cards with a title and a preview, pinned to the top or unpinned, edited and deleted**. **Milestones: a vertical timeline grouped by year with a rail (dot plus connector), title, date and note; an attached image is indicated but not rendered**. **Important dates: a hero countdown card for the nearest one (N 天 / 就是今天), then the rest as rows; each entry can be 阳历 or 农历 (leap months included), carries a repeat rule (只过一次 / 每年 / 每月 / 每周) and an optional reminder (提前 0 / 1 / 3 / 7 天), and a per-entry 导出 .ics writes a real RFC 5545 VEVENT — RRULE and an optional VALARM included — through the system file picker so the phone's calendar can import it** |
| 🎓 **Study** | A hub with a summary card (today's and this week's classes, GPA, the next exam countdown, open and overdue assignments), today's courses inline, and a grid of entry cards to eight sub-pages. **Timetable: a today / whole-week toggle and a week grid of 7 days × 12 periods, course blocks sized by their period range and coloured from a fixed palette; a course carries 星期, 起止节, a 周次表达式 (`1-16`, `1,3,5-9`, or 单双周 such as `1-16单`), the term start date and a colour**. **Exams: cards sorted by date with a big countdown (N 天 / 今天 / 已结束), each expanding to its revision task list (check off, delete, add inline) plus an 「自动排复习计划」 button that proposes evenly spaced tasks — the UI calls it a suggestion you can delete**. **Assignments: the to-do list filtered to entries that carry a course name, grouped 逾期 / 今天 / 本周 / 以后 / 没有日期 / 已完成 with overdue rows highlighted; adding one asks for title, course and due date**. **Grades & GPA: grades grouped by term with score, credit, converted grade point and category, an add / edit dialog taking 百分制 / 五级制 / 直接给绩点, and a documented 4.0-scale mapping that scales to 5.0 when you pick that 口径**. **Credit progress: per-category bars (已修 / 要求) with an overall total and an 「其他已修学分」 card for categories without a target**. **Awards: scholarship / contest / certificate / other records grouped, each with a level (国家级 / 省级 / 校级 / 院级), date and note — an attached image is indicated but not rendered**. **Weekly report: a read-only summary of the last 7 days (focus minutes and sessions, to-do completion, vocabulary check-ins, spending, overdue assignments) with an 「导出图片」 button that renders it to a PNG through the system file picker — the to-do figure is a plain count, because the app does not store a completion date** |
| ⚙️ **Settings** | **A system-settings-style list of seven categories — 外观 / 语言 / 记账 / 专注 / 学习 / 数据与备份 / 关于与更新 — each opening its own sub-page, and every setting that existed before moved into the matching one without changing**. **学习 covers the term start date (a date picker that turns the timetable's week numbers into real dates), the GPA 计算口径 (4.0 / 5.0) and a class reminder (a switch plus 5 / 10 / 15 / 30 minutes before)**. **关于 reads the app name, package, version and minimum Android version from the installed package at runtime — deliberately not hardcoded — and states that all data stays on the device, with licence notes; 检查更新 shows the current version and opens the GitHub releases page in the browser (`ACTION_VIEW`), with an honest note that the app has no internet permission and therefore cannot check for updates by itself**. **App language — 简体中文 / 繁體中文 / English / 日本語, four chips, applied instantly with no restart**, timer lengths, vibration, keep screen on, theme, **twelve colour schemes — 青 (Teal, the default) / 靛蓝 (Indigo) / 紫罗兰 (Violet) / 玫红 (Rose) / 琥珀 (Amber) / 森林 (Forest) / 天蓝 (Sky) / 薄荷 (Mint) / 珊瑚 (Coral) / 咖啡 (Coffee) / 石墨 (Graphite) / 樱花 (Sakura), each working in light and dark mode, and every one checked for contrast — body text at 4.5:1 or better and UI elements at 3:1, with the twelve primaries verified distinct — so none becomes unreadable in dark mode; dynamic colour still wins over the chosen scheme when it is on**, **a custom background image — pick any picture through the system file picker, decoded with a two-pass sample so a huge photo cannot exhaust memory; surfaces then go translucent over it and a 蒙版 slider (0–80) sets how far the picture shows through, with a 清除 button to remove it, and a failed decode is treated as "no background" instead of crashing**, **a daily focus goal**, **a monthly budget per category**, **budget alerts on or off**, **the nightly ledger reminder with a time you pick**, **recurring entries for fixed costs — rent, subscriptions and other regular costs are set up once with an amount, type, category, account, note, a weekly or monthly rule and a first due date; due entries are written automatically, filled in when you open the app or at the evening reminder, each one pushing the next date forward so nothing is missed and nothing is recorded twice, and a long absence only back-fills the most recent occurrence instead of flooding the ledger; each rule can be paused, resumed or deleted, and the settings row shows how many rules exist and when the next one is due**, **a periodic recap — off / weekly / monthly, sent on Sunday at 20:00 or on the 1st at 10:00**, **backup & restore**, **automatic backup — pick a folder through the system file picker and the app writes the complete data as one JSON file whenever you open it or at the evening reminder, at most one file per day (same day overwrites, never a duplicate copy) and keeping the newest 7 while cleaning up older ones; it only ever deletes files it created itself (the `DailyBook-backup-` prefix), failures are reported honestly by cause (no permission / cannot create / cannot write), and a failure is never recorded as a successful backup and never shown as success**, **and **CSV export**, **category management (add / remove expense and income categories, or restore the presets)**, **CSV import**, and data clearing |

Also included: category badges with colour and emoji, **launcher shortcuts (long-press the app
icon to jump straight into recording an entry, adding a to-do or starting a focus session)**,
and a tablet / landscape layout with a side navigation rail.

**Home-screen widgets:** the first shows today's spending and income, how many to-dos are still
open and today's focus minutes; tapping it opens the app. The second is an **exam countdown** — one
exam's name, date and countdown (N 天 / 今天 / 已结束) plus its course and location, with a small
「切换」 hotspot that cycles through the upcoming exams, remembering the choice for that widget
instance; with no upcoming exam it shows a friendly prompt instead. Both are built on the
framework's `RemoteViews`, refresh when the app's data changes and otherwise at the 30-minute
interval the system allows as a minimum, and their text follows the system language (with the
in-app language applied where the system supports it).

**Life, in one tab:** the 生活 tab switches between 待办, 习惯打卡, 备忘录, 大事记 and 重要日期.
Habits are rows with an emoji, a daily target with a unit, the current streak, this week's days and
a 7-day mini grid, logged with +/− or by tapping the row — and a 背单词 / 背书 plan is simply a habit
with a unit and a daily target, so it needs no separate feature. Memos are two-column cards.
Milestones are a timeline grouped by year. Important dates lead with a countdown card for the
nearest one and support both 阳历 and **农历** (leap months included), a repeat rule, a reminder and
a per-entry **.ics export** that the phone's calendar can import.

**Study, in one tab:** the 学习 tab is a hub — a summary card, today's courses and eight sub-pages
(课表, 考试与复习, 作业与 DDL, 成绩与 GPA, 学分进度, 奖助与证书, 学习周报 and the vocabulary plan).
The timetable is a 7 × 12 week grid whose blocks are coloured by course and sized by their period
range, driven by a 周次表达式 such as `1-16` or `1-16单`. Assignments are the to-do list filtered to
entries that carry a course name, grouped 逾期 / 今天 / 本周 / 以后 / 没有日期 / 已完成 with overdue
rows highlighted — the same rows, not a second list. Exams carry a revision task list and a button
that proposes an evenly spaced revision plan you are free to delete. Grades convert 百分制 / 五级制
to grade points, and the weekly report summarises the last 7 days and exports itself as a PNG.

**About & updates:** the 关于 page reads the app name, package, version and minimum Android version
from the installed package at runtime rather than hardcoding them, and states that all your data
stays on the device. **检查更新** shows the current version and opens the GitHub releases page in
your browser — the app has no internet permission, so an in-app update check is impossible, and the
page says exactly that instead of pretending otherwise.

**Twelve colour schemes:** 青 / 靛蓝 / 紫罗兰 / 玫红 / 琥珀 / 森林 / 天蓝 / 薄荷 / 珊瑚 / 咖啡 /
石墨 / 樱花, each in a light and a dark version. Contrast is not eyeballed but computed — a unit test
checks body text at 4.5:1 or better and UI elements at 3:1 in both modes, and verifies that the
twelve primaries are distinct, so a scheme that was wired up wrongly cannot ship unnoticed. You can
also set **a custom background image**: pick any picture through the system file picker, and the
surfaces turn translucent over it with a 蒙版 slider (0–80) deciding how far the picture shows
through, plus a 清除 button to take it away.

**Four languages:** every screen, every notification and the CSV export come in
**简体中文 / 繁體中文 / English / 日本語**. Pick one in **Settings → Language** and it takes effect
immediately, with no restart. The launcher name and the home-screen shortcut labels follow the
**system** language instead, because they are drawn by the system rather than by the app.

**Backup & restore:** export everything — transactions (with their account, tags, currency and
reimbursement state), todos (with their subtask checklists, priority, manual order and course name),
recurring rules, focus sessions and the monthly budget, including per-category budgets, plus the
life records (memos, milestones, important dates, habits and their check-ins) and the study records
(courses, exams with their revision tasks, grades, credit targets and awards) — into a single JSON
file, and load it back later (for example after changing phones).
The file is written to a location you pick through the system file picker, so the app still needs
no storage permission. Restoring replaces the current data, and asks for confirmation first.
Backups written by older versions still restore fine — backups from v2–v4 are read too, sections
they never stored simply come back empty, and single fields they never stored fall back to their
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
| **APK size** | About 2 MB (v1.9 grows by very little: the timetable, the weekly report, both widgets and the background image are all drawn with framework and Compose APIs) |
| **Installing** | The app is self-signed (not distributed through a store), so you must allow *"install apps from unknown sources"* in system settings |
| **Permissions** | Exactly three: notifications, vibration, and rescheduling reminders after a reboot (to-dos, the nightly ledger reminder and the periodic recap). No storage permission: backups, CSV files, monthly reports, the .ics export and the weekly-report PNG are written through the system file picker. v1.9 adds no permission at all — the second widget uses the framework's `RemoteViews`, the background image and the .ics / PNG exports go through the system file picker, and 检查更新 merely opens a browser |
| **Network** | **No internet permission at all.** Nothing is uploaded anywhere. The 检查更新 page therefore cannot check anything by itself: it shows your version and hands the releases page to the browser |
| **Your data** | Stored only on your device. Uninstalling the app deletes it. Upgrading keeps it — the database migrates automatically (now version 7, which adds `todos.courseName` and eleven tables: memos, milestones, important dates, habits, habit check-ins, courses, exams, revision tasks, grades, credit targets and awards), and rows written by older versions get safe defaults (no tags, not reimbursable, CNY at rate 10000, priority normal, no subtasks, no course name) |
| **Upgrading** | All versions share the same package name and signing key, so you can install a newer APK straight over an older one. Existing records, to-dos and focus sessions are preserved by the automatic database migration. Backups from older versions still restore — v2 to v4 files are read with their missing sections left empty, and fields they never stored fall back to their defaults |
| **Known limits** | Only release APKs are signed; debug builds use a different signature and cannot be installed over a release build. The timer stops counting if the system kills the app in the background. To-do reminders and the nightly ledger reminder are deliberately inexact (no exact-alarm permission), so a 09:00 or 21:00 notification may arrive a few minutes late. Statistics always stay on one CNY basis — foreign amounts are converted with the rate you enter. Deleting a category never touches existing records: the name stays on them. A focus stretch shorter than 1 minute is not recorded as an interrupted session, and breaks are never recorded. The in-app language does not rename the launcher icon or its shortcuts — those follow the system language. Dragging to reorder works in the plain list only: while a search is active, in the completed filter and in the kanban view the app says reordering is unavailable. Recurring entries are not written by a background job — they are filled in when you open the app or at the evening reminder, so a due entry lands on your next launch rather than at the exact due moment, and a long absence back-fills only the most recent occurrence. The 今日 widget shows today's figures only, and the countdown widget shows one exam at a time. Images attached to a milestone or an award are indicated but not rendered — no image library is bundled — and the weekly report's to-do figure is a plain count of completed to-dos, because the app does not store a completion date. The 作业/DDL list is the to-do list filtered by course name, and a 背单词 plan is a habit, not a separate table. The stats and ledger pages follow the month you selected, so a figure can look wrong until you notice the month. **App lock is gone in this release (v1.9)**: there is no passcode screen, no biometric path and nothing encrypts your data at rest — that lock existed only in v1.8 and was removed on request. Automatic backup happens only when you open the app or the daily alarm fires, so a user who never opens the app gets no fresh backup, and the JSON it writes is plaintext in the folder you chose, which may be a cloud-synced one |
