package com.budgetty.app.ui.components

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.category.EmojiCatalog
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.util.categoryDisplayName

/**
 * The VM-backed hooks the picker needs to list, create, edit, and delete the user's custom
 * categories, and to reach the paywall. Bundled so the review screen threads a single param down to
 * each row's [CategoryField]. Defaults are inert, for previews and non-custom callers.
 */
data class CustomCategoryActions(
    val categories: List<CategoryEntity> = emptyList(),
    val isPremium: Boolean = false,
    val onSave: (original: String?, name: String, icon: String, colorArgb: Int, parent: String?) -> Unit =
        { _, _, _, _, _ -> },
    val onDelete: (name: String) -> Unit = {},
    /** Re-homes [name] under a parent (null = top-level) — the picker's "Move to group" / re-parent. */
    val onReparent: (name: String, parent: String?) -> Unit = { _, _ -> },
    val onCountTransactions: suspend (String) -> Int = { 0 },
    val onOpenPaywall: () -> Unit = {},
)

/** The picker's two in-place modes: browsing/picking, or the create/edit form ([Edit.original] is
 *  null when creating a new category). */
private sealed interface PickerMode {
    data object Pick : PickerMode
    data class Edit(val original: CategoryEntity?) : PickerMode
}

/**
 * A grouped, searchable category picker shown as a full-screen surface. It is presented in a
 * full-bleed [Dialog] so it can cover whatever opened it (a form field, or another bottom sheet)
 * rather than stacking as yet another sheet. Its top "Your categories" section holds the user's
 * custom categories plus a Create tile; below it the predefined groups follow as three-up icon-card
 * grids. Picking a card or header calls [onSelect] and closes the screen. The Create tile (or a
 * custom tile's edit badge) swaps the screen in place to a create/edit form; the app bar's back
 * arrow returns to the grid, and a second back closes the screen. Free users may create
 * [Categories.FREE_CUSTOM_LIMIT] custom categories, Premium unlimited; at the free cap the Create
 * tile becomes a paywall prompt. On tablets the content is width-capped and centered.
 */
@Composable
fun CategoryPickerScreen(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    custom: CustomCategoryActions = CustomCategoryActions(),
) {
    var mode by remember { mutableStateOf<PickerMode>(PickerMode.Pick) }

    // Back — from the app bar or the system gesture: leave the create/edit form for the grid, or,
    // when already on the grid, close the whole screen.
    val onBack: () -> Unit = {
        when (mode) {
            is PickerMode.Edit -> mode = PickerMode.Pick
            PickerMode.Pick -> onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        CategoryPickerContent(
            selected = selected,
            custom = custom,
            mode = mode,
            onModeChange = { mode = it },
            selectAndClose = { onSelect(it); onDismiss() },
            onBack = onBack,
            onClose = onDismiss,
        )
    }
}

/**
 * The create/edit form on its own, as a full-screen dialog — the **Manage categories** screen opens
 * this to create a new custom category ([original] == null) or edit an existing one, reusing the
 * picker's exact form (preview, name, colour, emoji grid, parent group) and its Save/Delete flow.
 * [onClose] fires after a save or delete, or on back / dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorScreen(
    original: CategoryEntity?,
    custom: CustomCategoryActions,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    val titleRes = if (original == null) R.string.custom_new_title else R.string.custom_edit_title
                    TopAppBar(
                        title = { Text(stringResource(titleRes), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxSize(),
                    ) {
                        CreateEditContent(
                            original = original,
                            custom = custom,
                            onSaved = { onClose() },
                            onDeleted = onClose,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerContent(
    selected: String,
    custom: CustomCategoryActions,
    mode: PickerMode,
    onModeChange: (PickerMode) -> Unit,
    selectAndClose: (String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val titleRes = when (val m = mode) {
        is PickerMode.Edit -> if (m.original == null) R.string.custom_new_title else R.string.custom_edit_title
        PickerMode.Pick -> R.string.category_picker_title
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(titleRes), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxSize(),
                ) {
                    when (val m = mode) {
                        is PickerMode.Pick -> PickContent(
                            selected = selected,
                            custom = custom,
                            onSelect = selectAndClose,
                            onCreate = { onModeChange(PickerMode.Edit(null)) },
                            onEdit = { onModeChange(PickerMode.Edit(it)) },
                        )

                        is PickerMode.Edit -> CreateEditContent(
                            original = m.original,
                            custom = custom,
                            onSaved = selectAndClose,
                            onDeleted = onClose,
                        )
                    }
                }
            }
        }
    }
}

// ── Pick mode ──────────────────────────────────────────────────────────────────────────────────

/** A category row's effective parent: its stored override, else the code-defined group, else null
 *  (top-level). Row-accurate, so it doesn't depend on the process cache being in sync. */
private fun effectiveParentOf(cat: CategoryEntity): String? =
    cat.parent ?: Categories.defaultParentOf(cat.name)

/** The picker's two-level tree: the user's top-level custom categories, and — per top-level name —
 *  its effective children (built-ins in code order first, then re-homed-in and custom children). */
private data class CategoryTree(
    val customTopLevel: List<CategoryEntity>,
    val childrenByParent: Map<String, List<CategoryEntity>>,
)

private fun buildCategoryTree(all: List<CategoryEntity>): CategoryTree {
    val childrenByParent = all
        .mapNotNull { cat -> effectiveParentOf(cat)?.let { parent -> parent to cat } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (parent, kids) ->
            val codeOrder = Categories.children(parent).map { it.name }
            kids.sortedWith(
                compareBy(
                    { if (it.isCustom) 1 else 0 },
                    { codeOrder.indexOf(it.name).let { i -> if (i < 0) Int.MAX_VALUE else i } },
                    { it.createdAt },
                    { it.name },
                ),
            )
        }
    val customTopLevel = all
        .filter { it.isCustom && effectiveParentOf(it) == null }
        .sortedBy { it.createdAt }
    return CategoryTree(customTopLevel, childrenByParent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.PickContent(
    selected: String,
    custom: CustomCategoryActions,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (CategoryEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val context = LocalContext.current
    val customCats = remember(custom.categories) {
        custom.categories.filter { it.isCustom }.sortedBy { it.createdAt }
    }
    val cap = if (custom.isPremium) Categories.MAX_CUSTOM_LIMIT else Categories.FREE_CUSTOM_LIMIT
    // The two-level tree (primaries + effective children) and the built-in currently being re-homed.
    val tree = remember(custom.categories) { buildCategoryTree(custom.categories) }
    var reparenting by remember { mutableStateOf<CategoryEntity?>(null) }

    TextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text(stringResource(R.string.action_search)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(percent = 50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.lg),
    )
    Spacer(Modifier.height(MaterialTheme.dimens.md))
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = MaterialTheme.dimens.md),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(bottom = MaterialTheme.dimens.xxl),
    ) {
        if (q.isBlank()) {
            hierarchyItems(
                tree = tree,
                customCats = customCats,
                cap = cap,
                isPremium = custom.isPremium,
                selected = selected,
                onSelect = onSelect,
                onCreate = onCreate,
                onEdit = onEdit,
                onReparent = { reparenting = it },
                onOpenPaywall = custom.onOpenPaywall,
            )
        } else {
            val customMatches = customCats.filter { it.name.contains(q, ignoreCase = true) }
            val matches = Categories.predefined.filter {
                it.name.contains(q, ignoreCase = true) ||
                    categoryDisplayName(context, it.name).contains(q, ignoreCase = true)
            }
            items(customMatches, key = { "custom_${it.name}" }) { cat ->
                CategoryCard(
                    emoji = cat.icon,
                    name = cat.name,
                    color = Color(cat.colorArgb),
                    selected = cat.name.equals(selected, ignoreCase = true),
                    onClick = { onSelect(cat.name) },
                    onEdit = { onEdit(cat) },
                )
            }
            items(matches, key = { it.name }) { cat ->
                CategoryCard(
                    emoji = cat.emoji,
                    name = categoryDisplayName(cat.name),
                    color = Color(cat.colorArgb),
                    selected = cat.name.equals(selected, ignoreCase = true),
                    onClick = { onSelect(cat.name) },
                )
            }
        }
    }

    // Re-homing a built-in child (long-press → "Move to group"): pick its new parent.
    reparenting?.let { target ->
        ParentPickerDialog(
            currentParent = effectiveParentOf(target),
            excludeName = target.name,
            categories = custom.categories,
            onPick = { newParent ->
                custom.onReparent(target.name, newParent)
                reparenting = null
            },
            onDismiss = { reparenting = null },
        )
    }
}

/**
 * The "Your categories" section header. Mirrors [CategoryGroupHeader]'s proportions (a 44dp chip and
 * a large title) so it sits consistently above the predefined groups, with a "used of cap" counter
 * trailing on the right.
 */
@Composable
private fun YourCategoriesHeader(used: Int, cap: Int, unlimited: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = MaterialTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(33.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text("✦", color = MaterialTheme.colorScheme.onPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Text(
            text = stringResource(R.string.custom_your_categories),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Bottom),
        )
        Text(
            // Premium is unlimited, so just show the count (a huge "of N" cap would be meaningless).
            text = if (unlimited) used.toString() else stringResource(R.string.custom_count, used, cap),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Bottom),
        )
    }
}

/** The first tile in "Your categories": Create when under the cap, else a paywall prompt (free) or a
 *  disabled "Maximum reached" (Premium at the max). */
@Composable
private fun CreateTile(
    canCreate: Boolean,
    isPremium: Boolean,
    onCreate: () -> Unit,
    onOpenPaywall: () -> Unit,
) {
    when {
        canCreate -> CreateTileFrame(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            bordered = true,
            onClick = onCreate,
        ) {
            TileGlyph(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            TileLabel(stringResource(R.string.custom_create), MaterialTheme.colorScheme.primary)
        }

        !isPremium -> CreateTileFrame(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            bordered = false,
            onClick = onOpenPaywall,
        ) {
            TileGlyph(MaterialTheme.colorScheme.surfaceContainerLow) {
                Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
            TileLabel(stringResource(R.string.custom_unlock_more), MaterialTheme.colorScheme.primary)
        }

        else -> CreateTileFrame(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f),
            bordered = false,
            onClick = null,
        ) {
            TileGlyph(MaterialTheme.colorScheme.surfaceContainerHighest) {
                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
            }
            TileLabel(stringResource(R.string.custom_max_reached), MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreateTileFrame(
    containerColor: Color,
    bordered: Boolean,
    onClick: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .then(
                if (bordered) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                } else {
                    Modifier
                },
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        content = content,
    )
}

@Composable
private fun TileGlyph(background: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .background(background),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun TileLabel(text: String, color: Color) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.basicMarquee(),
        )
    }
}

/**
 * A full-width section header introducing one predefined group: a colour chip with the group's emoji
 * and its name. Tapping it picks the umbrella group, so it carries the same selected treatment
 * (tinted background + trailing check) as the cards.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryGroupHeader(
    emoji: String,
    name: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier.clickable(onClick = onClick)
                },
            )
            .padding(horizontal = 6.dp, vertical = MaterialTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(33.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Text(
            text = name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Bottom),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.cd_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
            )
        }
    }
}

/**
 * One category in the grid: a rounded icon tile over its name. The selected card gains a purple
 * outline, a tinted background, and a check badge. Custom categories ([onEdit] non-null) show a
 * pencil badge when unselected, and can be edited by long-pressing.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryCard(
    emoji: String,
    name: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onMove: (() -> Unit)? = null,
) {
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                )
                .then(
                    if (selected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                    } else {
                        Modifier
                    },
                )
                .then(
                    // Long-press edits a custom category, or (for a built-in) re-homes it into a group.
                    when {
                        onEdit != null -> Modifier.combinedClickable(onClick = onClick, onLongClick = onEdit)
                        onMove != null -> Modifier.combinedClickable(onClick = onClick, onLongClick = onMove)
                        else -> Modifier.clickable(onClick = onClick)
                    },
                )
                .padding(horizontal = 6.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 21.sp)
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.basicMarquee(),
                )
            }
        }
        if (selected) {
            CornerBadge(MaterialTheme.colorScheme.primary) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(10.dp),
                )
            }
        } else if (onEdit != null) {
            CornerBadge(MaterialTheme.colorScheme.secondaryContainer, onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.cd_edit_category, name),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(9.dp),
                )
            }
        }
    }
}

/** A 16dp round badge pinned to a card's top-right corner. */
@Composable
private fun BoxScope.CornerBadge(color: Color, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(5.dp)
            .size(MaterialTheme.dimens.lg)
            .clip(CircleShape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

// ── Create / edit mode ─────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.CreateEditContent(
    original: CategoryEntity?,
    custom: CustomCategoryActions,
    onSaved: (String) -> Unit,
    onDeleted: () -> Unit,
) {
    val key = original?.name
    var name by remember(key) { mutableStateOf(original?.name ?: "") }
    var color by remember(key) { mutableStateOf(original?.colorArgb ?: Categories.defaultColor) }
    var icon by remember(key) { mutableStateOf(original?.icon ?: "") }
    var iconQuery by remember(key) { mutableStateOf("") }
    var parent by remember(key) { mutableStateOf(original?.parent) }
    var showParentPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember(key) { mutableStateOf(false) }

    val trimmed = name.trim()
    val duplicate = remember(name, custom.categories) {
        val candidate = trimmed.lowercase()
        candidate.isNotEmpty() &&
            candidate != original?.name?.trim()?.lowercase() &&
            // Predefined names and existing customs are taken; an orphaned "ghost" row (non-predefined,
            // isCustom=0) is reclaimable, so it must not flag the name as a duplicate here either —
            // kept in lockstep with UploadViewModel.isDuplicateName.
            custom.categories.any {
                it.name.trim().lowercase() == candidate && (it.isCustom || Categories.isPredefined(it.name))
            }
    }
    val canSave = trimmed.isNotEmpty() && icon.isNotEmpty() && !duplicate

    // Pinned: live preview, name, colour, then the icon search — only the sectioned icon grid below
    // scrolls, so the search field stays reachable while browsing 200+ icons.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.lg),
    ) {
        PreviewTile(icon = icon, color = Color(color), name = trimmed)
        NameField(
            value = name,
            onValueChange = { name = it },
            isError = duplicate,
        )
        SectionLabel(stringResource(R.string.custom_color_label))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Categories.palette.forEach { swatch ->
                ColorSwatch(
                    color = Color(swatch),
                    selected = swatch == color,
                    onClick = { color = swatch },
                )
            }
        }
        SectionLabel(stringResource(R.string.custom_parent_label))
        ParentField(parentName = parent, onClick = { showParentPicker = true })
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        IconSearchField(value = iconQuery, onValueChange = { iconQuery = it })
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
    }

    // Blank query → the full sectioned pool; otherwise the keyword matches (exact-token first).
    val iconMatches = remember(iconQuery) {
        iconQuery.trim().takeIf { it.isNotEmpty() }?.let { EmojiCatalog.search(it) }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = MaterialTheme.dimens.lg),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(top = MaterialTheme.dimens.sm, bottom = MaterialTheme.dimens.sm),
    ) {
        iconChoiceGrid(
            query = iconQuery.trim(),
            matches = iconMatches,
            color = color,
            selectedIcon = icon,
            onPick = { icon = it },
        )
    }

    CreateEditFooter(
        isEditing = original != null,
        canSave = canSave,
        onSave = { custom.onSave(original?.name, trimmed, icon, color, parent); onSaved(trimmed) },
        onDelete = { showDeleteConfirm = true },
    )

    if (showDeleteConfirm && original != null) {
        DeleteConfirmDialog(
            categoryName = original.name,
            countProvider = custom.onCountTransactions,
            onConfirm = { custom.onDelete(original.name); onDeleted() },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (showParentPicker) {
        ParentPickerDialog(
            currentParent = parent,
            excludeName = original?.name,
            categories = custom.categories,
            onPick = { parent = it; showParentPicker = false },
            onDismiss = { showParentPicker = false },
        )
    }
}

/** The live preview of the category being built: its icon on the chosen color, with the name below. */
@Composable
private fun PreviewTile(icon: String, color: Color, name: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.dimens.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (icon.isNotEmpty()) Text(icon, fontSize = 30.sp)
        }
        Text(
            text = name.ifEmpty { stringResource(R.string.custom_new_title) },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (name.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit, isError: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(stringResource(R.string.custom_name_hint)) },
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(percent = 50),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                // Clearly-muted placeholder so the name hint reads as a hint, not as filled-in text.
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (isError) {
            Text(
                text = stringResource(R.string.custom_name_taken),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = MaterialTheme.dimens.lg, top = 6.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.7.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(MaterialTheme.dimens.avatar),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(MaterialTheme.dimens.avatar)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
        )
    }
}

@Composable
private fun IconTile(emoji: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                } else {
                    Modifier
                },
            )
            .padding(if (selected) 3.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 24.sp)
        }
    }
}

/** The "Pick" grid's hierarchy content: the "Your categories" header + Create tile, the user's custom
 *  top-level categories (a primary as a header + its children, a childless one as a card), then each
 *  predefined group with its effective children (re-homes included). */
private fun LazyGridScope.hierarchyItems(
    tree: CategoryTree,
    customCats: List<CategoryEntity>,
    cap: Int,
    isPremium: Boolean,
    selected: String,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (CategoryEntity) -> Unit,
    onReparent: (CategoryEntity) -> Unit,
    onOpenPaywall: () -> Unit,
) {
    item(key = "your_header", span = { GridItemSpan(maxLineSpan) }) {
        YourCategoriesHeader(used = customCats.size, cap = cap, unlimited = isPremium)
    }
    item(key = "create_tile") {
        CreateTile(
            canCreate = customCats.size < cap,
            isPremium = isPremium,
            onCreate = onCreate,
            onOpenPaywall = onOpenPaywall,
        )
    }
    tree.customTopLevel.forEach { primary ->
        val kids = tree.childrenByParent[primary.name].orEmpty()
        if (kids.isEmpty()) {
            item(key = "custom_${primary.name}") {
                CategoryCard(
                    emoji = primary.icon,
                    name = primary.name,
                    color = Color(primary.colorArgb),
                    selected = primary.name.equals(selected, ignoreCase = true),
                    onClick = { onSelect(primary.name) },
                    onEdit = { onEdit(primary) },
                )
            }
        } else {
            item(key = "customhdr_${primary.name}", span = { GridItemSpan(maxLineSpan) }) {
                CategoryGroupHeader(
                    emoji = primary.icon,
                    name = primary.name,
                    color = Color(primary.colorArgb),
                    selected = primary.name.equals(selected, ignoreCase = true),
                    onClick = { onSelect(primary.name) },
                    onLongClick = { onEdit(primary) },
                )
            }
            items(kids, key = { "cc_${it.name}" }) { child ->
                HierarchyChildCard(child, selected, onSelect, onEdit) { onReparent(child) }
            }
        }
    }
    Categories.groups.forEach { group ->
        item(key = "g_${group.name}", span = { GridItemSpan(maxLineSpan) }) {
            CategoryGroupHeader(
                emoji = group.emoji,
                name = categoryDisplayName(group.name),
                color = Color(group.colorArgb),
                selected = group.name.equals(selected, ignoreCase = true),
                onClick = { onSelect(group.name) },
            )
        }
        items(tree.childrenByParent[group.name].orEmpty(), key = { "c_${it.name}" }) { child ->
            HierarchyChildCard(child, selected, onSelect, onEdit) { onReparent(child) }
        }
    }
}

/** The create/edit icon grid content: the sectioned emoji pool, keyword matches, or a no-results state. */
private fun LazyGridScope.iconChoiceGrid(
    query: String,
    matches: List<String>?,
    color: Int,
    selectedIcon: String,
    onPick: (String) -> Unit,
) {
    when {
        matches == null -> EmojiCatalog.sections.forEach { section ->
            item(key = "sec_${section.title}", span = { GridItemSpan(maxLineSpan) }) {
                EmojiSectionHeader(title = section.title, count = section.entries.size)
            }
            items(section.entries, key = { it.emoji }) { entry ->
                IconTile(
                    emoji = entry.emoji,
                    color = Color(color),
                    selected = entry.emoji == selectedIcon,
                    onClick = { onPick(entry.emoji) },
                )
            }
        }

        matches.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
            NoIconMatches(query = query)
        }

        else -> items(matches, key = { it }) { emoji ->
            IconTile(
                emoji = emoji,
                color = Color(color),
                selected = emoji == selectedIcon,
                onClick = { onPick(emoji) },
            )
        }
    }
}

/** The create/edit footer: a full-width Save, plus a destructive Delete when editing. */
@Composable
private fun CreateEditFooter(
    isEditing: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = MaterialTheme.dimens.sm, bottom = MaterialTheme.dimens.lg),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.dimens.buttonHeight),
        ) {
            Text(
                text = stringResource(R.string.action_save),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (isEditing) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.dimens.buttonHeight),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                Text(
                    text = stringResource(R.string.custom_delete),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * One child card in the hierarchy grid. A custom child is editable (long-press opens its form, which
 * carries the Parent selector); a built-in child is re-homeable (long-press → "Move to group").
 */
@Composable
private fun HierarchyChildCard(
    child: CategoryEntity,
    selected: String,
    onSelect: (String) -> Unit,
    onEdit: (CategoryEntity) -> Unit,
    onMove: () -> Unit,
) {
    CategoryCard(
        emoji = child.icon.ifEmpty { Categories.emojiOf(child.name) },
        name = if (child.isCustom) child.name else categoryDisplayName(child.name),
        color = Color(child.colorArgb),
        selected = child.name.equals(selected, ignoreCase = true),
        onClick = { onSelect(child.name) },
        onEdit = if (child.isCustom) ({ onEdit(child) }) else null,
        onMove = if (child.isCustom) null else onMove,
    )
}

/** The "Parent category" selector row in create/edit: shows the chosen parent (or "None"), and opens
 *  the parent picker on tap. */
@Composable
private fun ParentField(parentName: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (parentName == null) {
            Text(
                text = stringResource(R.string.custom_parent_none),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(text = Categories.emojiOf(parentName), fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = categoryDisplayName(parentName),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Dialog to choose a parent: "None (top-level)" plus every top-level category (built-in groups +
 * custom primaries) except [excludeName] — a sub-category can't be a parent, and nothing can parent
 * itself. Used by both the create/edit Parent selector and the built-in "Move to group" flow.
 */
@Composable
private fun ParentPickerDialog(
    currentParent: String?,
    excludeName: String?,
    categories: List<CategoryEntity>,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val customPrimaries = remember(categories, excludeName) {
        categories.filter {
            it.isCustom && effectiveParentOf(it) == null && !it.name.equals(excludeName, ignoreCase = true)
        }.sortedBy { it.createdAt }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(vertical = MaterialTheme.dimens.lg)) {
                Text(
                    text = stringResource(R.string.custom_parent_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    ParentOptionRow(
                        emoji = "",
                        label = stringResource(R.string.custom_parent_none),
                        selected = currentParent == null,
                        onClick = { onPick(null) },
                    )
                    Categories.groups
                        .filter { !it.name.equals(excludeName, ignoreCase = true) }
                        .forEach { group ->
                            ParentOptionRow(
                                emoji = group.emoji,
                                label = categoryDisplayName(group.name),
                                selected = group.name.equals(currentParent, ignoreCase = true),
                                onClick = { onPick(group.name) },
                            )
                        }
                    customPrimaries.forEach { primary ->
                        ParentOptionRow(
                            emoji = primary.icon,
                            label = primary.name,
                            selected = primary.name.equals(currentParent, ignoreCase = true),
                            onClick = { onPick(primary.name) },
                        )
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 12.dp),
                ) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    }
}

@Composable
private fun ParentOptionRow(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (emoji.isNotEmpty()) {
            Text(text = emoji, fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
            )
        }
    }
}

/** The pinned search field at the top of the icon area: filters the whole emoji pool by keyword. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconSearchField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(stringResource(R.string.custom_icon_search)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(percent = 50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A section divider in the icon grid: the section title, its icon count, and a trailing rule. */
@Composable
private fun EmojiSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.dimens.sm, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Spacer(Modifier.width(MaterialTheme.dimens.sm))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(MaterialTheme.dimens.hairline)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

/** Empty-search state in the icon grid: a quiet "nothing matched" line plus a recovery hint. */
@Composable
private fun NoIconMatches(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.dimens.xl, start = MaterialTheme.dimens.md, end = MaterialTheme.dimens.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.custom_icon_no_match, query),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.custom_icon_no_match_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    categoryName: String,
    countProvider: suspend (String) -> Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val count by produceState(initialValue = 0, categoryName) { value = countProvider(categoryName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_delete_confirm_title, categoryName), fontWeight = FontWeight.Bold) },
        text = { Text(pluralStringResource(R.plurals.custom_delete_confirm_body, count, count)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Preview
@Composable
private fun CategoryPickerContentPreview() {
    BudgettyTheme {
        CategoryPickerContent(
            selected = "Groceries",
            custom = CustomCategoryActions(),
            mode = PickerMode.Pick,
            onModeChange = {},
            selectAndClose = {},
            onBack = {},
            onClose = {},
        )
    }
}
