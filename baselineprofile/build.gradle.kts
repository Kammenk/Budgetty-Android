plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

// The producer module: a com.android.test project whose only job is to run the Macrobenchmark that
// records which classes and methods :app touches during the traced journey. The androidx.baselineprofile
// plugin then feeds that recording back into :app as src/release/generated/baselineProfiles/. Nothing
// here ships — it exists purely to generate an artifact.
android {
    namespace = "com.budgetty.app.baselineprofile"
    compileSdk = 36

    defaultConfig {
        // Macrobenchmark needs minSdk 28+. Matches :app so the profile is generated against the same
        // floor it will run on.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // The app under test. The plugin drives :app's nonMinifiedRelease variant (a release build with
    // R8 off) so the recorded profile reflects real release code paths.
    targetProjectPath = ":app"
}

// Generate on whatever rooted/API-33+ device is plugged in (the CI-managed-device path is deliberately
// not used — see RELEASE.md / the how-to at the top of BaselineProfileGenerator).
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.junit)
}
