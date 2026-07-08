package com.budgetty.app.ui.widgets

import com.budgetty.app.ui.theme.dimens
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetBadColor
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.util.AppFormats
import com.budgetty.app.widget.WidgetKind
import com.budgetty.app.widget.WidgetPinning
import kotlinx.coroutines.launch

/**
 * Account → Widgets: a picker for the home-screen widgets. One card per type with a live Compose
 * mock, a Large/Compact size toggle, and an "Add to home screen" button that asks the launcher to
 * pin the chosen widget (then flips to a confirmation state).
 */
@Composable
fun WidgetsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WidgetsScreenContent(onNavigateBack = onNavigateBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetsScreenContent(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // Re-query each widget's placement whenever the screen resumes — e.g. after returning from the
    // launcher's pin dialog or after the user drags a widget on — so the buttons reflect real state.
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widgets_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                // Edge-to-edge: the nav Scaffold already applies the status-bar inset, so the app
                // bar adds none of its own (matches BudgetScreen / UploadScreen).
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.widthIn(max = 520.dp)) {
                Spacer(Modifier.height(MaterialTheme.dimens.xs))
                Text(
                    text = stringResource(R.string.widgets_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.lg))
                WidgetKind.entries.forEach { kind ->
                    WidgetTypeCard(
                        kind = kind,
                        snackbarHostState = snackbarHostState,
                        refreshKey = refreshKey,
                    )
                    Spacer(Modifier.height(MaterialTheme.dimens.lg))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetTypeCard(kind: WidgetKind, snackbarHostState: SnackbarHostState, refreshKey: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Default the toggle to the size the user last added for this type (else Large), so reopening the
    // picker shows what they chose before instead of always snapping back to Large.
    var large by remember(kind) { mutableStateOf(WidgetPinning.lastChosenLarge(context, kind) ?: true) }
    // Whether this type+size is already on a home screen. Recomputed when the size flips or the
    // screen resumes (refreshKey), so it tracks the launcher rather than an optimistic flag.
    val added = remember(kind, large, refreshKey) { WidgetPinning.isPlaced(context, kind, large) }
    // The size the user last added for this type (null = never), shown as a caption below the toggle.
    val lastLarge = remember(kind, refreshKey) { WidgetPinning.lastChosenLarge(context, kind) }
    val unsupportedMessage = stringResource(R.string.widgets_pin_unsupported)
    // Set true the instant the user taps Add (the launcher's pin dialog is now up). It lets the
    // resume effect below tell a real confirmation apart from a cancel.
    var pendingAdd by remember(kind) { mutableStateOf(false) }
    val activity = context.findActivity()
    // Auto-reveal: after the pin dialog closes and the screen resumes, if the widget we were waiting
    // on is now placed the user confirmed it — minimize the app so they see it land on the home
    // screen. If it's still not placed they cancelled, so just clear the flag and stay put.
    LaunchedEffect(refreshKey) {
        if (pendingAdd) {
            pendingAdd = false
            if (added) activity?.moveTaskToBack(true)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.lg)) {
            WidgetPreview(kind = kind, large = large)
            Spacer(Modifier.height(MaterialTheme.dimens.lg))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(kind.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(MaterialTheme.dimens.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(kind.titleRes()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(kind.descRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = large,
                    onClick = { large = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.widgets_size_large)) }
                SegmentedButton(
                    selected = !large,
                    onClick = { large = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.widgets_size_compact)) }
            }

            if (lastLarge != null) {
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                Text(
                    text = stringResource(
                        R.string.widgets_last_added,
                        stringResource(if (lastLarge) R.string.widgets_size_large else R.string.widgets_size_compact),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))

            Button(
                onClick = {
                    // Ask the launcher to pin; the button flips to "Added" once the widget is
                    // actually placed (detected on resume), not optimistically here.
                    if (WidgetPinning.request(context, kind, large)) {
                        // Pin dialog is up — mark that we're waiting so the resume effect can reveal
                        // the home screen once it's confirmed.
                        pendingAdd = true
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(unsupportedMessage) }
                    }
                },
                enabled = !added,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.dimens.buttonHeight),
            ) {
                if (added) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(MaterialTheme.dimens.xl))
                    Spacer(Modifier.width(MaterialTheme.dimens.sm))
                    Text(
                        text = stringResource(R.string.widgets_added),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.widgets_add),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** The live mock of a widget, drawn in Compose so it themes with the app. */
@Composable
private fun WidgetPreview(kind: WidgetKind, large: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (large) 150.dp else 190.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = if (large) Modifier.fillMaxWidth() else Modifier.width(190.dp),
            shape = RoundedCornerShape(MaterialTheme.dimens.xxl),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Box(Modifier.padding(if (large) MaterialTheme.dimens.lg else 14.dp)) {
                when (kind) {
                    WidgetKind.SUMMARY -> SummaryPreview(large)
                    WidgetKind.THIS_WEEK -> ThisWeekPreview(large)
                    WidgetKind.SCAN -> ScanPreview(large)
                    WidgetKind.TOP_CATEGORIES -> TopCategoriesPreview(large)
                    WidgetKind.BUDGET -> BudgetPreview(large)
                }
            }
        }
    }
}

@Composable
private fun BudgetPreview(large: Boolean) {
    val symbol = AppFormats.currencySymbol
    // Representative monthly state for the picker thumbnail; the live widget adapts to the user's period.
    val spent = "712"
    val budget = "1,200"
    val pct = 59
    val remaining = "488"
    val status = budgetWarnColor()
    val chip = "Jun 2026"
    val icon = Icons.Filled.BarChart

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (large) 28.dp else MaterialTheme.dimens.xxl)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(if (large) MaterialTheme.dimens.lg else 14.dp),
                )
            }
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Text(
                text = stringResource(R.string.widget_budget),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(text = chip, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(if (large) 14.dp else 10.dp))
        Text(
            text = "$spent $symbol",
            fontSize = if (large) 34.sp else 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.widget_of_budget, "$budget $symbol"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(if (large) MaterialTheme.dimens.lg else MaterialTheme.dimens.md))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.widget_pct_spent, pct),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.widget_remaining, "$remaining $symbol"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = status,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { pct / 100f },
            color = status,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.dimens.sm)
                .clip(RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun ThisWeekPreview(large: Boolean) {
    val symbol = AppFormats.currencySymbol
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (large) 28.dp else MaterialTheme.dimens.xxl)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(if (large) MaterialTheme.dimens.lg else 14.dp),
                )
            }
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Text(
                text = stringResource(R.string.widget_this_week),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(text = "Jun 24–30", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(if (large) 14.dp else 10.dp))
        Text(
            text = "173 $symbol",
            fontSize = if (large) 34.sp else 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.widget_vs_last_week, "▼ 12%"),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = budgetGoodColor(),
        )
        if (large) {
            Spacer(Modifier.height(14.dp))
            WeekCompareRow(stringResource(R.string.widget_this_week), "173 $symbol", 1f, MaterialTheme.colorScheme.primary, true)
            Spacer(Modifier.height(7.dp))
            WeekCompareRow(stringResource(R.string.widget_last_week), "146 $symbol", 0.84f, MaterialTheme.colorScheme.onSurfaceVariant, false)
        }
    }
}

@Composable
private fun WeekCompareRow(label: String, amount: String, ratio: Float, barColor: Color, emphasized: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(58.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        LinearProgressIndicator(
            progress = { ratio },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(50)),
        )
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text(
            amount,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScanPreview(large: Boolean) {
    if (large) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.widget_scan), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.widget_scan_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 18.dp, vertical = 9.dp),
                ) {
                    Text(stringResource(R.string.widget_scan_action), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.widget_scans_left, 3), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.widget_scan_action), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TopCategoriesPreview(large: Boolean) {
    val symbol = AppFormats.currencySymbol
    val cats = listOf(
        PreviewCat("Groceries", 242, Categories.colorOf("Groceries")),
        PreviewCat("Shopping", 117, Categories.colorOf("Shopping & Lifestyle")),
        PreviewCat("Dining", 114, Categories.colorOf("Dining & Entertainment")),
        PreviewCat("Transport", 90, Categories.colorOf("Transportation")),
    )
    val total = 712f
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(if (large) 26.dp else MaterialTheme.dimens.xxl).clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.DonutLarge, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(if (large) 15.dp else 14.dp))
            }
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Text(stringResource(R.string.widget_topcat), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("Jun 2026", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(MaterialTheme.dimens.md))
        val rows = if (large) cats else cats.take(3)
        rows.forEachIndexed { i, c ->
            if (i > 0) Spacer(Modifier.height(MaterialTheme.dimens.sm))
            TopCatRow(c, total, symbol, large)
        }
    }
}

@Composable
private fun TopCatRow(c: PreviewCat, total: Float, symbol: String, showPercent: Boolean) {
    val ratio = (c.amount / total).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(MaterialTheme.dimens.sm).clip(RoundedCornerShape(2.dp)).background(Color(c.color)))
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text(c.name, modifier = Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodySmall)
        if (showPercent) {
            Text("${(ratio * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
        }
        LinearProgressIndicator(
            progress = { ratio },
            color = Color(c.color),
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            modifier = Modifier.width(44.dp).height(MaterialTheme.dimens.xs).clip(RoundedCornerShape(50)),
        )
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Text("${c.amount} $symbol", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

private data class PreviewCat(val name: String, val amount: Int, val color: Int)

@Composable
private fun SummaryPreview(large: Boolean) {
    val symbol = AppFormats.currencySymbol
    val cats = listOf(
        PreviewCat("Groceries", 242, Categories.colorOf("Groceries")),
        PreviewCat("Shopping", 117, Categories.colorOf("Shopping & Lifestyle")),
        PreviewCat("Dining", 114, Categories.colorOf("Dining & Entertainment")),
    )
    val topAmount = cats.first().amount

    Column(Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.widget_this_month),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (large) stringResource(R.string.widget_receipts, 18) else "18",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(if (large) 10.dp else 6.dp))
        Text(
            text = "712.40 $symbol",
            fontSize = if (large) 30.sp else 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = if (large) stringResource(R.string.widget_vs_last_month, "+12%") else "+12%",
            style = MaterialTheme.typography.labelMedium,
            // +12% = spending more than last month → red (matches the live widget and the mockup).
            color = budgetBadColor(),
        )
        Spacer(Modifier.height(if (large) MaterialTheme.dimens.md else MaterialTheme.dimens.sm))
        cats.take(if (large) 3 else 2).forEachIndexed { index, cat ->
            if (index > 0) Spacer(Modifier.height(if (large) MaterialTheme.dimens.sm else 6.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(MaterialTheme.dimens.sm)
                        .clip(RoundedCornerShape(50))
                        .background(Color(cat.color)),
                )
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                Text(
                    text = cat.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(if (large) 72.dp else 60.dp),
                )
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                LinearProgressIndicator(
                    progress = { cat.amount.toFloat() / topAmount },
                    color = Color(cat.color),
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                )
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                Text(
                    text = "${cat.amount} $symbol",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun WidgetKind.titleRes(): Int = when (this) {
    WidgetKind.BUDGET -> R.string.widget_budget
    WidgetKind.SUMMARY -> R.string.widget_summary
    WidgetKind.THIS_WEEK -> R.string.widget_this_week
    WidgetKind.SCAN -> R.string.widget_scan
    WidgetKind.TOP_CATEGORIES -> R.string.widget_topcat
}

private fun WidgetKind.descRes(): Int = when (this) {
    WidgetKind.BUDGET -> R.string.widget_budget_desc
    WidgetKind.SUMMARY -> R.string.widget_summary_desc
    WidgetKind.THIS_WEEK -> R.string.widget_thisweek_desc
    WidgetKind.SCAN -> R.string.widget_scan_desc
    WidgetKind.TOP_CATEGORIES -> R.string.widget_topcat_desc
}

private fun WidgetKind.icon(): ImageVector = when (this) {
    WidgetKind.BUDGET -> Icons.Filled.BarChart
    WidgetKind.SUMMARY -> Icons.Filled.PieChart
    WidgetKind.THIS_WEEK -> Icons.AutoMirrored.Filled.TrendingUp
    WidgetKind.SCAN -> Icons.Filled.PhotoCamera
    WidgetKind.TOP_CATEGORIES -> Icons.Filled.DonutLarge
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun WidgetsScreenPreview() {
    BudgettyTheme {
        WidgetsScreenContent(onNavigateBack = {})
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
