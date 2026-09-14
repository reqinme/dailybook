package com.dailybook.app.i18n

/**
 * 待办页文案。
 *
 * 约定同 [AppStrings]：每个函数第一个参数都是 [Lang]，四语文案在编译期必须给全。
 * 能复用 [AppStrings] 的词（保存/取消/删除/清除/添加/全部/待完成/已完成/重复规则/日期…）不在这里重复定义。
 * 待办标题、备注这些是用户输入的数据，不进这里。
 */
object TodoStrings {

    // ---- 标题与计数 ----
    fun counts(lang: Lang, pending: Int, done: Int) = pickf(
        lang, "待完成 %d 项 · 已完成 %d 项", "待完成 %d 項 · 已完成 %d 項",
        "%d open · %d done", "未完了 %d 件 · 完了 %d 件", pending, done
    )

    /** 逾期任务一键顺延到明天 */
    fun postponeToTomorrow(lang: Lang) =
        pick(lang, "顺延到明天", "順延到明天", "Move to tomorrow", "明日に延ばす")

    // ---- 顶部操作 ----
    fun clearDone(lang: Lang) = pick(lang, "清除已完成", "清除已完成", "Clear completed", "完了を削除")
    fun addPlaceholder(lang: Lang) = pick(lang, "添加待办…", "新增待辦…", "Add a to-do…", "ToDo を追加…")
    fun addTodo(lang: Lang) = pick(lang, "添加待办", "新增待辦", "Add to-do", "ToDo を追加")

    /** 顶部的一行说明：点开一条待办能设置什么 */
    fun hint(lang: Lang) = pick(
        lang,
        "点开任意一条待办，可以设置到期日、重复规则和提醒",
        "點開任意一條待辦，可以設定到期日、重複規則和提醒",
        "Tap any to-do to set a due date, repeat rule, and reminder",
        "ToDo をタップすると期限・繰り返し・リマインダーを設定できます"
    )

    fun searchPlaceholder(lang: Lang) = pick(lang, "搜索待办", "搜尋待辦", "Search to-dos", "ToDo を検索")

    // ---- 空状态 ----
    fun emptyNoMatch(lang: Lang) = pick(
        lang, "没有匹配的待办", "沒有符合的待辦",
        "No matching to-dos", "一致する ToDo はありません"
    )
    fun emptyNoPending(lang: Lang) = pick(
        lang, "没有未完成的待办，很棒！", "沒有未完成的待辦，太棒了！",
        "Nothing left to do. Nice!", "未完了の ToDo はありません。素晴らしい！"
    )
    fun emptyNoDone(lang: Lang) = pick(
        lang, "还没有完成任何待办", "還沒有完成任何待辦",
        "Nothing done yet", "完了した ToDo はまだありません"
    )
    fun emptyNoTodos(lang: Lang) = pick(
        lang, "还没有待办，添加一条试试", "還沒有待辦，新增一條試試",
        "No to-dos yet — add one", "ToDo がまだありません。1 件追加してみましょう"
    )
    fun emptyTryAnotherKeyword(lang: Lang) = pick(
        lang, "换个关键词试试", "換個關鍵字試試",
        "Try another keyword", "別のキーワードで試してください"
    )

    // ---- 清除已完成 ----
    fun confirmClearDoneTitle(lang: Lang, count: Int) = pickf(
        lang, "清除 %d 条已完成待办？", "清除 %d 條已完成待辦？",
        "Clear %d completed to-dos?", "完了した ToDo を %d 件削除しますか？", count
    )
    fun confirmClearDoneText(lang: Lang) = pick(
        lang,
        "已完成的待办会被删除，且无法恢复。",
        "已完成的待辦會被刪除，且無法復原。",
        "Completed to-dos will be deleted. This cannot be undone.",
        "完了した ToDo は削除され、元に戻せません。"
    )
    fun confirmClearDoneAction(lang: Lang) = pick(
        lang, "确定清除", "確定清除", "Clear now", "削除する"
    )

    // ---- 编辑弹窗 ----
    fun editTitle(lang: Lang) = pick(lang, "编辑待办", "編輯待辦", "Edit to-do", "ToDo を編集")
    fun fieldContent(lang: Lang) = pick(lang, "内容", "內容", "Title", "内容")
    fun importantMarker(lang: Lang) = pick(lang, "标记为重要", "標記為重要", "Mark as important", "重要としてマーク")
    fun dueDateLabel(lang: Lang) = pick(lang, "到期日", "到期日", "Due date", "期限")

    fun repeatLabel(lang: Lang) = pick(lang, "重复", "重複", "Repeat", "繰り返し")

    /** 勾选完成后自动生成下一次（没有到期日） */
    fun autoNextHintNoDue(lang: Lang) = pick(
        lang, "勾选完成后自动生成下一次（没有到期日时按今天起算）",
        "勾選完成後自動產生下一次（沒有到期日時從今天起算）",
        "Checking it off creates the next one (counted from today when there is no due date)",
        "チェックすると次回分を自動で作成します（期限がない場合は今日から計算）"
    )

    /** 勾选完成后自动生成下一次（按到期日顺延） */
    fun autoNextHintDue(lang: Lang) = pick(
        lang, "勾选完成后自动生成下一次（到期日顺延）",
        "勾選完成後自動產生下一次（到期日順延）",
        "Checking it off creates the next one (pushed forward from the due date)",
        "チェックすると次回分を自動で作成します（期限を繰り延べ）"
    )

    fun reminderNotice(lang: Lang) = pick(
        lang,
        "到期日当天上午 9 点会发通知提醒；没完成的逾期任务次日再提醒一次。",
        "到期日當天上午 9 點會發通知提醒；沒完成的逾期任務隔天會再提醒一次。",
        "You get a notification at 9 AM on the due date; an unfinished overdue task is reminded once more the next day.",
        "期限日の午前 9 時に通知します。未完了で期限を過ぎたタスクは翌日にもう一度通知します。"
    )

    // ---- 列表行 ----
    fun focusTarget(lang: Lang) = pick(lang, "🎯 专注目标", "🎯 專注目標", "🎯 Focus goal", "🎯 集中目標")

    /** 重复规则前面的图标前缀（内容本身来自 [RepeatRule.label] / AppStrings.repeat*） */
    @Suppress("UNUSED_PARAMETER")
    fun repeatPrefix(lang: Lang, label: String) = "🔁 $label"

    fun setFocusTarget(lang: Lang) = pick(lang, "设为专注目标", "設為專注目標", "Set as focus goal", "集中目標に設定")
    fun unsetFocusTarget(lang: Lang) = pick(lang, "取消专注目标", "取消專注目標", "Unset focus goal", "集中目標を解除")
    fun important(lang: Lang) = pick(lang, "重要", "重要", "Important", "重要")
}
