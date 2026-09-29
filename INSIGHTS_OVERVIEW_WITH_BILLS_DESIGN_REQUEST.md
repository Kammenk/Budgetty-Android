# Claude Design request — "With bills" on the Insights → Overview hero (phone + tablet, both platforms)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project (id `5b8c8470-38ec-49d0-b332-b27a9000b4b0`).
> It will create the new `*.dc.html` mockups; once they're there, Claude Code
> reads them back via DesignSync and implements the approved one.

---

# "With bills" on the Insights → Overview hero — design brief (2026-09-29)

**Requested mockups:** fresh `*.dc.html` explorations of how the **Insights → Overview** tab's **"Total spent"** hero card can also surface a **bills-inclusive total** ("With bills"), for **Android phone (Material 3)**, **iOS phone (Liquid Glass)**, and **tablet (adaptive)**. Save as **new** files — don't overwrite `InsightsHybrid.dc.html` / `iOS InsightsHybrid.dc.html` / `TabletInsightsHybrid.dc.html`.

## Goal
On the Insights Overview tab, the hero's **"Total spent"** number is **actual transactions only** — it does not include recurring bills. That's correct and consistent with the app's other *spend* surfaces, but it means a user who wants "what's my period costing me, bills and all?" gets no answer on the tab they land on. We want to add a **secondary, clearly-labelled "With bills" figure** to that hero — reusing the **exact pattern Home already ships** — so the bills-inclusive total is glanceable without redefining what "Total spent" means.

## What "With bills" means here (please get this right — it's the crux)
- **Total spent** (the big hero number) = actual transactions/receipts for the selected period. **Unchanged. Do not fold bills into it.**
- **Bills** here = the period's **recurring bills, planned** (every recurring bill scaled to the selected Insights period, paid or not) — the same notion the **Money** tab already uses for its "out" / savings-rate maths, and the same "**Bills · planned**" language Home uses. It is *not* Home's paid-only "already paid" figure.
- **With bills = Total spent + planned bills.** A secondary, labelled figure — never a silent redefinition of the hero.
- The **↓/↑ % delta** and the **50/30/20 mini-bar** already in this hero stay **transactions-only** (they belong to the "Total spent" number). That's why "With bills" must read as a *separate labelled line*, not a change to the hero figure — otherwise the card contradicts itself.
- **No double-counting risk:** recurring bills are planning-only and never post a transaction, so adding them to transaction spend is always safe.

## Current state (what we're adding to)
See **`InsightsHybrid.dc.html`** (Overview tab, the shipped hybrid). The Overview hero card, top to bottom:
1. Label **"Total spent"** + the big € figure, with a small **↓/↑ %** period-delta beside it.
2. The **50/30/20** mini split bar + three compact bucket labels (Needs / Wants / Savings).
3. A row of three **stat tiles**: Avg/day · Receipts · Saved.

There is currently **no bills information anywhere on the Overview tab** — bills only appear on the Money tab and (as an opt-in overlay) on Spending/Trends.

## Reference — the pattern to mirror (don't reinvent)
- **`Home Bills Summary Explorations.dc.html`** and **`HomeTotalSpent.dc.html`** — Home's shipped "With bills" treatment: a slim **spent-vs-bills** proportion bar, a **"Spent"** row, a **"Bills · planned"** row (hatched key), then a combined **"With bills"** total. The Insights version should feel like the same family.
- **`InsightsRecurringOverlay.dc.html`** — the hatched "planned" bills language already used elsewhere in Insights; keep the planned-bills key consistent with it.

## Directions to explore (lay them side by side to compare)
- **A — Compact "With bills" line (recommended, try first):** under the hero number + delta, a single quiet secondary line — e.g. **"With bills · €1,240"** (label muted `--onv`, amount emphasised). No second bar. This is the lightest touch and avoids competing with the 50/30/20 bar that's already in this card. This is the direction the feature request is really asking for.
- **B — Full Home-style breakdown port:** drop Home's `BillsBreakdown` in verbatim — slim spent-vs-bills bar + "Spent" / "Bills · planned" rows + "With bills" total. Richest and most explicit, but note it puts a **second proportion bar** directly above/below the existing 50/30/20 bar — show whether that reads as busy.
- **C — "With bills" as a fourth stat:** fold it into the existing stat-tile row (Avg/day · Receipts · Saved · **With bills**), so it's a peer stat rather than a new block. Show how four tiles wrap on a narrow phone.

## Platforms & components to match
- **Android phone** — build on **`InsightsHybrid.dc.html`** (Overview tab). Material 3 (Material You), Roboto, phone preview **300×620**. **Not** Liquid Glass.
- **iOS phone** — build on **`iOS InsightsHybrid.dc.html`**. iOS 26 **Liquid Glass**, SF Pro; native-iOS, not a Material port.
- **Tablet** — build on **`TabletInsightsHybrid.dc.html`** (adaptive: left nav rail, wider hero). Extra width here — show whether "With bills" can sit **beside** the "Total spent" figure (horizontal) rather than stacked beneath it.

## States to draw (per platform)
1. **Has bills** — real spend + planned bills, e.g. Total spent **€820** + planned bills **€420** → **With bills €1,240**. The main state.
2. **No bills** — the user has no recurring bills (`hasBills = false`). The "With bills" line/block **must hide entirely** — the hero looks exactly as it ships today. Draw this so it's clear the addition is conditional.
3. **Bills but no spend yet** — Total spent **€0**, planned bills **€420** → **With bills €420** (early in the period). Must look intentional, not broken.

## Design tokens (CSS vars already defined in the project)
- Surfaces `--bg` / `--sc` (surfaceContainer) / `--sch`; text `--on` / `--onv` (muted labels); accent `--primary`; dividers `--outv`; planned-bills hatch exactly as `InsightsRecurringOverlay.dc.html` / Home use it.
- Token-driven only (no hard-coded grays) so it themes in **dark + light**.

## Please produce
- New `*.dc.html` file(s) — the directions above, in the three states, for **Android phone, iOS phone, and tablet**. One file per platform is fine, e.g. **`InsightsOverviewWithBills.dc.html`**, **`iOS Insights Overview With Bills.dc.html`**, **`TabletInsightsOverviewWithBills.dc.html`**.

## Implementation notes (no data-model work)
This is purely a **presentation** change. Everything is already computed on both platforms' Insights view-models:
- `state.total` = transactions-only spend for the period (the current hero number).
- `state.periodBills` = planned recurring bills scaled to the selected period (already populated for the Money tab).
- `state.hasBills` = whether any recurring bill exists (the gate for showing "With bills").
- **With bills = `state.total + state.periodBills`**, shown only when `hasBills` is true.
- Strings already exist: `home_legend_spent` ("Spent"), `home_legend_bills` ("Bills · planned"), `home_with_bills` ("With bills"). Reuse them (or add an `insights_*` alias) — no new copy needed for A.

No new fields, no Room migration, no schema change. Apply identically on Android and iOS (parity), only platform chrome + the tablet adaptive layout differ.

Thanks!
