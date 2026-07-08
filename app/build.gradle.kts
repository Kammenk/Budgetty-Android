import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.screenshot)
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

    // Turn on the Compose Preview Screenshot Testing source set (src/screenshotTest).
    // Required by the com.android.compose.screenshot plugin in addition to the
    // matching flag in gradle.properties.
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    defaultConfig {
        applicationId = "com.budgetty.app"
        minSdk = 28
        targetSdk = 36
        // Semantic version — bump these three and both fields below follow.
        // versionCode = the versionName with the dots removed (10.0.1 -> 1001,
        // 10.1.0 -> 1010, 11.0.0 -> 1100). Keep verMinor/verPatch in 0..9 so the
        // code stays monotonic; roll over to the next place at 9 (10.0.9 -> 10.1.0).
        val verMajor = 10
        val verMinor = 2
        val verPatch = 0
        versionCode = verMajor * 100 + verMinor * 10 + verPatch  // 1020
        versionName = "$verMajor.$verMinor.$verPatch"            // 10.2.0

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
}

dependencies {

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
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Play Billing (subscriptions)
    implementation(libs.billing.ktx)

    // Networking — Haiku receipt extraction via the Cloud Function proxy
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // Receipt ingestion
    implementation(libs.pdfbox.android)

    // Glance — home-screen app widgets (Compose for widgets)
    implementation(libs.androidx.glance.appwidget)

    // Google Play In-App Updates — prompt eligible users to move to the latest build
    implementation(libs.app.update.ktx)

    testImplementation(libs.junit)

    // Compose Preview Screenshot Testing — renders @Preview functions in
    // src/screenshotTest to PNGs on the host JVM (no emulator) and diffs them.
    // validation-api generates the per-preview test cases; ui-tooling provides the
    // @Preview detector. Both pinned (via `screenshot`) to the alpha10 / Kotlin-2.1
    // train so their metadata stays readable by this project's Kotlin 2.0.21 compiler.
    screenshotTestImplementation(platform(libs.androidx.compose.bom))
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.ui.tooling)
    // The JUnit-Platform TestEngine that actually reads the render inputs and rasterizes
    // previews. Must be on the screenshotTest runtime classpath or the Test task's launcher
    // loads no engine and reports 0 tests (rendering nothing).
    screenshotTestRuntimeOnly(libs.screenshot.junit.engine)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}