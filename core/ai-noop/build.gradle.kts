plugins {
    alias(libs.plugins.otakureader.android.library)
    alias(libs.plugins.otakureader.android.hilt)
}

android {
    namespace = "app.otakureader.core.ainoop"
}

dependencies {
    // Domain interfaces (AiRepository) — pure Kotlin, no Gemini SDK needed.
    implementation(projects.domain)
    implementation(projects.core.common)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
}
