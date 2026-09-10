# Claude Design request — Needs/Wants/Savings: the "count unspent income as savings?" one-time ask — phone + iOS + tablet

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code reads them back via
> DesignSync and implements the approved ones.

---

Hi! Small but important follow-up to the **Needs / Wants / Savings 50/30/20 split** (see [`NeedsWantsSplit.dc.html`](./NeedsWantsSplit.dc.html), already built). I need a **one-time, in-app choice** that lets each user decide **what counts as "Savings"** in the split — because the honest default surprises savers. Please mock all three surfaces (Android phone M3, iOS 26 Liquid Glass, tablet adaptive).

## Why / the goal
- Today the **Savings bucket** counts only *deliberate* savings — savings-goal transfers and spend tagged to a Savings category. So a user who simply **keeps** a lot of income (spends little on Needs/Wants) but doesn't log goal contributions sees **Savings 0%** with an amber *"−20"* under-target pill — even though they clearly saved. That reads as punishing good behaviour.
- The fix is not to pick a definition for everyone, but to **ask the user once** how they think about it, remember the answer, and let them change it later. Two honest models:
  1. **Count what I keep** — Savings = everything not spent on Needs or Wants (`income − Needs − Wants`). The bar's grey "leftover" becomes green Savings; a saver reads *"Savings 76% · great"*.
  2. **Only money I set aside** — today's behaviour: Savings = goal transfers + Savings-tagged spend; unspent income stays as the grey leftover track.
- **Free**, on-device, no new data collected.

## The feature (three parts)
1. **The one-time ask** — a friendly choice, shown **inline on the split card the first time it has data** (not a blocking modal, not a full screen). Two clearly-labelled options, each with a one-line plain explainer, and ideally a **tiny live preview** of what each does to *this user's* bar (e.g. a mini split bar: option 1 shows the leftover turning green, option 2 shows it staying grey). One tap chooses and dismisses; the split immediately reflects it. It never appears again unless reset.
2. **The two resulting split cards** — the populated `NeedsWantsSplit` card in each mode, so the difference is unmistakable: **(a)** "count what I keep" — Savings segment absorbs the leftover, high Savings %, positive/on-target pill, a warm one-line summary; **(b)** "only set aside" — unchanged from today, grey leftover, Savings from goals only.
3. **The later toggle** — the same choice as a normal switch/row so it's changeable after the one-time ask, living in the Insights **Customize sections** sheet (see [`InsightsCustomizeSheet.dc.html`](./InsightsCustomizeSheet.dc.html)) under a small "Savings" or "How Savings is counted" group. Label + one-line description; reflects the current choice.

## States to draw
1. **The ask, first run** — the choice card in place, above the split's rows (or replacing the summary line), with both options and the mini previews. Calm, not alarmed.
2. **After "count what I keep"** — the full split card with Savings filling the former leftover (green), e.g. *49 / 31 / 20*-style but here something like *16 / 8 / 76* from a real low-spend month; savings pill positive; summary celebrates the high save rate without being smug.
3. **After "only money I set aside"** — the full split card exactly as it ships today (grey leftover, Savings 0–low %), summary in the existing gentle-nudge tone.
4. **The Customize toggle** — the row/switch in the Customize sections sheet, showing the chosen state.
5. **(Optional) a dark variant** of the ask, since the split lives in both themes.

## Platforms & components to match
- **Android phone — Material 3.** Reuse the `NeedsWantsSplit` card frame (`--sc` surface, 20px radius, period chip), the same **bucket accents** (`--nd` / `--wt` / `--sv`) and delta-pill language, and the app's **SegmentedToggle** or a pair of tappable choice cards for the two options. Mini-preview bars reuse the split's 20px stacked-bar style. Phone **300×620**, Roboto.
- **iOS phone — iOS 26 Liquid Glass.** Same choice + previews + resulting cards, native chrome; match [`iOS NeedsWantsSplit.dc.html`](./iOS%20NeedsWantsSplit.dc.html).
- **Tablet — Material 3 adaptive.** The ask sits in the split card within the Insights **two-column grid**; the Customize toggle under the nav-rail Customize sheet. Build from [`TabletNeedsWantsSplit.dc.html`](./TabletNeedsWantsSplit.dc.html).

## Design tokens
- Surfaces `--bg` / `--sc` / `--sch`; text `--on` / `--onv`; dividers `--outv`; primary `--primary` / `--onprimary`; the good/warn pairs already used by the split's pills.
- **Bucket accents** identical to the split — `--nd` Needs, `--wt` Wants, `--sv` Savings (+ their `…c` containers), both themes.

## Please produce
- **`SavingsAllocationAsk.dc.html`** — Android phone: the one-time ask (state 1), both resulting cards (states 2–3), and the Customize toggle (state 4).
- **`iOS SavingsAllocationAsk.dc.html`** — iOS Liquid Glass version.
- **`TabletSavingsAllocationAsk.dc.html`** — adaptive tablet version.

## Implementation notes (for after the mockups)
- New **nullable** setting (e.g. `nwsCountLeftoverAsSavings: Boolean?`): `null` = not asked yet → show the one-time card once the split has data; `true`/`false` = the user's choice. Persisted in `SettingsStore` / `AppSettings` like the other Insights prefs; surfaced in the Customize sheet.
- Derivation (`InsightsViewModel.computeSplit`): when `true`, `Savings = max(0, income − Needs − Wants)` and leftover = 0; when `false` (or the current default), keep today's `Savings = goal contributions + Savings-tagged spend`.
- **Full Android + iOS parity** and tablet parity. Separate effort from the split itself — mockups first, then implement on the existing `feat/needs-wants-split` line.

Thanks!
