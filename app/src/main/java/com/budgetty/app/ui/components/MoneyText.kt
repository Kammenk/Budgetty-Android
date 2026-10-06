package com.budgetty.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.budgetty.app.R
import com.budgetty.app.ui.util.AppFormats
import com.budgetty.app.ui.util.formatMoney
import java.math.BigDecimal

/** The frosted pill is sized as a multiple of the text's font size (the mockup's 3.4em × 0.74em). */
private const val PILL_WIDTH_EM = 3.2f
private const val PILL_HEIGHT_EM = 0.74f

/**
 * Renders a monetary [amount] as text, or — while [AppFormats.hideAmounts] is on — as a fixed-width
 * "frosted pill" that leaks neither the figure's magnitude nor its sign (€22 and €1,962 look
 * identical). The approved privacy treatment from the Hide-amounts mockup. Everything around the
 * amount (labels, bars, chart shapes) stays untouched; only this slot is masked.
 */
@Composable
fun MoneyText(
    amount: BigDecimal,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    MaskedMoneySlot(modifier, style, color, fontWeight, textAlign, maxLines) { amount.formatMoney() }
}

/**
 * Masks a pre-built money string (e.g. a "spent / limit" pair like "€712 / €1,200") as a single pill.
 * Use when the value shown is already a composed string rather than one [BigDecimal]; the whole slot
 * becomes one pill while hidden, matching how the mockup masks combined figures.
 */
@Composable
fun MoneyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    MaskedMoneySlot(modifier, style, color, fontWeight, textAlign, maxLines) { text }
}

@Composable
private fun MaskedMoneySlot(
    modifier: Modifier,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight?,
    textAlign: TextAlign?,
    maxLines: Int,
    text: () -> String,
) {
    if (AppFormats.hideAmounts) {
        AmountMaskPill(style = style, modifier = modifier)
    } else {
        Text(
            text = text(),
            modifier = modifier,
            color = color,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
        )
    }
}

/**
 * The frosted pill itself: a fully-rounded bar sized to the text's font size, filled with a soft
 * outlineVariant→surfaceContainerHigh gradient that reads in both light and dark themes. Public so a
 * hero whose shown Text needs features [MoneyText] doesn't carry (marquee, fillMaxWidth, custom
 * overflow) can render it directly behind an `if (AppFormats.hideAmounts)` guard, keeping the shown
 * path's modifiers intact while the hidden path is a clean fixed-width pill.
 */
@Composable
fun AmountMaskPill(style: TextStyle, modifier: Modifier = Modifier) {
    val fontSize: TextUnit = style.fontSize.takeIf { it != TextUnit.Unspecified } ?: 14.sp
    val density = LocalDensity.current
    val widthDp = with(density) { fontSize.toDp() } * PILL_WIDTH_EM
    val heightDp = with(density) { fontSize.toDp() } * PILL_HEIGHT_EM
    val edge = MaterialTheme.colorScheme.outlineVariant
    val core = MaterialTheme.colorScheme.surfaceContainerHigh
    val hidden = stringResource(R.string.amount_hidden)
    Box(
        modifier = modifier
            .height(with(density) { fontSize.toDp() } * 1.1f)
            .clearAndSetSemantics { contentDescription = hidden },
        // Start-aligned so that when the caller passes a fillMaxWidth/weight modifier (e.g. a hero that
        // marquees its number), the pill sits where the number began and any sibling stays put.
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .width(widthDp)
                .height(heightDp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Brush.horizontalGradient(0f to edge, 0.55f to core, 1f to edge)),
        )
    }
}
