plugins {
    alias(libs.plugins.otakureader.android.library)
    alias(libs.plugins.otakureader.android.hilt)
    alias(libs.plugins.otakureader.android.room)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

android {
    namespace = "app.otakureader.data"

    buildFeatures {
        // Needed to expose tracker OAuth credentials via BuildConfig (C-5).
        buildConfig = true
    }

    defaultConfig {
        // ── Tracker OAuth Credentials ──────────────────────────────────────────
        // All credentials are injected at build time from environment variables so
        // that secret values are never stored in source control.
        //
        // CI/CD (GitHub Actions): add each variable as a Repository Secret under
        //   Settings → Secrets and variables → Actions.
        // Local development: export the variables in your shell before running Gradle,
        //   e.g.  export KITSU_CLIENT_ID="…"  in ~/.zshrc / ~/.bashrc.
        // Empty string ("") is a valid no-op default: tracker features remain available
        //   in the UI but OAuth flows will fail gracefully at runtime.
        //
        // ── Kitsu  (kitsu.io/api/oauth/token — client_credentials flow) ────────
        //   Register at: https://kitsu.io/settings/developer-apps
        //   Required scopes: none (public API uses client credentials)
        buildConfigField("String", "KITSU_CLIENT_ID",     "\"${System.getenv("KITSU_CLIENT_ID")     ?: ""}\"")
        buildConfigField("String", "KITSU_CLIENT_SECRET", "\"${System.getenv("KITSU_CLIENT_SECRET") ?: ""}\"")

        // ── MyAnimeList  (myanimelist.net/v1/oauth2 — PKCE authorization-code) ─
        //   Register at: https://myanimelist.net/apiconfig
        //   PKCE flow does not require a client secret.
        buildConfigField("String", "MAL_CLIENT_ID", "\"${System.getenv("MAL_CLIENT_ID") ?: ""}\"")

        // ── Shikimori  (shikimori.one/oauth — authorization-code flow) ─────────
        //   Register at: https://shikimori.one/oauth/applications
        //   Redirect URI must match what is registered in the Shikimori dashboard.
        buildConfigField("String", "SHIKIMORI_CLIENT_ID",     "\"${System.getenv("SHIKIMORI_CLIENT_ID")     ?: ""}\"")
        buildConfigField("String", "SHIKIMORI_CLIENT_SECRET", "\"${System.getenv("SHIKIMORI_CLIENT_SECRET") ?: ""}\"")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.core.preferences)
    implementation(projects.core.tachiyomiCompat)
    implementation(projects.core.extension)
    implementation(projects.domain)
    implementation(projects.sourceApi)

    implementation(libs.paging.runtime)
    implementation(libs.workmanager.ktx)
    implementation(libs.hilt.work)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.coil.compose)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.work.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
}

kover {
    reports {
        verify {
            rule {
                minBound(60)
            }
        }
    }
}
