package com.budgetty.app.ui.components

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budgetty.app.R
import com.budgetty.app.category.Categories
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.ui.util.categoryDisplayName
import com.budgetty.app.ui.util.formatMoney
import java.math.BigDecimal

/**
 * Bottom sheet listing every transaction in [category] for the period currently shown on
 * Insights. [transactions] is the period's full list; this sheet filters it to [matchCategories]
 * (just [category] for a plain slice, or a whole group's members when the breakdown is rolled up),
 * totals the matching lines, and shows them largest-spend first. [periodLabel] is the active
 * filter (e.g. "this month") shown beside the item count so it's clear which window the total
 * covers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsSheet(
    category: String,
    periodLabel: String,
    transactions: List<TransactionEntity>,
    storeByReceiptId: Map<Long, String>,
    onDismiss: () -> Unit,
    matchCategories: Set<String> = setOf(category),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lines = remember(transactions, matchCategories, storeByReceiptId) {
        transactions
            .filter { it.category in matchCategories }
            // Merge identical lines: same product + same (already-normalized) store + same unit
            // price collapse into one row with the quantities summed, e.g. two "Лимони кг" at
            // 4.45 from Kaufland show as a single "2 × 4.45". Differing prices stay separate so the
            // "qty × price" line is always literally true.
            .groupBy { Triple(it.name, storeByReceiptId[it.receiptId].orEmpty(), it.price.stripTrailingZeros()) }
            .map { (key, group) ->
                val quantity = group.sumOf { it.quantity }
                val unitPrice = group.first().price
                SheetLine(
                    name = key.first,
                    store = key.second.ifBlank { null },
                    unitPrice = unitPrice,
                    quantity = quantity,
                    lineTotal = unitPrice.multiply(BigDecimal(quantity)),
                )
            }
            .sortedByDescending { it.lineTotal }
    }
    val total = remember(lines) {
        lines.fold(BigDecimal.ZERO) { acc, l -> acc + l.lineTotal }
    }
    val emoji = Categories.emojiOf(category)
    val displayName = categoryDisplayName(category)

    AdaptiveSheet(onDismiss = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.dimens.xl)
                .padding(bottom = MaterialTheme.dimens.md),
        ) {
            // Category and total share one line; the "items · period" sub-label hangs below it.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (emoji.isBlank()) displayName else "$emoji  $displayName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                Text(
                    text = total.formatMoney(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            val itemsLabel = pluralStringResource(R.plurals.item_count, lines.size, lines.size)
            Text(
                text = "$itemsLabel · $periodLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xl))
        if (lines.isEmpty()) {
            Text(
                text = stringResource(R.string.category_no_transactions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.lg),
            )
        } else {
            // Only the list scrolls; the header above stays pinned. weight(fill = false) keeps the
            // sheet compact for a few lines yet caps + scrolls a long list within the sheet's own
            // bounds — a fixed heightIn(max) could overflow the sheet and jitter at the scroll edge.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(bottom = MaterialTheme.dimens.xxl),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(lines, key = { "${it.name}|${it.store}|${it.unitPrice.toPlainString()}" }) { line ->
                    TransactionLineRow(
                        name = line.name,
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        store = line.store,
                        prominent = true,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xl))
                }
            }
        }
    }
}

/**
 * Bottom sheet listing every transaction bought at [store] in the period shown on Insights. Mirrors
 * [CategoryTransactionsSheet] but filters [transactions] by store (via [storeByReceiptId]) and labels
 * each line with its category, since the store name is already in the header.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreTransactionsSheet(
    store: String,
    periodLabel: String,
    transactions: List<TransactionEntity>,
    storeByReceiptId: Map<Long, String>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lines = remember(transactions, store, storeByReceiptId) {
        transactions
            .filter { storeByReceiptId[it.receiptId].orEmpty() == store }
            // Merge identical lines (same product + category + unit price) with quantities summed.
            .groupBy { Triple(it.name, it.category, it.price.stripTrailingZeros()) }
            .map { (key, group) ->
                val quantity = group.sumOf { it.quantity }
                val unitPrice = group.first().price
                SheetLine(
                    name = key.first,
                    store = null,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    lineTotal = unitPrice.multiply(BigDecimal(quantity)),
                    category = key.second,
                )
            }
            .sortedByDescending { it.lineTotal }
    }
    val total = remember(lines) { lines.fold(BigDecimal.ZERO) { acc, l -> acc + l.lineTotal } }

    AdaptiveSheet(onDismiss = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.dimens.xl)
                .padding(bottom = MaterialTheme.dimens.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = store,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                Text(
                    text = total.formatMoney(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            val itemsLabel = pluralStringResource(R.plurals.item_count, lines.size, lines.size)
            Text(
                text = "$itemsLabel · $periodLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xl))
        if (lines.isEmpty()) {
            Text(
                text = stringResource(R.string.store_no_transactions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.lg),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(bottom = MaterialTheme.dimens.xxl),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(lines, key = { "${it.name}|${it.category}|${it.unitPrice.toPlainString()}" }) { line ->
                    TransactionLineRow(
                        name = line.name,
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        category = line.category,
                        prominent = true,
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xl))
                }
            }
        }
    }
}

/** One merged row in the category sheet: identical products from the same store at the same unit
 *  price, with their quantities summed. [store] is null when the receipt has no store name. */
private data class SheetLine(
    val name: String,
    val store: String?,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val lineTotal: BigDecimal,
    /** The line's category, shown instead of the store in the per-store sheet (null in the category sheet). */
    val category: String? = null,
)
