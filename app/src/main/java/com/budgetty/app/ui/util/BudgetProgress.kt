package com.budgetty.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetWarnColor
import java.math.BigDecimal
import java.math.RoundingMode

/** Fraction of [budget] used by [spent], clamped to 0..1. A zero/absent budget yields 0. */
fun budgetRatio(spent: BigDecimal, budget: BigDecimal?): Float {
    if (budget == null || budget.signum() <= 0) return 0f
    return (spent.toDouble() / budget.toDouble()).coerceIn(0.0, 1.0).toFloat()
}

/** Green under 50% of budget, yellow at 50–74%, red at 75%+ (theme-aware). */
@Composable
@ReadOnlyComposable
fun budgetColor(spent: BigDecimal, budget: BigDecimal): Color {
    val ratio = if (budget.signum() == 0) 0.0 else spent.toDouble() / budget.toDouble()
    return when {
        ratio >= 0.75 -> budgetBadColor()
        ratio >= 0.50 -> budgetWarnColor()
        else -> budgetGoodColor()
    }
}

/** Average weeks per month (52 ÷ 12 ≈ 4.33) for converting a budget between weekly and monthly. */
private val WEEKS_PER_MONTH: BigDecimal = BigDecimal("52").divide(BigDecimal("12"), 10, RoundingMode.HALF_UP)

/** The weekly-equivalent of a [monthly] budget (monthly ÷ 4.33), to 2 decimals. */
fun monthlyToWeekly(monthly: BigDecimal): BigDecimal =
    monthly.divide(WEEKS_PER_MONTH, 2, RoundingMode.HALF_UP)

/** The monthly-equivalent of a [weekly] budget (weekly × 4.33), to 2 decimals. */
fun weeklyToMonthly(weekly: BigDecimal): BigDecimal =
    weekly.multiply(WEEKS_PER_MONTH).setScale(2, RoundingMode.HALF_UP)
