import java.time.Duration

plugins {
    alias(libs.plugins.otakureader.kotlin.library)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.okhttp.core)
}

// This module runs network-dependent smoke tests — it must not run in regular CI.
// Invoke via: ./gradlew :tools:extension-smoke-test:test
// or via the manual-dispatch extension-smoke-test.yml workflow.
tasks.withType<Test> {
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
    // 60 s per test — network calls to GitHub Raw + APK download
    timeout.set(Duration.ofSeconds(60))
}
