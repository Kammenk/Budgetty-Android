package com.budgetty.app.ui.rules

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.ui.theme.BudgettyTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Account → Category rules: manage the learned "item name → category" rules that auto-file items on
 * scan. Each row shows the item, its target category, and how many saved transactions it matches,
 * with a delete action to forget it. Rules are only created inline (from Review & Edit); this screen
 * is view + delete.
 */
@Composable
fun CategoryRulesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryRulesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CategoryRulesContent(
        rules = uiState.rules,
        isLoaded = uiState.isLoaded,
        onDeleteRule = viewModel::deleteRule,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryRulesContent(
    rules: List<CategoryRuleRow>,
    isLoaded: Boolean,
    onDeleteRule: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_rules_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                // Edge-to-edge: the nav Scaffold already applies the status-bar inset, so this app
                // bar adds none of its own (matches WidgetsScreen / BudgetScreen).
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
                Text(
                    text = stringResource(R.string.category_rules_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xl).padding(top = MaterialTheme.dimens.xs, bottom = MaterialTheme.dimens.md),
                )
                when {
                    // Brief blank on cold start rather than flashing the empty state (see isLoaded).
                    !isLoaded -> Unit
                    rules.isEmpty() -> CategoryRulesEmpty(modifier = Modifier.weight(1f).fillMaxWidth())
                    else -> LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = MaterialTheme.dimens.lg, end = MaterialTheme.dimens.lg, bottom = MaterialTheme.dimens.xxl),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        items(rules, key = { it.key }) { rule ->
                            CategoryRuleCard(rule = rule, onDelete = { onDeleteRule(rule.key) })
                        }
                        item {
                            Text(
                                text = stringResource(R.string.category_rules_footer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, start = MaterialTheme.dimens.lg, end = MaterialTheme.dimens.lg),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRuleCard(rule: CategoryRuleRow, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = MaterialTheme.dimens.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    CategoryChip(category = rule.category, color = Color(rule.categoryColor))
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.category_rules_count,
                        rule.transactionCount,
                        rule.transactionCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.category_rules_delete, rule.displayName),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(MaterialTheme.dimens.iconSmall),
                )
            }
        }
    }
}

/** Tinted "emoji + name" pill in the category's own muted color, matching the propagation sheet. */
@Composable
private fun CategoryChip(category: String, color: Color) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val bg = color.copy(alpha = if (dark) 0.24f else 0.15f)
    val fg = if (dark) lerp(color, Color.White, 0.35f) else lerp(color, Color.Black, 0.42f)
    Text(
        text = "${Categories.emojiOf(category)} $category".trim(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = MaterialTheme.dimens.sm, vertical = 2.dp),
    )
}

@Composable
private fun CategoryRulesEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = MaterialTheme.dimens.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(MaterialTheme.dimens.touchTarget),
        )
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Text(
            text = stringResource(R.string.category_rules_empty),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        Text(
            text = stringResource(R.string.category_rules_footer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun CategoryRulesPreview() {
    BudgettyTheme {
        CategoryRulesContent(
            rules = listOf(
                CategoryRuleRow("bananas", "Bananas", "Groceries", Categories.colorOf("Groceries"), 14),
                CategoryRuleRow("банани", "Банани", "Groceries", Categories.colorOf("Groceries"), 6),
                CategoryRuleRow(
                    "chicken breast", "Chicken breast", "Meat & Poultry",
                    Categories.colorOf("Meat & Poultry"), 3,
                ),
            ),
            isLoaded = true,
            onDeleteRule = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun CategoryRulesEmptyPreview() {
    BudgettyTheme {
        CategoryRulesContent(
            rules = emptyList(),
            isLoaded = true,
            onDeleteRule = {},
            onNavigateBack = {},
        )
    }
}
