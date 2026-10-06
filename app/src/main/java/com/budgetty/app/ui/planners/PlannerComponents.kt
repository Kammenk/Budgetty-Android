package com.budgetty.app.ui.planners

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budgetty.app.ui.theme.dimens

/** A rounded planner card (surfaceContainer, xl radius), matching the Insights card chrome. */
@Composable
internal fun PlannerCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.lg), content = content)
    }
}

/**
 * A filled, labelled numeric input (the planner's shared field style): a small primary/muted label over
 * the value, with an optional [trailing] unit (e.g. "%" or the currency symbol). Decimal keyboard.
 */
@Composable
internal fun PlannerNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: String? = null,
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(MaterialTheme.dimens.radiusMd),
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            modifier = Modifier.padding(start = 14.dp, top = 8.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 14.dp),
                )
            }
        }
    }
}

/** Standard content padding for a scrolling planner screen body (gutters + a comfortable bottom). */
internal val plannerContentPadding: PaddingValues
    @Composable get() = PaddingValues(
        start = MaterialTheme.dimens.lg,
        end = MaterialTheme.dimens.lg,
        top = MaterialTheme.dimens.sm,
        bottom = MaterialTheme.dimens.xxl,
    )
