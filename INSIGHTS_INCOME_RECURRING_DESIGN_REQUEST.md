# Claude Design request — Insights: income & recurring-payment blocks

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and implement the approved ones.

---

Hi! The app now tracks **income sources** and **recurring payments** (fixed
bills) on the Budget screen. I want to put that data to work on the **Insights
screen** as a few new insight cards. Please mock up the blocks below as new
`*.dc.html` files, matching the existing design system exactly.

## Context — how Insights works today
Insights is a scrollable stack of **section cards** under a **period stepper**
(the user steps through **Week / Month / Quarter / Half / Custom**, backward and
forward). Existing sections cover **spending**: total spent, trends, top
categories, a category pie, period-over-period comparison, and budget-vs-actual.
There is a **"Customize sections"** sheet to show/hide and reorder sections. The
new blocks should slot in as **more section cards** that obey the same period and
the same customize framework.

Two constraints to design around:
- **Income & bills are planning data**, not a time-series — there's no historical
  "income per day." So these blocks are **current-plan vs. actual-spend**
  comparisons and **forward-looking** views, **not** line charts of income over
  time.
- **Everything respects the period stepper.** Spending is already period-scoped;
  income and bills are stored as monthly figures, so when the period isn't a
  month they should **scale to match** (a Week shows ~1 week of income/bills, a
  Quarter ~3 months) so the in/out numbers reconcile. Label the period on each
  card (e.g. "This month").

## The blocks to design (this is the brainstorm — mock the CORE ones, explore the STRETCH ones)

### CORE

#### 1. `InsightsIncomeVsSpending.dc.html` — Money in vs. money out
The headline block. For the period: **money in** (income) vs. **money out**
(actual spend), and the **net**.
- A compact two-bar or in/out layout: **In €2,400** (green) · **Out €1,820**
  (primary/neutral), with a bold **"Net +€580"** (green when positive, red when
  negative) and a one-line plain-language read: **"You kept €580 this month."**
- If out > in, net is negative and red: **"You spent €180 more than you earned."**

#### 2. `InsightsSavingsRate.dc.html` — Savings rate
How much of income the user kept: **(income − spend) ÷ income**, as a big **%**
with a ring/gauge or a slim progress bar.
- Big number **"24%"**, label **"of your income saved"**, subtitle **"€580 of
  €2,400"**. Color the ring by health (e.g. green ≥20%, amber 0–20%, red < 0).
- Show a negative/overspent state too (e.g. **"−8%"**, red, "You spent more than
  you earned").

#### 3. `InsightsFixedVsFlexible.dc.html` — Where your income goes
A single stacked bar of income split into three parts:
**Fixed bills** (recurring) · **Flexible spending** (everything else spent) ·
**Left**.
- Legend rows with amounts + % of income: **Fixed €780 (33%)**, **Flexible €1,040
  (43%)**, **Left €580 (24%)**.
- One-line read: **"Bills take a third of your income."** This reframes recurring
  payments as a share of what comes in — very actionable.

#### 4. `InsightsUpcomingBills.dc.html` — Upcoming bills
Forward-looking, uses each recurring payment's due day. **"€430 due in the next 7
days"** as a header number, then a short list of the nearest bills:
- 📺 **Netflix** — "in 2 days" — €13
- ⚡ **Electricity** — "in 5 days" — €117
- 🏠 **Rent** — "in 6 days" — €650 *(if within the window)*
Each row = category tile + name + a relative "in N days" (`--onv`) + amount. Cap
the list (~4) with a quiet "+ N more." Consider a 7-day and a 30-day variant, or a
small toggle — your call.

### STRETCH (explore if they fit; not required)

#### 5. `InsightsIncomeBySource.dc.html` — Income breakdown
Only meaningful with **multiple** income sources. A small breakdown (mini-pie or
stacked bar + legend): **Salary €2,400 (80%)**, **Freelance €600 (20%)**. Include
the **one-time** cadence in an example (a "Bonus €600 · Once" source) since a
shift worker's variable pay is exactly why this matters. If there's only one
source, this card should hide itself (note that).

#### 6. `InsightsBiggestBills.dc.html` — Your biggest bills
Recurring payments ranked largest-first, each with a slim bar relative to the
biggest: **Rent €650**, **Electricity €117**, **Netflix €13**. Helps users spot
what to cut.

## Layout & style
- Each block is a standalone **section card** in the existing Insights rhythm
  (rounded `--sc` card, bold title, generous padding), stackable in any order.
- Reuse existing **category tiles** (emoji + muted color), the **green/amber/red**
  health colors, and the app's **bar / ring** styling from the existing spending
  insights so these look native next to them.
- Buttons/chips are **fully-rounded pills**. Phone preview **300×620**,
  `font-family: Roboto`.
- Also show each card in the **"Customize sections" sheet** as a toggle row (reuse
  the existing customize-sheet styling) so it's clear these join that framework —
  a single combined screenshot of the sheet listing the new sections is enough.

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`, `--onprimary`; emphasis `--secc` / `--onsecc`
- Lines: `--outv`, `--outline`
- Green (positive/saved) / amber (caution) / red (overspent/negative) — reuse the
  existing budget-health palette.
- Keep everything token-driven (no hard-coded grays) so it themes light/dark.

## Copy (current intent — tweak if shorter reads better)
- Money in/out: **Money in / Money out / Net** · read: **"You kept €580 this
  month." / "You spent €180 more than you earned."**
- Savings rate: **"of your income saved"** · **"€580 of €2,400"**
- Fixed vs flexible: **Fixed bills / Flexible spending / Left** · read: **"Bills
  take a third of your income."**
- Upcoming bills: **"€430 due in the next 7 days"** · relative **"in N days"**
- Income by source / Biggest bills: section titles **"Income by source" /
  "Biggest bills"**

## Empty / edge states to show (a note or mini-panel per card is fine)
- **No income set** → the money-in/out, savings-rate and fixed/flexible cards
  can't compute. Show a quiet nudge: **"Add your income in Budget to see this."**
  with a link, rather than zeros.
- **No recurring payments** → Upcoming bills + Biggest bills show a small
  **"No recurring bills yet"** state.

## Already covered — please DON'T redo
- The **period stepper**, **spending** insight cards (total, trends, top
  categories, pie, comparison, budget-vs-actual) and the **Customize sections**
  sheet already exist — reference them; only add the new income/bill cards and
  show them inside the existing customize sheet.
- **Income / recurring rows and their edit sheets** already exist on Budget —
  reuse their tile/amount styling.

## Output
- Save each as a new `*.dc.html` file.
- Keep them dark/light-token driven (use the CSS vars above).
- Phone first (300×620). Once approved I'll ask for tablet/landscape variants
  (Insights already goes to a multi-column dashboard on tablet/landscape).

Thanks!
