# Firebase Test Lab — device-size checks via Robo

Run the app across a matrix of virtual phones and tablets in the cloud. Firebase installs
a debug APK on each device and runs a **Robo test** — it crawls the UI automatically (no
test code) and captures **screenshots, a video, an activity map, and a crash/ANR report**
per device. This is our check that the UI renders and behaves across screen sizes/densities.

Everything is driven by [`scripts/testlab-robo.sh`](testlab-robo.sh).

---

## One-time setup

### 1. Install the gcloud CLI
Homebrew isn't installed on this machine, so use Google's tarball (Apple Silicon):

```bash
cd ~
curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-darwin-arm.tar.gz
tar -xzf google-cloud-cli-darwin-arm.tar.gz
./google-cloud-sdk/install.sh --quiet --path-update=true   # add to PATH; open a new terminal after
```

The script also finds gcloud automatically at `~/google-cloud-sdk/bin/gcloud`, so you can skip
the PATH step if you prefer. Verify: `gcloud version`.

### 2. Authenticate and select the project
```bash
gcloud auth login
gcloud config set project budgetty-96a3d
gcloud services enable testing.googleapis.com toolresults.googleapis.com
```

### 3. Enable billing (Blaze plan)
Test Lab needs the **Blaze (pay-as-you-go)** plan. Virtual devices are cheap
(~$1/device-hour; the default 10-config run is ~3 min each ≈ $0.50–1 total), and there's
a small free daily quota. Enable it once at:
<https://console.firebase.google.com/project/budgetty-96a3d/usage/details>

### 4. Provide the test account
Robo signs in before it can crawl the app. Use a **disposable test account**, never a real user:

```bash
cp scripts/testlab.env.example scripts/testlab.env
# edit scripts/testlab.env → the +budgetty free-tier test account
```

`scripts/testlab.env` is gitignored.

---

## Running

```bash
scripts/testlab-robo.sh            # build the debug APK if needed, then run the matrix
scripts/testlab-robo.sh devices    # list valid virtual device models
scripts/testlab-robo.sh versions   # list valid OS versions
scripts/testlab-robo.sh build      # just build the APK
```

Results print a Console link, or browse them at:
<https://console.firebase.google.com/project/budgetty-96a3d/testlab/histories/>

Each device shows the screenshot gallery (great for eyeballing layout at each size), the
crawl video, and any crashes/ANRs with stack traces.

---

## The device matrix

Edit the `DEVICES` array in [`testlab-robo.sh`](testlab-robo.sh). Each line is
`model,version,orientation`. The default is 10 configs spanning a 4.65" small phone, a 6.4"
standard phone, a 5" 16:9 phone, a 10" tablet, and a 7" low-density tablet — across API
levels 28 (min sdk) → 36 (target sdk) and both orientations, so screen size, insets/edge-to-
edge, and density all get exercised. Validate model/version IDs with the `devices` /
`versions` subcommands; if one is wrong, gcloud lists the valid options in its error.

## How Robo gets past login (implementation note)

Robo matches controls by resource-id, which Jetpack Compose doesn't emit by default. So:

- `MainActivity.kt` sets `testTagsAsResourceId = true` on the root `Surface`, exposing every
  Compose `testTag` as a view resource-id.
- `LoginScreen.kt` tags the three login controls: `login_email`, `login_password`,
  `login_sign_in` (constants `LoginTag*`).
- The script passes `--robo-directives text:login_email=…,text:login_password=…,click:login_sign_in=`.

If you rename those tags, update the directives in the script to match.
