plugins {
    alias(libs.plugins.otakureader.android.library)
}

android {
    namespace = "app.otakureader.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.coroutines.core)
}
