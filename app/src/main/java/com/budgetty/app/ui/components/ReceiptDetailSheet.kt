package com.budgetty.app.ui.components

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.budgetty.app.R
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.data.model.Receipt
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.util.formatDate
import com.budgetty.app.ui.util.formatMoney
import kotlinx.coroutines.launch

/**
 * Bottom sheet showing a receipt's items, with per-item and whole-receipt delete (undoable). Shared
 * between Home and History so tapping a receipt on either screen opens the same detail view (rather
 * than jumping straight into the editor).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailSheet(
    receipt: Receipt,
    onDismiss: () -> Unit,
    onEditReceipt: () -> Unit,
    onDeleteItem: (TransactionEntity) -> Unit,
    onUndo: () -> Unit,
    onDeleteReceipt: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AdaptiveSheet(onDismiss = onDismiss, sheetState = sheetState) {
        ReceiptDetailContent(
            receipt = receipt,
            onEditReceipt = onEditReceipt,
            onDeleteItem = onDeleteItem,
            onUndo = onUndo,
            onDeleteReceipt = onDeleteReceipt,
        )
    }
}

/**
 * The receipt-detail body — store/date/total header, the scrolling item list, and the edit/delete
 * actions. Shared by [ReceiptDetailSheet] (phone & portrait, inside an AdaptiveSheet) and History's
 * landscape two-pane, where it renders inline as the right-hand detail pane.
 */
@Composable
fun ReceiptDetailContent(
    receipt: Receipt,
    onEditReceipt: () -> Unit,
    onDeleteItem: (TransactionEntity) -> Unit,
    onUndo: () -> Unit,
    onDeleteReceipt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetSnackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val itemDeletedMsg = stringResource(R.string.snackbar_item_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.dimens.xl, end = MaterialTheme.dimens.xl, bottom = 28.dp),
        ) {
            // Header — store, date, total and discount — stays pinned above the scrolling items.
            Row(verticalAlignment = Alignment.Top) {
                StoreLogo(store = receipt.store, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = receipt.store.ifBlank { "Receipt" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${receipt.timestamp.formatDate()} · ${pluralStringResource(R.plurals.item_count, receipt.transactions.size, receipt.transactions.size)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = receipt.paid.formatMoney(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (receipt.discount.signum() > 0) {
                        Text(
                            text = "Discount −${receipt.discount.formatMoney()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = budgetGoodColor(),
                        )
                    }
                    // The total above already includes this tax — added on top of the net line prices
                    // for a tax-exclusive receipt, or contained within them otherwise — so it reads as
                    // an "incl. VAT" note, not a separate addition.
                    if (receipt.tax.signum() > 0) {
                        Text(
                            text = stringResource(R.string.receipt_vat_included, receipt.tax.formatMoney()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            // Only the items list scrolls — the header above and the actions below stay put.
            // weight(fill = false) keeps the sheet compact for short receipts, yet caps and
            // scrolls a long list so the buttons remain reachable.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                receipt.transactions.forEach { txn ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TransactionRow(
                            transaction = txn,
                            modifier = Modifier.weight(1f),
                            showCategory = true,
                            // The sheet's Column already insets 20.dp; drop the row's own
                            // horizontal padding so item content lines up with the header.
                            contentPadding = PaddingValues(vertical = MaterialTheme.dimens.md),
                        )
                        IconButton(onClick = {
                            if (receipt.transactions.size <= 1) {
                                // Removing the only item is the same as deleting the receipt.
                                onDeleteReceipt()
                            } else {
                                onDeleteItem(txn)
                                scope.launch {
                                    val result = sheetSnackbar.showSnackbar(
                                        message = itemDeletedMsg,
                                        actionLabel = undoLabel,
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) onUndo()
                                }
                            }
                        }) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = stringResource(R.string.cd_delete_item),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
                OutlinedButton(
                    onClick = onEditReceipt,
                    modifier = Modifier
                        .weight(1f)
                        .height(MaterialTheme.dimens.buttonHeight),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.width(MaterialTheme.dimens.sm))
                    Text(stringResource(R.string.action_edit), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDeleteReceipt,
                    modifier = Modifier
                        .weight(1f)
                        .height(MaterialTheme.dimens.buttonHeight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(MaterialTheme.dimens.sm))
                    Text(stringResource(R.string.action_delete), fontWeight = FontWeight.Bold)
                }
            }
        }
        SnackbarHost(
            hostState = sheetSnackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** A colored square "logo" with the store's first letter (or a receipt glyph when unknown). */
@Composable
fun StoreLogo(store: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val tile = if (store.isBlank()) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        Color.hsv(((store.hashCode() and 0x7FFFFFFF) % 360).toFloat(), 0.45f, 0.6f)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(tile),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (store.isBlank()) "🧾" else store.trim().take(1).uppercase(),
            style = if (size > 44.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
