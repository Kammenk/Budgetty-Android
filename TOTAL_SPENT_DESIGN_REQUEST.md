# Claude Design request — Better "Total spent" on the Home screen (phone + tablet, both platforms)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project (id `5b8c8470-38ec-49d0-b332-b27a9000b4b0`).
> It will create the new `*.dc.html` mockups; once they're there, Claude Code
> reads them back via DesignSync and implements the approved one. This same
> brief is already saved in that project as **`Total Spent Home Brief.md`**.

---

# Better "Total spent" on the Home screen — design brief (2026-08-10)

**Requested mockups:** fresh `*.dc.html` explorations of how the Home summary / "Safe to spend" area presents **"total spent this cycle"**, for **Android phone (Material 3)**, **iOS phone (Liquid Glass)**, and **tablet**. Please save as **new** files — don't overwrite `HomeScreen.dc.html` / `iOS Home.dc.html` / `TabletHomeScreen.dc.html` / `SafeToSpendScreen.dc.html`.

## Goal
The Home "Safe to spend" card has grown dense. Around the hero figure it now carries **Spent**, **Bills still due**, an **"already paid"** sub-line, and a **"Total spent this cycle"** row. The one number users most often want at a glance — *how much money has actually left my account this pay-cycle* — is currently the quietest line on a busy card. I'd like new ideas for displaying **total spent** so it's glanceable and its relationship to safe-to-spend and bills is obvious. Explore for Android, iOS, and tablet.

## What "total spent" means here (please get this right — it's the crux)
Budgetty reserves the whole pay-cycle plan against income:
- **Safe to spend = Income − Total spent this cycle − Bills still due.**
- **Total spent this cycle = discretionary spending so far + recurring bills already paid.** A bill counts as paid either manually or by **Autopay** (it auto-marks paid once its due day passes). So this is the money that has genuinely left the account.

Worked example (the real shipped values): Income **€2,000**, discretionary **Spent €0**, **Bills still due €500**, **€60 already paid** → **Total spent this cycle €60** → **Safe to spend €1,440**.

The subtlety to make legible: **"Spent" (€0, discretionary)** and **"Total spent this cycle" (€60, which includes the €60 auto-paid electricity bill)** are *different* numbers, and today that's easy to misread. A good design should make the composition — *discretionary spend + paid bills = total spent* — clear without a paragraph of explanation.

## Current state (what we're improving)
See **`SafeToSpendScreen.dc.html`** (the shipped card). Top to bottom it stacks:
1. "Safe to spend" hero € + per-day secondary ("€65.45/day for 22 days · resets 1 Sep") + a thin spent-vs-income proportion bar.
2. Two columns: **Spent · {month}** and **Bills still due** (with "€X already paid" beneath the latter).
3. A **"Total spent this cycle €X"** summary row.
4. A footnote: "Both come out of €2,000 income this cycle."

It reads like a mini balance sheet — several numbers of similar visual weight, and "total spent" (the takeaway) has the least emphasis.

## Directions to explore (lay them side by side to compare)
Please try a few genuinely different takes, not just restyles:
- **A — Total spent as a stat with a composition bar (worth trying first):** promote "Total spent this cycle" to a clear stat, with a small **segmented bar** showing its make-up (discretionary spend vs paid bills), and safe-to-spend as the remaining share of income. One glance = spent / paid / still-due / safe.
- **B — One "money this cycle" flow bar:** a single horizontal bar + legend for the whole cycle — **Income = (spent + paid bills) + (still due) + (safe to spend)** — so total spent is a clearly-labelled segment of the whole.
- **C — Spent-first card:** a compact variant whose hero **is** "Total spent this cycle", with safe-to-spend demoted to a secondary line — the mirror of today's safe-to-spend-first card, for users who think spend-first.
- **D — Cleanup only (low-risk):** keep today's structure but resolve the "Spent €0 vs Total spent €60" confusion — relabel/group, or add a tiny "incl. €60 paid bills" hint.

## Platforms & components to match
- **Android phone** — build on `HomeScreen.dc.html` + `SafeToSpendScreen.dc.html`. Material 3 (Material You), Roboto, phone preview **300×620**. **Not** Liquid Glass.
- **iOS phone** — build on `iOS Home.dc.html` + `iOS Safe to Spend.dc.html`. iOS 26 **Liquid Glass**, SF Pro; keep it native-iOS, not a Material port.
- **Tablet** — `TabletHomeScreen.dc.html` / `HomeScreenTabletPortrait.dc.html`. There's much more width here — show how the composition reads when it can go **horizontal** (stat + bar side by side) instead of stacked.
- Reuse the safe-to-spend **green/amber/red** status language and the existing card radius / hero type scale. See also **`Home Bills Summary Explorations.dc.html`** for the planned-bills treatment.

## States to draw (per platform)
1. **Healthy** — the €2,000 / €0 spent / €500 due / €60 paid / €1,440 safe example above.
2. **Mid-cycle** — real discretionary spend plus a couple of paid bills (e.g. €420 discretionary + €180 paid = **€600** total spent), so the composition bar has two substantial segments.
3. **Nothing spent yet** — €0 total spent (first days of the cycle). The "total spent" treatment must look intentional at zero, not broken.

## Design tokens (CSS vars already defined in the project)
- Surfaces `--bg` / `--sc` (surfaceContainer) / `--sch` (surfaceContainerHigh); text `--on` / `--onv` (muted labels); accent `--primary`; safe-to-spend status greens/ambers/reds exactly as the card uses them; dividers `--outv`.
- Token-driven only (no hard-coded grays) so everything themes in **dark + light**.

## Please produce
- New `*.dc.html` file(s) — the directions above, in the three states, for **Android phone, iOS phone, and tablet**. One file per platform is fine (e.g. `HomeTotalSpent.dc.html`, `iOS Home Total Spent.dc.html`, `HomeTotalSpentTablet.dc.html`).

## Implementation notes (no data-model work)
Everything is already computed on both platforms' Home view-models: income, discretionary **spent**, **bills paid** (manual + Autopay via `isEffectivelyPaidThisCycle`), **bills still due**, and **safe-to-spend** — all over the pay-cycle window (`monthStartDay` / `PayCycle`). This is purely a **presentation** change to the existing Home card: no new fields, no schema change.

Thanks!
