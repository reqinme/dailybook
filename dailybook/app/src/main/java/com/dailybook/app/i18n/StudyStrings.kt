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

    /** 「最近没有考试」 */
    fun hubNoExam(lang: Lang) = pick(
        lang, "最近没有安排考试", "最近沒有安排考試",
        "No exams coming up", "予定されている試験はありません"
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

    /**
     * 字段标签：作业挂的课程。
     *
     * 不带「（可选）」：课程名不是随便填的装饰 —— 作业的判定就是「课程名非空」，
     * 新建时不填这条只会进待办、编辑时清空会把它搬回待办（见 [assignmentsCourseRule]，
     * 那两句说明常驻在这个字段下面）。标签写「可选」会和这个行为打架，所以只说这是什么字段。
     */
    fun assignmentsCourseField(lang: Lang) = pick(lang, "课程", "課程", "Course", "授業")

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

    /**
     * 字段标签：类型。
     *
     * 奖助记录弹窗里选的是奖助类别（奖助学金 / 竞赛 / 证书 / 其他）；
     * 周期记账弹窗里选的是收支类型（支出 / 收入）—— 两处都是「类型」这一个词，
     * 原来分居两张表、一字不差（SettingsStrings.recurringKindLabel），已合并到这里。
     */
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

    fun weeklyTodo(lang: Lang) = pick(lang, "待办完成", "待辦完成", "To-dos", "ToDo")

    fun weeklyTodoValue(lang: Lang, done: Int, total: Int) = pickf(
        lang, "%1\$d / %2\$d 条已完成", "%1\$d / %2\$d 條已完成",
        "%1\$d of %2\$d done", "%1\$d / %2\$d 完了",
        done, total
    )

    /**
     * 定量计划打卡这一栏的标题。
     *
     * 不说「背单词」：这一栏列的是**单位可数**的习惯（个 / 页），背单词只是其中一种，
     * 背书计划、每天练琴 30 个音阶都算；而「每天练琴 30 分钟」这种按时间记的习惯根本不在这一栏里
     * （见 MainViewModel 的 COUNTABLE_HABIT_UNITS）。标题必须和这一栏真正装的东西一致。
     */
    fun weeklyWords(lang: Lang) = pick(lang, "定量计划打卡", "定量計畫打卡", "Quantitative plans", "定量プランのチェック")

    fun weeklyWordsValue(lang: Lang, plans: Int) = pickf(
        lang, "%d 个定量计划在进行", "%d 個定量計畫在進行",
        "%d active quantitative plans", "進行中の定量プラン %d 件",
        plans
    )

    /**
     * 「近 7 天打卡 12 次」——定量计划的近 7 天打卡**天数**。
     *
     * 口径：一条记录 = 这一天这个习惯打过卡（同一天多条重复记录只算一天，
     * 见 HabitLogEntity 的说明），不是「记录行数」。
     */
    fun weeklyWordChecks(lang: Lang, count: Int) = pickf(
        lang, "定量计划近 7 天打卡 %d 天", "定量計畫近 7 天打卡 %d 天",
        "%d check-in days on quantitative plans in 7 days", "定量プランの直近 7 日で %d 日",
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

    /** 数据口径说明（也是老实交代限制的地方） */
    fun weeklyNote(lang: Lang) = pick(
        lang,
        "数据来自本机已有记录：专注取近 7 天的记录，待办只统计「已完成」的条数（没有存完成日期，所以不分先后），定量计划打卡取定量计划的近 7 天打卡天数，支出按记账日期取近 7 天。",
        "資料來自本機已有紀錄：專注取近 7 天的紀錄，待辦只統計「已完成」的條數（沒有存完成日期，所以不分先後），定量計畫打卡取定量計畫的近 7 天打卡天數，支出按記帳日期取近 7 天。",
        "Everything comes from local records: focus sessions from the last 7 days, to-dos as a plain count of completed items (no completion date is stored, so they are not dated), quantitative habit check-ins counted as days over the last 7 days, and expenses by entry date over the last 7 days.",
        "データは端末内の記録から算出します。集中は直近 7 日分、ToDo は完了件数のみ（完了日を保存していないため日付順ではありません）、定量プランのチェックインは直近 7 日の日数、支出は記録日の直近 7 日分です。"
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

    // ==================== 课程时间（上课提醒的依据） ====================

    /**
     * 字段标签：开始时间 / 结束时间。
     *
     * 传 `true` 取「开始」，`false` 取「结束」—— 比给每个方向单开一个函数更省，
     * 也不会让文案表多出一堆只差一个词的函数。
     */
    fun coursesBoundaryLabel(lang: Lang, start: Boolean) =
        if (start) {
            pick(lang, "开始时间", "開始時間", "Start time", "開始時刻")
        } else {
            pick(lang, "结束时间", "結束時間", "End time", "終了時刻")
        }

    /** 时间输入框为空时的占位说明：说清「不填也行、不填就不提醒」 */
    fun coursesTimeEmptyHint(lang: Lang) = pick(
        lang, "不填也能存，只是这节课不会有上课提醒",
        "不填也能存，只是這節課不會有上課提醒",
        "Optional — without a time this course gets no class reminder",
        "空欄でも保存できますが、この授業のリマインダーは鳴りません"
    )

    /** 时间输入框旁的清除按钮 */
    fun coursesTimeClear(lang: Lang) = pick(
        lang, "清除时间", "清除時間", "Clear time", "時刻をクリア"
    )

    /** 「08:00–09:40」这类时间范围的说明前缀，例如「上课时间 08:00–09:40」 */
    fun coursesTimeRange(lang: Lang, range: String) = pickf(
        lang, "上课时间 %s", "上課時間 %s",
        "Class time %s", "授業時間 %s",
        range
    )

    /** 时间填得不对：小时 0-23、分钟 0-59 */
    fun coursesTimeInvalid(lang: Lang) = pick(
        lang, "时间填得不对：小时 0-23，分钟 0-59",
        "時間填得不對：小時 0-23，分鐘 0-59",
        "That time is out of range: hour 0-23, minute 0-59",
        "時刻が正しくありません（時 0〜23、分 0-59）"
    )

    /** 下课时间不晚于上课时间 */
    fun coursesTimeOrder(lang: Lang) = pick(
        lang, "下课时间要晚于上课时间",
        "下課時間要晚於上課時間",
        "The end time must be later than the start time",
        "終了時刻は開始時刻より後にしてください"
    )

    // ============================================================
    // 追加（只增不改：上面原有的函数一个都没动）
    // ============================================================

    /**
     * 作业弹窗里「课程」下面常驻的一句说明：填与不填**分别会发生什么**。
     *
     * 作业的判定就是「课程名非空」（[com.dailybook.app.data.TodoEntity.courseName]），
     * 所以课程名一空，这条就从作业页消失、变成普通待办。字段留空在语法上是允许的，
     * 但两种空法后果不同，不能在弹窗里不说：
     * 新建时不填 → 只会进待办，点了保存却在这一页看不到，像是没保存成功；
     * 编辑时清空 → 这条会搬走，需要用户点确认。
     *
     * 正因如此，[assignmentsCourseField] 的标签**不再**写「（可选）」——
     * 标签说「可选」、下面却说「不填就离开这一页」，两句话互相打架。
     */
    fun assignmentsCourseRule(lang: Lang) = pick(
        lang,
        "填了课程才留在作业页：新建时不填只会进待办，编辑时清空会离开这一页回到待办",
        "填了課程才留在作業頁：新增時不填只會進待辦，編輯時清空會離開這一頁回到待辦",
        "A course keeps it on this page — leave it empty when adding and it only lands in your to-dos; clear it when editing and it leaves this page for your to-dos",
        "授業名があるとこのページに残ります。新規で空欄なら ToDo に入るだけ、編集で消すとこのページから外れます"
    )

    /**
     * 学期起始日会被归到那一周的周一（第 1 周的周一）。
     *
     * 字段标签写的就是「第 1 周的周一」，而 [com.dailybook.app.ui.study.weekNumberFor]
     * 内部也是先把起始日归到周一再算周次；用户选了周中的日子时，这句话说明真正生效的是哪一天
     * （不做静默改动，也不让「标签说的」和「算出来的」差几天）。
     */
    fun coursesTermStartMonday(lang: Lang, date: String) = pickf(
        lang, "第 1 周从 %s 起算：选了周中的日子会归到那一周的周一",
        "第 1 週從 %s 起算：選了週中的日子會歸到那一週的週一",
        "Week 1 starts on %s — a mid-week pick snaps back to that week's Monday",
        "第 1 週は %s から：平日を選ぶとその週の月曜に合わせます",
        date
    )

    /**
     * 节次填到第 1~12 节之外（或结束早于开始）时的提示。
     *
     * 课表只有 12 行（`CoursesScreen` 里那个固定网格），越界的输入以前是**静默**夹到 1~12
     * 再保存，用户填了 13 却存成 12、图上也就画 1 行；
     * 现在把实际会存下来的节次说出来，看到的和存下来的不再是两个数。
     */
    fun coursesPeriodsClamped(lang: Lang, start: Int, end: Int) = pickf(
        lang, "课表只有第 1-12 节：保存时会记成第 %1\$d-%2\$d 节",
        "課表只有第 1-12 節：儲存時會記成第 %1\$d-%2\$d 節",
        "The grid only has periods 1-12: this will be saved as period %1\$d-%2\$d",
        "時間割は 1〜12 時限のみ：保存時は %1\$d〜%2\$d 時限になります",
        start, end
    )

    /** 奖助弹窗里配图那一行的字段标签（和大事记那边的口径一致） */
    fun awardsFieldImage(lang: Lang) = pick(lang, "配图", "配圖", "Image", "画像")

    /** 奖助已经选好了一张配图（只记 URI，列表上仍然只显示「已附图片」胶囊、不显示缩略图） */
    fun awardsImageChosen(lang: Lang) = pick(
        lang, "已选择图片", "已選擇圖片", "Image chosen", "画像を選択済み"
    )

    /**
     * 「已修学分」的口径说明（成绩页的汇总卡与学分进度页共用）。
     *
     * 以前已修学分把不及格（绩点 0.00）的课也算进去，和按绩点加权的 GPA 自相矛盾；
     * 现在只算「有学分且绩点 > 0」的课，这句话把这个规则写给用户看。
     */
    fun gradesCreditsRule(lang: Lang) = pick(
        lang,
        "已修学分只算及格（绩点 > 0）且有学分的课；不及格、缓考或还没出分的不计入",
        "已修學分只算及格（績點 > 0）且有學分的課；不及格、緩考或還沒出分的不計入",
        "Earned credits count only passed courses with credits (credit > 0 and grade point > 0); failed, deferred or ungraded courses are left out",
        "修得単位数は「単位数 > 0 かつ GPA ポイント > 0」の科目のみ。不合格・追試・未採点は含みません"
    )
}
