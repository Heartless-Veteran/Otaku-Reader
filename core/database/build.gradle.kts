plugins {
    alias(libs.plugins.otakureader.android.library)
    alias(libs.plugins.otakureader.android.room)
    alias(libs.plugins.otakureader.android.hilt)
}

android {
    namespace = "app.otakureader.core.database"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.domain)
    api(libs.room.paging)
    implementation(libs.paging.runtime)
}
