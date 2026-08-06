package com.budgetty.app.data.export

import com.budgetty.app.data.local.ReceiptEntity
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class ExportBuilderTest {

    private fun ms(y: Int, m: Int, d: Int) =
        LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun tx(receiptId: Long, price: String, category: String, y: Int, mo: Int, d: Int) =
        TransactionEntity(
            name = "x", timestamp = ms(y, mo, d), price = BigDecimal(price),
            quantity = 1, category = category, receiptId = receiptId,
        )

    private fun receipt(id: Long, store: String) =
        ReceiptEntity(timestamp = id, store = store, date = id, discount = BigDecimal.ZERO)

    @Test fun `buildCore groups per receipt, totals and category breakdown`() {
        val source = ExportSource(
            transactions = listOf(
                tx(1000, "20.00", "Groceries", 2026, 7, 2),
                tx(1000, "28.20", "Groceries", 2026, 7, 2),
                tx(2000, "62.00", "Fuel", 2026, 7, 4),
            ),
            receiptsById = mapOf(
                1000L to receipt(1000, "Kaufland"),
                2000L to receipt(2000, "Shell"),
            ),
            incomeRecurring = listOf(
                RecurringEntity(label = "Salary", amount = BigDecimal("2400.00"), isIncome = true),
            ),
            currencySymbol = "€",
            monthStartDay = 1,
            loaded = true,
        )
        val start = ms(2026, 7, 1)
        val end = ms(2026, 8, 1) - 1
        val core = ExportBuilder.buildCore(source, start, end)

        assertEquals(2, core.rows.size) // one row per receipt
        assertEquals(BigDecimal("110.20"), core.totalSpent)
        assertEquals(BigDecimal("2400.00"), core.income) // full-month recurring income
        assertEquals(BigDecimal("2289.80"), core.net)
        // sorted by cost: Fuel (62) before Groceries (48.20)
        assertEquals(listOf("Fuel", "Groceries"), core.byCategory.map { it.name })
        assertEquals(56, core.byCategory.first().pct) // 62 / 110.20
        assertEquals("Kaufland", core.rows.first().store)
    }

    @Test fun `empty period yields no rows`() {
        val source = ExportSource(loaded = true, monthStartDay = 1)
        val core = ExportBuilder.buildCore(source, ms(2026, 1, 1), ms(2026, 2, 1) - 1)
        assertTrue(core.isEmpty)
    }

    @Test fun `csv escapes commas and quotes and has a header`() {
        val data = ExportData(
            periodLabel = "P", generatedLabel = "G", currencySymbol = "€",
            rows = listOf(
                ExportRow(0, "02 Jul", "Bäcker, Sonne", "Dining", 0, BigDecimal("7.40")),
                ExportRow(0, "03 Jul", "He said \"hi\"", "Other", 0, BigDecimal("5.00")),
            ),
            totalSpent = BigDecimal("12.40"), income = BigDecimal.ZERO, net = BigDecimal("-12.40"),
            byCategory = emptyList(), totalRowLabel = "Total",
        )
        val csv = ExportBuilder.toCsv(data)
        val lines = csv.trim().lines()
        assertEquals("Date,Store,Category,Amount,Currency", lines[0])
        assertEquals("02 Jul,\"Bäcker, Sonne\",Dining,7.40,€", lines[1])
        assertEquals("03 Jul,\"He said \"\"hi\"\"\",Other,5.00,€", lines[2])
    }

    @Test fun `period label collapses a single month`() {
        assertEquals("1 – 31 July 2026", ExportBuilder.periodLabel(ms(2026, 7, 1), ms(2026, 8, 1) - 1))
    }
}
