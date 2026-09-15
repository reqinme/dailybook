package com.dailybook.app.data

import android.content.Context
import androidx.room.withTransaction
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** 周期记账一次最多补多少笔：一条很多年前的「每天」规则也不该在这里长跑（剩下的由收尾一步推到未来） */
private const val MAX_CATCH_UP = 60

/** 记账 + 待办 + 专注记录的数据入口 */
class DailyRepository(context: Context) {

    private val db = AppDatabase.get(context)
    private val txDao = db.transactionDao()
    private val todoDao = db.todoDao()
    private val sessionDao = db.focusSessionDao()
    private val subtaskDao = db.subtaskDao()
    private val recurringDao = db.recurringDao()
    private val memoDao = db.memoDao()
    private val milestoneDao = db.milestoneDao()
    private val importantDateDao = db.importantDateDao()
    private val habitDao = db.habitDao()
    private val courseDao = db.courseDao()
    private val examDao = db.examDao()
    private val studyDao = db.studyDao()

    val transactions: Flow<List<TransactionEntity>> = txDao.observeAll()
    val todos: Flow<List<TodoEntity>> = todoDao.observeAll()
    val focusSessions: Flow<List<FocusSessionEntity>> = sessionDao.observeAll()
    val subtasks: Flow<List<SubtaskEntity>> = subtaskDao.observeAll()
    val recurring: Flow<List<RecurringEntity>> = recurringDao.observeAll()
    val memos: Flow<List<MemoEntity>> = memoDao.observeAll()
    val milestones: Flow<List<MilestoneEntity>> = milestoneDao.observeAll()
    val importantDates: Flow<List<ImportantDateEntity>> = importantDateDao.observeAll()
    val habits: Flow<List<HabitEntity>> = habitDao.observeAll()
    val habitLogs: Flow<List<HabitLogEntity>> = habitDao.observeLogs()
    val courses: Flow<List<CourseEntity>> = courseDao.observeAll()
    val exams: Flow<List<ExamEntity>> = examDao.observeAll()
    val studyTasks: Flow<List<StudyTaskEntity>> = examDao.observeTasks()
    val grades: Flow<List<GradeEntity>> = studyDao.observeGrades()
    val creditTargets: Flow<List<CreditTargetEntity>> = studyDao.observeTargets()
    val awards: Flow<List<AwardEntity>> = studyDao.observeAwards()

    // ---- 记账 ----

    suspend fun addTransaction(
        amountCents: Long,
        type: TxType,
        category: String,
        note: String,
        dateMillis: Long,
        account: String = Accounts.DEFAULT,
        tags: List<String> = emptyList(),
        reimbursable: Boolean = false,
        currency: String = Currencies.BASE,
        foreignAmountCents: Long = 0L,
        rateScaled: Long = Currencies.RATE_SCALE,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        txDao.insert(
            TransactionEntity(
                amountCents = amountCents,
                typeName = type.name,
                category = category,
                account = account.ifBlank { Accounts.DEFAULT },
                note = note,
                tags = TransactionEntity.joinTags(tags),
                reimbursable = reimbursable,
                currency = currency,
                foreignAmountCents = if (foreignAmountCents > 0L) foreignAmountCents else amountCents,
                rateScaled = rateScaled,
                dateMillis = dateMillis,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateTransaction(
        item: TransactionEntity,
        amountCents: Long,
        type: TxType,
        category: String,
        note: String,
        dateMillis: Long,
        account: String = item.account,
        tags: List<String> = item.tagList,
        reimbursable: Boolean = item.reimbursable,
        currency: String = item.currency,
        foreignAmountCents: Long = item.foreignAmountCents,
        rateScaled: Long = item.rateScaled
    ) {
        txDao.update(
            item.copy(
                amountCents = amountCents,
                typeName = type.name,
                category = category,
                account = account.ifBlank { Accounts.DEFAULT },
                note = note,
                tags = TransactionEntity.joinTags(tags),
                reimbursable = reimbursable,
                currency = currency,
                foreignAmountCents = if (foreignAmountCents > 0L) foreignAmountCents else amountCents,
                rateScaled = rateScaled,
                dateMillis = dateMillis
            )
        )
    }

    /** 标记一笔是否已经报销 */
    suspend fun setReimbursed(item: TransactionEntity, reimbursed: Boolean) =
        txDao.update(item.copy(reimbursed = reimbursed))

    suspend fun deleteTransaction(item: TransactionEntity) = txDao.delete(item)

    /** 批量写入（CSV 导入用） */
    suspend fun insertTransactions(items: List<TransactionEntity>) {
        if (items.isEmpty()) return
        txDao.insertAll(items)
    }

    suspend fun clearTransactions() = txDao.clearAll()

    // ---- 待办 ----

    suspend fun addTodo(
        title: String,
        dueMillis: Long?,
        repeatRule: RepeatRule = RepeatRule.NONE,
        priority: TodoPriority = TodoPriority.NORMAL,
        /** 作业 / DDL 用：属于哪门课；普通待办留空 */
        courseName: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ) {
        // 新待办排在最前面（sortOrder 越小越靠前）
        val minOrder = todoDao.getAll().minOfOrNull { it.sortOrder } ?: 0L
        todoDao.insert(
            TodoEntity(
                title = title,
                dueMillis = dueMillis,
                repeatRule = repeatRule.name,
                priority = priority.name,
                sortOrder = minOrder - 1L,
                courseName = courseName.trim(),
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateTodo(item: TodoEntity) = todoDao.update(item)

    /** 手工排序：按传入顺序重排（列表拖动后调用） */
    suspend fun reorderTodos(ordered: List<TodoEntity>) {
        db.withTransaction {
            ordered.forEachIndexed { index, todo ->
                if (todo.sortOrder != index.toLong()) todoDao.updateSortOrder(todo.id, index.toLong())
            }
        }
    }

    // ---- 子任务 ----

    suspend fun subtasksOf(todoId: Long): List<SubtaskEntity> = subtaskDao.forTodo(todoId)

    suspend fun addSubtask(todoId: Long, title: String, nowMillis: Long = System.currentTimeMillis()) {
        val text = title.trim()
        if (text.isEmpty()) return
        val maxOrder = subtaskDao.forTodo(todoId).maxOfOrNull { it.sortOrder } ?: 0L
        subtaskDao.insert(
            SubtaskEntity(
                todoId = todoId,
                title = text,
                sortOrder = maxOrder + 1L,
                createdAt = nowMillis
            )
        )
    }

    suspend fun setSubtaskDone(item: SubtaskEntity, done: Boolean) =
        subtaskDao.update(item.copy(done = done))

    suspend fun deleteSubtask(item: SubtaskEntity) = subtaskDao.delete(item)

    /**
     * 勾选 / 取消勾选待办。
     * 勾上一条「重复任务」时，自动生成下一次的那条（未完成、到期日顺延到未来）；
     * 取消勾选只改状态，不生成新任务。
     */
    suspend fun setTodoDone(item: TodoEntity, done: Boolean) {
        // 先按 id 把库里这一行重新读出来，不用调用方传进来的 item 直接生成下一次：
        // item 是列表渲染那一刻的旧快照（界面上的改名 / 优先级 / 重要标记都是先写库再刷新列表的），
        // 用户改完立刻勾选时快照还是老值，照它生成的下一次会把这次改动整个丢掉。
        // 读不到（id = 0 之类）才退回 item，行为和以前一样。
        val current = todoDao.getById(item.id) ?: item
        if (!done || !current.repeats) {
            todoDao.update(current.copy(done = done))
            return
        }
        db.withTransaction {
            val base = current.dueMillis ?: LocalDate.now().toDayMillis()
            val next = nextDueMillisOf(base, current.repeat)
            todoDao.update(current.copy(done = true))
            // 去重键是「标题 + 到期日 + 未完成」：连点两下勾选框（或先勾后取消再勾）都不会冒出重复的下一次。
            // 反过来，改过标题之后生成的下一次会被当成另一条任务（去重查不到同名的那条）——这正是想要的：
            // 改了名说明用户把它当成另一件事，旧名字那条不该再拦着它。所以这里必须用刚读出来的当前标题。
            if (next != null && todoDao.findPending(current.title, next) == null) {
                todoDao.insert(current.copy(id = 0L, done = false, dueMillis = next))
            }
        }
    }

    suspend fun toggleTodoImportant(item: TodoEntity) =
        todoDao.update(item.copy(important = !item.important))

    /** 删除待办时连它的子任务一起删，不留孤儿数据 */
    suspend fun deleteTodo(item: TodoEntity) {
        db.withTransaction {
            subtaskDao.deleteForTodo(item.id)
            todoDao.delete(item)
        }
    }

    suspend fun clearCompletedTodos() = todoDao.clearCompleted()

    /** 清空待办时连子任务一起删，不留孤儿数据（和 [deleteTodo] 一样先子后父） */
    suspend fun clearTodos() {
        db.withTransaction {
            subtaskDao.clearAll()
            todoDao.clearAll()
        }
    }

    // ---- 专注记录 ----

    suspend fun recordFocusSession(
        startedAtMillis: Long,
        endedAtMillis: Long,
        minutes: Int,
        taskTitle: String,
        interrupted: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        sessionDao.insert(
            FocusSessionEntity(
                startedAtMillis = startedAtMillis,
                endedAtMillis = endedAtMillis,
                minutes = minutes,
                taskTitle = taskTitle,
                interrupted = interrupted,
                createdAt = nowMillis
            )
        )
    }

    suspend fun clearFocusSessions() = sessionDao.clearAll()

    // ---- 周期记账 / 固定支出 ----

    suspend fun addRecurring(
        amountCents: Long,
        type: TxType,
        category: String,
        account: String,
        note: String,
        tags: List<String>,
        rule: RepeatRule,
        nextDueMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        recurringDao.insert(
            RecurringEntity(
                amountCents = amountCents,
                typeName = type.name,
                category = category,
                account = account.ifBlank { Accounts.DEFAULT },
                note = note,
                tags = TransactionEntity.joinTags(tags),
                rule = if (rule == RepeatRule.NONE) RepeatRule.MONTHLY.name else rule.name,
                nextDueMillis = nextDueMillis,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateRecurring(item: RecurringEntity) = recurringDao.update(item)

    suspend fun deleteRecurring(item: RecurringEntity) = recurringDao.delete(item)

    /**
     * 把到期的周期记账补成真实记录，并把下一次推到「严格晚于今天」。
     *
     * 每一条错过的到期日都补成一笔**落在它自己日期上**的记录：每月 3000 元的房租、三个月没打开
     * App，得到的是三笔（各自落在那三个月里），而不是只补一笔、还被记在已经过去的月份里。
     *
     * 循环上限 [MAX_CATCH_UP] 只管「这一次补多少笔」：十年前的「每天」规则也只补 60 次，
     * 剩下的由收尾那一步直接推到未来 —— 收尾保证 nextDueMillis 严格晚于今天，
     * 所以漏下的历史是**真的不补了**（而不是留到下次打开又补一批）。整个补齐在**一个事务**里完成。
     *
     * 返回这次一共补了几笔。
     */
    suspend fun materializeRecurring(today: LocalDate = LocalDate.now()): Int {
        val todayMillis = today.toDayMillis()
        val due = recurringDao.getAll().filter { it.enabled && it.nextDueMillis <= todayMillis }
        if (due.isEmpty()) return 0

        var inserted = 0
        db.withTransaction {
            due.forEach { rule ->
                // rule.rule 是存下来的枚举名，rule.repeat 是它解析出来的枚举（认不出来退回「每月」）
                val repeat = rule.repeat
                fun nextPeriod(from: LocalDate): LocalDate = when (repeat) {
                    RepeatRule.DAILY -> from.plusDays(1)
                    RepeatRule.WEEKLY -> from.plusWeeks(1)
                    RepeatRule.MONTHLY -> from.plusMonths(1)
                    RepeatRule.NONE -> today.plusDays(1)
                }

                var date = rule.nextDueMillis.toLocalDate()
                var steps = 0
                while (date.toDayMillis() <= todayMillis && steps < MAX_CATCH_UP) {
                    txDao.insert(
                        TransactionEntity(
                            amountCents = rule.amountCents,
                            typeName = rule.typeName,
                            category = rule.category,
                            account = rule.account,
                            note = rule.note,
                            tags = rule.tags,
                            // 补出来的每一笔都记在「它自己那个到期日」上，后面的月份统计才对得上
                            dateMillis = date.toDayMillis(),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    inserted++
                    date = nextPeriod(date)
                    steps++
                }
                // 收尾：把 nextDueMillis 落到**严格晚于今天**的第一个到期日。
                //
                // 只有撞到上限（date 还停在今天或更早）时才需要走这一步：正常收尾时 date 已经是今天之后的
                // 第一个到期日，而 helper 的语义是「base 之后的下一期」——它的校正循环至少会往后走一整期，
                // 对已经晚于今天的 date 再走一次就会白白跳过一期（1 月 1 日的房租、4 月 15 日打开时
                // date 已经推到 5 月 1 日，再走一步就把整个 5 月跳掉、再也补不回来）。
                //
                // 撞上限时改用 Entities.kt 里那个已被单测覆盖的 helper，而不是在这里再步进 MAX_CATCH_UP 次：
                // 后者对「逾期很多年的每天规则」会停在过去，下次打开又补 60 笔。
                // helper 对 NONE 返回 null（这种规则没有下一期），退回 nextPeriod 的 NONE 口径 = 明天，
                // 保证收尾之后 nextDueMillis 一定严格晚于今天。
                if (date.toDayMillis() <= todayMillis) {
                    date = nextDueMillisOf(date.toDayMillis(), repeat, today)?.toLocalDate()
                        ?: nextPeriod(date)
                }
                recurringDao.update(rule.copy(nextDueMillis = date.toDayMillis()))
            }
        }
        return inserted
    }

    // ---- 备忘录 ----

    suspend fun addMemo(
        title: String,
        content: String,
        pinned: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        memoDao.insert(
            MemoEntity(
                title = title,
                content = content,
                pinned = pinned,
                createdAt = nowMillis,
                updatedAt = nowMillis
            )
        )
    }

    /** 改内容时顺手刷新 updatedAt（列表按「最近改过」排） */
    suspend fun updateMemo(item: MemoEntity, nowMillis: Long = System.currentTimeMillis()) =
        memoDao.update(item.copy(updatedAt = nowMillis))

    suspend fun deleteMemo(item: MemoEntity) = memoDao.delete(item)

    /** 置顶 / 取消置顶；置顶本身也算一次改动，同样刷新 updatedAt */
    suspend fun toggleMemoPinned(item: MemoEntity, nowMillis: Long = System.currentTimeMillis()) =
        memoDao.update(item.copy(pinned = !item.pinned, updatedAt = nowMillis))

    // ---- 大事记 ----

    suspend fun addMilestone(
        title: String,
        note: String,
        dateMillis: Long,
        imageUri: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ) {
        milestoneDao.insert(
            MilestoneEntity(
                title = title,
                note = note,
                dateMillis = dateMillis,
                imageUri = imageUri,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateMilestone(item: MilestoneEntity) = milestoneDao.update(item)

    suspend fun deleteMilestone(item: MilestoneEntity) = milestoneDao.delete(item)

    // ---- 重要日期 ----

    suspend fun addImportantDate(
        title: String,
        dateMillis: Long,
        lunar: Boolean = false,
        lunarMonth: Int = 1,
        lunarDay: Int = 1,
        lunarLeap: Boolean = false,
        repeat: DateRepeat = DateRepeat.YEARLY,
        remindDaysBefore: Int = 0,
        note: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ) {
        importantDateDao.insert(
            ImportantDateEntity(
                title = title,
                dateMillis = dateMillis,
                lunar = lunar,
                lunarMonth = lunarMonth,
                lunarDay = lunarDay,
                lunarLeap = lunarLeap,
                // 存枚举名，不存本地化文案
                repeat = repeat.name,
                remindDaysBefore = remindDaysBefore,
                note = note,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateImportantDate(item: ImportantDateEntity) = importantDateDao.update(item)

    suspend fun deleteImportantDate(item: ImportantDateEntity) = importantDateDao.delete(item)

    // ---- 习惯打卡 ----

    suspend fun addHabit(
        name: String,
        emoji: String = "✅",
        targetPerDay: Int = 1,
        unit: String = "次",
        daysPerWeek: Int = 7,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        // 新习惯排在最前面（sortOrder 越小越靠前，和待办一个思路）
        val minOrder = habitDao.getAll().minOfOrNull { it.sortOrder } ?: 0L
        habitDao.insert(
            HabitEntity(
                name = name,
                emoji = emoji,
                targetPerDay = targetPerDay.coerceAtLeast(1),
                unit = unit,
                daysPerWeek = daysPerWeek.coerceIn(1, 7),
                sortOrder = minOrder - 1L,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateHabit(item: HabitEntity) = habitDao.update(item)

    /** 删习惯时连它的打卡记录一起删，不留孤儿数据 */
    suspend fun deleteHabit(item: HabitEntity) {
        db.withTransaction {
            habitDao.deleteLogsOf(item.id)
            habitDao.delete(item)
        }
    }

    /**
     * 记一次打卡（把当天的量改成 count）。
     * 当天还没有记录就新建一条，已经有了就原地改，保证「一天一条」。
     */
    suspend fun logHabit(
        habitId: Long,
        dayMillis: Long,
        count: Int,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val existing = habitDao.logOf(habitId, dayMillis)
        if (existing == null) {
            habitDao.insertLog(
                HabitLogEntity(
                    habitId = habitId,
                    dateMillis = dayMillis,
                    count = count,
                    createdAt = nowMillis
                )
            )
        } else {
            habitDao.updateLog(existing.copy(count = count))
        }
    }

    /**
     * 勾选 / 取消勾选某一天的习惯。
     * 没有记录 = 没做过，勾上就直接记满当天的目标量；
     * 已经有记录且大于 0 = 已做过，再点一次清成 0（取消勾选）；
     * 其余情况（记录存在但是 0）= 取消过又点回来，直接补满目标量。
     */
    suspend fun toggleHabitDone(
        habitId: Long,
        dayMillis: Long,
        targetPerDay: Int,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val target = targetPerDay.coerceAtLeast(1)
        val existing = habitDao.logOf(habitId, dayMillis)
        when {
            existing == null -> habitDao.insertLog(
                HabitLogEntity(
                    habitId = habitId,
                    dateMillis = dayMillis,
                    count = target,
                    createdAt = nowMillis
                )
            )
            existing.count > 0 -> habitDao.updateLog(existing.copy(count = 0))
            else -> habitDao.updateLog(existing.copy(count = target))
        }
    }

    // ---- 课表 ----

    /**
     * 新增一门课。
     *
     * [startMinutes] / [endMinutes] 是当天 00:00 起的分钟数（-1 = 没填），
     * 上课提醒靠它算准点时刻；只填了节次也能存，只是排不出提醒。
     */
    suspend fun addCourse(
        name: String,
        teacher: String = "",
        location: String = "",
        dayOfWeek: Int = 1,
        startPeriod: Int = 1,
        endPeriod: Int = 2,
        weeks: String = "",
        termStartMillis: Long,
        startMinutes: Int = -1,
        endMinutes: Int = -1,
        colorIndex: Int = 0,
        note: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ) {
        courseDao.insert(
            CourseEntity(
                name = name,
                teacher = teacher,
                location = location,
                dayOfWeek = dayOfWeek,
                startPeriod = startPeriod,
                endPeriod = endPeriod,
                weeks = weeks,
                termStartMillis = termStartMillis,
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                colorIndex = colorIndex,
                note = note,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateCourse(item: CourseEntity) = courseDao.update(item)

    suspend fun deleteCourse(item: CourseEntity) = courseDao.delete(item)

    // ---- 考试与复习计划 ----

    suspend fun addExam(
        name: String,
        courseName: String = "",
        examMillis: Long,
        location: String = "",
        note: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ) {
        examDao.insert(
            ExamEntity(
                name = name,
                courseName = courseName,
                examMillis = examMillis,
                location = location,
                note = note,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateExam(item: ExamEntity) = examDao.update(item)

    /** 删考试时连它的复习计划一起删，不留孤儿数据 */
    suspend fun deleteExam(item: ExamEntity) {
        db.withTransaction {
            examDao.deleteTasksOf(item.id)
            examDao.delete(item)
        }
    }

    suspend fun addStudyTask(
        examId: Long,
        title: String,
        dateMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        // 同一天里排在最后（sortOrder 越大越靠后）
        val maxOrder = examDao.getAllTasks()
            .filter { it.examId == examId && it.dateMillis == dateMillis }
            .maxOfOrNull { it.sortOrder } ?: 0L
        examDao.insertTask(
            StudyTaskEntity(
                examId = examId,
                title = title,
                dateMillis = dateMillis,
                sortOrder = maxOrder + 1L,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateStudyTask(item: StudyTaskEntity) = examDao.updateTask(item)

    suspend fun deleteStudyTask(item: StudyTaskEntity) = examDao.deleteTask(item)

    suspend fun toggleStudyTask(item: StudyTaskEntity) =
        examDao.updateTask(item.copy(done = !item.done))

    // ---- 成绩 / 学分 / 奖助 ----

    suspend fun addGrade(
        term: String,
        courseName: String,
        credit: Double = 0.0,
        score: String = "",
        scoreKind: ScoreKind = ScoreKind.PERCENT,
        point: Double = 0.0,
        category: String = "必修",
        note: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ) {
        studyDao.insertGrade(
            GradeEntity(
                term = term,
                courseName = courseName,
                credit = credit,
                score = score,
                // 存枚举名，不存本地化文案
                scoreKind = scoreKind.name,
                point = point,
                category = category,
                note = note,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateGrade(item: GradeEntity) = studyDao.updateGrade(item)

    suspend fun deleteGrade(item: GradeEntity) = studyDao.deleteGrade(item)

    suspend fun addCreditTarget(
        category: String,
        required: Double,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        studyDao.insertTarget(
            CreditTargetEntity(
                category = category,
                required = required,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateCreditTarget(item: CreditTargetEntity) = studyDao.updateTarget(item)

    suspend fun deleteCreditTarget(item: CreditTargetEntity) = studyDao.deleteTarget(item)

    suspend fun addAward(
        title: String,
        kind: AwardKind = AwardKind.SCHOLARSHIP,
        dateMillis: Long,
        level: String = "",
        note: String = "",
        imageUri: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ) {
        studyDao.insertAward(
            AwardEntity(
                title = title,
                // 存枚举名，不存本地化文案
                kind = kind.name,
                dateMillis = dateMillis,
                level = level,
                note = note,
                imageUri = imageUri,
                createdAt = nowMillis
            )
        )
    }

    suspend fun updateAward(item: AwardEntity) = studyDao.updateAward(item)

    suspend fun deleteAward(item: AwardEntity) = studyDao.deleteAward(item)

    // ---- 备份 / 恢复 ----

    /** 读出全部数据，用于导出备份 */
    suspend fun snapshot(): DbSnapshot = DbSnapshot(
        transactions = txDao.getAll(),
        todos = todoDao.getAll(),
        focusSessions = sessionDao.getAll(),
        subtasks = subtaskDao.getAll(),
        recurring = recurringDao.getAll(),
        memos = memoDao.getAll(),
        milestones = milestoneDao.getAll(),
        importantDates = importantDateDao.getAll(),
        habits = habitDao.getAll(),
        habitLogs = habitDao.getAllLogs(),
        courses = courseDao.getAll(),
        exams = examDao.getAll(),
        studyTasks = examDao.getAllTasks(),
        grades = studyDao.getAllGrades(),
        creditTargets = studyDao.getAllTargets(),
        awards = studyDao.getAllAwards()
    )

    /** 用备份内容整体替换现有数据：先清空再写入，同一个事务内完成 */
    suspend fun restore(data: DbSnapshot) {
        db.withTransaction {
            // 先清子表再清父表（习惯 → 打卡、考试 → 复习计划），避免留下悬空引用
            habitDao.clearAllLogs()
            habitDao.clearAll()
            examDao.clearAllTasks()
            examDao.clearAll()
            studyDao.clearGrades()
            studyDao.clearTargets()
            studyDao.clearAwards()
            memoDao.clearAll()
            milestoneDao.clearAll()
            importantDateDao.clearAll()
            courseDao.clearAll()
            txDao.clearAll()
            todoDao.clearAll()
            sessionDao.clearAll()
            subtaskDao.clearAll()
            recurringDao.clearAll()
            // 再按父表在前、子表在后的顺序写回去
            txDao.insertAll(data.transactions)
            todoDao.insertAll(data.todos)
            sessionDao.insertAll(data.focusSessions)
            subtaskDao.insertAll(data.subtasks)
            recurringDao.insertAll(data.recurring)
            memoDao.insertAll(data.memos)
            milestoneDao.insertAll(data.milestones)
            importantDateDao.insertAll(data.importantDates)
            habitDao.insertAll(data.habits)
            habitDao.insertAllLogs(data.habitLogs)
            courseDao.insertAll(data.courses)
            examDao.insertAll(data.exams)
            examDao.insertAllTasks(data.studyTasks)
            studyDao.insertAllGrades(data.grades)
            studyDao.insertAllTargets(data.creditTargets)
            studyDao.insertAllAwards(data.awards)
        }
    }

    // ---- 全局 ----

    suspend fun clearAll() {
        // 同样先子表后父表
        habitDao.clearAllLogs()
        habitDao.clearAll()
        examDao.clearAllTasks()
        examDao.clearAll()
        studyDao.clearGrades()
        studyDao.clearTargets()
        studyDao.clearAwards()
        memoDao.clearAll()
        milestoneDao.clearAll()
        importantDateDao.clearAll()
        courseDao.clearAll()
        txDao.clearAll()
        todoDao.clearAll()
        sessionDao.clearAll()
        subtaskDao.clearAll()
        recurringDao.clearAll()
    }
}

/** 一份完整的数据库快照（备份文件的内容） */
data class DbSnapshot(
    val transactions: List<TransactionEntity>,
    val todos: List<TodoEntity>,
    val focusSessions: List<FocusSessionEntity>,
    val subtasks: List<SubtaskEntity> = emptyList(),
    val recurring: List<RecurringEntity> = emptyList(),
    // 以下都是新表的默认空值：老调用点（只传前五个参数）不用改
    val memos: List<MemoEntity> = emptyList(),
    val milestones: List<MilestoneEntity> = emptyList(),
    val importantDates: List<ImportantDateEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
    val courses: List<CourseEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val studyTasks: List<StudyTaskEntity> = emptyList(),
    val grades: List<GradeEntity> = emptyList(),
    val creditTargets: List<CreditTargetEntity> = emptyList(),
    val awards: List<AwardEntity> = emptyList()
)
