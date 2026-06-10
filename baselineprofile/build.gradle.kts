plugins {
    // Versionless: AGP is on the build-logic classpath, so a versioned alias request
    // fails plugin resolution. Kotlin support is built into AGP 9. The
    // androidx.baselineprofile Gradle plugin is intentionally NOT applied — it does
    // not support AGP 9 (expects the removed TestExtension type). Profile collection
    // still works through benchmark-macro's BaselineProfileRule; the generated file
    // is copied into app/src/main/baseline-prof.txt manually (see that file's header).
    id("com.android.test")
}

android {
    namespace = "app.otakureader.baselineprofile"
    // Keep compileSdk in sync with the rest of the project (KotlinAndroid.kt).
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // Macrobenchmarks must run against a release-like target. The app defines a
        // matching debug-signed "benchmark" build type; self-instrumentation lets
        // this module act as its own test harness.
        create("benchmark") {
            isDebuggable = true
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    // Gradle Managed Device so CI can run macrobenchmarks without a pre-booted
    // emulator — AGP boots and tears it down itself (#1022).
    testOptions {
        managedDevices {
            localDevices {
                create("pixel6Api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.uiautomator)
    implementation(libs.androidx.benchmark.macro)
}
