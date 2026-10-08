package com.budgetty.app.ui.planners

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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.budgetty.app.R
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.dimens

/**
 * The Planners hub (Account → Planners): a small landing with the two free financial tools — the Debt
 * payoff planner and the Loan calculator — each opening its full screen. Same chrome as the other
 * pushed Account screens (TopAppBar + back, bottom nav hidden).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannersScreen(
    onNavigateBack: () -> Unit,
    onOpenDebtPayoff: () -> Unit,
    onOpenLoanCalculator: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.planners_title), fontWeight = FontWeight.Bold) },
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
            Column(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(MaterialTheme.dimens.lg)) {
                Text(
                    text = stringResource(R.string.planners_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.dimens.xs,
                        vertical = MaterialTheme.dimens.sm,
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
                    PlannerTile(
                        emoji = "🏔️",
                        title = stringResource(R.string.planners_debt_title),
                        subtitle = stringResource(R.string.planners_debt_subtitle),
                        onClick = onOpenDebtPayoff,
                        modifier = Modifier.weight(1f),
                    )
                    PlannerTile(
                        emoji = "🏦",
                        title = stringResource(R.string.planners_loan_title),
                        subtitle = stringResource(R.string.planners_loan_subtitle),
                        onClick = onOpenLoanCalculator,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannerTile(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlannerCard(modifier = modifier.clickable(onClick = onClick)) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlannersScreenPreview() {
    BudgettyTheme {
        PlannersScreen(onNavigateBack = {}, onOpenDebtPayoff = {}, onOpenLoanCalculator = {})
    }
}
