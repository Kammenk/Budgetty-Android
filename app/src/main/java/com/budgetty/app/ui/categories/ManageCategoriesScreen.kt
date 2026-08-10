package com.budgetty.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.ui.components.CategoryEditorScreen
import com.budgetty.app.ui.components.CustomCategoryActions
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.SinglePaneMaxWidth
import com.budgetty.app.ui.util.categoryDisplayName
import com.budgetty.app.ui.util.isExpandedWidth
import org.koin.androidx.compose.koinViewModel

@Composable
fun ManageCategoriesScreen(
    onNavigateBack: () -> Unit,
    onOpenPaywall: () -> Unit,
    viewModel: ManageCategoriesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = CustomCategoryActions(
        categories = state.categories,
        isPremium = state.isPremium,
        onSave = viewModel::saveCustomCategory,
        onDelete = viewModel::deleteCustomCategory,
        onReparent = viewModel::setCategoryParent,
        onCountTransactions = viewModel::transactionCount,
        onOpenPaywall = onOpenPaywall,
    )
    ManageCategoriesContent(
        categories = state.categories,
        isPremium = state.isPremium,
        actions = actions,
        onNavigateBack = onNavigateBack,
        isExpanded = isExpandedWidth(),
    )
}

/** A built-in group and the rows that hang off it: its own built-in sub-categories, and how many of
 *  the user's custom categories live in it (those show in "Your categories", but the count surfaces
 *  the connection). */
private data class GroupView(
    val name: String,
    val subs: List<CategoryEntity>,
    val customCount: Int,
)

/** A category's effective parent — its stored override, else its code-defined group, else null
 *  (top-level). Row-accurate so it never depends on the process cache being in sync. */
private fun effectiveParent(cat: CategoryEntity): String? =
    cat.parent ?: Categories.defaultParentOf(cat.name)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesContent(
    categories: List<CategoryEntity>,
    isPremium: Boolean,
    actions: CustomCategoryActions,
    onNavigateBack: () -> Unit,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val customs = remember(categories) { categories.filter { it.isCustom }.sortedBy { it.createdAt } }
    val order = remember(categories) {
        Categories.predefined.withIndex().associate { (i, c) -> c.name to i }
    }
    val groupViews = remember(categories, customs) {
        Categories.groups.map { g ->
            val subs = categories
                .filter { !it.isCustom && it.name != g.name && effectiveParent(it) == g.name }
                .sortedBy { order[it.name] ?: Int.MAX_VALUE }
            GroupView(g.name, subs, customs.count { effectiveParent(it) == g.name })
        }
    }
    val realGroupCount = remember { Categories.groups.count { it.name != Categories.OTHER } }
    val builtInSubCount = remember(groupViews) { groupViews.sumOf { it.subs.size } }
    val cap = Categories.FREE_CUSTOM_LIMIT
    val atCap = !isPremium && customs.size >= cap

    var editorOpen by remember { mutableStateOf(false) }
    var editorOriginal by remember { mutableStateOf<CategoryEntity?>(null) }
    var movingName by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(setOf<String>()) }

    fun openCreate() {
        if (atCap) actions.onOpenPaywall() else {
            editorOriginal = null
            editorOpen = true
        }
    }
    fun openEdit(cat: CategoryEntity) {
        editorOriginal = cat
        editorOpen = true
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.account_manage_categories), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = (if (isExpanded) Modifier.widthIn(max = SinglePaneMaxWidth) else Modifier.fillMaxWidth())
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 28.dp),
            ) {
                item {
                    SectionHeaderRow(
                        title = stringResource(R.string.manage_your_categories),
                        trailing = "${customs.size} / $cap",
                        accent = true,
                    )
                }
                if (customs.isEmpty()) {
                    item { EmptyYoursHint() }
                } else {
                    items(customs, key = { "cust:${it.name}" }) { cat ->
                        CustomCategoryRow(cat = cat, onEdit = { openEdit(cat) })
                        Spacer(Modifier.size(7.dp))
                    }
                }
                item { CreateCategoryRow(atCap = atCap, onClick = ::openCreate) }
                item { CapHint(isPremium = isPremium, cap = cap) }
                item {
                    SectionHeaderRow(
                        title = stringResource(R.string.manage_builtin),
                        trailing = stringResource(R.string.manage_builtin_summary, realGroupCount, builtInSubCount),
                    )
                }
                builtInGroups(
                    groupViews = groupViews,
                    expanded = expanded,
                    onToggle = { name -> expanded = if (name in expanded) expanded - name else expanded + name },
                    onMove = { movingName = it },
                )
            }
        }
    }

    if (editorOpen) {
        CategoryEditorScreen(
            original = editorOriginal,
            custom = actions,
            onClose = { editorOpen = false },
        )
    }
    movingName?.let { name ->
        MoveToGroupDialog(
            currentGroup = categories.firstOrNull { it.name == name }?.let { effectiveParent(it) },
            onMove = { group ->
                actions.onReparent(name, group)
                movingName = null
            },
            onDismiss = { movingName = null },
        )
    }
}

/** The Built-in section's collapsible group rows and their (indented) sub-category rows. Extracted
 *  from [ManageCategoriesContent] so that function stays within the method-length limit. */
private fun LazyListScope.builtInGroups(
    groupViews: List<GroupView>,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    onMove: (String) -> Unit,
) {
    groupViews.forEach { gv ->
        val isOpen = gv.name in expanded
        item(key = "grp:${gv.name}") {
            GroupRow(group = gv, expanded = isOpen, onToggle = { onToggle(gv.name) })
            Spacer(Modifier.size(7.dp))
        }
        if (isOpen) {
            item(key = "subs:${gv.name}") {
                Column(
                    modifier = Modifier.padding(start = 14.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (gv.subs.isEmpty()) {
                        Text(
                            text = stringResource(R.string.manage_no_subs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 4.dp),
                        )
                    }
                    gv.subs.forEach { sub ->
                        SubCategoryRow(
                            cat = sub,
                            movable = gv.name != Categories.OTHER,
                            onMove = { onMove(sub.name) },
                        )
                    }
                }
                Spacer(Modifier.size(7.dp))
            }
        }
    }
}

/** A colored square with the category's emoji — the shared tile used by every row. */
@Composable
private fun CategoryTile(colorArgb: Int, emoji: String, tile: Int, corner: Int, glyph: Int) {
    Box(
        modifier = Modifier
            .size(tile.dp)
            .clip(RoundedCornerShape(corner.dp))
            .background(Color(colorArgb)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = glyph.sp)
    }
}

/** Uppercase section label + a muted count on the right; [accent] shows the small ✦ premium mark. */
@Composable
private fun SectionHeaderRow(title: String, trailing: String, accent: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (accent) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "✦",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = trailing,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/** A user's custom category: tap anywhere (or the pencil) to edit; its group shows on the sub-line. */
@Composable
private fun CustomCategoryRow(cat: CategoryEntity, onEdit: () -> Unit) {
    val parent = effectiveParent(cat)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onEdit)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        CategoryTile(Categories.colorOf(cat.name), Categories.emojiOf(cat.name), tile = 30, corner = 9, glyph = 16)
        Column(Modifier.weight(1f)) {
            Text(
                text = categoryDisplayName(cat.name),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (parent != null) {
                    stringResource(R.string.manage_in_group, categoryDisplayName(parent))
                } else {
                    stringResource(R.string.manage_top_level)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clickable(onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.cd_edit_category, categoryDisplayName(cat.name)),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

/** A built-in top-level group: tap to expand its sub-categories; the count notes how many are the
 *  user's own ("· N yours"). */
@Composable
private fun GroupRow(group: GroupView, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        CategoryTile(Categories.colorOf(group.name), Categories.emojiOf(group.name), tile = 30, corner = 9, glyph = 16)
        Column(Modifier.weight(1f)) {
            Text(
                text = categoryDisplayName(group.name),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val subCount = pluralStringResource(R.plurals.manage_sub_count, group.subs.size, group.subs.size)
            val yours = if (group.customCount > 0) {
                stringResource(R.string.manage_yours_suffix, group.customCount)
            } else {
                ""
            }
            Text(
                text = subCount + yours,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** An indented built-in sub-category. Its one action is the folder button → the Move dialog. */
@Composable
private fun SubCategoryRow(cat: CategoryEntity, movable: Boolean, onMove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CategoryTile(Categories.colorOf(cat.name), Categories.emojiOf(cat.name), tile = 26, corner = 8, glyph = 14)
        Text(
            text = categoryDisplayName(cat.name),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (movable) {
            IconButton(onClick = onMove, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = stringResource(R.string.manage_move_cd, categoryDisplayName(cat.name)),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** The dashed "Create category" row; at the free cap it turns into a Premium prompt. */
@Composable
private fun CreateCategoryRow(atCap: Boolean, onClick: () -> Unit) {
    val dim = if (atCap) 0.4f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = dim), RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = dim))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp),
            )
        }
        Text(
            text = stringResource(R.string.manage_create_category),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (atCap) {
            Text(
                text = stringResource(R.string.tier_premium),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

/** The free-tier explainer under the Create row. */
@Composable
private fun CapHint(isPremium: Boolean, cap: Int) {
    if (isPremium) return
    Row(modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 12.dp)) {
        Text(
            text = stringResource(R.string.manage_cap_hint, cap) + " · ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.manage_unlimited_premium),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Empty state for a user who hasn't created any custom categories yet. */
@Composable
private fun EmptyYoursHint() {
    Text(
        text = stringResource(R.string.manage_empty_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
    Spacer(Modifier.size(7.dp))
}

/** Centered dialog to re-home a built-in sub-category under another group (its current group is
 *  pre-selected). Mirrors the app's other single-choice dialogs (Theme / Currency). */
@Composable
private fun MoveToGroupDialog(
    currentGroup: String?,
    onMove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val groups = remember { Categories.groups.filter { it.name != Categories.OTHER } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_move_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                groups.forEach { group ->
                    val isSelected = group.name.equals(currentGroup, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onMove(group.name) }
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        RadioButton(selected = isSelected, onClick = { onMove(group.name) })
                        CategoryTile(
                            Categories.colorOf(group.name),
                            Categories.emojiOf(group.name),
                            tile = 26,
                            corner = 8,
                            glyph = 13,
                        )
                        Text(
                            text = categoryDisplayName(group.name),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun ManageCategoriesPreview() {
    val sample = listOf(
        CategoryEntity(
            name = "Coffee",
            colorArgb = 0xFFC8793A.toInt(),
            icon = "☕",
            isCustom = true,
            createdAt = 1,
            parent = "Dining & Entertainment",
        ),
        CategoryEntity(
            name = "Gym",
            colorArgb = 0xFF5BB6A6.toInt(),
            icon = "🏋️",
            isCustom = true,
            createdAt = 2,
            parent = null,
        ),
    )
    BudgettyTheme {
        ManageCategoriesContent(
            categories = sample,
            isPremium = false,
            actions = CustomCategoryActions(categories = sample),
            onNavigateBack = {},
        )
    }
}
