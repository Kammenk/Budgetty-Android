# Claude Design request — Android Savings goals (phone)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups; Claude Code reads them back via DesignSync and implements the approved ones.

---

Hi! I'd like to add **Savings goals** to Budgetty (Android phone). A user sets a
goal (name, emoji, target amount, optional target date) and tracks progress toward
it with a ring, topping it up with manual contributions. Please mock this up as new
`*.dc.html` files, matching the existing Material 3 design system exactly. **iOS is
getting the identical feature** (`IOS_DESIGN_REQUEST_SAVINGS_GOALS.md`) — same
behavior, native Liquid Glass visuals.

## Why / the goal
- A motivational, planning-side feature ("New laptop €1,200", "Emergency fund
  €3,000"). Budgetty has budgets, income and bills but nothing forward-looking for
  *saving*.
- Budgetty has **no account balances** — so a goal is a **manual tracker**: the
  user adds/withdraws contributions themselves; we don't pull from a real balance.
  Keep the tone honest about that (it's a savings *tracker*, not a wallet).

## Gating (please show the locked state)
**Capped-free: 1 goal free, unlimited with Premium** — the same freemium pattern as
widgets (2 free) and custom categories (3 free). A free user with one goal who taps
"New goal" hits a **paywall nudge**. Please design the **locked "Add goal"** state
(a Premium lock affordance + short "Unlock unlimited goals" prompt).

## The data (drives what the cards show)
A goal = **emoji + name + target amount + optional target date**, and a
**saved amount** built from **contributions** (dated add/withdraw entries, so there's
a little history). Derived: **remaining = target − saved**, **% = saved / target**,
and if a target date is set, **"€X/month to reach by {date}"** plus an on-track /
behind hint.

## Match these existing components for style
- **`BudgetScreen.dc.html`** — Savings goals live as a **new section on the Budget
  tab**, below the budget / income / recurring sections. Match its section headers,
  cards and rows.
- **Budgets progress card ring** (on `HomeScreen.dc.html`) and the **Insights
  donut** — reuse that ring/arc style for the goal progress ring.
- **Category emoji chips** — the goal's emoji uses the same rounded-square emoji
  chip badge as categories.
- **The paywall / premium lock** (`AccountPaywallScreen.dc.html` / the widget-cap
  lock) — reuse for the 1-goal cap.
- Phone preview **300×620**, `font-family: Roboto`. Material 3, **not** iOS Liquid Glass.

## Screens / states to draw
1. **Savings section — empty:** the section on the Budget tab with no goals yet — an
   inviting *"Set a savings goal"* empty card + primary CTA.
2. **Savings section — one goal in progress:** a **goal card** (emoji chip, name,
   progress **ring** with %, *"€480 of €1,200 · €720 to go"*, optional
   *"€120/month to reach by Dec"*).
3. **Savings section — multiple goals (Premium):** 2–3 goal cards stacked, plus a
   small total-saved summary at the section header.
4. **Locked "New goal" (free user at cap):** the add-goal affordance in its Premium-locked state + "Unlock unlimited goals" nudge.
5. **Goal detail** (opened from a card): large ring, saved/target/remaining, the
   **contribution history** list (dated +/− rows), and actions **Add to savings** /
   **Withdraw** / **Edit** / **Delete**.
6. **Add-to-savings sheet:** amount input (currency suffix), optional note, date; a Withdraw variant (negative).
7. **Create / edit goal sheet:** emoji picker chip, name, target amount, optional
   target date (toggle → date). 
8. **Goal reached:** a goal at 100% — celebratory treatment (*"Goal reached! 🎉"*), ring full.

## Design tokens (CSS vars already defined)
- Surfaces `--bg` / `--sc` / `--sch`; text `--on` / `--onv`; accent `--primary`;
  positive/savings **green** for progress and contributions; dividers `--outv`.

## Please produce
- **`SavingsGoalsScreen.dc.html`** — the Budget-tab Savings section in states 1–4
  (empty / one goal / multiple / locked).
- **`SavingsGoalDetailScreen.dc.html`** — goal detail (state 5) + the add/withdraw
  sheet (6) + the create/edit sheet (7) + the reached state (8).
- Token-driven only so they theme in **dark + light**. Phone first; tablet to follow.

## Implementation notes (for after the mockup)
- **New data model:** a `SavingsGoal` entity + a `SavingsContribution` entity (Room
  migration), a repository + `SavingsViewModel`. `saved = Σ contributions`.
  Nothing pulls from a balance (the app has none).
- **Gating:** free = 1 goal; the 2nd `New goal` routes to the paywall, same check as
  the widget/custom-category caps (`billingManager.isPremium`).
- **"€X/month to reach by {date}"** = remaining ÷ whole months to target date.

Thanks!
