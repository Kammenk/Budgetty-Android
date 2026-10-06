package com.budgetty.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetty.app.R
import com.budgetty.app.data.local.TagCount
import com.budgetty.app.data.local.TagEntity
import com.budgetty.app.ui.theme.dimens

/** "#name" with the hash tinted in the primary colour — the shared tag label used by every pill. */
@Composable
private fun tagLabel(name: String) = buildAnnotatedString {
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("#") }
    append(name)
}

/**
 * A read-only outlined "#tag" pill. Categories stay filled emoji badges; tags are outline-only with
 * no colour fill and no emoji, so the two systems never blur together (per the tags design). Used on
 * transaction / receipt rows.
 */
@Composable
fun TagPill(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 50))
            .padding(horizontal = 7.dp, vertical = 1.dp),
    ) {
        Text(
            text = tagLabel(name),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * The review screen's "Tags" field: the row's tags as removable outlined pills plus a dashed
 * "＋ tag" chip, all opening the [TagInputSheet]. Reads the ambient [LocalTagEditing] for the
 * catalog/recent and the add/remove callbacks, keyed by this row's [clientId].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagField(clientId: String, tags: List<String>, modifier: Modifier = Modifier) {
    val editing = LocalTagEditing.current
    var showSheet by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { showSheet = true }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = stringResource(R.string.upload_tags),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tags.forEach { tag ->
                RemovableTagChip(name = tag, onRemove = { editing.onRemove(clientId, tag) })
            }
            AddTagChip(onClick = { showSheet = true })
        }
    }
    if (showSheet) {
        TagInputSheet(
            current = tags,
            catalog = editing.catalog,
            recent = editing.recent,
            onAdd = { editing.onAdd(clientId, it) },
            onRemove = { editing.onRemove(clientId, it) },
            onDismiss = { showSheet = false },
        )
    }
}

/** An applied tag in the review field: outlined "#tag" with a trailing ✕ that removes it. */
@Composable
private fun RemovableTagChip(name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 50))
            .clickable(onClick = onRemove)
            .padding(start = 10.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tagLabel(name),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Filled.Close,
            contentDescription = stringResource(R.string.tag_remove, name),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** The dashed "＋ tag" affordance that opens the tag sheet. */
@Composable
private fun AddTagChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.tag_add_chip),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * The tag input bottom sheet: the applied tags as removable chips, a normalised text input, and
 * autocomplete. Typing filters existing tags by substring ("Existing tags"); when nothing matches it
 * falls back to tags sharing the first three letters ("Similar"). A "Create '#x'" row appears unless
 * the tag already exists. Recent tags are one tap. Mirrors the tags design (2b).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TagInputSheet(
    current: List<String>,
    catalog: List<TagCount>,
    recent: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val normalized = TagEntity.normalize(query)
    val selected = current.toSet()

    val substring = remember(normalized, catalog, selected) {
        if (normalized.isEmpty()) {
            emptyList()
        } else {
            catalog.filter { it.name.contains(normalized) && it.name !in selected }
        }
    }
    val similar = remember(normalized, catalog, selected, substring) {
        if (normalized.length >= 3 && substring.isEmpty()) {
            val prefix = normalized.take(3)
            catalog.filter { it.name.startsWith(prefix) && it.name !in selected }
        } else {
            emptyList()
        }
    }
    val suggestions = (if (substring.isNotEmpty()) substring else similar).take(6)
    val exactExists = catalog.any { it.name == normalized }
    val canCreate = normalized.isNotEmpty() && !exactExists && normalized !in selected
    val recentChips = remember(recent, selected) { recent.filter { it !in selected }.take(8) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    AdaptiveSheet(onDismiss = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.padding(horizontal = MaterialTheme.dimens.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.tag_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
            }
            if (current.isNotEmpty()) {
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    current.forEach { tag -> SelectedTagChip(name = tag, onRemove = { onRemove(tag) }) }
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.tag_sheet_input_hint)) },
                singleLine = true,
                prefix = { Text("#", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (canCreate) {
                        onAdd(normalized)
                        query = ""
                    }
                }),
                shape = RoundedCornerShape(percent = 50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            Text(
                text = stringResource(R.string.tag_sheet_normalize_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = MaterialTheme.dimens.md, top = 5.dp),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                if (suggestions.isNotEmpty()) {
                    item {
                        SheetSectionLabel(
                            if (substring.isNotEmpty()) R.string.tag_sheet_existing else R.string.tag_sheet_similar,
                        )
                    }
                    items(suggestions, key = { it.name }) { tag ->
                        SuggestionRow(tag = tag, onClick = { onAdd(tag.name); query = "" })
                    }
                }
                if (canCreate) {
                    item {
                        CreateTagRow(normalized = normalized, onClick = { onAdd(normalized); query = "" })
                    }
                }
                if (recentChips.isNotEmpty()) {
                    item { SheetSectionLabel(R.string.tag_sheet_recent) }
                    item {
                        FlowRow(
                            modifier = Modifier.padding(top = 4.dp, bottom = MaterialTheme.dimens.md),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            recentChips.forEach { name ->
                                RecentTagChip(name = name, onClick = { onAdd(name); query = "" })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A selected tag inside the sheet: filled (secondary container) with a ✕. */
@Composable
private fun SelectedTagChip(name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onRemove)
            .padding(start = 10.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tagLabel(name),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Filled.Close,
            contentDescription = stringResource(R.string.tag_remove, name),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun SheetSectionLabel(resId: Int) {
    Text(
        text = stringResource(resId),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = MaterialTheme.dimens.sm, bottom = 4.dp),
    )
}

@Composable
private fun SuggestionRow(tag: TagCount, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Sell,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = tagLabel(tag.name),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (tag.count > 0) {
            Text(
                text = pluralStringResource(R.plurals.tag_uses, tag.count, tag.count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreateTagRow(normalized: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(13.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.tag_sheet_create, normalized),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun RecentTagChip(name: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 26.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = tagLabel(name),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
