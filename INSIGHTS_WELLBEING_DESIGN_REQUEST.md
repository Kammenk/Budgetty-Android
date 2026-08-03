# Claude Design request — Android "Wellbeing" score + coaching: entry banners → dedicated screen (phone)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and implement the approved ones.

---

Hi! I'd like to add a **"Wellbeing" experience to Budgetty** — a single **0–100 wellbeing
score** for the user's finances plus a short feed of **specific, actionable tips** that
refresh **weekly and monthly** to help them improve. Think of it as turning Budgetty from
a *rear-view mirror* (here's what you spent) into a *coach* (here's how you're doing, and
here's the one thing to do next).

**Important architecture note (this changed):** the coaching content lives on its **own
dedicated full-screen Wellbeing screen**, reached from a **small live "invite" banner** on
Home (and a slim entry row in Insights) — it is **not** a new block inside the Insights
scroll. The Insights tab is already very dense (13 toggleable sections), so a heavy new
section there would get buried and bloat the tab. A compact banner that shows the score +
the single top tip, opening a screen with room to breathe, is the right shape. Please mock
up **both the entry points and the screen**, matching the existing Material 3 design system.
**iOS gets the identical feature** — once approved I'll spin a matching iOS (Liquid Glass)
request; behaviour must be the same on both platforms.

## Why / the goal
- Budgetty already surfaces a lot of *readouts* — breakdown donut, savings rate, income vs
  spending, fixed vs flexible, subscriptions, trends. They sit as separate cards and the
  user has to interpret them. **Nothing tells the user "how am I doing overall?" and "what
  should I change?"**
- This is the **narrator**: one score to anchor on, and tips that each reference a **real
  number from the user's own data** and deep-link to the screen that fixes it. The rule is
  **observation → the user's actual number → one tap to act.** No generic "spend less."
- It builds on the existing rule-based **Highlights** engine and uses data the app **already
  computes** — no new number-crunching.
- The user explicitly wants a **score** front and centre — a clear grade (even a tough one)
  is motivating, not discouraging.

## Where it lives (the entry points + the screen)
- **A dedicated full-screen "Wellbeing" screen** — a top-level destination like **Budget**,
  **Paywall**, and **Upload** (top app bar + back arrow, **bottom nav hidden**). This is
  where the score, components, weekly/monthly toggle, tips, wins, and focus live, with room
  to breathe. **No new bottom-nav tab.**
- **Home banner = the primary entry** (Home is the daily-open surface). A **small live-teaser
  card** that shows the current score + the top tip, so value is visible without tapping in.
  It's a **Home "Customize sections" item**, so users can hide it.
- **Insights slim entry row = the secondary entry.** A **single-line row at the very top of
  Insights** (above Breakdown) — score chip + "Wellbeing score" + chevron. It's an Insights
  section so it lives in **Customize sections** and can be hidden/reordered, but unlike every
  other section it's **one tappable row, not a content block.** This directly answers "the
  Insights tab has too many blocks."
- **Optional third entry:** a "Wellbeing" row in the **Account** grouped list (right where
  Budget already sits) — near-zero cost, extra discoverability.
- **Free feature** (not premium) — the retention hook we want every user to feel on day one.
  *(Optional: a locked "premium teaser" screen state if we decide to gate the deeper monthly
  review later. Default assumption: free.)*

## The Home banner (the invite — keep it genuinely small)
- **One compact card** (surfaceContainer, 20dp radius, ~64–72dp tall). Placed on the phone
  Home **below the Budgets / Upcoming-bills cluster and above the Receipts list**. It must
  read **lighter than the Safe-to-spend hero** at the top — this is a nudge, not a second hero.
- **Anatomy (single row):** a **mini score ring (~40dp)** or score chip on the left showing
  the number in its band colour · a middle column with a **"Wellbeing"** label and the
  **current top tip as one truncated line** · a trailing **chevron**. A small **"new" dot** on
  the ring when there's an unseen weekly tip.
- **Three states to draw:**
  1. **Live** — e.g. `◔ 72 · Healthy` + *"Dining out is up 34% this month"* + chevron.
  2. **Attention** — score is low or has dropped, or the top tip is a red alert: the card
     takes a gentle **amber/red tint or edge** to pull the eye; the line is the alert
     (*"You spent €120 more than you earned"*).
  3. **First-run / not enough data** — no score yet: *"Set a budget to unlock your Wellbeing
     score"* + chevron (still tappable — the screen shows its own setup state).
- The **weekly nudge** is just this banner swapping to this week's top tip; **dismiss/snooze**
  applies to the tip, not to the banner-as-nav-affordance.

## The Insights entry row (secondary)
- A **single-line row pinned to the top of the Insights list, above Breakdown**: a small
  **score chip + "Wellbeing score" + band word** on the left, **chevron** on the right. Tapping
  opens the Wellbeing screen. Draw it in the **live** and **first-run** states.

## The Wellbeing screen (anatomy, top to bottom)
Full-screen, top app bar **"Wellbeing"** + back arrow.

**0. Mode toggle — `Weekly | Monthly`** (reuse the app's `SegmentedToggle`, not M3
SegmentedButton). Switches the screen between a light **weekly check-in** and the full
**monthly review**.

**1. Wellbeing score card (the hero)**
- A **score ring** (reuse the Breakdown donut's ring language — thin ring, rounded caps),
  big **0–100** number in the centre + a **band word** beneath.
- **Bands & colour** (reuse the budget traffic-light palette):
  - **0–39 "Needs work"** — red `#D32F2F`
  - **40–59 "Getting there"** — amber `#F9A825`
  - **60–79 "Healthy"** — green `#2E7D32`
  - **80–100 "Thriving"** — deep green / positive accent
- A **trend chip**: `↑ 4 vs last month` (green) or `↓ 5 vs last month` (muted).
- Below the ring, **tappable component chips/bars** — the sub-scores the total is built from,
  each with its own mini value + traffic-light colour: **Savings rate · Budget adherence ·
  Spending trend · Subscriptions · Goals**. Tapping one expands a short plain-language
  explanation ("You saved **18%** of your income — anything above 20% scores full marks") with
  a link to the relevant screen. Please draw one component **expanded**.

**2. Tips feed — "Focus this week" / "Focus this month"**
- A prioritised list of **3–5 tip cards**. Each: a **leading icon tile** tinted by tip type, a
  **bold title with the real number** ("Dining out is up **34%** — €182 vs your €136 average"),
  a one-line detail, a **CTA button** that deep-links ("Set a dining budget", "Review
  subscriptions", "Create a goal", "See where it went"), and a subtle **dismiss / "Got it"**.
- **Card tones to draw:** **Alert (red)** · **Caution (amber)** · **Opportunity (primary)** ·
  **Win (green, always at least one when available)**.

**3. Wins strip (monthly)** — a compact horizontal strip of streak/achievement pills:
"3 weeks under budget 🎉", "Dining down 20%". Positive-only.

**4. Focus for next month (monthly)** — one highlighted call-out that carries forward:
*"Keep dining under €150 next month to lift your score to ~76."*

## Weekly vs Monthly — how the two modes differ (on the screen)
- **Monthly (full review):** big score ring + all component chips + up to 5 tips + Wins strip +
  "Focus for next month".
- **Weekly (light check-in):** the top element becomes a **"This week" pace card** — spent so
  far this week, `↓ 12% vs last week`, a thin pace bar — then **2–3 tactical tips only**. The
  **score ring still shows** but smaller/secondary with a `72 · updates monthly` note, because
  the score is a monthly measure and shouldn't be re-graded on noisy weekly data.

## How the score is built (for realistic mockups — final weights are an implementation detail)
| Component | ~weight | Data source (already in the app) | Full marks when… |
|---|---|---|---|
| Savings rate | 25% | income − spending (Savings-rate section) | keeps ≥ 20% of income |
| Budget adherence | 25% | spend vs budgets (Budget screen) | every category within budget |
| Spending trend | 15% | period vs trailing avg (Trend / Period-comparison) | flat or down |
| Subscriptions | 15% | recurring-merchant detection (Subscriptions) | low recurring share |
| Goals | 20% | savings-goals progress | all active goals on pace |

Components with **no data** (no budget set, no goals yet) are **excluded and the weights
renormalise** — so a brand-new user still gets an honest score from what they do have.

## The tip library (draw cards from these so they feel real)
- **Category spike** — "Dining out is up 34% — €182 vs your €136 average." → *Set a budget*
- **Small-purchase leak** — "42 coffee runs this month = €120." → *See transactions*
- **Budget pace** — "You've used 80% of your weekly budget by Thursday." → *View budget*
- **New / duplicate subscription** — "You're paying for 2 music services — €22/mo combined." → *Review subscriptions*
- **Subscription creep** — "Subscriptions now €67/mo, up from €50." → *Review subscriptions*
- **Missing budget** — "You spend ~€200/mo on Transport but have no budget." → *Set a budget*
- **Chronic underspend** — "You underspend Groceries by ~€50 every month — move it to a goal?" → *Create a goal*
- **Negative cashflow** — "You spent €120 more than you earned this month." → *See where it went*
- **Goal off-track** — "At this pace 'Vacation' lands 2 months late — add €30/mo." → *Adjust goal*
- **No goal yet** — "No savings goal yet — even €25/mo builds a buffer." → *Create a goal*
- **Wins** — "Saved 18% of income", "3 months under budget", "Dining down 20%"

## Match these existing components for style
- **Home banner:** match the **Home cards** and the **`SafeToSpendScreen.dc.html`** hero for
  card radius / weight — but lighter and shorter than Safe-to-spend (it's a nudge).
- **Insights row:** match the compact list-row rhythm already on Insights.
- **Screen:** match the **Budget screen** chrome (top app bar + back arrow, full-screen, no
  bottom nav) and the **Breakdown donut ring** for the score ring.
- **Score bands & component bars:** reuse the **Budgets card's green/amber/red** status language
  so "healthy/careful/over" reads consistently.
- Reuse the app's **`SegmentedToggle`** for Weekly | Monthly.
- Phone preview size **300×620**, `font-family: Roboto`. Material 3 (Material You), **not** iOS
  Liquid Glass — this is the Android side.

## The states to draw
**Entry points** (in `WellbeingEntry.dc.html`, shown in their host screens):
1. **Home banner — Live** (in the Home stack, below Budgets/Upcoming-bills, above Receipts).
2. **Home banner — Attention** (amber/red-tinted, alert tip).
3. **Home banner — First-run** ("Set a budget to unlock your score").
4. **Insights entry row — Live** (pinned above Breakdown) + **First-run** variant.

**The screen** (in `WellbeingScreen.dc.html`):
5. **Monthly · Healthy** — score **72 "Healthy"** (green), `↑ 4`. Components: Savings 78,
   Budget 65 (amber), Trend 80, Subscriptions 55 (amber), Goals 70. Tips: dining spike (amber),
   duplicate music services (amber), a green win. Wins strip + "Focus for next month". The
   flagship, fully populated.
6. **Monthly · Needs attention** — score **38 "Needs work"** (red), `↓ 5`. Savings 20 (red),
   Budget 30 (red), Trend 35, Subscriptions 40, Goals excluded (none set). Tips: negative
   cashflow (red), over budget in 3 categories (red), "no goal yet" opportunity. Tough-love end
   of the range without shaming.
7. **Component expanded** — the Healthy screen with the **Savings rate** component tapped open.
8. **Weekly mode** — "This week" pace card on top + smaller secondary score ring + 2–3 tips.
9. **First-run / not enough data** — friendly intentional setup state, partial/greyed ring,
   *"Log a couple of weeks of spending and set a budget to unlock your Wellbeing score,"* + a CTA.

Everything **token-driven** (CSS vars below, no hard-coded greys) so every frame themes in
**dark + light**.

## Design tokens (CSS vars already defined in the project)
- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`; status greens/ambers/reds exactly as the Budgets card uses them
- Lines: `--outv` (dividers)

## Please produce
- **`WellbeingEntry.dc.html`** — the Home banner (Live / Attention / First-run) in the Home
  stack, plus the slim Insights entry row (Live + First-run). Light + dark.
- **`WellbeingScreen.dc.html`** — the full-screen Wellbeing destination in states 5–9 above.
  Light + dark. Phone first.
- Once I approve the phone layouts, I'll ask for the **tablet / landscape** variants.

## Implementation notes (for after the mockup — essentially no new data model)
- **New full-screen destination** + nav wiring (like Budget/Paywall/Upload — bottom nav hidden).
- **Two tiny entry surfaces:** a new **Home "Customize sections"** banner item, and a new
  **Insights section that renders as a single row** (reuses the existing `InsightsSection`
  customization machinery). Both just read the current score + top tip.
- **Score components** already exist as Insights sections / screens (Savings-rate, Income &
  spending, Fixed vs flexible, Subscriptions, Trend & Period-comparison) plus Budgets and
  Savings-goals. **Tips** extend the existing rule-based **Highlights** detector primitive.
- **Everything stays on-device / rule-based** — no cloud call, no LLM, no spending data leaving
  the phone; fully unit-testable.
- **Only new persistence:** a small store of **dismissed/snoozed tip ids**. The score's trend
  arrow is computed from current-vs-previous period on the fly (no score-history table for v1).

## Scope note (we're ~4–5 days from launch)
Must-have MVP the mockup should nail: the **Home banner (Live + First-run)**, and the screen's
**Monthly Healthy**, **Monthly Needs-attention**, **Weekly**, and **First-run** states. The
**Insights entry row**, **Account row**, **Wins strip**, **Focus-for-next-month**, the
component-expanded interaction, and the premium-teaser are **nice-to-have** — draw them if easy,
but they can be a fast follow. The banner + score ring + tips feed + Weekly/Monthly toggle are
the core.

Thanks!
