package com.dailybook.app.i18n

/**
 * 记账页文案：流水列表、月度汇总、预算卡、记一笔 / 编辑记录弹窗。
 *
 * 约定：每个函数第一个参数都是 [Lang]，四语文案在编译期必须给全（见 [pick] / [pickf]）。
 * 分类名、账户名是**数据**不是界面文案，不进这里；通用动作（保存 / 取消 / 删除）走 [AppStrings]。
 */
object LedgerStrings {

    // ---- 顶部与工具条 ----
    fun addEntry(lang: Lang) = pick(lang, "记一笔", "記一筆", "Add entry", "記録する")
    fun searchHint(lang: Lang) = pick(lang, "搜索分类或备注", "搜尋分類或備註", "Search category or note", "カテゴリ・メモを検索")
    fun accountFilterAll(lang: Lang) = pick(lang, "全部账户", "全部帳戶", "All accounts", "すべての口座")

    // ---- 空状态 ----
    fun emptySearchTitle(lang: Lang) = pick(lang, "没有匹配的记录", "沒有符合的紀錄", "No matching entries", "一致する記録がありません")
    fun emptySearchSubtitle(lang: Lang) = pick(lang, "换个关键词试试", "換個關鍵字試試", "Try another keyword", "別のキーワードを試してください")
    fun emptyFilteredTitle(lang: Lang, account: String) = pickf(
        lang,
        "$account 这个月没有记录", "$account 這個月沒有紀錄",
        "No $account entries this month", "$account は今月の記録がありません",
        account
    )
    fun emptyFilteredSubtitle(lang: Lang) = pick(
        lang,
        "点上面的「全部账户」看全部流水", "點上面的「全部帳戶」看全部流水",
        "Tap \"All accounts\" above to see every entry", "上の「すべての口座」で全記録を表示"
    )
    fun emptyMonthTitle(lang: Lang) = pick(lang, "这个月还没有记账", "這個月還沒有記帳", "Nothing recorded this month", "今月はまだ記録がありません")
    fun emptyMonthSubtitle(lang: Lang) = pick(
        lang,
        "点右下角「记一笔」开始", "點右下角的「記一筆」開始",
        "Tap \"Add entry\" at the bottom right to start", "右下の「記録する」から始めましょう"
    )

    // ---- 月度汇总卡 ----
    fun balanceThisMonth(lang: Lang) = pick(lang, "本月结余", "本月結餘", "Balance this month", "今月の収支")
    fun statTodayExpense(lang: Lang) = pick(lang, "今日支出", "今日支出", "Spent today", "今日の支出")
    fun monthCount(lang: Lang, count: Int) = pickf(
        lang,
        "本月共 %d 笔记录", "本月共 %d 筆紀錄",
        "%d entries this month", "今月は %d 件"
    )

    // ---- 预算卡 ----
    fun budgetThisMonth(lang: Lang) = pick(lang, "本月预算", "本月預算", "Budget this month", "今月の予算")
    fun overBudget(lang: Lang, amount: String) = pickf(
        lang,
        "已超支 ¥%s", "已超支 ¥%s",
        "Over by ¥%s", "¥%s 超過"
    )
    fun budgetRemaining(lang: Lang, amount: String) = pickf(
        lang,
        "剩余 ¥%s", "剩餘 ¥%s",
        "¥%s left", "残り ¥%s"
    )
    fun budgetUsed(lang: Lang, used: String, budget: String) = pickf(
        lang,
        "已用 ¥%1\$s / ¥%2\$s", "已用 ¥%1\$s / ¥%2\$s",
        "¥%1\$s of ¥%2\$s used", "使用済み ¥%1\$s / ¥%2\$s"
    )

    // ---- 日期分组标题 ----
    fun statSpent(lang: Lang, amount: String) = pickf(
        lang,
        "支 ¥%s", "支 ¥%s",
        "Spent ¥%s", "支出 ¥%s"
    )
    fun statReceived(lang: Lang, amount: String) = pickf(
        lang,
        "收 ¥%s", "收 ¥%s",
        "In ¥%s", "収入 ¥%s"
    )

    // ---- 流水行与删除确认 ----
    fun deleteTitle(lang: Lang) = pick(lang, "删除这条记录？", "刪除這筆紀錄？", "Delete this entry?", "この記録を削除しますか？")
    fun deleteBody(lang: Lang, category: String, amount: String, note: String) = pickf(
        lang,
        if (note.isEmpty()) "$category ¥%s" else "$category ¥%s（$note）",
        if (note.isEmpty()) "$category ¥%s" else "$category ¥%s（$note）",
        if (note.isEmpty()) "$category ¥%s" else "$category ¥%s ($note)",
        if (note.isEmpty()) "$category ¥%s" else "$category ¥%s（$note）"
    )

    // ---- 记一笔 / 编辑记录弹窗 ----
    fun editEntry(lang: Lang) = pick(lang, "编辑记录", "編輯紀錄", "Edit entry", "記録を編集")
    fun amountLabel(lang: Lang) = pick(lang, "金额", "金額", "Amount", "金額")
    fun categoryLabel(lang: Lang) = pick(lang, "分类", "分類", "Category", "カテゴリ")
    fun dateLabel(lang: Lang) = pick(lang, "日期", "日期", "Date", "日付")
    fun accountLabel(lang: Lang) = pick(lang, "账户", "帳戶", "Account", "口座")
    fun pickDate(lang: Lang) = pick(lang, "选日期", "選日期", "Pick date", "日付を選ぶ")
    fun customAccountChip(lang: Lang) = pick(lang, "＋ 自定义", "＋ 自訂", "＋ Custom", "＋ カスタム")
    fun noteOptional(lang: Lang) = pick(lang, "备注（可选）", "備註（選填）", "Note (optional)", "メモ（任意）")
    fun enterAmount(lang: Lang) = pick(lang, "请输入金额", "請輸入金額", "Enter an amount", "金額を入力してください")
    fun saveAmount(lang: Lang, amount: String) = pickf(
        lang,
        "保存  ¥%s", "儲存  ¥%s",
        "Save  ¥%s", "保存  ¥%s"
    )
    fun saveChangesAmount(lang: Lang, amount: String) = pickf(
        lang,
        "保存修改  ¥%s", "儲存修改  ¥%s",
        "Save changes  ¥%s", "変更を保存  ¥%s"
    )

    // ---- 自定义账户弹窗 ----
    fun customAccountTitle(lang: Lang) = pick(lang, "自定义账户", "自訂帳戶", "Custom account", "カスタム口座")
    fun accountNameLabel(lang: Lang) = pick(lang, "账户名", "帳戶名稱", "Account name", "口座名")
    fun accountNamePlaceholder(lang: Lang) = pick(
        lang,
        "例如：招商银行、饭卡", "例如：招商銀行、飯卡",
        "e.g. China Merchants Bank, meal card", "例：みずほ銀行、食堂カード"
    )
    fun accountNameHint(lang: Lang) = pick(
        lang,
        "账户名会跟着记录一起保存，可以随时按账户筛选和统计。",
        "帳戶名稱會跟著紀錄一起儲存，可以隨時依帳戶篩選和統計。",
        "The account name is saved with each entry, so you can filter and total by account at any time.",
        "口座名は記録と一緒に保存され、いつでも口座ごとに絞り込み・集計できます。"
    )
}
