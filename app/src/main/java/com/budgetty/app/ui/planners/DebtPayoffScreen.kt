package com.budgetty.app.ui.planners

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.data.local.DebtEntity
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.components.SegmentedToggle
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.formatMoney
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val EXTRA_STEP = 25
private const val EXTRA_MAX = 500
private val DEBT_EMOJIS = listOf("💳", "🏠", "🚗", "🎓", "🛍️", "💰", "🏥", "📱")

/** A blank debt for the add sheet (id 0 = new). */
private fun newDebt() =
    DebtEntity(balance = BigDecimal.ZERO, aprPercent = BigDecimal.ZERO, minPayment = BigDecimal.ZERO)

@Composable
fun DebtPayoffScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebtsViewModel = koinViewModel(),
) {
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    DebtPayoffContent(
        debts = debts,
        onNavigateBack = onNavigateBack,
        onSaveDebt = viewModel::save,
        onDeleteDebt = viewModel::delete,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DebtPayoffContent(
    debts: List<DebtEntity>,
    onNavigateBack: () -> Unit,
    onSaveDebt: (DebtEntity) -> Unit,
    onDeleteDebt: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var strategy by rememberSaveable { mutableStateOf(PayoffStrategy.AVALANCHE) }
    var extra by rememberSaveable { mutableStateOf(150) }
    // null = closed; a wrapped value whose .id==0 is a new debt, else the one under edit.
    var editorFor by remember { mutableStateOf<DebtEntity?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debt_title), fontWeight = FontWeight.Bold) },
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
                if (debts.isEmpty()) {
                    DebtEmptyState(onAdd = { editorFor = newDebt() })
                } else {
                    val plan = remember(debts, extra, strategy) { computePlan(debts, extra, strategy) }
                    StrategyToggle(strategy = strategy, onChange = { strategy = it })
                    ResultCard(plan)
                    CompareRow(plan, strategy)
                    PayoffChartCard(plan)
                    ExtraCard(extra = extra, totalMin = plan.totalMin, onChange = { extra = it })
                    DebtListSection(
                        debts = debts,
                        onAdd = { editorFor = newDebt() },
                        onEdit = { editorFor = it },
                    )
                    PayoffOrderSection(plan = plan, strategy = strategy)
                    Text(
                        text = stringResource(R.string.debt_estimate_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xs),
                    )
                }
            }
        }
    }

    editorFor?.let { target ->
        DebtEditorSheet(
            initial = target,
            isEditing = target.id != 0L,
            onSave = { onSaveDebt(it); editorFor = null },
            onDelete = if (target.id != 0L) { { onDeleteDebt(target.id); editorFor = null } } else null,
            onDismiss = { editorFor = null },
        )
    }
}

// ── Sections ────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun StrategyToggle(strategy: PayoffStrategy, onChange: (PayoffStrategy) -> Unit) {
    Column {
        SegmentedToggle(
            options = listOf(stringResource(R.string.debt_snowball), stringResource(R.string.debt_avalanche)),
            selectedIndex = if (strategy == PayoffStrategy.SNOWBALL) 0 else 1,
            onSelect = { onChange(if (it == 0) PayoffStrategy.SNOWBALL else PayoffStrategy.AVALANCHE) },
        )
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        Text(
            text = stringResource(
                if (strategy == PayoffStrategy.SNOWBALL) R.string.debt_snowball_hint else R.string.debt_avalanche_hint,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xs),
        )
    }
}

@Composable
private fun ResultCard(plan: DebtPlan) {
    PlannerCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
        if (plan.result.clearedAll) {
            Text(
                text = stringResource(R.string.debt_free_by),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = plan.debtFreeDate,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.debt_months_sooner, plan.result.months, plan.monthsSooner),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(
                text = stringResource(R.string.debt_saves_interest, plan.interestSaved.formatMoney()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = budgetGoodColor(),
            )
        } else {
            Text(
                text = stringResource(R.string.debt_not_clearing),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.xs))
            Text(
                text = stringResource(R.string.debt_not_clearing_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun CompareRow(plan: DebtPlan, strategy: PayoffStrategy) {
    val cheapest = if (plan.avalanche.totalInterest <= plan.snowball.totalInterest) {
        PayoffStrategy.AVALANCHE
    } else {
        PayoffStrategy.SNOWBALL
    }
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
        CompareCard(
            name = stringResource(R.string.debt_snowball),
            tag = stringResource(
                if (cheapest == PayoffStrategy.SNOWBALL) R.string.debt_cheapest else R.string.debt_quick_wins,
            ),
            tagIsCheapest = cheapest == PayoffStrategy.SNOWBALL,
            date = plan.snowballDate,
            interest = plan.snowball.totalInterest,
            selected = strategy == PayoffStrategy.SNOWBALL,
            modifier = Modifier.weight(1f),
        )
        CompareCard(
            name = stringResource(R.string.debt_avalanche),
            tag = stringResource(
                if (cheapest == PayoffStrategy.AVALANCHE) R.string.debt_cheapest else R.string.debt_quick_wins,
            ),
            tagIsCheapest = cheapest == PayoffStrategy.AVALANCHE,
            date = plan.avalancheDate,
            interest = plan.avalanche.totalInterest,
            selected = strategy == PayoffStrategy.AVALANCHE,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CompareCard(
    name: String,
    tag: String,
    tagIsCheapest: Boolean,
    date: String,
    interest: BigDecimal,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .padding(MaterialTheme.dimens.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(MaterialTheme.dimens.xs))
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (tagIsCheapest) budgetGoodColor() else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(MaterialTheme.dimens.xs))
        Text(date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = stringResource(R.string.debt_interest_suffix, interest.formatMoney()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PayoffChartCard(plan: DebtPlan) {
    val primary = MaterialTheme.colorScheme.primary
    val baselineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surfaceContainer
    PlannerCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.debt_balance_to_zero),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.debt_owed, plan.totalOwed.formatMoney()),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = budgetBadColor(),
            )
        }
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val plan0 = plan.result.balanceSeries
            val base0 = plan.baseline.balanceSeries
            val maxMonths = (plan.baseline.months).coerceAtLeast(1).toFloat()
            val maxBal = plan0.firstOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
            fun pt(i: Int, v: Float) = Offset(i / maxMonths * size.width, size.height - (v / maxBal * size.height))

            // Minimums-only baseline (dashed).
            val basePath = Path().apply {
                base0.forEachIndexed { i, v ->
                    val o = pt(i, v.toFloat())
                    if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                }
            }
            drawPath(
                basePath,
                color = baselineColor.copy(alpha = 0.55f),
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))),
            )

            // Your plan (filled area + line).
            val linePath = Path().apply {
                plan0.forEachIndexed { i, v ->
                    val o = pt(i, v.toFloat())
                    if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                }
            }
            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(pt(plan0.lastIndex, 0f).x, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(areaPath, color = primary.copy(alpha = 0.12f))
            drawPath(linePath, color = primary, style = Stroke(width = 5f))

            // A ring where each debt clears.
            plan.result.payoffMonthById.values.forEach { m ->
                if (m < plan0.size) {
                    val o = pt(m, plan0[m].toFloat())
                    drawCircle(surface, radius = 7f, center = o)
                    drawCircle(primary, radius = 7f, center = o, style = Stroke(width = 3f))
                }
            }
        }
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
            ChartLegend(primary, stringResource(R.string.debt_legend_plan))
            ChartLegend(baselineColor.copy(alpha = 0.6f), stringResource(R.string.debt_legend_minimums))
        }
    }
}

@Composable
private fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(12.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun ExtraCard(extra: Int, totalMin: BigDecimal, onChange: (Int) -> Unit) {
    PlannerCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.debt_extra_each_month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.debt_on_top_of, totalMin.formatMoney()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(Icons.Filled.Remove, enabled = extra > 0) {
                onChange((extra - EXTRA_STEP).coerceAtLeast(0))
            }
            Text(
                text = BigDecimal(extra).formatMoney(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            StepButton(Icons.Filled.Add, enabled = extra < EXTRA_MAX, filled = true) {
                onChange((extra + EXTRA_STEP).coerceAtMost(EXTRA_MAX))
            }
        }
        Slider(
            value = extra.toFloat(),
            onValueChange = { onChange((it / EXTRA_STEP).toInt() * EXTRA_STEP) },
            valueRange = 0f..EXTRA_MAX.toFloat(),
            steps = EXTRA_MAX / EXTRA_STEP - 1,
        )
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    enabled: Boolean,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val disabledTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .then(if (filled) Modifier else Modifier.border(1.dp, outline, RoundedCornerShape(50)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) fg else disabledTint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun DebtListSection(debts: List<DebtEntity>, onAdd: () -> Unit, onEdit: (DebtEntity) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xs),
    ) {
        Text(
            text = stringResource(R.string.debt_your_debts),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(MaterialTheme.dimens.xs))
            Text(stringResource(R.string.debt_add))
        }
    }
    PlannerCard {
        debts.forEachIndexed { index, debt ->
            if (index > 0) Spacer(Modifier.height(MaterialTheme.dimens.sm))
            DebtRow(debt, onClick = { onEdit(debt) })
        }
    }
}

@Composable
private fun DebtRow(debt: DebtEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(debt.emoji.ifBlank { "💳" }, fontSize = 18.sp)
        }
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = debt.name.ifBlank { stringResource(R.string.debt_untitled) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(
                    R.string.debt_row_sub,
                    plainPercent(debt.aprPercent),
                    debt.minPayment.formatMoney(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(debt.balance.formatMoney(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PayoffOrderSection(plan: DebtPlan, strategy: PayoffStrategy) {
    if (plan.order.isEmpty()) return
    val stratName = stringResource(
        if (strategy == PayoffStrategy.SNOWBALL) R.string.debt_snowball else R.string.debt_avalanche,
    )
    Text(
        text = stringResource(R.string.debt_payoff_order, stratName),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xs),
    )
    PlannerCard {
        plan.order.forEachIndexed { index, row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = MaterialTheme.dimens.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (index == 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (index == 0) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(Modifier.width(MaterialTheme.dimens.md))
                Text(
                    "${row.emoji} ${row.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.debt_paid_off, row.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DebtEmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.dimens.xxl, start = MaterialTheme.dimens.xl, end = MaterialTheme.dimens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📉", fontSize = 40.sp)
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        Text(
            text = stringResource(R.string.debt_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            text = stringResource(R.string.debt_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Button(onClick = onAdd) { Text(stringResource(R.string.debt_empty_cta)) }
    }
}

// ── Add / edit sheet ──────────────────────────────────────────────────────────────────────────────

// An add/edit form: emoji picker, four fields and a live solo-cost line. The branching is inherent to
// a form, not real logic, so the complexity rule is suppressed.
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("CyclomaticComplexMethod")
@Composable
private fun DebtEditorSheet(
    initial: DebtEntity,
    isEditing: Boolean,
    onSave: (DebtEntity) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var emoji by remember { mutableStateOf(initial.emoji.ifBlank { DEBT_EMOJIS.first() }) }
    var name by remember { mutableStateOf(initial.name) }
    var balanceText by remember { mutableStateOf(initial.balance.takeIf { it.signum() > 0 }?.toPlainString() ?: "") }
    var aprText by remember { mutableStateOf(initial.aprPercent.takeIf { it.signum() > 0 }?.toPlainString() ?: "") }
    var minText by remember { mutableStateOf(initial.minPayment.takeIf { it.signum() > 0 }?.toPlainString() ?: "") }

    val balance = balanceText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val apr = aprText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val min = minText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val valid = name.isNotBlank() && balance.signum() > 0 && min.signum() > 0

    // Live: how long this one debt takes at its minimum alone, and what it costs in interest.
    val soloCost = remember(balance, apr, min) {
        if (balance.signum() > 0 && min.signum() > 0) {
            DebtPayoffSimulator.simulate(listOf(DebtInput(1, balance, apr, min)), BigDecimal.ZERO, null)
        } else {
            null
        }
    }

    AdaptiveSheet(onDismiss = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.sm)) {
            Text(
                text = stringResource(if (isEditing) R.string.debt_edit_title else R.string.debt_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs)) {
                DEBT_EMOJIS.forEach { e ->
                    val sel = e == emoji
                    Box(
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
                            .background(
                                if (sel) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                            )
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = 18.sp) }
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            PlannerNumberField(
                label = stringResource(R.string.debt_field_name),
                value = name,
                onValueChange = { name = it },
                labelColor = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            PlannerNumberField(
                label = stringResource(R.string.debt_field_balance),
                value = balanceText,
                onValueChange = { balanceText = it.filter { c -> c.isDigit() || c == '.' } },
                trailing = com.budgetty.app.ui.util.AppFormats.currencySymbol,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                PlannerNumberField(
                    label = stringResource(R.string.debt_field_apr),
                    value = aprText,
                    onValueChange = { aprText = it.filter { c -> c.isDigit() || c == '.' } },
                    trailing = "%",
                    modifier = Modifier.weight(1f),
                )
                PlannerNumberField(
                    label = stringResource(R.string.debt_field_min),
                    value = minText,
                    onValueChange = { minText = it.filter { c -> c.isDigit() || c == '.' } },
                    trailing = com.budgetty.app.ui.util.AppFormats.currencySymbol,
                    modifier = Modifier.weight(1f),
                )
            }
            if (soloCost != null && soloCost.clearedAll) {
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                Text(
                    text = stringResource(
                        R.string.debt_solo_cost,
                        soloCost.months,
                        soloCost.totalInterest.formatMoney(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                if (onDelete != null) {
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
                Button(
                    onClick = {
                        onSave(
                            initial.copy(
                                emoji = emoji,
                                name = name.trim(),
                                balance = balance,
                                aprPercent = apr,
                                minPayment = min,
                            ),
                        )
                    },
                    enabled = valid,
                    modifier = Modifier.weight(if (onDelete != null) 1.4f else 1f),
                ) { Text(stringResource(if (isEditing) R.string.action_save else R.string.debt_add)) }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
        }
    }
}

// ── Plan computation ──────────────────────────────────────────────────────────────────────────────

private data class PayoffOrderRow(val emoji: String, val name: String, val date: String)

private data class DebtPlan(
    val result: DebtPayoffResult,
    val baseline: DebtPayoffResult,
    val snowball: DebtPayoffResult,
    val avalanche: DebtPayoffResult,
    val debtFreeDate: String,
    val snowballDate: String,
    val avalancheDate: String,
    val monthsSooner: Int,
    val interestSaved: BigDecimal,
    val totalOwed: BigDecimal,
    val totalMin: BigDecimal,
    val order: List<PayoffOrderRow>,
)

private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())

private fun monthsFromNow(months: Int): String =
    LocalDate.now().plusMonths(months.toLong()).format(MONTH_YEAR)

private fun computePlan(debts: List<DebtEntity>, extra: Int, strategy: PayoffStrategy): DebtPlan {
    val inputs = debts.map { DebtInput(it.id, it.balance, it.aprPercent, it.minPayment) }
    val extraBd = BigDecimal(extra)
    val result = DebtPayoffSimulator.simulate(inputs, extraBd, strategy)
    val baseline = DebtPayoffSimulator.simulate(inputs, BigDecimal.ZERO, null)
    val snowball = DebtPayoffSimulator.simulate(inputs, extraBd, PayoffStrategy.SNOWBALL)
    val avalanche = DebtPayoffSimulator.simulate(inputs, extraBd, PayoffStrategy.AVALANCHE)
    val byId = debts.associateBy { it.id }
    val order = result.payoffMonthById.entries
        .sortedBy { it.value }
        .mapNotNull { (id, month) ->
            byId[id]?.let { PayoffOrderRow(it.emoji.ifBlank { "💳" }, it.name, monthsFromNow(month)) }
        }
    return DebtPlan(
        result = result,
        baseline = baseline,
        snowball = snowball,
        avalanche = avalanche,
        debtFreeDate = monthsFromNow(result.months),
        snowballDate = monthsFromNow(snowball.months),
        avalancheDate = monthsFromNow(avalanche.months),
        monthsSooner = (baseline.months - result.months).coerceAtLeast(0),
        interestSaved = (baseline.totalInterest - result.totalInterest).coerceAtLeast(BigDecimal.ZERO),
        totalOwed = debts.fold(BigDecimal.ZERO) { acc, d -> acc + d.balance },
        totalMin = debts.fold(BigDecimal.ZERO) { acc, d -> acc + d.minPayment },
        order = order,
    )
}

/** APR as a plain, trailing-zero-trimmed percent string, e.g. "19.9" or "6" (no currency). */
private fun plainPercent(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun DebtPayoffPreview() {
    BudgettyTheme {
        DebtPayoffContent(
            debts = listOf(
                DebtEntity(1, "💳", "Credit card", BigDecimal("2400"), BigDecimal("19.9"), BigDecimal("60")),
                DebtEntity(2, "🚗", "Car loan", BigDecimal("6800"), BigDecimal("6.5"), BigDecimal("210")),
                DebtEntity(3, "🛍️", "Store card", BigDecimal("650"), BigDecimal("24.9"), BigDecimal("25")),
            ),
            onNavigateBack = {},
            onSaveDebt = {},
            onDeleteDebt = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun DebtPayoffEmptyPreview() {
    BudgettyTheme {
        DebtPayoffContent(debts = emptyList(), onNavigateBack = {}, onSaveDebt = {}, onDeleteDebt = {})
    }
}
