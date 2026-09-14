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

    // ---- 分类管理 ----
    fun categoryManage(lang: Lang) = pick(lang, "分类管理", "分類管理", "Categories", "カテゴリ管理")
    /** 记账卡片上的副标题：支出和收入各有多少个分类可选 */
    fun categoryManageSubtitle(lang: Lang, expense: Int, income: Int) = pickf(
        lang,
        "支出 %d 个 · 收入 %d 个",
        "支出 %d 個 · 收入 %d 個",
        "Expense %d · Income %d",
        "支出 %d 件・収入 %d 件",
        expense, income
    )
    fun categoryManageHint(lang: Lang) = pick(
        lang,
        "删除只是把它从选择器里拿掉，已有记录上的分类名照旧保留，也还能继续选到；想回到预置分类，用下面的按钮恢复。",
        "刪除只是把它從選擇器裡拿掉，既有紀錄上的分類名照舊保留，也還能繼續選到；想回到預設分類，用下面的按鈕還原。",
        "Deleting a category only removes it from the picker — existing records keep their category name and it stays selectable. Use the button below to bring the built-in list back.",
        "削除しても選び方から外れるだけで、既存の記録のカテゴリ名はそのまま残り、引き続き選べます。既定のカテゴリには下のボタンで戻せます。"
    )
    fun categoryManageDone(lang: Lang) = pick(lang, "完成", "完成", "Done", "完了")
    fun newCategoryName(lang: Lang) = pick(lang, "新分类名", "新分類名稱", "New category", "新しいカテゴリ名")
    /** addCategory 返回 false 时的行内提示：空名 / 重名 / 超过 8 个字 */
    fun addCategoryFailed(lang: Lang) = pick(
        lang,
        "名称不能为空、不能跟已有分类重复，最多 8 个字。",
        "名稱不能為空、不能跟既有分類重複，最多 8 個字。",
        "The name can't be blank or already in the list, and is limited to 8 characters.",
        "名前は空欄・重複不可、8 文字までです。"
    )
    fun resetCategories(lang: Lang) =
        pick(lang, "恢复预置分类", "還原預設分類", "Restore default categories", "既定のカテゴリに戻す")
    fun removeCategoryLabel(lang: Lang, name: String) =
        pickf(lang, "删除分类 %s", "刪除分類 %s", "Remove category %s", "カテゴリ %s を削除", name)

    // ---- 周期记账 ----
    // 标题 / 说明 / 空状态 / 「下次 %s」在 AppStrings 里（记账页也用得到），
    // 这里只放设置页独有的那几句。规则名走 RepeatRule.label，分类名和账户名是数据。
    /** 记账卡片上的副标题：已建了几条周期记账 */
    fun recurringCount(lang: Lang, count: Int) =
        pickf(lang, "%d 条", "%d 條", "%d rules", "%d 件", count)
    fun recurringAdd(lang: Lang) = pick(lang, "新增周期记账", "新增週期記帳", "Add recurring entry", "定期的な記録を追加")
    fun recurringKindLabel(lang: Lang) = pick(lang, "类型", "類型", "Type", "種類")
    fun recurringEnabled(lang: Lang) =
        pick(lang, "已启用（到日子自动记一笔）", "已啟用（到日子自動記一筆）", "Active — recorded on its due date", "有効（期日に自動で記録）")
    fun recurringPaused(lang: Lang) = pick(lang, "已暂停", "已暫停", "Paused", "一時停止中")
    fun recurringFirstDue(lang: Lang) =
        pick(lang, "首次记账日期", "首次記帳日期", "First due date", "最初の記録日")
    fun recurringAmountRequired(lang: Lang) =
        pick(lang, "请先填写大于 0 的金额", "請先填寫大於 0 的金額", "Enter an amount greater than 0", "0 より大きい金額を入力してください")

    // ---- CSV 导入 ----
    fun importCsvTitle(lang: Lang) =
        pick(lang, "导入记账 CSV？", "匯入記帳 CSV？", "Import ledger CSV?", "記録 CSV を取り込みますか？")
    fun importCsvConfirmText(lang: Lang) = pick(
        lang,
        "导入不会替换现有数据，只会把文件里的记录追加进来；和现有记录完全相同的行会自动跳过。行数很多时要稍等一会儿。",
        "匯入不會取代目前資料，只會把檔案裡的紀錄追加進來；和既有紀錄完全相同的行會自動略過。筆數很多時要稍等一下。",
        "Importing replaces nothing — the rows in the file are added on top of what you have, and rows identical to an existing record are skipped. A big file can take a moment.",
        "取り込みで既存のデータは置き換わりません。ファイル内の記録が追加され、既存の記録と完全に同じ行は自動でスキップされます。行数が多いと少し時間がかかります。"
    )
    fun pickCsvFile(lang: Lang) = pick(lang, "选择 CSV 文件", "選擇 CSV 檔案", "Pick CSV file", "CSV ファイルを選択")

    // ---- v1.8 外观：配色方案 ----
    // 六个配色名（青 / 靛蓝 / …）在 AppStrings.paletteTeal…paletteForest 里，
    // 主题页也用得到，所以不在这里重复；这里只放配色独有的一句提示。
    /** 跟随系统取色打开时的说明：配色会被系统取色盖掉 */
    fun paletteDynamicHint(lang: Lang) = pick(
        lang,
        "已开启跟随系统取色，配色会被系统取色盖掉，关掉它才生效。",
        "已開啟跟隨系統取色，配色會被系統取色蓋掉，關掉它才生效。",
        "Dynamic colour is on, so the system palette overrides this choice — turn it off to use it.",
        "システム連動の動的カラーがオンのため、この配色は使われません。オフにすると反映されます。"
    )

    // ---- v1.8 数据：自动备份 ----
    // 标题、说明、按钮名、失败原因的人话都在 AppStrings（VM 的 Toast 也用它），这里只补两处。
    fun autoBackupFolderLabel(lang: Lang) =
        pick(lang, "备份文件夹", "備份資料夾", "Backup folder", "バックアップ先フォルダ")
    /** 开关打开但没有文件夹时的行内提示（AppStrings.autoBackupNeedsFolder 是 Toast 版，这里常驻在卡片上） */
    fun autoBackupNeedsFolderHint(lang: Lang) = pick(
        lang,
        "还没有选文件夹，自动备份不会开始；先选一个文件夹再打开开关。",
        "還沒有選資料夾，自動備份不會開始；先選一個資料夾再打開開關。",
        "No folder chosen yet, so nothing is backed up — pick a folder first, then turn this on.",
        "フォルダが未選択のため自動バックアップは動きません。先にフォルダを選んでからオンにしてください。"
    )

    // =====================================================================
    // v1.9 系统设置式改造：设置首页（分类列表）+ 各分类子页面
    // =====================================================================

    /**
     * 设置首页的标题。
     *
     * 顶栏的标题走 [AppStrings.settingsTitle]（路由层用），这里放一份让设置页面
     * 自己也能取到标题，不必到处 import 两张表。
     */
    fun homeTitle(lang: Lang) = pick(lang, "设置", "設定", "Settings", "設定")

    /** 首页顶部小标题：「日常本 1.8」那种写法，版本号由 queryAppVersion() 拿出来 */
    fun homeHeader(lang: Lang, appName: String, version: String) =
        pickf(lang, "%1\$s %2\$s", "%1\$s %2\$s", "%1\$s %2\$s", "%1\$s %2\$s", appName, version)

    /** 首页分类列表的副标题（分类名用 SettingsCategory.label） */
    fun homeCategoryHint(lang: Lang) = pick(
        lang,
        "点一项进入它的子页面，每个分类都有自己的页面。",
        "點一項進入它的子頁面，每個分類都有自己的頁面。",
        "Tap a category to open its own page.",
        "項目をタップすると、そのカテゴリ専用のページが開きます。"
    )

    /** 背景图（还没做的功能）：行是灰色的，不要写成「已经能用」 */
    fun backgroundImage(lang: Lang) = pick(lang, "背景图", "背景圖", "Background image", "背景画像")
    fun notAvailableYet(lang: Lang) = pick(lang, "暂未开放", "尚未開放", "Not available yet", "未対応")
    fun backgroundNotYetHint(lang: Lang) = pick(
        lang,
        "这个开关还没做，现在选了也不会有变化，下一版再补。",
        "這個開關還沒做，現在選了也不會有變化，下一版再補。",
        "This switch isn't built yet — choosing it does nothing for now; it's coming in a later version.",
        "このスイッチはまだ実装していません。今は選んでも何も変わりません（後の版で対応します）。"
    )

    // ---- 外观：自定义背景图（SettingsStore.backgroundUri / backgroundScrim）----
    /** 「已选好背景图」；没有背景图时复用 AppStrings.autoBackupNone（「还没有选择」） */
    fun backgroundChosen(lang: Lang) =
        pick(lang, "已设置背景图", "已設定背景圖", "Background image set", "背景画像を設定済み")

    fun backgroundPick(lang: Lang) = pick(lang, "选择图片", "選擇圖片", "Choose image", "画像を選ぶ")

    /** 背景图上的蒙版浓度：越大文字越清楚、图越淡 */
    fun backgroundScrim(lang: Lang) = pick(lang, "蒙版浓度", "蒙版濃度", "Scrim opacity", "マスクの濃さ")

    /**
     * LabeledSlider 自己会把 %d 换成当前数值，所以这里给出的是模板。
     *
     * ⚠️ 只写一个 `%`：这个模板**不过 String.format**（是 LabeledSlider 用
     * `valueText.replace("%d", …)` 直接换的），写成 `%%` 会在界面上原样显示成「30%%」。
     */
    fun percentTemplate(lang: Lang) = pick(lang, "%d%", "%d%", "%d%", "%d%")

    fun backgroundHint(lang: Lang) = pick(
        lang,
        "图片只在手机本地读取，不会复制进 App 也不会外传；蒙版越浓，界面文字越清楚。",
        "圖片只在手機本機讀取，不會複製進 App 也不會外傳；蒙版越濃，介面文字越清楚。",
        "The image is read on this phone only — it is never copied into the app or uploaded. A stronger scrim keeps text readable.",
        "画像は端末内でのみ読み込み、アプリに複製も送信もしません。マスクを濃くすると文字が読みやすくなります。"
    )

    // ---- 学习设置（学期起始日 / GPA 口径 / 上课提醒）----
    fun termStart(lang: Lang) = pick(lang, "学期起始日", "學期起始日", "Term start date", "学期の開始日")
    fun gpaScale(lang: Lang) = pick(lang, "GPA 计算口径", "GPA 計算口徑", "GPA scale", "GPA の方式")
    fun classReminder(lang: Lang) = pick(lang, "上课提醒", "上課提醒", "Class reminders", "授業リマインダー")
    fun minutesBefore(lang: Lang) = pick(lang, "提前多少分钟", "提前多少分鐘", "How many minutes early", "何分前に知らせるか")

    /** 「4.0 分制」/「5.0 分制」 */
    fun gpaScaleValue(lang: Lang, scale: String) =
        pickf(lang, "%s 分制", "%s 分制", "%s scale", "%s 方式", scale)

    /** 「提前 %d 分钟」 */
    fun minutesBeforeValue(lang: Lang, minutes: Int) =
        pickf(lang, "提前 %d 分钟", "提前 %d 分鐘", "%d min before", "%d 分前", minutes)

    /**
     * 学习这三项还没有对应的偏好设置：SettingsStore 里没有键，VM 也没有写入入口，
     * 所以行是灰的，并明确说出「存在哪里」而不是假装已经生效。
     */
    fun studyNotConfiguredHint(lang: Lang) = pick(
        lang,
        "这个设置还没接线：偏好里还没有对应的键，现在改不了。课表里每门课可以单独填学期起始日。",
        "這個設定還沒接線：偏好裡還沒有對應的鍵，現在改不了。課表裡每門課可以單獨填學期起始日。",
        "Not wired up yet — there is no preference behind it, so it can't be changed here. Each course in the timetable has its own term start date.",
        "まだ配線されていません（対応する設定項目がありません）。時間割の各授業には学期開始日を個別に設定できます。"
    )

    // ---- 关于与更新 ----
    /** 关于卡片里的第一行：应用名 + 版本号 */
    fun aboutAppLine(lang: Lang, appName: String, version: String) =
        pickf(lang, "%1\$s %2\$s", "%1\$s %2\$s", "%1\$s %2\$s", "%1\$s %2\$s", appName, version)

    fun aboutPackage(lang: Lang) = pick(lang, "包名", "套件名稱", "Package", "パッケージ名")

    fun aboutVersionName(lang: Lang) = pick(lang, "版本号", "版本號", "Version", "バージョン")
    fun aboutVersionCode(lang: Lang) = pick(lang, "版本代码", "版本代碼", "Version code", "バージョンコード")

    /** 「版本信息」那一节的标题（别和 [aboutVersionName] 的行标签重复） */
    fun aboutVersionSection(lang: Lang) =
        pick(lang, "版本信息", "版本資訊", "Version info", "バージョン情報")

    fun aboutMinAndroid(lang: Lang) = pick(lang, "支持的最低 Android 版本", "支援的最低 Android 版本", "Minimum Android version", "対応する最小の Android バージョン")

    /** 「Android 8.0（API 26）及以上」，数值来自 Build 常量 */
    fun aboutMinAndroidValue(lang: Lang, release: String, api: Int) = pickf(
        lang,
        "Android %1\$s（API %2\$d）及以上", "Android %1\$s（API %2\$d）及以上",
        "Android %1\$s (API %2\$d) and newer", "Android %1\$s（API %2\$d）以上",
        release, api
    )

    /** 版本号旁边的小字：数字是从系统里读的，不是我写死的 */
    fun aboutVersionSource(lang: Lang) = pick(
        lang,
        "版本号由系统从安装包里读出，代码里不写死。",
        "版本號由系統從安裝檔裡讀出，程式碼裡不寫死。",
        "The version comes from the installed package at runtime — it is not hard-coded.",
        "バージョンはインストール済みパッケージから実行時に読み取ります（コードに固定していません）。"
    )

    /** 系统查不到版本时的兜底显示（不写死一个假版本号） */
    fun versionUnknown(lang: Lang) = pick(lang, "未知", "未知", "Unknown", "不明")

    fun aboutDescription(lang: Lang) = pick(
        lang,
        "记账、待办、专注计时、习惯与学习管理合在一起的一个小工具。",
        "記帳、待辦、專注計時、習慣與學習管理合在一起的一個小工具。",
        "One small app that puts a ledger, to-dos, a focus timer, habits and study planning together.",
        "家計簿・ToDo・集中タイマー・習慣・学習管理を 1 つにまとめた小さなアプリです。"
    )

    /** 关于页上「数据与联网」那一节的标题 */
    fun aboutDataTitle(lang: Lang) = pick(
        lang, "数据与联网", "資料與連網", "Data & network", "データと通信"
    )

    /** 关于页最要紧的一句：数据只在本地 */
    fun aboutAllLocal(lang: Lang) = pick(
        lang,
        "所有数据只存在这台手机里，App 没有申请联网权限，也不会把任何内容上传到服务器；导出与备份都由你自己挑位置保存。",
        "所有資料只存在這台手機裡，App 沒有申請連網權限，也不會把任何內容上傳到伺服器；匯出與備份都由你自己挑位置儲存。",
        "Everything stays on this phone: the app does not request the internet permission and never uploads anything. Exports and backups are saved wherever you choose.",
        "データはすべてこの端末の中だけに保存されます。アプリはインターネット権限を要求せず、何も送信しません。書き出しやバックアップの保存先は自分で選べます。"
    )

    // ---- 开源许可 ----
    fun licenseTitle(lang: Lang) = pick(lang, "开源许可", "開源授權", "Open-source licences", "オープンソースライセンス")

    fun licenseText(lang: Lang) = pick(
        lang,
        "界面用 Jetpack Compose，数据库用 Room，偏好设置用 DataStore，都是 Apache-2.0 许可。图表和图片处理是自己画的，没有引入任何第三方图表库或图片库，所以安装包里没有它们的许可条文要列。",
        "介面用 Jetpack Compose，資料庫用 Room，偏好設定用 DataStore，都是 Apache-2.0 授權。圖表和圖片處理是自己畫的，沒有引入任何第三方圖表庫或圖片庫，所以安裝檔裡沒有它們的授權條文要列。",
        "The interface uses Jetpack Compose, the database uses Room and preferences use DataStore — all Apache-2.0. Charts and image handling are drawn in-house, so no third-party chart or image library ships with the app and there are no extra licence notices to list.",
        "UI は Jetpack Compose、データベースは Room、設定は DataStore を使用しており、いずれも Apache-2.0 です。グラフや画像処理は自前で描いているため、サードパーティのチャート・画像ライブラリは同梱しておらず、追加のライセンス表示はありません。"
    )

    // ---- 检查更新 ----
    /** 「当前版本：1.8（9）」，和关于页用的是同一份数据 */
    fun updateCurrentVersion(lang: Lang, version: String) =
        pickf(lang, "当前版本：%s", "目前版本：%s", "Current version: %s", "現在のバージョン：%s", version)

    fun updateOpenReleases(lang: Lang) =
        pick(lang, "打开浏览器看发布页", "開啟瀏覽器看發佈頁", "Open the releases page in a browser", "ブラウザでリリースページを開く")

    fun updateUrl(lang: Lang) =
        pick(lang, "发布页地址", "發佈頁網址", "Releases page", "リリースページ")

    /** 没有联网权限，所以「检查更新」只能是打开浏览器 */
    fun updateNoNetworkNote(lang: Lang) = pick(
        lang,
        "这个 App 没有申请联网权限，所以没法在应用内自己查新版本：点下面的按钮会交给系统浏览器打开发布页，新版本和安装包都在那里。",
        "這個 App 沒有申請連網權限，所以沒辦法在應用內自己查新版本：點下面的按鈕會交給系統瀏覽器開啟發佈頁，新版本和安裝包都在那裡。",
        "This app has no internet permission, so it cannot check for updates by itself: the button below hands the releases page to your browser, where new versions and the APK are published.",
        "このアプリはインターネット権限を持たないため、アプリ内で更新を確認できません。下のボタンでシステムのブラウザにリリースページを開きます。新しい版と APK はそこで公開しています。"
    )

    fun updateApkNote(lang: Lang) = pick(
        lang,
        "只从上面这个发布页下载安装包；安装时系统会要求你确认来源。",
        "只從上面這個發佈頁下載安裝檔；安裝時系統會要求你確認來源。",
        "Only download the APK from that releases page; Android will ask you to confirm the source when installing.",
        "APK は上記のリリースページからのみ入手してください。インストール時に Android が提供元の確認を求めます。"
    )

    /** 设备上没有任何浏览器能接这个 Intent 时的提示，不崩 */
    fun updateNoBrowser(lang: Lang) = pick(
        lang,
        "这台设备上没有能打开网页的浏览器，请自己手动访问：",
        "這台裝置上沒有能開啟網頁的瀏覽器，請自己手動前往：",
        "No browser on this device can open the page — please visit it manually:",
        "この端末にはページを開けるブラウザがありません。手動でアクセスしてください："
    )

    /** 学期起始日说明：课表的「第几周」靠它换算 */
    fun termStartHint(lang: Lang) = pick(
        lang, "课表的「第几周」用它换算成真实日期；每门课也可以单独填",
        "課表的「第幾週」用它換算成真實日期；每門課也可以單獨填",
        "Turns the timetable's week numbers into real dates; each course can override it",
        "時間割の「第何週」を実際の日付に換算します。授業ごとに上書きもできます"
    )

    /** 上课提醒说明 */
    fun classReminderHint(lang: Lang) = pick(
        lang, "按课表在每节课开始前提醒一次（先填学期起始日，并给课程填好周次）",
        "按課表在每節課開始前提醒一次（先填學期起始日，並給課程填好週次）",
        "Reminds you before each class on your timetable (set the term start date and each course's weeks first)",
        "時間割に沿って授業の前に通知します（先に学期開始日と各授業の週を設定してください）"
    )
}
