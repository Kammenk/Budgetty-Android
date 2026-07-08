# Claude Design request — History screen: a third "Budgets" tab

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and implement the approved ones.

---

Hi! I'm adding a **third tab to the History screen**. Today History has a
**"Receipts | Items"** segmented toggle over the same transaction data. I want to
add a third segment — **"Budgets"** — that instead shows the user's **income
sources** and **recurring payments** (the money-flow data that lives on the
Budget screen). Please mock up the screens below as new `*.dc.html` files,
matching the existing design system exactly.

## What this tab is (context — important)
The History screen normally lists **past transactions** (receipts and their line
items), grouped by month, searchable and filterable. The new **Budgets** tab is
different: income and recurring payments are **planning data**, not posted
transactions — they don't have a per-day history. So this tab is a **snapshot of
the user's current money plan**, surfaced in History so they can review "what's
coming in and what's going out" alongside their spending.

Because it's a snapshot, on the Budgets tab:
- **Hide the search field and the filter/sort row** — they don't apply to
  planning data. (They stay on Receipts and Items.)
- It is **not** month-paged like the receipt list. It shows the current plan.

> Naming note for you to weigh in on: the tab is labelled **"Budgets"** (the
> user's word), but it shows *income + bills*, and the Budget screen separately
> has a "Spending budget." If a clearer one-word label reads better here
> (e.g. **"Plan"** or **"Money"**), mock that as an alternative — but default to
> **"Budgets"**.

## The toggle
The segmented pill becomes **three** equal segments: **Receipts · Items ·
Budgets** (reuse the exact existing segmented-toggle component — rounded track,
`--secc`-filled selected segment, 12px track / 9px thumb radii). Please check the
labels still read cleanly at three segments on the 300px-wide phone; keep them
single-line.

## Layout of the Budgets tab (top → bottom)
1. **Summary / breakdown card** — the same idea as the Budget screen's breakdown,
   framed for review. Small header **"This month"**. Rows:
   - **Income** +€2,400 (positive/green tone)
   - **Recurring bills** −€780
   - divider
   - **Left after bills** **€1,620** (emphasized; green when positive, red when
     negative). *(This is income − bills. Note it is NOT the same as the Budget
     screen's "Left to spend," which also subtracts spending so far — keep this
     one as the simpler income − bills so the two screens don't look wrong side by
     side. Use the label "Left after bills.")*
2. **Income** card — header row **"Income"** (left) + monthly total **"€2,400 /
   mo"** (right, `--onv`). One row per source:
   - a neutral `--secc` tile (income has no category), **name** (e.g. "Salary"),
     subtitle in `--onv` (e.g. **"Monthly · 25th"**), amount **"+€2,400"** in the
     positive/green tone on the right.
   - Include a **one-time** example row to show the new cadence: **"Bonus"**,
     subtitle **"Once · 24 Jun"** (a one-time entry shows the date it was added,
     not a recurring day), amount **"+€600"**.
3. **Recurring payments** card — header **"Recurring payments"** + **"€780 / mo"**.
   Rows = category tile (emoji + muted category color) + name + subtitle
   "Category · cadence · day" + amount on the right:
   - 🏠 **Rent** — "Housing · Monthly · 1st" — €650
   - 📺 **Netflix** — "Entertainment · Monthly · 14th" — €13
   - ⚡ **Electricity** — "Utilities · Monthly · 8th" — €117

Rows here are **read-only for review**. Put a single quiet affordance to go make
changes — e.g. a small **"Manage in Budget →"** text button under the lists (or a
trailing link in each section header). Tapping a row can either do nothing or
also route to Budget — your call; show whichever reads cleaner.

## Screens to design

### 1. `HistoryBudgetsTab.dc.html` — the populated Budgets tab
The full History screen with the **Budgets** segment selected: the History header
("History"), the **three-segment** toggle (Budgets selected), **no search/filter
row**, then the summary card + income card + recurring card + the "Manage in
Budget" affordance, as described above.

### 2. `HistoryBudgetsEmpty.dc.html` — empty state
The Budgets tab for a user who hasn't set up any income or recurring payments:
- three-segment toggle (Budgets selected),
- a friendly empty state (reuse the app's empty-state pattern: emoji + title +
  one-line subtitle + a pill button). Emoji 💰, title **"No budget set up yet"**,
  subtitle **"Add your income and bills in Budget to see your plan here."**, and a
  filled pill **"Go to Budget"**.

## Match these existing components for style
- The **existing History screen** (`HistoryScreen.dc.html`) — header, the
  segmented toggle, month cards, row rhythm. Build on it.
- The **Budget screen's breakdown card + income/recurring rows**
  (`BudgetIncomeRecurringScreen.dc.html`) — reuse that exact row styling (neutral
  tile for income, category tile for bills, +/− amounts, `--onv` subtitles).
- The app's **empty-state** pattern (centered emoji + title + subtitle + pill).
- Buttons are **fully-rounded pills**. Phone preview **300×620**,
  `font-family: Roboto`.

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`, `--onprimary`; selected/emphasis `--secc` / `--onsecc`
- Lines/scrim: `--outv`, `--outline`, `--scrim`
- Green for positive income / positive "left after bills," red for negative.
- Keep everything token-driven (no hard-coded grays) so it themes light/dark.

## Copy (current intent — tweak if shorter reads better)
- Tab label: **Budgets** (alt to explore: **Plan** / **Money**)
- Summary: **This month** / **Income** / **Recurring bills** / **Left after bills**
- Section headers: **Income**, **Recurring payments**; totals as **"€2,400 / mo"**
- One-time subtitle format: **"Once · 24 Jun"**
- Manage affordance: **Manage in Budget →**
- Empty: **No budget set up yet** / **Add your income and bills in Budget to see
  your plan here.** / **Go to Budget**

Note: the app is localized into 21 languages (German and Bulgarian run long) and
amounts show in the user's currency (EUR by default) — let headers, labels, and
the summary rows wrap/space gracefully rather than truncate.

## Already covered — please DON'T redo
- The **income / recurring rows, the add/edit sheets, and the Budget breakdown**
  already exist (`BudgetIncomeRecurringScreen.dc.html`, `IncomeSheet.dc.html`,
  `RecurringPaymentSheet.dc.html`) — reference them, don't recreate them.
- The **Receipts and Items tabs** already exist — keep them; we're only adding the
  third segment and its content.

## Output
- Save each as a new `*.dc.html` file (don't overwrite the existing History
  screen).
- Keep them dark/light-token driven (use the CSS vars above).
- Phone first (300×620). Once approved I'll ask for matching tablet/landscape
  variants (History already goes to a wider grid on tablet).

Thanks!
