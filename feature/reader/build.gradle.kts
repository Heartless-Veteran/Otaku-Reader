plugins {
    alias(libs.plugins.otakureader.android.feature)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

android {
    namespace = "app.otakureader.feature.reader"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { testTask ->
                testTask.systemProperty(
                    "roborazzi.output.dir",
                    "${project.projectDir}/src/test/screenshots",
                )
                // Forward -Proborazzi.test.record=true / -Proborazzi.test.verify=true from CLI
                listOf("roborazzi.test.record", "roborazzi.test.verify").forEach { prop ->
                    if (project.hasProperty(prop)) {
                        testTask.systemProperty(prop, project.property(prop)!!)
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.preferences)
    implementation(projects.core.ui)
    implementation(projects.core.discord)
    implementation(projects.domain)
    implementation(projects.sourceApi)
    implementation(libs.paging.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    // WorkManager is used in ReaderViewModel.onCleared() to enqueue guaranteed history writes (H-5)
    implementation(libs.workmanager.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)

    // Compose UI tests on JVM (Robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit)
}

kover {
    reports {
        verify {
            rule {
                // Ratchet floor from measured coverage (Kover 0.9.8, 2026-06-10). The previous 60% gate passed vacuously because Kover 0.8.x could not instrument AGP 9 modules. Raise this as coverage improves; never lower it.
                minBound(15)
            }
        }
    }
}
