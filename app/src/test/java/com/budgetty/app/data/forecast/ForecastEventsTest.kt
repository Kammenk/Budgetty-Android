package com.budgetty.app.data.forecast

import com.budgetty.app.data.local.RecurringEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

class ForecastEventsTest {

    private val today = LocalDate.of(2026, 10, 1)
    private val end = LocalDate.of(2026, 11, 30)

    private fun recurring(amount: String, isIncome: Boolean, cadence: String, dueDay: Int) =
        RecurringEntity(
            label = "x", amount = BigDecimal(amount), isIncome = isIncome,
            cadence = cadence, dueDay = dueDay,
        )

    @Test
    fun `monthly income and bills land on their due day each month, yearly and once skipped`() {
        val events = listOf(
            recurring("2000", isIncome = true, RecurringEntity.Cadence.MONTHLY, dueDay = 25),
            recurring("850", isIncome = false, RecurringEntity.Cadence.MONTHLY, dueDay = 1),
            recurring("300", isIncome = false, RecurringEntity.Cadence.YEARLY, dueDay = 10),
            recurring("50", isIncome = false, RecurringEntity.Cadence.ONCE, dueDay = 1),
        ).toForecastEvents(today, end)

        assertEquals(4, events.size) // 2 salary + 2 rent; yearly + once skipped
        assertTrue(events.contains(CashEvent(LocalDate.of(2026, 10, 25), BigDecimal("2000"))))
        assertTrue(events.contains(CashEvent(LocalDate.of(2026, 11, 25), BigDecimal("2000"))))
        assertTrue(events.contains(CashEvent(LocalDate.of(2026, 10, 1), BigDecimal("-850"))))
        assertTrue(events.contains(CashEvent(LocalDate.of(2026, 11, 1), BigDecimal("-850"))))
    }

    @Test
    fun `a 31st due day clamps to a short month`() {
        val events = listOf(recurring("20", isIncome = false, RecurringEntity.Cadence.MONTHLY, dueDay = 31))
            .toForecastEvents(today, end)
        // November has 30 days, so the 31st clamps to the 30th.
        assertTrue(events.contains(CashEvent(LocalDate.of(2026, 11, 30), BigDecimal("-20"))))
    }

    @Test
    fun `weekly bills land on every matching weekday`() {
        val events = listOf(recurring("10", isIncome = false, RecurringEntity.Cadence.WEEKLY, dueDay = 1))
            .toForecastEvents(today, end) // dueDay 1 = Monday
        assertTrue(events.isNotEmpty())
        assertTrue(events.all { it.date.dayOfWeek == DayOfWeek.MONDAY && it.amount == BigDecimal("-10") })
    }
}
