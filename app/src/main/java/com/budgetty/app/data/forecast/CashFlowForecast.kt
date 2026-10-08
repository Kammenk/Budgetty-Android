package com.budgetty.app.data.forecast

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/** A dated cash movement in the forecast: positive for income, negative for a bill. */
data class CashEvent(val date: LocalDate, val amount: BigDecimal)

/** One month's projected flows (magnitudes) and the balance it ends on. */
data class MonthProjection(
    val month: YearMonth,
    val income: BigDecimal,
    val bills: BigDecimal,
    val discretionary: BigDecimal,
    val net: BigDecimal,
    val endBalance: BigDecimal,
    val trough: BigDecimal,
    val troughDate: LocalDate,
    val dipsBelowComfort: Boolean,
)

/** A point on the projected balance curve. */
data class ForecastPoint(val date: LocalDate, val balance: BigDecimal)

/** The whole projection: the daily balance curve, the overall trough, end balance and per-month rows. */
data class ForecastResult(
    val points: List<ForecastPoint>,
    val startBalance: BigDecimal,
    val endBalance: BigDecimal,
    val endDate: LocalDate,
    val trough: BigDecimal,
    val troughDate: LocalDate,
    val dipsBelowComfort: Boolean,
    val months: List<MonthProjection>,
)

/**
 * Pure day-by-day cash-flow projection — no Android/Room deps, so it is unit-testable on the host.
 * Everything is modelled forward from a user-entered balance; it is an estimate, never a bank balance
 * (Budgetty doesn't connect to banks). The view model expands the user's recurring income/bills into
 * dated [CashEvent]s and seeds [monthlyDiscretionary] from spending history.
 */
object CashFlowForecast {

    private val DAYS_PER_YEAR = BigDecimal(365)
    private val MONTHS_PER_YEAR = BigDecimal(12)

    /**
     * Projects the daily balance from [startBalance] on [today] over [horizonMonths] whole months (the
     * current month's remaining days plus the following months), applying each dated [events] movement
     * on its day and spreading [monthlyDiscretionary] evenly across every day (× 12 ÷ 365).
     * [comfortThreshold] is the "warn me below" line used to flag the trough.
     */
    fun project(
        startBalance: BigDecimal,
        today: LocalDate,
        horizonMonths: Int,
        events: List<CashEvent>,
        monthlyDiscretionary: BigDecimal,
        comfortThreshold: BigDecimal,
    ): ForecastResult {
        val endDate = YearMonth.from(today)
            .plusMonths((horizonMonths.coerceAtLeast(1) - 1).toLong())
            .atEndOfMonth()
        val dailyDisc = monthlyDiscretionary.multiply(MONTHS_PER_YEAR)
            .divide(DAYS_PER_YEAR, 2, RoundingMode.HALF_UP)

        val incomeByDay = HashMap<LocalDate, BigDecimal>()
        val billByDay = HashMap<LocalDate, BigDecimal>()
        for (e in events) {
            if (e.date.isBefore(today) || e.date.isAfter(endDate)) continue
            if (e.amount.signum() >= 0) incomeByDay.merge(e.date, e.amount, BigDecimal::add)
            else billByDay.merge(e.date, e.amount.negate(), BigDecimal::add)
        }

        val daily = ArrayList<DayRow>()
        var balance = startBalance
        daily.add(DayRow(today, balance, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
        var day = today.plusDays(1)
        while (!day.isAfter(endDate)) {
            val inc = incomeByDay[day] ?: BigDecimal.ZERO
            val bill = billByDay[day] ?: BigDecimal.ZERO
            balance = balance.add(inc).subtract(bill).subtract(dailyDisc)
            daily.add(DayRow(day, balance, inc, bill, dailyDisc))
            day = day.plusDays(1)
        }

        val troughRow = daily.minByOrNull { it.balance } ?: daily.first()
        val months = daily.groupBy { YearMonth.from(it.date) }.map { (month, rows) ->
            val lowest = rows.minByOrNull { it.balance } ?: rows.first()
            val income = rows.sumOf2 { it.income }
            val bills = rows.sumOf2 { it.bill }
            val disc = rows.sumOf2 { it.disc }
            MonthProjection(
                month = month,
                income = income,
                bills = bills,
                discretionary = disc,
                net = income.subtract(bills).subtract(disc).setScale(2, RoundingMode.HALF_UP),
                endBalance = rows.last().balance,
                trough = lowest.balance,
                troughDate = lowest.date,
                dipsBelowComfort = lowest.balance < comfortThreshold,
            )
        }

        return ForecastResult(
            points = daily.map { ForecastPoint(it.date, it.balance) },
            startBalance = startBalance,
            endBalance = daily.last().balance,
            endDate = daily.last().date,
            trough = troughRow.balance,
            troughDate = troughRow.date,
            dipsBelowComfort = troughRow.balance < comfortThreshold,
            months = months,
        )
    }

    private data class DayRow(
        val date: LocalDate,
        val balance: BigDecimal,
        val income: BigDecimal,
        val bill: BigDecimal,
        val disc: BigDecimal,
    )

    private inline fun <T> List<T>.sumOf2(selector: (T) -> BigDecimal): BigDecimal =
        fold(BigDecimal.ZERO) { acc, item -> acc.add(selector(item)) }.setScale(2, RoundingMode.HALF_UP)
}
