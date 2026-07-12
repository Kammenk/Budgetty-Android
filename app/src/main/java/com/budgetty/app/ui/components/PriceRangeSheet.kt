package com.budgetty.app.ui.components

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetty.app.R
import com.budgetty.app.ui.util.formatMoney
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A bottom sheet (centered dialog on tablets, via [AdaptiveSheet]) for filtering History by line
 * total. Seeded with [initialMin]/[initialMax] when a range is already active; the slider spans
 * `0..`[upperBound]. "Apply" reports the chosen bounds — a bound that sits at the slider's extreme is
 * reported as `null` (unbounded) so a full-width selection clears the filter rather than capping it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceRangeSheet(
    initialMin: BigDecimal?,
    initialMax: BigDecimal?,
    upperBound: BigDecimal,
    onConfirm: (min: BigDecimal?, max: BigDecimal?) -> Unit,
    onDismiss: () -> Unit,
) {
    val maxF = upperBound.toFloat().coerceAtLeast(1f)
    var range by remember {
        mutableStateOf((initialMin?.toFloat() ?: 0f)..(initialMax?.toFloat() ?: maxF))
    }

    AdaptiveSheet(onDismiss = onDismiss) {
        // Scrolls so the presets and action buttons stay reachable on short screens (e.g. landscape);
        // weight(fill = false) keeps the sheet compact at its natural height otherwise.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.xs),
        ) {
            Text(
                text = stringResource(R.string.price_range_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = MaterialTheme.dimens.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = rangeReadout(range.start, range.endInclusive, maxF),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                // Snaps the slider back to the full range (which, on Apply, clears the filter).
                val isFullRange = range.start <= 0f && range.endInclusive >= maxF
                IconButton(onClick = { range = 0f..maxF }, enabled = !isFullRange) {
                    Icon(
                        imageVector = Icons.Filled.Restore,
                        contentDescription = stringResource(R.string.cd_reset_price_range),
                    )
                }
            }

            RangeSlider(value = range, onValueChange = { range = it }, valueRange = 0f..maxF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = BigDecimal.ZERO.formatMoney(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = upperBound.formatMoney(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(R.string.price_presets),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = MaterialTheme.dimens.lg, bottom = MaterialTheme.dimens.sm),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm)) {
                pricePresets(maxF).forEach { preset ->
                    PresetChip(label = preset.label) { range = preset.start..preset.end }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.dimens.xl, bottom = MaterialTheme.dimens.sm),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(MaterialTheme.dimens.buttonHeight),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        val min = if (range.start <= 0f) null else range.start.toMoney()
                        val max = if (range.endInclusive >= maxF) null else range.endInclusive.toMoney()
                        onConfirm(min, max)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(MaterialTheme.dimens.buttonHeight),
                ) { Text(stringResource(R.string.action_apply)) }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = MaterialTheme.dimens.sm),
        )
    }
}

private class PricePreset(val label: String, val start: Float, val end: Float)

/** The fixed Under/range/Over presets, dropping any whose lower bound exceeds the data's max. */
@Composable
private fun pricePresets(maxF: Float): List<PricePreset> {
    fun money(v: Int) = BigDecimal(v).formatMoney()
    return buildList {
        add(PricePreset(stringResource(R.string.price_preset_under, money(10)), 0f, 10f))
        add(PricePreset(stringResource(R.string.price_range_value, money(10), money(50)), 10f, 50f))
        add(PricePreset(stringResource(R.string.price_range_value, money(50), money(100)), 50f, 100f))
        add(PricePreset(stringResource(R.string.price_preset_over, money(100)), 100f, maxF))
    }.filter { it.start < maxF }.map { PricePreset(it.label, it.start, it.end.coerceAtMost(maxF)) }
}

@Composable
private fun rangeReadout(start: Float, end: Float, maxF: Float): String =
    if (start <= 0f && end >= maxF) {
        stringResource(R.string.price_range_any)
    } else {
        stringResource(R.string.price_range_value, start.toMoney().formatMoney(), end.toMoney().formatMoney())
    }

private fun Float.toMoney(): BigDecimal = BigDecimal(toDouble()).setScale(2, RoundingMode.HALF_UP)
