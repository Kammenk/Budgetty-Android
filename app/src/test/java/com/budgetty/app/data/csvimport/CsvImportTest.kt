package com.budgetty.app.data.csvimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class CsvImportTest {

    private val utc = ZoneId.of("UTC")
    private fun millis(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atStartOfDay(utc).toInstant().toEpochMilli()

    @Test
    fun `parse splits rows and columns`() {
        val table = CsvImport.parse("Date,Store,Amount\n2026-10-12,Lidl,-42.80\n2026-10-11,Spotify,-10.99\n")
        assertEquals(listOf("Date", "Store", "Amount"), table.headers)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("2026-10-11", "Spotify", "-10.99"), table.rows[1])
    }

    @Test
    fun `parse honours quoted fields, embedded commas and doubled quotes`() {
        val table = CsvImport.parse("Store,Note\n\"Lidl, Berlin\",\"Said \"\"danke\"\"\"\n")
        assertEquals(listOf("Lidl, Berlin", "Said \"danke\""), table.rows[0])
    }

    @Test
    fun `parse handles CRLF line endings and ragged rows`() {
        // Second row is short (padded to 3), third row is long (truncated to 3).
        val table = CsvImport.parse("A,B,C\r\n1,2\r\n4,5,6,7\r\n")
        assertEquals(listOf("1", "2", ""), table.rows[0])
        assertEquals(listOf("4", "5", "6"), table.rows[1])
    }

    @Test
    fun `parse drops blank lines`() {
        val table = CsvImport.parse("A,B\n\n1,2\n\n")
        assertEquals(1, table.rows.size)
    }

    @Test
    fun `guessMapping reads typical bank headers and never maps the running balance as amount`() {
        val headers = listOf("Booking date", "Payee", "Purpose", "Amount", "Category", "Balance")
        assertEquals(
            listOf(
                CsvField.DATE, CsvField.STORE, CsvField.IGNORE,
                CsvField.AMOUNT, CsvField.CATEGORY, CsvField.IGNORE,
            ),
            CsvImport.guessMapping(headers),
        )
    }

    @Test
    fun `mappingError flags a missing required field and a duplicated one`() {
        assertEquals(
            CsvImport.MappingError.Missing(listOf(CsvField.AMOUNT)),
            CsvImport.mappingError(listOf(CsvField.DATE, CsvField.STORE)),
        )
        assertEquals(
            CsvImport.MappingError.Duplicated(CsvField.DATE),
            CsvImport.mappingError(listOf(CsvField.DATE, CsvField.DATE, CsvField.AMOUNT)),
        )
        assertNull(CsvImport.mappingError(listOf(CsvField.DATE, CsvField.AMOUNT, CsvField.IGNORE)))
    }

    @Test
    fun `parseAmount handles signs, currency symbols, thousands and both decimal separators`() {
        assertEquals(0, BigDecimal("-42.80").compareTo(CsvImport.parseAmount("-42.80")))
        assertEquals(0, BigDecimal("2450.00").compareTo(CsvImport.parseAmount("2,450.00")))
        assertEquals(0, BigDecimal("42.80").compareTo(CsvImport.parseAmount("€42.80")))
        assertEquals(0, BigDecimal("-12.50").compareTo(CsvImport.parseAmount("-12,50"))) // comma decimal
        assertEquals(0, BigDecimal("1234.56").compareTo(CsvImport.parseAmount("1.234,56"))) // European
        assertEquals(0, BigDecimal("-8.40").compareTo(CsvImport.parseAmount("(8.40)"))) // parenthesised
        assertNull(CsvImport.parseAmount(""))
        assertNull(CsvImport.parseAmount("n/a"))
    }

    @Test
    fun `parseDate reads each supported format and rejects impossible dates`() {
        assertEquals(millis(2026, 10, 12), CsvImport.parseDate("12/10/2026", CsvImport.PATTERN_DMY, utc))
        assertEquals(millis(2026, 10, 12), CsvImport.parseDate("10/12/2026", CsvImport.PATTERN_MDY, utc))
        assertEquals(millis(2026, 10, 12), CsvImport.parseDate("2026-10-12", CsvImport.PATTERN_ISO, utc))
        assertNull(CsvImport.parseDate("31/02/2026", CsvImport.PATTERN_DMY, utc)) // no 31 Feb
        assertNull(CsvImport.parseDate("", CsvImport.PATTERN_DMY, utc))
    }

    private val mapping = listOf(
        CsvField.DATE, CsvField.STORE, CsvField.IGNORE, CsvField.AMOUNT, CsvField.CATEGORY, CsvField.IGNORE,
    )

    @Test
    fun `parseRow produces an expense with a title-cased name`() {
        val row = listOf("12/10/2026", "LIDL SAGT DANKE", "Card payment", "-42.80", "Groceries", "1,204.11")
        val parsed = CsvImport.parseRow(row, mapping, CsvImport.PATTERN_DMY, SignConvention.MINUS_IS_EXPENSE, utc)
        assertEquals(RowKind.EXPENSE, parsed.kind)
        assertEquals("Lidl Sagt Danke", parsed.name)
        assertEquals(millis(2026, 10, 12), parsed.dateMillis)
        assertEquals(0, BigDecimal("42.80").compareTo(parsed.amount))
        assertEquals("Groceries", parsed.category)
    }

    @Test
    fun `parseRow marks a positive amount as income under the minus-is-expense convention`() {
        val row = listOf("10/10/2026", "ACME GMBH", "Salary", "2450.00", "Income", "")
        val parsed = CsvImport.parseRow(row, mapping, CsvImport.PATTERN_DMY, SignConvention.MINUS_IS_EXPENSE, utc)
        assertEquals(RowKind.INCOME, parsed.kind)
    }

    @Test
    fun `parseRow flags a row with an unreadable date as invalid`() {
        val row = listOf("31/02/2026", "Bolt", "Ride", "-8.40", "Transport", "")
        val parsed = CsvImport.parseRow(row, mapping, CsvImport.PATTERN_DMY, SignConvention.MINUS_IS_EXPENSE, utc)
        assertEquals(RowKind.INVALID, parsed.kind)
    }

    @Test
    fun `sign convention can be inverted`() {
        val row = listOf("10/10/2026", "ACME", "x", "2450.00", "Income", "")
        val parsed = CsvImport.parseRow(row, mapping, CsvImport.PATTERN_DMY, SignConvention.MINUS_IS_INCOME, utc)
        assertEquals(RowKind.EXPENSE, parsed.kind)
    }

    @Test
    fun `dedupKey matches same day, amount and store regardless of case`() {
        val a = CsvImport.dedupKey(millis(2026, 10, 12), BigDecimal("42.80"), "Lidl", utc)
        val b = CsvImport.dedupKey(millis(2026, 10, 12), BigDecimal("42.8"), "LIDL", utc)
        assertEquals(a, b)
        assertTrue(a.contains("2026-10-12"))
    }
}
