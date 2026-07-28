# Claude Design request — Android Home period filter (phone)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and implement the approved ones.

---

Hi! I'd like to add a **period filter to the Android phone Home screen**, so
users can view their spending over more than just the current month —
**This month / Last month / Last 3 months / Last 6 months / All time**. Please
mock this up as new `*.dc.html` files, matching the existing Material 3 design
system exactly.

## Why / the goal
- Today the **phone** Home is locked to the current month — there's no way to
  change the window. The period filter already exists **only on the
  wide/landscape-tablet Home (≥840dp)** and on **Insights**; the phone Home and
  the portrait-tablet Home never got it.
- iOS just added exactly this to its phone Home (see **`iOS Home Period.dc.html`**
  / `iOS Home Period Filter Brief.md`). This is the **Android counterpart**, so
  the two platforms match. It's also parity with Android's own tablet Home.
- The period set mirrors the app's `DateRangeFilter`: **This month, Last month,
  Last 3 months, Last 6 months, All time**. Default = **This month** (today's
  look, unchanged).

## Match these existing components for style
- **`HomeScreen.dc.html`** — the phone Home to build on (the "Total spent this
  month" summary card, the Budgets progress card, the planned-bills strip, the
  Receipts list, the "Add receipt" FAB). Build the period control into this.
- **`PeriodDropdownScreen.dc.html`** — the app's existing Android period-filter
  dropdown (This month / Last month / Last 3 / Last 6). Reuse this menu pattern;
  we're just adding **All time** and surfacing it on the phone Home.
- **`InsightsScreen.dc.html`** — how the period filter already reads on a phone
  card (filter affordance top-right of the card, active period label beneath the
  title), so Home and Insights feel like the same control.
- Phone preview size **300×620**, `font-family: Roboto`. Material 3 (Material
  You), **not** iOS Liquid Glass — this is the Android side.

## The control — reuse Android's existing period pill
Good news: Android already has this exact control — the **`HomePeriodFilter`**
pill on the wide Home. It's a **rounded pill** (`surfaceContainerHigh`) showing a
**filter icon + the selected period label**, which opens a Material 3 **dropdown
menu** of the five options with a **check** on the active one. The phone version
should **reuse that same pill + menu**, just placed on the summary card. Default
= **This month**.

The summary card header today reads a static **"Total spent this month"**. Please
show **two placements/treatments** so I can pick (both open the same 5-option
menu, active option checked):

- **Variant A — reuse the existing pill (recommended):** the `HomePeriodFilter`
  pill (filter icon + label) sits **top-right of the summary card**; the "Total
  spent" label drops its "this month" suffix since the pill now carries the
  period. Most consistent with the rest of Android (Insights + wide Home).
- **Variant B — iOS-style label + chevron pill:** the lighter pill from
  **`iOS Home Period.dc.html`** (period label + a **chevron**, no filter icon).
  Use this if we'd rather the phone Home match what iOS just shipped exactly.

Design tokens: pill/menu on `surfaceContainerHigh` (`--sch`), accent `--primary`
for the check / chevron, muted `--onv` for the period label.

## The open design question — how each Home card adapts per period
Home is anchored to "this month" today. The **spend total scales to any period
cleanly**, but the **budget** and **bills** are inherently *monthly-plan*
concepts, so please show how each behaves across periods — especially the
multi-month and All-time states:

1. **Summary / spend card** — the big **"€X spent"** total + receipt count.
   **Scales to the selected period** (straightforward). The label reflects the
   period ("Last 3 months", "All time", …).
2. **Budgets progress card** (monthly + weekly budget bars). A monthly limit
   doesn't map onto "Last 6 months". *Heads-up — the app is inconsistent here
   today:* Android's wide Home **keeps this card fixed to the current month**
   regardless of period, whereas **iOS hides it** for multi-month / all-time.
   Options (please pick and show one):
   - (a) **Hide** it for any non-month period *(recommended — matches iOS, keeps
     Home honest)*;
   - (b) keep it monthly, with a small **"this month"** tag *(what Android's wide
     Home does today)*;
   - (c) scale the limit to the period (budget × months) — *not recommended,
     reads oddly*.
3. **Planned-bills strip** (this month's recurring bills, drawn hatched next to
   spend on the summary card). Android's wide Home **already hides this** for any
   non-month period (`showsBills()` is true only for *This month*), so:
   - (a) **Hide** it for non-month periods *(recommended — already the Android
     behavior)*;
   - (b) keep it monthly with a "this month" tag.
4. **Receipts list / any other Home sections** — scope to the selected period.

**Recommendation to visualize:** the *minimal, clean* reading — the period
filter drives the **spend** total (and receipts), while the **budget card and
bills strip hide** for non-month periods (Home falls back to a spend-focused
view for multi-month / all-time). This keeps the monthly-plan cards honest
instead of stretching them.

## States to draw
Please show **three representative states** of the phone Home so we can see the
adaptation, for **each** control variant (A and B):
1. **This month** (default — current Home look, just with the period now
   selectable).
2. **Last 3 months** (a multi-month state — shows how budget/bills resolve).
3. **All time** (the extreme — spend-focused).
Plus the **open menu** (the five options in the dropdown, "This month" checked).

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`; budget status greens/ambers/reds as already used on the
  Budgets card; savings/positive green for discounts
- Lines: `--outv` (dividers)

## Please produce
- **`HomePeriodScreen.dc.html`** — the phone Home with the period control, in the
  three states above, for **both** variants (A filter-icon and B labelled pill).
  Lay them out side by side or stacked so I can compare.
- If the open menu is clearest as its own frame, add it there too.

## Output
- Save as **new** `*.dc.html` file(s) — don't overwrite `HomeScreen.dc.html`.
- Token-driven only (use the CSS vars above, no hard-coded grays) so they theme
  correctly in **dark + light**.
- Phone first. Once I approve, I'll ask for the tablet / landscape variants (the
  tablet Home already has a period filter, so those just need to match).

## Implementation notes (for after the mockup)
- **No data-model work.** Android's `HomeViewModel` already has the full period
  filter (`selectedFilter` + `DateRangeFilter`, all five options including
  `ALL_TIME`) and `monthStartDay` / `PayCycle` month windows, and the
  **`HomePeriodFilter`** composable already renders the pill + menu — it all
  drives the wide (≥840dp) Home today. This is purely **surfacing that control on
  the phone summary card** (wiring `onFilterSelected` into `PhoneHomeContent`,
  which currently doesn't receive it) and deciding the budget/bills adaptation
  above. Bills already gate on `showsBills()` (This month only); the budget card
  would need the same treatment if we choose to hide it. "All time" spans from the
  earliest receipt to now.

Thanks!
