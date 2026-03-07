plugins {
    alias(libs.plugins.otakureader.android.library)
}

android {
    namespace = "app.otakureader.core.navigation"
}

dependencies {
    api(libs.navigation.compose)
    api(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
