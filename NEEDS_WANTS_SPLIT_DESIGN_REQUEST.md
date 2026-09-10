# Claude Design request — Needs / Wants / Savings (50-30-20) split — phone + iOS + tablet

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code reads them back via
> DesignSync and implements the approved ones.

---

Hi! I'd like a new **Insights** feature that splits spending into **Needs / Wants / Savings** and shows it against the classic **50 / 30 / 20** budgeting rule — with a **trend over time** so people can watch their pattern. This framework is having a real moment (finance YouTube / FIRE), and users want to *follow* their savings pattern, not just see one number. Please mock all three surfaces (Android phone M3, iOS 26 Liquid Glass, tablet adaptive).

## Why / the goal
- Budgetty already tags every purchase with a **category**. If each category maps to a **bucket** — Need, Want, or Savings — we can show the 50/30/20 split for free from data we already have.
- This **upgrades** the existing "Where your income goes" fixed-vs-flexible card into a real needs/wants/savings framework with **targets** and **history**.
- **Free** Insights section, toggleable via **Customize sections**, like the others.

## The feature (three parts)
1. **The split card** — for the selected period: a three-segment bar (or three rings) showing **Needs / Wants / Savings** as **% of spending (or income)**, each with its **actual % vs target** (50 / 30 / 20). Make over/under target legible at a glance (e.g. *"Wants 40% · target 30% · +10"*).
2. **The trend** — the split **over time** (stacked % bars per month, or three thin lines), so the user sees their savings share trending up or down. This is the emotional hook — design it to feel rewarding.
3. **Category → bucket tagging** — in **Manage Categories**, each category carries a **Need / Want / Savings** tag (sensible defaults: Rent, Utilities, Groceries, Transport, Insurance, Health = **Need**; Dining, Entertainment, Shopping, Hobbies, Travel = **Want**; Savings goals / transfers = **Savings**), user-editable.

## States to draw
1. **On track** — near 50/30/20 (positive, *"nicely balanced"*).
2. **Over on Wants** — e.g. 45 / 40 / 15; Wants over target, Savings under. Gentle, non-judgy caution tone.
3. **Under-saving** — Savings well below 20%; a soft nudge toward the goal.
4. **Trend view** — 6 months of the split, with the savings share visibly improving.
5. **Setup / not enough data** — before categories are tagged or with too little history: a friendly *"Tag your categories to see your 50/30/20"* with a CTA into Manage Categories. Make it look intentional, not broken.
6. **Manage Categories — tagging row** — a category row showing its **Need / Want / Savings** selector (a 3-way segmented control), so the tagging UX is clear.

## Platforms & components to match
- **Android phone — Material 3.** Card style from **`InsightsScreen.dc.html`** (this replaces / upgrades the fixed-vs-flexible card); reuse the **savings-rate ring** type scale and the Budgets card's status-color language. Tagging row matches **`ManageCategories.dc.html`** + the shipped **SegmentedToggle**. Phone **300×620**, Roboto.
- **iOS phone — iOS 26 Liquid Glass.** Same card + trend + tagging, native chrome.
- **Tablet — Material 3 adaptive.** In the Insights **multi-column** grid the split card and its trend can sit **side by side**; Manage Categories under the **nav rail**. Build from the `Tablet*` Insights treatment, not a stretched phone.

## Design tokens
- Surfaces `--bg` / `--sc` / `--sch`; text `--on` / `--onv`; dividers `--outv`.
- **Three harmonious bucket accents** — one tint each for **Needs / Wants / Savings**, used identically across all three surfaces and both themes (define as CSS vars so they theme in dark + light). Keep them distinct from the category pie palette so the two don't clash.

## Please produce
- **`NeedsWantsSplit.dc.html`** — Android phone: the split card (states 1–3), the trend (state 4), setup (state 5), and the Manage Categories tagging row (state 6).
- **`iOS NeedsWantsSplit.dc.html`** — iOS Liquid Glass version.
- **`TabletNeedsWantsSplit.dc.html`** — adaptive tablet version.

## Implementation notes (for after the mockups)
- New **bucket** dimension on categories (Need / Want / Savings) with a default mapping + user override — a Room migration per our new-category checklist; income basis reuses the recurring-plan income already summed for the period.
- New Insights derivation + card in `InsightsViewModel`; the trend reuses the existing monthly bucketing.
- **Full Android + iOS parity** (budgeting feature) and tablet parity. This is a **separate effort from the telemetry work** — mockups first, then implement.

Thanks!
