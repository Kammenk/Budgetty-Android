package com.budgetty.app.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.billing.BillingManager
import com.budgetty.app.data.forecast.CashFlowForecast
import com.budgetty.app.data.forecast.ForecastResult
import com.budgetty.app.data.forecast.toForecastEvents
import com.budgetty.app.data.local.RecurringEntity
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.repository.RecurringRepository
import com.budgetty.app.data.repository.TransactionRepository
import com.budgetty.app.data.settings.AppSettings
import com.budgetty.app.data.settings.SettingsStore
import com.budgetty.app.ui.util.PayCycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Allowed forecast horizons (months), cycled by the horizon chip. */
val FORECAST_HORIZONS = listOf(3, 6, 12)

data class ForecastUiState(
    val isLoaded: Boolean = false,
    val isPremium: Boolean = false,
    val hasIncome: Boolean = false,
    val hasBills: Boolean = false,
    /** Null until the user enters a starting balance in the assumptions sheet. */
    val startBalance: BigDecimal? = null,
    /** Average monthly spend derived from the last 3 months of receipts (the default discretionary). */
    val derivedDiscretionary: BigDecimal = BigDecimal.ZERO,
    /** The discretionary actually used — the user's override, or [derivedDiscretionary]. */
    val discretionary: BigDecimal = BigDecimal.ZERO,
    val hasDiscretionaryOverride: Boolean = false,
    val comfortThreshold: BigDecimal = BigDecimal.ZERO,
    val horizonMonths: Int = 3,
    /** The projection, or null while [needsData] / [needsBalance]. */
    val result: ForecastResult? = null,
) {
    /** The forecast can't be drawn without both recurring income and bills. */
    val needsData: Boolean get() = !hasIncome || !hasBills
    /** The user hasn't entered a starting balance yet. */
    val needsBalance: Boolean get() = startBalance == null
}

/**
 * Builds the cash-flow forecast from the user's recurring income/bills, an entered starting balance and
 * a discretionary assumption seeded from recent spending. Premium feature (the screen draws a locked
 * state for free users); the projection math lives in [CashFlowForecast].
 */
class ForecastViewModel(
    private val recurringRepository: RecurringRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsStore: SettingsStore,
    billingManager: BillingManager,
) : ViewModel() {

    val uiState: StateFlow<ForecastUiState> =
        combine(
            recurringRepository.items,
            transactionRepository.getAll(),
            settingsStore.settings,
            billingManager.isPremium,
        ) { recurring, transactions, settings, premium ->
            build(recurring, transactions, settings, premium)
        }.flowOn(Dispatchers.Default).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ForecastUiState(),
        )

    fun setStartBalance(text: String) = settingsStore.setForecastStartBalance(normalize(text))
    fun setComfortThreshold(text: String) = settingsStore.setForecastComfortThreshold(normalize(text))
    fun setDiscretionaryOverride(text: String) = settingsStore.setForecastDiscretionary(normalize(text))
    fun resetDiscretionary() = settingsStore.setForecastDiscretionary("")
    fun setHorizon(months: Int) = settingsStore.setForecastHorizonMonths(months)

    /** Cycles 3 → 6 → 12 → 3 for the horizon chip. */
    fun cycleHorizon() {
        val current = settingsStore.settings.value.forecastHorizonMonths
        val next = FORECAST_HORIZONS[(FORECAST_HORIZONS.indexOf(current).coerceAtLeast(0) + 1) % FORECAST_HORIZONS.size]
        settingsStore.setForecastHorizonMonths(next)
    }

    private fun build(
        recurring: List<RecurringEntity>,
        transactions: List<TransactionEntity>,
        settings: AppSettings,
        premium: Boolean,
    ): ForecastUiState {
        val today = LocalDate.now()
        val hasIncome = recurring.any { it.isIncome }
        val hasBills = recurring.any { !it.isIncome }
        val derived = averageMonthlySpend(transactions, today, settings.monthStartDay)
        val override = settings.forecastDiscretionary.toAmountOrNull()
        val discretionary = override ?: derived
        val startBalance = settings.forecastStartBalance.toAmountOrNull()
        val comfort = settings.forecastComfortThreshold.toAmountOrNull() ?: BigDecimal.ZERO
        val horizon = settings.forecastHorizonMonths

        val result = if (hasIncome && hasBills && startBalance != null) {
            val endDate = YearMonth.from(today).plusMonths((horizon - 1).toLong()).atEndOfMonth()
            CashFlowForecast.project(
                startBalance = startBalance,
                today = today,
                horizonMonths = horizon,
                events = recurring.toForecastEvents(today, endDate),
                monthlyDiscretionary = discretionary,
                comfortThreshold = comfort,
            )
        } else {
            null
        }

        return ForecastUiState(
            isLoaded = true,
            isPremium = premium,
            hasIncome = hasIncome,
            hasBills = hasBills,
            startBalance = startBalance,
            derivedDiscretionary = derived,
            discretionary = discretionary,
            hasDiscretionaryOverride = override != null,
            comfortThreshold = comfort,
            horizonMonths = horizon,
            result = result,
        )
    }

    /** Average spend per month over the last 3 completed pay-cycle months (0 when there's no history). */
    private fun averageMonthlySpend(
        transactions: List<TransactionEntity>,
        today: LocalDate,
        monthStartDay: Int,
    ): BigDecimal {
        val zone = ZoneId.systemDefault()
        val windowStart = PayCycle.month(today, monthStartDay, -3).first
        val windowEnd = PayCycle.month(today, monthStartDay, -1).second
        val startMs = windowStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = windowEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val total = transactions
            .filter { it.timestamp in startMs..endMs }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.price.multiply(BigDecimal(t.quantity))) }
        return total.divide(BigDecimal(3), 2, RoundingMode.HALF_UP)
    }

    private fun normalize(text: String): String = text.replace(',', '.').trim()

    private fun String.toAmountOrNull(): BigDecimal? =
        replace(',', '.').trim().takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()
}
