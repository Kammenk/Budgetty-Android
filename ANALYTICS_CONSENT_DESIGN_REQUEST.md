# Claude Design request — Analytics & crash-reporting consent (first-run) — phone + iOS + tablet

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups in that project; once they're there, Claude Code reads them back via
> DesignSync and implements the approved ones.

---

Hi! I'd like a **first-run consent screen** where the user opts in to **anonymous usage analytics** and **crash & diagnostics reporting**. Budgetty is a privacy-first finance app, and today these are collected by default with only a buried Account toggle — I want an explicit, friendly **opt-in** the first time the app opens, and nothing collected until the user chooses. Please mock it up for **all three surfaces** (Android phone Material 3, iOS 26 Liquid Glass, Android tablet adaptive).

## Why / the goal
- We're EU-first, so this should read as genuine informed **opt-in**, not a pre-ticked opt-out. Calm and reassuring, not a scary legal wall.
- The #1 message to land: **we never collect financial data.** Receipts, amounts, income and categories stay on the device. Analytics is only *which features get used* + crash/performance diagnostics.
- **Free / universal** — every user sees it once, right after onboarding / sign-in, before the main app.

## What it collects (say this plainly on the screen)
Two independent switches:
1. **Usage analytics** — "Anonymous data about which features you use, so we can improve Budgetty. Never your receipts, amounts, or personal info."
2. **Crash & diagnostics** — "Automatic crash reports and performance data so we can fix problems faster."

Plus a persistent reassurance line — **"No financial data, ever — your money stays on your device."** — and a **Privacy Policy** link.

## The flow / states to draw
1. **Consent screen (default):** branded, centered — Budgetty mark, a warm title (*"Help improve Budgetty"*), a one-line explainer, the two switches, the reassurance line + Privacy Policy link, a primary **Continue** button and a quiet **Not now** (proceeds with everything off). Draw it with the switches **off** (true opt-in).
2. **Both switches on:** a second copy so we can see the "on" treatment.
3. **Expanded detail:** an optional "What we collect / what we never collect" expandable — a clear **two-column "Collected ✓ / Never ✗"** so the "never financial data" promise is unmissable (Never column: receipts, amounts, income, categories, your identity).
4. **Account · Data & privacy group:** the ongoing control that lives in Account afterwards — a small settings group with the same two toggles (this restyles the existing Account toggles to match). Show it so the first-run screen and the settings row read consistently.

## Platforms & components to match
- **Android phone — Material 3.** Full-screen, in the style of **`OnboardingScreen.dc.html`** (calm, branded, centered) for the consent screen; **`AccountScreen.dc.html`** settings-row style for state 4. Phone **300×620**, `font-family: Roboto`.
- **iOS phone — iOS 26 Liquid Glass.** Same content, native chrome — the switches, buttons and settings group in the iOS Liquid Glass treatment (match the shipped iOS mockups / dock language). Don't reuse Material switches.
- **Tablet — Material 3 adaptive.** On a wide layout the consent screen is a **centered card** (not a stretched phone column); the Account "Data & privacy" group sits under the **nav-rail** layout. Build from the shipped `Tablet*` treatment.

## Design tokens (CSS vars already in the project)
- Surfaces `--bg` / `--sc` (surfaceContainer) / `--sch`; text `--on` / `--onv` (muted labels); accent `--primary` (active switch, primary button); success **green** for the "Collected ✓" column; dividers `--outv`.
- Token-driven only, so it themes in **dark + light**.

## Please produce
- **`AnalyticsConsentScreen.dc.html`** — Android phone, states 1–3, plus the Account group (state 4).
- **`iOS AnalyticsConsentScreen.dc.html`** — the iOS Liquid Glass version of the same.
- **`TabletAnalyticsConsentScreen.dc.html`** — the adaptive tablet version.

## Implementation notes (for after the mockups — minimal new surface)
- Wires to the existing prefs `analyticsEnabled` / `crashReportingEnabled` (`data/settings/AppSettings.kt`) — we'll flip their defaults to **off** and gate Firebase Analytics / Crashlytics init in `BudgettyApplication` until the choice is made.
- The Account toggles already exist; state 4 is a restyle, not a new setting.
- Ships on **both platforms**; we'll also update the Play Data-safety form + iOS privacy label to match.

Thanks!
