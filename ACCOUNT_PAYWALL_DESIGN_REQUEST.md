# Claude Design request — Account trim, Paywall benefits, and the AI wording

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will update the existing `*.dc.html`
> mockups in that project; once they're there, Claude Code can read them back
> via DesignSync and confirm the app matches.
>
> Implemented in the app on branch `account-paywall-cleanup` (commits
> `6547e73` + the onboarding follow-up). The **code has already shipped these
> changes** — this request exists so the mockups catch up, not the other way
> round.

---

Hi! We just trimmed the **Account** screen, rebuilt the **Paywall** benefit list,
and took the **AI wording out of the product** in the Android app. The mockups are
now behind in a few places — and while checking, I found some drift that predates
all of this. Please bring them back in line. Everything below is already live in
code, so treat it as a spec to match rather than a proposal to explore — except
the three spots marked **your call**, where I'd like your judgement.

**One thing to be careful about:** "no AI wording" applies to **product copy
only**. The privacy policy's AI limited-use disclosure names Anthropic
deliberately and is a **required Play disclosure** — it must not be touched, in
the app or anywhere in this project.

## Why these changes happened

Three Account toggles — **Push notifications**, **Biometric authentication** and
**Analytics** — were inert. Not "partly wired": there was no notification
channel, no `POST_NOTIFICATIONS` permission, no FCM, no `androidx.biometric`
dependency and no `FirebaseAnalytics` anywhere in the project. Each switch wrote
a boolean to SharedPreferences that nothing ever read back. They're gone.

Separately, the Paywall was advertising **three** benefits, one of which
(**Cloud backup & sync**) doesn't exist — the backup manager is local
export/import, and export/import are both free and ungated. Meanwhile **two
real, enforced unlocks were never mentioned at all**. The paywall now lists all
five, honestly.

## 1. `AccountScreen.dc.html` (phone) — the main one

### Remove
- **"Push notifications"** row (the toggle in the first, header-less section).
- The entire **"PRIVACY & SECURITY"** section — both "Biometric authentication"
  and "Analytics". Removing both empties the section, so the section header goes
  with them. There is no Privacy section in the app any more.

### Move
- **"Currency"** (value `EUR (€)`) moves **out** of the first section and **into
  "PREFERENCES"**, sitting between "Accent color" and "Date format". It belongs
  with the other format/locale choices rather than with subscription and data.

### Add — this row is missing from the mockup and already exists in the app
- **"Category rules"** in the first section, between "Budget" and "Export data".
  Chevron row, no value. (This shipped a while back and the mockup never caught
  up — worth a glance for other misses while you're in here.)

### Add — the Support section, which the app shows inline
The app renders **Support & About as a section of the Account screen**, not as a
separate destination, but the mockup only has it as the standalone
`SupportAboutScreen.dc.html`. Please add a **"SUPPORT & ABOUT"** section to the
bottom of `AccountScreen.dc.html`, above the Sign out / Delete account footer:

| Row | Icon | Note |
|---|---|---|
| Help & FAQ | help/question | chevron |
| **Contact us** | mail | **two-line row — see §2** |
| Rate Budgetty | star | chevron |
| Privacy Policy | shield/lock | chevron |

### Resulting section order
1. *(header-less)*: Subscription · Budget · Category rules · Export data · Import data · Widgets
2. **PREFERENCES**: Theme · Accent color · **Currency** · Date format · Language
3. **SUPPORT & ABOUT**: Help & FAQ · Contact us · Rate Budgetty · Privacy Policy
4. Footer: Sign out · Delete account · `Budgetty · v1.0`

Keep the existing row anatomy exactly: 34×34 badge, `border-radius:10px`,
`background:var(--scl)`, 18px SVG icon; label 14px/600; `padding:13px 15px`;
dividers `1px var(--outv)` at `margin-left:62px`; cards `var(--sc)` /
`border-radius:20px`; section headers 12px/700 `var(--onv)`.

## 2. The "Contact us" row — new two-line treatment

"Contact support" is renamed and now carries a second line. The point is that
people shouldn't think the address is only for bug reports — feature ideas and
plain "this is nice" mail are equally welcome.

- **Title**: `Contact us` — 14px/600 `var(--on)` (unchanged from other rows)
- **Second line**: `Report an issue, suggest a feature, or just say hello` —
  12px `var(--onv)`, ~2px below the title
- The badge stays vertically centred against the two-line block, and the row
  keeps its chevron. Row padding grows to fit; everything else is unchanged.

Please apply the same rename + second line in **`SupportAboutScreen.dc.html`**.

> **Your call (a):** `SupportAboutScreen.dc.html` also lists a **"Terms of
> Service"** row under a "LEGAL" header. The app has no Terms of Service — the
> Support section is just the four rows above. Either drop that row, or tell me
> it should exist and I'll wire it up. Also note that screen's badges hold
> **emoji at 16px** while the Account screen's hold **SVG icons** — they're
> meant to be the same rows, so one of them is wrong. I'd pick the SVGs.

## 3. Paywall — all five benefits, each with its free-tier limit

Applies to `PaywallScreen.dc.html`, `PaywallScreenTabletPortrait.dc.html`,
`PaywallScreenTabletTwoPane.dc.html` and `TabletPaywallScreen.dc.html`. The
benefit list must be **identical across all four** — in code it's now a single
shared list precisely so the two layouts can't drift.

Replace the current three single-line rows with these **five two-line rows, in
this order**:

| # | Title (14px/600) | Detail line (12px `var(--onv)`) | State |
|---|---|---|---|
| 1 | Unlimited receipt scans | Free plan stops at 10 | ✓ |
| 2 | Unlimited custom categories | Free plan includes 3 | ✓ |
| 3 | Unlimited recurring bills | Free plan includes 3 | ✓ |
| 4 | Every accent theme | Sage, Ocean and Plum | ✓ |
| 5 | Cloud backup & sync | **Coming soon** | ◷ muted |

Notes that matter:

- **These five are the complete, exhaustive list of what Premium unlocks.** There
  is nothing else behind the paywall. Rows 2 and 3 are the ones that were never
  advertised despite being enforced in code all along.
- **Row 5 is not shipped.** It must read as roadmap, not as a reason to pay:
  swap the checkmark for a **clock** icon, mute the badge fill (e.g.
  `color-mix(in srgb, var(--onv) 12%, transparent)` instead of `var(--secc)`),
  and set the **title** to `var(--onv)` rather than `var(--on)` so it visibly
  sits below the four real ones. The detail line reads "Coming soon".
- The numbers 10 / 3 / 3 are **interpolated from the constants that enforce the
  caps**, so please leave them as literal text in the mockup but don't treat them
  as design decisions — if a cap changes, the copy follows automatically.
- "Sage, Ocean and Plum" are **not translated** anywhere — they're fixed theme
  names in the picker, so they stay English in every locale.

### Row structure
Keep the existing shape and just let it grow a second line:
- Row `display:flex; align-items:center; gap:11px` — badge stays centred against
  the two-line text block
- Circle `24×24`, `border-radius:99px`, `background:var(--secc)`; icon 14×14,
  `color:var(--onsecc)`, `stroke-width:2.4`
- Title 14px/600 `var(--on)`; detail 12px `var(--onv)` directly beneath
- Rows are ~2× taller now, so tighten the list `gap` from `11px` to about `10px`

> **Your call (b):** every benefit row currently uses the **same generic
> checkmark**. With five rows and two lines each, per-benefit iconography
> (receipt / tag / repeat / palette / cloud) might carry the list better — or it
> might get noisy against the gradient. Your judgement; the code can follow
> either.

### Landscape brand panel must tolerate a short window
In `PaywallScreenTabletTwoPane.dc.html`, five two-line benefits **overflow a
~410dp-tall landscape window** (a phone or 7" tablet on its side). In code the
panel now **scrolls between a pinned header and a pinned "Restore purchases"**.
Please show the panel as scrollable rather than assuming everything fits — this
exact class of bug (content pushed below the fold on short landscape) already bit
the login screen once and is worth designing against, not around.

## 4. Login tablet brand panel — drop the AI line, add a Premium note

Applies to `LoginScreenTabletTwoPane.dc.html` (the 1280×800 landscape split),
`TabletLoginScreen.dc.html` and `LoginScreenTabletPortrait.dc.html`. **All three
must say the same thing** — right now the first two already disagree with each
other (feature 3 is "insights" in one and "alerts" in the other).

Tagline is unchanged: **"Receipt-driven personal budgeting"**.

Replace the three feature rows:

| | Old | New |
|---|---|---|
| 1 | ~~Snap a receipt — AI reads it~~ | **Scan a receipt, get every line item** |
| 2 | ~~Auto-categorized line items~~ | **Automatic category sorting** |
| 3 | ~~Budget tracking & alerts / & insights~~ | **Budgets, bills & spending insights** |

Two reasons, both worth knowing:
- **We're not naming AI in the product any more.** (The privacy policy's AI
  limited-use disclosure stays — that one is a required Play disclosure. This is
  only about marketing copy.)
- **"alerts" was as unimplemented as the notifications toggle** — same missing
  feature, advertised in a second place. "insights" is real; the Insights screen
  exists.

### New: a closing Premium note
Below feature 3, add a divider and one line:

- Divider: full-width, `rgba(255,255,255,.22)`, with roughly `24px` above and
  `12px` below
- Text: **"Premium unlocks unlimited scans, categories & bills"** — 12px,
  `rgba(255,255,255,.75)`

This panel is **pre-auth**: there's no account to buy against yet, so it keeps
selling the app and merely *names* what Premium adds. Please don't turn it into a
paywall pitch — the crown, the plan cards and the full benefit list stay on the
Paywall itself.

### Short-landscape behaviour
The panel is vertically centred and doesn't scroll, and the new line makes it
taller. On a short landscape window (~410dp) the app now: **hides the 80×80 app
mark entirely**, drops "Budgetty" from 36px to ~28px, and tightens the gaps
(feature gap 20px → 12px). Worth showing as a variant if that's cheap — this is
the screen the below-the-fold bug hit last time.

## 5. Onboarding — the AI line goes here too, and the copy has drifted badly

Applies to `OnboardingScreen.dc.html`, `OnboardingScreenTabletTwoPane.dc.html`,
`OnboardingScreenTabletPortrait.dc.html` and `TabletOnboardingScreen.dc.html`.

**Every one of the four names AI on the categorize slide, and every one says it
differently:**

| File | Current slide-2 body |
|---|---|
| `OnboardingScreen.dc.html` | "**AI** pulls every line item and sorts it into a category — you just check it." |
| `OnboardingScreenTabletTwoPane.dc.html` | "**AI** pulls every line item and sorts it into a category automatically. You just review and confirm." |
| `TabletOnboardingScreen.dc.html` | "Snap any receipt — **AI** pulls every line item and sorts it into a category. You just check it." |
| `OnboardingScreenTabletPortrait.dc.html` | "**AI** sorts each item into a category automatically. You just review and confirm in seconds." |

In the app this slide has already been reworded — Budgetty itself is now the
subject of the sentence, which also reads better than crediting the tech:

> **We read & categorize it**
> Budgetty pulls out the store, date, discount and every line item — each sorted
> into a category. You just review and save.

### While you're in there: two more things

- **`OnboardingScreenTabletPortrait.dc.html` titles slide 2 "We categorize it"**
  where the other three say "We read & categorize it". The app says "We read &
  categorize it".
- **Slide 4 promises alerts** — "Get alerts before you overspend, not after"
  (both deck files). This is the **same phantom feature** as the notifications
  toggle in §1 and the "alerts" line in §4: nothing sends any alert, and nothing
  will. The app's slide 4 makes no such promise. Please drop it.

### Please resync all four slides to the app's actual copy

The mockups have drifted well beyond the AI line — slide titles and bodies differ
from the app *and* from each other. The app is the source of truth here:

| # | Title | Body |
|---|---|---|
| 1 | Snap any receipt | Photograph a paper receipt or upload a PDF. Capturing a purchase is always one tap away. |
| 2 | We read & categorize it | Budgetty pulls out the store, date, discount and every line item — each sorted into a category. You just review and save. |
| 3 | See where your money goes | Clear charts break your spending down by category, store and time, so the full picture is always a glance away. |
| 4 | Set budgets that keep you on track | Monthly, weekly and per-category budgets with friendly green-amber-red progress keep your goals in sight. |

(For reference, the mockups currently title slides 3 and 4 "See where it goes"
and "Set budgets that work", and slide 1's body is about pointing the camera
rather than the photo-or-PDF choice the app actually offers.)

> **Your call (c):** `OnboardingScreen.dc.html` and `TabletOnboardingScreen.dc.html`
> aren't decks at all — they're prop-driven single-slide components whose
> *default props* happen to contain slide 2's text, which is why the AI line is
> baked into them. If the canonical phone deck lives in a caller
> (`Phone.dc.html` / `Shots Phone.dc.html`?), the fix probably belongs there and
> these two just need their defaults cleaned. Your call which — I only need the
> AI wording gone from wherever it renders.

## 6. Three mockups are now orphaned — please retire or park them

These depict features that this change **deleted rather than deferred**. Nothing
in the app navigates to any of them, and nothing will:

| File | Depicts | Status |
|---|---|---|
| `NotificationPrefsScreen.dc.html` | Budget alerts, weekly summary, quiet hours | No notifications exist |
| `AlertsInboxScreen.dc.html` | Alerts inbox with 4 notification cards | Same — nothing sends these |
| `BiometricLockScreen.dc.html` | "Unlock Budgetty" fingerprint gate | No biometric dependency |

They were never wired up — the Account toggles were terminal switches that never
navigated into them. I'd rather they be **archived than left in the deck** where
they read as shipped or imminent. If there's a "parked/exploration" convention in
this project, use it; otherwise just tell me and I'll leave them.

There are iOS counterparts (`iOS Notifications.dc.html`, `iOS Biometric
Lock.dc.html`) plus `iOS Account.dc.html`, `iOS Paywall.dc.html`, `iOS
Login.dc.html` and `iOS Support & About.dc.html`. **Please leave all the iOS
files alone for now** — the iOS app hasn't taken this change yet, and it'll get
its own request once the port lands, so the mockups should keep matching the iOS
code until then.

## Summary of exact new copy (English source of truth)

```
account_contact               Contact us
account_contact_subtitle      Report an issue, suggest a feature, or just say hello

paywall_benefit_scans         Unlimited receipt scans
paywall_benefit_scans_detail  Free plan stops at 10
paywall_benefit_categories    Unlimited custom categories
   …_categories_detail        Free plan includes 3
paywall_benefit_recurring     Unlimited recurring bills
   …_recurring_detail         Free plan includes 3
paywall_benefit_themes        Every accent theme
   …_themes_detail            Sage, Ocean and Plum
paywall_benefit_cloud         Cloud backup & sync
paywall_benefit_soon          Coming soon

login_feature_1               Scan a receipt, get every line item
login_feature_2               Automatic category sorting
login_feature_3               Budgets, bills & spending insights
login_premium_note            Premium unlocks unlimited scans, categories & bills

onb2_title                    We read & categorize it          (unchanged)
onb2_body                     Budgetty pulls out the store, date, discount and
                              every line item — each sorted into a category.
                              You just review and save.
```

Removed entirely: `account_notifications`, `section_privacy`, `account_biometric`,
`account_analytics`.

Unchanged: `paywall_title` ("Budgetty Premium"), `paywall_hero_title` ("Get more
from Budgetty"), `paywall_hero_subtitle` ("No free trial · cancel anytime"),
`login_tagline` ("Receipt-driven personal budgeting"), and every plan-card /
restore / status string.
