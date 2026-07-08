# Budgetty

Scan a receipt and Budgetty turns it into itemized, automatically-categorized transactions — then helps you track spending against budgets and understand it through insights. Receipt images are never stored.

Android app (`com.budgetty.app`), built with Jetpack Compose and a Firebase + Claude receipt-extraction backend.

## Features

- **Receipt scanning** — snap a photo or pick a PDF; a cloud function extracts the store, date, totals, and every line item, each auto-categorized
- **Categories** — a two-level built-in taxonomy plus user-created custom categories
- **Budgets** — monthly/weekly budgets, income, and recurring bills
- **Insights** — spending trends, category breakdowns, highlights, savings rate, top stores
- **History** — searchable, filterable transaction & receipt history
- **Widgets** — home-screen widgets via Jetpack Glance
- **Localized & adaptive** — 21 languages, 19 currencies, phone + tablet layouts
- **Premium** — optional tier via Google Play Billing

## Tech stack

| Area | Choice |
|------|--------|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| DI / data | Koin, Room, Coroutines + Flow |
| Backend | Firebase Auth + Cloud Functions (Node.js) |
| AI extraction | Anthropic Claude (Messages API) via the `extractReceipt` function |
| Other | Jetpack Glance (widgets), Play Billing, Play In-App Updates |

Receipt images are sent to the cloud function only for extraction and are never persisted — on the device or the server.

## Project layout

```
app/          Android application
functions/    Firebase Cloud Function (extractReceipt) + prompt & offline eval harness
CHANGELOG.md  Per-version release notes
```

## Building

Requirements: Android Studio (latest), JDK 11, Android SDK 36 (min SDK 28 / Android 9).

This is a Firebase app, so two local files are required and are **not** committed to the repo:

1. **`app/google-services.json`** — download from your Firebase project (Project settings → your Android app) and place it at `app/google-services.json`.
2. **`keystore.properties`** (repo root, **release builds only**):
   ```properties
   storeFile=your-upload-key.jks
   storePassword=…
   keyAlias=…
   keyPassword=…
   ```
   Debug builds don't need this.

Then:

```bash
./gradlew assembleDebug      # debug APK
./gradlew bundleRelease      # signed release AAB (requires keystore.properties)
```

## Cloud function

The receipt-extraction proxy lives in `functions/`. It calls the Anthropic API using a key held as a Firebase secret — never shipped in the app.

```bash
cd functions
npm install
firebase functions:secrets:set ANTHROPIC_API_KEY   # one-time
firebase deploy --only functions
```

Prompt/format eval (offline, no API key needed):

```bash
node eval/run-eval.js --coverage
```

## Versioning

Semantic `MAJOR.MINOR.PATCH`; the Play `versionCode` is derived as `major*100 + minor*10 + patch`. See [CHANGELOG.md](CHANGELOG.md).

## License

No open-source license yet — all rights reserved. Add a `LICENSE` file if you intend others to reuse the code.
