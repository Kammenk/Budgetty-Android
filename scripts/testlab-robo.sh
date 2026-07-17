#!/usr/bin/env bash
#
# Firebase Test Lab — Robo test runner for Budgetty.
#
# Uploads a debug APK and runs an automated Robo crawl across a matrix of virtual
# devices (phones + tablets, several API levels, both orientations). Robo taps
# through the UI on its own — no test code needed — and captures screenshots, a
# video, activity map, and a crash/ANR report per device. This is how we check the
# app renders and behaves across screen sizes/densities.
#
# The app opens on a login wall, so Robo is fed the test account via --robo-directives,
# which target the login fields by the testTags added in LoginScreen.kt / MainActivity.kt
# (exposed to Robo as resource-ids by the root `testTagsAsResourceId`).
#
# USAGE
#   scripts/testlab-robo.sh run        # build (if needed) + run the matrix  [default]
#   scripts/testlab-robo.sh build      # just build the debug APK
#   scripts/testlab-robo.sh devices    # list available virtual device models
#   scripts/testlab-robo.sh versions   # list available OS versions
#
# PREREQUISITES (one-time, done by you — see scripts/TESTLAB.md):
#   1. gcloud CLI installed and on PATH (or set GCLOUD=/path/to/gcloud)
#   2. gcloud auth login   &&   gcloud config set project budgetty-96a3d
#   3. Firebase project on the Blaze (pay-as-you-go) billing plan
#   4. Credentials set: copy scripts/testlab.env.example -> scripts/testlab.env and fill in,
#      or export BUDGETTY_TEST_EMAIL / BUDGETTY_TEST_PASSWORD in your shell.
#
set -euo pipefail

# --- Resolve repo root (this script lives in <root>/scripts) ---------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

# --- Config (override any of these via the environment) --------------------------
PROJECT="${BUDGETTY_FIREBASE_PROJECT:-budgetty-96a3d}"
APK="${BUDGETTY_APK:-app/build/outputs/apk/debug/app-debug.apk}"
ROBO_TIMEOUT="${BUDGETTY_ROBO_TIMEOUT:-180s}"   # per-device crawl budget
RESULTS_BUCKET_DIR="${BUDGETTY_RESULTS_DIR:-robo-$(date +%Y%m%d-%H%M%S)}"

# Resolve gcloud: PATH first, then the default tarball location, then GCLOUD override.
GCLOUD="${GCLOUD:-$(command -v gcloud || true)}"
if [ -z "$GCLOUD" ] && [ -x "$HOME/google-cloud-sdk/bin/gcloud" ]; then
  GCLOUD="$HOME/google-cloud-sdk/bin/gcloud"
fi

# --- Device matrix ---------------------------------------------------------------
# Each entry: "model,version,orientation". Edit freely. Confirm valid model/version
# IDs with `scripts/testlab-robo.sh devices` and `... versions` (gcloud lists valid
# options in its error if one is wrong). The generic *.arm models are Google's
# virtual (emulator) profiles; SmallPhone / MediumPhone / MediumTablet give us a
# small phone, a standard phone, and a tablet — i.e. the range of sizes we care about.
DEVICES=(
  # model,version,orientation — validated against `scripts/testlab-robo.sh devices`
  # (the VIRTUAL catalog). Spread across 3 physical sizes + a low-dpi tablet, 4 API
  # levels (28 min sdk → 36 target sdk), and both orientations, so screen size,
  # window insets/edge-to-edge, and density are all exercised.
  "SmallPhone.arm,28,portrait"           # 4.65" small phone, MIN sdk (old-API insets)
  "SmallPhone.arm,35,portrait"           # 4.65" small phone, newer API
  "MediumPhone.arm,28,portrait"          # 6.4" standard phone, min sdk
  "MediumPhone.arm,33,portrait"          # 6.4" standard phone, mid API
  "MediumPhone.arm,36,portrait"          # 6.4" standard phone, TARGET sdk
  "MediumPhone.arm,36,landscape"         # 6.4" standard phone, landscape
  "Pixel2.arm,33,portrait"               # 5" 16:9 phone, a different aspect ratio
  "MediumTablet.arm,35,portrait"         # 10" tablet, portrait
  "MediumTablet.arm,35,landscape"        # 10" tablet, landscape (two-pane master/detail)
  "AndroidTablet270dpi.arm,30,landscape" # 7" low-density tablet, landscape (density check)
)

# --- Helpers ---------------------------------------------------------------------
die() { echo "ERROR: $*" >&2; exit 1; }

require_gcloud() {
  [ -n "$GCLOUD" ] && [ -x "$GCLOUD" ] || die \
"gcloud not found. Install it (see scripts/TESTLAB.md) or set GCLOUD=/path/to/gcloud."
}

load_credentials() {
  # Optional dotenv file (gitignored) with the test account.
  if [ -f "$SCRIPT_DIR/testlab.env" ]; then
    # shellcheck disable=SC1091
    set -a; . "$SCRIPT_DIR/testlab.env"; set +a
  fi
  : "${BUDGETTY_TEST_EMAIL:?Set BUDGETTY_TEST_EMAIL (see scripts/testlab.env.example)}"
  : "${BUDGETTY_TEST_PASSWORD:?Set BUDGETTY_TEST_PASSWORD (see scripts/testlab.env.example)}"
}

build_apk() {
  echo "==> Building debug APK…"
  ./gradlew :app:assembleDebug
}

# --- Subcommands -----------------------------------------------------------------
cmd_build() { build_apk; echo "APK: $APK"; }

cmd_devices() {
  require_gcloud
  "$GCLOUD" firebase test android models list --project "$PROJECT"
}

cmd_versions() {
  require_gcloud
  "$GCLOUD" firebase test android versions list --project "$PROJECT"
}

cmd_run() {
  require_gcloud
  load_credentials
  [ -f "$APK" ] || build_apk
  [ -f "$APK" ] || die "APK not found at $APK"

  # Build the --device flags from the matrix.
  local device_args=()
  for d in "${DEVICES[@]}"; do
    IFS=',' read -r model version orientation <<< "$d"
    device_args+=( --device "model=${model},version=${version},orientation=${orientation}" )
  done

  # Robo login: fill the two fields (matched by testTag/resource-id) then tap Sign in.
  local directives="text:login_email=${BUDGETTY_TEST_EMAIL},text:login_password=${BUDGETTY_TEST_PASSWORD},click:login_sign_in="

  echo "==> Running Robo on ${#DEVICES[@]} device configs (project $PROJECT, timeout $ROBO_TIMEOUT each)…"
  # --robo-directives is passed via KEY=VALUE stdin-safe form; results land in the
  # project's default Test Lab bucket under gs://…/$RESULTS_BUCKET_DIR and in the Console.
  "$GCLOUD" firebase test android run \
    --type robo \
    --project "$PROJECT" \
    --app "$APK" \
    --timeout "$ROBO_TIMEOUT" \
    --results-dir "$RESULTS_BUCKET_DIR" \
    --robo-directives "$directives" \
    "${device_args[@]}"

  echo
  echo "Done. Open the Firebase Console → Test Lab to view screenshots, video and crashes:"
  echo "  https://console.firebase.google.com/project/${PROJECT}/testlab/histories/"
}

# --- Dispatch --------------------------------------------------------------------
case "${1:-run}" in
  run)      cmd_run ;;
  build)    cmd_build ;;
  devices)  cmd_devices ;;
  versions) cmd_versions ;;
  *) die "Unknown command '${1}'. Use: run | build | devices | versions" ;;
esac
