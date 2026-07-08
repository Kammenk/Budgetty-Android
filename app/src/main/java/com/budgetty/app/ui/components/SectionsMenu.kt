package com.budgetty.app.ui.components

import com.budgetty.app.ui.theme.dimens
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.budgetty.app.R

/**
 * Reorders [sections] into the user's saved display [order]: known keys first in the saved order,
 * then any section not yet listed (kept in its default [sections] position). Empty [order] — and any
 * keys that no longer map to a section — fall back to the default order, so the result is always the
 * full set exactly once and stays valid as sections are added or removed across app versions.
 */
fun <T> resolveSectionOrder(order: List<String>, sections: List<T>, key: (T) -> String): List<T> {
    if (order.isEmpty()) return sections
    val byKey = sections.associateBy(key)
    val ordered = order.mapNotNull { byKey[it] }
    val rest = sections.filter { it !in ordered }
    return ordered + rest
}

/**
 * The header "customize sections" entry point: a [Tune] icon that opens an [AdaptiveSheet] letting the
 * user show/hide and reorder a screen's content sections. Generic over the section type so Home and
 * Insights reuse it with their own enums — [sectionKey] gives the stable key compared against
 * [hiddenSections] / saved in [order], and [labelRes] the row label.
 *
 * @param onToggle invoked with the section and its new *hidden* state when a row is tapped.
 * @param onReorder invoked with the full new key order when a section is moved up or down.
 * @param onRevertToDefault invoked when the user taps "Revert to default" to restore the original
 *   visibility and order.
 */
@Composable
fun <T> SectionsMenu(
    sections: List<T>,
    order: List<String>,
    hiddenSections: Set<String>,
    sectionKey: (T) -> String,
    @StringRes labelRes: (T) -> Int,
    onToggle: (T, Boolean) -> Unit,
    onReorder: (List<String>) -> Unit,
    onRevertToDefault: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.cd_customize_sections))
    }
    if (open) {
        CustomizeSectionsSheet(
            sections = sections,
            order = order,
            hiddenSections = hiddenSections,
            sectionKey = sectionKey,
            labelRes = labelRes,
            onToggle = onToggle,
            onReorder = onReorder,
            onRevertToDefault = onRevertToDefault,
            onDismiss = { open = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> CustomizeSectionsSheet(
    sections: List<T>,
    order: List<String>,
    hiddenSections: Set<String>,
    sectionKey: (T) -> String,
    @StringRes labelRes: (T) -> Int,
    onToggle: (T, Boolean) -> Unit,
    onReorder: (List<String>) -> Unit,
    onRevertToDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    val ordered = resolveSectionOrder(order, sections, sectionKey)
    val keys = ordered.map(sectionKey)
    AdaptiveSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.dimens.xl, end = MaterialTheme.dimens.xl, bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.customize_sections_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.xs))
            Text(
                text = stringResource(R.string.customize_sections_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            ordered.forEachIndexed { index, section ->
                val visible = sectionKey(section) !in hiddenSections
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Tapping the label/checkbox flips visibility: the new "hidden" value is the
                    // current "visible" value.
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                            .clickable { onToggle(section, visible) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = visible, onCheckedChange = null)
                        Spacer(Modifier.width(MaterialTheme.dimens.sm))
                        Text(
                            text = stringResource(labelRes(section)),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    IconButton(
                        onClick = { onReorder(keys.swapped(index, index - 1)) },
                        enabled = index > 0,
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.cd_move_section_up),
                        )
                    }
                    IconButton(
                        onClick = { onReorder(keys.swapped(index, index + 1)) },
                        enabled = index < ordered.lastIndex,
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_move_section_down),
                        )
                    }
                }
            }
            // Offer a reset only once the user has actually customized something; tapping it restores
            // the default order and visibility, which the open sheet reflects immediately.
            if (order.isNotEmpty() || hiddenSections.isNotEmpty()) {
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                        .clickable(onClick = onRevertToDefault)
                        .padding(horizontal = MaterialTheme.dimens.sm, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(MaterialTheme.dimens.sm))
                    Text(
                        text = stringResource(R.string.customize_sections_reset),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Returns a copy of this list with the items at [a] and [b] swapped. */
private fun List<String>.swapped(a: Int, b: Int): List<String> =
    toMutableList().apply {
        val tmp = this[a]
        this[a] = this[b]
        this[b] = tmp
    }
