# Releasing Budgetty

How we ship. The one rule that shapes everything below: **Budgetty has two release lanes with
very different risk profiles — keep them separate.**

| Lane | Ships via | Reach | Speed | Reversible? |
|---|---|---|---|---|
| **Backend** — receipt extraction (prompt, model, `functions/`) | `firebase deploy --only functions` | every user instantly, all app versions | seconds | yes — redeploy prior commit / flip a flag |
| **App** — everything in `app/` (UI, client reconciliation, guards) | Play Console (AAB) | only users who update, at their own pace | hours–days | **no** — you can't un-ship an update |

Most day-to-day fixes are *extraction quality* — the prompt, the eval, the model — and those live in
the **backend** lane. They never need an app release. Reach for the slow app lane only when `app/`
code actually changed.

---

## Cadence

- **Backend:** deploy whenever a change is ready and the eval is green. It's instant and fully
  reversible, so iterate freely — the [eval harness](functions/eval/README.md) is the gate, not a
  release calendar.
- **App:** **batch changes; don't drip.** Collect work on `main`, cut a release when there's a batch
  worth shipping — realistically no more than ~weekly, bi-weekly is fine. **Always staged rollout**
  (see below). Hotfixes are the only exception: a single PATCH, fast-tracked, still staged.

The habit from internal testing — multiple app uploads a week — stops at production. The urge behind
it (fixing scans) mostly belongs in the backend lane anyway.

---

## Versioning

One place, three numbers: `verMajor` / `verMinor` / `verPatch` in
[`app/build.gradle.kts`](app/build.gradle.kts) (~line 40). Everything else derives:

```
versionCode = verMajor*100 + verMinor*10 + verPatch      // 10.6.1 -> 1061
versionName = "verMajor.verMinor.verPatch"               // "10.6.1"
```

- **PATCH** for fixes, **MINOR** for features, **MAJOR** for big shifts.
- Keep MINOR and PATCH in **0–9** so the code stays monotonic; at `.9` roll into the next place
  (10.6.9 → 10.7.0). `versionCode` must **strictly increase on every Play upload** — it's the only
  thing Play orders by.
- Full scheme and history live in [`CHANGELOG.md`](CHANGELOG.md).

---

## Backend release (Cloud Functions)

```bash
# 1. Gate: run the offline eval on the real prompt (no deploy, ~$0.15 in API tokens).
cd functions
ANTHROPIC_API_KEY="$(firebase functions:secrets:access ANTHROPIC_API_KEY)" node eval/run-eval.js
#    Green = all cases pass except any documented KNOWN-RED (e.g. bg-kaufland-interleaved).

# 2. Deploy.
cd ..
firebase deploy --only functions          # project budgetty-96a3d, region europe-west1

# 3. Watch.
firebase functions:log                    # confirm real scans succeed; look for 400s / errors
```

- **De-risk big changes behind a flag.** The `HAIKU_FIRST` env var is the pattern: ship new behavior
  dark, flip it on, kill-switch it off — no redeploy. Prefer this over a risky all-at-once deploy.
- **Rollback:** `git checkout <prev-commit> -- functions/ && firebase deploy --only functions`
  (or flip the flag off). Fast and total — this is why extraction logic lives server-side.
- The Anthropic key comes from Secret Manager (`defineSecret`), never `.env`. Never print it.

---

## App release (Play)

### 1. Land the code
All changes merged to `main` via their own branch, built and tested. `main` is the release source.

### 2. Bump the version
Edit the three vals in [`app/build.gradle.kts`](app/build.gradle.kts). Confirm the derived
`versionCode` is higher than the last **uploaded** one.

### 3. Write the notes
Add a top section to [`CHANGELOG.md`](CHANGELOG.md) describing only what changed since the last
entry. Then the Play "What's new" text (wrap each language `<en-US>…</en-US>`, **max 500 chars**).

> ⚠️ Play release notes are for users updating from the **last published** version — which may be
> older than the last *built* version. If versions were built but never uploaded, fold their changes
> into these notes too. (This bit us: 10.5.0 and 10.6.0 were built and never published.)

### 4. Build a fresh, signed AAB
```bash
./gradlew clean :app:bundleRelease
# -> app/build/outputs/bundle/release/app-release.aab
jarsigner -verify app/build/outputs/bundle/release/app-release.aab   # expect "jar verified"
```
- Signing is automatic **only if** `keystore.properties` (+ the `.jks` it points to) exist at the
  repo root — both are gitignored. **If they're missing, `bundleRelease` silently produces an
  UNSIGNED AAB and Play rejects it.** Verify with `jarsigner -verify` every time.
- `isMinifyEnabled = false` — leave it off (turning it on needs a full keep-rules pass first).
- **Always build fresh from the tagged commit. Never upload an old locally-built `.aab`.**

### 5. Verify before humans see it
- **Backend touched?** Eval must be green (above).
- **Device smoke:** `scripts/testlab-robo.sh` — Robo across the phone/tablet matrix. Note: a Robo
  "Passed" means **no crash only**; judge real coverage by the screenshot gallery. See
  [`scripts/TESTLAB.md`](scripts/TESTLAB.md).
- **Manual smoke:** install the **Internal** track build on a real device and run the changed flow
  end-to-end. This is the step that catches what automation can't.

### 6. Promote one artifact through the tracks
Upload the AAB to **Internal testing**, smoke it, then **promote the same file** — don't rebuild —
up the ladder:

```
Internal testing  →  Closed testing (beta)  →  Production
```

The Closed track you stood up for the launch gate is now your permanent **beta lane**. Every real
release passes through it.

### 7. Staged rollout — your recall button
Never ship Production at 100%. Start at **10–20%**, hold **24–48h** watching vitals, then ramp
20 → 50 → 100%. If crash/ANR rate rises, **halt the rollout** — the un-updated majority stays safe.

### 8. Tag it
```bash
git tag v<MAJOR.MINOR.PATCH> && git push origin v<MAJOR.MINOR.PATCH>
```

---

## Pre-release checklist (app)

```
[ ] All intended changes merged to main, each built + tested
[ ] Version bumped in app/build.gradle.kts; versionCode > last UPLOADED code
[ ] CHANGELOG.md updated; Play "What's new" written (<en-US>…</en-US>, ≤500 chars,
    covers everything since the last PUBLISHED version)
[ ] Fresh ./gradlew clean :app:bundleRelease; jarsigner -verify says "jar verified"
[ ] Eval green (if functions/ changed) + backend deployed first if the app depends on it
[ ] Test Lab Robo run reviewed (screenshots, not just "Passed")
[ ] Manual smoke of the changed flow on the Internal track
[ ] Promote same AAB Internal → Closed → Production
[ ] Production staged at 10–20%; watching vitals before ramping
[ ] git tag vX.Y.Z pushed
```

---

## Rollback / incident playbook

- **Bad backend deploy:** `git checkout <good-commit> -- functions/ && firebase deploy --only
  functions`, or flip the feature flag off. Live in minutes.
- **Bad app release:** you can't recall an update. **Halt the staged rollout** immediately (caps the
  blast radius), then ship a **PATCH hotfix** (new higher versionCode) and stage it. Users on the bad
  build get nudged by the in-app FLEXIBLE update ([`InAppUpdateManager.kt`](app/src/main/java/com/budgetty/app/update/InAppUpdateManager.kt));
  for a severe fix, consider switching that flow to IMMEDIATE for one release.
- Prefer fixing forward on the app lane — a hotfix reaches users faster than any app-side "undo."

---

## Monitoring

- **Play Console → Android vitals** — crash rate and ANR rate. These have Google-enforced bad-behavior
  thresholds; crossing them can suppress or delist the app. Check after every staged-rollout step.
- **Backend:** `firebase functions:log` — watch for extraction 400s/errors after a deploy.
- ⚠️ **Gap: no Crashlytics is wired up yet** (no `firebase-crashlytics` dependency). On production,
  Android vitals alone is a lagging, sampled signal — you're largely flying blind on real stack
  traces. **Wiring up Firebase Crashlytics is the one thing worth doing before the first Production
  rollout.**

---

## One-time: the Production gate

Kamsk Studios is a **personal** Play account, so reaching Production requires **12 testers opted into a
Closed track for 14 continuous days**, then a production-access review (~2–3 weeks total; recruiting the
testers is the bottleneck, not the code). Internal testing's testers don't count toward this. Once
cleared, the Closed track stays as the beta lane in the flow above. Access + status:
[`Play Console`](https://play.google.com/console) under Kamsk Studios (maynawear@gmail.com).
