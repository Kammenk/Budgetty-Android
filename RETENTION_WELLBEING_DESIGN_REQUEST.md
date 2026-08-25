# Claude Design request — Retention #2: Wellbeing score as meta-progression (Android, iOS, tablet)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project (id `5b8c8470-38ec-49d0-b332-b27a9000b4b0`).
> It will create the new `*.dc.html` mockups; once they're there, Claude Code
> reads them back via DesignSync and implements what I approve. Please
> **don't overwrite** the shipped `WellbeingScreen.dc.html` — create **new**
> comparison files.

---

# Retention #2 — Wellbeing as meta-progression (2026-08-25)

Hi! Second of three retention requests (same rules as #1: reward real financial outcomes, **no loss framing**, nothing paywalled). This one makes the **Wellbeing score** feel like something you can move on purpose, instead of a thermometer you can only read.

**Today** the Wellbeing screen (shipped `WellbeingScreen.dc.html`) shows: a **score ring hero** with a band word and a small **trend chip** (just `↑4` vs the *one* prior month), a **components section** (Budget / Trend / Subscriptions / Goals rows, each with a "why" line and a deep link), a **weekly pace card**, a **tips feed** (dismissible cards with a tone, a title, a detail line quoting a real figure, and a "fix it" CTA), a **wins strip**, and a **focus card**. Two gaps: there's **no trajectory** (one prior point isn't a trend), and tips say *what's wrong* but never *how much fixing it is worth*.

**What I'm asking for:** a **side-by-side comparison mockup** per platform — **left = shipped Wellbeing screen**, **right = proposed** with the four changes pinned. **Numbered pins + a legend**. **Three surfaces, one file each:** Android phone (Material 3), iOS phone (Liquid Glass), Android tablet (adaptive — nav rail + the screen's cards in a multi-column grid at ≥600dp).

## State to draw (right column)
Draw a user with **~6 months of history** and a score **near a band boundary** so every new element is visible at once:
- Score **57** ("Getting there"), **3 points to Healthy** (the 60 boundary).
- A **rising** six-month trajectory (e.g. 49 → 57), so the sparkline reads clearly.
- A couple of **actionable tips** with a worthwhile projected gain, and at least one **win-tone** tip with **no** pill.
- The **Budget** component carrying a streak ("Groceries — 4 months under").
Also draw the **thin-data** state (fewer than two stored months): the sparkline area renders **nothing at all** — no placeholder, no "not enough data yet" card.

## Changes to show on the "proposed" (right) side
1. **Trend line (six-point sparkline).** Replace the lone trend chip with a small **hand-rolled sparkline** of the last **six closed months**, plus the **in-flight month as a ghost/hollow point** at the end. One plain sentence under it: **"Up 8 since March."** Use the app's existing chart idiom (the Insights trend bars' visual family) — calm, no gridlines, no axis clutter. Show where it sits relative to the score ring (I lean toward directly under the hero, above the components — but show your recommendation).
2. **"What this is worth" tip pills.** On each actionable tip row, a small pill: **"+6 to your score"**. It's a *modelled* number (what the score would be if you took that one action). Draw it as a quiet accent pill on the tip card, not a loud badge. **Suppress the pill entirely** when the projected gain is small (< 2) or non-positive — show one tip **with** a pill and one **without** so the difference is clear. Win-tone tips (nothing to act on) never get a pill.
3. **Rank tips by projected gain.** With pills in play, the **top tip should be the most impactful actionable one**, not just the most severe. Draw the feed ordered so the biggest-gain tip leads (this is ordering, not a new element — just make sure the mock reflects it).
4. **Band-up nudge.** When the score is within **3 points of a band boundary** (40 / 60 / 80), the **header** shows a concrete near-term target: **"3 points to Healthy"**. Draw it beside/under the band word in the hero. Suppress it otherwise (show the healthy-state hero without it, for contrast).
5. **Streak evidence under Budget (shared streak language).** Under the **Budget** component row, list streaks as evidence: **"Groceries — 4 months under"** (and maybe a second scope). Use the **exact streak motif from the Recap request** — calm dots, **no flames**, `current ≥ 2` only, Best-run fallback if the current run is 0. This should read as supporting detail for the Budget sub-score, not a new hero.

## Platforms & components to match
- **Android** — Material 3, Roboto, phone **300×620**. Build "current" from `WellbeingScreen.dc.html` (score ring hero, component rows, tip cards, wins/focus). **Not** Liquid Glass.
- **iOS** — iOS 26 **Liquid Glass**, SF Pro. Build "current" from the iOS Wellbeing mockup; keep it native (the sparkline and pills in the Liquid-Glass card idiom, not a Material port).
- **Android tablet** — Material 3 **adaptive**: left **nav rail**, the Wellbeing cards in a **multi-column grid** at ≥600dp. The **hero + sparkline span the top** (full content width), not tucked in one column. Sparkline, pills, nudge, and streak evidence are identical to phone — only the surrounding layout adapts.
- Keep all three in **parity**.

## Design tokens (CSS vars already in the project)
- Score ring / band colours as the screen uses today (needs-work / getting-there / healthy / thriving). The **band-up nudge** text uses the **accessible warn/near tone** for legibility, not the bright band hue.
- Sparkline stroke in `--primary` (or the score's band tone); the in-flight ghost point at reduced alpha / hollow. Reuse the Insights trend colour family — no new palette.
- Tip pill: a quiet **positive** accent (`budgetGoodColor` container at low alpha) — it's an *upside*, so green-family, never a status red.
- Streak motif tokens: see the Recap request (good tone + dotted ghost).
- Token-driven only; theme **dark + light**.

## Please produce
- **`RetentionWellbeing.dc.html`** (Android phone), **`iOS Retention Wellbeing.dc.html`** (iOS phone), **`TabletRetentionWellbeing.dc.html`** (Android tablet) — each a **current vs proposed** comparison covering the **sparkline (+ thin-data empty), tip pills (with/without), band-up nudge, and Budget streak evidence**, with **numbered pins + a legend**, themed **dark + light**.

## Notes (one small schema change; the rest is presentation)
- The **trend line needs stored history** — the score is snapshotted per closed month (a new `wellbeing_scores` table, DB v25 → **v26**), because recomputing an old month against today's budgets would rewrite the past and make the line lie. So the sparkline may legitimately be **short** for a while (that's why the thin-data state shows nothing). Design for "grows over time".
- The **"+X to your score"** number is a *modelled delta under an explicit assumption*, not a promise — a genuinely good action can occasionally model to ≤ 0 (a renormalisation quirk), and in that case **the pill is hidden**. So never draw a "−X" pill.
- Everything else (pills, nudge, ranking, streak evidence) derives from data already on the device. No other new fields.

Thanks!
