# Device-test bugs — 11 new features (2026-10-09)

Found while testing all 11 competitor features on **Android `main`** (Pixel 10 Pro Fold, debug build) and
**iOS `main`** (iPhone 17 Pro simulator) with a real backup (113 receipts / 345 transactions / 5 bills).
Paths starting `Budgetty/…` (iOS) are relative to the iOS repo (`~/Budgetty iOS/Budgetty`); the rest
are in this repo.

## Status — ALL FIXED (2026-10-09)

Android: branch `fix/device-test-bugs`, merged to `main` (`81db9b0`) + pushed. Verified on the Pixel 10 Pro
Fold **unfolded** (debug build) + 425 JVM tests, detekt, lintDebug green.
iOS: branch `fix/device-test-bugs`, merged to iOS `main` (`4965f39`) + pushed; 345 tests green, sim-verified.

| # | Fix | Verified |
|---|-----|----------|
| 1 | `recurringDao.clearAll()` restored; `BackupManagerReplaceTest` drives the real import and fails if any table isn't cleared | Fold: 2× Replace all → still 5 bills, Home figures unchanged |
| 2 | Active trip read from the DB when a fresh entry starts (`UploadViewModelTripTagTest`) | Fold: manual + template entry start with `#lisbon-2026` |
| 3 | Shared `ui/util/DatePickerMillis.kt` at all 4 M3 pickers (warranty, both savings sheets, review) | Fold: 3× OK keeps 1 Oct |
| 4 | iOS `RecurringDTO.autoPay` (+ paid stamp, category bucket/overrides, tag catalog) | Sim: Android backup restore → 110,91 € due, 3 bills |
| 5 | Template picker gets Manage-categories actions + shared `categorySuggestionsFlow` | Fold: Create works, customs + Suggested row shown |
| 6 | iOS buckets by `Receipt.date` / `LineItem.purchaseDate` everywhere | Sim: October 1416,25 € = Android |
| 7 | Both directions: Android `IosBackupConverter`, iOS `AndroidBackup.swift` | Android→iOS→Android round trip identical (only lossy bit: Android-only Insights sections, iOS `system` date format) |
| 8 | Both: replace clears rollover + ignored subscriptions; `ignoredSubscriptions` now backed up | Unit tests both platforms |
| 9 | Year stamped only if the name doesn't already end in `-YYYY` (`TripEntity.tagFor` / `TripOps`) | Fold: "Lisbon 2026" → `#lisbon-2026` |

Minor items: envelope plural, picker "Your categories" count, FAB + Category TalkBack labels (Android);
price field race (+ every other iOS money field), zero-price placeholder, localized restore prompt,
fortnightly sheet with window + conversion, no pre-picked category on iOS manual entry (iOS).
Not done: the 313 English-only Android strings (waiting on Crowdin).

Also found + fixed on the unfolded Fold: Insights Overview stat tiles clipped in the side pane, Trips
"Past trips" header misaligned, Recap Done button full-width (modifier order).

---

## 1. Android — "Replace all" restore duplicates recurring bills · HIGH

- **Symptom:** after a full-replace restore, the device's existing recurring bills/income are kept and the
  backup's are added on top. On test: 3 × Salary → Income showed **9,200 €/mo instead of 3,200 €**; a
  second replace-restore doubled every bill. Safe-to-spend and bills-due are wrong as a result.
- **Repro:** have any recurring bill → Account → Import data → pick a backup → **Replace all** → Budget tab
  lists both sets.
- **Root cause:** the replace block in `BackupManager.import` clears 17 tables but not `recurring`.
  `recurringDao.clearAll()` was removed by accident in **`24341b8`** ("Include display preferences in the
  JSON data backup", 2026-09-09) — that change had nothing to do with recurring.
- **Where:** `app/src/main/java/com/budgetty/app/data/backup/BackupManager.kt:81-101`
- **Fix:** restore `recurringDao.clearAll()` in the `if (replace)` block + a unit test that replace leaves
  exactly the backup's recurring rows. (iOS already clears `Recurring` correctly.)

## 2. Android — Travel mode never auto-tags new expenses · HIGH

- **Symptom:** with a trip active, a new manual entry or template entry does **not** get the trip tag
  (e.g. `#lisbon-2026`). Back-fill at trip start works; only new expenses are missed — the core promise of
  the feature. iOS works (shows "✈️ Lisbon trip is on" banner + pre-applied pill).
- **Repro:** Account → Trips → start a trip → Home → Add receipt → Add manually (or tap a template) → Tags
  field is empty.
- **Root cause (race):** `UploadViewModel` is created per navigation; `activeTripTag` is filled by an async
  `tripRepository.activeTrip.collect` started in `init`, but `startManual()` runs immediately from the
  screen's `LaunchedEffect`, so `seedTrip()` still sees `null`.
- **Where:** `app/src/main/java/com/budgetty/app/ui/upload/UploadViewModel.kt:163,217,305` and
  `app/src/main/java/com/budgetty/app/ui/upload/UploadScreen.kt:227`
- **Fix:** read the active trip with a suspend `first()` inside `startManual` / the scan-parse paths (or make
  the seeded rows react to the trip flow) + a test that a manual entry during an active trip carries the tag.

## 3. Android — Warranty purchase date drifts back a day · HIGH

- **Symptom:** opening the purchase-date picker and tapping OK **without changing anything** moves the date
  back one day (1 Oct → 30 Sep → 29 Sep). The picker also opens on the wrong day. Hits every user east of
  UTC — i.e. the whole (European) market.
- **Root cause:** local-midnight millis are passed to the M3 `DatePicker`, which expects UTC-midnight millis.
- **Where:** `app/src/main/java/com/budgetty/app/ui/warranties/WarrantiesScreen.kt:470-475`
- **Likely also affected (same pattern, not tested):**
  `app/src/main/java/com/budgetty/app/ui/savings/SavingsSheets.kt:240` and `:390` (savings goal / contribution dates).
- **Fix:** use the UTC↔local conversion `UploadScreen` already uses (`toUtcDayMillis()` / `toLocalDayMillis()`,
  `UploadScreen.kt:1117-1122`).

## 4. iOS — Backups lose the Autopay setting · HIGH

- **Symptom:** after any restore (iOS→iOS too), Autopay is off on every bill. On test the Car Loan
  (autopay) showed as still due: **790.49 € due instead of 110.91 €**, safe-to-spend off by 679.58 €.
- **Root cause:** `RecurringDTO` has no `autoPay` field (also no `nextDue` / `lastPosted`).
- **Where:** `Budgetty/Data/Backup.swift:100-113` (iOS repo)
- **Fix:** add optional `autoPay: Bool?` to `RecurringDTO` (optional so old backups still decode), write it
  on export, apply on restore.

## 5. Android — Template editor's category picker is half-wired · MEDIUM

- **Symptom:** in Account → Templates → New template → Category: tapping **Create** and saving a new
  category does nothing — no category is created, but the template is left pointing at that non-existent
  name (blank icon). The picker there also shows **no "Suggested for you" row** and **none of the user's
  custom categories**.
- **Root cause:** `CategoryPickerScreen` is called without `custom = …` (defaults to no-op save/empty list)
  and without category suggestions provided.
- **Where:** `app/src/main/java/com/budgetty/app/ui/templates/TemplatesScreen.kt:362-368`
- **Fix:** pass the same `CustomCategoryActions` + suggestions the Upload screen passes (see
  `UploadScreen.kt:1397`). iOS is not affected (its picker loads its own data).

## 6. Parity — iOS and Android put late-scanned receipts in different months · MEDIUM

- **Symptom:** same data, different period totals. October on test: Android **1,416.25 €**, iOS
  **1,424.81 €** — the 8.56 € gap is two receipts *dated* June/September but *uploaded* in October.
  18 of 123 receipts in the test data fall in a different month by upload date.
- **Root cause:** iOS filters periods by `Receipt.createdAt` (upload moment); Android uses the transaction
  timestamp (= the receipt's printed date). Editing a receipt's date on iOS therefore doesn't move it.
- **Where:** `Budgetty/Scenes/Home/HomeView.swift:114` (+ the other `createdAt` window filters in
  HomeView / InsightsView / RecapBuilder / WellbeingSummary)
- **Fix:** bucket by `Receipt.date` on iOS to match Android (check every `window.contains($0.createdAt)`).

## 7. Cross-platform — iOS cannot import an Android backup at all · MEDIUM

- **Symptom:** "That file isn't a valid Budgetty backup." (earlier notes said cross-platform restore
  "degrades" — it actually fails outright).
- **Root cause:** different schemas: Android = flat `transactions` + epoch-ms numbers; iOS = receipts with
  nested `items` + ISO-8601 dates, different key names (`budgetKey`→`key`, `cadence`→`cadenceRaw`,
  newline-joined lists vs arrays). Settings enums also differ in case (`SYSTEM` vs `system`) despite the
  `SettingsDTO` comment claiming parity — a converted file set the iOS language override to `"SYSTEM"`.
- **Where:** `Budgetty/Data/Backup.swift` (iOS) vs `app/src/main/java/com/budgetty/app/data/backup/BackupData.kt`
- **Fix (decide first):** either make iOS accept the Android format (an adapter decode path), or state
  clearly in-app that backups are platform-specific.

## 8. Both — "Replace all" leaves the budget carry-over behind · MEDIUM

- **Symptom:** a stale `MONTHLY 2026-10` carry-over of **1,200 €** from the pre-restore data survived a
  replace restore on iOS (seen in the DB; UI impact not checked).
- **Root cause:** neither platform clears budget rollover (Android `budget_rollover`, iOS `BudgetRollover`)
  — nor ignored subscriptions — on replace.
- **Where:** `BackupManager.kt:81-101` (Android); `Budgetty/Data/Backup.swift` restore `.replace` block (iOS)
- **Fix:** clear both tables on replace (rollover is recomputed; ignored-subscriptions: decide whether it
  should be backed up).

## 9. Both — Trip tag repeats the year · LOW

- **Symptom:** naming a trip "Lisbon 2026" produces `#lisbon-2026-2026`.
- **Where:** `app/src/main/java/com/budgetty/app/ui/trips/TripsViewModel.kt:172` + the preview at
  `TripsScreen.kt:591`; iOS `Budgetty/Scenes/Trips/TripOps.swift:72`
- **Fix:** don't append the year if the normalized name already ends with `-<year>`.

---

## Minor / polish

- **Android:** envelope subtitle says "1 categories" (`envelopes_scope_categories` should be a plural).
- **Android:** category picker header shows "Your categories **1**" but no tile under it — a custom category
  with a parent (e.g. "Loan" under Other) is counted but only listed inside its group.
- **Android:** the Home **Add receipt** FAB and the review screen's **Category** field have no accessibility
  label (not in the a11y tree → TalkBack users can't identify them).
- **iOS (unconfirmed):** a price typed as "2,80" and saved immediately was stored as **2**; a second try
  with a pause before Save stored 4.5 correctly. Possible commit race on
  `TextField(value:format:)` in `Budgetty/Scenes/Scan/ReviewView.swift:387` — worth a second look.
- **iOS:** price field pre-fills "0" and doesn't select it on focus → typing gives "018.50" (parses fine).
- **iOS:** the restore prompt title ("Import 113 receipts and 345 items?") is English in a German UI.
- **Android:** 313 new strings are English-only (`tools:ignore="MissingTranslation"`) — expected until Crowdin
  is reactivated.
- **Parity:** iOS fortnightly confirmation is a plain alert without the window dates / "650 € → 300 €"
  conversion Android shows; iOS applies the cadence immediately, Android needs Save; iOS manual entry
  pre-selects Groceries, Android leaves Category empty.

## Not covered by this test

- Free-tier caps (warranties 5, envelopes 1, Forecast lock) — debug builds force Premium.
- Fold **unfolded** (tablet / two-pane layouts).
- iOS Planners, Templates, Warranties and CSV-import screens were only code-checked, not driven in the UI.
- Real iOS device.
