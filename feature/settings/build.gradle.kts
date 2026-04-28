plugins {
    alias(libs.plugins.otakureader.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.otakureader.feature.settings"

    defaultConfig {
        // AI features are not currently shipped in the app (the Gemini-backed
        // implementations live in a separate companion repo — see #708).
        // SettingsMvi reads this flag to decide whether to surface AI UI.
        buildConfigField("boolean", "AI_FEATURES_AVAILABLE", "false")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.preferences)
    implementation(projects.core.discord)
    implementation(projects.data)
    // Note: feature.reader was removed as a dependency here. The shared types
    // (ImageQuality, ReaderMode, etc.) now come from :domain and ReaderSettingsRepository
    // comes from :data, which are both already included via the feature convention plugin.
    implementation(libs.paging.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.serialization.json)
    // T-1: Unit test dependencies for AiKeyValidationTest and SettingsViewModelTest
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
