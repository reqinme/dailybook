package com.dailybook.app.data

import android.content.Context
import androidx.room.withTransaction
import com.dailybook.app.util.toDayMillis
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

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
        if (!done || !item.repeats) {
            todoDao.update(item.copy(done = done))
            return
        }
        val base = item.dueMillis ?: LocalDate.now().toDayMillis()
        val next = nextDueMillisOf(base, item.repeat)
        db.withTransaction {
            todoDao.update(item.copy(done = true))
            // 同一个到期日只生成一条：连点两下勾选框（或先勾后取消再勾）都不会冒出重复的下一次
            if (next != null && todoDao.findPending(item.title, next) == null) {
                todoDao.insert(item.copy(id = 0L, done = false, dueMillis = next))
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

    suspend fun clearTodos() = todoDao.clearAll()

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
     * 把到期的周期记账补成真实记录，并把下一次往后推。
     * 只补「到期的那一次」，长期没打开 App 也不会一次刷出一堆；
     * 返回这次补记了几笔（0 表示没有到期的）。
     */
    suspend fun materializeRecurring(today: LocalDate = LocalDate.now()): Int {
        val todayMillis = today.toDayMillis()
        val due = recurringDao.getAll().filter { it.enabled && it.nextDueMillis <= todayMillis }
        if (due.isEmpty()) return 0

        db.withTransaction {
            due.forEach { rule ->
                txDao.insert(
                    TransactionEntity(
                        amountCents = rule.amountCents,
                        typeName = rule.typeName,
                        category = rule.category,
                        account = rule.account,
                        note = rule.note,
                        tags = rule.tags,
                        dateMillis = rule.nextDueMillis,
                        createdAt = System.currentTimeMillis()
                    )
                )
                val next = nextDueMillisOf(rule.nextDueMillis, rule.repeat, today)
                    ?: rule.nextDueMillis
                recurringDao.update(rule.copy(nextDueMillis = next))
            }
        }
        return due.size
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

    suspend fun addCourse(
        name: String,
        teacher: String = "",
        location: String = "",
        dayOfWeek: Int = 1,
        startPeriod: Int = 1,
        endPeriod: Int = 2,
        weeks: String = "",
        termStartMillis: Long,
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
