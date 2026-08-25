# Claude Design request — End-of-period Recap (weekly / monthly, on app open) — TWO directions to compare (Android phone)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project (id `5b8c8470-38ec-49d0-b332-b27a9000b4b0`).
> It will create the new `*.dc.html` mockups in that project; once they're there,
> Claude Code reads them back via DesignSync and implements the approved ones.

---

Hi! I'd like to add an **end-of-period Recap** to Budgetty — when a week or month
closes, the next time the user opens the app they get a **recap screen** that tells
them, at a glance, **whether their spending improved** and what to focus on next.
Think of it as a periodic **report card** that reuses what the **Insights** and
**Wellbeing** screens already compute. **iOS gets the identical feature** — once a
direction is approved I'll spin a matching iOS (Liquid Glass) request; behaviour must
be the same on both platforms.

**Important — I want to compare two directions before committing.** Please mock the
**same recap, with the same data, two ways**, so I can pick:
- **A — a scrollable recap screen** (calm, reuses Insights cards).
- **B — a Wrapped-style story** (full-screen swipeable stat cards, à la Spotify Wrapped).

Build **both**, with the **same July numbers**, so it's a fair A/B.

## What the feature does (context)
- Shown **once per completed period, on first open on/after the boundary**, then
  dismissed until the next period — it **never nags**. It's a full-screen moment the
  user can dismiss to Home (modelled on the post-signup quiz gate).
- **Enabled from Account**, where the user picks **Weekly**, **Monthly**, or **Both**.
  Default: **Monthly on, Weekly off**.
- **Monthly follows the pay-cycle month** (`monthStartDay`); Weekly on the first day of
  the week.
- Content mirrors **Insights + Wellbeing**: spend vs the previous period, category
  movers, budget / safe-to-spend outcome, receipts & items, savings-goal progress,
  **buying-limits outcome** (ties into the sibling feature), one **highlight** and one
  **tip**.
- Tone is **constructive, never shaming** — a worse month is framed as "here's the one
  thing to change," not a telling-off.

## Monthly vs Weekly — how they differ (important)
Budgetty's **0–100 wellbeing score is a monthly measure** (weekly only has a pace card
+ tactical tips today), so:
- **Monthly recap = the full report card**: the **wellbeing score + its change**, budget
  adherence, top movers, wins / streak, one focus for next month.
- **Weekly recap = a lighter momentum check**: **spend vs last week** + a pace bar, top
  1–2 movers, one tactical tip. If the score appears at all it's **small / secondary**
  with a *"72 · updates monthly"* note (mirroring how the Wellbeing screen treats
  weekly). Don't re-grade the score on noisy weekly data.

## The content blocks (same in both directions)
For the **July monthly example** (an improved month):
- **Hero — did you improve?** *"You spent **€1,240** in July · **12% less** than June ✓"*
  with an up/down indicator and constructive colour.
- **Wellbeing score** *"**72** · Healthy · **↑4** vs June"* (monthly only).
- **Top movers** — biggest category changes: *Dining **−€40**, Groceries **+€25***.
- **Budget outcome** — *"Under budget in **5 of 6** categories · Safe-to-spend ended
  **+€60**."*
- **Activity** — *"**34** receipts · **210** items scanned."*
- **Savings goals** — *"'New laptop' **€480 / €1,200** (+€120 this month)."*
- **Buying limits** — *"Stayed under **3 of 4** limits."*
- **One highlight** *(green win)* + **one tip** *(carry-forward focus)*: *"Keep dining
  under €150 to lift your score to ~76."* + a **"See details"** CTA into Insights.
- **Optional streak** — *"3rd month under budget 🔥."*

## Direction A — scrollable recap (`RecapScrollScreen.dc.html`)
A single full-screen scroll (top app bar with the period, e.g. **"July"**, + a close ✕),
the hero pinned at top, then the blocks above as **Insights-style cards**, ending with
the tip + **"See details"** and a **"Done"**. States:
1. **Monthly · improved** — the fully-populated July example above (spend down, score ↑).
2. **Monthly · tougher month** — spend **up 9%**, score **↓5**, over budget in 3
   categories — same layout, **constructive** framing (no red-shaming; lead with the one
   fix).
3. **Weekly · light** — "This week **€280** · **↓8%** vs last week" pace card + 2 movers +
   one tip; secondary *"72 · updates monthly"*.
4. **First-run / not enough data** — friendly *"Your first monthly recap will be ready at
   the end of {month}"* (or a partial recap without the comparison), `isLoaded`-gated, no
   cold-start flash.

## Direction B — Wrapped-style story (`RecapStoryScreen.dc.html`)
A **swipeable full-screen card sequence** with a segmented **progress bar** at the top
(tap right / swipe to advance, tap left to go back), each card **one big stat** on a bold,
band-coloured backdrop. Monthly sequence (improved):
1. **Cover** — *"Your July, in review"*.
2. **Total spent** — huge *"€1,240"* · *"12% less than June 🎉"*.
3. **Wellbeing score** — big ring *"72 · Healthy · ↑4"*.
4. **Biggest move** — *"Dining was your biggest drop — €40 less."*
5. **Streak / budget** — *"3rd month under budget 🔥."*
6. **Buying limits** — *"You stayed under 3 of 4 limits."*
7. **Next month** — one focus tip + **"Done"** / **"See details"** → Insights.

States: the **improved** sequence above, one **tougher-month** card treatment (to see the
constructive tone in the bold format), and a **short weekly** sequence (3–4 cards: cover →
spend vs last week → one tip). **Same July numbers as Direction A.**

## The Account control (`RecapSettings.dc.html`)
The **Account** row that turns this on + picks cadence: an **"End-of-period recap"** entry
with a **Weekly / Monthly / Both** choice (a `SegmentedToggle`, or the dropdown pattern
used by auto-lock). Draw the **default** (Monthly on, Weekly off) and the **Both** state.
Match the Account `SettingRow` / toggle rhythm.

## Match these existing components for style
- **Wellbeing screen** — reuse the **score ring**, the bands (**0–39 Needs work / 40–59
  Getting there / 60–79 Healthy / 80–100 Thriving**) and the budget **green / amber / red**
  language.
- **Insights** — reuse the **breakdown donut / movers / trend** card styles for Direction
  A's blocks.
- **`SegmentedToggle`**, **fully-rounded pills**, Account **`SettingRow`** for the settings
  frame.
- Phone preview **300×620**, `font-family: Roboto`. Material 3, **not** iOS Liquid Glass.

## Design tokens (CSS vars already defined)
- Surfaces `--bg` / `--sc` / `--sch`; text `--on` / `--onv`; accent `--primary`; status
  greens / ambers / reds as the Budgets card uses them; dividers `--outv`.
- Token-driven only so every frame themes in **dark + light** (the Wrapped cards too — use
  band colours from tokens, no hard-coded greys).

## Please produce
- **`RecapScrollScreen.dc.html`** — Direction A, states 1–4.
- **`RecapStoryScreen.dc.html`** — Direction B, the monthly sequence + a tougher-month card
  + a short weekly sequence.
- **`RecapSettings.dc.html`** — the Account enable + Weekly/Monthly/Both control (default +
  Both).
- **Same July data across A and B** so I can compare the two directions fairly.
  Token-driven, dark + light. Phone first; tablet to follow.

## Implementation notes (for after the mockup — essentially no new data model)
- Everything reuses the **Insights + Wellbeing view-models** over the pay-cycle window
  (`monthStartDay` / `PayCycle`). The score, movers, budget outcome, goals and limits are
  already computed.
- **No notifications / no background work** — the recap is an **in-process interstitial**
  shown on app open (modelled on the onboarding / quiz gate in `BudgettyApp`), fired **once
  per period** via a **last-shown period key** (the `ReviewTracker` cooldown pattern).
- **New persistence only:** `recapEnabled` + `recapFrequency` (Weekly/Monthly/Both) +
  `recapLastShown{Week,Month}` in `SettingsStore` / `AppSettings`, plus the Account control.
- **Weekly has no 0–100 score** — the weekly recap uses the existing weekly **pace +
  tactical-tips** layer, not a new weekly score.
- **Graceful first run:** under the wellbeing scoring floor (**5 receipts**) or with no
  prior period to compare, skip the recap or show it without the comparison.
- Optionally store the **last recap** so the user can re-open it from Insights (a quick
  dismiss shouldn't lose it).

Thanks!
