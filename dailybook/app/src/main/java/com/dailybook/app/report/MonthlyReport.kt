package com.dailybook.app.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.dailybook.app.i18n.Lang
import com.dailybook.app.util.formatAmount

/**
 * 月报数据。
 *
 * 这个模块故意「哑」：所有文案（标题、小节名、金额、洞察句子）都由界面层用
 * i18n 里的 pick/pickf 先拼好再传进来，模块只负责排版，不碰任何字符串资源。
 * 这样四种语言、以及以后加语言，都只需要改 i18n，不用动导出逻辑。
 */
data class MonthlyReportData(
    /** 报告主标题，例如「2026-09 月报」 */
    val title: String,
    /** 月份文字，例如「2026年9月」 */
    val monthLabel: String,
    /** 应用名，出现在页脚 */
    val appName: String,
    /** 本月概览 */
    val summarySectionTitle: String,
    val expenseLabel: String,
    val incomeLabel: String,
    val balanceLabel: String,
    val expenseCents: Long,
    val incomeCents: Long,
    val balanceCents: Long,
    /** 一行补充说明，例如「2026年9月 的收入与支出」 */
    val summaryNote: String,
    /** 每日支出小节；[DailyBarData.ratio] 是 0～1 的相对高度 */
    val dailySectionTitle: String,
    val dailyBars: List<DailyBarData>,
    /** 分类小节；[CategoryData.ratio] 是 0～1 的占比 */
    val categorySectionTitle: String,
    val categoryTopLabel: String,
    val categories: List<CategoryData>,
    /** 专注小节 */
    val focusSectionTitle: String,
    val focusLine: String,
    /** 环比小节：每条是「标签 · 数值 · 变化」 */
    val compareSectionTitle: String,
    val comparisons: List<CompareData>,
    /** 洞察小节：界面层已经拼好的完整句子 */
    val insightSectionTitle: String,
    val insights: List<String>,
    /** 数据为空时的小节占位文案 */
    val emptyText: String
)

/** 月报里的一根柱（每天支出） */
data class DailyBarData(val label: String, val ratio: Float)

/** 月报里的一个分类行 */
data class CategoryData(val name: String, val amountText: String, val ratio: Float)

/** 月报里的一条环比 */
data class CompareData(val label: String, val amountText: String, val deltaText: String)

// ---------- 配色（与 App 的品牌色保持一致，导出件自带颜色，不依赖主题） ----------

private const val BRAND = "#17897F"
private const val EXPENSE = "#D1453B"
private const val INCOME = "#1F8F68"
private const val TEXT_MAIN = "#1B1C1F"
private const val TEXT_DIM = "#5C6069"
private const val CARD_BG = "#F5F7F8"
private const val LINE = "#DCE0E6"

private const val FONT_STACK =
    "-apple-system,BlinkMacSystemFont,'Segoe UI','Noto Sans CJK SC','Microsoft YaHei',sans-serif"

private const val SOFTWARE = "DailyBook"

// ==================== HTML ====================

/**
 * 生成一份完全自包含的 UTF-8 HTML 月报：样式全内联在 <style> 里，
 * 没有任何外链、脚本或网络请求，离线双击就能看。
 */
fun buildHtml(data: MonthlyReportData, lang: Lang): String {
    val sb = StringBuilder()
    sb.append("<!DOCTYPE html>\n")
    sb.append("<html lang=\"").append(langTag(lang)).append("\">\n<head>\n")
    sb.append("<meta charset=\"utf-8\">\n")
    sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
    sb.append("<meta name=\"generator\" content=\"").append(SOFTWARE).append("\">\n")
    sb.append("<title>").append(esc(data.title)).append("</title>\n")
    sb.append("<style>\n").append(styles()).append("</style>\n</head>\n<body>\n")
    sb.append("<div class=\"page\">\n")

    sb.append("<header class=\"head\">\n")
    sb.append("<h1>").append(esc(data.title)).append("</h1>\n")
    sb.append("<p class=\"sub\">").append(esc(data.monthLabel)).append("</p>\n")
    sb.append("</header>\n")

    appendSummary(sb, data)
    appendDaily(sb, data)
    appendCategories(sb, data)
    appendFocus(sb, data)
    appendCompare(sb, data)
    appendInsights(sb, data)

    sb.append("<footer class=\"foot\">").append(esc(data.appName)).append("</footer>\n")
    sb.append("</div>\n</body>\n</html>\n")
    return sb.toString()
}

/** 三个大数字 */
private fun appendSummary(sb: StringBuilder, data: MonthlyReportData) {
    sb.append("<section class=\"card\">\n")
    sb.append("<h2>").append(esc(data.summarySectionTitle)).append("</h2>\n")
    sb.append("<div class=\"figures\">\n")
    figure(sb, data.expenseLabel, "¥${formatAmount(data.expenseCents)}", EXPENSE)
    figure(sb, data.incomeLabel, "¥${formatAmount(data.incomeCents)}", INCOME)
    val balanceColor = if (data.balanceCents < 0L) EXPENSE else TEXT_MAIN
    figure(sb, data.balanceLabel, "¥${formatAmount(data.balanceCents)}", balanceColor)
    sb.append("</div>\n")
    row(sb, data.summaryNote, "", TEXT_DIM)
    sb.append("</section>\n")
}

/** 每日支出：纯 div 宽度的横向柱条，不需要任何绘图库 */
private fun appendDaily(sb: StringBuilder, data: MonthlyReportData) {
    sb.append("<section class=\"card\">\n")
    sb.append("<h2>").append(esc(data.dailySectionTitle)).append("</h2>\n")
    val bars = data.dailyBars.filter { it.ratio > 0f }
    if (bars.isEmpty()) {
        sb.append("<p class=\"hint\">").append(esc(data.emptyText)).append("</p>\n")
    } else {
        sb.append("<div class=\"bars\">\n")
        bars.forEach { bar ->
            val percent = (bar.ratio * 100f).toInt().coerceIn(1, 100)
            // 柱条靠左、说明靠右：窄屏也不会把文字挤没
            sb.append("<div class=\"barrow\">")
            sb.append("<span class=\"bar\"><i style=\"width:").append(percent).append("%\"></i></span>")
            sb.append("<span class=\"barlabel\">").append(esc(bar.label)).append("</span>")
            sb.append("</div>\n")
        }
        sb.append("</div>\n")
    }
    sb.append("</section>\n")
}

/** 分类表格 */
private fun appendCategories(sb: StringBuilder, data: MonthlyReportData) {
    sb.append("<section class=\"card\">\n")
    sb.append("<h2>").append(esc(data.categorySectionTitle)).append("</h2>\n")
    if (data.categories.isEmpty()) {
        sb.append("<p class=\"hint\">").append(esc(data.emptyText)).append("</p>\n")
    } else {
        sb.append("<table>\n<thead><tr>")
        sb.append("<th>").append(esc(data.categoryTopLabel)).append("</th>")
        sb.append("<th class=\"num\">¥</th>")
        sb.append("<th class=\"num\">%</th>")
        sb.append("</tr></thead>\n<tbody>\n")
        data.categories.forEach { item ->
            val percent = (item.ratio * 100f).roundToIntSafe()
            sb.append("<tr>")
            sb.append("<td>").append(esc(item.name)).append("</td>")
            sb.append("<td class=\"num\">").append(esc(item.amountText)).append("</td>")
            sb.append("<td class=\"num dim\">").append(percent).append("%</td>")
            sb.append("</tr>\n")
        }
        sb.append("</tbody>\n</table>\n")
    }
    sb.append("</section>\n")
}

private fun appendFocus(sb: StringBuilder, data: MonthlyReportData) {
    if (data.focusLine.isBlank()) return
    sb.append("<section class=\"card\">\n")
    sb.append("<h2>").append(esc(data.focusSectionTitle)).append("</h2>\n")
    row(sb, data.focusLine, "", BRAND)
    sb.append("</section>\n")
}

private fun appendCompare(sb: StringBuilder, data: MonthlyReportData) {
    if (data.comparisons.isEmpty()) return
    sb.append("<section class=\"card\">\n")
    sb.append("<h2>").append(esc(data.compareSectionTitle)).append("</h2>\n")
    data.comparisons.forEach { item ->
        row(sb, item.label, "${item.amountText}  ${item.deltaText}", TEXT_MAIN)
    }
    sb.append("</section>\n")
}

private fun appendInsights(sb: StringBuilder, data: MonthlyReportData) {
    val lines = data.insights.filter { it.isNotBlank() }
    if (lines.isEmpty()) return
    sb.append("<section class=\"card\">\n")
    sb.append("<h2>").append(esc(data.insightSectionTitle)).append("</h2>\n")
    sb.append("<ul class=\"insights\">\n")
    lines.forEach { line ->
        sb.append("<li>").append(esc(line)).append("</li>\n")
    }
    sb.append("</ul>\n</section>\n")
}

private fun figure(sb: StringBuilder, label: String, value: String, color: String) {
    sb.append("<div class=\"figure\"><span class=\"flabel\">").append(esc(label)).append("</span>")
    sb.append("<span class=\"fvalue\" style=\"color:").append(color).append("\">")
    sb.append(esc(value)).append("</span></div>\n")
}

private fun row(sb: StringBuilder, label: String, value: String, valueColor: String) {
    sb.append("<div class=\"row\"><span>").append(esc(label)).append("</span>")
    if (value.isNotBlank()) {
        sb.append("<b style=\"color:").append(valueColor).append("\">").append(esc(value)).append("</b>")
    }
    sb.append("</div>\n")
}

private fun styles(): String = """
body{margin:0;background:#FFFFFF;color:$TEXT_MAIN;font-family:$FONT_STACK;font-size:14px;line-height:1.6}
.page{max-width:760px;margin:0 auto;padding:28px 22px 40px}
.head h1{margin:0;font-size:26px;line-height:1.3}
.head .sub{margin:6px 0 0;color:$TEXT_DIM;font-size:14px}
.card{margin-top:18px;padding:16px 18px;background:$CARD_BG;border:1px solid $LINE;border-radius:12px}
.card h2{margin:0 0 12px;font-size:16px;color:$BRAND}
.figure{display:inline-block;width:32%;vertical-align:top}
.flabel{display:block;color:$TEXT_DIM;font-size:12px}
.fvalue{display:block;font-size:20px;font-weight:600;margin-top:2px}
.row{display:flex;justify-content:space-between;gap:12px;margin-top:8px;font-size:13px}
.bars{margin-top:4px}
.barrow{display:flex;align-items:center;gap:10px;margin:5px 0}
.bar{flex:1;height:10px;background:#E4E9EC;border-radius:5px;overflow:hidden}
.bar i{display:block;height:10px;background:$BRAND;border-radius:5px}
.barlabel{width:170px;text-align:right;color:$TEXT_DIM;font-size:12px}
table{width:100%;border-collapse:collapse;font-size:13px}
th,td{padding:6px 4px;border-bottom:1px solid $LINE;text-align:left}
th{color:$TEXT_DIM;font-weight:500;font-size:12px}
.num{text-align:right;white-space:nowrap}
.dim{color:$TEXT_DIM}
.insights{margin:0;padding-left:18px}
.insights li{margin:6px 0}
.hint{margin:0;color:$TEXT_DIM;font-size:13px}
.foot{margin-top:22px;color:$TEXT_DIM;font-size:12px;text-align:center}
"""

/** 极简 HTML 转义：分类名、备注都是用户输入，必须转义后才能进 HTML */
private fun esc(raw: String): String {
    val sb = StringBuilder(raw.length + 16)
    raw.forEach { c ->
        when (c) {
            '&' -> sb.append("&amp;")
            '<' -> sb.append("&lt;")
            '>' -> sb.append("&gt;")
            '"' -> sb.append("&quot;")
            '\'' -> sb.append("&#39;")
            else -> sb.append(c)
        }
    }
    return sb.toString()
}

private fun langTag(lang: Lang): String = when (lang) {
    Lang.ZH_CN -> "zh-CN"
    Lang.ZH_TW -> "zh-TW"
    Lang.EN -> "en"
    Lang.JA -> "ja"
}

private fun Float.roundToIntSafe(): Int = (this * 100f).toInt()

// ==================== 文件写入（SAF 给的 Uri） ====================

/**
 * 写文本文件。任何失败（Uri 打不开、流写不进去）都直接抛出，
 * 让调用方用 runCatching 收成一句提示语——模块自己不吞异常也不弹 Toast。
 */
fun writeText(context: Context, uri: Uri, text: String) {
    val stream = context.contentResolver.openOutputStream(uri, "wt")
        ?: throw IllegalStateException("openOutputStream returned null")
    stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
}

// ==================== PDF ====================

/** A4 的点数（72dpi 下的 210mm × 297mm） */
private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f

/**
 * 画 A4 多页 PDF：标题、概览、每日柱状图、分类表、专注、环比、洞察。
 * 只放得下多少画多少，画满就新开一页；字体一律用系统默认，不内嵌任何字体文件。
 * 返回 false 表示没画成功（Uri 打不开等），调用方按失败提示。
 *
 * [lang] 只是为了让三个渲染入口签名一致：PDF 里所有文案都由调用方预先本地化好了，
 * 这里不再查任何字符串资源。
 */
fun writePdf(
    context: Context,
    uri: Uri,
    data: MonthlyReportData,
    lang: Lang
): Boolean {
    val document = PdfDocument()
    return try {
        val title = textPaint(20f, TEXT_MAIN, bold = true)
        val section = textPaint(13f, BRAND, bold = true)
        val body = textPaint(11f, TEXT_MAIN)
        val dim = textPaint(9.5f, TEXT_DIM)
        val figure = textPaint(16f, TEXT_MAIN, bold = true)
        // 柱条底 / 柱条身两种颜色各一个 Paint，别在循环里反复 new
        val barBackground = fillPaint(CARD_BG)
        val barFill = fillPaint(EXPENSE)
        val bars = mutableListOf<Pair<Float, String>>()
        var pageNumber = 0
        var page = document.startPage(info(pageNumber + 1))
        var canvas = page.canvas
        var y = 0f

        fun newPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(info(pageNumber + 1))
            canvas = page.canvas
            y = MARGIN
        }

        fun need(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN) newPage()
        }

        fun line(text: String, paint: Paint, gap: Float = 6f) {
            // 一行文字放不下就换行，长分类名 / 长洞察句都不会被裁掉
            val size = paint.textSize
            val maxChars =
                ((PAGE_WIDTH - MARGIN * 2) / (size * 0.95f)).toInt().coerceAtLeast(8)
            wrap(text, maxChars).forEach { piece ->
                need(size + gap + 6f)
                canvas.drawText(piece, MARGIN, y + size, paint)
                y += size + gap
            }
        }

        // ---- 第一页 ----
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 6f, fillPaint(BRAND))
        y = MARGIN + 6f
        line(data.title, title, 4f)
        line(data.monthLabel, dim, 12f)
        line(data.summarySectionTitle, section, 6f)

        val figureWidth = (PAGE_WIDTH - MARGIN * 2) / 3f
        need(56f)
        val figureTop = y
        listOf(
            Triple(data.expenseLabel, "¥${formatAmount(data.expenseCents)}", EXPENSE),
            Triple(data.incomeLabel, "¥${formatAmount(data.incomeCents)}", INCOME),
            Triple(
                data.balanceLabel,
                "¥${formatAmount(data.balanceCents)}",
                if (data.balanceCents < 0L) EXPENSE else TEXT_MAIN
            )
        ).forEachIndexed { index, item ->
            val left = MARGIN + figureWidth * index
            canvas.drawText(item.first, left, figureTop + 10f, dim)
            figure.color = Color.parseColor(item.third)
            canvas.drawText(item.second, left, figureTop + 34f, figure)
        }
        y = figureTop + 46f
        line(data.summaryNote, dim, 10f)

        // ---- 每日支出 ----
        bars.clear()
        data.dailyBars.filter { it.ratio > 0f }.forEach { bar ->
            bars += bar.ratio.coerceIn(0.02f, 1f) to bar.label
        }
        if (bars.isNotEmpty()) {
            line(data.dailySectionTitle, section, 6f)
            val barLeft = MARGIN
            val barFull = PAGE_WIDTH - MARGIN * 2 - 150f
            bars.forEach { (ratio, label) ->
                need(15f)
                canvas.drawRect(
                    RectF(barLeft, y + 3f, barLeft + barFull, y + 12f),
                    barBackground
                )
                canvas.drawRect(
                    RectF(barLeft, y + 3f, barLeft + (barFull * ratio).coerceAtLeast(2f), y + 12f),
                    barFill
                )
                canvas.drawText(label, barLeft + barFull + 8f, y + 12f, dim)
                y += 15f
            }
            y += 8f
        }

        // ---- 分类 ----
        if (data.categories.isNotEmpty()) {
            line(data.categorySectionTitle, section, 6f)
            data.categories.forEach { item ->
                need(16f)
                canvas.drawText(item.name, MARGIN, y + 11f, body)
                val amount = "${item.amountText}   ${(item.ratio * 100f).toInt()}%"
                canvas.drawText(
                    amount,
                    PAGE_WIDTH - MARGIN - body.measureText(amount),
                    y + 11f,
                    dim
                )
                y += 16f
            }
            y += 6f
        }

        // ---- 专注 / 环比 / 洞察 ----
        if (data.focusLine.isNotBlank()) {
            line(data.focusSectionTitle, section, 4f)
            line(data.focusLine, body, 10f)
        }
        if (data.comparisons.isNotEmpty()) {
            line(data.compareSectionTitle, section, 4f)
            data.comparisons.forEach { item ->
                val text = "${item.label}  ${item.amountText}  ${item.deltaText}"
                line(text, body, 5f)
            }
            y += 6f
        }
        val insights = data.insights.filter { it.isNotBlank() }
        if (insights.isNotEmpty()) {
            line(data.insightSectionTitle, section, 4f)
            insights.forEach { line("· $it", body, 5f) }
        }

        document.finishPage(page)
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return false
        stream.use { document.writeTo(it) }
        true
    } catch (_: Exception) {
        false
    } finally {
        // 无论成功失败都释放：PdfDocument 不 close 会泄漏原生页缓冲
        document.close()
    }
}

/** 只有尺寸和页码，没有别的可配项（PdfDocument.PageInfo 上不存在元数据 API） */
private fun info(pageNumber: Int) = PdfDocument.PageInfo.Builder(
    PAGE_WIDTH,
    PAGE_HEIGHT,
    pageNumber
).create()

/**
 * 画图用的三个小工具：月报的 PDF / PNG 和学习周报的 PNG 共用一份，
 * 所以是 internal（周报在 ui.study 包里，同模块可见）。
 */
internal fun textPaint(size: Float, colorHex: String, bold: Boolean = false) = Paint().apply {
    isAntiAlias = true
    textSize = size
    color = Color.parseColor(colorHex)
    typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
}

internal fun fillPaint(colorHex: String) = Paint().apply {
    isAntiAlias = true
    color = Color.parseColor(colorHex)
    style = Paint.Style.FILL
}

/** 按字符数粗略折行。只用于 PDF，宁可多折一行也不把字裁掉 */
private fun wrap(text: String, maxChars: Int): List<String> {
    if (text.length <= maxChars) return listOf(text)
    val out = ArrayList<String>(text.length / maxChars + 1)
    var index = 0
    while (index < text.length) {
        val end = (index + maxChars).coerceAtMost(text.length)
        out += text.substring(index, end)
        index = end
    }
    return out
}

// ==================== 分享用 PNG ====================

private const val PNG_PADDING = 56f
private const val PNG_MAX_ROWS = 8

/**
 * 画一张可以直接分享的概览图（1080px 宽）。
 * 高度不写死，按实际要画的行数算出来，所以洞察只有一条时不会留一大片空白。
 */
fun renderBitmap(data: MonthlyReportData, lang: Lang, widthPx: Int = 1080): Bitmap {
    val width = widthPx.coerceAtLeast(480)
    val scale = width / 1080f
    fun px(value: Float) = value * scale

    val pad = px(PNG_PADDING)
    val inner = width - pad * 2f
    val titleSize = px(46f)
    val sectionSize = px(30f)
    val bodySize = px(28f)
    val amountSize = px(34f)

    val categories = data.categories.take(PNG_MAX_ROWS)
    val insights = data.insights.filter { it.isNotBlank() }.take(PNG_MAX_ROWS)
    val figures = listOf(
        Triple(data.expenseLabel, "¥${formatAmount(data.expenseCents)}", EXPENSE),
        Triple(data.incomeLabel, "¥${formatAmount(data.incomeCents)}", INCOME),
        Triple(
            data.balanceLabel,
            "¥${formatAmount(data.balanceCents)}",
            if (data.balanceCents < 0L) EXPENSE else TEXT_MAIN
        )
    )

    // 先把要换行的行折好，高度才能算准（每个分类还多占一行放金额）
    val categoryLines = categories.map { wrap(it.name, 18) }
    val insightLines = insights.map { wrap(it, 30) }

    var contentHeight = titleSize * 1.6f + sectionSize * 1.8f + amountSize * 1.6f
    contentHeight += bodySize * 2.2f
    if (categories.isNotEmpty()) {
        contentHeight += sectionSize * 1.8f
        categoryLines.forEach { lines -> contentHeight += (lines.size + 1) * bodySize * 1.5f }
    }
    if (insights.isNotEmpty()) {
        contentHeight += sectionSize * 1.8f
        insightLines.forEach { lines -> contentHeight += lines.size * bodySize * 1.5f }
    }
    contentHeight += bodySize * 2.4f

    val height = (pad * 2f + contentHeight).toInt().coerceAtLeast(400)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)

    // 顶部品牌色横条 / 页脚分隔线：各一个 Paint，别重复 new
    val brandBar = fillPaint(BRAND)
    val divider = fillPaint(LINE)
    canvas.drawRect(0f, 0f, width.toFloat(), px(10f), brandBar)

    val title = textPaint(titleSize, TEXT_MAIN, bold = true)
    val section = textPaint(sectionSize, BRAND, bold = true)
    val body = textPaint(bodySize, TEXT_MAIN)
    val dim = textPaint(bodySize * 0.85f, TEXT_DIM)
    val amount = textPaint(amountSize, TEXT_MAIN, bold = true)

    var y = pad
    canvas.drawText(data.title, pad, y + titleSize, title)
    y += titleSize * 1.6f
    canvas.drawText(data.monthLabel, pad, y + bodySize, dim)
    y += bodySize * 2.2f

    figures.forEachIndexed { index, item ->
        val left = pad + inner / 3f * index
        canvas.drawText(item.first, left, y + bodySize * 0.7f, dim)
        amount.color = Color.parseColor(item.third)
        canvas.drawText(item.second, left, y + bodySize * 0.7f + amountSize * 1.4f, amount)
    }
    y += amountSize * 1.6f + bodySize * 0.6f

    if (categories.isNotEmpty()) {
        canvas.drawText(data.categorySectionTitle, pad, y + sectionSize, section)
        y += sectionSize * 1.8f
        categories.forEachIndexed { index, item ->
            val nameLines = categoryLines[index]
            nameLines.forEach { piece ->
                canvas.drawText(piece, pad, y + bodySize, body)
                y += bodySize * 1.5f
            }
            // 金额与占比单独一行，长分类名换行后也不会跟金额挤在一起
            val right = "${item.amountText}  ${(item.ratio * 100f).toInt()}%"
            canvas.drawText(right, pad, y + bodySize, dim)
            y += bodySize * 1.5f
        }
    }

    if (insights.isNotEmpty()) {
        canvas.drawText(data.insightSectionTitle, pad, y + sectionSize, section)
        y += sectionSize * 1.8f
        insightLines.forEach { lines ->
            lines.forEachIndexed { lineIndex, piece ->
                val text = if (lineIndex == 0) "· $piece" else "  $piece"
                canvas.drawText(text, pad, y + bodySize, body)
                y += bodySize * 1.5f
            }
        }
    }

    y += bodySize * 0.6f
    canvas.drawRect(pad, y, pad + inner, y + px(2f), divider)
    y += bodySize * 1.6f
    canvas.drawText(data.appName, pad, y, dim)

    return bitmap
}

/** 把 [renderBitmap] 的结果写成 PNG；失败时抛出，由调用方收成提示语 */
fun writePng(context: Context, uri: Uri, bitmap: Bitmap) {
    val stream = context.contentResolver.openOutputStream(uri, "wt")
        ?: throw IllegalStateException("openOutputStream returned null")
    stream.use { output ->
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            throw IllegalStateException("bitmap.compress failed")
        }
    }
}
