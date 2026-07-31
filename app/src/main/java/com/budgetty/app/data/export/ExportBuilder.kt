package com.budgetty.app.data.export

import com.budgetty.app.category.Categories
import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.paidAdjustmentOf
import com.budgetty.app.store.StoreNormalizer
import com.budgetty.app.ui.util.windowAmount
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** All-time export inputs (independent of the chosen period). */
data class ExportSource(
    val transactions: List<TransactionEntity> = emptyList(),
    val receiptsById: Map<Long, ReceiptEntity> = emptyMap(),
    val incomeRecurring: List<RecurringEntity> = emptyList(),
    val currencySymbol: String = "",
    val monthStartDay: Int = 1,
    val loaded: Boolean = false,
)

/** The period-scoped figures behind an export, before display labels are attached. */
data class ExportCore(
    val rows: List<ExportRow>,
    val totalSpent: BigDecimal,
    val income: BigDecimal,
    val net: BigDecimal,
    val byCategory: List<ExportCategory>,
    val startMillis: Long,
    val endMillis: Long,
) {
    val receiptCount: Int get() = rows.size
    val isEmpty: Boolean get() = rows.isEmpty()
}

/** Pure aggregation + CSV for exports (no Android graphics — unit-testable on the host). */
object ExportBuilder {

    private val rowDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())

    /** Builds the period's rows (one per receipt), totals, income and category breakdown. */
    fun buildCore(source: ExportSource, startMillis: Long, endMillis: Long): ExportCore {
        val zone = ZoneId.systemDefault()
        val txns = source.transactions.filter { it.timestamp in startMillis..endMillis }
        val rows = txns.groupBy { it.receiptId }.mapNotNull { (rid, group) ->
            val net = group.fold(BigDecimal.ZERO) { acc, t -> acc + t.price.multiply(BigDecimal(t.quantity)) }
            val total = (net + paidAdjustmentOf(group, source.receiptsById)).setScale(2, RoundingMode.HALF_UP)
            if (total.signum() <= 0) return@mapNotNull null
            val category = group.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key
                ?: group.first().category
            val date = group.firstOrNull()?.timestamp ?: rid
            ExportRow(
                dateMillis = date,
                dateLabel = Instant.ofEpochMilli(date).atZone(zone).toLocalDate().format(rowDate),
                store = StoreNormalizer.normalize(source.receiptsById[rid]?.store.orEmpty()).ifBlank { "—" },
                category = category,
                colorArgb = Categories.colorOf(category),
                amount = total,
            )
        }.sortedBy { it.dateMillis }

        val totalSpent = rows.fold(BigDecimal.ZERO) { acc, r -> acc + r.amount }
        val income = source.incomeRecurring
            .fold(BigDecimal.ZERO) { acc, r -> acc + r.windowAmount(startMillis, endMillis, zone) }
            .setScale(2, RoundingMode.HALF_UP)
        val byCategory = rows.groupBy { it.category }
            .map { (name, catRows) ->
                val sum = catRows.fold(BigDecimal.ZERO) { a, r -> a + r.amount }
                val pct = if (totalSpent.signum() > 0) (sum.toDouble() / totalSpent.toDouble() * 100).roundToInt() else 0
                ExportCategory(name, Categories.emojiOf(name), Categories.colorOf(name), sum, pct)
            }
            .sortedByDescending { it.total }

        return ExportCore(rows, totalSpent, income, income - totalSpent, byCategory, startMillis, endMillis)
    }

    fun toCsv(data: ExportData): String = buildString {
        append("Date,Store,Category,Amount,Currency\n")
        data.rows.forEach { r ->
            append(r.dateLabel.csv()).append(',')
            append(r.store.csv()).append(',')
            append(r.category.csv()).append(',')
            append(r.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()).append(',')
            append(data.currencySymbol.csv()).append('\n')
        }
    }

    /** A human period label from a millis range, e.g. "1 – 31 July 2026" / "1 May – 30 Jun 2026". */
    fun periodLabel(startMillis: Long, endMillis: Long): String {
        val zone = ZoneId.systemDefault()
        val s = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val e = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        val loc = Locale.getDefault()
        fun month(d: LocalDate) = d.month.getDisplayName(TextStyle.FULL, loc)
        fun monthShort(d: LocalDate) = d.month.getDisplayName(TextStyle.SHORT, loc)
        return when {
            s.year == e.year && s.month == e.month -> "${s.dayOfMonth} – ${e.dayOfMonth} ${month(e)} ${e.year}"
            s.year == e.year -> "${s.dayOfMonth} ${monthShort(s)} – ${e.dayOfMonth} ${monthShort(e)} ${e.year}"
            else -> "${s.dayOfMonth} ${monthShort(s)} ${s.year} – ${e.dayOfMonth} ${monthShort(e)} ${e.year}"
        }
    }

    private fun String.csv(): String =
        if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + replace("\"", "\"\"") + "\"" else this
}
