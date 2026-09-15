package com.dailybook.app.i18n

/**
 * 生活模块文案（习惯打卡 / 备忘录 / 大事记 / 重要日期）。
 *
 * 约定同 [AppStrings]：每个函数第一个参数都是 [Lang]，四语文案在编译期必须给全（见 [pick] / [pickf]）。
 * 能复用 [AppStrings] 的词（保存 / 取消 / 删除 / 添加 / 清除 / 今天 / 昨天 / 选择 / 可选 / 未设置）不在这里重复定义。
 *
 * 注意：习惯名、备忘录标题、单位（次 / 个 / 页 / 分钟）、分类名都是**数据**，不进这里。
 * 农历月名与日名（正月初一）由 [com.dailybook.app.util.Lunar] 直接给出，同样属于数据，也不翻译。
 *
 * ⚠️ 用 [pickf] 的函数，模板里每个 %s / %d 都要有对应的尾参——
 * 测试 I18nSmokeTest 会把每个文案函数在四种语言下各跑一遍，少传参就是运行期崩。
 */
object LifeStrings {

    // ============================================================
    // 习惯打卡
    // ============================================================

    /** 页面标题（[AppStrings] 里只有模块名，没有这个子页面的名字） */
    fun habitsTitle(lang: Lang) = pick(lang, "习惯打卡", "習慣打卡", "Habit check-in", "習慣チェックイン")

    /** 顶部概览：今天完成了几个 / 一共几个 */
    fun habitsCounts(lang: Lang, done: Int, total: Int) = pickf(
        lang, "今日已完成 %d / %d 个习惯", "今日已完成 %d / %d 個習慣",
        "%d of %d habits done today", "今日は %d / %d 個の習慣を達成", done, total
    )

    fun habitAdd(lang: Lang) = pick(lang, "添加习惯", "新增習慣", "Add habit", "習慣を追加")

    fun habitsEmpty(lang: Lang) = pick(
        lang, "还没有习惯，添加一个开始打卡", "還沒有習慣，新增一個開始打卡",
        "No habits yet — add one to start checking in", "習慣がまだありません。1 つ追加して記録を始めましょう"
    )

    fun habitsEmptyHint(lang: Lang) = pick(
        lang, "每天点一下就行，连续天数会自动累计",
        "每天點一下就行，連續天數會自動累計",
        "One tap a day is enough — your streak builds itself",
        "1 日 1 タップで十分です。連続日数は自動で貯まります"
    )

    /** 行上的说明：点主体部分 = 打卡开关，右侧 ± 调数量 */
    fun habitsRowHint(lang: Lang) = pick(
        lang, "点一下完成打卡，右侧 ＋ / － 调整今天的数量",
        "點一下完成打卡，右側 ＋ / － 調整今天的數量",
        "Tap to check in; use ＋ / － on the right to adjust today's count",
        "タップで達成、右側の ＋ / － で今日の回数を調整"
    )

    /** 进度：「12 / 20 次」 */
    fun habitProgress(lang: Lang, done: Int, target: Int, unit: String) = pickf(
        lang, "%d / %d %s", "%d / %d %s", "%d / %d %s", "%d / %d %s",
        done, target, unit
    )

    fun habitStreak(lang: Lang, days: Int) =
        pickf(lang, "连续 %d 天", "連續 %d 天", "%d-day streak", "連続 %d 日", days)

    fun habitWeekDone(lang: Lang, days: Int) =
        pickf(lang, "本周 %d 天", "本週 %d 天", "%d days this week", "今週 %d 日", days)

    /** 本周目标天数（[com.dailybook.app.data.HabitEntity.daysPerWeek]）的进度：「本周 2 / 3 天」 */
    fun habitWeekProgress(lang: Lang, done: Int, target: Int) = pickf(
        lang, "本周 %d / %d 天", "本週 %d / %d 天",
        "%d / %d days this week", "今週 %d / %d 日", done, target
    )

    /** 本周目标天数已经达标（只在 daysPerWeek > 0 时才会出现） */
    fun habitWeekGoalReached(lang: Lang) = pick(
        lang, "本周目标已达成", "本週目標已達成",
        "Weekly target reached", "今週の目標を達成"
    )

    /** 今天的数量再减就是 0 了（＋ / － 的减号禁用时的无障碍说明） */
    fun habitDecrease(lang: Lang) = pick(lang, "减少一次", "減少一次", "Decrease", "1 回減らす")

    fun habitIncrease(lang: Lang) = pick(lang, "增加一次", "增加一次", "Increase", "1 回増やす")

    fun habitEdit(lang: Lang) = pick(lang, "编辑习惯", "編輯習慣", "Edit habit", "習慣を編集")

    fun habitFieldName(lang: Lang) = pick(lang, "习惯名称", "習慣名稱", "Habit name", "習慣の名前")

    fun habitFieldEmoji(lang: Lang) = pick(lang, "图标", "圖示", "Icon", "アイコン")

    fun habitFieldTarget(lang: Lang) = pick(lang, "每天目标量", "每日目標量", "Daily target", "1 日の目標量")

    fun habitFieldUnit(lang: Lang) = pick(lang, "单位", "單位", "Unit", "単位")

    fun habitFieldDaysPerWeek(lang: Lang) = pick(
        lang, "每周目标天数", "每週目標天數", "Target days per week", "週の目標日数"
    )

    fun habitDeleteTitle(lang: Lang, name: String) = pickf(
        lang, "删除习惯「%s」？", "刪除習慣「%s」？",
        "Delete the habit \"%s\"?", "習慣「%s」を削除しますか？", name
    )

    fun habitDeleteText(lang: Lang) = pick(
        lang, "这个习惯的全部打卡记录也会一起删除，且无法恢复。",
        "這個習慣的全部打卡記錄也會一起刪除，且無法復原。",
        "All check-in records of this habit are deleted too. This cannot be undone.",
        "この習慣のチェックイン記録もすべて削除され、元に戻せません。"
    )

    // ============================================================
    // 备忘录
    // ============================================================

    /** 页面标题 */
    fun memosTitle(lang: Lang) = pick(lang, "备忘录", "備忘錄", "Memos", "メモ")

    fun memosCounts(lang: Lang, total: Int, pinned: Int) = pickf(
        lang, "共 %d 条 · 置顶 %d 条", "共 %d 條 · 置頂 %d 條",
        "%d memos · %d pinned", "全 %d 件・ピン留め %d 件", total, pinned
    )

    fun memosEmpty(lang: Lang) = pick(
        lang, "还没有备忘，随手记一条试试", "還沒有備忘，隨手記一條試試",
        "No memos yet — jot one down", "メモがまだありません。1 件書いてみましょう"
    )

    fun memosEmptyHint(lang: Lang) = pick(
        lang, "点一张卡片看全文；长按或右上角 ⋮ 可以置顶、删除",
        "點一張卡片看全文；長按或右上角 ⋮ 可以置頂、刪除",
        "Tap a card to read it; long-press or use ⋮ to pin or delete",
        "カードをタップで全文表示。長押しまたは ⋮ でピン留め・削除"
    )

    /** 列表页顶部提示（和空状态里的说明分开写，避免同一句话在一屏里出现两次） */
    fun memosHint(lang: Lang) = pick(
        lang, "点一张卡片看全文；长按或卡片右上角 ⋮ 可以置顶、编辑、删除",
        "點一張卡片看全文；長按或卡片右上角 ⋮ 可以置頂、編輯、刪除",
        "Tap a card to read it; long-press or use ⋮ on a card to pin, edit, or delete",
        "カードをタップで全文表示。長押しまたはカード右上の ⋮ でピン留め・編集・削除"
    )

    fun memoAdd(lang: Lang) = pick(lang, "新建备忘", "新增備忘", "New memo", "メモを追加")

    fun memoEdit(lang: Lang) = pick(lang, "编辑备忘", "編輯備忘", "Edit memo", "メモを編集")

    fun memoFieldTitle(lang: Lang) = pick(lang, "标题", "標題", "Title", "タイトル")

    fun memoFieldContent(lang: Lang) = pick(lang, "内容", "內容", "Content", "内容")

    fun memoPin(lang: Lang) = pick(lang, "置顶", "置頂", "Pin", "ピン留め")

    fun memoUnpin(lang: Lang) = pick(lang, "取消置顶", "取消置頂", "Unpin", "ピン留めを解除")

    /** 卡片角上的置顶角标 */
    fun memoPinnedBadge(lang: Lang) = pick(lang, "📌 置顶", "📌 置頂", "📌 Pinned", "📌 ピン留め")

    fun memoDeleteTitle(lang: Lang, title: String) = pickf(
        lang, "删除备忘「%s」？", "刪除備忘「%s」？",
        "Delete the memo \"%s\"?", "メモ「%s」を削除しますか？", title
    )

    fun memoDeleteText(lang: Lang) = pick(
        lang, "这条备忘会被删除，且无法恢复。",
        "這條備忘會被刪除，且無法復原。",
        "This memo will be deleted. This cannot be undone.",
        "このメモは削除され、元に戻せません。"
    )

    fun memoUpdatedAt(lang: Lang) =
        pick(lang, "更新于", "更新於", "Updated", "更新")

    // ============================================================
    // 大事记
    // ============================================================

    /** 页面标题 */
    fun milestonesTitle(lang: Lang) = pick(lang, "大事记", "大事記", "Milestones", "記録")

    fun milestonesCounts(lang: Lang, total: Int) = pickf(
        lang, "共 %d 条大事记", "共 %d 條大事記",
        "%d milestones", "全 %d 件の記録", total
    )

    fun milestonesEmpty(lang: Lang) = pick(
        lang, "还没有大事记，记下第一件事", "還沒有大事記，記下第一件事",
        "No milestones yet — record the first one", "記録がまだありません。最初の 1 件を残しましょう"
    )

    fun milestonesEmptyHint(lang: Lang) = pick(
        lang, "毕业、入职、第一次旅行，都值得留在时间线上",
        "畢業、入職、第一次旅行，都值得留在時間線上",
        "Graduations, first jobs, first trips — all worth a place on the timeline",
        "卒業、入社、初めての旅行。タイムラインに残す価値があります"
    )

    fun milestoneAdd(lang: Lang) = pick(lang, "添加大事记", "新增大事記", "Add milestone", "記録を追加")

    fun milestoneEdit(lang: Lang) = pick(lang, "编辑大事记", "編輯大事記", "Edit milestone", "記録を編集")

    fun milestoneFieldTitle(lang: Lang) = pick(lang, "标题", "標題", "Title", "タイトル")

    fun milestoneFieldNote(lang: Lang) = pick(lang, "备注", "備註", "Note", "メモ")

    fun milestoneFieldDate(lang: Lang) = pick(lang, "日期", "日期", "Date", "日付")

    /**
     * 配图标记。
     * 这里只显示「有没有图」，不显示图片本身：本项目没有引入任何图片加载库
     * （Coil / Glide 都没有，也不允许为这一个界面新增依赖），
     * 而 imageUri 是 SAF 授权后的字符串，直接按文件路径解 Bitmap 既拿不到内容也容易踩权限问题。
     */
    fun milestoneHasImage(lang: Lang) = pick(
        lang, "🖼 已附图片", "🖼 已附圖片", "🖼 Image attached", "🖼 画像あり"
    )

    fun milestoneDeleteTitle(lang: Lang, title: String) = pickf(
        lang, "删除大事记「%s」？", "刪除大事記「%s」？",
        "Delete the milestone \"%s\"?", "記録「%s」を削除しますか？", title
    )

    fun milestoneDeleteText(lang: Lang) = pick(
        lang, "这条大事记会从时间线上移除，且无法恢复。",
        "這條大事記會從時間線上移除，且無法復原。",
        "This milestone is removed from the timeline. This cannot be undone.",
        "この記録はタイムラインから削除され、元に戻せません。"
    )

    // ============================================================
    // 重要日期 / 倒计时
    // ============================================================

    /** 页面标题 */
    fun datesTitle(lang: Lang) = pick(
        lang, "重要日期", "重要日期", "Important dates", "大切な日付"
    )

    fun datesHeroLabel(lang: Lang) = pick(
        lang, "最近的纪念日", "最近的紀念日", "Next up", "次の記念日"
    )

    /** 非复数语种也说「N 天」，不需要区分 1 天 / N 天 */
    fun dateDaysLeft(lang: Lang, days: Long) =
        pickf(lang, "%d 天", "%d 天", "%d days", "%d 日", days)

    fun dateToday(lang: Lang) = pick(lang, "就是今天", "就是今天", "Today", "今日です")

    /**
     * 「只过一次」又已经过完的日子：**已过去 N 天**。
     *
     * 这类日期之前会凭空消失（[com.dailybook.app.data.DateRepeat.ONCE] 一过期就算不出下一次），
     * 现在留在列表里明确标成已过去，既不假装它还在倒计时，也不会让用户以为数据丢了。
     */
    fun dateDaysPassed(lang: Lang, days: Long) =
        pickf(lang, "已过去 %d 天", "已過去 %d 天", "%d days ago", "%d 日前", days)

    /** 列表里「已过去」那一组的小标题 */
    fun datesPastTitle(lang: Lang) = pick(
        lang, "已经过去", "已經過去", "Already passed", "過ぎた日付"
    )

    fun datesEmpty(lang: Lang) = pick(
        lang, "还没有重要日期，添加一个开始倒计时",
        "還沒有重要日期，新增一個開始倒數",
        "No important dates yet — add one to start counting down",
        "重要な日付がまだありません。追加してカウントダウンを始めましょう"
    )

    fun datesEmptyHint(lang: Lang) = pick(
        lang, "支持农历，生日和纪念日都能每年自动提醒",
        "支援農曆，生日和紀念日都能每年自動提醒",
        "Lunar dates supported — birthdays and anniversaries recur every year",
        "旧暦にも対応。誕生日や記念日は毎年自動で繰り返します"
    )

    fun datesOtherTitle(lang: Lang) = pick(lang, "其他日子", "其他日子", "More dates", "そのほかの日付")

    fun dateAdd(lang: Lang) = pick(lang, "添加重要日期", "新增重要日期", "Add important date", "重要な日付を追加")

    fun dateEdit(lang: Lang) = pick(lang, "编辑重要日期", "編輯重要日期", "Edit important date", "重要な日付を編集")

    fun dateFieldTitle(lang: Lang) = pick(lang, "名称", "名稱", "Name", "名前")

    fun dateFieldDate(lang: Lang) = pick(lang, "日期", "日期", "Date", "日付")

    fun dateFieldCalendar(lang: Lang) = pick(lang, "历法", "曆法", "Calendar", "暦")

    /** 历法切换：阳历 */
    fun dateSolar(lang: Lang) = pick(lang, "阳历", "陽曆", "Solar", "新暦")

    /** 历法切换：农历（农历本身的名字，四语都写「农历 / 農曆」） */
    fun dateLunarLabel(lang: Lang) = pick(lang, "农历", "農曆", "Lunar", "旧暦")

    fun dateFieldLunarMonth(lang: Lang) = pick(lang, "农历月", "農曆月", "Lunar month", "旧暦の月")

    fun dateFieldLunarDay(lang: Lang) = pick(lang, "农历日", "農曆日", "Lunar day", "旧暦の日")

    fun dateLunarLeap(lang: Lang) = pick(lang, "闰月", "閏月", "Leap month", "閏月")

    fun dateLunarHint(lang: Lang) = pick(
        lang, "闰月不是每年都有，没有的年份按普通月算",
        "閏月不是每年都有，沒有的年份按普通月算",
        "A leap month does not occur every year; years without it fall back to the regular month",
        "閏月は毎年あるとは限りません。ない年は通常の月として扱います"
    )

    /** 弹窗底部说明：提醒与导出（v1.10 起提醒不再只靠导出的 .ics，应用内也会发） */
    fun dateDialogHint(lang: Lang) = pick(
        lang, "农历日期按每年重复算；列表里每条都能导出 .ics 给系统日历，提醒也会在应用内按你设的「提前 N 天」发一次",
        "農曆日期按每年重複算；清單裡每條都能匯出 .ics 給系統行事曆，提醒也會在應用程式內按你設的「提前 N 天」發一次",
        "Lunar dates repeat every year; each entry can also be exported as .ics for your system calendar, and the reminder you set fires inside the app as well",
        "旧暦の日付は毎年繰り返します。各項目は .ics としてシステムのカレンダーに書き出せるほか、設定した「N 日前」の通知はアプリ内でも出ます"
    )

    /**
     * 农历日期换算不出来时的提示：这个月没有这一天。
     *
     * 触发场景：小月只有 29 天，用户却选了「三十」。以前这种情况会**静默**改用阳历选择器的日期，
     * 用户以为记的是农历那天、实际存的是另一天 —— 现在直接说明并且不保存。
     */
    fun dateLunarNotExist(lang: Lang) = pick(
        lang, "这个农历月没有这一天（小月只有 29 天，没有「三十」）—— 请换一天",
        "這個農曆月沒有這一天（小月只有 29 天，沒有「三十」）—— 請換一天",
        "That day does not exist in this lunar month (a short month has only 29 days, so there is no 三十) — please pick another day",
        "この旧暦の月にはその日がありません（小の月は 29 日までで「三十」は存在しません）—— 別の日を選んでください"
    )

    fun dateFieldRepeat(lang: Lang) = pick(lang, "重复", "重複", "Repeat", "繰り返し")

    /** 提醒天数的标签（芯片文案由 [dateRemindValue] 生成） */
    fun dateFieldRemind(lang: Lang) = pick(lang, "提醒", "提醒", "Reminder", "リマインダー")

    fun dateFieldNote(lang: Lang) = pick(lang, "备注", "備註", "Note", "メモ")

    fun dateRemindOnDay(lang: Lang) = pick(lang, "当天提醒", "當天提醒", "On the day", "当日に通知")

    fun dateRemindBefore(lang: Lang, days: Int) = pickf(
        lang, "提前 %d 天", "提前 %d 天",
        "%d days before", "%d 日前に通知", days
    )

    fun dateRepatOnce(lang: Lang) = pick(lang, "只过一次", "只過一次", "Once", "1 回だけ")

    fun dateRepeatYearly(lang: Lang) = pick(lang, "每年", "每年", "Every year", "毎年")

    fun dateRepeatMonthly(lang: Lang) = pick(lang, "每月", "每月", "Every month", "毎月")

    fun dateRepeatWeekly(lang: Lang) = pick(lang, "每周", "每週", "Every week", "毎週")

    /** 农历日期的展示：`农历闰六月十五`（月名 / 日名来自 Lunar 的数据） */
    fun dateLunar(lang: Lang, monthName: String, dayName: String) = pickf(
        lang, "农历%s%s", "農曆%s%s",
        "Lunar %s%s", "旧暦 %s%s", monthName, dayName
    )

    fun dateDeleteTitle(lang: Lang, title: String) = pickf(
        lang, "删除「%s」？", "刪除「%s」？",
        "Delete \"%s\"?", "「%s」を削除しますか？", title
    )

    fun dateDeleteText(lang: Lang) = pick(
        lang, "这个日期和它的提醒会被删除，且无法恢复。",
        "這個日期和它的提醒會被刪除，且無法復原。",
        "This date and its reminder are deleted. This cannot be undone.",
        "この日付とリマインダーは削除され、元に戻せません。"
    )

    // ---- 应用内提醒通知 ----
    // 通知渠道名 / 通知标题这类「不进界面」的文案也放这里（重要日期模块自己的通知）：
    // 渠道的建法与命名风格和 AppStrings 里那几个通知渠道保持一致。

    /** 通知渠道名：重要日期提醒 */
    fun notifChannelDate(lang: Lang) = pick(
        lang, "重要日期提醒", "重要日期提醒", "Important date reminders", "大切な日付の通知"
    )

    /** 通知渠道说明 */
    fun notifChannelDateDesc(lang: Lang) = pick(
        lang, "生日、纪念日这类日子按你设的提前量提醒你",
        "生日、紀念日這類日子按你設的提前量提醒你",
        "Reminds you about birthdays and anniversaries as early as you set",
        "誕生日や記念日を、設定した日数だけ早く知らせます"
    )

    /**
     * 通知标题：%1$s = 日期名（数据，不翻译），%2$d = 提前几天。
     * 日文里「N 日前」放在标题里很别扭，所以日文只说「快到了」，具体天数交给正文。
     */
    fun dateNotifTitle(lang: Lang, title: String, daysBefore: Int) = pickf(
        lang, "%1\$s 还有 %2\$d 天", "%1\$s 還有 %2\$d 天",
        "%1\$s in %2\$d days", "%1\$s が近づいています", title, daysBefore
    )

    /** 「只过一次」的日期没有「还剩几天」的说法，通知标题单独一句 */
    fun dateNotifTitleOnce(lang: Lang, title: String) = pickf(
        lang, "快到了：%s", "快到了：%s",
        "Coming up: %s", "もうすぐ：%s", title
    )

    // ---- 导出 .ics ----

    fun dateExportIcs(lang: Lang) = pick(
        lang, "导出 .ics", "匯出 .ics", "Export .ics", ".ics を書き出す"
    )

    /** 导出成功（%s = 文件名 / 保存位置） */
    fun dateExportDone(lang: Lang, where: String) = pickf(
        lang, "已导出：%s", "已匯出：%s",
        "Exported: %s", "書き出しました：%s", where
    )

    fun dateExportFailed(lang: Lang) = pick(
        lang, "导出失败，请换个位置再试一次",
        "匯出失敗，請換個位置再試一次",
        "Export failed — try another location",
        "書き出しに失敗しました。別の場所でもう一度お試しください"
    )

    /** 导出的日历事件标题（%s = 用户输入的日期名，属数据不翻译） */
    fun dateIcsSummary(lang: Lang, title: String) = pickf(
        lang, "%s（日常本）", "%s（日常本）",
        "%s (DailyBook)", "%s（日常本）", title
    )

    fun dateIcsDescription(lang: Lang, note: String) = pickf(
        lang, "来自日常本的重要日期。备注：%s", "來自日常本的重要日期。備註：%s",
        "Important date from DailyBook. Note: %s", "日常本の重要な日付。メモ：%s", note
    )

    fun dateIcsDescriptionLunar(lang: Lang, lunar: String, note: String) = pickf(
        lang, "来自日常本的重要日期（%s）。备注：%s",
        "來自日常本的重要日期（%s）。備註：%s",
        "Important date from DailyBook (%s). Note: %s",
        "日常本の重要な日付（%s）。メモ：%s", lunar, note
    )

    /** 导出时附带的提醒（VALARM）说明，%s = 日期名 */
    fun dateIcsAlarm(lang: Lang, title: String) = pickf(
        lang, "日常本提醒：%s", "日常本提醒：%s",
        "DailyBook reminder: %s", "日常本のリマインダー：%s", title
    )
}
