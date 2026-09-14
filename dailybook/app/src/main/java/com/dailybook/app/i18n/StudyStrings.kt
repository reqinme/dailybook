package com.dailybook.app.i18n

/**
 * 学习模块（校园）文案：学习首页 / 课表 / 作业 / 考试 / GPA / 学分 / 奖助 / 周报。
 *
 * 约定同 [AppStrings]：每个函数第一个参数都是 [Lang]，四语文案在编译期必须给全（见 [pick] / [pickf]）。
 *
 * 两条硬规则：
 * 1. 课程名、教师、地点、学期标签、类别（必修 / 选修 / 通识）、级别（国家级 / 校级）、
 *    奖助名称这些都是**数据**，不进这里，直接显示原文；
 * 2. `pickf` 模板里每个 `%s` / `%d` 都必须在末尾配上同数量的实参——
 *    I18nSmokeTest 会用反射把每个函数 × 四种语言各跑一遍，漏参数就是构建失败。
 *
 * 与别处完全相同的通用词直接复用 [AppStrings]（例如 tabCourses == tabStudy + 课表），
 * 这样两个入口的标题永远一致。
 */
object StudyStrings {

    // ==================== 学习首页（Tab 内容） ====================

    /** 首页大标题：与底部标签「学习」同一份文案 */
    fun hubTitle(lang: Lang) = AppStrings.tabStudy(lang)

    /** 「9月13日 周六」—— 首页头部的今天 */
    fun hubToday(lang: Lang, date: String, weekday: String) = pickf(
        lang,
        "%1\$s %2\$s", "%1\$s %2\$s",
        "%1\$s · %2\$s", "%1\$s %2\$s",
        date, weekday
    )

    /** 「今日 3 节课」 */
    fun hubTodayCourses(lang: Lang, count: Int) = pickf(
        lang, "今日 %d 节课", "今日 %d 節課",
        "%d classes today", "本日 %d コマ",
        count
    )

    /** 「本周 12 节课」 */
    fun hubWeekCourses(lang: Lang, count: Int) = pickf(
        lang, "本周 %d 节课", "本週 %d 節課",
        "%d classes this week", "今週 %d コマ",
        count
    )

    /** 「考试倒计时 5 天」，考试名是数据 */
    fun hubExamCountdown(lang: Lang, name: String, days: Long) = pickf(
        lang, "%1\$s 还有 %2\$d 天", "%1\$s 還有 %2\$d 天",
        "%1\$s in %2\$d days", "%1\$s まで %2\$d 日",
        name, days
    )

    /** 「考试倒计时 · 今天」 */
    fun hubExamToday(lang: Lang, name: String) = pickf(
        lang, "%s 就是今天", "%s 就是今天",
        "%s is today", "%s は今日",
        name
    )

    /** 「最近没有考试」 */
    fun hubNoExam(lang: Lang) = pick(
        lang, "最近没有安排考试", "最近沒有安排考試",
        "No exams coming up", "予定されている試験はありません"
    )

    /** 「未完成作业 3 条」 */
    fun hubOpenAssignments(lang: Lang, count: Int) = pickf(
        lang, "未完成作业 %d 条", "未完成作業 %d 條",
        "%d assignments open", "未完了の課題 %d 件",
        count
    )

    /** 「其中 2 条已逾期」 */
    fun hubOverdue(lang: Lang, count: Int) = pickf(
        lang, "其中 %d 条已逾期", "其中 %d 條已逾期",
        "%d of them overdue", "うち %d 件が期限超過",
        count
    )

    /** 「GPA 3.62」 */
    fun hubGpa(lang: Lang, value: String) = pickf(
        lang, "GPA %s", "GPA %s",
        "GPA %s", "GPA %s",
        value
    )

    /** 「已修 24.5 学分」 */
    fun hubCredits(lang: Lang, value: String) = pickf(
        lang, "已修 %s 学分", "已修 %s 學分",
        "%s credits earned", "取得単位数 %s",
        value
    )

    /** 概览里的标签：考试倒计时 */
    fun hubExamLabel(lang: Lang) = pick(
        lang, "考试倒计时", "考試倒數",
        "Next exam", "試験まで"
    )

    /** 概览里的标签：未完成作业 */
    fun hubAssignmentLabel(lang: Lang) = pick(
        lang, "未完成作业", "未完成作業",
        "Open assignments", "未完了の課題"
    )

    /** 「3 条」——数字后面带量词，作为数字块的取值 */
    fun hubCountValue(lang: Lang, count: Int) = pickf(
        lang, "%d 条", "%d 條",
        "%d", "%d 件",
        count
    )

    /** 「2 条逾期」 */
    fun hubOverdueValue(lang: Lang, count: Int) = pickf(
        lang, "%d 条逾期", "%d 條逾期",
        "%d overdue", "%d 件超過",
        count
    )

    /** 「5 天」，考试倒计时数字块在没有考试时显示 */
    fun hubDays(lang: Lang, days: Long) = pickf(
        lang, "%d 天", "%d 天",
        "%d d", "%d 日",
        days
    )

    /** 首页入口卡片的分区标题 */
    fun hubEntries(lang: Lang) = pick(
        lang, "全部功能", "全部功能",
        "All sections", "すべての機能"
    )

    /** 首页「今日课程」分区标题 */
    fun hubTodaySection(lang: Lang) = pick(
        lang, "今日课程", "今日課程",
        "Today's classes", "今日の授業"
    )

    /** 「今天没有课，休息一下」 */
    fun hubNoCourseToday(lang: Lang) = pick(
        lang, "今天没有课，休息一下", "今天沒有課，休息一下",
        "No classes today — enjoy the break", "今日は授業がありません"
    )

    /** 入口卡片副标题：课表 */
    fun hubHintCourses(lang: Lang, count: Int) = pickf(
        lang, "今天 %d 节", "今天 %d 節",
        "%d today", "本日 %d コマ",
        count
    )

    /** 入口卡片副标题：作业 */
    fun hubHintAssignments(lang: Lang, open: Int, overdue: Int) = pickf(
        lang, "待完成 %1\$d · 逾期 %2\$d", "待完成 %1\$d · 逾期 %2\$d",
        "%1\$d open · %2\$d overdue", "未完了 %1\$d · 超過 %2\$d",
        open, overdue
    )

    /** 入口卡片副标题：考试 */
    fun hubHintExams(lang: Lang, count: Int) = pickf(
        lang, "共 %d 场", "共 %d 場",
        "%d upcoming", "全 %d 件",
        count
    )

    /** 入口卡片副标题：GPA */
    fun hubHintGrades(lang: Lang, count: Int) = pickf(
        lang, "%d 门成绩", "%d 門成績",
        "%d courses", "%d 科目",
        count
    )

    /** 入口卡片副标题：学分 */
    fun hubHintCredits(lang: Lang, done: String, required: String) = pickf(
        lang, "%1\$s / %2\$s", "%1\$s / %2\$s",
        "%1\$s / %2\$s", "%1\$s / %2\$s",
        done, required
    )

    /** 入口卡片副标题：奖助 */
    fun hubHintAwards(lang: Lang, count: Int) = pickf(
        lang, "%d 条记录", "%d 條紀錄",
        "%d records", "%d 件",
        count
    )

    /** 入口卡片副标题：单词 */
    fun hubHintWords(lang: Lang, plans: Int) = pickf(
        lang, "%d 个背词计划", "%d 個背詞計畫",
        "%d vocab plans", "単語プラン %d 件",
        plans
    )

    /** 入口卡片副标题：周报 */
    fun hubHintWeekly(lang: Lang) = pick(
        lang, "近 7 天回顾", "近 7 天回顧",
        "Last 7 days", "直近 7 日間"
    )

    // ==================== 课表 ====================

    /** 页面大标题：与子页面顶栏一致，都走 AppStrings.tabCourses */
    fun coursesTitle(lang: Lang) = AppStrings.tabCourses(lang)

    /** 「第 1-2 节」 */
    fun coursesPeriodRange(lang: Lang, start: Int, end: Int) = pickf(
        lang, "第 %1\$d-%2\$d 节", "第 %1\$d-%2\$d 節",
        "Period %1\$d-%2\$d", "%1\$d-%2\$d 限",
        start, end
    )

    /** 「第 3 节」（起止相同时） */
    fun coursesPeriodOne(lang: Lang, period: Int) = pickf(
        lang, "第 %d 节", "第 %d 節",
        "Period %d", "%d 限",
        period
    )

    /** 周次表达式的输入提示（也是语法说明） */
    fun coursesWeeksHint(lang: Lang) = pick(
        lang,
        "例：1-16 每周 · 1-16单 单周 · 2-16双 双周 · 3,5,7-9 跳周；留空＝每周都有",
        "例：1-16 每週 · 1-16單 單週 · 2-16雙 雙週 · 3,5,7-9 跳週；留空＝每週都有",
        "e.g. 1-16 every week · 1-16单 odd weeks · 2-16双 even weeks · 3,5,7-9 picked weeks; empty = every week",
        "例：1-16 毎週 · 1-16单 奇数週 · 2-16双 偶数週 · 3,5,7-9 指定週；空欄＝毎週"
    )

    /** 字段标签：周次 */
    fun coursesWeeks(lang: Lang) = pick(lang, "周次", "週次", "Weeks", "週")

    /** 字段标签：星期 */
    fun coursesDay(lang: Lang) = pick(lang, "星期", "星期", "Weekday", "曜日")

    /** 字段标签：第几节 */
    fun coursesPeriods(lang: Lang) = pick(lang, "第几节", "第幾節", "Periods", "時限")

    /** 字段标签：教师 */
    fun coursesTeacher(lang: Lang) = pick(lang, "教师", "教師", "Teacher", "担当")

    /** 字段标签：地点 */
    fun coursesLocation(lang: Lang) = pick(lang, "地点", "地點", "Location", "教室")

    /** 字段标签：学期起始日 */
    fun coursesTermStart(lang: Lang) = pick(
        lang, "学期起始日（第 1 周的周一）", "學期起始日（第 1 週的週一）",
        "Term start (Monday of week 1)", "学期開始日（第1週の月曜）"
    )

    /** 字段标签：课程名 */
    fun coursesName(lang: Lang) = pick(lang, "课程名", "課程名", "Course name", "授業名")

    /** 字段标签：课表配色 */
    fun coursesColor(lang: Lang) = pick(lang, "配色", "配色", "Colour", "色")

    /** 弹窗标题 */
    fun coursesAddTitle(lang: Lang) = pick(lang, "添加课程", "新增課程", "Add a course", "授業を追加")

    fun coursesEditTitle(lang: Lang) = pick(lang, "编辑课程", "編輯課程", "Edit course", "授業を編集")

    /** 今天 / 本周 视图切换 */
    fun coursesViewToday(lang: Lang) = pick(lang, "今天", "今天", "Today", "今日")

    fun coursesViewWeek(lang: Lang) = pick(lang, "本周", "本週", "This week", "今週")

    /** 「第 5 周」 */
    fun coursesWeekNumber(lang: Lang, week: Int) = pickf(
        lang, "第 %d 周", "第 %d 週",
        "Week %d", "第 %d 週",
        week
    )

    /** 「第 5 周 · 9月14日 - 9月20日」 */
    fun coursesWeekRange(lang: Lang, week: Int, start: String, end: String) = pickf(
        lang, "第 %1\$d 周 · %2\$s - %3\$s", "第 %1\$d 週 · %2\$s - %3\$s",
        "Week %1\$d · %2\$s - %3\$s", "第 %1\$d 週 · %2\$s - %3\$s",
        week, start, end
    )

    /** 今天视图里显示的星期几 + 日期 */
    fun coursesTodayHeader(lang: Lang, weekday: String, date: String) = pickf(
        lang, "%1\$s · %2\$s", "%1\$s · %2\$s",
        "%1\$s · %2\$s", "%1\$s · %2\$s",
        weekday, date
    )

    /** 「今天没有课」 */
    fun coursesNoCourseToday(lang: Lang) = pick(
        lang, "今天没有课", "今天沒有課",
        "No classes today", "今日は授業がありません"
    )

    /** 本周视图里的「今天」标记 */
    fun coursesTodayMark(lang: Lang) = pick(lang, "今天", "今天", "today", "今日")

    /** 课表为空 */
    fun coursesEmpty(lang: Lang) = pick(
        lang, "还没有课程，点右下角加一节", "還沒有課程，點右下角加一節",
        "No courses yet — add one with the button below", "まだ授業がありません。右下から追加できます"
    )

    /** 空状态副标题 */
    fun coursesEmptyHint(lang: Lang) = pick(
        lang, "填好星期与节次，作业和考试都能挂到课程上",
        "填好星期與節次，作業和考試都能掛到課程上",
        "Set the weekday and periods; assignments and exams can then link to the course",
        "曜日と時限を入れておくと、課題や試験を授業に紐づけられます"
    )

    /** 编辑区标题 */
    fun coursesListTitle(lang: Lang) = pick(
        lang, "全部课程", "全部課程",
        "All courses", "すべての授業"
    )

    /** 课程行右侧的删除按钮说明 */
    fun coursesDelete(lang: Lang) = pick(lang, "删除课程", "刪除課程", "Delete course", "授業を削除")

    /** 删除前的二次确认 */
    fun coursesDeleteConfirm(lang: Lang, name: String) = pickf(
        lang, "删除「%s」？课表上的一格会一起消失。", "刪除「%s」？課表上的一格會一起消失。",
        "Delete \"%s\"? Its slot in the timetable goes away too.", "「%s」を削除しますか？時間割のコマも消えます。",
        name
    )

    /** 「周三 第 1-2 节 · A101」 */
    fun coursesRowSubtitle(lang: Lang, weekday: String, periods: String, location: String) =
        if (location.isBlank()) {
            pickf(
                lang, "%1\$s %2\$s", "%1\$s %2\$s",
                "%1\$s %2\$s", "%1\$s %2\$s",
                weekday, periods
            )
        } else {
            pickf(
                lang, "%1\$s %2\$s · %3\$s", "%1\$s %2\$s · %3\$s",
                "%1\$s %2\$s · %3\$s", "%1\$s %2\$s · %3\$s",
                weekday, periods, location
            )
        }

    // ==================== 考试与复习计划 ====================

    fun examsTitle(lang: Lang) = AppStrings.tabExams(lang)

    /** 倒计时的大字 */
    fun examsDaysLeft(lang: Lang, days: Long) = pickf(
        lang, "%d 天", "%d 天",
        "%d days", "%d 日",
        days
    )

    fun examsToday(lang: Lang) = pick(lang, "今天", "今天", "Today", "今日")

    fun examsFinished(lang: Lang) = pick(lang, "已结束", "已結束", "Finished", "終了")

    /** 「还有 5 天」/「已过去 3 天」的说明文字 */
    fun examsCountdownNote(lang: Lang, days: Long) = pickf(
        lang, "还有 %d 天", "還有 %d 天",
        "%d days to go", "あと %d 日",
        days
    )

    fun examsPassedNote(lang: Lang, days: Long) = pickf(
        lang, "已过去 %d 天", "已過去 %d 天",
        "%d days ago", "%d 日前",
        days
    )

    fun examsAddTitle(lang: Lang) = pick(lang, "添加考试", "新增考試", "Add an exam", "試験を追加")

    fun examsEditTitle(lang: Lang) = pick(lang, "编辑考试", "編輯考試", "Edit exam", "試験を編集")

    fun examsName(lang: Lang) = pick(lang, "考试名称", "考試名稱", "Exam name", "試験名")

    fun examsLocation(lang: Lang) = pick(lang, "考场", "考場", "Room", "会場")

    fun examsDate(lang: Lang) = pick(lang, "考试日期", "考試日期", "Exam date", "試験日")

    fun examsDateHint(lang: Lang) = pick(
        lang, "点这里选考试日期", "點這裡選考試日期",
        "Tap to pick the exam date", "タップして試験日を選ぶ"
    )

    fun examsCourse(lang: Lang) = pick(lang, "关联课程", "關聯課程", "Course", "授業")

    fun examsTasksSection(lang: Lang) = pick(
        lang, "复习计划", "複習計畫",
        "Study plan", "復習プラン"
    )

    fun examsAddTaskPlaceholder(lang: Lang) = pick(
        lang, "加一条复习任务", "加一條複習任務",
        "Add a study task", "復習タスクを追加"
    )

    fun examsNoTask(lang: Lang) = pick(
        lang, "还没有复习任务，可以点上面的「自动排复习计划」",
        "還沒有複習任務，可以點上面的「自動排複習計畫」",
        "No study tasks yet — try Auto-plan above",
        "復習タスクはまだありません。上の「自動プラン」が使えます"
    )

    fun examsDeleteTask(lang: Lang) = pick(lang, "删除任务", "刪除任務", "Delete task", "タスクを削除")

    /** 「复习 2 / 5」——卡片标题行上的完成情况 */
    fun examsTaskCount(lang: Lang, done: Int, total: Int) = pickf(
        lang, "复习 %1\$d / %2\$d", "複習 %1\$d / %2\$d",
        "Revision %1\$d / %2\$d", "復習 %1\$d / %2\$d",
        done, total
    )

    // ---- 自动排复习计划的四条建议任务（是数据也是文案，四语都给） ----

    fun examPlanTaskRead(lang: Lang) = pick(
        lang, "过一遍课本", "過一遍課本",
        "Skim the textbook", "教科書を通読"
    )

    fun examPlanTaskPastPapers(lang: Lang) = pick(
        lang, "刷真题", "刷真題",
        "Do past papers", "過去問を解く"
    )

    fun examPlanTaskKeyPoints(lang: Lang) = pick(
        lang, "背重点", "背重點",
        "Memorise key points", "要点を暗記"
    )

    fun examPlanTaskMock(lang: Lang) = pick(
        lang, "模拟一遍", "模擬一遍",
        "Full mock run", "模擬試験"
    )

    fun examPlanTaskReview(lang: Lang) = pick(
        lang, "过一遍错题", "過一遍錯題",
        "Review mistakes", "間違いを見直す"
    )

    /** 只剩一两天时排的那一条 */
    fun examPlanTaskFinal(lang: Lang) = pick(
        lang, "突击重点，快速过一遍", "突擊重點，快速過一遍",
        "Cram the key points", "要点を詰め込む"
    )

    fun examsAutoPlan(lang: Lang) = pick(
        lang, "自动排复习计划", "自動排複習計畫",
        "Auto-plan revision", "復習プランを自動作成"
    )

    /** 自动计划的说明：只是建议，可以删 */
    fun examsAutoPlanHint(lang: Lang) = pick(
        lang,
        "按剩余天数平均排几条（过一遍课本 / 刷真题 / 背重点 / 模拟一遍），只是建议，不合适的直接删。",
        "按剩餘天數平均排幾條（過一遍課本 / 刷真題 / 背重點 / 模擬一遍），只是建議，不合適的直接刪。",
        "Spreads a few tasks evenly across the days left (read the book / past papers / key points / mock run). It is only a suggestion — delete anything you do not need.",
        "残り日数に合わせて数件を均等に配置します（教科書通読・過去問・要点暗記・模試）。あくまで提案なので不要なら削除してください。"
    )

    fun examsPlanDone(lang: Lang, count: Int) = pickf(
        lang, "已排入 %d 条复习任务", "已排入 %d 條複習任務",
        "Added %d study tasks", "%d 件の復習タスクを追加しました",
        count
    )

    fun examsPlanTooLate(lang: Lang) = pick(
        lang, "考试已经结束，没法排计划了", "考試已經結束，沒法排計畫了",
        "This exam is over, so there is nothing to plan", "試験が終わっているためプランを作れません"
    )

    fun examsPlanNotEnoughDays(lang: Lang) = pick(
        lang, "只剩一两天了，就直接冲刺吧", "只剩一兩天了，就直接衝刺吧",
        "Only a day or two left — just cram", "残り 1〜2 日です。直前対策に切り替えましょう"
    )

    fun examsDelete(lang: Lang) = pick(lang, "删除考试", "刪除考試", "Delete exam", "試験を削除")

    fun examsDeleteConfirm(lang: Lang, name: String) = pickf(
        lang, "删除「%s」？它的复习任务也会一起删掉。", "刪除「%s」？它的複習任務也會一起刪掉。",
        "Delete \"%s\"? Its study tasks are removed too.", "「%s」を削除しますか？復習タスクも一緒に消えます。",
        name
    )

    /** 「2026年9月20日 09:00」的时间行 */
    fun examsWhen(lang: Lang, date: String, time: String) = pickf(
        lang, "%1\$s %2\$s", "%1\$s %2\$s",
        "%1\$s %2\$s", "%1\$s %2\$s",
        date, time
    )

    fun examsNote(lang: Lang) = pick(lang, "备注", "備註", "Note", "メモ")

    fun examsEmpty(lang: Lang) = pick(
        lang, "还没有考试安排", "還沒有考試安排",
        "No exams scheduled", "試験の予定はありません"
    )

    fun examsEmptyHint(lang: Lang) = pick(
        lang, "加上考试日期，首页就会有倒计时", "加上考試日期，首頁就會有倒數",
        "Add the date and the home tab shows a countdown", "日付を入れるとホームにカウントダウンが出ます"
    )

    // ==================== 作业 / DDL ====================

    fun assignmentsTitle(lang: Lang) = AppStrings.tabAssignments(lang)

    fun assignmentsSummary(lang: Lang, open: Int, overdue: Int) = pickf(
        lang, "待完成 %1\$d 条 · 逾期 %2\$d 条", "待完成 %1\$d 條 · 逾期 %2\$d 條",
        "%1\$d open · %2\$d overdue", "未完了 %1\$d 件 · 期限超過 %2\$d 件",
        open, overdue
    )

    /** 分组标题 */
    fun assignmentsOverdue(lang: Lang) = pick(lang, "已逾期", "已逾期", "Overdue", "期限超過")

    fun assignmentsToday(lang: Lang) = pick(lang, "今天到期", "今天到期", "Due today", "今日が期限")

    fun assignmentsThisWeek(lang: Lang) = pick(lang, "本周内", "本週內", "This week", "今週中")

    fun assignmentsLater(lang: Lang) = pick(lang, "以后", "以後", "Later", "それ以降")

    fun assignmentsDone(lang: Lang) = pick(lang, "已完成", "已完成", "Completed", "完了")

    /** 没有到期日的作业 */
    fun assignmentsNoDate(lang: Lang) = pick(lang, "没有日期", "沒有日期", "No due date", "期限なし")

    fun assignmentsAddTitle(lang: Lang) = pick(lang, "添加作业", "新增作業", "Add an assignment", "課題を追加")

    fun assignmentsEditTitle(lang: Lang) = pick(lang, "编辑作业", "編輯作業", "Edit assignment", "課題を編集")

    fun assignmentsTitleField(lang: Lang) = pick(lang, "作业内容", "作業內容", "Title", "課題名")

    fun assignmentsCourseField(lang: Lang) = pick(lang, "课程（可选）", "課程（選填）", "Course (optional)", "授業（任意）")

    fun assignmentsDueField(lang: Lang) = pick(lang, "截止日期", "截止日期", "Due date", "締切")

    fun assignmentsNoDue(lang: Lang) = pick(lang, "没有截止日期", "沒有截止日期", "No due date", "締切なし")

    fun assignmentsPickDue(lang: Lang) = pick(
        lang, "点这里选截止日期", "點這裡選截止日期",
        "Tap to pick a due date", "タップして締切を選ぶ"
    )

    fun assignmentsClearDue(lang: Lang) = pick(lang, "清除日期", "清除日期", "Clear date", "日付をクリア")

    /** 说明：作业就是带课程名的待办 */
    fun assignmentsHint(lang: Lang) = pick(
        lang, "作业就是带课程名的待办，也会出现在「待办」标签里。",
        "作業就是帶課程名的待辦，也會出現在「待辦」標籤裡。",
        "Assignments are to-dos with a course name, and they show up in the Todos tab too.",
        "課題は授業名つきの ToDo です。「ToDo」タブにも表示されます。"
    )
    fun assignmentsDelete(lang: Lang) = pick(lang, "删除作业", "刪除作業", "Delete assignment", "課題を削除")

    fun assignmentsEmpty(lang: Lang) = pick(
        lang, "没有作业，太棒了", "沒有作業，太棒了",
        "No assignments — nice", "課題はありません"
    )

    fun assignmentsEmptyHint(lang: Lang) = pick(
        lang, "有 DDL 就记一条，别靠脑子记", "有 DDL 就記一條，別靠腦子記",
        "Log a due date instead of remembering it", "締切は頭で覚えず記録しましょう"
    )

    /** 已完成的作业数量提示 */
    fun assignmentsDoneCount(lang: Lang, count: Int) = pickf(
        lang, "已完成 %d 条", "已完成 %d 條",
        "%d completed", "%d 件完了",
        count
    )

    fun assignmentsCourseChip(lang: Lang) = pick(lang, "课程", "課程", "Course", "授業")

    // ==================== GPA 计算器 ====================

    fun gradesTitle(lang: Lang) = AppStrings.tabGrades(lang)

    fun gradesSummary(lang: Lang) = pick(lang, "学业概览", "學業概覽", "Academic summary", "成績の概要")

    fun gradesGpa(lang: Lang) = pick(lang, "平均绩点", "平均績點", "GPA", "GPA")

    fun gradesCredits(lang: Lang) = pick(lang, "已修学分", "已修學分", "Credits earned", "取得単位")

    fun gradesCourseCount(lang: Lang) = pick(lang, "课程数", "課程數", "Courses", "科目数")

    fun gradesCountValue(lang: Lang, count: Int) = pickf(
        lang, "%d 门", "%d 門",
        "%d", "%d 科目",
        count
    )

    /** 「按 4.0 口径计算」/「按 5.0 口径计算」 */
    fun gradesScaleNote(lang: Lang, scale: String) = pickf(
        lang, "按 %s 口径换算", "按 %s 口徑換算",
        "Converted on a %s scale", "%s 換算",
        scale
    )

    fun gradesAddTitle(lang: Lang) = pick(lang, "添加成绩", "新增成績", "Add a grade", "成績を追加")

    fun gradesEditTitle(lang: Lang) = pick(lang, "编辑成绩", "編輯成績", "Edit grade", "成績を編集")

    fun gradesCourse(lang: Lang) = pick(lang, "课程名", "課程名", "Course name", "科目名")

    fun gradesTerm(lang: Lang) = pick(lang, "学期", "學期", "Term", "学期")

    fun gradesTermHint(lang: Lang) = pick(
        lang, "例如 2026春 / 2026-2027-1", "例如 2026春 / 2026-2027-1",
        "e.g. 2026 Spring / 2026-2027-1", "例：2026 春 / 2026-2027-1"
    )

    fun gradesScore(lang: Lang) = pick(lang, "分数", "分數", "Score", "評点")

    fun gradesCredit(lang: Lang) = pick(lang, "学分", "學分", "Credits", "単位数")

    fun gradesKind(lang: Lang) = pick(lang, "计分方式", "計分方式", "Scoring", "評価方法")

    fun gradesKindPercent(lang: Lang) = pick(lang, "百分制", "百分制", "Percentage", "100点法")

    fun gradesKindGrade(lang: Lang) = pick(lang, "五级制", "五級制", "Five-level", "5段階")

    fun gradesKindPoint(lang: Lang) = pick(lang, "直接填绩点", "直接填績點", "Grade point", "GP 直接入力")

    fun gradesCategory(lang: Lang) = pick(lang, "类别", "類別", "Category", "区分")

    fun gradesCategoryRequired(lang: Lang) = pick(lang, "必修", "必修", "Required", "必修")

    fun gradesCategoryElective(lang: Lang) = pick(lang, "选修", "選修", "Elective", "選択")

    fun gradesCategoryGeneral(lang: Lang) = pick(lang, "通识", "通識", "General", "教養")

    fun gradesCategoryOther(lang: Lang) = pick(lang, "其他", "其他", "Other", "その他")

    /** 「绩点 3.7」 */
    fun gradesPoint(lang: Lang, point: String) = pickf(
        lang, "绩点 %s", "績點 %s",
        "GP %s", "GP %s",
        point
    )

    /** 「3 学分」 */
    fun gradesCreditValue(lang: Lang, credit: String) = pickf(
        lang, "%s 学分", "%s 學分",
        "%s credits", "%s 単位",
        credit
    )

    /** 没有学期标签的分组名 */
    fun gradesNoTerm(lang: Lang) = pick(lang, "未填学期", "未填學期", "No term", "学期未入力")

    fun gradesDelete(lang: Lang) = pick(lang, "删除成绩", "刪除成績", "Delete grade", "成績を削除")

    fun gradesEmpty(lang: Lang) = pick(
        lang, "还没有成绩记录", "還沒有成績記錄",
        "No grades recorded yet", "成績の記録はまだありません"
    )

    fun gradesEmptyHint(lang: Lang) = pick(
        lang, "录入分数和学分，GPA 自动算", "錄入分數和學分，GPA 自動算",
        "Enter score and credits and the GPA updates itself", "評点と単位数を入れると GPA が自動計算されます"
    )

    /** 录入弹窗里的实时绩点预览 */
    fun gradesPreview(lang: Lang, point: String) = pickf(
        lang, "换算绩点：%s", "換算績點：%s",
        "Converted GP: %s", "換算 GP：%s",
        point
    )

    /** 分数填了但算不出绩点时 */
    fun gradesPreviewUnknown(lang: Lang) = pick(
        lang, "这个分数暂时换算不出绩点", "這個分數暫時換算不出績點",
        "This score cannot be converted yet", "この評点はまだ換算できません"
    )

    // ==================== 学分进度 ====================

    fun creditsTitle(lang: Lang) = AppStrings.tabCredits(lang)

    fun creditsTotal(lang: Lang) = pick(lang, "总学分进度", "總學分進度", "Overall progress", "全体の進捗")

    /** 「已修 86 / 160 学分」 */
    fun creditsSummary(lang: Lang, done: String, required: String) = pickf(
        lang, "已修 %1\$s / %2\$s 学分", "已修 %1\$s / %2\$s 學分",
        "%1\$s / %2\$s credits earned", "取得 %1\$s / %2\$s 単位",
        done, required
    )

    /** 「还差 74 学分」 */
    fun creditsRemaining(lang: Lang, value: String) = pickf(
        lang, "还差 %s 学分", "還差 %s 學分",
        "%s credits to go", "あと %s 単位",
        value
    )

    /** 「已达标」 */
    fun creditsReached(lang: Lang) = pick(
        lang, "已达标 🎉", "已達標 🎉",
        "Target reached 🎉", "目標達成 🎉"
    )

    /** 「已修 12 / 要求 20」 */
    fun creditsRowValue(lang: Lang, done: String, required: String) = pickf(
        lang, "已修 %1\$s / 要求 %2\$s", "已修 %1\$s / 要求 %2\$s",
        "%1\$s of %2\$s earned", "%1\$s / 要求 %2\$s 取得",
        done, required
    )

    fun creditsTargets(lang: Lang) = pick(
        lang, "分类要求", "分類要求",
        "Category targets", "区分ごとの要件"
    )

    /** 「其他已修学分」——录了成绩但没建对应要求的类别 */
    fun creditsUntracked(lang: Lang) = pick(
        lang, "其他已修学分", "其他已修學分",
        "Credits without a target", "要件未設定の単位"
    )

    fun creditsUntrackedHint(lang: Lang) = pick(
        lang, "这些类别还没设要求，加一条就能一起算进度。",
        "這些類別還沒設要求，加一條就能一起算進度。",
        "These categories have no target yet — add one to track them.",
        "これらの区分には要件が未設定です。追加すると進捗に含まれます。"
    )

    fun creditsAddTarget(lang: Lang) = pick(
        lang, "添加学分要求", "新增學分要求",
        "Add a credit target", "要件を追加"
    )

    fun creditsEditTarget(lang: Lang) = pick(
        lang, "编辑学分要求", "編輯學分要求",
        "Edit credit target", "要件を編集"
    )

    fun creditsCategory(lang: Lang) = pick(lang, "类别", "類別", "Category", "区分")

    fun creditsRequired(lang: Lang) = pick(
        lang, "要求学分", "要求學分",
        "Required credits", "必要単位数"
    )

    fun creditsDeleteTarget(lang: Lang) = pick(
        lang, "删除要求", "刪除要求",
        "Delete target", "要件を削除"
    )

    fun creditsEmpty(lang: Lang) = pick(
        lang, "还没有设定毕业学分要求", "還沒有設定畢業學分要求",
        "No graduation credit targets yet", "卒業要件の単位はまだ未設定です"
    )

    fun creditsEmptyHint(lang: Lang) = pick(
        lang, "按类别设置后，这页会显示每条要求的完成进度",
        "按類別設定後，這頁會顯示每條要求的完成進度",
        "Set them per category to see progress for each requirement",
        "区分ごとに設定すると、この画面で進捗が確認できます"
    )

    /** 计入 GPA 说明：只有非必修/通识在别处算，这里统一说明 */
    fun creditsHint(lang: Lang) = pick(
        lang, "学分从「成绩」里的课程学分累加而来，改成绩这页会跟着变。",
        "學分從「成績」裡的課程學分累加而來，改成績這頁會跟著變。",
        "Credits come from the credits you enter with each grade — fixing a grade updates this page.",
        "単位数は「成績」で入力した単位数の合計です。成績を直すとこの画面も変わります。"
    )

    // ==================== 奖助 / 竞赛 / 证书 ====================

    fun awardsTitle(lang: Lang) = AppStrings.tabAwards(lang)

    fun awardsAddTitle(lang: Lang) = pick(lang, "添加记录", "新增紀錄", "Add a record", "記録を追加")

    fun awardsEditTitle(lang: Lang) = pick(lang, "编辑记录", "編輯紀錄", "Edit record", "記録を編集")

    fun awardsName(lang: Lang) = pick(lang, "名称", "名稱", "Title", "名称")

    fun awardsKind(lang: Lang) = pick(lang, "类型", "類型", "Type", "種類")

    fun awardsKindScholarship(lang: Lang) = pick(lang, "奖助学金", "獎助學金", "Scholarship", "奨学金")

    fun awardsKindContest(lang: Lang) = pick(lang, "竞赛", "競賽", "Contest", "コンテスト")

    fun awardsKindCertificate(lang: Lang) = pick(lang, "证书", "證書", "Certificate", "資格・検定")

    fun awardsKindOther(lang: Lang) = pick(lang, "其他", "其他", "Other", "その他")

    fun awardsLevel(lang: Lang) = pick(lang, "级别", "級別", "Level", "レベル")

    fun awardsLevelNational(lang: Lang) = pick(lang, "国家级", "國家級", "National", "全国")

    fun awardsLevelProvincial(lang: Lang) = pick(lang, "省级", "省級", "Provincial", "省")

    fun awardsLevelSchool(lang: Lang) = pick(lang, "校级", "校級", "School", "大学")

    fun awardsLevelCollege(lang: Lang) = pick(lang, "院级", "院級", "College", "学部")

    fun awardsLevelNone(lang: Lang) = pick(lang, "不填", "不填", "None", "なし")

    fun awardsDate(lang: Lang) = pick(lang, "获得日期", "獲得日期", "Date", "取得日")

    fun awardsNote(lang: Lang) = pick(lang, "备注", "備註", "Note", "メモ")

    fun awardsDelete(lang: Lang) = pick(lang, "删除记录", "刪除紀錄", "Delete record", "記録を削除")

    fun awardsEmpty(lang: Lang) = pick(
        lang, "还没有奖助与证书", "還沒有獎助與證書",
        "No awards or certificates yet", "賞罰や資格の記録はまだありません"
    )

    fun awardsEmptyHint(lang: Lang) = pick(
        lang, "拿到的奖学金、竞赛名次、证书都可以记在这里",
        "拿到的獎學金、競賽名次、證書都可以記在這裡",
        "Scholarships, contest placings and certificates all live here",
        "奨学金・コンテストの成績・資格をここに記録できます"
    )

    /**
     * 有图片附件时显示的胶囊。
     * 本项目没有任何图片加载库（不引依赖），所以这里**不显示缩略图**，只提示「已附图片」。
     */
    fun awardsHasImage(lang: Lang) = pick(
        lang, "已附图片", "已附圖片",
        "Image attached", "画像あり"
    )

    fun awardsImageHint(lang: Lang) = pick(
        lang, "图片只是记了个路径，本页不显示缩略图（不引图片库）。",
        "圖片只是記了個路徑，本頁不顯示縮圖（不引圖片庫）。",
        "The image is only stored as a path — no thumbnail is shown (no image library is bundled).",
        "画像はパスのみ保存しています。この画面ではサムネイルを表示しません（画像ライブラリ非搭載）。"
    )

    /** 「共 5 条」 */
    fun awardsCount(lang: Lang, count: Int) = pickf(
        lang, "共 %d 条", "共 %d 條",
        "%d records", "%d 件",
        count
    )

    // ==================== 学习周报 ====================

    fun weeklyTitle(lang: Lang) = AppStrings.tabWeeklyReport(lang)

    fun weeklyRange(lang: Lang, start: String, end: String) = pickf(
        lang, "%1\$s - %2\$s", "%1\$s - %2\$s",
        "%1\$s - %2\$s", "%1\$s 〜 %2\$s",
        start, end
    )

    fun weeklyFocus(lang: Lang) = pick(lang, "专注", "專注", "Focus", "集中")

    fun weeklyFocusValue(lang: Lang, minutes: Int, count: Int) = pickf(
        lang, "%1\$d 分钟 · %2\$d 次", "%1\$d 分鐘 · %2\$d 次",
        "%1\$d min · %2\$d sessions", "%1\$d 分 · %2\$d 回",
        minutes, count
    )

    fun weeklyTodo(lang: Lang) = pick(lang, "待办完成", "待辦完成", "To-dos", "ToDo")

    fun weeklyTodoValue(lang: Lang, done: Int, total: Int) = pickf(
        lang, "%1\$d / %2\$d 条已完成", "%1\$d / %2\$d 條已完成",
        "%1\$d of %2\$d done", "%1\$d / %2\$d 完了",
        done, total
    )

    fun weeklyWords(lang: Lang) = pick(lang, "背单词打卡", "背單詞打卡", "Vocabulary", "単語学習")

    fun weeklyWordsValue(lang: Lang, plans: Int) = pickf(
        lang, "%d 个计划在进行", "%d 個計畫在進行",
        "%d active plans", "進行中のプラン %d 件",
        plans
    )

    /** 「近 7 天打卡 12 次」——单词计划的定量打卡（habit_logs 里近 7 天的记录数） */
    fun weeklyWordChecks(lang: Lang, count: Int) = pickf(
        lang, "近 7 天打卡 %d 次", "近 7 天打卡 %d 次",
        "%d check-ins in 7 days", "直近 7 日で %d 回",
        count
    )

    fun weeklySpend(lang: Lang) = pick(lang, "支出", "支出", "Spending", "支出")

    fun weeklySpendValue(lang: Lang, amount: String) = pickf(
        lang, "近 7 天支出 ¥%s", "近 7 天支出 ¥%s",
        "¥%s spent in 7 days", "直近 7 日で ¥%s",
        amount
    )

    fun weeklyAssignments(lang: Lang) = pick(lang, "作业", "作業", "Assignments", "課題")

    fun weeklyOverdueValue(lang: Lang, count: Int) = pickf(
        lang, "%d 条逾期未交", "%d 條逾期未交",
        "%d overdue", "期限超過 %d 件",
        count
    )

    fun weeklyNoOverdue(lang: Lang) = pick(
        lang, "没有逾期作业，保持住", "沒有逾期作業，保持住",
        "Nothing overdue — keep it up", "期限超過はありません"
    )

    /** 近 7 天专注柱状图的小标题 */
    fun weeklyDailyFocus(lang: Lang) = pick(
        lang, "近 7 天专注分钟", "近 7 天專注分鐘",
        "Focus minutes, last 7 days", "直近 7 日の集中時間"
    )

    fun weeklyDailySpend(lang: Lang) = pick(
        lang, "近 7 天支出", "近 7 天支出",
        "Daily spending, last 7 days", "直近 7 日の支出"
    )

    fun weeklyMinuteValue(lang: Lang, minutes: Int) = pickf(
        lang, "%d 分", "%d 分",
        "%d min", "%d 分",
        minutes
    )

    /** 数据口径说明（也是老实交代限制的地方） */
    fun weeklyNote(lang: Lang) = pick(
        lang,
        "数据来自本机已有记录：专注取近 7 天的记录，待办只统计「已完成」的条数（没有存完成日期，所以不分先后），背单词打卡取定量计划的近 7 天打卡次数，支出按记账日期取近 7 天。",
        "資料來自本機已有紀錄：專注取近 7 天的紀錄，待辦只統計「已完成」的條數（沒有存完成日期，所以不分先後），背單詞打卡取定量計畫的近 7 天打卡次數，支出按記帳日期取近 7 天。",
        "Everything comes from local records: focus sessions from the last 7 days, to-dos as a plain count of completed items (no completion date is stored, so they are not dated), vocabulary check-ins from quantitative habits over the last 7 days, and expenses by entry date over the last 7 days.",
        "データは端末内の記録から算出します。集中は直近 7 日分、ToDo は完了件数のみ（完了日を保存していないため日付順ではありません）、単語のチェックインは定量プランの直近 7 日分、支出は記録日の直近 7 日分です。"
    )

    /** 什么都没有的时候 */
    fun weeklyEmpty(lang: Lang) = pick(
        lang, "这一周还没有数据", "這一週還沒有資料",
        "No data for this week yet", "今週はまだデータがありません"
    )

    fun weeklyExportImage(lang: Lang) = pick(
        lang, "导出图片", "匯出圖片",
        "Export image", "画像を書き出す"
    )

    fun weeklyExportDone(lang: Lang) = pick(
        lang, "周报已导出", "週報已匯出",
        "Weekly report exported", "週報を書き出しました"
    )

    fun weeklyExportFailed(lang: Lang, reason: String) = pickf(
        lang, "导出失败：%s", "匯出失敗：%s",
        "Export failed: %s", "書き出しに失敗しました：%s",
        reason
    )

    fun weeklyFileName(lang: Lang, appName: String, date: String) = pickf(
        lang, "%1\$s周报-%2\$s", "%1\$s週報-%2\$s",
        "%1\$s-weekly-%2\$s", "%1\$s週報-%2\$s",
        appName, date
    )

    /** 导出图上的标题（不是界面文案，但同样要四语） */
    fun weeklyImageFooter(lang: Lang, appName: String) = pickf(
        lang, "由 %s 生成", "由 %s 產生",
        "Generated by %s", "%s で作成",
        appName
    )
}
