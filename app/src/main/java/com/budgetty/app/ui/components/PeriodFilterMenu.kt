package com.budgetty.app.ui.components

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetty.app.R
import com.budgetty.app.ui.home.DateRangeFilter

/**
 * A filter icon that opens a dropdown of the period options; the active one is checked.
 *
 * When [customLabel] is non-null an extra "Custom" entry is appended below the presets. Selecting
 * it invokes [onCustomClick] (e.g. to open a date-range picker) rather than [onSelected]. While a
 * custom range is active, pass `selected = null` and `customSelected = true` so the check moves to
 * the custom row.
 */
@Composable
fun PeriodFilterMenu(
    selected: DateRangeFilter?,
    onSelected: (DateRangeFilter) -> Unit,
    modifier: Modifier = Modifier,
    customLabel: String? = null,
    customSelected: Boolean = false,
    onCustomClick: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.cd_filter_period))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DateRangeFilter.entries.forEach { option ->
                val isSelected = option == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(option.labelRes),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    // Leading check on the active preset (an empty slot keeps labels aligned).
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Box(Modifier.size(MaterialTheme.dimens.icon))
                        }
                    },
                )
            }
            if (customLabel != null && onCustomClick != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            text = customLabel,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    onClick = {
                        onCustomClick()
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = if (customSelected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
