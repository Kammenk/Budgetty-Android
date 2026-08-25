package com.budgetty.app.ui.buyinglimits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetty.app.R
import com.budgetty.app.data.local.BuyingLimitEntity
import com.budgetty.app.data.local.BuyingLimitTimeframe
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.components.SegmentedToggle
import com.budgetty.app.ui.savings.SavingsFieldShape
import com.budgetty.app.ui.savings.SavingsSheetLabel
import com.budgetty.app.ui.savings.savingsFieldColors
import com.budgetty.app.ui.theme.budgetWarnColor
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.BuyingLimitPreview
import com.budgetty.app.ui.util.CountableItem

/** Emoji offered in the buying-limit editor — a small, everyday-purchase-flavoured curated set. */
val BUYING_LIMIT_EMOJIS: List<String> = listOf(
    "🥤", "☕", "🍫", "🍺", "🍷", "🚬", "🍔", "🍟", "🍕", "🍩",
    "🍪", "🧋", "🥐", "🍜", "🧃", "🍿", "🍭", "🥟", "🧀", "🥩",
)

/**
 * Commits any complete tokens in [raw] (split on space / comma / return) into [current] as normalized,
 * de-duplicated keyword chips, returning the updated list and the trailing partial token still being
 * typed. Pure so the commit-on-delimiter behaviour is obvious and testable.
 */
private fun commitKeywordInput(current: List<String>, raw: String): Pair<List<String>, String> {
    if (raw.none { it == ' ' || it == ',' || it == '\n' }) return current to raw
    val parts = raw.split(' ', ',', '\n')
    var result = current
    parts.dropLast(1).forEach { token ->
        val key = BuyingLimitEntity.normalizeKeyword(token)
        if (key.isNotEmpty() && key !in result) result = result + key
    }
    return result to parts.last()
}

/**
 * Create (initial == null) or edit a buying limit. [onDelete] is non-null only when editing. Follows
 * the bottom-sheet scroll convention (scroll region capped with weight(1f, fill = false)); the
 * Cancel/Save action row sits fixed below it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyingLimitEditorSheet(
    initial: BuyingLimitEntity?,
    items: List<CountableItem>,
    monthStartDay: Int,
    onSave: (emoji: String, label: String, keywords: List<String>, timeframe: BuyingLimitTimeframe, count: Int) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    // Whether the sheet edits an existing limit (title + delete) vs creates one. Defaults to "editing
    // when a subject was passed", but a suggestion pre-fills [initial] for a NEW limit (§4.4), so it
    // can be overridden false to keep the "New limit" title and hide delete.
    isEditing: Boolean = initial != null,
) {
    AdaptiveSheet(onDismiss = onDismiss) {
        BuyingLimitEditorBody(
            initial = initial,
            isEditing = isEditing,
            items = items,
            monthStartDay = monthStartDay,
            onSave = onSave,
            onDelete = onDelete,
            onDismiss = onDismiss,
        )
    }
}

/**
 * The editor's scrolling body plus the fixed Cancel/Save row, split out of [BuyingLimitEditorSheet] so
 * it can be rendered on its own (screenshot goldens) without the bottom-sheet / dialog window wrapper.
 * Runs in the [ColumnScope] the sheet provides, keeping the weight(1f, fill = false) scroll cap.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ColumnScope.BuyingLimitEditorBody(
    initial: BuyingLimitEntity?,
    items: List<CountableItem>,
    monthStartDay: Int,
    onSave: (emoji: String, label: String, keywords: List<String>, timeframe: BuyingLimitTimeframe, count: Int) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    isEditing: Boolean = initial != null,
) {
    var emoji by remember { mutableStateOf(initial?.emoji.orEmpty()) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var keywords by remember { mutableStateOf(initial?.keywordList ?: emptyList()) }
    var keywordField by remember { mutableStateOf("") }
    var timeframe by remember { mutableStateOf(initial?.timeframe ?: BuyingLimitTimeframe.MONTHLY) }
    var count by remember { mutableStateOf(initial?.count ?: 1) }

    // Provisional keywords include the still-uncommitted field so the preview updates as the user types.
    val previewKeywords = if (keywordField.isBlank()) keywords else keywords + keywordField
    val preview = remember(previewKeywords, timeframe, items, label, monthStartDay) {
        BuyingLimitPreview.compute(items, previewKeywords, timeframe, label, monthStartDay = monthStartDay)
    }
    val canSave = keywords.isNotEmpty() || keywordField.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.dimens.xl),
    ) {
        EditorHeader(isEditing = isEditing, onDelete = onDelete)
        Spacer(Modifier.height(MaterialTheme.dimens.md))

        EmojiAndLabel(
            emoji = emoji,
            showPicker = showEmojiPicker,
            onTogglePicker = { showEmojiPicker = !showEmojiPicker },
            onSelectEmoji = { emoji = if (emoji == it) "" else it },
            label = label,
            onLabelChange = { label = it },
        )
        Spacer(Modifier.height(MaterialTheme.dimens.lg))

        SavingsSheetLabel(stringResource(R.string.buying_limits_keywords))
        Spacer(Modifier.height(6.dp))
        KeywordInput(
            keywords = keywords,
            field = keywordField,
            onFieldChange = {
                val (updated, remainder) = commitKeywordInput(keywords, it)
                keywords = updated
                keywordField = remainder
            },
            onCommit = {
                val (updated, remainder) = commitKeywordInput(keywords, "$keywordField ")
                keywords = updated
                keywordField = remainder
            },
            onRemove = { keywords = keywords - it },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.buying_limits_keywords_helper),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.lg))

        MatchPreview(
            preview = preview,
            timeframe = timeframe,
            onUseSuggestion = { keywords = (keywords + it).distinct(); keywordField = "" },
        )
        Spacer(Modifier.height(MaterialTheme.dimens.lg))

        SavingsSheetLabel(stringResource(R.string.buying_limits_timeframe))
        Spacer(Modifier.height(6.dp))
        SegmentedToggle(
            options = listOf(
                stringResource(R.string.buying_limits_weekly),
                stringResource(R.string.buying_limits_monthly),
            ),
            selectedIndex = if (timeframe == BuyingLimitTimeframe.WEEKLY) 0 else 1,
            onSelect = { timeframe = if (it == 0) BuyingLimitTimeframe.WEEKLY else BuyingLimitTimeframe.MONTHLY },
        )
        Spacer(Modifier.height(MaterialTheme.dimens.lg))

        SavingsSheetLabel(
            stringResource(
                if (timeframe == BuyingLimitTimeframe.WEEKLY) {
                    R.string.buying_limits_per_week
                } else {
                    R.string.buying_limits_per_month
                },
            ),
        )
        Spacer(Modifier.height(6.dp))
        CountStepper(count = count, onChange = { count = it })
    }

    EditorActions(
        canSave = canSave,
        onCancel = onDismiss,
        onSave = {
            val (finalKeywords, _) = commitKeywordInput(keywords, "$keywordField ")
            onSave(emoji, label, finalKeywords, timeframe, count)
        },
    )
}

/** Sheet title + a delete affordance shown only while editing. */
@Composable
private fun EditorHeader(isEditing: Boolean, onDelete: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                if (isEditing) R.string.buying_limits_edit_title else R.string.buying_limits_new_title,
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.buying_limits_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Emoji chip (opens an inline picker) beside the optional Label field. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiAndLabel(
    emoji: String,
    showPicker: Boolean,
    onTogglePicker: () -> Unit,
    onSelectEmoji: (String) -> Unit,
    label: String,
    onLabelChange: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        EmojiPickerChip(emoji = emoji, expanded = showPicker, onClick = onTogglePicker)
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Column(modifier = Modifier.weight(1f)) {
            SavingsSheetLabel(stringResource(R.string.buying_limits_label_optional))
            Spacer(Modifier.height(6.dp))
            CompactTextField(
                value = label,
                onValueChange = onLabelChange,
                hint = stringResource(R.string.buying_limits_label_hint),
            )
        }
    }
    if (showPicker) {
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        EmojiFlow(selected = emoji, onSelect = onSelectEmoji)
    }
}

/**
 * A compact single-line filled field (~48dp) — shorter than the M3 default so it pairs cleanly with the
 * 46dp emoji chip beside it, the way the mockup draws the label row.
 */
@Composable
private fun CompactTextField(value: String, onValueChange: (String) -> Unit, hint: String) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier
            .fillMaxWidth()
            .clip(SavingsFieldShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                inner()
            }
        },
    )
}

/** The muted rounded container the match read-out sits in (mockup: a surfaceContainerLowest card). */
@Composable
private fun PreviewCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SavingsFieldShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(MaterialTheme.dimens.md),
        content = content,
    )
}

/** Outlined Cancel + filled Save, full-width halves, 56dp pills (same pair as the income/goal sheets). */
@Composable
private fun EditorActions(canSave: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.md),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
    ) {
        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(50),
            modifier = Modifier.weight(1f).height(MaterialTheme.dimens.buttonHeight),
        ) {
            Text(stringResource(R.string.action_cancel))
        }
        Button(
            onClick = onSave,
            enabled = canSave,
            shape = RoundedCornerShape(50),
            modifier = Modifier.weight(1f).height(MaterialTheme.dimens.buttonHeight),
        ) {
            Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
        }
    }
}

/** The tappable emoji chip beside the Label field; a dashed tag placeholder when no emoji is set. */
@Composable
private fun EmojiPickerChip(emoji: String, expanded: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(MaterialTheme.dimens.radiusMd)
    val hasEmoji = emoji.isNotEmpty()
    val fill = if (hasEmoji || expanded) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    // Taller than the ~48dp label field beside it and bottom-aligned with it (see the Row above): the
    // chip's top rises up beside the LABEL while its bottom edge lines up flush with the field's bottom.
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(shape)
            .background(fill)
            .then(
                if (hasEmoji) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (hasEmoji) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
        } else {
            Icon(
                Icons.Filled.Sell,
                contentDescription = stringResource(R.string.buying_limits_pick_emoji),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(MaterialTheme.dimens.icon),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiFlow(selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        BUYING_LIMIT_EMOJIS.forEach { e ->
            val isSelected = e == selected
            val bg = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                    .background(bg)
                    .clickable { onSelect(e) },
                contentAlignment = Alignment.Center,
            ) {
                Text(e, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordInput(
    keywords: List<String>,
    field: String,
    onFieldChange: (String) -> Unit,
    onCommit: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SavingsFieldShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(MaterialTheme.dimens.sm),
    ) {
        if (keywords.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            ) {
                keywords.forEach { kw -> KeywordChip(text = kw, onRemove = { onRemove(kw) }) }
            }
        }
        TextField(
            value = field,
            onValueChange = onFieldChange,
            placeholder = { Text(stringResource(R.string.buying_limits_keywords_hint)) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onCommit() }),
            colors = savingsFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A removable keyword pill in the category-emoji-chip colours, with a ✕. Breaks inside long words. */
@Composable
private fun KeywordChip(text: String, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(
                start = MaterialTheme.dimens.md,
                end = MaterialTheme.dimens.xs,
                top = MaterialTheme.dimens.xs,
                bottom = MaterialTheme.dimens.xs,
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Spacer(Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.buying_limits_remove_keyword, text),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** The "CURRENTLY MATCHES" block: matches / no-match / the amber "catching a lot" warning. */
@Composable
private fun MatchPreview(
    preview: BuyingLimitPreview,
    timeframe: BuyingLimitTimeframe,
    onUseSuggestion: (String) -> Unit,
) {
    when (preview.state) {
        BuyingLimitPreview.State.HIDDEN -> Unit
        BuyingLimitPreview.State.NO_MATCH -> PreviewCard {
            PreviewHeader()
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.buying_limits_no_match_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.buying_limits_no_match_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BuyingLimitPreview.State.MATCH ->
            if (preview.tooBroad) {
                TooBroadCard(preview = preview, timeframe = timeframe, onUseSuggestion = onUseSuggestion)
            } else {
                PreviewCard {
                    PreviewHeader()
                    Spacer(Modifier.height(6.dp))
                    MatchNames(preview)
                    Spacer(Modifier.height(2.dp))
                    MatchCount(preview, timeframe)
                }
            }
    }
}

@Composable
private fun PreviewHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.buying_limits_matches_header).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MatchNames(preview: BuyingLimitPreview, color: Color = MaterialTheme.colorScheme.onSurface) {
    val names = preview.names.joinToString(" · ")
    val text = if (preview.moreCount > 0) {
        names + "  " + stringResource(R.string.buying_limits_more, preview.moreCount)
    } else {
        names
    }
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
}

@Composable
private fun MatchCount(
    preview: BuyingLimitPreview,
    timeframe: BuyingLimitTimeframe,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val period = stringResource(
        if (timeframe == BuyingLimitTimeframe.WEEKLY) {
            R.string.buying_limits_this_week
        } else {
            R.string.buying_limits_this_month
        },
    )
    Text(
        text = pluralStringResource(
            R.plurals.buying_limits_bought_this,
            preview.windowQuantity,
            preview.windowQuantity,
            period,
        ),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

/** Amber "catching a lot" warning — a warning only; it never blocks Save. */
@Composable
private fun TooBroadCard(
    preview: BuyingLimitPreview,
    timeframe: BuyingLimitTimeframe,
    onUseSuggestion: (String) -> Unit,
) {
    val warn = budgetWarnColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(warn.copy(alpha = 0.14f))
            .padding(MaterialTheme.dimens.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = warn,
                modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.buying_limits_broad_header).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = warn,
            )
        }
        Spacer(Modifier.height(6.dp))
        MatchNames(preview)
        Spacer(Modifier.height(2.dp))
        MatchCount(preview, timeframe)
        preview.suggestion?.let { suggestion ->
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onUseSuggestion(suggestion) }
                    .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.xs),
            ) {
                Text(
                    text = stringResource(R.string.buying_limits_use_instead, suggestion),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** − [n] + stepper matching the payday field style; − greys out at the floor of 1. */
@Composable
private fun CountStepper(count: Int, onChange: (Int) -> Unit) {
    val canDecrease = count > 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SavingsFieldShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val decreaseTint = if (canDecrease) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        }
        IconButton(onClick = { if (canDecrease) onChange(count - 1) }, enabled = canDecrease) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = stringResource(R.string.buying_limits_decrease),
                tint = decreaseTint,
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onChange(count + 1) }) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.buying_limits_increase),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
