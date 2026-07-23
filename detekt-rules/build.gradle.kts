// Custom detekt rules for Budgetty, packaged as a detekt plugin JAR and loaded by the :app detekt
// task via `detektPlugins(project(":detekt-rules"))`. A plain Kotlin/JVM module — no Android.
//
// detekt-api is compileOnly: detekt supplies it (and the embedded Kotlin compiler PSI) at analysis
// time, so shipping it here would clash. Bytecode targets JVM 11 to match the detekt runtime and the
// rest of the build.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test)
    testImplementation(libs.junit)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
