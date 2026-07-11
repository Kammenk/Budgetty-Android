# Scan-quality change — revert guide

This documents the **scan-quality** change (2026-07-11, shipped in **10.3.0 / versionCode 1030**) so it
can be undone cleanly if it ever causes trouble. Two things landed together:

- **Part A — Document-scanner capture.** The receipt "camera" step now launches the **ML Kit Document
  Scanner** (auto edge-detection, deskew, glare handling, review/retake) instead of the raw
  `ACTION_IMAGE_CAPTURE` camera intent. Root cause it fixed: the intent produced marginal images, so the
  model dropped/merged lines and a re-scan "fixed" it. See the `scan-capture-quality-2026-07` memory.
- **Part B — Dropped-line guard.** A **blocking "Double-check your items" dialog** on Finalize when the
  scanned items sum to noticeably **less than the receipt's printed subtotal** — the signature of a
  dropped/under-read line that the printed-grand-total anchor would otherwise absorb into `extraCharges`.

Everything landed in one merge commit: **`606f0ea`** (`Merge scan-quality-doc-scanner …`).
This file lives in a **later** commit on `main`, so reverting the merge does **not** delete it.

---

## Fastest full revert (both parts)

```sh
git revert -m 1 606f0ea
```

That reverts to the pre-feature `main` (parent 1). Then **also** undo the release bookkeeping, which is
a separate commit and is *not* touched by the revert above:

- `app/build.gradle.kts` — set `verMinor` back to `2` (10.3.0 → 10.2.0) **only if 10.3.0 was never
  uploaded to Play**. If it *was* uploaded, do **not** lower the version — cut a new higher version whose
  notes say the scanner was rolled back (Play `versionCode` must strictly increase).
- `CHANGELOG.md` — remove or amend the `10.3.0` entry.

Rebuild: `./gradlew bundleRelease`.

> To revert only one part, don't use the merge revert — do the manual surgery below.

---

## Full change inventory

### Dependency (Part A)
- **`gradle/libs.versions.toml`** — `mlkitDocScanner = "16.0.0-beta1"` under `[versions]`, and
  `mlkit-document-scanner = { group = "com.google.android.gms", name = "play-services-mlkit-document-scanner", version.ref = "mlkitDocScanner" }` under `[libraries]`.
- **`app/build.gradle.kts`** — `implementation(libs.mlkit.document.scanner)`.
- Remove both to drop the dependency. (Release builds have `isMinifyEnabled = false`, so there are **no
  ProGuard keep rules** to remove.)

### Part A — Document-scanner capture — all in `app/src/main/java/com/budgetty/app/ui/upload/UploadScreen.kt`
- **Imports:** `android.app.Activity`, `android.content.ContextWrapper`,
  `androidx.activity.result.IntentSenderRequest`, and the three
  `com.google.mlkit.vision.documentscanner.GmsDocumentScanning*` imports.
- **`docScanner` client** + **`scanLauncher`** + **`addReceiptScanLauncher`** (the two
  `StartIntentSenderForResult` launchers), declared next to the existing camera launchers.
- **`launchSource`** — the `"camera"` branch calls the scanner with an `addOnFailureListener` fallback.
  To revert, restore it to the original body:
  ```kotlin
  "camera" -> {
      val uri = createReceiptImageUri(context)
      cameraUri = uri
      cameraLauncher.launch(uri)
  }
  ```
- **`onTakePhoto`** (in the `AddReceiptSourceSheet` call) — calls the scanner with a fallback. Restore to:
  ```kotlin
  onTakePhoto = {
      showAddReceiptSheet = false
      val uri = createReceiptImageUri(context)
      addReceiptUri = uri
      addReceiptCameraLauncher.launch(uri)
  },
  ```
- **`persistScannedImage(context, source)`** helper — copies the scanner's output into our cache as a
  `.jpg` under our FileProvider so extraction gets a readable `image/jpeg` uri. Remove it.
- **`Context.findActivity()`** helper — remove it.
- The original `cameraLauncher`, `addReceiptCameraLauncher`, `cameraUri`, `addReceiptUri`,
  `createReceiptImageUri()` and `fileLauncher` were **kept** (they are the fallback), so reverting Part A
  just means routing back to them and deleting the scanner additions above.

### Part B — Dropped-line guard
- **`app/src/main/java/com/budgetty/app/data/ingest/ParsedReceipt.kt`** — the `receiptSubtotal:
  BigDecimal?` field.
- **`app/src/main/java/com/budgetty/app/data/ingest/HaikuReceiptExtractor.kt`** — the line populating
  `receiptSubtotal = (response.subtotal …)` in the `ParsedReceipt(...)` construction. (`ExtractResponse.subtotal`
  already existed — leave it.)
- **`app/src/main/java/com/budgetty/app/ui/upload/UploadViewModel.kt`** — `receiptSubtotal` in
  `UploadUiState`, its assignment in `onReceiptPicked`, and the `receiptSubtotal = null` lines in
  `startManual` / `startEdit`.
- **`UploadScreen.kt`** — the `AlertDialog` import; the `receiptSubtotal` parameter on `ReviewList` and the
  `receiptSubtotal = state.receiptSubtotal` argument in `UploadScreenContent`; the `itemsShortfall`,
  `showMismatchDialog`, `attemptFinalize` block and the `AlertDialog { … }`; and the two Finalize
  `onClick = attemptFinalize` (restore to `onClick = onFinalize`). The soft `PriceMismatchNotice`
  (over-read warning) predates this change — **leave it**.
- **Strings** — `upload_items_missing_title` and `upload_save_anyway` in `values/strings.xml` **and all 20
  `values-*/strings.xml`**. `upload_total_mismatch` (reused as the dialog body) predates this — leave it.
  Remove with:
  ```sh
  grep -rl 'upload_items_missing_title\|upload_save_anyway' app/src/main/res/values*/strings.xml \
    | xargs sed -i '' '/upload_items_missing_title\|upload_save_anyway/d'
  ```

### Diagnostics (safe to keep even if reverting the feature)
- **`UploadViewModel.kt`** — `import android.util.Log` and the two `Log.w("BudgettyScan", …)` calls in the
  `onReceiptPicked` / `attachAndScan` catch blocks. Privacy-safe (no receipt content); useful for
  diagnosing any future scan failure via `adb logcat -s BudgettyScan`.

---

## Notes / gotchas
- **`16.0.0-beta1`** is Google's only published version of the document scanner (a long-lived beta), widely
  used in production. If it's ever pulled, Part A must be reverted to the camera-intent path.
- The scanner needs **Google Play services**; on a device without it, `getStartScanIntent` fails and the
  code already **falls back** to the old camera intent — so Part A degrades gracefully rather than breaking.
- ML Kit scanner output URIs can report a **null MIME type**; `persistScannedImage` re-hosting them is what
  makes extraction accept them. If you keep the scanner but drop that helper, scans fail with "Scanning is
  temporarily unavailable".
