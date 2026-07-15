# Claude Design request — setup questionnaire v2 (currency step + inline amount fields)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It revises the already-approved
> questionnaire mocks (`InsightsQuizQuestionScreen`, `InsightsQuizQuestionAltScreen`,
> `InsightsQuizDoneScreen`, `InsightsQuizTabletScreen`) — it creates two new
> `*.dc.html` mockups and updates the closing one.

---

Hi! A follow-up to the **post-signup setup questionnaire** you already designed
for me (the "InsightsQuiz*" mocks in this project — approved and implemented).
Three additions make the quiz configure the app itself, not just the Insights
layout. Everything else — the shared step template, option-card language,
auto-advance, tokens, tablet centered column — stays exactly as approved.

**What's new:**

1. A **Currency step** is inserted as step 2 — the quiz becomes **7 questions**
   (progress reads "X of 7" everywhere now).
2. The **income** step and the **budget** step grow an **inline amount field**
   that appears only when the user picks "Yes".
3. The **closing step's summary card** reflects the new answers (currency,
   amounts) and gains a quiet "add your bills next" hint.

## New flow — 7 question steps + closing

1. Main goal *(unchanged)*
2. **Currency — NEW.** Placed before the income step on purpose: the amount
   fields on steps 3 and 5 show the chosen currency's symbol.
3. Do you want to track income too? — **now with a conditional amount field**
4. Any recurring bills or subscriptions to watch? *(unchanged)*
5. Do you plan to set a spending budget? — **now with a conditional amount field**
6. How much detail do you like? *(unchanged)*
7. How will you mostly add expenses? *(unchanged)*

## A. The Currency step (new, step 2 of 7)

Title: **"Which currency do you use?"** · muted subtitle along the lines of
*"Used for every amount in the app — change it anytime in Account."* (final
microcopy is yours).

- The app supports **19 currencies** (code + symbol): EUR €, USD $, GBP £,
  JPY ¥, CNY ¥, AUD A$, CAD C$, CHF, HKD HK$, SGD S$, INR ₹, KRW ₩, SEK kr,
  NOK kr, NZD NZ$, MXN MX$, BRL R$, ZAR R, TRY ₺.
- The app **auto-detects** a currency from the device region. Pin the detected
  one as the **first row with a small "Suggested for your region" caption** —
  visually distinct but *not* in the selected state; the remaining 18 follow in
  one **scrollable list**.
- Because there are 19 options, use a **compact row** variant of the option
  card (tighter than the big emoji cards on other steps): the **currency
  symbol sits in the leading glyph slot** where other steps have an emoji,
  then the code (**EUR**) and the currency's name, e.g. "Euro".
- Interaction matches every other step: **one tap selects and auto-advances**
  (~300 ms) — tapping the suggested row is simply the confirmation. No
  Continue button here.

## B. Inline amount field on the income step (step 3 of 7)

Question, subtitle, and the two options are unchanged. New behavior:

- **"🧾 No — just my spending"** auto-advances exactly as today.
- **"💰 Yes — income and spending"** does **not** auto-advance anymore: the
  card stays in the selected (`--secc`) state and an **amount field animates
  in below the options**, together with a full-width 56px **"Continue"** pill.
  - Field label: **"Your monthly income (roughly)"**, with a muted helper like
    *"Optional — you can add exact income sources later."*
  - Numeric input. The **currency symbol from step 2 is a suffix** after the
    amount — Budgetty renders "2,400 €", never "€2,400".
  - **Empty is always fine** — Continue is never disabled; a blank field just
    means nothing gets pre-filled.
  - Mind the keyboard: the field sits directly under the two option cards, and
    Continue must remain visible while the keyboard occupies the bottom ~40%
    of the screen. (Mock without a keyboard is fine — just keep the layout
    top-anchored so it survives one.)
- If the user comes Back and switches to "No", the field collapses and that
  option auto-advances as usual.
- *Effect: pre-fills one monthly income source (labelled "Income") on the
  Budget tab — fully editable there later.*

## C. Inline amount field on the budget step (step 5 of 7)

**Same reveal pattern as B** — one mock of the pattern is enough (screen 2
below); listing this only for the copy and behavior:

- **"✅ Yes"** reveals the field + Continue; **"🤔 Maybe later"** and
  **"❌ No"** auto-advance as today.
- Field label: **"Monthly spending budget"**, helper like *"Optional — switch
  to a weekly budget later in the Budget tab."* (The app's budget is a single
  amount, Monthly or Weekly; the quiz keeps it monthly for simplicity.)
- Same suffix-symbol numeric field, same always-enabled Continue.
- *Effect: pre-fills the overall monthly budget on the Budget tab.*

## D. Closing step — updated summary card

Same structure as the approved `InsightsQuizDoneScreen`, richer content. Mock
the summary card with these example lines:

- 🎯 Budget-focused layout
- 💶 Currency — EUR *(pick one generic money/exchange glyph for this line; it
  shouldn't change per currency)*
- 💰 Income set — 2,400 €/month
- ✅ Monthly budget — 1,500 €
- 🌅 Big-picture view

(When the user answered Yes but left an amount blank, the line falls back to
the current plain wording, e.g. "💰 Income and spending on" — no mock needed.)

**New, conditional:** when the user said **Yes to recurring bills**, add a
**quiet secondary hint under the primary CTA** — e.g. a text-button/muted line
like *"Next: add your recurring bills in the Budget tab"*. Bills are too heavy
to enter mid-quiz (each needs a name, amount, category and cadence), so this
hint is the hand-off instead. It must read clearly subordinate — **"Get
started" stays the only pill.**

## Screens to design

### 1. `InsightsQuizCurrencyScreen.dc.html` — currency step (phone)

Step 2 of 7: back chevron + progress "2 of 7" + Skip, the suggested-currency
row pinned on top with its caption, the compact scrollable list below (a few
rows visibly cut off at the bottom edge to signal scrollability).

### 2. `InsightsQuizFieldScreen.dc.html` — income step with field revealed (phone)

Step 3 of 7: "Yes — income and spending" in the selected state, the revealed
amount field with a typed example value ("2,400" + € suffix), helper text, and
the Continue pill. This one mock defines the reveal pattern for both B and C.

### 3. `InsightsQuizDoneScreen.dc.html` — closing step (UPDATE existing)

Same layout as approved, with the 5-line summary card from section D and the
conditional "add your recurring bills" hint under the CTA.

### Notes, no mocks needed

- All previously approved question mocks stay valid — only the progress text
  becomes "X of 7".
- Tablet: the same centered ~520px column as `InsightsQuizTabletScreen`, no
  separate mock.

## Style/tokens — same as the approved set

Phone canvas **300×620**, `font-family: Roboto`, fully-rounded pills (56px
primary), option cards on `--sc`/`--sch`, selected = `--secc`/`--onsecc`,
text `--on`/`--onv`, dividers `--outv`. Text fields should follow the same
token-driven language (e.g. filled field on `--sc` with a `--outline` focus
treatment) — keep everything themeable light/dark, no hard-coded grays.
