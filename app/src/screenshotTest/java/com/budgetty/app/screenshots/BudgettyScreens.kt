package com.budgetty.app.screenshots

import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview that renders a screen across the window sizes and font scale that matter for
 * Budgetty's adaptive layouts. The widths deliberately bracket the two breakpoints defined in
 * `ui/util/WindowSize.kt` — 600dp (phone → tablet: bottom bar → nav rail, single → multi-column)
 * and 840dp (stacked → dashboard) — so a single annotation exercises every adaptive branch:
 *
 *  - **phone / phone-landscape** — compact width; note a phone in landscape is ~891dp wide, so it
 *    crosses 600dp and renders the *expanded* layout (matches real device behaviour).
 *  - **tablet-portrait (800dp)** — expanded but below 840dp, so stacked, not dashboard.
 *  - **tablet-landscape (1280dp)** — the wide dashboard layout.
 *  - **breakpoint-599 / breakpoint-601** — one dp either side of the phone→tablet switch, the most
 *    regression-prone seam given the layout keys off `screenWidthDp >= 600`.
 *  - **font-2x** — largest accessibility font scale, the usual source of clipping/overflow.
 *
 * Apply it to a stateless preview to lock the rendering down; `validateDebugScreenshotTest` then
 * diffs future changes against the recorded PNGs. Render on the host JVM — no emulator required.
 */
@Preview(name = "phone", showBackground = true, widthDp = 411, heightDp = 891)
@Preview(name = "phone-landscape", showBackground = true, widthDp = 891, heightDp = 411)
@Preview(name = "tablet-portrait", showBackground = true, widthDp = 800, heightDp = 1280)
@Preview(name = "tablet-landscape", showBackground = true, widthDp = 1280, heightDp = 800)
@Preview(name = "breakpoint-599", showBackground = true, widthDp = 599, heightDp = 900)
@Preview(name = "breakpoint-601", showBackground = true, widthDp = 601, heightDp = 900)
@Preview(name = "font-2x", showBackground = true, widthDp = 411, heightDp = 891, fontScale = 2.0f)
annotation class BudgettyScreens
