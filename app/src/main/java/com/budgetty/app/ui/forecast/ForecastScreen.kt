package com.budgetty.app.ui.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.data.forecast.MonthProjection
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.formatMoney
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthLabel = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val dayLabel = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

@Composable
fun ForecastScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToBudget: () -> Unit,
    viewModel: ForecastViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ForecastContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onNavigateToPaywall = onNavigateToPaywall,
        onNavigateToBudget = onNavigateToBudget,
        onCycleHorizon = viewModel::cycleHorizon,
        onSetStartBalance = viewModel::setStartBalance,
        onSetComfort = viewModel::setComfortThreshold,
        onSetDiscretionary = viewModel::setDiscretionaryOverride,
        onResetDiscretionary = viewModel::resetDiscretionary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ForecastContent(
    state: ForecastUiState,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onCycleHorizon: () -> Unit,
    onSetStartBalance: (String) -> Unit,
    onSetComfort: (String) -> Unit,
    onSetDiscretionary: (String) -> Unit,
    onResetDiscretionary: () -> Unit,
) {
    var assumptionsOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.forecast_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
        ) {
            when {
                !state.isLoaded -> Unit
                state.needsData -> SetupState(state.hasIncome, state.hasBills, onNavigateToBudget)
                !state.isPremium -> LockedTeaser(onNavigateToPaywall)
                state.needsBalance -> NeedsBalancePrompt { assumptionsOpen = true }
                else -> ReadyContent(state, onCycleHorizon) { assumptionsOpen = true }
            }
        }
    }

    if (assumptionsOpen) {
        AssumptionsSheet(
            state = state,
            onSetStartBalance = onSetStartBalance,
            onSetComfort = onSetComfort,
            onSetDiscretionary = onSetDiscretionary,
            onResetDiscretionary = onResetDiscretionary,
            onDismiss = { assumptionsOpen = false },
        )
    }
}

@Composable
private fun SetupState(hasIncome: Boolean, hasBills: Boolean, onNavigateToBudget: () -> Unit) {
    SectionCard {
        Text(
            stringResource(R.string.forecast_setup_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.forecast_setup_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChecklistRow(stringResource(R.string.forecast_setup_income), hasIncome)
        ChecklistRow(stringResource(R.string.forecast_setup_bills), hasBills)
        Button(
            onClick = onNavigateToBudget,
            modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
        ) { Text(stringResource(R.string.forecast_go_to_budget)) }
    }
}

@Composable
private fun ChecklistRow(label: String, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
    ) {
        Text(
            text = if (done) "✓" else "•",
            color = if (done) budgetGoodColor() else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LockedTeaser(onUnlock: () -> Unit) {
    SectionCard {
        Text("✦", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(R.string.forecast_locked_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.forecast_locked_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
        ) { Text(stringResource(R.string.forecast_unlock)) }
    }
}

@Composable
private fun NeedsBalancePrompt(onEdit: () -> Unit) {
    SectionCard {
        Text(
            stringResource(R.string.forecast_balance_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.forecast_balance_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
        ) { Text(stringResource(R.string.forecast_add_balance)) }
    }
}

@Composable
private fun ReadyContent(state: ForecastUiState, onCycleHorizon: () -> Unit, onEditAssumptions: () -> Unit) {
    val result = state.result ?: return
    HorizonChip(state.horizonMonths, onCycleHorizon)
    SectionCard {
        Text(
            stringResource(R.string.forecast_projected_end, monthLabel.format(YearMonth.from(result.endDate))),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = result.endBalance.formatMoney(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        TroughCallout(result.trough, result.troughDate.format(dayLabel), result.dipsBelowComfort)
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        ForecastChart(
            result = result,
            comfortThreshold = state.comfortThreshold,
            modifier = Modifier.fillMaxWidth().height(150.dp),
        )
    }
    SectionCard {
        Text(
            text = stringResource(R.string.forecast_month_by_month),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        result.months.forEach { MonthRow(it) }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        modifier = Modifier.clickable(onClick = onEditAssumptions),
    ) {
        Row(modifier = Modifier.padding(MaterialTheme.dimens.lg), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.forecast_assumptions),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    stringResource(
                        R.string.forecast_assumptions_sub,
                        (state.startBalance ?: BigDecimal.ZERO).formatMoney(),
                        state.discretionary.formatMoney(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.action_edit),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    Text(
        stringResource(R.string.forecast_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xs),
    )
}

@Composable
private fun HorizonChip(horizonMonths: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.sm),
    ) {
        Text(
            stringResource(R.string.forecast_horizon, horizonMonths),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun TroughCallout(amount: BigDecimal, date: String, dips: Boolean) {
    val color = if (dips) budgetWarnColor() else budgetGoodColor()
    val label = if (dips) R.string.forecast_dips_to else R.string.forecast_lowest_point
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.dimens.sm)
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.sm),
    ) {
        Text(
            stringResource(label, amount.formatMoney(), date),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MonthRow(month: MonthProjection) {
    Column(modifier = Modifier.padding(vertical = MaterialTheme.dimens.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = monthLabel.format(month.month),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                month.endBalance.formatMoney(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (month.dipsBelowComfort) budgetWarnColor() else budgetGoodColor(),
            )
        }
        Text(
            stringResource(
                R.string.forecast_month_flows,
                month.income.formatMoney(),
                month.bills.formatMoney(),
                month.discretionary.formatMoney(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (month.dipsBelowComfort) {
            Text(
                stringResource(
                    R.string.forecast_month_dips,
                    month.trough.formatMoney(),
                    month.troughDate.format(dayLabel),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = budgetWarnColor(),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssumptionsSheet(
    state: ForecastUiState,
    onSetStartBalance: (String) -> Unit,
    onSetComfort: (String) -> Unit,
    onSetDiscretionary: (String) -> Unit,
    onResetDiscretionary: () -> Unit,
    onDismiss: () -> Unit,
) {
    AdaptiveSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
        ) {
            Text(
                text = stringResource(R.string.forecast_assumptions),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            AmountField(
                label = stringResource(R.string.forecast_start_balance),
                initial = state.startBalance?.toPlainString().orEmpty(),
                onCommit = onSetStartBalance,
            )
            Text(
                stringResource(R.string.forecast_no_bank),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AmountField(
                label = stringResource(R.string.forecast_discretionary),
                initial = if (state.hasDiscretionaryOverride) state.discretionary.toPlainString() else "",
                placeholder = state.derivedDiscretionary.formatMoney(),
                onCommit = onSetDiscretionary,
            )
            TextButton(onClick = onResetDiscretionary) {
                Text(stringResource(R.string.forecast_discretionary_reset, state.derivedDiscretionary.formatMoney()))
            }
            AmountField(
                label = stringResource(R.string.forecast_comfort),
                initial = state.comfortThreshold.toPlainString(),
                onCommit = onSetComfort,
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(MaterialTheme.dimens.buttonHeight),
            ) { Text(stringResource(R.string.action_done)) }
        }
    }
}

@Composable
private fun AmountField(label: String, initial: String, onCommit: (String) -> Unit, placeholder: String = "") {
    var text by remember(initial) { mutableStateOf(initial) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onCommit(it)
        },
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.xl),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
            content = { content() },
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun ForecastPreview() {
    val today = java.time.LocalDate.of(2026, 10, 6)
    val result = com.budgetty.app.data.forecast.CashFlowForecast.project(
        startBalance = BigDecimal("1200"),
        today = today,
        horizonMonths = 3,
        events = listOf(
            com.budgetty.app.data.forecast.CashEvent(java.time.LocalDate.of(2026, 10, 25), BigDecimal("2600")),
            com.budgetty.app.data.forecast.CashEvent(java.time.LocalDate.of(2026, 11, 25), BigDecimal("2600")),
            com.budgetty.app.data.forecast.CashEvent(java.time.LocalDate.of(2026, 11, 15), BigDecimal("-640")),
            com.budgetty.app.data.forecast.CashEvent(java.time.LocalDate.of(2026, 11, 1), BigDecimal("-850")),
            com.budgetty.app.data.forecast.CashEvent(java.time.LocalDate.of(2026, 12, 1), BigDecimal("-850")),
        ),
        monthlyDiscretionary = BigDecimal("780"),
        comfortThreshold = BigDecimal("300"),
    )
    BudgettyTheme {
        ForecastContent(
            state = ForecastUiState(
                isLoaded = true, isPremium = true, hasIncome = true, hasBills = true,
                startBalance = BigDecimal("1200"), derivedDiscretionary = BigDecimal("780"),
                discretionary = BigDecimal("780"), comfortThreshold = BigDecimal("300"),
                horizonMonths = 3, result = result,
            ),
            onNavigateBack = {}, onNavigateToPaywall = {}, onNavigateToBudget = {}, onCycleHorizon = {},
            onSetStartBalance = {}, onSetComfort = {}, onSetDiscretionary = {}, onResetDiscretionary = {},
        )
    }
}
