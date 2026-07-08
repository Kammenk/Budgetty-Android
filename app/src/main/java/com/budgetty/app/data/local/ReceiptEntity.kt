package com.budgetty.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * Receipt-level details for one upload. [timestamp] is the upload time shared with the receipt's
 * [TransactionEntity]s (so they join without a foreign key). [date] is the receipt's own printed
 * date (epoch millis); [discount] is the total savings on the receipt.
 */
@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val timestamp: Long,
    val store: String,
    val date: Long,
    val discount: BigDecimal,
    /** True if this receipt was entered manually (no scan). Drives the edit-screen "Add receipt" action. */
    val isManual: Boolean = false,
    /**
     * The receipt's tax/VAT. Meaning depends on [taxOnTop]: when false it is *contained in* the line
     * prices (a normal tax-inclusive receipt) and already sits inside the item sum; when true it is
     * *added on top* of the (printed, net) line prices — a tax-exclusive receipt (US sales tax, EU net
     * invoice) — so it must be added to the item sum to reach what was paid. Either way it's shown as
     * an "incl. VAT" line. 0 when the receipt reports no tax. Stored since DB v15; the default mirrors
     * [MIGRATION_14_15]'s `DEFAULT '0'` so Room's schema validation passes.
     */
    @ColumnInfo(defaultValue = "0")
    val tax: BigDecimal = BigDecimal.ZERO,
    /**
     * True when [tax] is added ON TOP of the net line prices (tax-exclusive receipt) rather than
     * contained within them. Drives whether totals add [tax] to the item sum (see
     * [com.budgetty.app.data.model.additiveChargesOf]).
     * Defaults false — every pre-v16 receipt, and every tax-inclusive one, has its tax inside the
     * prices. Stored since DB v16 ([MIGRATION_15_16], `DEFAULT 0`).
     */
    @ColumnInfo(defaultValue = "0")
    val taxOnTop: Boolean = false,
    /**
     * Money the customer paid that isn't a line item, an order [discount], or on-top [tax] — delivery
     * & service fees plus a courier tip on a delivery-app order, an uncaptured deposit, etc. It is the
     * gap by which the printed grand total exceeds what the items reconcile to (item sum − discount +
     * any on-top tax), added to summed spend (see [com.budgetty.app.data.model.additiveChargesOf]) so
     * totals equal what was actually paid without inventing product line items. 0 for an ordinary
     * receipt whose items already reconcile to its total. Stored since DB v17 ([MIGRATION_16_17],
     * TEXT like [discount], `DEFAULT '0'`).
     */
    @ColumnInfo(defaultValue = "0")
    val extraCharges: BigDecimal = BigDecimal.ZERO,
)
