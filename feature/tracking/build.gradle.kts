plugins {
    alias(libs.plugins.otakureader.android.feature)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

android {
    namespace = "app.otakureader.feature.tracking"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.preferences)
    implementation(projects.core.ui)
    implementation(projects.domain)
    implementation(libs.paging.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.browser)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
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
