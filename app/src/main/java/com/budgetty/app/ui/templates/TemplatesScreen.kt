package com.budgetty.app.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.data.local.TemplateEntity
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.components.CategoryPickerScreen
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.AppFormats
import com.budgetty.app.ui.util.categoryDisplayName
import com.budgetty.app.ui.util.formatMoney
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal

private val TEMPLATE_EMOJIS =
    listOf("☕", "🥪", "🛒", "🚆", "⛽", "🔑", "🎬", "💊", "📱", "🏋️", "🍺", "🅿️")

/**
 * Manage transaction templates (Account → Templates): the saved regulars listed with their amount and
 * category, each tappable to edit, plus a "New template" row. Free, no cap. Same chrome as the other
 * pushed Account screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplatesViewModel = koinViewModel(),
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    TemplatesContent(
        templates = templates,
        onNavigateBack = onNavigateBack,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemplatesContent(
    templates: List<TemplateEntity>,
    onNavigateBack: () -> Unit,
    onSave: (TemplateEntity) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editorFor by remember { mutableStateOf<TemplateEntity?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.templates_title), fontWeight = FontWeight.Bold) },
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
            Column(modifier = Modifier.widthIn(max = 520.dp).fillMaxSize()) {
                Text(
                    text = stringResource(R.string.templates_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.dimens.xl,
                        vertical = MaterialTheme.dimens.sm,
                    ),
                )
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = MaterialTheme.dimens.lg,
                        end = MaterialTheme.dimens.lg,
                        bottom = MaterialTheme.dimens.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
                ) {
                    items(templates, key = { it.id }) { template ->
                        TemplateManageRow(template = template, onClick = { editorFor = template })
                    }
                    item(key = "new") {
                        NewTemplateRow(onClick = { editorFor = TemplateEntity(amount = BigDecimal.ZERO) })
                    }
                }
            }
        }
    }

    editorFor?.let { target ->
        TemplateEditorSheet(
            initial = target,
            isEditing = target.id != 0L,
            onSave = { onSave(it); editorFor = null },
            onDelete = if (target.id != 0L) { { onDelete(target.id); editorFor = null } } else null,
            onDismiss = { editorFor = null },
        )
    }
}

@Composable
private fun TemplateManageRow(template: TemplateEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(MaterialTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiTile(template.emoji)
        Spacer(Modifier.size(MaterialTheme.dimens.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template.name.ifBlank { stringResource(R.string.templates_untitled) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = templateSubtitle(template),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NewTemplateRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .clickable(onClick = onClick)
            .padding(MaterialTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.size(MaterialTheme.dimens.md))
        Text(
            text = stringResource(R.string.templates_new),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmojiTile(emoji: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji.ifBlank { "🧾" }, fontSize = 20.sp)
    }
}

@Composable
private fun templateSubtitle(template: TemplateEntity): String {
    val amount = if (template.askAmount) {
        stringResource(R.string.templates_ask_amount_short)
    } else {
        template.amount.formatMoney()
    }
    val category = template.category.takeIf { it.isNotBlank() }?.let { categoryDisplayName(it) }
    return listOfNotNull(amount, category).joinToString(" · ")
}

// ── Editor sheet ────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("CyclomaticComplexMethod")
@Composable
private fun TemplateEditorSheet(
    initial: TemplateEntity,
    isEditing: Boolean,
    onSave: (TemplateEntity) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var emoji by remember { mutableStateOf(initial.emoji.ifBlank { TEMPLATE_EMOJIS.first() }) }
    var name by remember { mutableStateOf(initial.name) }
    var amountText by remember {
        mutableStateOf(initial.amount.takeIf { it.signum() > 0 }?.toPlainString() ?: "")
    }
    var category by remember { mutableStateOf(initial.category) }
    var store by remember { mutableStateOf(initial.store) }
    var askAmount by remember { mutableStateOf(initial.askAmount) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val amount = amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val valid = name.isNotBlank() && (askAmount || amount.signum() > 0)

    AdaptiveSheet(onDismiss = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (isEditing) R.string.templates_edit_title else R.string.templates_add_title,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            EmojiRow(selected = emoji, onSelect = { emoji = it })
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.templates_field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(stringResource(R.string.templates_field_amount)) },
                suffix = { Text(AppFormats.currencySymbol) },
                singleLine = true,
                enabled = !askAmount,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            CategoryField(category = category, onClick = { showCategoryPicker = true })
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            OutlinedTextField(
                value = store,
                onValueChange = { store = it },
                label = { Text(stringResource(R.string.templates_field_store)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.templates_ask_amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.templates_ask_amount_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = askAmount, onCheckedChange = { askAmount = it })
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Text(
                text = stringResource(R.string.templates_footer_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            androidx.compose.material3.Button(
                onClick = {
                    onSave(
                        initial.copy(
                            emoji = emoji,
                            name = name.trim(),
                            amount = if (askAmount) BigDecimal.ZERO else amount,
                            category = category,
                            store = store.trim(),
                            askAmount = askAmount,
                        ),
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(if (isEditing) R.string.action_save else R.string.templates_save)) }
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
        }
    }

    if (showCategoryPicker) {
        CategoryPickerScreen(
            selected = category,
            onSelect = { category = it },
            onDismiss = { showCategoryPicker = false },
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EmojiRow(selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs),
    ) {
        TEMPLATE_EMOJIS.forEach { e ->
            val isSelected = e == selected
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusSm))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                    .clickable { onSelect(e) },
                contentAlignment = Alignment.Center,
            ) { Text(e, fontSize = 18.sp) }
        }
    }
}

@Composable
private fun CategoryField(category: String, onClick: () -> Unit) {
    Box {
        OutlinedTextField(
            value = if (category.isBlank()) "" else categoryDisplayName(category),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(stringResource(R.string.templates_field_category)) },
            leadingIcon = if (category.isNotBlank()) {
                { Text(Categories.emojiOf(category), fontSize = 18.sp) }
            } else {
                null
            },
            placeholder = { Text(stringResource(R.string.templates_pick_category)) },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(modifier = Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

@Preview(showBackground = true)
@Composable
private fun TemplatesScreenPreview() {
    BudgettyTheme {
        TemplatesContent(
            templates = listOf(
                TemplateEntity(1, "☕", "Coffee", BigDecimal("3.20"), "Food", "Café Delta"),
                TemplateEntity(2, "🚆", "Metro", BigDecimal("1.60"), "Transport", "Metro"),
                TemplateEntity(3, "🛒", "Groceries", BigDecimal.ZERO, "Groceries", "", askAmount = true),
            ),
            onNavigateBack = {},
            onSave = {},
            onDelete = {},
        )
    }
}
