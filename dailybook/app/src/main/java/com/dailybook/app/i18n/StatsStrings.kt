package com.dailybook.app.i18n

/**
 * 统计页文案。
 *
 * 约定：每个函数第一个参数都是 [Lang]，四语文案在编译期必须给全（见 [pick] / [pickf]）。
 * 分类名 / 账户名 / emoji 是数据，不进这里；与别处完全相同的通用词直接复用 [AppStrings]。
 */
object StatsStrings {

    /** 页面大标题：与底部标签「统计」是同一份文案 */
    fun title(lang: Lang) = AppStrings.tabStats(lang)

    // ---- 本月概览 ----

    fun expenseCategoryTitle(lang: Lang) = pick(
        lang, "支出分类占比", "支出分類佔比",
        "Spending by category", "支出カテゴリの割合"
    )

    fun noExpenseThisMonth(lang: Lang) = pick(
        lang, "本月还没有支出记录", "本月還沒有支出記錄",
        "No expenses this month yet", "今月の支出はまだありません"
    )

    fun accountTitle(lang: Lang) = pick(
        lang, "账户支出分布", "帳戶支出分布",
        "Spending by account", "口座別の支出"
    )

    fun categoryBudgetTitle(lang: Lang) = pick(
        lang, "分类预算", "分類預算",
        "Category budgets", "カテゴリ予算"
    )

    fun overBudgetNote(lang: Lang) = pick(
        lang,
        "标红的分类已经超支，下个月从这几个下手最省事。",
        "標紅的分類已經超支，下個月從這幾個下手最省事。",
        "Categories in red are over budget — start with these next month.",
        "赤いカテゴリは予算超過です。来月はここから見直すのが近道です。"
    )

    fun dailyExpenseTitle(lang: Lang) = pick(
        lang, "每日支出", "每日支出",
        "Daily spending", "日別の支出"
    )

    // ---- 近 12 个月趋势与年度汇总 ----

    fun yearTrendTitle(lang: Lang) = pick(
        lang, "近 12 个月收支趋势", "近 12 個月收支趨勢",
        "12-month trend", "直近12か月の収支"
    )

    fun noYearRecords(lang: Lang) = pick(
        lang, "最近一年还没有记账记录", "最近一年還沒有記帳記錄",
        "No entries in the past year yet", "この1年の記録はまだありません"
    )

    fun yearSummaryTitle(lang: Lang, year: Int) = pickf(
        lang, "%d 年汇总", "%d 年彙總",
        "%d summary", "%d年の集計",
        year
    )

    fun noThisYearRecords(lang: Lang) = pick(
        lang, "今年还没有记账记录", "今年還沒有記帳記錄",
        "No entries this year yet", "今年の記録はまだありません"
    )

    fun yearExpense(lang: Lang) = pick(lang, "年支出", "年支出", "Year expense", "年間支出")

    fun yearIncome(lang: Lang) = pick(lang, "年收入", "年收入", "Year income", "年間収入")

    fun yearBalance(lang: Lang) = pick(lang, "年结余", "年結餘", "Year balance", "年間収支")

    fun yearCount(lang: Lang, count: Int) = pickf(
        lang, "全年 %d 笔", "全年 %d 筆",
        "%d entries this year", "年間 %d 件",
        count
    )

    /** 接在「全年 N 笔」后面的「 · 花得最多：」，后面紧跟分类 emoji 与分类名 */
    fun topCategoryPrefix(lang: Lang) = pick(
        lang, " · 花得最多：", " · 花得最多：",
        " · Top: ", " · 最多："
    )

    // ---- 累计 ----

    fun cumulative(lang: Lang) = pick(lang, "累计", "累計", "All time", "累計")

    fun noRecordsAtAll(lang: Lang) = pick(
        lang, "还没有任何记账记录", "還沒有任何記帳記錄",
        "No entries yet", "まだ記録がありません"
    )

    fun totalIncome(lang: Lang) = pick(lang, "总收入", "總收入", "Total income", "総収入")

    fun totalExpense(lang: Lang) = pick(lang, "总支出", "總支出", "Total expense", "総支出")

    fun netBalance(lang: Lang) = pick(lang, "净结余", "淨結餘", "Net balance", "純残高")

    fun sinceFirstRecord(lang: Lang, count: Int) = pickf(
        lang,
        "从第一笔记账到现在，共 %d 笔", "從第一筆記帳到現在，共 %d 筆",
        "%d entries since your first one", "最初の記録から今日まで、合計 %d 件",
        count
    )

    fun incomeSourceTitle(lang: Lang) = pick(
        lang, "收入来源占比", "收入來源佔比",
        "Income by source", "収入の内訳"
    )

    // ---- 专注 ----

    fun focusToday(lang: Lang) = pick(lang, "今日", "今日", "Today", "今日")

    fun focusStreak(lang: Lang) = pick(lang, "连续", "連續", "Streak", "連続")

    fun todayFocusMinutes(lang: Lang, minutes: Int) = pickf(
        lang, "今日专注 %d 分钟", "今日專注 %d 分鐘",
        "%d focus minutes today", "今日の集中 %d 分",
        minutes
    )

    fun focusWeekCount(lang: Lang) = pick(lang, "本周次数", "本週次數", "Week count", "今週の回数")

    fun focusWeekDuration(lang: Lang) = pick(lang, "本周时长", "本週時長", "Week time", "今週の時間")

    fun focusMonthDuration(lang: Lang) = pick(lang, "本月时长", "本月時長", "Month time", "今月の時間")

    /** StatBlock 里的短时长：「45 分」 */
    fun focusMinutesShort(lang: Lang, minutes: Int) = pickf(
        lang, "%d 分", "%d 分",
        "%d min", "%d 分",
        minutes
    )

    fun focusMonthSummary(lang: Lang, count: Int, minutes: Int) = pickf(
        lang,
        "本月已完成 %1\$d 次专注 · 累计 %2\$d 分钟（近 12 周）",
        "本月已完成 %1\$d 次專注 · 累計 %2\$d 分鐘（近 12 週）",
        "%1\$d focus sessions this month · %2\$d minutes in total (last 12 weeks)",
        "今月の集中 %1\$d 回 · 累計 %2\$d 分（直近12週）",
        count, minutes
    )

    fun heatmapTitle(lang: Lang) = pick(
        lang, "专注热力图（近 12 周）", "專注熱力圖（近 12 週）",
        "Focus heatmap (last 12 weeks)", "集中ヒートマップ（直近12週）"
    )

    fun heatmapHint(lang: Lang) = pick(
        lang,
        "每一列是一周，从上到下为周一到周日；颜色越深，当天专注的时间越长。",
        "每一列是一週，從上到下為週一到週日；顏色越深，當天專注的時間越長。",
        "Each column is one week, Monday to Sunday from top to bottom; the darker the color, the longer the focus time that day.",
        "各列が1週間で、上から月曜から日曜の順です。色が濃いほど、その日の集中時間が長いことを表します。"
    )

    fun heatmapLess(lang: Lang) = pick(lang, "少", "少", "Less", "少ない")

    fun heatmapMore(lang: Lang) = pick(lang, "多", "多", "More", "多い")

    fun last7DaysTitle(lang: Lang) = pick(
        lang, "最近 7 天完成的专注", "最近 7 天完成的專注",
        "Focus completed in the last 7 days", "直近7日間の集中"
    )

    fun todayDetailTitle(lang: Lang) = pick(
        lang, "今日专注明细", "今日專注明細",
        "Today's focus sessions", "今日の集中の内訳"
    )

    fun noFocusToday(lang: Lang) = pick(
        lang, "今天还没有完成的专注", "今天還沒有完成的專注",
        "No focus sessions completed today", "今日はまだ集中の記録がありません"
    )

    /** 单条专注记录里的时长：「25 分钟」 */
    fun sessionMinutes(lang: Lang, minutes: Int) = pickf(
        lang, "%d 分钟", "%d 分鐘",
        "%d min", "%d 分",
        minutes
    )

    // ---- 预算 ----

    fun overBudget(lang: Lang, amount: String) = pickf(
        lang, "超支 ¥%s", "超支 ¥%s",
        "Over by ¥%s", "¥%s 超過",
        amount
    )

    fun remainingBudget(lang: Lang, amount: String) = pickf(
        lang, "还可花 ¥%s", "還可花 ¥%s",
        "¥%s left", "あと ¥%s",
        amount
    )

    fun usedOfBudget(lang: Lang, used: String, budget: String) = pickf(
        lang,
        "已用 ¥%1\$s / 预算 ¥%2\$s", "已用 ¥%1\$s / 預算 ¥%2\$s",
        "¥%1\$s used of ¥%2\$s", "¥%1\$s / 予算 ¥%2\$s",
        used, budget
    )

    // ---- 每日专注目标 ----

    fun focusGoalTitle(lang: Lang) =
        pick(lang, "今日目标", "今日目標", "Today's goal", "今日の目標")

    fun focusGoalProgress(lang: Lang, done: Int, goal: Int) = pickf(
        lang, "已完成 %1\$d / %2\$d 个番茄", "已完成 %1\$d / %2\$d 個番茄",
        "%1\$d of %2\$d pomodoros", "%1\$d / %2\$d ポモドーロ", done, goal
    )

    fun focusGoalReached(lang: Lang) =
        pick(lang, "目标达成，干得漂亮 🎉", "目標達成，漂亮 🎉", "Goal reached — well done 🎉", "目標達成、お見事 🎉")

    // ---- 按待办统计投入时间 ----

    fun perTodoTitle(lang: Lang) =
        pick(lang, "按待办统计投入时间", "依待辦統計投入時間", "Time spent per to-do", "ToDo 別の集中時間")

    fun noPerTodo(lang: Lang) = pick(
        lang, "还没有关联待办的专注记录", "還沒有關聯待辦的專注紀錄",
        "No focus sessions linked to a to-do yet", "ToDo に紐づいた集中記録はまだありません"
    )

    fun perTodoHint(lang: Lang) = pick(
        lang, "只统计设了「专注目标」的那些待办。",
        "只統計設了「專注目標」的那些待辦。",
        "Only sessions started from a to-do (focus target) are counted.",
        "「集中目標」に設定した ToDo からの記録だけを集計します。"
    )

    fun perTodoMinutes(lang: Lang, minutes: Int, count: Int) = pickf(
        lang, "%1\$d 分钟 · %2\$d 次", "%1\$d 分鐘 · %2\$d 次",
        "%1\$d min · %2\$d sessions", "%1\$d 分 · %2\$d 回", minutes, count
    )

    // ---- 待报销（全部时间，与当前月份无关）----

    fun reimbursementTitle(lang: Lang) = pick(
        lang, "待报销", "待報銷", "Reimbursement", "精算"
    )

    fun pendingReimbursementLabel(lang: Lang) = pick(
        lang, "待报销合计", "待報銷合計", "Pending total", "精算待ちの合計"
    )

    fun reimbursedLabel(lang: Lang) = pick(
        lang, "已报销", "已報銷", "Reimbursed", "精算済み"
    )

    /** 待报销金额与笔数：「¥320.00（3 笔）」，金额由调用方 formatAmount 后传入 */
    fun reimbursementAmountWithCount(lang: Lang, amount: String, count: Int) = pickf(
        lang,
        "¥%1\$s（%2\$d 笔）", "¥%1\$s（%2\$d 筆）",
        "¥%1\$s (%2\$d entries)", "¥%1\$s（%2\$d 件）",
        amount, count
    )

    /** 已报销金额：「¥320.00」 */
    fun reimbursementAmount(lang: Lang, amount: String) = pickf(
        lang, "¥%s", "¥%s", "¥%s", "¥%s",
        amount
    )

    /** 说明：统计口径是「全部时间」，且已报销的不再计入待报销合计 */
    fun reimbursementNote(lang: Lang) = pick(
        lang,
        "统计全部时间（不限本月）里标记为「待报销」的记录；已经报销掉的部分只算进「已报销」，不再计入合计。",
        "統計全部時間（不限本月）裡標記為「待報銷」的記錄；已經報銷掉的部分只算進「已報銷」，不再計入合計。",
        "Counts every entry flagged for reimbursement across all time, not just this month. Already reimbursed entries are counted only under \"Reimbursed\".",
        "今月に限らず全期間の「精算待ち」を集計します。精算済みの分は「精算済み」にのみ含まれ、合計には入りません。"
    )

    // ---- 专注明细：中断标记 ----

    /** 中途停止 / 跳过的专注记录后面挂的小标签 */
    fun sessionInterrupted(lang: Lang) = pick(
        lang, "中断", "中斷", "Interrupted", "中断"
    )

    // ==================== v1.7：环比对比 ====================

    fun comparisonTitle(lang: Lang) = pick(
        lang, "环比对比", "環比對比",
        "Month on month", "前月・前年との比較"
    )

    /** 上月 / 去年都还没有记录时，环比卡片不硬报百分比，改说这句 */
    fun comparisonNoBase(lang: Lang) = pick(
        lang, "还没有上个月或去年的记录可以对比", "還沒有上個月或去年的記錄可以對比",
        "Nothing from last month or last year to compare with yet",
        "先月・昨年の記録がまだないため比較できません"
    )

    /** 环比行里「当前值」的标签：「本月支出」 */
    fun thisMonthExpense(lang: Lang) = pick(
        lang, "本月支出", "本月支出",
        "This month", "今月の支出"
    )

    fun thisMonthIncome(lang: Lang) = pick(
        lang, "本月收入", "本月收入",
        "This month", "今月の収入"
    )

    /** 环比行里「对比基准」的标签：「上月支出」 */
    fun lastMonthExpense(lang: Lang) = pick(
        lang, "上月支出", "上月支出",
        "Last month", "先月の支出"
    )

    fun lastMonthIncome(lang: Lang) = pick(
        lang, "上月收入", "上月收入",
        "Last month", "先月の収入"
    )

    /** 今年 / 去年支出直接复用 [AppStrings.yearExpense]，这里只补「去年」这一侧 */
    fun lastYearExpense(lang: Lang) = pick(
        lang, "去年支出", "去年支出",
        "Last year", "昨年の支出"
    )

    /**
     * 涨跌文案：没有可比基数（去年 / 上月为 0）时给「基本持平」。
     * 界面层只管把百分比传进来，正负号与四语说法都在这里收口。
     */
    fun compareDelta(lang: Lang, percent: Int?): String = when {
        percent == null -> AppStrings.comparisonFlat(lang)
        percent > 0 -> AppStrings.comparisonUp(lang, percent)
        percent < 0 -> AppStrings.comparisonDown(lang, -percent)
        else -> AppStrings.comparisonFlat(lang)
    }

    // ==================== v1.7：智能洞察 ====================

    fun insightTitle(lang: Lang) = pick(
        lang, "智能洞察", "智慧洞察",
        "Insights", "インサイト"
    )

    /** 有洞察条目但都拼不出句子时的兜底文案 */
    fun insightNoData(lang: Lang) = pick(
        lang, "这个月还看不出什么趋势", "這個月還看不出什麼趨勢",
        "Not enough data for insights this month", "今月はまだ傾向を出せるだけのデータがありません"
    )

    /**
     * 「比上月多花了 …，主要在…」在没有具体分类时的说法。
     * 数据层只在「多花的钱能落到某个分类」时才填 category，
     * 所以这句是 [AppStrings.insightSpentMore] 的兜底版本，不是重复文案。
     */
    fun insightSpentMoreNoCategory(lang: Lang, amount: String) = pickf(
        lang, "比上月多花了 ¥%s", "比上月多花了 ¥%s",
        "Spent ¥%s more than last month", "先月より ¥%s 多く使いました",
        amount
    )

    // ==================== v1.7：月报导出 ====================

    /** 月报小节标题：概览 */
    fun reportSummarySection(lang: Lang) = pick(
        lang, "本月概览", "本月概覽",
        "Monthly summary", "今月の概要"
    )

    /** 概览下面是哪个月的哪一份收入支出 */
    fun reportSummaryLine(lang: Lang, yearMonth: String) = pickf(
        lang, "%s 的收入与支出", "%s 的收入與支出",
        "Income and spending for %s", "%s の収入と支出",
        yearMonth
    )

    fun reportCategorySection(lang: Lang) = pick(
        lang, "支出分类", "支出分類",
        "Spending by category", "支出カテゴリ"
    )

    fun reportDailySection(lang: Lang) = pick(
        lang, "每日支出", "每日支出",
        "Daily spending", "日別の支出"
    )

    /** 每日柱状图每一根柱子下面的说明 */
    fun reportDailyBar(lang: Lang, day: Int, amount: String) = pickf(
        lang, "%1\$d 日 · ¥%2\$s", "%1\$d 日 · ¥%2\$s",
        "Day %1\$d · ¥%2\$s", "%1\$d日 · ¥%2\$s",
        day, amount
    )

    fun reportTopCategoryLabel(lang: Lang) = pick(
        lang, "花得最多", "花得最多",
        "Top category", "最多カテゴリ"
    )

    fun reportFocusSection(lang: Lang) = pick(
        lang, "专注统计", "專注統計",
        "Focus", "集中"
    )

    /** 月报里的专注时长与次数：「本月 12 次 · 共 300 分钟」 */
    fun reportFocusLine(lang: Lang, count: Int, minutes: Int) = pickf(
        lang,
        "本月 %1\$d 次 · 共 %2\$d 分钟", "本月 %1\$d 次 · 共 %2\$d 分鐘",
        "%1\$d sessions this month · %2\$d minutes in total",
        "今月 %1\$d 回 · 合計 %2\$d 分",
        count, minutes
    )

    fun reportInsightSection(lang: Lang) = pick(
        lang, "智能洞察", "智慧洞察",
        "Insights", "インサイト"
    )

    fun reportCompareSection(lang: Lang) = pick(
        lang, "环比变化", "環比變化",
        "Compared with before", "前期間との比較"
    )

    /** 月报里没有图表数据时的兜底说明 */
    fun reportEmptySection(lang: Lang) = pick(
        lang, "这个月没有相关记录", "這個月沒有相關記錄",
        "No records for this month", "今月は該当する記録がありません"
    )

    /** 导出卡片里的一句说明：文件落到用户自己挑的位置，App 不申请存储权限 */
    fun reportExportHint(lang: Lang) = pick(
        lang,
        "导出的文件会保存到你选择的位置，App 不申请任何存储权限；HTML 与图片都是离线可读的。",
        "匯出的檔案會儲存到你選擇的位置，App 不申請任何儲存權限；HTML 與圖片都是離線可讀的。",
        "The file is saved wherever you choose — the app needs no storage permission. The HTML and image versions work offline.",
        "書き出したファイルは選んだ場所に保存されます。アプリはストレージ権限を必要としません。HTML と画像はオフラインでも閲覧できます。"
    )

    /** 导出失败时的提示语；成功用 [AppStrings.reportSaved] */
    fun reportExportFailed(lang: Lang, reason: String) = pickf(
        lang,
        "月报导出失败：%s", "月報匯出失敗：%s",
        "Could not export the report: %s", "レポートを書き出せませんでした：%s",
        reason
    )

    /** 月报落盘时的默认文件名（不含扩展名），英文名不带空格方便分享 */
    fun reportFileName(lang: Lang, appName: String, yearMonth: String): String =
        if (lang == Lang.EN) "$appName-report-$yearMonth" else "$appName-月报-$yearMonth"
}
