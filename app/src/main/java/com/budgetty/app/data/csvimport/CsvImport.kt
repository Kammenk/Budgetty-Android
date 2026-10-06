package com.budgetty.app.data.csvimport

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/**
 * Pure CSV-import parsing: turn raw file text into a table, guess which column feeds which field, and
 * parse one row into an importable transaction. No Android/Compose/Room deps, so the whole mapping and
 * number/date handling is unit-testable on the host. The UI/view-model layer resolves categories,
 * de-duplicates against existing data and writes the rows.
 */

/** The field a CSV column can feed; the Map step cycles through these in order. */
enum class CsvField { DATE, AMOUNT, STORE, CATEGORY, IGNORE }

/** How the sign of the amount column is read. Budgetty stores only expenses, so the "income" side is
 *  recognised (to report + skip it) rather than imported. */
enum class SignConvention { MINUS_IS_EXPENSE, MINUS_IS_INCOME }

/** A row's outcome after parsing: an importable [EXPENSE], a recognised but un-importable [INCOME]
 *  (Budgetty tracks income as recurring, not as transactions), or an unparseable [INVALID] row. */
enum class RowKind { EXPENSE, INCOME, INVALID }

/** The parsed header + data rows of a CSV (data rows padded/truncated to the header width). */
data class CsvTable(val headers: List<String>, val rows: List<List<String>>) {
    val isEmpty: Boolean get() = headers.isEmpty()
}

/** One CSV row resolved against a column mapping. [amount] is the absolute expense amount (always
 *  positive) for an [RowKind.EXPENSE]; [category] is the raw CSV category (the caller canonicalises it). */
data class ParsedImportRow(
    val dateMillis: Long?,
    val name: String,
    val amount: BigDecimal?,
    val category: String,
    val kind: RowKind,
)

object CsvImport {

    // Date patterns per the Review step's format choice. "d"/"M" accept 1- or 2-digit values; "uuuu"
    // (proleptic year, not "yyyy" year-of-era) is required for STRICT resolution, which rejects
    // impossible dates like 31 February instead of silently coercing them.
    const val PATTERN_DMY = "d/M/uuuu"
    const val PATTERN_MDY = "M/d/uuuu"
    const val PATTERN_ISO = "uuuu-M-d"

    /**
     * Parses [text] into a [CsvTable] — RFC-4180-ish: fields may be quoted, a quoted field may contain
     * commas and newlines, and an embedded quote is written doubled (`""`). Matches the quoting
     * `ExportBuilder.toCsv` produces, so Budgetty's own exports round-trip. The first non-blank line is
     * the header; blank lines are dropped.
     */
    @Suppress("CyclomaticComplexMethod") // A single-pass CSV state machine; splitting it would obscure it.
    fun parse(text: String): CsvTable {
        val rows = mutableListOf<MutableList<String>>()
        var field = StringBuilder()
        var row = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        fun endField() { row.add(field.toString()); field = StringBuilder() }
        fun endRow() { endField(); rows.add(row); row = mutableListOf() }
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> endField()
                c == '\n' -> endRow()
                c == '\r' -> { endRow(); if (i + 1 < text.length && text[i + 1] == '\n') i++ }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()

        val cleaned = rows.filter { r -> r.any { it.isNotBlank() } }
        if (cleaned.isEmpty()) return CsvTable(emptyList(), emptyList())
        val headers = cleaned.first().map { it.trim() }
        val dataRows = cleaned.drop(1).map { r -> List(headers.size) { idx -> r.getOrElse(idx) { "" } } }
        return CsvTable(headers, dataRows)
    }

    /** Guesses a [CsvField] for each header from its name; Date/Amount/Store/Category are each assigned
     *  to the first matching header only (so nothing is mapped twice by default), the rest Ignore. */
    fun guessMapping(headers: List<String>): List<CsvField> {
        val taken = mutableSetOf<CsvField>()
        return headers.map { header ->
            val h = header.lowercase(Locale.ROOT)
            val guess = when {
                DATE_KEYS.any { h.contains(it) } && CsvField.DATE !in taken -> CsvField.DATE
                // "balance" is a running total, never the transaction amount — exclude it.
                AMOUNT_KEYS.any { h.contains(it) } && !h.contains("balance") && CsvField.AMOUNT !in taken ->
                    CsvField.AMOUNT
                CATEGORY_KEYS.any { h.contains(it) } && CsvField.CATEGORY !in taken -> CsvField.CATEGORY
                STORE_KEYS.any { h.contains(it) } && CsvField.STORE !in taken -> CsvField.STORE
                else -> CsvField.IGNORE
            }
            if (guess != CsvField.IGNORE) taken.add(guess)
            guess
        }
    }

    /** Whether a [mapping] can be imported: Date and Amount are mapped exactly once each. */
    fun mappingError(mapping: List<CsvField>): MappingError? {
        val missing = listOf(CsvField.DATE, CsvField.AMOUNT).filter { f -> mapping.none { it == f } }
        if (missing.isNotEmpty()) return MappingError.Missing(missing)
        val duped = listOf(CsvField.DATE, CsvField.AMOUNT, CsvField.STORE, CsvField.CATEGORY)
            .firstOrNull { f -> mapping.count { it == f } > 1 }
        return duped?.let { MappingError.Duplicated(it) }
    }

    sealed interface MappingError {
        data class Missing(val fields: List<CsvField>) : MappingError
        data class Duplicated(val field: CsvField) : MappingError
    }

    /** Parses one raw [row] against [mapping], the chosen [datePattern] and [sign] convention. */
    fun parseRow(
        row: List<String>,
        mapping: List<CsvField>,
        datePattern: String,
        sign: SignConvention,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ParsedImportRow {
        fun value(field: CsvField): String =
            mapping.indexOf(field).takeIf { it >= 0 }?.let { row.getOrElse(it) { "" }.trim() } ?: ""

        val dateMillis = parseDate(value(CsvField.DATE), datePattern, zone)
        val signed = parseAmount(value(CsvField.AMOUNT))
        val name = titleCase(value(CsvField.STORE)).ifBlank { DEFAULT_NAME }
        val category = value(CsvField.CATEGORY)
        val kind = when {
            dateMillis == null || signed == null || signed.signum() == 0 -> RowKind.INVALID
            isExpense(signed, sign) -> RowKind.EXPENSE
            else -> RowKind.INCOME
        }
        return ParsedImportRow(dateMillis, name, signed?.abs(), category, kind)
    }

    /** Parses [raw] as a date using [pattern] (English month names); null on any failure. */
    fun parseDate(raw: String, pattern: String, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (raw.isBlank()) return null
        return runCatching {
            val fmt = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT)
            LocalDate.parse(raw.trim(), fmt).atStartOfDay(zone).toInstant().toEpochMilli()
        }.getOrNull()
    }

    /**
     * Parses a monetary [raw] string to a signed [BigDecimal], tolerating currency symbols, spaces,
     * thousands separators and parenthesised negatives. Decimal separator: when both '.' and ',' are
     * present the last one wins (the other is thousands); a lone ',' is treated as the decimal point
     * (European). Returns null when there's no number.
     */
    fun parseAmount(raw: String): BigDecimal? {
        var t = raw.trim().replace(" ", "").replace(" ", "")
        if (t.isEmpty()) return null
        val negative = t.startsWith("-") || (t.startsWith("(") && t.endsWith(")"))
        t = t.filter { it.isDigit() || it == '.' || it == ',' }
        if (t.isEmpty()) return null
        val hasDot = t.contains('.')
        val hasComma = t.contains(',')
        t = when {
            hasDot && hasComma ->
                if (t.lastIndexOf('.') > t.lastIndexOf(',')) t.replace(",", "")
                else t.replace(".", "").replace(",", ".")
            hasComma -> t.replace(",", ".")
            else -> t
        }
        val n = t.toBigDecimalOrNull() ?: return null
        return if (negative) n.negate() else n
    }

    /** A stable key for duplicate detection: same day, same absolute amount, same store (normalised). */
    fun dedupKey(dateMillis: Long, amount: BigDecimal, name: String, zone: ZoneId = ZoneId.systemDefault()): String {
        val day = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
        return "$day|${amount.setScale(2, RoundingMode.HALF_UP).toPlainString()}|${name.lowercase(Locale.ROOT).trim()}"
    }

    private fun isExpense(signed: BigDecimal, sign: SignConvention): Boolean = when (sign) {
        SignConvention.MINUS_IS_EXPENSE -> signed.signum() < 0
        SignConvention.MINUS_IS_INCOME -> signed.signum() > 0
    }

    /** Title-cases an UPPER/lower store name ("LIDL SAGT DANKE" -> "Lidl Sagt Danke"). */
    private fun titleCase(raw: String): String =
        raw.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
        }

    private const val DEFAULT_NAME = "Imported"
    private val DATE_KEYS = listOf("date", "booking", "buchung", "datum", "time")
    private val AMOUNT_KEYS = listOf("amount", "betrag", "sum", "value", "price", "montant", "importe", "debit")
    private val CATEGORY_KEYS = listOf("categ", "kategorie")
    private val STORE_KEYS =
        listOf("payee", "merchant", "store", "description", "name", "recipient", "beneficiary", "narrative")
}
