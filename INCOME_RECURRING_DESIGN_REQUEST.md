# Claude Design request — Budget screen: income & recurring payments

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and implement the approved ones.

---

Hi! I'm adding two related features to the Budgetty **Budget screen**: **income**
(a list of income sources) and **recurring payments** (fixed bills like rent,
subscriptions, utilities). Both live on the Budget screen. Please mock up the
screens below as new `*.dc.html` files in this project, matching the existing
design system exactly.

## What the feature does (context)
Today the Budget screen has one spending budget (a Monthly/Weekly pill + an
amount + a progress bar) and a per-category budget grid. We're adding, **above**
that, a small "money flow" area:
- **Income** — a list of income sources (Salary, Freelance, …), each with an
  amount and how often it arrives. Summed into a monthly income figure.
- **Recurring payments** — a list of fixed bills, each with an amount, a
  category, and a due day. Summed into a monthly total.
- **Breakdown** — a small summary that ties it together: income − recurring
  bills − spent so far = what's left this month.

For this first version recurring items are **planning only** — they inform the
breakdown but don't post transactions yet (that's a later phase), so nothing
outside the Budget screen changes.

## Layout & behaviour (important)
The Budget screen, top → bottom:
1. **Income** section
2. **Recurring payments** section
3. **Breakdown** card — *only shown once at least one income source exists*
4. **Spending budget** section — the existing Monthly/Weekly pill + amount +
   progress + category grid, now under a new header

Rules:
- **Never show an empty card.** When a section has no items, collapse it to a
  single slim, muted, tappable "add" row (no card chrome) — not a big empty card.
  A section becomes a full card only once it has items.
- **No income → no breakdown.** Hide the breakdown entirely until an income
  source is added.
- So a brand-new Budget screen shows just: a slim "Add income" row, a slim
  "Add recurring payment" row, then the Spending budget section.

## Match these existing components for style
- The **existing Budget screen** (`BudgetScreen.dc.html` / the Budgets screen in
  the brief) — the base to build on: the Monthly/Weekly segmented pill, the big
  amount card with the green/amber/red progress bar, and the 2-column category
  grid. Keep all of it; we're adding sections above it and a header on it.
- **Category tiles** render everywhere as **color dot / 38×38 rounded tile +
  emoji + name** in the category's muted color — reuse that for a recurring
  payment's category.
- **Bottom-sheet pattern** (`DateRangeScreen.dc.html` / the brief's sheet
  pattern: scrim, drag handle, 28px rounded top, Cancel/Save pill buttons) —
  reuse for the add/edit sheet.
- Buttons are **fully-rounded pills**. Phone preview size **300×620**,
  `font-family: Roboto`.

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`, `--onprimary`; selected/emphasis uses `--secc` / `--onsecc`
- Lines/scrim: `--outv` (dividers), `--outline`, `--scrim`
- Reuse the existing **green / amber / red budget-progress colors** for the
  progress bar and for the breakdown's "left" figure (green when positive, red
  when negative).
- Keep everything token-driven (no hard-coded grays) so it themes light/dark.

## Screens to design

### 1. `BudgetIncomeRecurringScreen.dc.html` — the full screen, populated
The reworked Budget screen with income + recurring set. Top → bottom:
- **Income** card. Header row: "Income" (left) + "€2,400 / mo" (right, `--onv`).
  One item row: a neutral `--secc` tile (income has no category), **"Salary"**,
  subtitle "Monthly · 25th" (`--onv`), amount **"+€2,400"** in the positive/green
  tone on the right. Then a full-width **"＋ Add income"** row (primary-tinted).
- **Recurring payments** card. Header row: "Recurring payments" + "€780 / mo".
  Rows, each = category tile (emoji+color) + name + subtitle "Category · Monthly ·
  day" + amount on the right:
  - 🏠 **Rent** — "Housing · Monthly · 1st" — €650
  - 📺 **Netflix** — "Entertainment · Monthly · 14th" — €13
  - ⚡ **Electricity** — "Utilities · Monthly · 8th" — €117
  - then a **"＋ Add recurring payment"** row.
- **Breakdown** card. Small header "This month". Rows: "Income" +€2,400 (green) ·
  "Recurring bills" −€780 · "Spent so far" −€612 · divider · **"Left to spend"
  €1,008** (emphasized, green). Make it visually **distinct** from the
  spending-budget progress card below — this reads as *cash left this month*,
  that one reads as *budget used*.
- **Spending budget** section: a **new section header "Spending budget"** with a
  one-line hint ("What you plan to spend, on top of your bills.") directly
  **above the Monthly/Weekly pill** — so it's clearly separate from income/bills.
  Below it, the existing pill + amount card (€800, ~76% bar, "€612 spent · €188
  left") + the 2-column category grid (a couple of tiles is enough).

### 2. `BudgetMoneyEmptyScreen.dc.html` — fresh / empty state
The Budget screen for a user who hasn't set income or recurring yet:
- A slim, muted, tappable **"＋ Add income"** row (no card chrome).
- A slim **"＋ Add recurring payment"** row.
- **No breakdown** (no income yet).
- The **Spending budget** section (header + pill + amount + categories) below,
  exactly as today.
- Optional: one quiet one-line explainer at the very top —
  "Add your income and bills to see what's left each month." — include it only if
  it doesn't add clutter.

### 3. `RecurringPaymentSheet.dc.html` — add / edit a recurring payment
A bottom sheet (DateRange pattern): drag handle, title **"Add payment"**. Fields:
- **Name** — text field, placeholder "Rent".
- **Amount** — currency field (€).
- **How often** — a small segmented control / chips: **Monthly · Weekly ·
  Yearly** (Monthly selected).
- **Due day** — the day it's charged (a "1st" field/stepper for Monthly; a
  weekday for Weekly). Keep it simple.
- **Category** — a row that opens the existing category picker; show it set to
  🏠 Housing (color dot + emoji + name).
- **Cancel** / filled **"Save"** pills. Show a subtle **Delete** affordance for
  the edit case.

### 4. `IncomeSheet.dc.html` — add / edit an income source
The same sheet in **income mode**, title **"Add income"**:
- **Name** — placeholder "Salary".
- **Amount** — currency (€).
- **How often** — Monthly · Weekly · Yearly.
- **Payday** — the day it arrives.
- **No category row** (income isn't a spending category) — note the absence vs
  the payment sheet, since these two sheets share a layout.
- **Cancel** / **Save** pills.

## Optional / stretch (nice to explore, not required)

### 5. `BudgetIncomeNoRecurringScreen.dc.html` — income set, recurring still empty
To pin the conditional states: **Income** card populated, **Recurring payments**
collapsed to just the slim "＋ Add recurring payment" row, **Breakdown** visible
(income exists) with "Recurring bills €0", then the Spending budget section.

### 6. Free-tier limit state (can be a note/overlay on screen 1 or its own file)
Free users get **3 recurring items**; premium is unlimited. Show the
**"＋ Add recurring payment"** row in its **at-limit** treatment — a lock glyph +
"Upgrade to add more" (tapping it would open the existing paywall). A small
"3 / 3" hint on the section header is fine if it reads cleanly.

## Copy (current intent — tweak if a shorter phrasing reads better)
- Section headers: **Income**, **Recurring payments**, **Spending budget**
- Spending-budget hint: **What you plan to spend, on top of your bills.**
- Add rows: **Add income**, **Add recurring payment**
- Breakdown: **This month** / **Income** / **Recurring bills** / **Spent so
  far** / **Left to spend**
- Sheet fields: **Name**, **Amount**, **How often** (Monthly / Weekly / Yearly),
  **Due day** / **Payday**, **Category**
- Empty explainer: **Add your income and bills to see what's left each month.**
- At limit: **Upgrade to add more**

Note: the app is localized into 21 languages (German and Bulgarian run long) and
amounts show in the user's currency (EUR by default) — please let headers,
labels, and the breakdown rows wrap/space gracefully rather than truncate.

## Already covered — please DON'T redo
- The **category picker sheet** and the **per-category budget sheet / category
  grid** already exist — reference them, don't recreate them.
- The **Monthly/Weekly pill**, **amount card**, and **progress bar** already
  exist on the Budget screen — keep them; we're only adding the header above the
  pill and the new sections above.

## Output
- Save each as a new `*.dc.html` file in this project (don't overwrite the
  existing Budget screen).
- Keep them dark/light-token driven (use the CSS vars above, no hard-coded grays).
- Phone first (300×620). Once I approve these, I'll ask for matching tablet /
  landscape variants (the app already adapts Budget to a 3–4 column grid and
  centers sheets as dialogs on tablet).

Thanks!
