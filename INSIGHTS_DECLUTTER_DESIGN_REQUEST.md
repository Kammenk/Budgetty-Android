# Claude Design request — Insights screen declutter / reorganization — phone + iOS + tablet (EXPLORATORY)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code reads them back via
> DesignSync so we can look at the directions together.

---

Hi! This one is **exploratory — I'm not committed to changing the screen**, I want to see how you'd think about it. The **Insights** screen has grown a lot over the last few months and now feels **overloaded**: too many stacked analytics cards *and* too much surrounding chrome (period controls, a customize menu that quietly holds extra toggles, and several different banners / nudges / CTAs all competing for attention). I'd love **2–4 distinct directions** for organizing / decluttering it, presented as labeled variants I can react to — **not** one finished redesign. Please explore across all three surfaces (Android phone M3, iOS 26 Liquid Glass, tablet adaptive).

## The problem, plainly
- The screen is essentially **one long vertical scroll of ~13 full-width cards**, each visually similar (rounded `surfaceContainer` card, title, chart/rows). Nothing is prioritized — the pie chart and "biggest purchases" get the same weight — so it reads as a wall and users stop scrolling.
- The **chrome around the content** has quietly accumulated: the header holds the title + a "Customize sections" menu, but that menu *also* hides a Savings-allocation choice and a "Layers" (planned-bills overlay) switch; below it sits a full period stepper. That's a lot before any insight appears.
- **Nudges & CTAs are scattered** through the scroll with no shared home: a one-time "planned overlay" discovery nudge, a one-time savings-allocation ask, repeated "Go to Budget" nudges inside the money-flow cards, and "Planned" badges on three cards that open explainer dialogs. Individually fine; together they feel noisy.

## Everything currently on the screen (full inventory — so nothing gets lost)
**Header (sticky-ish, top):**
- Screen title "Insights".
- **"Customize sections"** menu (icon button) → a reorder/hide list of the sections below, **plus** a Savings-allocation choice **and** a "Layers" toggle for the planned-bills overlay tucked in its header.
- **Period stepper**: unit selector (day / week / month / quarter / year), step back / forward arrows, a **custom range** option, and **All-time**.

**Pinned rows (above the section list):**
- **Wellbeing** — a slim one-line entry that opens the Wellbeing score screen.
- **Recap** re-open row (only when a recap is available).

**The section list — ~13 toggleable, reorderable cards, in this default order:**
1. **Breakdown** — category donut + legend (with a one-time *planned-overlay discovery nudge* rendered right above it).
2. **Subscriptions** — on-device recurring-merchant detection (Premium; a **locked teaser** for free users).
3. **Summary** — 4 stat tiles (avg/day, receipts, avg/receipt, total saved).
4. **Highlights** — rule-based narrative callouts (biggest movers, new/dominant categories).
5. **Trend** — spend-over-time bar chart + projection.
6. **Period comparison** — this period vs the previous one.
7. **Income vs spending** — money-flow card (contains a *"Go to Budget"* nudge).
8. **Savings rate** — a ring (also contains a *"Go to Budget"* nudge).
9. **Needs / Wants / Savings** — the 50/30/20 split card (contains a one-time *savings-allocation ask*), with a **bucket-trend** card rendered beneath it.
10. **Income by source** — income broken down by source.
11. **Top categories** — ranked category rows.
12. **Top stores** — ranked store rows.
13. **Biggest purchases** — largest single line-items in the period.

**Anchored at the very end (not user-managed, always last):**
- **By-category change** — per-category up/down vs the comparison period.

**Cross-cutting CTAs / nudges to account for:**
- Planned-overlay discovery nudge, savings-allocation ask, the repeated Go-to-Budget nudges, and "Planned" badges (on Breakdown / Summary / Trend) that open read-only explainer dialogs.

## What I'd like you to explore (starting points — feel free to propose your own or a hybrid)
- **Direction A — Overview + drill-down (progressive disclosure).** A compact, prioritized "at a glance" top (a few hero numbers + the split or the donut) with everything else **collapsed into expandable rows / smaller tiles** the user opens on demand. Turn the infinite scroll into a short, scannable summary that *leads somewhere*.
- **Direction B — Grouped segments / sub-tabs.** A segmented control under the header that files the cards into a few coherent buckets, e.g. **Overview · Spending · Money flow · Trends** (Spending = Breakdown + Top categories + Top stores + Biggest; Money flow = Income vs spending + Savings rate + Needs/Wants + Income by source; Trends = Trend + Period comparison + Highlights). Kills the single endless page.
- **Direction C — Fewer, denser cards (consolidation).** Merge closely-related cards so 13 becomes ~6: e.g. Summary + Breakdown into one "This period" block; Top categories + Top stores + Biggest into one "Where it went" block with an internal toggle; the money-flow trio into one card. Same information, far less card-frame repetition.
- **Direction D — Calmer chrome.** Keep the section model but **tame the surroundings**: give the scattered nudges/CTAs a single, quiet, dismissible home; rethink where the period stepper + Customize control live (a compact sticky header?); and surface the two hidden toggles (Savings-allocation, Layers) somewhere more honest than buried in the Customize menu.

I don't know which of these is right — that's the point. Show me each as a clearly **labeled variant** so I can compare. A strong hybrid ("B's tabs + A's overview tab") is very welcome.

## Constraints / non-negotiables
- **Nothing gets deleted.** Every capability above must stay reachable in whatever you propose — this is reorganization, not feature removal.
- **Keep it free** (no new paywalls beyond the existing Subscriptions teaser), and keep the **Customize-sections** idea alive (users can hide/reorder) — though you may evolve *how* it's presented; just say how.
- **Match the existing visual language**: `surfaceContainer` cards, 20dp radius (16dp rows), brand violet (`#6650A4` light / `#D0BCFF` dark), muted `onSurfaceVariant` for secondary text. This is a re-org of what's there, not a new theme.
- Preserve the **period context** (whatever's on screen always reflects the selected period + planned-overlay state).

## Current-state references already in the project (please build the "current" column from these)
- **Phone:** `InsightsScreen.dc.html`, `InsightsScreen Variants.dc.html`, `InsightsCustomizeSheet.dc.html`, and the per-card files `InsightsFixedVsFlexible.dc.html`, `InsightsIncomeVsSpending.dc.html`, `InsightsSavingsRate.dc.html`, `InsightsIncomeBySource.dc.html`, `InsightsBiggestBills.dc.html`, `InsightsPieLegend.dc.html`, `InsightsRecurringOverlay.dc.html`, `NeedsWantsSplit.dc.html`, `SavingsAllocationAsk.dc.html`, `TrendsScreen.dc.html`, `WellbeingEntry.dc.html`.
- **iOS:** `iOS Insights.dc.html`, `iOS Insights Extra Cards.dc.html`, `iOS Insights Setup.dc.html`, `iOS Insights Recurring Overlay.dc.html`, `iOS NeedsWantsSplit.dc.html`.
- **Tablet:** `InsightsScreenTabletTwoPane.dc.html`, `InsightsScreenTabletPortrait.dc.html`, `TabletInsightsScreen.dc.html`, `TabletTrendsScreen.dc.html`.

## Platforms & components to match
- **Android phone — Material 3.** Phone frame **300×620**, Roboto. Show the current screen (or a faithful compressed version of the wall) beside each proposed direction so the before/after is obvious.
- **iOS phone — iOS 26 Liquid Glass.** Same directions, native chrome, matching the existing `iOS Insights.dc.html` treatment.
- **Tablet — Material 3 adaptive.** Left **nav rail**, multi-column grid; the two-pane Insights layout already exists (`InsightsScreenTabletTwoPane.dc.html`) — show how each direction adapts to that width, especially where the segmented control / overview / Customize control land under the rail. Build from the tablet treatment, not a stretched phone.

## Design tokens
- Surfaces `--bg` / `--sc` / `--sch`; text `--on` / `--onv`; dividers `--outv` — same vars used across the other Budgetty mockups, themed for dark + light.

## Please produce
- **`InsightsDeclutter.dc.html`** — Android phone: the current wall + each explored direction as a labeled variant, side by side.
- **`iOS InsightsDeclutter.dc.html`** — iOS 26 Liquid Glass versions of the same directions.
- **`TabletInsightsDeclutter.dc.html`** — adaptive tablet versions.

## Notes (for after the mockups)
- **Exploratory only — no implementation until I pick a direction.** If I do go ahead it's full **Android + iOS + tablet parity** (Insights is a shared surface), matched to the chosen mockup's CSS.

Thanks — excited to see what you come up with!
