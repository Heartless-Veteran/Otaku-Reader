plugins {
    alias(libs.plugins.otakureader.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.otakureader.core.navigation"
    buildFeatures { compose = true }
}

dependencies {
    api(libs.navigation.compose)
    api(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
