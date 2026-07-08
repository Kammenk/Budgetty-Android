package com.budgetty.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.budgetty.app.ui.util.ExpandedContentMaxWidth

/**
 * Central design-token set for every layout dimension in the app.
 *
 * WHY THIS EXISTS: dimensions used to be hard-coded inline (`16.dp`, `56.dp`, …) all over the
 * screens. That made it impossible to tune spacing per device without hunting through dozens of
 * files. This is the Compose-idiomatic equivalent of an Android `dimens.xml` set: a single source
 * of truth, provided through [LocalDimens] and read as `MaterialTheme.dimens.<token>`.
 *
 * The set is swapped by window size in [com.budgetty.app.ui.theme.BudgettyTheme] — phones get
 * [CompactDimens], tablets/expanded windows get [ExpandedDimens] — so the *same* call site
 * (`padding(MaterialTheme.dimens.screenPadding)`) yields a comfortably larger value on a tablet
 * without any `if (isExpandedWidth())` branching at the call site.
 *
 * Grouping:
 *  - **Spacing grid** — the canonical 4dp step ladder used for the vast majority of gaps and
 *    paddings. Device-invariant (a 4dp gap is 4dp everywhere); centralized purely so the grid is
 *    named and consistent.
 *  - **Layout roles** — semantic page/section/card dimensions. These are the ones that grow on
 *    larger screens (see [ExpandedDimens]).
 *  - **Component sizes** — icons, avatar, the standardized action-button height, etc.
 *  - **Corner radii** — raw dp radii. [Shape.kt] still owns the Material3 [androidx.compose
 *    .material3.Shapes] scale; these cover the ad-hoc `RoundedCornerShape(n.dp)` call sites.
 *  - **Named one-offs** — chart / imagery heights that were previously magic numbers.
 */
@Immutable
data class Dimens(
    // ---- Spacing grid (canonical 4dp ladder; device-invariant) ----
    /** 4dp — tightest grid step (icon↔label, fine insets). */
    val xs: Dp = 4.dp,
    /** 8dp — the default small gap. */
    val sm: Dp = 8.dp,
    /** 12dp — medium gap between related items. */
    val md: Dp = 12.dp,
    /** 16dp — standard content gap / list-row padding. */
    val lg: Dp = 16.dp,
    /** 20dp — card inner padding on phones. */
    val xl: Dp = 20.dp,
    /** 24dp — section gap. */
    val xxl: Dp = 24.dp,
    /** 32dp — large block separation. */
    val xxxl: Dp = 32.dp,

    // ---- Layout roles (semantic; scaled up on tablets in ExpandedDimens) ----
    /** Horizontal margin from the screen edge to page content. */
    val screenPadding: Dp = 16.dp,
    /** Vertical gap between the major sections of a screen. */
    val sectionSpacing: Dp = 16.dp,
    /** Inner padding of cards / elevated surfaces. */
    val cardPadding: Dp = 20.dp,
    /** Gap between sibling cards / list rows. */
    val cardSpacing: Dp = 12.dp,
    /** Upper bound on centred content width in expanded (tablet) layouts. */
    val contentMaxWidth: Dp = ExpandedContentMaxWidth,

    // ---- Component sizes ----
    /** Standardized height for every primary action button. */
    val buttonHeight: Dp = 56.dp,
    /** Minimum comfortable tap target. */
    val touchTarget: Dp = 48.dp,
    /** Small inline icon (e.g. trailing chevrons, chip glyphs). */
    val iconSmall: Dp = 18.dp,
    /** Default icon size. */
    val icon: Dp = 24.dp,
    /** Avatar / leading category tile. */
    val avatar: Dp = 40.dp,
    /** Height of budget / progress bars. */
    val progressHeight: Dp = 8.dp,
    /** Divider / hairline border thickness. */
    val hairline: Dp = 1.dp,

    // ---- Corner radii (raw dp; Shape.kt owns the M3 Shapes scale) ----
    val radiusSm: Dp = 8.dp,
    val radiusMd: Dp = 12.dp,
    val radiusLg: Dp = 16.dp,
    val radiusXl: Dp = 20.dp,
    val radiusXxl: Dp = 28.dp,

    // ---- Named one-offs (previously magic numbers) ----
    /** Height of the trend / line charts on Insights & Home. */
    val chartHeight: Dp = 150.dp,
    /** Diameter of the spending donut / pie chart. */
    val donutSize: Dp = 200.dp,
    /** Height of the receipt image preview in detail sheets. */
    val sheetImageHeight: Dp = 250.dp,
)

/** Phone / compact-width dimensions (the design baseline). */
val CompactDimens = Dimens()

/**
 * Tablet / expanded-width dimensions. Only the semantic *layout roles* breathe wider here — the
 * spacing grid, component sizes and radii stay identical so a given gap or icon is the same
 * physical size regardless of device.
 */
val ExpandedDimens = Dimens(
    screenPadding = 24.dp,
    sectionSpacing = 24.dp,
    cardPadding = 24.dp,
    cardSpacing = 16.dp,
)

/**
 * The dimension set for the current window. Defaults to [CompactDimens]; the real value is
 * provided per window size by [com.budgetty.app.ui.theme.BudgettyTheme]. `static` because it only
 * changes on a configuration change (rotation, fold, split-screen), not on every recomposition.
 */
val LocalDimens = staticCompositionLocalOf { CompactDimens }

/**
 * Read the current dimensions as `MaterialTheme.dimens.<token>`, mirroring how the rest of the app
 * reads `MaterialTheme.colorScheme` / `MaterialTheme.typography`.
 */
val MaterialTheme.dimens: Dimens
    @Composable
    @ReadOnlyComposable
    get() = LocalDimens.current
