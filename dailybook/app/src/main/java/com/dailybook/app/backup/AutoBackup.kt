package com.dailybook.app.backup

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.dailybook.app.data.DailyRepository
import com.dailybook.app.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 自动备份的结果。[AutoBackup.runBackupIfDue] 只回这个枚举，
 * 具体文案由设置页负责，本文件里没有任何面向用户的句子（也不弹 Toast）。
 */
enum class BackupOutcome {
    /** 这次真的写出了一份备份 */
    SUCCESS,

    /** 没开自动备份，或者还没挑文件夹 */
    NOT_CONFIGURED,

    /** 距上次成功备份不到 24 小时（或刚失败过、还在退避期），这次不做 */
    NOT_DUE,

    /** 真的试了但失败了，失败原因记在 prefs 里，见 [AutoBackup.lastFailureReason] */
    FAILED
}

/**
 * 失败原因的「代码」，不是句子。设置页按这个键自己查词条。
 * 都是英语常量，别塞中文进来，否则这个模块就不是纯返 key 了。
 */
enum class BackupFailureCode {
    /** SAF 没给我们持久化权限（用户换过文件夹、权限被系统回收、提供方不支持） */
    NO_PERMISSION,

    /** 建文件失败 */
    CREATE_FAILED,

    /** 写文件失败 */
    WRITE_FAILED,

    /** 轮换清理那一步没做成（列目录或删除时抛了）。注意：它和 [SUCCESS] 可能同时成立 —— 备份本身写成功了 */
    LIST_FAILED,

    /** 其他意外（建仓库、取数据、拼 JSON 时炸的） */
    UNKNOWN;

    /** 把任意异常映射成最接近的失败代码 */
    companion object {
        internal fun of(error: Throwable): BackupFailureCode = when (error) {
            is SecurityException -> NO_PERMISSION
            is java.io.FileNotFoundException -> CREATE_FAILED
            is java.io.IOException -> WRITE_FAILED
            else -> UNKNOWN
        }
    }
}

/**
 * 自动备份：把整份数据写成 JSON，放进用户自己挑的文件夹。
 *
 * 设计取舍（都写在这里，免得以后被当成 bug）：
 *
 * 1. **它就是一个普通 JSON 文件，放在用户挑的文件夹里**（SAF，`OpenDocumentTree` 选的树）。
 *    正因为文件在 App 私有目录之外，卸载重装、清数据都不会把它带走，换机时直接拷走即可 ——
 *    这就是「自动备份」真正的价值。反过来也意味着：这个文件在别的 App/云同步盘里是明文可读的，
 *    里面是记账和待办，介意的话别挑同步盘。
 * 2. **轮换只保留最新的 7 份**（按文件名里的日期排序），第 8 份开始删最旧的。
 *    只删文件名以 [FILE_PREFIX] 开头的文件，用户自己放进这个文件夹的任何东西一律不碰。
 * 3. **触发时机只有两个：App 打开时、以及每晚记账提醒闹钟响的时候。** 这里没有任何后台任务、
 *    没有 `WorkManager`、没有前台服务。所以：**用户长期不开 App，就不会有新的备份** ——
 *    这是明确的已知限制，不是没做完。真的想要「不开 App 也备份」，得另加
 *    `WorkManager` 之类的依赖，本模块故意不引。
 * 4. 一天最多一份：文件名叫 `DailyBook-backup-YYYYMMDD.json`，同一天再备份就覆盖那一个文件，
 *    而不是又建一个 `... (1).json`。所以反复开 App 不会刷出一堆文件。
 * 5. 失败就是失败：[BackupOutcome.FAILED] 不会伪装成成功，失败时间与原因码都落盘，
 *    设置页可以直接展示；并且失败后有 1 小时退避，避免每次开 App 都对着一个坏文件夹猛撞。
 */
class AutoBackup private constructor(context: Context) {

    private val app: Context = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    /** 自动备份开关 */
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _folderUri = MutableStateFlow(prefs.getString(KEY_FOLDER_URI, null))
    /** 用户挑的文件夹（树 URI 字符串）；没挑过就是 null */
    val folderUri: StateFlow<String?> = _folderUri.asStateFlow()

    private val _lastBackup = MutableStateFlow(prefs.getLong(KEY_LAST_AT, 0L))
    /** 上次成功备份的时间戳（毫秒），0 = 从来没成功过 */
    val lastBackupAt: StateFlow<Long> = _lastBackup.asStateFlow()

    private val _failure = MutableStateFlow(readFailure())
    /** 上次失败的原因码；null = 没有未处理的失败 */
    val lastFailure: StateFlow<BackupFailureCode?> = _failure.asStateFlow()

    // ---------- 给设置页用 ----------

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
    }

    /**
     * 用户在设置页用 `ActivityResultContracts.OpenDocumentTree()` 挑完文件夹后调用。
     *
     * 先申请持久化读写权限（不申请的话重启 App 后这个 Uri 就废了），但**申请失败不算错**：
     * 有的提供方（某些第三方文件管理器）就是不支持持久化授权，这时我们照样把 Uri 存下来，
     * 本次运行内还能用；下次跑备份会发现权限没了，返回 [BackupOutcome.FAILED] +
     * [BackupFailureCode.NO_PERMISSION]，让用户重新挑一个 —— 不崩，也不假装成功。
     *
     * 存下新文件夹时会顺手清掉旧的失败记录（换了位置就该重新试一次）。
     */
    fun setFolder(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { app.contentResolver.takePersistableUriPermission(uri, flags) }
            .onFailure { error ->
                // 提供方不支持持久化授权时这里会抛，属预期，不当失败处理
                Log.w(TAG, "takePersistableUriPermission failed", error)
            }

        prefs.edit()
            .putString(KEY_FOLDER_URI, uri.toString())
            .remove(KEY_FAILED_AT)
            .remove(KEY_FAILED_CODE)
            .apply()
        _folderUri.value = uri.toString()
        _failure.value = null
    }

    /** 上次失败的原因码；null = 没有未处理的失败 */
    fun lastFailureReason(): BackupFailureCode? = _failure.value

    /** 上次失败的时间戳（毫秒），0 = 没有；设置页可以拿它显示「什么时候失败的」 */
    fun lastFailureMillis(): Long = prefs.getLong(KEY_FAILED_AT, 0L)

    /**
     * 文件夹的展示用短名（树 URI 的最后一段）。
     * 只是给界面一个提示，不同提供方给的可能是文档 id，不保证好看，也不保证是真实路径。
     */
    fun folderLabel(): String? = _folderUri.value
        ?.let { runCatching { Uri.parse(it).lastPathSegment }.getOrNull() }
        ?.substringAfterLast(':')
        ?.takeIf { it.isNotBlank() }

    // ---------- 核心 ----------

    /**
     * App 打开时、每晚提醒闹钟响时调这个。**永远不会抛异常**，最差也是返回
     * [BackupOutcome.FAILED] 并把原因码记在 prefs 里。
     *
     * 适合从后台协程调用（内部会读 Room，不能在主线程直接调）。
     */
    suspend fun runBackupIfDue(nowMillis: Long = System.currentTimeMillis()): BackupOutcome {
        val folder = _folderUri.value?.takeIf { it.isNotBlank() }
        if (!_enabled.value || folder == null) return BackupOutcome.NOT_CONFIGURED

        if (nowMillis - _lastBackup.value < MIN_INTERVAL_MILLIS) return BackupOutcome.NOT_DUE
        // 上次失败后一小时内不重试，否则每次开 App 都会对着一个坏文件夹猛撞
        val failedAt = prefs.getLong(KEY_FAILED_AT, 0L)
        if (failedAt > 0L && nowMillis - failedAt < FAILURE_BACKOFF_MILLIS) return BackupOutcome.NOT_DUE

        return run(folder, nowMillis)
    }

    /**
     * 「立即备份」：跳过 24 小时与退避判断，设置页的按钮 / 调试用。
     * 其余行为（当日覆盖、轮换、失败记录）与 [runBackupIfDue] 完全一致。
     */
    suspend fun runBackupNow(nowMillis: Long = System.currentTimeMillis()): BackupOutcome {
        val folder = _folderUri.value?.takeIf { it.isNotBlank() }
        if (!_enabled.value || folder == null) return BackupOutcome.NOT_CONFIGURED
        return run(folder, nowMillis)
    }

    private suspend fun run(folderUri: String, nowMillis: Long): BackupOutcome = try {
        val treeUri = Uri.parse(folderUri)

        // 先确认权限还在。不在了就直接判失败，省得建文件时抛一个含义模糊的异常
        if (!hasPersistedPermission(treeUri)) {
            recordFailure(nowMillis, BackupFailureCode.NO_PERMISSION)
            BackupOutcome.FAILED
        } else {
            val today = LocalDate.now()
            val displayName = fileNameFor(today)
            val json = buildJson(nowMillis)
            writeInto(app, treeUri, displayName, json)
            // 顺序很要紧：**先记成功，再轮换**。
            // 以前是反过来的（写 LIST_FAILED 之后又调 recordSuccess），而 recordSuccess 会把
            // 失败码与 `_failure` 一起清掉 —— 于是「只保留最近 7 份」的清理失败被成功记账抹掉，
            // 设置页永远看不到，失败可以无声无息地一直发生。
            // 现在两件事各记各的：KEY_LAST_AT = 这次写成功的时间，KEY_FAILED_* = 清理没做成
            // （recordFailure 不碰 KEY_LAST_AT，所以「上次成功」仍然是真实的上次成功时刻）。
            recordSuccess(nowMillis)
            runCatching { prune(treeUri) }
                .onFailure { error ->
                    Log.w(TAG, "prune failed", error)
                    recordFailure(nowMillis, BackupFailureCode.LIST_FAILED)
                }
            BackupOutcome.SUCCESS
        }
    } catch (error: Throwable) {
        // OutOfMemoryError 之类的 Error 也一起吃掉：这个函数对调用方的承诺是「不抛」
        recordFailure(nowMillis, BackupFailureCode.of(error))
        Log.w(TAG, "auto backup failed", error)
        BackupOutcome.FAILED
    }

    /** 与手动导出同样的一份 JSON（带预算与各类设置，导回去能完整恢复），格式交给 [Backup] 自己维护 */
    private suspend fun buildJson(nowMillis: Long): String {
        val snapshot = DailyRepository(app).snapshot()
        val budget = SettingsStore.get(app).monthlyBudgetCents.value
        // 设置那一段和「导出备份」按钮走同一个 readBackupSettings：
        // 自动备份出来的文件和手动导出的文件内容一样，恢复哪个都不会少设置
        return Backup.toJson(snapshot, budget, readBackupSettings(app), nowMillis)
    }

    private fun hasPersistedPermission(treeUri: Uri): Boolean = try {
        app.contentResolver.persistedUriPermissions.any {
            it.isWritePermission && it.uri == treeUri
        }
    } catch (error: Exception) {
        // 拿不到授权列表时别拦着自己，让它真去写一次，失败了自然会被 catch 住
        Log.w(TAG, "persistedUriPermissions failed", error)
        true
    }

    private fun recordSuccess(nowMillis: Long) {
        prefs.edit()
            .putLong(KEY_LAST_AT, nowMillis)
            .remove(KEY_FAILED_AT)
            .remove(KEY_FAILED_CODE)
            .apply()
        _lastBackup.value = nowMillis
        _failure.value = null
    }

    private fun recordFailure(nowMillis: Long, code: BackupFailureCode) {
        prefs.edit()
            .putLong(KEY_FAILED_AT, nowMillis)
            .putString(KEY_FAILED_CODE, code.name)
            .apply()
        // 失败不动 KEY_LAST_AT：UI 上「上次成功」必须还是上一次成功
        _failure.value = code
    }

    private fun readFailure(): BackupFailureCode? = runCatching {
        prefs.getString(KEY_FAILED_CODE, null)?.let { BackupFailureCode.valueOf(it) }
    }.getOrNull()

    // ---------- 同一个文件夹里的子文档 ----------

    private data class ChildDoc(
        val id: String,
        val name: String,
        val lastModified: Long?
    )

    /** 列树下的直接子文件；单个坏行跳过，不因为一行读不出来就整次失败 */
    private fun children(treeUri: Uri): List<ChildDoc> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        val out = ArrayList<ChildDoc>()
        app.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (cursor.moveToNext()) {
                val id = if (idIndex >= 0) cursor.getString(idIndex) else null
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                if (id.isNullOrBlank() || name.isNullOrBlank()) continue
                val modified = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) {
                    cursor.getLong(modifiedIndex)
                } else {
                    null
                }
                out += ChildDoc(id = id, name = name, lastModified = modified)
            }
        }
        return out
    }

    // ---------- 写 ----------

    /**
     * 把 [text] 写进 [treeUri] 下名为 [displayName] 的文件。
     *
     * 当天已经有一份 `DailyBook-backup-<今天>.json` 就**覆盖**它（内容整体替换），
     * 不再 `createDocument` 出第二个 —— 不然同一天开两次 App 就会多出个 `... (1).json`。
     * 列表读不出来（提供方不支持 query）时退回「新建」，大不了多一个同后缀文件，由轮换收拾。
     */
    private fun writeInto(context: Context, treeUri: Uri, displayName: String, text: String) {
        val resolver = context.contentResolver
        val existing = runCatching { children(treeUri) }
            .onFailure { Log.w(TAG, "list children failed", it) }
            .getOrDefault(emptyList())
            .firstOrNull { it.name == displayName }

        val target = existing?.let {
            DocumentsContract.buildDocumentUriUsingTree(treeUri, it.id)
        } ?: DocumentsContract.createDocument(
            resolver,
            treeUri,
            MIME_JSON,
            displayName
        ) ?: throw java.io.IOException("createDocument returned null")

        val stream = resolver.openOutputStream(target)
            ?: throw java.io.IOException("openOutputStream returned null")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    // ---------- 轮换：只留最新 7 份 ----------

    /**
     * 只保留最新的 [KEEP] 份备份，更旧的删掉。
     *
     * 排序键是**文件名里的日期**（`DailyBook-backup-YYYYMMDD.json` 的 8 位数字），
     * 不是提供方给的修改时间 —— 有些提供方根本不填 `COLUMN_LAST_MODIFIED`，
     * 而且覆盖写同一天文件时修改时间会变，用文件名才稳定。
     * 文件名解析不出日期的（几乎不可能，因为前缀是我们自己写的）当作最旧，先被删。
     *
     * **只处理名字以 [FILE_PREFIX] 开头的文件**，用户在同一个文件夹里放的其它东西一律不删。
     *
     * 会抛（列目录 / 删除失败），由调用方兜住并记成 [BackupFailureCode.LIST_FAILED]。
     */
    private fun prune(treeUri: Uri) {
        val mine = children(treeUri).filter { it.name.startsWith(FILE_PREFIX) }
        if (mine.size <= KEEP) return

        val doomed = mine
            .sortedWith(compareByDescending<ChildDoc> { sortKeyOf(it.name) }.thenBy { it.name })
            .drop(KEEP)

        doomed.forEach { doc ->
            runCatching {
                DocumentsContract.deleteDocument(
                    app.contentResolver,
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, doc.id)
                )
            }.onFailure { Log.w(TAG, "delete failed: ${doc.name}", it) }
        }
    }

    /** 文件名 → 可排序的键：日期数字越大越新；解析不出日期的给 Long.MIN_VALUE（当最旧） */
    private fun sortKeyOf(name: String): Long {
        val digits = name.removePrefix(FILE_PREFIX).substringBefore('.').takeWhile { it.isDigit() }
        return digits.toLongOrNull() ?: Long.MIN_VALUE
    }

    companion object {
        private const val TAG = "AutoBackup"

        /** 自己的 prefs 文件；故意不塞进 SettingsStore，免得动那个类 */
        const val PREFS = "dailybook_autobackup"

        /** 备份文件名前缀，也是轮换时唯一的「这是我们的文件」判据，不匹配的绝不删 */
        const val FILE_PREFIX = "DailyBook-backup-"

        const val MIME_JSON = "application/json"

        /** 每 24 小时最多一份 */
        const val MIN_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L

        /** 失败后退避一小时再试 */
        const val FAILURE_BACKOFF_MILLIS = 60L * 60L * 1000L

        /** 轮换保留的份数 */
        const val KEEP = 7

        // ---- prefs 键 ----
        const val KEY_ENABLED = "enabled"
        const val KEY_FOLDER_URI = "folder_uri"
        const val KEY_LAST_AT = "last_backup_at"
        const val KEY_FAILED_AT = "last_failure_at"
        const val KEY_FAILED_CODE = "last_failure_code"

        private val NAME_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        /** `DailyBook-backup-20260914.json`（本地日期，跟用户的「今天」一致） */
        fun fileNameFor(date: LocalDate): String = "$FILE_PREFIX${date.format(NAME_DATE)}.json"

        @Volatile
        // 这里持有的是 applicationContext（进程级、跟界面生命周期无关），
        // 所以不是内存泄漏；lint 只看到「静态字段里有 Context」就报，属于误报。
        @SuppressLint("StaticFieldLeak")
        private var instance: AutoBackup? = null

        fun get(context: Context): AutoBackup =
            instance ?: synchronized(this) {
                instance ?: AutoBackup(context.applicationContext).also { instance = it }
            }

        /**
         * 便捷入口：`AutoBackup.runBackupIfDue(context)`。
         * 给 MainActivity 的启动钩子和提醒闹钟的 BroadcastReceiver 用，省得它们自己 `get`。
         */
        suspend fun runBackupIfDue(
            context: Context,
            nowMillis: Long = System.currentTimeMillis()
        ): BackupOutcome = get(context).runBackupIfDue(nowMillis)
    }
}
