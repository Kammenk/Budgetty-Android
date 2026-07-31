package com.budgetty.app.ui.savings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.data.local.SavingsContributionEntity
import com.budgetty.app.data.local.SavingsGoalEntity
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.SavingsProgress
import com.budgetty.app.ui.util.formatDayMonth
import com.budgetty.app.ui.util.formatMoney
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

private enum class GoalSheet { ADD, WITHDRAW, EDIT }

/** VM-backed detail screen: owns the sheets + delete dialog, delegates the view to [SavingsGoalDetailContent]. */
@Composable
fun SavingsGoalDetailScreen(
    goalId: Long,
    onNavigateBack: () -> Unit,
    viewModel: SavingsGoalViewModel = koinViewModel { parametersOf(goalId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.loaded, state.missing) {
        if (state.loaded && state.missing) onNavigateBack()
    }
    val goal = state.goal
    val progress = state.progress
    var sheet by remember { mutableStateOf<GoalSheet?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (goal == null || progress == null) {
        Scaffold { padding -> Box(Modifier.fillMaxSize().padding(padding)) }
        return
    }

    SavingsGoalDetailContent(
        goal = goal,
        contributions = state.contributions,
        progress = progress,
        onBack = onNavigateBack,
        onAdd = { sheet = GoalSheet.ADD },
        onWithdraw = { sheet = GoalSheet.WITHDRAW },
        onEditGoal = { sheet = GoalSheet.EDIT },
        onDeleteGoal = { confirmDelete = true },
    )

    when (sheet) {
        GoalSheet.ADD, GoalSheet.WITHDRAW -> SavingsContributionSheet(
            goalName = goal.name,
            goalEmoji = goal.emoji,
            saved = progress.saved,
            target = goal.targetAmount,
            isWithdraw = sheet == GoalSheet.WITHDRAW,
            onConfirm = { amount, note, date ->
                viewModel.addContribution(amount, note, date)
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        GoalSheet.EDIT -> SavingsGoalEditSheet(
            initial = goal,
            onSave = { name, emoji, target, targetDate ->
                viewModel.updateGoal(name, emoji, target, targetDate)
                sheet = null
            },
            onDelete = { sheet = null; confirmDelete = true },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.savings_delete_goal)) },
            text = { Text(stringResource(R.string.savings_delete_confirm, goal.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteGoal(onNavigateBack)
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Stateless goal-detail view: hero ring + stats + pace, Add/Withdraw, contribution history, edit/delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalDetailContent(
    goal: SavingsGoalEntity,
    contributions: List<SavingsContributionEntity>,
    progress: SavingsProgress,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onWithdraw: () -> Unit,
    onEditGoal: () -> Unit,
    onDeleteGoal: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiChip(goal.emoji, size = 26.dp)
                        Spacer(Modifier.width(MaterialTheme.dimens.sm))
                        Text(goal.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                // The outer MainScaffold already applies the status-bar inset to this route's content,
                // so zero the bar's own inset — otherwise it double-insets into a large top gap.
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.screenPadding),
        ) {
            HeroCard(goal, progress)
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                PrimaryPill(
                    text = stringResource(if (progress.reached) R.string.savings_withdraw_to_spend else R.string.savings_add),
                    icon = if (progress.reached) null else Icons.Filled.Add,
                    onClick = if (progress.reached) onWithdraw else onAdd,
                    modifier = Modifier.weight(1f),
                    fillWidth = true,
                )
                FilledTonalButton(
                    onClick = if (progress.reached) onEditGoal else onWithdraw,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        stringResource(if (progress.reached) R.string.savings_edit_target else R.string.savings_withdraw),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.lg))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.savings_contributions), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                Text(
                    text = pluralStringResource(R.plurals.savings_entries, contributions.size, contributions.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            if (contributions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column {
                        contributions.forEachIndexed { i, c ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ContributionRow(c)
                        }
                    }
                }
                Spacer(Modifier.height(MaterialTheme.dimens.md))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column {
                    ActionRow(Icons.Filled.Edit, stringResource(R.string.savings_edit_goal_title), MaterialTheme.colorScheme.onSurface, onEditGoal)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ActionRow(Icons.Filled.Delete, stringResource(R.string.savings_delete_goal), MaterialTheme.colorScheme.error, onDeleteGoal)
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.xxl))
        }
    }
}

@Composable
private fun HeroCard(goal: SavingsGoalEntity, progress: SavingsProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val color = savingsProgressColor(progress.behind)
            if (progress.reached) {
                Text(
                    text = stringResource(R.string.savings_reached),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = savingsProgressColor(behind = false),
                )
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
            }
            SavingsRing(fraction = progress.fraction, color = color, strokeWidth = 8.dp, modifier = Modifier.size(128.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (progress.reached) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = savingsProgressColor(behind = false), modifier = Modifier.size(34.dp))
                    } else {
                        Text("${(progress.fraction * 100).roundToInt()}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = stringResource(R.string.savings_of_amount, goal.targetAmount.formatMoney()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Row(modifier = Modifier.fillMaxWidth()) {
                HeroStat(stringResource(R.string.savings_saved_label), progress.saved.formatMoney(), savingsProgressColor(behind = false), Modifier.weight(1f))
                StatDivider()
                HeroStat(stringResource(R.string.savings_target_label), goal.targetAmount.formatMoney(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                StatDivider()
                HeroStat(stringResource(R.string.savings_left_label), progress.remaining.formatMoney(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            }
            detailPaceText(goal.targetDate, progress)?.let { pace ->
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                Text(pace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
    }
}

@Composable
private fun StatDivider() {
    Box(Modifier.width(1.dp).height(34.dp).padding(vertical = 2.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun ContributionRow(c: SavingsContributionEntity) {
    val deposit = c.amount.signum() >= 0
    val color = if (deposit) savingsProgressColor(behind = false) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.md), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (deposit) "+" else "−", fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Column(Modifier.weight(1f)) {
            Text(
                text = c.note.ifBlank { stringResource(if (deposit) R.string.savings_contribution else R.string.savings_withdrawal) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = Instant.ofEpochMilli(c.date).atZone(ZoneId.systemDefault()).toLocalDate().formatDayMonth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = (if (deposit) "+" else "−") + c.amount.abs().formatMoney(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(MaterialTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

/** Detail pace line: reached-on note, or "€X/month to reach by {date}", or null (no date / no rate). */
@Composable
private fun detailPaceText(targetDate: Long?, progress: SavingsProgress): String? {
    if (targetDate == null) return null
    val date = Instant.ofEpochMilli(targetDate).atZone(ZoneId.systemDefault()).toLocalDate().formatDayMonth()
    return when {
        progress.reached -> stringResource(R.string.savings_reached_by, date)
        progress.monthlyRate != null -> stringResource(R.string.savings_rate_by, progress.monthlyRate.formatMoney(), date)
        else -> null
    }
}
