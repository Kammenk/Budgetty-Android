package com.budgetty.app.ui.components

import com.budgetty.app.ui.theme.dimens
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetty.app.category.Categories
import com.budgetty.app.data.local.TransactionEntity
import com.budgetty.app.ui.util.categoryDisplayName
import com.budgetty.app.ui.util.formatMoney
import java.math.BigDecimal

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    modifier: Modifier = Modifier,
    showCategory: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.md),
) {
    TransactionLineRow(
        name = transaction.name,
        quantity = transaction.quantity,
        unitPrice = transaction.price,
        category = transaction.category.takeIf { showCategory },
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

/**
 * Renders one product line: name, "<qty> × <unit price>", an optional [store] label, and the line
 * total (unit price × qty). The Insights category sheet uses this directly for its merged lines —
 * identical products from the same store are summed, so [quantity] can exceed 1 even for weighed
 * items (e.g. "2 × 4.45"). Home/History reach it via [TransactionRow].
 *
 * [prominent] scales the type up a step for the Insights category drill-down, where the list is the
 * whole screen and reads larger than the compact Home/History rows. It defaults off so those callers
 * are unchanged.
 */
@Composable
fun TransactionLineRow(
    name: String,
    quantity: Int,
    unitPrice: BigDecimal,
    modifier: Modifier = Modifier,
    store: String? = null,
    category: String? = null,
    prominent: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = if (prominent) MaterialTheme.dimens.xl else MaterialTheme.dimens.lg,
        vertical = if (prominent) 14.dp else MaterialTheme.dimens.md,
    ),
) {
    val lineTotal = unitPrice.multiply(BigDecimal(quantity))
    val nameStyle = when {
        prominent -> MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium)
        category != null -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        else -> MaterialTheme.typography.bodyLarge
    }
    val secondaryStyle = if (prominent) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.bodySmall
    }
    val amountStyle = if (prominent) {
        MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
    } else {
        MaterialTheme.typography.bodyLarge
    }
    val categoryLabel = category?.let { categoryDisplayName(it) }
    val secondaryText = buildString {
        if (category != null) {
            val emoji = Categories.emojiOf(category)
            if (emoji.isNotEmpty()) append("$emoji ")
            append(categoryLabel)
            append(" · ")
        }
        append("$quantity × ${unitPrice.formatMoney()}")
        if (!store.isNullOrBlank()) append(" · $store")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = nameStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = secondaryText,
                style = secondaryStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = lineTotal.formatMoney(),
            style = amountStyle,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
