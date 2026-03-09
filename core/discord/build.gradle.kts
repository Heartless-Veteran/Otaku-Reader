plugins {
    alias(libs.plugins.otakureader.android.library)
    alias(libs.plugins.otakureader.android.hilt)
}

android {
    namespace = "app.otakureader.core.discord"
}

dependencies {
    implementation(projects.core.preferences)

    // Kizzy - Discord Rich Presence library
    implementation(libs.kizzy.rpc)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
