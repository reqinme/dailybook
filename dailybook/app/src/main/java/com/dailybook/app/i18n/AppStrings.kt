package com.dailybook.app.i18n

import java.util.Locale

/**
 * 核心界面文案：应用名、底部标签、通用按钮、日期与状态词、通知文案。
 *
 * 约定：每个函数第一个参数都是 [Lang]，四语文案在编译期必须给全（见 [pick] / [pickf]）。
 * 分类名（餐饮 / 交通 …）是**数据**不是界面文案，不进这里，避免翻译后跟已有记录对不上。
 */
object AppStrings {

    fun appName(lang: Lang) = pick(lang, "日常本", "日常本", "DailyBook", "日常本")

    // ---- 底部标签 ----
    fun tabLedger(lang: Lang) = pick(lang, "记账", "記帳", "Ledger", "家計簿")
    fun tabTodo(lang: Lang) = pick(lang, "待办", "待辦", "Todos", "ToDo")
    fun tabFocus(lang: Lang) = pick(lang, "专注", "專注", "Focus", "集中")
    fun tabStats(lang: Lang) = pick(lang, "统计", "統計", "Stats", "統計")
    fun tabSettings(lang: Lang) = pick(lang, "设置", "設定", "Settings", "設定")

    // ---- 通用动作 ----
    fun save(lang: Lang) = pick(lang, "保存", "儲存", "Save", "保存")
    fun cancel(lang: Lang) = pick(lang, "取消", "取消", "Cancel", "キャンセル")
    fun confirm(lang: Lang) = pick(lang, "确定", "確定", "OK", "OK")
    fun delete(lang: Lang) = pick(lang, "删除", "刪除", "Delete", "削除")
    fun edit(lang: Lang) = pick(lang, "编辑", "編輯", "Edit", "編集")
    fun clear(lang: Lang) = pick(lang, "清除", "清除", "Clear", "クリア")
    fun add(lang: Lang) = pick(lang, "添加", "新增", "Add", "追加")
    fun search(lang: Lang) = pick(lang, "搜索", "搜尋", "Search", "検索")
    fun select(lang: Lang) = pick(lang, "选择", "選擇", "Pick", "選択")
    fun notSet(lang: Lang) = pick(lang, "未设置", "未設定", "Not set", "未設定")
    fun manage(lang: Lang) = pick(lang, "管理", "管理", "Manage", "管理")
    fun optional(lang: Lang) = pick(lang, "可选", "選填", "optional", "任意")

    // ---- 记账类型 ----
    fun txExpense(lang: Lang) = pick(lang, "支出", "支出", "Expense", "支出")
    fun txIncome(lang: Lang) = pick(lang, "收入", "收入", "Income", "収入")
    fun txBalance(lang: Lang) = pick(lang, "结余", "結餘", "Balance", "収支")

    // ---- 待办筛选 ----
    fun filterAll(lang: Lang) = pick(lang, "全部", "全部", "All", "すべて")
    fun filterPending(lang: Lang) = pick(lang, "待完成", "待完成", "Open", "未完了")
    fun filterDone(lang: Lang) = pick(lang, "已完成", "已完成", "Done", "完了")

    // ---- 重复规则 ----
    fun repeatNone(lang: Lang) = pick(lang, "不重复", "不重複", "Does not repeat", "繰り返さない")
    fun repeatDaily(lang: Lang) = pick(lang, "每天", "每天", "Every day", "毎日")
    fun repeatWeekly(lang: Lang) = pick(lang, "每周", "每週", "Every week", "毎週")
    fun repeatMonthly(lang: Lang) = pick(lang, "每月", "每月", "Every month", "毎月")

    // ---- 专注阶段 ----
    fun phaseFocus(lang: Lang) = pick(lang, "专注", "專注", "Focus", "集中")
    fun phaseShortBreak(lang: Lang) = pick(lang, "短休息", "短休息", "Short break", "小休憩")
    fun phaseLongBreak(lang: Lang) = pick(lang, "长休息", "長休息", "Long break", "長休憩")

    // ---- 语言 ----
    fun language(lang: Lang) = pick(lang, "语言", "語言", "Language", "言語")
    fun languageHint(lang: Lang) = pick(
        lang,
        "界面语言会立刻切换，通知和提醒也跟着变。分类名是数据，不随语言变化。",
        "介面語言會立刻切換，通知與提醒也跟著變。分類名稱屬於資料，不隨語言變化。",
        "The interface switches immediately, including notifications and reminders. Category names are data and stay as they are.",
        "表示言語はすぐに切り替わり、通知やリマインダーも連動します。カテゴリ名はデータなので変わりません。"
    )

    // ---- 日期相关 ----
    fun today(lang: Lang) = pick(lang, "今天", "今天", "Today", "今日")
    fun yesterday(lang: Lang) = pick(lang, "昨天", "昨天", "Yesterday", "昨日")
    fun dayBeforeYesterday(lang: Lang) = pick(lang, "前天", "前天", "2 days ago", "一昨日")
    fun weekday(lang: Lang, indexFromMonday: Int): String = when (indexFromMonday) {
        0 -> pick(lang, "周一", "週一", "Mon", "月")
        1 -> pick(lang, "周二", "週二", "Tue", "火")
        2 -> pick(lang, "周三", "週三", "Wed", "水")
        3 -> pick(lang, "周四", "週四", "Thu", "木")
        4 -> pick(lang, "周五", "週五", "Fri", "金")
        5 -> pick(lang, "周六", "週六", "Sat", "土")
        else -> pick(lang, "周日", "週日", "Sun", "日")
    }

    /** 「9月13日」/「Sep 13」 */
    fun monthDay(lang: Lang, month: Int, day: Int): String = when (lang) {
        Lang.EN -> "${monthNameEn(month)} $day"
        else -> String.format(Locale.ROOT, "%d月%d日", month, day)
    }

    /** 「2026年9月」/「Sep 2026」 */
    fun yearMonth(lang: Lang, year: Int, month: Int): String = when (lang) {
        Lang.EN -> "${monthNameEn(month)} $year"
        else -> String.format(Locale.ROOT, "%d年%d月", year, month)
    }

    /** 「2026年」/「2026」 */
    fun year(lang: Lang, year: Int): String = when (lang) {
        Lang.EN -> year.toString()
        else -> String.format(Locale.ROOT, "%d年", year)
    }

    private fun monthNameEn(month: Int): String = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )[(month - 1).coerceIn(0, 11)]

    // ---- 到期文案 ----
    fun dueToday(lang: Lang) = pick(lang, "今天到期", "今天到期", "Due today", "今日が期限")
    fun dueTomorrow(lang: Lang) = pick(lang, "明天到期", "明天到期", "Due tomorrow", "明日が期限")
    fun dueOverdue(lang: Lang, days: Long) = pickf(
        lang,
        "已逾期 %d 天", "已逾期 %d 天", "Overdue by %d days", "%d 日超過",
        days
    )

    // ---- 通知 ----
    fun notifChannelFocus(lang: Lang) = pick(lang, "计时提醒", "計時提醒", "Timer alerts", "タイマー通知")
    fun notifChannelFocusDesc(lang: Lang) = pick(
        lang,
        "专注或休息阶段结束时通知你", "專注或休息階段結束時通知你",
        "Tells you when a focus or break phase ends", "集中・休憩のフェーズが終わったときに知らせます"
    )
    fun notifChannelTodo(lang: Lang) = pick(lang, "待办提醒", "待辦提醒", "To-do reminders", "ToDo リマインダー")
    fun notifChannelTodoDesc(lang: Lang) = pick(
        lang,
        "待办到期时提醒你", "待辦到期時提醒你",
        "Reminds you when a to-do is due", "ToDo の期限に知らせます"
    )
    fun notifChannelLedger(lang: Lang) = pick(lang, "记账提醒", "記帳提醒", "Ledger reminder", "家計簿リマインダー")
    fun notifChannelLedgerDesc(lang: Lang) = pick(
        lang,
        "每晚提醒你记下当天的收支", "每晚提醒你記下當天的收支",
        "A nightly nudge to record the day's spending", "毎晩、その日の収支を記録するよう促します"
    )
    fun notifLedgerTitle(lang: Lang) = pick(
        lang, "今天的账记了吗？", "今天的帳記了嗎？",
        "Did you record today's spending?", "今日の支出は記録しましたか？"
    )
    fun notifLedgerText(lang: Lang) = pick(
        lang, "花一分钟记一下，月底就不会糊里糊涂", "花一分鐘記一下，月底就不會糊里糊塗",
        "One minute now saves the guesswork at month end", "1 分で済みます。月末に困らないように"
    )
    fun notifOverBudgetTitle(lang: Lang) = pick(
        lang, "预算快用完了", "預算快用完了", "Budget almost used up", "予算がもうすぐ上限です"
    )
    fun notifOverBudgetText(lang: Lang, category: String, used: String, budget: String) = pickf(
        lang,
        "%1\$s 已用 ¥%2\$s / ¥%3\$s", "%1\$s 已用 ¥%2\$s / ¥%3\$s",
        "%1\$s: ¥%2\$s of ¥%3\$s used", "%1\$s：¥%2\$s / ¥%3\$s 使用済み",
        category, used, budget
    )
    fun notifOverBudgetOver(lang: Lang, category: String, over: String) = pickf(
        lang,
        "%1\$s 已超支 ¥%2\$s", "%1\$s 已超支 ¥%2\$s",
        "%1\$s is over budget by ¥%2\$s", "%1\$s が ¥%2\$s 予算超過",
        category, over
    )

    /** 金额前缀：中文用 ¥，英文也用 ¥（人民币口径） */
    fun money(lang: Lang, amount: String): String = "¥$amount"

    // ---- 定期汇总 ----
    fun summaryOff(lang: Lang) = pick(lang, "关闭", "關閉", "Off", "オフ")
    fun summaryWeekly(lang: Lang) = pick(lang, "每周", "每週", "Weekly", "毎週")
    fun summaryMonthly(lang: Lang) = pick(lang, "每月", "每月", "Monthly", "毎月")
    fun summaryTitle(lang: Lang, weekly: Boolean) = if (weekly) {
        pick(lang, "本周小结", "本週小結", "Your week in review", "今週のまとめ")
    } else {
        pick(lang, "本月小结", "本月小結", "Your month in review", "今月のまとめ")
    }

    fun summaryBody(lang: Lang, expense: String, income: String, minutes: Int, count: Int) = pickf(
        lang,
        "支出 ¥%1\$s · 收入 ¥%2\$s · 专注 %3\$d 分钟（%4\$d 次）",
        "支出 ¥%1\$s · 收入 ¥%2\$s · 專注 %3\$d 分鐘（%4\$d 次）",
        "Spent ¥%1\$s · earned ¥%2\$s · focused %3\$d min over %4\$d sessions",
        "支出 ¥%1\$s · 収入 ¥%2\$s · 集中 %3\$d 分（%4\$d 回）",
        expense, income, minutes, count
    )

    // ---- 专注目标 ----
    fun focusGoalTitle(lang: Lang) = pick(
        lang, "今天还没达成专注目标", "今天還沒達成專注目標",
        "Today's focus goal isn't met yet", "今日の集中目標はまだ未達成です"
    )

    fun focusGoalBody(lang: Lang, done: Int, goal: Int) = pickf(
        lang, "已完成 %1\$d / %2\$d 个番茄", "已完成 %1\$d / %2\$d 個番茄",
        "%1\$d of %2\$d pomodoros done", "%1\$d / %2\$d 完了", done, goal
    )

    // ---- 通知上的「稍后提醒」 ----
    fun snoozeOneHour(lang: Lang) = pick(lang, "1 小时后", "1 小時後", "In 1 hour", "1 時間後")
    fun snoozeTomorrowMorning(lang: Lang) =
        pick(lang, "明天早上", "明天早上", "Tomorrow morning", "明日の朝")

    // ---- 预算预警 ----
    fun budgetAlert(lang: Lang) = pick(lang, "预算预警", "預算預警", "Budget alerts", "予算アラート")

    fun summaryChannel(lang: Lang) =
        pick(lang, "汇总与预警", "彙總與預警", "Summaries & alerts", "まとめとアラート")

    fun summaryChannelDesc(lang: Lang) = pick(
        lang, "每周 / 每月小结与预算超支提醒", "每週 / 每月小結與預算超支提醒",
        "Weekly / monthly recaps and over-budget warnings", "週次・月次のまとめと予算超過の通知"
    )
    fun budgetNearTitle(lang: Lang) = pick(
        lang, "预算快用完了", "預算快用完了", "Budget almost used up", "予算がもうすぐ上限です"
    )

    fun budgetNearText(lang: Lang, used: String, budget: String) = pickf(
        lang, "本月已用 ¥%1\$s / ¥%2\$s", "本月已用 ¥%1\$s / ¥%2\$s",
        "¥%1\$s of ¥%2\$s used this month", "今月は ¥%1\$s / ¥%2\$s 使用済み", used, budget
    )

    fun budgetOverTitle(lang: Lang) = pick(
        lang, "本月已经超支", "本月已經超支", "You are over budget", "予算を超えました"
    )

    fun budgetOverText(lang: Lang, over: String) = pickf(
        lang, "超出 ¥%1\$s，后面省着点花", "超出 ¥%1\$s，後面省著點花",
        "Over by ¥%1\$s — slow down a little", "¥%1\$s 超過。少し抑えましょう", over
    )

    // ---- 专注阶段结束通知 ----
    fun focusDoneTitle(lang: Lang) = pick(lang, "专注完成 🍅", "專注完成 🍅", "Focus session done 🍅", "集中完了 🍅")
    fun breakDoneTitle(lang: Lang) = pick(lang, "休息结束", "休息結束", "Break is over", "休憩終了")
    fun focusDoneLongBreak(lang: Lang) =
        pick(lang, "很棒！来一次长休息吧", "很棒！來一次長休息吧", "Nice work — take a long break", "お見事。長めに休憩しましょう")

    fun focusDoneShortBreak(lang: Lang) =
        pick(lang, "喝口水，短暂休息一下", "喝口水，短暫休息一下", "Grab some water and take a short break", "水分補給して少し休憩を")

    fun breakDoneBackToFocus(lang: Lang) =
        pick(lang, "回到专注，继续加油", "回到專注，繼續加油", "Back to focus — keep going", "集中に戻りましょう")

    // ---- 操作结果提示（导出 / 恢复的 Toast）----
    fun actionFailed(lang: Lang, reason: String) = pickf(
        lang, "操作失败：%s", "操作失敗：%s", "Something went wrong: %s", "失敗しました：%s", reason
    )

    fun backupExported(lang: Lang, tx: Int, todos: Int, sessions: Int) = pickf(
        lang,
        "已导出 %1\$d 笔记账、%2\$d 条待办、%3\$d 条专注记录",
        "已匯出 %1\$d 筆記帳、%2\$d 條待辦、%3\$d 條專注紀錄",
        "Exported %1\$d ledger entries, %2\$d to-dos, %3\$d focus sessions",
        "%1\$d 件の記録、%2\$d 件の ToDo、%3\$d 件の集中を書き出しました",
        tx, todos, sessions
    )

    fun backupRestored(lang: Lang, tx: Int, todos: Int, sessions: Int) = pickf(
        lang,
        "已恢复 %1\$d 笔记账、%2\$d 条待办、%3\$d 条专注记录",
        "已還原 %1\$d 筆記帳、%2\$d 條待辦、%3\$d 條專注紀錄",
        "Restored %1\$d ledger entries, %2\$d to-dos, %3\$d focus sessions",
        "%1\$d 件の記録、%2\$d 件の ToDo、%3\$d 件の集中を復元しました",
        tx, todos, sessions
    )

    fun csvExported(lang: Lang, count: Int) = pickf(
        lang, "已导出 %d 笔流水", "已匯出 %d 筆流水",
        "Exported %d entries", "%d 件を書き出しました", count
    )

    fun nothingToExport(lang: Lang) =
        pick(lang, "还没有记账记录可导出", "還沒有記帳紀錄可匯出", "No ledger entries to export yet", "書き出せる記録がありません")

    // ---- CSV 导入 ----
    fun importCsv(lang: Lang) = pick(lang, "导入记账 CSV", "匯入記帳 CSV", "Import ledger CSV", "記録 CSV を取り込む")

    fun importCsvHint(lang: Lang) = pick(
        lang, "支持本 App 导出的 CSV（5 / 6 / 7 列都可以）；日期与金额相同的行会自动跳过，重复导入不会翻倍。",
        "支援本 App 匯出的 CSV（5 / 6 / 7 欄都可以）；日期與金額相同的行會自動略過，重複匯入不會翻倍。",
        "Accepts CSV exported by this app (5, 6 or 7 columns). Rows matching an existing record are skipped, so importing twice is safe.",
        "本アプリが書き出した CSV（5 / 6 / 7 列）に対応。既存の記録と一致する行は自動で飛ばすので、二重取り込みになりません。"
    )

    fun nothingToImport(lang: Lang) = pick(
        lang, "这个文件里没有可导入的记账记录", "這個檔案裡沒有可匯入的記帳紀錄",
        "No importable ledger rows in that file", "取り込める記録がこのファイルにありません"
    )

    fun importNothingNew(lang: Lang) = pick(
        lang, "这些记录都已经在了，没有新增", "這些紀錄都已經在了，沒有新增",
        "All of those rows already exist — nothing new", "すべて既に存在します。新規はありません"
    )

    fun importedCsv(lang: Lang, added: Int, skipped: Int) = pickf(
        lang, "已导入 %1\$d 笔（跳过 %2\$d 笔重复）", "已匯入 %1\$d 筆（略過 %2\$d 筆重複）",
        "Imported %1\$d rows (%2\$d duplicates skipped)",
        "%1\$d 件を取り込み（重複 %2\$d 件はスキップ）", added, skipped
    )

    fun backupFileName(lang: Lang) =
        pick(lang, "日常本备份", "日常本備份", "DailyBook-backup", "日常本バックアップ")

    fun ledgerFileName(lang: Lang) =
        pick(lang, "日常本记账", "日常本記帳", "DailyBook-ledger", "日常本家計簿")

    // ---- 新设置项 ----
    fun focusGoalLabel(lang: Lang) =
        pick(lang, "每日专注目标", "每日專注目標", "Daily focus goal", "1 日の集中目標")

    fun focusGoalOff(lang: Lang) = pick(lang, "不设目标", "不設目標", "No goal", "目標なし")

    fun focusGoalHint(lang: Lang) = pick(
        lang, "晚上（或记账提醒时间）还没达标就提醒一次。",
        "晚上（或記帳提醒時間）還沒達標就提醒一次。",
        "If the goal isn't reached by the evening reminder time, you get one nudge.",
        "夜（または家計簿リマインダーの時刻）に未達成なら一度だけ知らせます。"
    )

    fun focusGoalValue(lang: Lang, count: Int) = pickf(
        lang, "每天 %d 个番茄", "每天 %d 個番茄",
        "%d pomodoros a day", "1 日 %d ポモドーロ", count
    )

    fun summaryLabel(lang: Lang) =
        pick(lang, "定期小结", "定期小結", "Periodic recap", "定期のまとめ")

    fun summaryHint(lang: Lang) = pick(
        lang, "每周日晚上 20:00 或每月 1 号上午 10:00 发一条小结，没数据时不打扰。",
        "每週日晚上 20:00 或每月 1 號上午 10:00 發一條小結，沒資料時不打擾。",
        "A recap is sent Sunday 20:00 or on the 1st at 10:00 — skipped when there is nothing to report.",
        "日曜 20:00 または毎月 1 日 10:00 にまとめを送ります。データが無いときは送りません。"
    )
}
