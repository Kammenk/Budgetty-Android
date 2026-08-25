package com.budgetty.app.ui.streaks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.budgetGoodColor
import com.budgetty.app.ui.theme.dimens

/**
 * The shared, calm streak visual language (§2.6): a short row of rounded segments that reads the
 * same everywhere a streak surfaces — the Recap Streak card (§1.3), the de-flamed monthly budget
 * card (§2.4), and later the Wellbeing evidence (§3) and Buying-limits card (§4). Deliberately
 * general and stateless so those callers can reuse it as-is.
 *
 * The motif carries three states, all in the [budgetGoodColor] "good" tone — never a flame, never a
 * status red, never amber, and never a countdown (§1 principle 2):
 *  - **[filledCount] solid segments** = closed periods met.
 *  - **one dashed ghost segment** (60% alpha) when [showLive] = the open period, "on track so far".
 *    Only rendered for a live current run, never for a best-run fallback.
 *  - **[muted] mode** = every segment solid good at ~55% alpha with no ghost — the best-run fallback,
 *    a quiet record of a past run rather than a live one.
 *
 * Segment geometry mirrors the design mockup (≈ 2.6em × 0.85em, fully-rounded); [maxSegments] caps the
 * row so a long (up to 24-period) run can't overflow — the exact number always lives in the hero copy.
 */
@Composable
fun StreakMotif(
    filledCount: Int,
    modifier: Modifier = Modifier,
    showLive: Boolean = false,
    muted: Boolean = false,
    maxSegments: Int = MAX_MOTIF_SEGMENTS,
) {
    val good = budgetGoodColor()
    val filled = filledCount.coerceIn(0, maxSegments)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xs)) {
        repeat(filled) {
            Spacer(
                modifier = Modifier
                    .size(SegmentWidth, SegmentHeight)
                    .clip(CircleShape)
                    .background(if (muted) good.copy(alpha = MUTED_ALPHA) else good),
            )
        }
        // The live/on-track ghost: a dashed outline of the same good tone, only on a live current run.
        if (showLive && !muted) {
            Spacer(
                modifier = Modifier
                    .size(SegmentWidth, SegmentHeight)
                    .drawBehind {
                        drawRoundRect(
                            color = good.copy(alpha = LIVE_ALPHA),
                            cornerRadius = CornerRadius(size.height / 2f),
                            style = Stroke(
                                width = SegmentBorder.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(DashOn.toPx(), DashOff.toPx()),
                                ),
                            ),
                        )
                    },
            )
        }
    }
}

// Design-specific sizing (mirrors the mockup's em geometry); named like RecapStoryScreen's own
// hero-size constants. Spacing between segments stays on the dimens grid (dimens.xs).
private val SegmentWidth = 24.dp
private val SegmentHeight = 8.dp
private val SegmentBorder = 1.5.dp
private val DashOn = 2.5.dp
private val DashOff = 2.5.dp
private const val MUTED_ALPHA = 0.55f
private const val LIVE_ALPHA = 0.6f
private const val MAX_MOTIF_SEGMENTS = 8

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Streak motif · states", widthDp = 220)
@Composable
private fun StreakMotifPreview() {
    BudgettyTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .size(220.dp, 96.dp),
        ) {
            StreakMotif(filledCount = 3, showLive = true)
            StreakMotif(filledCount = 6, muted = true)
            StreakMotif(filledCount = 2)
        }
    }
}
