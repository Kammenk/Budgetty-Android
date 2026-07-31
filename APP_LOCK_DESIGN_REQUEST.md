# Claude Design request — Android App lock: PIN + biometric (phone)

> Paste everything below the line into the **Claude Design** chat for the
> "Budgetty app design brief" project. It will create the new `*.dc.html`
> mockups; Claude Code reads them back via DesignSync and implements the approved ones.

---

Hi! I'd like to add an optional **App lock** to Budgetty (Android phone) — a **PIN**
plus optional **biometric** (fingerprint / face unlock) that gates the app on
launch. It's a privacy feature: Budgetty holds a full picture of someone's spending,
income and bills, and today anyone who opens the phone sees it (we never sign the
user out). Please mock this up in the Material 3 system. **iOS gets the identical
feature** (`IOS_DESIGN_REQUEST_APP_LOCK.md`).

## Why / the goal
- A second layer for the borrowed-/lost-while-unlocked phone. Table-stakes for a
  finance app; fits our privacy-first, no-image, local-data stance.
- **Free feature** — we don't paywall security.
- Fully **native**: Android `BiometricPrompt` + a custom PIN screen. Nothing leaves
  the device; the PIN is stored **hashed**, never in plaintext.

## How it works
- **Enable** in Account: toggle **App lock** on → **set a PIN** (enter, then
  confirm) → optionally enable **biometric**.
- **Lock screen** appears on **cold start** and on **return to foreground after the
  auto-lock delay**: a **numeric keypad** for the PIN, plus a **biometric
  affordance** that fires the system prompt (auto-triggered when biometric is on).
- **Auto-lock** delay setting: **Immediately / After 1 minute / After 5 minutes.**
- **Forgot PIN?** — the user is always signed in, so recovery = **re-authenticate**
  (sign in again) to reset the PIN. No secret backdoor.

## Match these existing components for style
- **`AccountScreen.dc.html`** — the App-lock settings live here as a small
  **Security** group (App lock toggle → sub-rows: Change PIN, Use biometric,
  Auto-lock). Match the existing settings row / toggle style.
- **`OnboardingScreen.dc.html` / login** — for the full-screen **lock** treatment
  (calm, branded, centered) so the lock screen feels part of the app.
- Phone **300×620**, `font-family: Roboto`. Material 3, **not** iOS Liquid Glass.

## Screens / states to draw
1. **Account — Security group (off):** the **App lock** toggle in its off state.
2. **Account — Security group (on):** expanded — **Change PIN**, **Use fingerprint /
   face unlock** toggle, **Auto-lock** (with the Immediately / 1 min / 5 min picker).
3. **Set PIN:** the create-PIN screen — keypad, PIN dots, *"Set a PIN"*; then the
   **confirm** step (*"Confirm your PIN"*); and the **mismatch** error (*"PINs don't match"*).
4. **Lock screen (PIN):** the full-screen gate — Budgetty mark, *"Enter PIN"*, PIN
   dots, numeric keypad, a **biometric icon** (fingerprint/face), and a **Forgot PIN?** text link.
5. **Lock screen — wrong PIN:** the error state (*"Wrong PIN, try again"*, dots shake / redden).
6. **Biometric prompt moment:** the lock screen with the system biometric sheet up (represent the system prompt).
7. **Auto-lock picker:** the Immediately / After 1 minute / After 5 minutes options.

## Design tokens (CSS vars already defined)
- Surfaces `--bg` / `--sc` / `--sch`; text `--on` / `--onv`; accent `--primary`
  (active dots, keypad press); error **red** for wrong-PIN / mismatch; dividers `--outv`.

## Please produce
- **`AppLockScreen.dc.html`** — the lock screen (states 4–6) + the set/confirm PIN
  flow (state 3).
- **`AppLockSettings.dc.html`** — the Account Security group (states 1, 2, 7).
- Token-driven so everything themes in **dark + light**.

## Implementation notes (for after the mockup)
- **Native:** `BiometricPrompt` for biometric; a Compose PIN keypad for the fallback.
- **PIN stored hashed** (e.g. salted hash in the settings store / EncryptedSharedPreferences) — never plaintext.
- Lock on `ON_START` after the chosen idle delay; a one-shot unlock per foreground session.
- **Forgot PIN** re-runs the existing Firebase sign-in, then clears + re-sets the PIN.

Thanks!
