package com.budgetty.app.ui.planners

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.budgetty.app.R
import com.budgetty.app.ui.components.SegmentedToggle
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.formatMoney
import java.math.BigDecimal

private val TERM_YEARS = listOf(3, 5, 7)

/**
 * The Loan calculator: scratch inputs (amount, APR, term) feeding a closed-form amortisation
 * ([LoanCalculator]). Nothing is persisted — it's a free what-if tool. Monthly payment is the hero,
 * followed by total interest/paid, a principal-vs-interest split and a by-year breakdown + table.
 */
// One scrolling calc screen: inputs wired to a pure amortisation plus the result cards it renders.
// Its length is layout, not branching, so the long-method rule is suppressed.
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun LoanCalculatorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var amountText by rememberSaveable { mutableStateOf("15000") }
    var aprText by rememberSaveable { mutableStateOf("6.9") }
    var termIndex by rememberSaveable { mutableStateOf(1) }

    val amount = amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val apr = aprText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val years = TERM_YEARS[termIndex]
    val result = remember(amount, apr, years) { LoanCalculator.compute(amount, apr, years) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loan_calc_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(plannerContentPadding),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
            ) {
                // Inputs
                PlannerCard {
                    PlannerNumberField(
                        label = stringResource(R.string.loan_calc_amount),
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                        labelColor = MaterialTheme.colorScheme.primary,
                        trailing = com.budgetty.app.ui.util.AppFormats.currencySymbol,
                    )
                    Spacer(Modifier.height(MaterialTheme.dimens.sm))
                    PlannerNumberField(
                        label = stringResource(R.string.loan_calc_apr),
                        value = aprText,
                        onValueChange = { aprText = it.filter { c -> c.isDigit() || c == '.' } },
                        trailing = "%",
                    )
                    Spacer(Modifier.height(MaterialTheme.dimens.md))
                    Text(
                        text = stringResource(R.string.loan_calc_term),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = MaterialTheme.dimens.xs),
                    )
                    SegmentedToggle(
                        options = TERM_YEARS.map { stringResource(R.string.loan_calc_years, it) },
                        selectedIndex = termIndex,
                        onSelect = { termIndex = it },
                    )
                }

                // Monthly payment hero
                PlannerCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.loan_calc_monthly_payment),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(Modifier.height(MaterialTheme.dimens.xs))
                        Text(
                            text = result.monthlyPayment.formatMoney(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.loan_calc_for_months, result.months),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                // Stat tiles
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
                    LoanStatTile(
                        label = stringResource(R.string.loan_calc_total_interest),
                        value = result.totalInterest.formatMoney(),
                        valueColor = budgetBadColor(),
                        modifier = Modifier.weight(1f),
                    )
                    LoanStatTile(
                        label = stringResource(R.string.loan_calc_total_paid),
                        value = result.totalPaid.formatMoney(),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Where the money goes
                PlannerCard {
                    Text(
                        text = stringResource(R.string.loan_calc_where),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(MaterialTheme.dimens.md))
                    SplitBar(principalPercent = result.principalPercent)
                    Spacer(Modifier.height(MaterialTheme.dimens.sm))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        LegendDot(
                            color = MaterialTheme.colorScheme.primary,
                            label = stringResource(R.string.loan_calc_principal_pct, result.principalPercent),
                        )
                        LegendDot(
                            color = budgetBadColor(),
                            label = stringResource(R.string.loan_calc_interest_pct, result.interestPercent),
                        )
                    }
                    if (result.years.size > 1) {
                        Spacer(Modifier.height(MaterialTheme.dimens.lg))
                        Text(
                            text = stringResource(R.string.loan_calc_by_year),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(MaterialTheme.dimens.sm))
                        YearlyBars(result.years)
                    }
                }

                // Amortisation table
                PlannerCard {
                    Text(
                        text = stringResource(R.string.loan_calc_amortisation),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(MaterialTheme.dimens.sm))
                    AmortisationHeader()
                    result.years.forEach { AmortisationRow(it) }
                    Spacer(Modifier.height(MaterialTheme.dimens.sm))
                    Text(
                        text = stringResource(R.string.loan_calc_estimate_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanStatTile(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    PlannerCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

/** A thin two-segment bar: principal (primary) then interest (red), proportional to [principalPercent]. */
@Composable
private fun SplitBar(principalPercent: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val p = principalPercent.coerceIn(0, 100)
        if (p > 0) {
            Box(Modifier.weight(p.toFloat()).fillMaxSize().background(MaterialTheme.colorScheme.primary))
        }
        if (p < 100) {
            Box(Modifier.weight((100 - p).toFloat()).fillMaxSize().background(budgetBadColor()))
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.height(8.dp).width(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Per-year stacked bars (interest on top of principal), each scaled to the biggest year's payments. */
@Composable
private fun YearlyBars(years: List<LoanYear>) {
    val maxH = 70.dp
    val maxYear = years.maxOf { it.principalPaid + it.interestPaid }.toDouble().coerceAtLeast(1.0)
    Row(
        modifier = Modifier.fillMaxWidth().height(maxH),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        years.forEach { y ->
            val principalH = maxH * (y.principalPaid.toDouble() / maxYear).toFloat().coerceIn(0f, 1f)
            val interestH = maxH * (y.interestPaid.toDouble() / maxYear).toFloat().coerceIn(0f, 1f)
            Column(modifier = Modifier.weight(1f)) {
                if (interestH > 0.dp) {
                    Box(
                        Modifier.fillMaxWidth().height(interestH)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(budgetBadColor()),
                    )
                }
                Box(Modifier.fillMaxWidth().height(principalH).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
private fun AmortisationHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.xs)) {
        TableCell(stringResource(R.string.loan_calc_col_year), weight = 0.6f, header = true)
        TableCell(stringResource(R.string.loan_calc_col_principal), weight = 1f, header = true, end = true)
        TableCell(stringResource(R.string.loan_calc_col_interest), weight = 1f, header = true, end = true)
        TableCell(stringResource(R.string.loan_calc_col_balance), weight = 1f, header = true, end = true)
    }
}

@Composable
private fun AmortisationRow(year: LoanYear) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.dimens.xs)) {
        TableCell(year.year.toString(), weight = 0.6f, bold = true)
        TableCell(year.principalPaid.formatMoney(), weight = 1f, end = true)
        TableCell(year.interestPaid.formatMoney(), weight = 1f, end = true, color = budgetBadColor())
        TableCell(year.endBalance.formatMoney(), weight = 1f, end = true, bold = true)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TableCell(
    text: String,
    weight: Float,
    header: Boolean = false,
    end: Boolean = false,
    bold: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = text,
        style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        fontWeight = if (header || bold) FontWeight.Bold else FontWeight.Normal,
        color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else color,
        textAlign = if (end) TextAlign.End else TextAlign.Start,
        modifier = Modifier.weight(weight),
    )
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun LoanCalculatorScreenPreview() {
    BudgettyTheme {
        LoanCalculatorScreen(onNavigateBack = {})
    }
}
