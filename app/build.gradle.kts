import com.android.build.api.variant.BuildConfigField
import io.gitlab.arturbosch.detekt.Detekt
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.detekt)
    alias(libs.plugins.roborazzi)
}

// Room writes the schema of every DB version here. The JSONs are committed: they are what lets a
// migration test build a database at an older version, and Room diff them to verify a migration
// produces exactly the schema the entities declare. Bump the version without updating these and the
// build fails — which is the point.
room {
    schemaDirectory("$projectDir/schemas")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

android {
    namespace = "com.budgetty.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.budgetty.app"
        minSdk = 28
        targetSdk = 36
        // Semantic version — bump these three and both fields below follow.
        // versionCode = the versionName with the dots removed (10.0.1 -> 1001,
        // 10.1.0 -> 1010, 11.0.0 -> 1100). Keep verMinor/verPatch in 0..9 so the
        // code stays monotonic; roll over to the next place at 9 (10.0.9 -> 10.1.0).
        val verMajor = 10
        val verMinor = 7
        val verPatch = 0
        versionCode = verMajor * 100 + verMinor * 10 + verPatch  // 1070
        versionName = "$verMajor.$verMinor.$verPatch"            // 10.7.0

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Records the pre-existing findings (the codebase had never been gated on Lint) so the CI
        // lintDebug task passes today and only NEW issues fail. Regenerate deliberately by deleting
        // the file and re-running lint. Most current errors are RestrictedApi false positives from
        // calling Glance's own ColorProvider API.
        baseline = file("lint-baseline.xml")
        warningsAsErrors = false
        // Lint runs as its own CI step (lintDebug); don't also run it inside every release assemble.
        checkReleaseBuilds = false
    }
    testOptions {
        unitTests {
            // Robolectric needs merged Android resources on the unit-test classpath (and Room's
            // in-memory builder touches them). The Robolectric SDK itself is pinned in
            // src/test/resources/robolectric.properties — compileSdk 36 is newer than any image
            // Robolectric ships, so tests run against SDK 34.
            isIncludeAndroidResources = true
        }
    }
}

// BuildConfig.TEST_HOOKS_ENABLED gates debug/profiling-only hooks (the SKIP_AUTH auth bypass). True
// for `debug` and for the plugin-created profiling variants `nonMinifiedRelease` / `benchmarkRelease`
// — the Baseline Profile is generated against nonMinifiedRelease, a RELEASE-typed build where
// BuildConfig.DEBUG is false, so DEBUG alone wouldn't let the generator reach past login. False for
// the shipped `release`, so the hook is inert in production. Set via the variant API (single source
// of truth; every variant gets the field, so BuildConfig.TEST_HOOKS_ENABLED resolves everywhere).
androidComponents {
    onVariants { variant ->
        val enabled = variant.name in setOf("debug", "nonMinifiedRelease", "benchmarkRelease")
        variant.buildConfigFields?.put(
            "TEST_HOOKS_ENABLED",
            BuildConfigField("boolean", enabled.toString(), "debug/profiling-only test hooks"),
        )
    }
}

// Compose compiler stability reports. Off by default (they slow the build and write to disk); opt in
// with `-PcomposeMetrics` on any assemble task, e.g.
//
//     ./gradlew assembleRelease -PcomposeMetrics
//
// Then read app/build/compose-reports/app_release-composables.txt: a composable listed as
// `restartable skippable` is fine, `restartable` alone means it recomposes even when its arguments
// haven't changed. app_release-classes.txt names the unstable types responsible. Diagnostic only —
// this changes no shipped code.
composeCompiler {
    // Types the compiler can't prove stable but which are stable here — see the file for the
    // reasoning behind each entry, and for the rule that keeps the collection entries honest.
    // Unlike the metrics above, this one DOES affect shipped code: it changes skipping behaviour.
    stabilityConfigurationFile = layout.projectDirectory.file("compose-stability.conf")

    if (project.hasProperty("composeMetrics")) {
        metricsDestination = layout.buildDirectory.dir("compose-metrics")
        reportsDestination = layout.buildDirectory.dir("compose-reports")
    }
}

// Static analysis. `./gradlew :app:detekt` checks Kotlin for smells, complexity, and (via the
// formatting rules) style. buildUponDefaultConfig means config/detekt/detekt.yml only holds overrides
// on top of detekt's defaults. baseline.xml records every pre-existing finding on this 113-file
// codebase so the task passes today and only NEW code is gated — regenerate it deliberately with
// `./gradlew :app:detektBaseline`, never as a way to silence a fresh finding.
detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("$rootDir/config/detekt/baseline.xml")
    parallel = true
}

// Roborazzi golden images live in a committed directory (not build/) so they act as the reviewed
// reference. `./gradlew :app:recordRoborazziDebug` writes them; `verifyRoborazziDebug` (run in the
// normal test task) fails on a pixel diff.
roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}

// Roborazzi is published compiled with a newer Kotlin (2.3 metadata) than the project's compiler
// (2.0.21), though it targets the 2.0.21 stdlib — so its runtime API is compatible and only the
// metadata version check trips. Relax that check for TEST compilation only; main/release compilation
// stays strict, and nothing in main depends on Roborazzi.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>()
    .matching { it.name.contains("Test", ignoreCase = true) }
    .configureEach {
        compilerOptions.freeCompilerArgs.add("-Xskip-metadata-version-check")
    }

tasks.withType<Detekt>().configureEach {
    jvmTarget = "11"
    reports {
        html.required.set(true)
        sarif.required.set(true) // consumed by the GitHub code-scanning upload in CI
        md.required.set(false)
        txt.required.set(false)
    }
}

dependencies {
    detektPlugins(libs.detekt.formatting)


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Lifecycle / ViewModel for Compose
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Koin (DI)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Firebase Auth + Credential Manager (Google Sign-In)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    // Crash reporting. Collection is default-on with an opt-out toggle in Account (CrashReporting).
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Play Billing (subscriptions)
    implementation(libs.billing)

    // Networking — Haiku receipt extraction via the Cloud Function proxy
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // Glance — home-screen app widgets (Compose for widgets)
    implementation(libs.androidx.glance.appwidget)

    // Google Play In-App Updates — prompt eligible users to move to the latest build
    implementation(libs.app.update.ktx)

    // Google Play In-App Review — the native rating card, asked after a successful scan
    implementation(libs.app.review.ktx)

    // Applies the generated Baseline Profile (baselineprofile/) on-device at install for AOT
    // compilation of the startup path. The :baselineprofile module records it; this consumes it.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    // ML Kit Document Scanner — high-quality receipt capture (auto edge-detect, deskew, glare
    // handling + review/retake) in place of the raw camera intent, which produced marginal images.
    implementation(libs.mlkit.document.scanner)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)              // run Android/Room code on the JVM, no emulator
    testImplementation(libs.turbine)                  // assert on Flow emissions
    testImplementation(libs.kotlinx.coroutines.test)  // runTest + virtual-time dispatcher
    testImplementation(libs.androidx.room.testing)     // in-memory Room database for DAO tests
    testImplementation(libs.androidx.test.core)        // ApplicationProvider for Robolectric
    testImplementation(libs.androidx.core.testing)     // InstantTaskExecutorRule for arch components
    testImplementation(libs.truth)
    testImplementation(libs.roborazzi)                // JVM screenshot capture + compare
    testImplementation(libs.roborazzi.compose)         // captureRoboImage for Compose
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.ui.test.junit4)   // createComposeRule under Robolectric
    testImplementation(libs.androidx.ui.test.manifest) // ComponentActivity host for the compose rule
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}