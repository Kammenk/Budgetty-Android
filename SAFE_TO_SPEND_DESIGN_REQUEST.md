# Claude Design request — Android "Safe to spend" until payday (phone)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and implement the approved ones.

---

Hi! I'd like to add a **"Safe to spend" figure to the Android phone Home screen** —
one clear number that tells the user *how much they can freely spend before their
next payday*, after the money they've already spent and the bills they still owe
this pay-cycle. This is the signature Monzo/Simple "safe-to-spend" idea. Please
mock it up as new `*.dc.html` files, matching the existing Material 3 design
system exactly. **iOS is getting the identical feature** (see
`IOS_DESIGN_REQUEST_SAFE_TO_SPEND.md`), so the two platforms should behave the same.

## Why / the goal
- Budgetty already knows the user's **income**, their **recurring bills** (with a
  paid/unpaid state), their **spending so far**, and their **pay-cycle** (the month
  can start on a chosen day). Nothing surfaces the single most useful daily number:
  *what's actually safe to spend right now.* This turns data the app already has
  into a glanceable answer.
- It's **cash-flow**, distinct from the existing **Budget** (a spending *target*).
  Both coexist — please make it read as its own thing, not a re-skin of the budget
  bar.
- **Free feature** (not premium) — it's core daily value.

## The number (this is the whole feature — get this across)
**Safe to spend = Income this pay-cycle − Spent so far this cycle − Bills still unpaid this cycle.**
- Resets every pay-cycle; the cycle can start on any day of the month (not just the 1st).
- Optional secondary line: **per-day** = safe-to-spend ÷ days until payday
  (e.g. *"€28/day for 9 days"*).
- Savings goals are **not** subtracted in v1 (they're a separate manual tracker).

## Match these existing components for style
- **`HomeScreen.dc.html`** — the phone Home this lands on (the "Total spent this
  month" summary card, Budgets progress card, planned-bills strip, Receipts list,
  "Add receipt" FAB). The safe-to-spend hero goes at the **top**, above the spend
  summary.
- **`HomePeriodScreen.dc.html`** — the recent Home summary-card treatment, for the
  card radius / hero-number type scale.
- **Budgets progress card** on `HomeScreen.dc.html` — reuse its green/amber/red
  status color language for the safe-to-spend states, so "healthy/careful/over"
  reads consistently.
- Phone preview size **300×620**, `font-family: Roboto`. Material 3 (Material You),
  **not** iOS Liquid Glass — this is the Android side.

## Placements to show (so I can pick)
All three drive the same number; please lay them out to compare:
- **Variant A — dedicated hero card (recommended):** a new card at the very top of
  Home, **"Safe to spend"** label + a **large € amount** + the per-day secondary,
  with a thin progress/proportion hint (spent vs income). The flagship treatment.
- **Variant B — folded into the spend summary card:** the existing "Total spent"
  card gains safe-to-spend as its **primary** hero line, with "spent" demoted to a
  sub-stat.
- **Variant C — compact Home card:** a smaller card that can be toggled via
  Customize sections (for users who prefer spend-first).

## The states to draw (for the recommended Variant A)
1. **Healthy** — comfortably positive. e.g. *"€420 safe to spend · €28/day · until 1 Aug"*. Positive/green accent.
2. **Getting low** — small amount left for the days remaining. Amber tint, gentle caution tone (*"€40 left for 9 days"*).
3. **Overspent** — negative. Red, *"€65 over — spent more than you have left this cycle."*
4. **Setup / no income yet** — the app can't compute it until income is set. Show a friendly setup state: *"Add your income to see what's safe to spend"* with a CTA that points to the **Budget** tab (income lives there). **This is a common first-run state — please make it look intentional, not broken.**

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`; status greens/ambers/reds exactly as the Budgets card uses them
- Lines: `--outv` (dividers)

## Please produce
- **`SafeToSpendScreen.dc.html`** — the phone Home with the safe-to-spend hero, in
  the **four states** above, for the **recommended Variant A**, plus a compact
  side-by-side of **Variants B and C** so I can choose the placement.
- Token-driven only (CSS vars above, no hard-coded grays) so it themes in **dark + light**.
- Phone first. Once I approve, I'll ask for the tablet / landscape variant.

## Implementation notes (for after the mockup — no new data model)
Everything this needs already ships on Android `main`:
- **Pay-cycle window + days-to-payday:** `ui/util/PayCycle.kt` (+ `monthStartDay`),
  same windows `DateRangeFilter` uses.
- **Income:** recurring rows with `isIncome = true` (summed to the cycle).
- **Bills still unpaid:** recurring bills (`isIncome = false`) where
  `RecurringMath.isPaidThisCycle` is false (mark-as-paid already exists).
- **Spent so far:** transactions summed within the current pay-cycle window
  (`HomeViewModel` already computes this).
So this is a **new `HomeViewModel` derivation + one Home card**, no schema change.

Thanks!
