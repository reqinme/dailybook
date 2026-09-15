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
    /**
     * 搜索框的提示。搜索实际匹配**分类 / 备注 / 标签**三者
     * （见 MainViewModel.buildState 里的 `searched`），所以提示必须把标签也说上 ——
     * 以前只写「分类或备注」，而流水行上明明显示着标签，用户搜标签搜不到就会以为记录丢了。
     */
    fun searchHint(lang: Lang) = pick(
        lang, "搜索分类、备注或标签", "搜尋分類、備註或標籤",
        "Search category, note or tag", "カテゴリ・メモ・タグを検索"
    )
    fun accountFilterAll(lang: Lang) = pick(lang, "全部账户", "全部帳戶", "All accounts", "すべての口座")

    // ---- 空状态 ----
    fun emptySearchTitle(lang: Lang) = pick(lang, "没有匹配的记录", "沒有符合的紀錄", "No matching entries", "一致する記録がありません")
    fun emptySearchSubtitle(lang: Lang) = pick(lang, "换个关键词试试", "換個關鍵字試試", "Try another keyword", "別のキーワードを試してください")
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
        "%d entries this month", "今月は %d 件",
        count
    )

    // ---- 预算卡 ----
    fun budgetThisMonth(lang: Lang) = pick(lang, "本月预算", "本月預算", "Budget this month", "今月の予算")
    fun overBudget(lang: Lang, amount: String) = pickf(
        lang,
        "已超支 ¥%s", "已超支 ¥%s",
        "Over by ¥%s", "¥%s 超過",
        amount
    )
    fun budgetRemaining(lang: Lang, amount: String) = pickf(
        lang,
        "剩余 ¥%s", "剩餘 ¥%s",
        "¥%s left", "残り ¥%s",
        amount
    )
    fun budgetUsed(lang: Lang, used: String, budget: String) = pickf(
        lang,
        "已用 ¥%1\$s / ¥%2\$s", "已用 ¥%1\$s / ¥%2\$s",
        "¥%1\$s of ¥%2\$s used", "使用済み ¥%1\$s / ¥%2\$s",
        used, budget
    )

    // ---- 日期分组标题 ----
    fun statSpent(lang: Lang, amount: String) = pickf(
        lang,
        "支 ¥%s", "支 ¥%s",
        "Spent ¥%s", "支出 ¥%s",
        amount
    )
    fun statReceived(lang: Lang, amount: String) = pickf(
        lang,
        "收 ¥%s", "收 ¥%s",
        "In ¥%s", "収入 ¥%s",
        amount
    )

    // ---- 流水行与删除确认 ----
    fun deleteTitle(lang: Lang) = pick(lang, "删除这条记录？", "刪除這筆紀錄？", "Delete this entry?", "この記録を削除しますか？")
    fun deleteBody(lang: Lang, category: String, amount: String, note: String) =
        if (note.isEmpty()) pickf(
            lang,
            "%1\$s ¥%2\$s", "%1\$s ¥%2\$s",
            "%1\$s ¥%2\$s", "%1\$s ¥%2\$s",
            category, amount
        ) else pickf(
            lang,
            "%1\$s ¥%2\$s（%3\$s）", "%1\$s ¥%2\$s（%3\$s）",
            "%1\$s ¥%2\$s (%3\$s)", "%1\$s ¥%2\$s（%3\$s）",
            category, amount, note
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
        "Save  ¥%s", "保存  ¥%s",
        amount
    )
    fun saveChangesAmount(lang: Lang, amount: String) = pickf(
        lang,
        "保存修改  ¥%s", "儲存修改  ¥%s",
        "Save changes  ¥%s", "変更を保存  ¥%s",
        amount
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

    // ---- v1.6：标签 ----
    fun tagFilterAll(lang: Lang) = pick(lang, "全部标签", "全部標籤", "All tags", "すべてのタグ")
    fun tagsLabel(lang: Lang) = pick(lang, "标签（可选）", "標籤（選填）", "Tags (optional)", "タグ（任意）")
    fun tagsHint(lang: Lang) = pick(
        lang,
        "例如：旅行 报销", "例如：旅行 報銷",
        "e.g. travel, work", "例：旅行 出張"
    )
    fun tagsTip(lang: Lang) = pick(
        lang,
        "空格或逗号分开，写几个都行", "空格或逗號分開，寫幾個都行",
        "Separate with a space or comma, as many as you like", "スペースかカンマで区切って複数書けます"
    )

    // ---- v1.6：多币种 ----
    fun currencyLabel(lang: Lang) = pick(lang, "币种", "幣別", "Currency", "通貨")
    fun amountForeignLabel(lang: Lang, code: String) = pickf(
        lang,
        "金额（%s）", "金額（%s）",
        "Amount (%s)", "金額（%s）",
        code
    )
    fun rateLabel(lang: Lang, code: String) = pickf(
        lang,
        "汇率（1 %s = ? 元）", "匯率（1 %s = ? 元）",
        "Rate (1 %s = ? CNY)", "レート（1 %s = ? 元）",
        code
    )
    fun rateHint(lang: Lang, symbol: String, code: String) = pickf(
        lang,
        "填 %s1 %s 折合多少元人民币", "填 %s1 %s 折合多少元人民幣",
        "How many CNY is %s1 %s worth", "%s1 %s が何元になるか入力",
        symbol, code
    )
    fun rateConverted(lang: Lang, amount: String) = pickf(
        lang,
        "折合 ¥%s", "折合 ¥%s",
        "Equals ¥%s", "¥%s に相当",
        amount
    )
    fun convertedFrom(lang: Lang, symbol: String, amount: String, code: String) = pickf(
        lang,
        "%s%s %s", "%s%s %s",
        "%s%s %s", "%s%s %s",
        symbol, amount, code
    )

    // ---- v1.6：报销 ----
    fun reimbursableLabel(lang: Lang) = pick(lang, "待报销", "待報銷", "Reimbursable", "要精算")
    fun pendingReimbursement(lang: Lang) = pick(lang, "待报销", "待報銷", "Pending", "精算待ち")
    fun reimbursed(lang: Lang) = pick(lang, "已报销", "已報銷", "Reimbursed", "精算済み")

    /**
     * 记账页报销卡上的一句指引。
     *
     * 这一页只留「待报销」一行（金额 + 笔数），已报销合计与统计口径的说明都在统计页的
     * 报销卡片里 —— 同一个总金额在两页各画一张卡，改了一处忘了另一处就会对不上。
     */
    fun reimbursementSeeStats(lang: Lang) = pick(
        lang,
        "只显示待报销；已报销合计与统计口径（不限本月）在「统计」页。",
        "只顯示待報銷；已報銷合計與統計口徑（不限本月）在「統計」頁。",
        "Only the pending total is shown here — the reimbursed total and what the figures cover (all time, not just this month) are on the Stats page.",
        "ここでは精算待ちのみを表示します。精算済みの合計と集計の範囲（今月に限らず全期間）は「統計」ページにあります。"
    )

    // ---- v1.6：日历视图与当天筛选 ----
    fun viewList(lang: Lang) = pick(lang, "列表", "列表", "List", "リスト")
    fun viewCalendar(lang: Lang) = pick(lang, "日历", "日曆", "Calendar", "カレンダー")
    fun clearDayFilter(lang: Lang) = pick(lang, "取消当天筛选", "取消當天篩選", "Clear day filter", "日付の絞り込みを解除")
    fun dayFilterSummary(lang: Lang, count: Int, expense: String, income: String) = pickf(
        lang,
        "%1\$d 笔 · 支 ¥%2\$s · 收 ¥%3\$s", "%1\$d 筆 · 支 ¥%2\$s · 收 ¥%3\$s",
        "%1\$d entries · spent ¥%2\$s · in ¥%3\$s", "%1\$d 件 · 支出 ¥%2\$s · 収入 ¥%3\$s",
        count, expense, income
    )

    // ---- v1.6：筛选后的空状态（不带账户名的通用版，界面统一用它） ----
    fun emptyNoMatchTitle(lang: Lang) = pick(
        lang,
        "没有符合当前筛选的记录", "沒有符合目前篩選的紀錄",
        "No entries match this filter", "現在の絞り込みに合う記録がありません"
    )
    fun emptyNoMatchHint(lang: Lang) = pick(
        lang,
        "清掉筛选看看全部", "清掉篩選看看全部",
        "Clear the filters to see everything", "絞り込みを解除するとすべて表示されます"
    )

    // ==================== v1.8：年 / 月快速切换（MonthYearPicker） ====================
    // 这一组是「年份 + 12 个月」选择器与日历视图提示的文案。
    // 组件放在 ui/MonthYearPicker.kt，记账页与统计页共用，
    // 所以「上个月 / 下个月 / 回本月」仍复用 CommonStrings，这里只放选择器自己的词。

    /** 选择器标题：「选择年月」 */
    fun monthYearTitle(lang: Lang) = pick(
        lang, "选择年月", "選擇年月",
        "Pick a month", "年月を選択"
    )

    /** 选择器底部的「回到本月」——比年月条上的短标签「回本月」更完整 */
    fun monthYearThisMonth(lang: Lang) = pick(
        lang, "回到本月", "回到本月",
        "Back to this month", "今月に戻る"
    )

    /** 年份两侧箭头的无障碍说明 */
    fun prevYear(lang: Lang) = pick(lang, "上一年", "上一年", "Previous year", "前の年")
    fun nextYear(lang: Lang) = pick(lang, "下一年", "下一年", "Next year", "次の年")

    /** 月份按钮上的短月份名：「1月」（英文是 Jan） */
    fun monthShort(lang: Lang, month: Int): String = when (lang) {
        Lang.EN -> MONTH_SHORT_EN[(month - 1).coerceIn(0, 11)]
        else -> pickf(lang, "%d月", "%d月", "%d", "%d月", month)
    }

    /** 手输年份那一栏 */
    fun yearFieldLabel(lang: Lang) = pick(lang, "年份", "年份", "Year", "年")
    fun yearFieldHint(lang: Lang) = pick(
        lang, "例如 2026", "例如 2026",
        "e.g. 2026", "例：2026"
    )

    /** 输入范围外的年份时的提示 */
    fun yearFieldInvalid(lang: Lang, min: Int, max: Int) = pickf(
        lang,
        "请输入 %1\$d - %2\$d 之间的年份", "請輸入 %1\$d - %2\$d 之間的年份",
        "Enter a year between %1\$d and %2\$d", "%1\$d〜%2\$d の年を入力してください",
        min, max
    )

    /** 日历视图里的一句提示：日历在列表最上面，下面的流水可以继续往下滚 */
    fun calendarScrollHint(lang: Lang) = pick(
        lang,
        "点某一天按天筛选，下面的流水可以直接往上滑，日历会跟着滚走。",
        "點某一天依日篩選，下面的流水可以直接往上滑，日曆會跟著捲走。",
        "Tap a day to filter by date — swipe the list below, the calendar scrolls away with it.",
        "日をタップすると日付で絞り込めます。下のリストはそのままスクロールでき、カレンダーも一緒に流れます。"
    )

    private val MONTH_SHORT_EN = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
}
