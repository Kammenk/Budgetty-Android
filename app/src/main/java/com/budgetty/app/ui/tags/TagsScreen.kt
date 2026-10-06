package com.budgetty.app.ui.tags

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.dimens
import org.koin.androidx.compose.koinViewModel

/**
 * Account → Tags: every free-form tag with its transaction count, and a per-row overflow menu to
 * Rename, Merge into another tag, or Delete it. Renaming/merging re-tags every matching transaction;
 * deleting a tag only removes the tag, never the transactions. Tags are created inline from Review &
 * Edit, so this screen is manage-only.
 */
@Composable
fun TagsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TagsContent(
        tags = uiState.tags,
        isLoaded = uiState.isLoaded,
        onRename = viewModel::rename,
        onMerge = viewModel::merge,
        onDelete = viewModel::delete,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagsContent(
    tags: List<TagRow>,
    isLoaded: Boolean,
    onRename: (from: String, to: String) -> Unit,
    onMerge: (from: String, into: String) -> Unit,
    onDelete: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var renaming by remember { mutableStateOf<String?>(null) }
    var merging by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<TagRow?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tags_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (tags.isNotEmpty()) {
                        Text(
                            text = pluralStringResource(R.plurals.tags_count, tags.size, tags.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = MaterialTheme.dimens.lg),
                        )
                    }
                },
                // Edge-to-edge: the nav Scaffold already applies the status-bar inset.
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(modifier = Modifier.widthIn(max = 520.dp).fillMaxSize()) {
                when {
                    !isLoaded -> Unit
                    tags.isEmpty() -> TagsEmpty(modifier = Modifier.weight(1f).fillMaxWidth())
                    else -> LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = MaterialTheme.dimens.lg,
                            end = MaterialTheme.dimens.lg,
                            top = MaterialTheme.dimens.sm,
                            bottom = MaterialTheme.dimens.xxl,
                        ),
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                tags.forEachIndexed { index, tag ->
                                    if (index > 0) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                    TagManageRow(
                                        tag = tag,
                                        onRename = { renaming = tag.name },
                                        onMerge = { merging = tag.name },
                                        onDelete = { deleting = tag },
                                    )
                                }
                            }
                        }
                        item {
                            Text(
                                text = stringResource(R.string.tags_footer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, start = 4.dp, end = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    renaming?.let { from ->
        RenameTagDialog(
            from = from,
            onConfirm = { to -> onRename(from, to); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    merging?.let { from ->
        MergeTagDialog(
            from = from,
            others = tags.filter { it.name != from },
            onConfirm = { into -> onMerge(from, into); merging = null },
            onDismiss = { merging = null },
        )
    }
    deleting?.let { tag ->
        DeleteTagDialog(
            tag = tag,
            onConfirm = { onDelete(tag.name); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

/** "#name" with the hash tinted in the primary colour, matching the tag pills elsewhere. */
@Composable
private fun hashName(name: String) = buildAnnotatedString {
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("#") }
    append(name)
}

@Composable
private fun TagManageRow(
    tag: TagRow,
    onRename: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = hashName(tag.name),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pluralStringResource(R.plurals.tags_transaction_count, tag.count, tag.count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.tags_row_actions, tag.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tag_action_rename)) },
                    onClick = { menuOpen = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tag_action_merge)) },
                    onClick = { menuOpen = false; onMerge() },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.tag_action_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameTagDialog(from: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(from) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tag_rename_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    prefix = { Text("#", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                )
                Spacer(Modifier.height(MaterialTheme.dimens.sm))
                Text(
                    text = stringResource(R.string.tag_rename_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun MergeTagDialog(
    from: String,
    others: List<TagRow>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var target by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tag_merge_title, from), fontWeight = FontWeight.Bold) },
        text = {
            if (others.isEmpty()) {
                Text(
                    stringResource(R.string.tag_merge_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column {
                    Text(
                        text = stringResource(R.string.tag_merge_body, from),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(MaterialTheme.dimens.sm))
                    Column(
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        others.forEach { other ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = target == other.name,
                                        onClick = { target = other.name },
                                    )
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = target == other.name,
                                    onClick = { target = other.name },
                                )
                                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                                Text(
                                    text = hashName(other.name),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = other.count.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { target?.let(onConfirm) },
                enabled = target != null,
            ) { Text(stringResource(R.string.tag_merge_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun DeleteTagDialog(tag: TagRow, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tag_delete_title, tag.name), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.tag_delete_body)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun TagsEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = MaterialTheme.dimens.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Sell,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(MaterialTheme.dimens.touchTarget),
        )
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Text(
            text = stringResource(R.string.tags_empty),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            text = stringResource(R.string.tags_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun TagsPreview() {
    BudgettyTheme {
        TagsContent(
            tags = listOf(
                TagRow("lisbon-2026", 14),
                TagRow("work", 9),
                TagRow("reimbursable", 6),
                TagRow("дача", 2),
            ),
            isLoaded = true,
            onRename = { _, _ -> },
            onMerge = { _, _ -> },
            onDelete = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun TagsEmptyPreview() {
    BudgettyTheme {
        TagsContent(
            tags = emptyList(),
            isLoaded = true,
            onRename = { _, _ -> },
            onMerge = { _, _ -> },
            onDelete = {},
            onNavigateBack = {},
        )
    }
}
