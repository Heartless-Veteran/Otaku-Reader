plugins {
    alias(libs.plugins.otakureader.android.library)
    alias(libs.plugins.otakureader.android.library.compose)
    alias(libs.plugins.otakureader.android.hilt)
}

android {
    namespace = "app.otakureader.core.webview"
}

dependencies {
    implementation(projects.core.navigation)
    implementation(projects.core.preferences)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.compose.material.icons.extended)
    // Cookie syncing between WebView's CookieManager and OkHttp's CookieJar
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.core)
}
