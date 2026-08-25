# Claude Design request — Retention #1: Weekly Recap as a rhythm + outcome Streaks (Android, iOS, tablet)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project (id `5b8c8470-38ec-49d0-b332-b27a9000b4b0`).
> It will create the new `*.dc.html` mockups; once they're there, Claude Code
> reads them back via DesignSync and implements what I approve. Please
> **don't overwrite** the shipped `RecapStoryScreen.dc.html` — create **new**
> comparison files.

---

# Retention #1 — Weekly Recap + Streaks (2026-08-25)

Hi! This is the first of three design requests for a **subtle retention pass** across Budgetty. The guiding rule for **all three**: this is a *money* app, so every mechanic rewards a **real financial outcome**, never app usage — and there is **no loss framing** anywhere (no "streak broken", no flames, no countdowns, no "don't lose your progress"). Nothing here is behind the paywall.

The **Recap** is Budgetty's Wrapped-style, swipeable end-of-period story (shipped as `RecapStoryScreen.dc.html` — a `HorizontalPager` of one-figure cards on tonal band backdrops, with a top progress bar, a ✕, and Done / See-details on the final card). Today it defaults to **monthly only**, so it fires ~12×/year. We're turning the **weekly** recap into a real rhythm and adding an **outcome-streak** card. Before I build it, I want to **see it**.

**What I'm asking for:** a **side-by-side comparison mockup** per platform.
- **Left column = today's shipped recap** (the baseline; nothing moves here).
- **Right column = proposed** — the fuller weekly sequence, the new Streak card, the de-flamed streak language, and the in-story frequency control.
- **Numbered pins** on each change on the right, with a short **legend** (number → one line) underneath, so I can approve/reject one at a time.
- **Three surfaces, one comparison file each:** Android phone (Material 3), iOS phone (Liquid Glass), Android tablet (the recap centres its content column ~460dp wide and is already tablet-aware — draw it centred, not stretched).

## The streak visual language (shared — the Wellbeing & Limits requests reference this)
Streaks appear in three places across the app; they must read **identically** everywhere and must obey these rules:
- **Per-scope, not all-or-nothing.** A streak is "**3 weeks under your Groceries budget**" or "**4 months under budget**" — one scope at a time. We surface the best one or two.
- **Only ever shown at `current ≥ 2`.** A 1-period streak is just "this period" and is never shown.
- **Closed periods only.** The number counts completed periods. The in-flight period is **never** in the number — at most a **dotted / ghost segment** meaning "on track so far".
- **No loss framing, no flames, no countdowns, no "at risk".** A positive streak is stated plainly.
- **Best-run fallback.** When the current run is 0 but the personal best was > 0, show **only** `Best run: 6 months` (labelled "best in the last 24 months"). No consolation copy, no explanation — the absence of a current number is the whole signal.

Please design a **small streak motif** that carries this (e.g. a short row of filled dots/segments = closed periods met, plus one dotted/ghost = the live on-track period) — calm, not celebratory, and unmistakably **not** a Duolingo flame.

## State to draw (right column)
Draw the weekly story primarily in a **data-rich week** so the new cards appear, plus a **bare week** so I can confirm nothing pads:
- **Rich week:** Cover → Pace → **Limits** → **Streak** → Focus (5 cards, under 10s to swipe).
- **Bare week:** Cover → Pace → Focus (3 cards — Limits & Streak dropped out entirely).

## Changes to show on the "proposed" (right) side
1. **New Streak card** (weekly). One-figure card in the streak visual language: hero line **"3 weeks under your Groceries budget"**, the small streak motif beneath (3 filled + 1 dotted "on track"), on a calm tonal band (reuse a GOOD/SECONDARY band, **not** a celebratory one). Draw the **Best-run fallback** variant too (`Best run: 6 weeks`, no current number).
2. **De-flame the existing monthly Streak card.** The shipped `BudgetStreak` card currently ends its hero with a **🔥** ("3rd month under budget 🔥") — that violates the no-flames rule. Show it refreshed to the shared streak language: same panel (under-count of scopes + the segment bar + safe-to-spend), hero **without** the flame, streak motif instead.
3. **Fuller weekly sequence.** Today the weekly story is only Cover → Pace → Focus. Show the weekly story gaining the **Limits card** (it already exists for monthly — same visual, now populated for the week: "Stayed under 3 of 4 limits", the emoji chip rows) and the new **Streak card**, and confirm both **drop out cleanly** on a bare week (draw that second sequence).
4. **In-story frequency control.** On the weekly **Focus** card (the last card), a **low-emphasis** text row near the bottom: **"Weekly recaps · Change"**. Tapping it opens a **small sheet** with four options — **Weekly / Monthly / Both / Off** — a plain radio/selectable list, dismissible. Draw the Focus card with the row, and the sheet open. This is the off-switch that makes turning weekly recaps on-by-default safe, so it must be discoverable but never shouty.

## Platforms & components to match
- **Android** — Material 3, Roboto, phone preview **300×620**. Build the "current" column from the shipped `RecapStoryScreen.dc.html` (band-backdrop cards, top segmented progress bar, ✕ + kind tag, Done / See-details). **Not** Liquid Glass.
- **iOS** — iOS 26 **Liquid Glass**, SF Pro. The recap is a full-screen native story; keep the ✕, progress bar, and swipe model but render it iOS-native (the story cards are full-bleed, so this is mostly type + the Liquid-Glass sheet for the frequency control).
- **Android tablet** — same story, content **centred** in the ~460dp column on the band backdrop (it is **not** a nav-rail/multi-column screen — it's a full-screen interstitial). Just confirm the centred sizing and that the frequency sheet is a centred dialog, not edge-to-edge.
- Keep all three in **parity** — the streak motif, the card sequence, and the frequency sheet read the same on each; only platform chrome differs.

## Design tokens (CSS vars already in the project)
- Band backdrops reuse the recap's existing tonal bands: `--primary-container` / `--secondary-container` and the wellbeing band containers (good / warn / great). Text uses the band's `on-` role.
- Streak motif: filled segments in the **good** tone (`budgetGoodColor`), the live/on-track segment as a **dotted or 40%-alpha ghost** of the same — never a status red, never amber.
- Pills/figures reuse the recap's `Kicker` / `BigFigure` / `RecapPill` (GOOD = green, WARN = accessible amber `wellbeingWarnOn`) idioms.
- Token-driven only (no hard-coded greys) so everything themes in **dark + light**.

## Please produce
- **`RetentionRecap.dc.html`** (Android phone), **`iOS Retention Recap.dc.html`** (iOS phone), **`TabletRetentionRecap.dc.html`** (Android tablet) — each a **current vs proposed** comparison covering the **rich-week sequence, the bare-week sequence, the new Streak card (+ Best-run variant), the de-flamed monthly Streak card, and the Focus-card frequency row + sheet**, with **numbered pins + a legend**, themed **dark + light**.

## Notes (mostly presentation)
The recap is a stateless render of a prebuilt story; the numbers already exist. Two behavioural facts to design around, not solve:
- **A weekly recap under 3 receipts is skipped** (a bare week shows the 3-card sequence, never a hollow one).
- **No backfill:** a user who's away for three weeks sees **one** weekly recap, never a queue.

Streaks come from a new pure `StreakEngine` (no schema change; derived from transactions/budgets). **No new database fields for anything in this request.**

Thanks!
