plugins {
    id("otakureader.android.library")
    id("otakureader.android.hilt")
}

android {
    namespace = "app.otakureader.core.tachiyomi"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(project(":source-api"))
    implementation(project(":domain"))
    implementation(project(":core:common"))

    // Tachiyomi dependencies (would be included via AAR or local Maven)
    // For now, using compileOnly since these come from the extension APK
    compileOnly("com.github.tachiyomiorg:extensions-lib:1.4")

    // RxJava for Tachiyomi compatibility
    implementation("io.reactivex:rxjava:1.3.8")
    implementation("io.reactivex:rxandroid:1.2.1")

    // OkHttp (shared with Tachiyomi)
    implementation(libs.okhttp.core)

    // XML parsing
    implementation("org.xmlpull:xmlpull:1.1.3.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso)
}
