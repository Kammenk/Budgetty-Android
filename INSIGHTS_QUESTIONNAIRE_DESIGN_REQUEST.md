# Claude Design request — post-signup Insights setup questionnaire

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and implement the approved ones.

---

Hi! I'm adding a **one-time setup questionnaire** to Budgetty. It appears
**full-screen right after a user creates an account** (never on plain sign-in,
never for existing users) and asks **6 quick questions** about how they'll use
the app. The answers pre-configure the **Insights screen** — sections that
aren't relevant get hidden, and the sections matching the user's main goal move
toward the top. Please mock up the screens below as new `*.dc.html` files in
this project, matching the existing design system exactly.

## What the feature does (context)

- Budgetty's Insights screen is built from **14 optional sections** (Breakdown,
  Summary, Highlights, Trend, Period comparison, Budget, Income & spending,
  Savings rate, Fixed vs flexible, Upcoming bills, Income by source, Top
  categories, Top stores, Biggest purchases). Today the user can hide/reorder
  them via the **"Customize sections"** menu. The questionnaire simply
  **pre-fills that same setting**, so every choice stays reversible later.
- It runs **once**, between registration and the main app, and is **skippable
  at any time** (skip = keep everything visible, today's default).
- The user is brand new — **zero receipts** — so the closing step must set the
  expectation that Insights fill in as they scan.
- Five core sections are **never** hidden by the quiz (Breakdown, Summary,
  Highlights, Trend, Top categories), so Insights never looks broken.
- (For context only, no mock needed: the Customize-sections menu is also being
  extended to tablet Insights, reusing the phone design as-is.)

## Flow — 6 question steps + 1 closing step

All six question steps share **one template**:

- **Top bar:** back chevron (from step 2 on) · a **progress indicator**
  ("2 of 6" — dots or a thin segmented bar, your call) · a quiet **"Skip"**
  text button top-right that skips the whole questionnaire.
- **Question title** (one line, large) with an optional one-line muted
  subtitle.
- **Option cards:** full-width tappable cards, vertically stacked — leading
  **emoji**, label, generous touch height. **Single-select; picking an option
  auto-advances** (~300 ms) to the next step, so there is no Continue button.
  The selected state uses the `--secc` / `--onsecc` treatment (visible when
  the user navigates Back, and briefly during the auto-advance).
- 2–4 options per question.

## The 6 questions (exact copy)

1. **What's your main goal with Budgetty?**
   - 🔍 See where my money goes
   - 🎯 Stick to a budget
   - 📅 Keep bills & subscriptions in check
   - 🪙 Save more each month
   - *Effect: moves the matching sections toward the top (hides nothing).*
2. **Do you want to track income too?**
   - 💰 Yes — income and spending
   - 🧾 No — just my spending
   - *Effect: "just spending" hides Income & spending, Savings rate, Income by
     source.*
3. **Any recurring bills or subscriptions to watch?**
   - 🔁 Yes, I have recurring payments
   - ✨ Not really
   - *Effect: "not really" hides Upcoming bills, Fixed vs flexible.*
4. **Do you plan to set a spending budget?**
   - ✅ Yes
   - 🤔 Maybe later
   - ❌ No
   - *Effect: "no" hides the Budget section ("maybe later" keeps it as a
     nudge).*
5. **How much detail do you like?**
   - 🌅 Just the big picture
   - 🔬 All the details
   - *Effect: "big picture" hides Top stores, Biggest purchases, Period
     comparison.*
6. **How will you mostly add expenses?**
   - 📷 Scanning receipts
   - ⌨️ Typing them in
   - 🤝 A bit of both
   - *Effect: none yet — stored to tune things later.*

## Closing step — "You're all set!"

No Skip, no back chevron, progress shows complete. Contents:

- A celebratory glyph (🎉 or a check), title **"You're all set!"**.
- One line: **"Insights are tailored to you — they'll fill in as you add your
  first receipts."**
- A small **summary card** with one line per meaningful answer, e.g.
  "🎯 Budget-focused layout" · "🧾 Spending only — income cards off" ·
  "🔁 Bills & subscriptions on" · "🌅 Big-picture view" (mock 3–4 lines).
- Muted footnote: *"Change anytime in Insights → ⋮ → Customize sections."*
- Primary CTA: a fully-rounded pill button, **"Get started"**.

## Match these existing components for style

- The **onboarding carousel** — same welcoming tone and pacing; this runs in
  the same "before the main app" zone (no bottom tab bar, no nav rail).
- The **category picker / All-categories rows** — the emoji-leading tappable
  card language for the option cards.
- Buttons are **fully-rounded pills**; the primary action is a tall (56px)
  full-width pill. Phone preview size **300×620**, `font-family: Roboto`.

## Design tokens (CSS vars already defined in the project)

- Surfaces: `--bg`, `--sc` (surfaceContainer), `--sch` (surfaceContainerHigh)
- Text: `--on` (onSurface), `--onv` (onSurfaceVariant, muted labels)
- Accent: `--primary`, `--onprimary`; selected/emphasis uses `--secc` / `--onsecc`
- Lines/scrim: `--outv` (dividers), `--outline`, `--scrim`
- Keep everything token-driven (no hard-coded grays) so it themes light/dark.

## Screens to design

### 1. `InsightsQuizQuestionScreen.dc.html` — a question step (phone)

Question 1 ("What's your main goal with Budgetty?") on the shared template:
progress "1 of 6", Skip top-right (no back chevron on step 1), the four option
cards with **"Stick to a budget" in the selected state** mid-auto-advance.

### 2. `InsightsQuizQuestionAltScreen.dc.html` — a yes/no step (phone, nice-to-have)

Question 3 with its two options unselected, back chevron visible, progress
"3 of 6" — shows how the template breathes with only two options.

### 3. `InsightsQuizDoneScreen.dc.html` — the closing step (phone)

As specced above, with the summary card reflecting the example answers
(budget goal, spending only, has bills, big picture).

### 4. `InsightsQuizTabletScreen.dc.html` — tablet variant (landscape)

Landscape tablet canvas, **16:10, ~1180×760**. Same question step as screen 1,
as a **centered column (~520px wide)** on the full-bleed background — the
questionnaire runs before the main app, so **no navigation rail**. Portrait
tablet behaves the same (same centered column), no separate mock needed.
