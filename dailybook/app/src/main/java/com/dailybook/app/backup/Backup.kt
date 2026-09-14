package com.dailybook.app.backup

import android.content.Context
import android.net.Uri
import com.dailybook.app.data.Accounts
import com.dailybook.app.data.AwardEntity
import com.dailybook.app.data.AwardKind
import com.dailybook.app.data.CourseEntity
import com.dailybook.app.data.CreditTargetEntity
import com.dailybook.app.data.Currencies
import com.dailybook.app.data.DateRepeat
import com.dailybook.app.data.DbSnapshot
import com.dailybook.app.data.ExamEntity
import com.dailybook.app.data.FocusSessionEntity
import com.dailybook.app.data.GradeEntity
import com.dailybook.app.data.HabitEntity
import com.dailybook.app.data.HabitLogEntity
import com.dailybook.app.data.ImportantDateEntity
import com.dailybook.app.data.MemoEntity
import com.dailybook.app.data.MilestoneEntity
import com.dailybook.app.data.RecurringEntity
import com.dailybook.app.data.RepeatRule
import com.dailybook.app.data.ScoreKind
import com.dailybook.app.data.StudyTaskEntity
import com.dailybook.app.data.SubtaskEntity
import com.dailybook.app.data.TodoEntity
import com.dailybook.app.data.TransactionEntity
import com.dailybook.app.data.TxType
import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.toDayMillis
import com.dailybook.app.util.toLocalDate
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** 备份文件解析结果 */
data class ParsedBackup(val snapshot: DbSnapshot, val budgetCents: Long)

/**
 * 备份文件的读写。
 *
 * 只用系统自带的 org.json，不引入任何第三方依赖；文件经系统文件选择器（SAF）
 * 落到用户自己挑的位置，所以 App 既不需要存储权限，也不联网。
 */
object Backup {

    /**
     * 备份格式版本。
     * 1：记账 / 待办 / 专注记录 + 预算
     * 2：记账多了「账户」，待办多了「重复规则」
     * 3：记账多了「标签 / 待报销 / 多币种」，专注记录多了「中断」
     * 4：多了「子任务」与「周期记账」，待办多了「优先级 / 手动排序」
     * 5：多了生活与学习两个模块——「备忘录 / 大事记 / 重要日期 / 习惯 + 打卡记录 /
     *    课表 / 考试 + 复习计划 / 成绩 / 学分要求 / 奖助记录」
     * 读取时兼容更旧的版本（缺字段就取默认值），比当前版本更新的才拒绝。
     */
    const val FORMAT = 5

    // ---------- 导出 ----------

    fun toJson(
        snapshot: DbSnapshot,
        budgetCents: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
        root.put("app", "dailybook")
        root.put("format", FORMAT)
        root.put("exportedAt", nowMillis)
        root.put("budgetCents", budgetCents)

        root.put("transactions", JSONArray().apply {
            snapshot.transactions.forEach { tx ->
                put(JSONObject().apply {
                    put("id", tx.id)
                    put("amountCents", tx.amountCents)
                    put("typeName", tx.typeName)
                    put("category", tx.category)
                    put("account", tx.account)
                    put("note", tx.note)
                    put("tags", tx.tags)
                    put("reimbursable", tx.reimbursable)
                    put("reimbursed", tx.reimbursed)
                    put("currency", tx.currency)
                    put("foreignAmountCents", tx.foreignAmountCents)
                    put("rateScaled", tx.rateScaled)
                    put("dateMillis", tx.dateMillis)
                    put("createdAt", tx.createdAt)
                })
            }
        })

        root.put("todos", JSONArray().apply {
            snapshot.todos.forEach { todo ->
                put(JSONObject().apply {
                    put("id", todo.id)
                    put("title", todo.title)
                    put("done", todo.done)
                    put("important", todo.important)
                    // org.json 的 put(key, null) 是「删掉这个键」，所以空值要写 JSONObject.NULL
                    put("dueMillis", todo.dueMillis ?: JSONObject.NULL)
                    put("repeatRule", todo.repeatRule)
                    put("priority", todo.priority)
                    put("sortOrder", todo.sortOrder)
                    put("createdAt", todo.createdAt)
                })
            }
        })

        root.put("focusSessions", JSONArray().apply {
            snapshot.focusSessions.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("startedAtMillis", s.startedAtMillis)
                    put("endedAtMillis", s.endedAtMillis)
                    put("minutes", s.minutes)
                    put("taskTitle", s.taskTitle)
                    put("interrupted", s.interrupted)
                    put("createdAt", s.createdAt)
                })
            }
        })

        appendExtras(root, snapshot)
        appendLife(root, snapshot)
        appendStudy(root, snapshot)

        return root.toString(2)
    }

    /** 子任务与周期记账是 v1.7 新增的表，单独拼一段，避免把上面的函数撑得太长 */
    private fun appendExtras(root: JSONObject, snapshot: DbSnapshot) {
        root.put("subtasks", JSONArray().apply {
            snapshot.subtasks.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("todoId", item.todoId)
                    put("title", item.title)
                    put("done", item.done)
                    put("sortOrder", item.sortOrder)
                    put("createdAt", item.createdAt)
                })
            }
        })
        root.put("recurring", JSONArray().apply {
            snapshot.recurring.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("amountCents", item.amountCents)
                    put("typeName", item.typeName)
                    put("category", item.category)
                    put("account", item.account)
                    put("note", item.note)
                    put("tags", item.tags)
                    put("rule", item.rule)
                    put("nextDueMillis", item.nextDueMillis)
                    put("enabled", item.enabled)
                    put("createdAt", item.createdAt)
                })
            }
        })
    }

    /**
     * 生活模块的四张表（备忘录 / 大事记 / 重要日期 / 习惯 + 打卡记录）。
     * 字段名一律用实体自己的名字，读回来时能一一对上。
     */
    private fun appendLife(root: JSONObject, snapshot: DbSnapshot) {
        root.put("memos", JSONArray().apply {
            snapshot.memos.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("content", item.content)
                    put("pinned", item.pinned)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                })
            }
        })
        root.put("milestones", JSONArray().apply {
            snapshot.milestones.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("note", item.note)
                    put("dateMillis", item.dateMillis)
                    put("imageUri", item.imageUri)
                    put("createdAt", item.createdAt)
                })
            }
        })
        root.put("importantDates", JSONArray().apply {
            snapshot.importantDates.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("dateMillis", item.dateMillis)
                    put("lunar", item.lunar)
                    put("lunarMonth", item.lunarMonth)
                    put("lunarDay", item.lunarDay)
                    put("lunarLeap", item.lunarLeap)
                    // 枚举一律写名字，不写界面上显示的文案
                    put("repeat", item.repeat)
                    put("remindDaysBefore", item.remindDaysBefore)
                    put("note", item.note)
                    put("createdAt", item.createdAt)
                })
            }
        })
        root.put("habits", JSONArray().apply {
            snapshot.habits.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("emoji", item.emoji)
                    put("targetPerDay", item.targetPerDay)
                    put("unit", item.unit)
                    put("daysPerWeek", item.daysPerWeek)
                    put("sortOrder", item.sortOrder)
                    put("archived", item.archived)
                    put("createdAt", item.createdAt)
                })
            }
        })
        root.put("habitLogs", JSONArray().apply {
            snapshot.habitLogs.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("habitId", item.habitId)
                    put("dateMillis", item.dateMillis)
                    put("count", item.count)
                    put("note", item.note)
                    put("createdAt", item.createdAt)
                })
            }
        })
    }

    /** 学习模块的五张表（课表 / 考试 + 复习计划 / 成绩 / 学分要求 / 奖助记录） */
    private fun appendStudy(root: JSONObject, snapshot: DbSnapshot) {
        root.put("courses", JSONArray().apply {
            snapshot.courses.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("teacher", item.teacher)
                    put("location", item.location)
                    put("dayOfWeek", item.dayOfWeek)
                    put("startPeriod", item.startPeriod)
                    put("endPeriod", item.endPeriod)
                    put("weeks", item.weeks)
                    put("termStartMillis", item.termStartMillis)
                    put("colorIndex", item.colorIndex)
                    put("note", item.note)
                    put("createdAt", item.createdAt)
                })
            }
        })
        root.put("exams", JSONArray().apply {
            snapshot.exams.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("courseName", item.courseName)
                    put("examMillis", item.examMillis)
                    put("location", item.location)
                    put("note", item.note)
                    put("createdAt", item.createdAt)
                })
            }
        })
        root.put("studyTasks", JSONArray().apply {
            snapshot.studyTasks.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("examId", item.examId)
                    put("title", item.title)
                    put("done", item.done)
                    put("dateMillis", item.dateMillis)
                    put("sortOrder", item.sortOrder)
                    put("createdAt", item.createdAt)
                })
            }
        })
        root.put("grades", JSONArray().apply {
            snapshot.grades.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("term", item.term)
                    put("courseName", item.courseName)
                    put("credit", item.credit)
                    put("score", item.score)
                    put("scoreKind", item.scoreKind)
                    put("point", item.point)
                    put("category", item.category)
                    put("note", item.note)
                    put("createdAt", item.createdAt)
                })
            }
        })
        root.put("creditTargets", JSONArray().apply {
            snapshot.creditTargets.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("category", item.category)
                    put("required", item.required)
                    put("createdAt", item.createdAt)
                })
            }
        })
        root.put("awards", JSONArray().apply {
            snapshot.awards.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("kind", item.kind)
                    put("dateMillis", item.dateMillis)
                    put("level", item.level)
                    put("note", item.note)
                    put("imageUri", item.imageUri)
                    put("createdAt", item.createdAt)
                })
            }
        })
    }

    /**
     * 解析备份文件。结构不对或版本不认识时抛 [IllegalArgumentException]，
     * 调用方直接把 message 提示给用户即可。
     */
    fun parse(text: String): ParsedBackup {
        val root = try {
            JSONObject(text)
        } catch (_: Exception) {
            throw IllegalArgumentException("不是有效的备份文件")
        }

        if (root.optString("app") != "dailybook") {
            throw IllegalArgumentException("这不是「日常本」的备份文件")
        }
        val format = root.optInt("format", 0)
        if (format <= 0 || format > FORMAT) {
            throw IllegalArgumentException("备份文件版本（$format）比当前版本新，请先升级 App")
        }

        val transactions = root.optJSONArray("transactions").mapObjects { o ->
            TransactionEntity(
                id = o.optLong("id", 0L),
                amountCents = o.optLong("amountCents", 0L),
                typeName = o.optString("typeName", TxType.EXPENSE.name),
                category = o.optString("category", "其他"),
                account = o.optString("account", Accounts.DEFAULT).ifBlank { Accounts.DEFAULT },
                note = o.optString("note", ""),
                tags = o.optString("tags", ""),
                reimbursable = o.optBoolean("reimbursable", false),
                reimbursed = o.optBoolean("reimbursed", false),
                currency = o.optString("currency", Currencies.BASE).ifBlank { Currencies.BASE },
                foreignAmountCents = o.optLong("foreignAmountCents", 0L),
                rateScaled = o.optLong("rateScaled", Currencies.RATE_SCALE)
                    .takeIf { it > 0L } ?: Currencies.RATE_SCALE,
                dateMillis = o.optLong("dateMillis", 0L),
                createdAt = o.optLong("createdAt", 0L)
            )
        }

        val todos = root.optJSONArray("todos").mapObjects { o ->
            TodoEntity(
                id = o.optLong("id", 0L),
                title = o.optString("title", ""),
                done = o.optBoolean("done", false),
                important = o.optBoolean("important", false),
                dueMillis = if (o.isNull("dueMillis")) null else o.optLong("dueMillis"),
                repeatRule = runCatching { RepeatRule.valueOf(o.optString("repeatRule")) }
                    .getOrDefault(RepeatRule.NONE)
                    .name,
                createdAt = o.optLong("createdAt", 0L)
            )
        }

        val sessions = root.optJSONArray("focusSessions").mapObjects { o ->
            FocusSessionEntity(
                id = o.optLong("id", 0L),
                startedAtMillis = o.optLong("startedAtMillis", 0L),
                endedAtMillis = o.optLong("endedAtMillis", 0L),
                minutes = o.optInt("minutes", 0),
                taskTitle = o.optString("taskTitle", ""),
                interrupted = o.optBoolean("interrupted", false),
                createdAt = o.optLong("createdAt", 0L)
            )
        }

        return ParsedBackup(
            snapshot = DbSnapshot(
                transactions = transactions,
                todos = todos.filter { it.title.isNotBlank() },
                focusSessions = sessions,
                subtasks = root.optJSONArray("subtasks").mapObjects { o ->
                    SubtaskEntity(
                        id = o.optLong("id", 0L),
                        todoId = o.optLong("todoId", 0L),
                        title = o.optString("title", ""),
                        done = o.optBoolean("done", false),
                        sortOrder = o.optLong("sortOrder", 0L),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                }.filter { it.title.isNotBlank() && it.todoId > 0L },
                recurring = root.optJSONArray("recurring").mapObjects { o ->
                    RecurringEntity(
                        id = o.optLong("id", 0L),
                        amountCents = o.optLong("amountCents", 0L),
                        typeName = o.optString("typeName", TxType.EXPENSE.name),
                        category = o.optString("category", "其他"),
                        account = o.optString("account", Accounts.DEFAULT)
                            .ifBlank { Accounts.DEFAULT },
                        note = o.optString("note", ""),
                        tags = o.optString("tags", ""),
                        rule = runCatching { RepeatRule.valueOf(o.optString("rule")) }
                            .getOrDefault(RepeatRule.MONTHLY).name,
                        nextDueMillis = o.optLong("nextDueMillis", 0L),
                        enabled = o.optBoolean("enabled", true),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                }.filter { it.amountCents > 0L },
                // ---- v5：生活模块 ----
                memos = root.optJSONArray("memos").mapObjects { o ->
                    MemoEntity(
                        id = o.optLong("id", 0L),
                        title = o.optString("title", ""),
                        content = o.optString("content", ""),
                        pinned = o.optBoolean("pinned", false),
                        createdAt = o.optLong("createdAt", 0L),
                        updatedAt = o.optLong("updatedAt", 0L)
                    )
                }.filter { it.title.isNotBlank() },
                milestones = root.optJSONArray("milestones").mapObjects { o ->
                    MilestoneEntity(
                        id = o.optLong("id", 0L),
                        title = o.optString("title", ""),
                        note = o.optString("note", ""),
                        dateMillis = o.optLong("dateMillis", 0L),
                        imageUri = o.optString("imageUri", ""),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                }.filter { it.title.isNotBlank() },
                importantDates = root.optJSONArray("importantDates").mapObjects { o ->
                    ImportantDateEntity(
                        id = o.optLong("id", 0L),
                        title = o.optString("title", ""),
                        dateMillis = o.optLong("dateMillis", 0L),
                        lunar = o.optBoolean("lunar", false),
                        lunarMonth = o.optInt("lunarMonth", 1),
                        lunarDay = o.optInt("lunarDay", 1),
                        lunarLeap = o.optBoolean("lunarLeap", false),
                        // 认不出来的重复方式退回「每年」，不丢这条日期
                        repeat = runCatching { DateRepeat.valueOf(o.optString("repeat")) }
                            .getOrDefault(DateRepeat.YEARLY).name,
                        remindDaysBefore = o.optInt("remindDaysBefore", 0),
                        note = o.optString("note", ""),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                }.filter { it.title.isNotBlank() },
                habits = root.optJSONArray("habits").mapObjects { o ->
                    HabitEntity(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", ""),
                        emoji = o.optString("emoji", "✅"),
                        targetPerDay = o.optInt("targetPerDay", 1).coerceAtLeast(1),
                        unit = o.optString("unit", "次"),
                        daysPerWeek = o.optInt("daysPerWeek", 7).coerceIn(1, 7),
                        sortOrder = o.optLong("sortOrder", 0L),
                        archived = o.optBoolean("archived", false),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                }.filter { it.name.isNotBlank() },
                habitLogs = root.optJSONArray("habitLogs").mapObjects { o ->
                    HabitLogEntity(
                        id = o.optLong("id", 0L),
                        habitId = o.optLong("habitId", 0L),
                        dateMillis = o.optLong("dateMillis", 0L),
                        count = o.optInt("count", 0),
                        note = o.optString("note", ""),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                }.filter { it.habitId > 0L && it.dateMillis > 0L },
                // ---- v5：学习模块 ----
                courses = root.optJSONArray("courses").mapObjects { o ->
                    CourseEntity(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", ""),
                        teacher = o.optString("teacher", ""),
                        location = o.optString("location", ""),
                        dayOfWeek = o.optInt("dayOfWeek", 1).coerceIn(1, 7),
                        startPeriod = o.optInt("startPeriod", 1),
                        endPeriod = o.optInt("endPeriod", 2),
                        weeks = o.optString("weeks", ""),
                        termStartMillis = o.optLong("termStartMillis", 0L),
                        colorIndex = o.optInt("colorIndex", 0),
                        note = o.optString("note", ""),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                }.filter { it.name.isNotBlank() },
                exams = root.optJSONArray("exams").mapObjects { o ->
                    ExamEntity(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", ""),
                        courseName = o.optString("courseName", ""),
                        examMillis = o.optLong("examMillis", 0L),
                        location = o.optString("location", ""),
                        note = o.optString("note", ""),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                }.filter { it.name.isNotBlank() },
                studyTasks = root.optJSONArray("studyTasks").mapObjects { o ->
                    StudyTaskEntity(
                        id = o.optLong("id", 0L),
                        examId = o.optLong("examId", 0L),
                        title = o.optString("title", ""),
                        done = o.optBoolean("done", false),
                        dateMillis = o.optLong("dateMillis", 0L),
                        sortOrder = o.optLong("sortOrder", 0L),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                }.filter { it.title.isNotBlank() },
                grades = root.optJSONArray("grades").mapObjects { o ->
                    GradeEntity(
                        id = o.optLong("id", 0L),
                        term = o.optString("term", ""),
                        courseName = o.optString("courseName", ""),
                        credit = o.optDouble("credit", 0.0),
                        score = o.optString("score", ""),
                        // 认不出来的计分方式退回百分制
                        scoreKind = runCatching { ScoreKind.valueOf(o.optString("scoreKind")) }
                            .getOrDefault(ScoreKind.PERCENT).name,
                        point = o.optDouble("point", 0.0),
                        category = o.optString("category", "必修"),
                        note = o.optString("note", ""),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                }.filter { it.courseName.isNotBlank() },
                creditTargets = root.optJSONArray("creditTargets").mapObjects { o ->
                    CreditTargetEntity(
                        id = o.optLong("id", 0L),
                        category = o.optString("category", ""),
                        required = o.optDouble("required", 0.0),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                }.filter { it.category.isNotBlank() },
                awards = root.optJSONArray("awards").mapObjects { o ->
                    AwardEntity(
                        id = o.optLong("id", 0L),
                        title = o.optString("title", ""),
                        // 认不出来的类别退回「其他」
                        kind = runCatching { AwardKind.valueOf(o.optString("kind")) }
                            .getOrDefault(AwardKind.OTHER).name,
                        dateMillis = o.optLong("dateMillis", 0L),
                        level = o.optString("level", ""),
                        note = o.optString("note", ""),
                        imageUri = o.optString("imageUri", ""),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                }.filter { it.title.isNotBlank() },
            ),
            budgetCents = root.optLong("budgetCents", 0L).coerceAtLeast(0L)
        )
    }

    // ---------- 导出 CSV ----------

    /**
     * 记账流水导出为 CSV。开头写入 UTF-8 BOM，Excel 双击打开中文才不会乱码；
     * 换行用 CRLF，同样是照顾 Excel。
     */
    fun toCsv(transactions: List<TransactionEntity>, lang: Lang = Lang.DEFAULT): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append(
            when (lang) {
                Lang.ZH_CN -> "日期,类型,分类,账户,金额,备注,标签"
                Lang.ZH_TW -> "日期,類型,分類,帳戶,金額,備註,標籤"
                Lang.EN -> "Date,Type,Category,Account,Amount,Note,Tags"
                Lang.JA -> "日付,種別,カテゴリ,口座,金額,メモ,タグ"
            }
        ).append("\r\n")
        transactions
            .sortedWith(compareBy({ it.dateMillis }, { it.id }))
            .forEach { tx ->
                sb.append(csvCell(tx.dateMillis.toLocalDate().toString())).append(',')
                sb.append(csvCell(tx.type.label(lang))).append(',')
                sb.append(csvCell(tx.category)).append(',')
                sb.append(csvCell(tx.account)).append(',')
                sb.append(csvCell(formatAmount(tx.amountCents))).append(',')
                sb.append(csvCell(tx.note)).append(',')
                sb.append(csvCell(tx.tags))
                sb.append("\r\n")
            }
        return sb.toString()
    }

    // ---------- 导入 CSV ----------

    /**
     * 解析记账 CSV。兼容本 App 导出的三种列数：
     * 5 列（v1.3：日期,类型,分类,金额,备注）、6 列（多「账户」）、7 列（多「标签」）。
     * 表头行自动跳过；解析不了的行直接忽略，所以脏数据不会让整次导入失败。
     */
    fun parseCsv(text: String, lang: Lang = Lang.DEFAULT): List<TransactionEntity> {
        val rows = mutableListOf<TransactionEntity>()
        val now = System.currentTimeMillis()
        text.removePrefix("\uFEFF").split('\n').forEach { raw ->
            val line = raw.trimEnd('\r')
            if (line.isBlank()) return@forEach
            val cells = splitCsvLine(line)
            if (cells.size < 5) return@forEach

            val date = runCatching { LocalDate.parse(cells[0].trim()) }.getOrNull() ?: return@forEach
            val type = typeOfCell(cells[1], lang)
            val category = cells[2].trim().ifBlank { "其他" }

            // 列数不同，金额 / 备注 / 标签的位置也不同
            val account: String
            val amountCell: String
            val note: String
            val tags: String
            if (cells.size >= 7) {
                account = cells[3].trim().ifBlank { Accounts.DEFAULT }
                amountCell = cells[4]
                note = cells[5].trim()
                tags = cells[6].trim()
            } else if (cells.size == 6) {
                account = cells[3].trim().ifBlank { Accounts.DEFAULT }
                amountCell = cells[4]
                note = cells[5].trim()
                tags = ""
            } else {
                account = Accounts.DEFAULT
                amountCell = cells[3]
                note = cells[4].trim()
                tags = ""
            }

            val cents = centsOf(amountCell) ?: return@forEach
            rows += TransactionEntity(
                amountCents = cents,
                typeName = type.name,
                category = category,
                account = account,
                note = note,
                tags = tags,
                currency = Currencies.BASE,
                foreignAmountCents = cents,
                rateScaled = Currencies.RATE_SCALE,
                dateMillis = date.toDayMillis(),
                createdAt = now
            )
        }
        return rows
    }

    /**
     * 导入去重用的指纹：日期 + 金额 + 类型 + 分类 + 账户 + 备注。
     * 故意不含 id（导入的记录 id 是新的）与创建时间（同一笔两次导入会不同），
     * 这样「同一份文件重复导入」能稳定识别成重复，而金额或日期不同的真实新记录不会误判。
     */
    fun fingerprint(tx: TransactionEntity): String = listOf(
        tx.dateMillis.toString(),
        tx.amountCents.toString(),
        tx.typeName,
        tx.category,
        tx.account,
        tx.note
    ).joinToString("|")

    /** 按行拆 CSV，支持引号包裹与转义的双引号 */    private fun splitCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    cells += sb.toString()
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        cells += sb.toString()
        return cells
    }

    private fun centsOf(raw: String): Long? {
        val text = raw.trim().replace(",", "").replace("¥", "").replace("$", "").replace("€", "")
        if (text.isEmpty()) return null
        return runCatching {
            BigDecimal(text).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        }.getOrNull()?.takeIf { it > 0L }
    }

    /** 类型列可能是四种语言里任意一种（导出时是本地化的），也可能是枚举名 */
    private fun typeOfCell(raw: String, lang: Lang): TxType {
        val value = raw.trim()
        runCatching { TxType.valueOf(value.uppercase()) }.getOrNull()?.let { return it }
        val incomeLabels = Lang.entries.map { AppStrings.txIncome(it) }
        return if (incomeLabels.any { it.equals(value, ignoreCase = true) }) TxType.INCOME
        else TxType.EXPENSE
    }

    private fun csvCell(raw: String): String {
        val needsQuote = raw.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuote) return raw
        return "\"" + raw.replace("\"", "\"\"") + "\""
    }

    // ---------- 文件读写（经系统文件选择器给的 Uri） ----------

    fun writeText(context: Context, uri: Uri, text: String) {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("无法写入所选文件")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    fun readText(context: Context, uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("无法读取所选文件")
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    /** 默认文件名：日常本备份-2026-09-14.json */
    fun suggestName(prefix: String, extension: String, today: LocalDate = LocalDate.now()): String =
        "$prefix-${today}.$extension"
}

/** JSONArray → List，数组缺失时返回空列表 */
private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val item = optJSONObject(i) ?: continue
        out += transform(item)
    }
    return out
}
