package com.budgetty.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.budgetty.app.ui.theme.dimens

/**
 * The Insights tab strip as a horizontally scrollable row of fully-rounded pills — the same corner
 * radius as the [PeriodStepper] pill above it, and the iOS `InsightsTabBar`. Unlike the fixed
 * [com.budgetty.app.ui.components.SegmentedToggle], the pills keep their natural width and scroll, so
 * all groups stay reachable at any screen size or font scale instead of squeezing five labels into one
 * fixed row (a small phone or large font would otherwise truncate them). The pills use the same corner
 * radius as the History screen's filter chips (`MaterialTheme.shapes.small`, 12dp); the selected pill
 * takes the primary fill; tapping scrolls the selection into view.
 */
@Composable
fun InsightsTabRow(
    tabs: List<InsightsTab>,
    selected: InsightsTab,
    onSelect: (InsightsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Keep the active pill on screen when it changes (e.g. an Overview "Spending ›" deep-link).
    LaunchedEffect(selected) {
        val index = tabs.indexOf(selected)
        if (index >= 0) listState.animateScrollToItem(index)
    }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        items(tabs) { tab ->
            val isSelected = tab == selected
            Text(
                text = stringResource(tab.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                // Match the History screen's FilterChips: a tonal `secondaryContainer` fill when
                // selected (not the bright `primary`), on a `surfaceContainerHigh` resting pill.
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    )
                    .clickable { onSelect(tab) }
                    .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.sm),
            )
        }
    }
}
