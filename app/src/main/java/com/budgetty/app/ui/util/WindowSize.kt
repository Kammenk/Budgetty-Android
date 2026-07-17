package com.budgetty.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The width (in dp) at and above which the layout switches from the phone presentation (bottom
 * navigation bar, single-column screens) to the tablet/expanded one (side navigation rail,
 * multi-column screens). 600dp is the Material breakpoint between "compact" and "medium" widths,
 * so a Pixel Tablet gets the expanded layout in both portrait (~800dp) and landscape (~1280dp).
 */
const val EXPANDED_WIDTH_DP = 600

/**
 * The width (in dp) at and above which a tablet screen is wide enough for a multi-column
 * "dashboard" presentation (e.g. the three-column landscape Home) rather than the stacked
 * single-/two-column layout used on portrait tablets. 840dp is the Material breakpoint between
 * "medium" and "expanded" widths, so a Pixel Tablet stays stacked in portrait (~800dp) and gets the
 * dashboard in landscape (~1280dp).
 */
const val WIDE_WIDTH_DP = 840

/**
 * The height (in dp) below which the window is too short to lay a screen out with its full vertical
 * rhythm — a header plus generous spacing would push the primary action below the fold. 480dp is the
 * Material breakpoint between "compact" and "medium" heights, so phones and small tablets in
 * landscape (~410–430dp) count as compact, while portrait phones (~800dp+) and landscape tablets
 * (~800dp) do not.
 */
const val COMPACT_HEIGHT_DP = 480

/**
 * Upper bound on the content column width in the expanded layouts. Past this the centred content
 * stops stretching, so cards stay a comfortable reading width on large landscape tablets while
 * still filling a portrait tablet (~720dp of content).
 */
val ExpandedContentMaxWidth: Dp = 1080.dp

/**
 * Width cap for the **single-pane** portrait-tablet layouts: one centred column of phone-density
 * content, sized so it reads like a comfortable single measure (not a stretched dashboard) on a
 * ~800dp portrait tablet. Landscape tablets use a two-pane layout instead and don't apply this.
 * Matches the ~640px content column in the `*TabletPortrait` design mocks.
 */
val SinglePaneMaxWidth: Dp = 640.dp

/**
 * True when the available window is wide enough for the tablet/expanded layouts. Read from the
 * current [LocalConfiguration] so it tracks rotation, split-screen, and unfolding live.
 */
@Composable
@ReadOnlyComposable
fun isExpandedWidth(): Boolean =
    LocalConfiguration.current.screenWidthDp >= EXPANDED_WIDTH_DP

/**
 * True when the window is wide enough for a multi-column dashboard (landscape tablet). Implies
 * [isExpandedWidth]. Tracks the live [LocalConfiguration] so it follows rotation and split-screen.
 */
@Composable
@ReadOnlyComposable
fun isWideWidth(): Boolean =
    LocalConfiguration.current.screenWidthDp >= WIDE_WIDTH_DP

/**
 * True when the window is short enough that content must economise on vertical space — typically a
 * phone or small tablet in landscape. Screens use this to drop decorative headers and tighten
 * spacing so their primary action stays above the fold. Tracks the live [LocalConfiguration] so it
 * follows rotation and split-screen.
 */
@Composable
@ReadOnlyComposable
fun isCompactHeight(): Boolean =
    LocalConfiguration.current.screenHeightDp < COMPACT_HEIGHT_DP
