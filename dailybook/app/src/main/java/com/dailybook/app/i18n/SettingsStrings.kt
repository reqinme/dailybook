package com.dailybook.app.i18n

/**
 * 设置页文案。
 *
 * 只用 pick / pickf 四语取值，参数顺序固定「简中 / 繁中 / 英文 / 日文」。
 * 通用按钮（保存 / 取消 / 确定 / 清除 / 管理 / 语言 …）复用 [AppStrings]，不在这里重复。
 * 分类名、账户名、emoji 和语言自己的原文名（[Lang.label]）属于数据，不进这个文件。
 */
object SettingsStrings {

    // ---- 页面标题 / 分区标题 ----
    fun title(lang: Lang) = pick(lang, "设置", "設定", "Settings", "設定")
    fun sectionAppearance(lang: Lang) = pick(lang, "外观", "外觀", "Appearance", "外観")
    fun sectionLedger(lang: Lang) = pick(lang, "记账", "記帳", "Ledger", "家計簿")
    fun sectionFocus(lang: Lang) = pick(lang, "专注计时", "專注計時", "Focus timer", "集中タイマー")
    fun sectionData(lang: Lang) = pick(lang, "数据", "資料", "Data", "データ")

    // ---- 外观：主题 ----
    fun theme(lang: Lang) = pick(lang, "主题", "主題", "Theme", "テーマ")
    fun themeSystem(lang: Lang) = pick(lang, "跟随系统", "跟隨系統", "System", "システム")
    fun themeLight(lang: Lang) = pick(lang, "浅色", "淺色", "Light", "ライト")
    fun themeDark(lang: Lang) = pick(lang, "深色", "深色", "Dark", "ダーク")
    fun dynamicColor(lang: Lang) =
        pick(lang, "动态取色（Android 12+）", "動態取色（Android 12+）", "Dynamic colour (Android 12+)", "ダイナミックカラー（Android 12+）")

    // ---- 记账：预算 ----
    fun monthlyBudget(lang: Lang) = pick(lang, "月度预算", "月度預算", "Monthly budget", "月間予算")
    fun budgetNotSet(lang: Lang) =
        pick(lang, "未设置（不显示预算进度）", "未設定（不顯示預算進度）", "Not set (no progress bar)", "未設定（予算の進捗は表示しません）")
    fun setMonthlyBudget(lang: Lang) = pick(lang, "设置月度预算", "設定月度預算", "Set monthly budget", "月間予算を設定")
    fun monthlyBudgetExplain(lang: Lang) = pick(
        lang,
        "每月支出接近预算时，记账页会显示进度条；超支会标红。留空或填 0 表示不设置。",
        "每月支出接近預算時，記帳頁會顯示進度條；超支會標紅。留空或填 0 表示不設定。",
        "When monthly spending nears the budget, the ledger shows a progress bar and turns red once you overspend. Leave empty or enter 0 for no budget.",
        "月の支出が予算に近づくと家計簿ページに進捗バーが出て、超過すると赤くなります。空欄か 0 で予算なし。"
    )
    fun amount(lang: Lang) = pick(lang, "金额", "金額", "Amount", "金額")
    fun categoryBudget(lang: Lang) = pick(lang, "分类预算", "分類預算", "Category budgets", "カテゴリ別予算")
    fun categoryBudgetExplain(lang: Lang) = pick(
        lang,
        "给常超支的分类单独设每月上限，留空表示不限。超支的分类会在统计页标红。",
        "給常超支的分類單獨設每月上限，留空表示不限。超支的分類會在統計頁標紅。",
        "Set a monthly cap for categories you often overspend on; leave empty for no limit. Overspent categories turn red on the stats page.",
        "よく超過するカテゴリに月の上限を設定できます。空欄なら無制限。超過したカテゴリは統計ページで赤くなります。"
    )
    fun categoryBudgetHint(lang: Lang) =
        pick(lang, "给常超支的分类单独设上限", "給常超支的分類單獨設上限", "Set a cap for categories you overspend on", "よく超過するカテゴリに上限を設定")
    fun categoryBudgetCount(lang: Lang, count: Int) =
        pickf(lang, "%d 个分类已设置", "%d 個分類已設定", "%d categories set", "%d 件のカテゴリを設定済み", count)
    fun categoryBudgetOver(lang: Lang, count: Int) =
        pickf(lang, "%d 个超支", "%d 個超支", "%d over budget", "%d 件が超過", count)

    // ---- 记账：提醒 ----
    fun nightlyLedgerReminder(lang: Lang) = pick(lang, "每晚记账提醒", "每晚記帳提醒", "Nightly ledger reminder", "毎晩の記帳リマインダー")
    fun reminderTime(lang: Lang) = pick(lang, "提醒时间", "提醒時間", "Reminder time", "リマインダー時刻")
    fun reminderTimeTitle(lang: Lang) = pick(lang, "每晚提醒时间", "每晚提醒時間", "Nightly reminder time", "毎晩のリマインダー時刻")

    // ---- 专注计时 ----
    fun focusDuration(lang: Lang) = pick(lang, "专注时长", "專注時長", "Focus length", "集中時間")
    fun shortBreak(lang: Lang) = pick(lang, "短休息", "短休息", "Short break", "小休憩")
    fun longBreak(lang: Lang) = pick(lang, "长休息", "長休息", "Long break", "長休憩")
    /** LabeledSlider 自己会把 %d 换成当前数值，所以这里给出的是模板 */
    fun minutesTemplate(lang: Lang) =
        pick(lang, "%d 分钟", "%d 分鐘", "%d min", "%d 分")
    fun longBreakEveryLabel(lang: Lang) = pick(lang, "长休息间隔", "長休息間隔", "Long break interval", "長休憩の間隔")
    fun longBreakEveryValue(lang: Lang, count: Int) =
        pickf(lang, "每 %d 个", "每 %d 個", "Every %d", "%d 回ごと", count)
    fun decrease(lang: Lang) = pick(lang, "减少", "減少", "Decrease", "減らす")
    fun increase(lang: Lang) = pick(lang, "增加", "增加", "Increase", "増やす")
    fun vibrateOnPhaseEnd(lang: Lang) = pick(lang, "阶段结束震动提醒", "階段結束震動提醒", "Vibrate at phase end", "フェーズ終了時に振動")
    fun autoStartNext(lang: Lang) = pick(lang, "自动开始下一阶段", "自動開始下一階段", "Auto-start next phase", "次のフェーズを自動開始")
    fun keepScreenOn(lang: Lang) = pick(lang, "计时中保持屏幕常亮", "計時中保持螢幕恆亮", "Keep screen on while timing", "計測中は画面を常時点灯")

    // ---- 数据摘要 / 备份导出 ----
    fun ledgerSummary(lang: Lang, monthCount: Int, todoCount: Int, focusCount: Int) = pickf(
        lang,
        "本月 %1\$d 笔记录 · %2\$d 条待办 · 今日 %3\$d 个专注",
        "本月 %1\$d 筆紀錄 · %2\$d 條待辦 · 今日 %3\$d 個專注",
        "%1\$d entries this month · %2\$d to-dos · %3\$d focus sessions today",
        "今月 %1\$d 件の記録 · %2\$d 件の ToDo · 今日 %3\$d 回の集中",
        monthCount, todoCount, focusCount
    )
    fun backupAndExport(lang: Lang) = pick(lang, "备份与导出", "備份與匯出", "Backup & export", "バックアップと書き出し")
    fun exportBackup(lang: Lang) = pick(lang, "导出备份", "匯出備份", "Export backup", "バックアップを書き出す")
    fun restoreBackup(lang: Lang) = pick(lang, "恢复备份", "還原備份", "Restore backup", "バックアップを復元")
    fun exportCsv(lang: Lang) =
        pick(lang, "导出记账 CSV（Excel 可打开）", "匯出記帳 CSV（Excel 可開啟）", "Export ledger CSV (opens in Excel)", "家計簿 CSV を書き出す（Excel で開けます）")
    /** 说明段落前半句，末尾留一个空格；拼接 [backupExplain2] 后与原文一致 */
    fun backupExplain1(lang: Lang) = pick(
        lang,
        "备份是一个 JSON 文件，装下全部记账、待办、专注记录和预算设置，",
        "備份是一個 JSON 檔案，裝下全部記帳、待辦、專注紀錄和預算設定，",
        "A backup is one JSON file holding every ledger entry, to-do, focus record and budget setting, ",
        "バックアップは 1 つの JSON ファイルで、家計簿・ToDo・集中の記録と予算設定をすべて含みます。"
    )
    fun backupExplain2(lang: Lang) = pick(
        lang,
        "换手机或重装后可以整份恢复。文件存到你挑的位置，恢复时会覆盖当前数据。",
        "換手機或重裝後可以整份還原。檔案存到你挑的位置，還原時會覆蓋目前資料。",
        "everything can be restored on a new phone or after a reinstall. The file goes wherever you pick, and restoring overwrites your current data.",
        "機種変更や再インストール後も丸ごと復元できます。保存先は自分で選べますが、復元すると現在のデータは上書きされます。"
    )
    fun restoreFromBackupTitle(lang: Lang) = pick(lang, "从备份文件恢复？", "從備份檔案還原？", "Restore from a backup file?", "バックアップから復元しますか？")
    fun restoreConfirmText(lang: Lang) = pick(
        lang,
        "当前所有记账、待办和专注记录都会被备份文件里的内容替换，无法撤销。建议先点「导出备份」存一份现在的数据。",
        "目前所有記帳、待辦和專注紀錄都會被備份檔案裡的內容取代，無法復原。建議先點「匯出備份」存一份目前的資料。",
        "Everything you have now — ledger, to-dos and focus records — will be replaced by the contents of the backup file, and this cannot be undone. Export a backup of the current data first.",
        "現在の家計簿・ToDo・集中の記録はすべてバックアップの内容に置き換わり、元に戻せません。先に「バックアップを書き出す」で現在のデータを保存しておくことをおすすめします。"
    )
    fun pickBackupFile(lang: Lang) = pick(lang, "选择备份文件", "選擇備份檔案", "Pick backup file", "バックアップファイルを選択")

    // ---- 清除数据 ----
    fun clearData(lang: Lang) = pick(lang, "清除数据", "清除資料", "Clear data", "データを消去")
    fun confirmClearTitle(lang: Lang, target: String) =
        pickf(lang, "确认%s？", "確認%s？", "Confirm: %s?", "%s でよろしいですか？", target)
    fun clearAllRecords(lang: Lang) =
        pick(lang, "清除所有记账记录", "清除所有記帳紀錄", "Clear all ledger records", "すべての記録を削除")
    fun clearAllRecordsMessage(lang: Lang) =
        pick(lang, "所有收支记录都会被删除，且无法恢复。", "所有收支紀錄都會被刪除，且無法復原。", "All income and expense records will be deleted, and this cannot be undone.", "すべての収支記録が削除され、元に戻せません。")
    fun clearAllTodos(lang: Lang) =
        pick(lang, "清除所有待办", "清除所有待辦", "Clear all to-dos", "すべての ToDo を削除")
    fun clearAllTodosMessage(lang: Lang) =
        pick(lang, "所有待办事项都会被删除，且无法恢复。", "所有待辦事項都會被刪除，且無法復原。", "All to-dos will be deleted, and this cannot be undone.", "すべての ToDo が削除され、元に戻せません。")
    fun clearFocusStats(lang: Lang) =
        pick(lang, "清除专注记录", "清除專注紀錄", "Clear focus records", "集中の記録を削除")
    fun clearFocusStatsMessage(lang: Lang) = pick(
        lang,
        "专注次数、连续天数和时段明细都会被清零，且无法恢复。",
        "專注次數、連續天數和時段明細都會被清零，且無法復原。",
        "Focus counts, streaks and session details will all be reset, and this cannot be undone.",
        "集中回数・連続日数・時間帯の内訳がすべて 0 になり、元に戻せません。"
    )
    fun clearEverything(lang: Lang) = pick(lang, "清空全部数据", "清空全部資料", "Erase all data", "すべてのデータを消去")
    fun clearEverythingMessage(lang: Lang) = pick(
        lang,
        "记账记录、待办和专注记录都会被删除，且无法恢复。",
        "記帳紀錄、待辦和專注紀錄都會被刪除，且無法復原。",
        "Ledger entries, to-dos and focus records will all be deleted, and this cannot be undone.",
        "家計簿・ToDo・集中の記録がすべて削除され、元に戻せません。"
    )
    fun confirmClear(lang: Lang) = pick(lang, "确定清除", "確定清除", "Clear", "消去する")

    // ---- 底部关于 ----
    fun aboutVersion(lang: Lang, version: String) =
        pickf(lang, "日常本 v%s", "日常本 v%s", "DailyBook v%s", "日常本 v%s", version)
    fun aboutText(lang: Lang) = pick(
        lang,
        "记账 + 待办 + 专注计时，三合一。数据全部存在手机本地，不联网、不上传，只有通知、震动和开机后排提醒需要系统权限。",
        "記帳 + 待辦 + 專注計時，三合一。資料全部存在手機本機，不連網、不上傳，只有通知、震動和開機後排提醒需要系統權限。",
        "Ledger, to-dos and a focus timer in one app. All data stays on your phone — no network, no uploads. Only notifications, vibration and rescheduling reminders after a reboot need system permission.",
        "家計簿 + ToDo + 集中タイマーの三役アプリ。データはすべて端末内に保存され、通信も送信もありません。システム権限が必要なのは通知・振動・再起動後のリマインダー再設定だけです。"
    )
}
