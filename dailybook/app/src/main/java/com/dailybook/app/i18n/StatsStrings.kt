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
}
