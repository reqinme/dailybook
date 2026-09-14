package com.dailybook.app.backup

import android.content.Context
import android.net.Uri
import com.dailybook.app.data.DbSnapshot
import com.dailybook.app.data.FocusSessionEntity
import com.dailybook.app.data.RepeatRule
import com.dailybook.app.data.TodoEntity
import com.dailybook.app.data.TransactionEntity
import com.dailybook.app.data.TxType
import com.dailybook.app.util.formatAmount
import com.dailybook.app.util.toLocalDate
import org.json.JSONArray
import org.json.JSONObject
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

    /** 备份格式版本，将来结构变化时用来判断兼容性 */
    const val FORMAT = 1

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
                    put("note", tx.note)
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
                    put("createdAt", s.createdAt)
                })
            }
        })

        return root.toString(2)
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
                note = o.optString("note", ""),
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
                createdAt = o.optLong("createdAt", 0L)
            )
        }

        return ParsedBackup(
            snapshot = DbSnapshot(
                transactions = transactions,
                todos = todos.filter { it.title.isNotBlank() },
                focusSessions = sessions
            ),
            budgetCents = root.optLong("budgetCents", 0L).coerceAtLeast(0L)
        )
    }

    // ---------- 导出 CSV ----------

    /**
     * 记账流水导出为 CSV。开头写入 UTF-8 BOM，Excel 双击打开中文才不会乱码；
     * 换行用 CRLF，同样是照顾 Excel。
     */
    fun toCsv(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("日期,类型,分类,金额,备注\r\n")
        transactions
            .sortedWith(compareBy({ it.dateMillis }, { it.id }))
            .forEach { tx ->
                sb.append(csvCell(tx.dateMillis.toLocalDate().toString())).append(',')
                sb.append(csvCell(tx.type.label)).append(',')
                sb.append(csvCell(tx.category)).append(',')
                sb.append(csvCell(formatAmount(tx.amountCents))).append(',')
                sb.append(csvCell(tx.note))
                sb.append("\r\n")
            }
        return sb.toString()
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
