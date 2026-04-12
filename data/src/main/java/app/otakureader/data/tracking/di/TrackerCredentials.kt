package app.otakureader.data.tracking.di

import app.otakureader.data.BuildConfig

/**
 * Tracker OAuth / API credentials.
 *
 * ## Security Notice (Audit C-5 — Fixed)
 *
 * Real credentials are never committed to source control.  They are injected at
 * build time from **CI/CD encrypted environment variables** via the `buildConfigField`
 * declarations in `data/build.gradle.kts`.
 *
 * ### How to provide credentials
 *
 * **CI/CD (GitHub Actions):**
 * Add the following repository secrets in *Settings → Secrets → Actions*:
 * `KITSU_CLIENT_ID`, `KITSU_CLIENT_SECRET`,
 * `MAL_CLIENT_ID`, `MAL_CLIENT_SECRET`,
 * `SHIKIMORI_CLIENT_ID`, `SHIKIMORI_CLIENT_SECRET`.
 * The build script reads them via `System.getenv(...)`.
 *
 * **Local development:**
 * Export the variables in your shell before invoking Gradle, or add them to a
 * `local.properties` file (already git-ignored) and load them in your IDE's
 * run configuration.  If the variables are not set, the credentials default to
 * empty strings and the associated tracker will fail to authenticate — which is
 * safe for development builds that do not need those trackers.
 */
object TrackerCredentials {
    // Kitsu — register at https://kitsu.app/api/edge/
    val KITSU_CLIENT_ID: String     get() = BuildConfig.KITSU_CLIENT_ID
    val KITSU_CLIENT_SECRET: String get() = BuildConfig.KITSU_CLIENT_SECRET

    // MyAnimeList — register at https://myanimelist.net/apiconfig
    val MAL_CLIENT_ID: String     get() = BuildConfig.MAL_CLIENT_ID
    val MAL_CLIENT_SECRET: String get() = BuildConfig.MAL_CLIENT_SECRET
    const val MAL_REDIRECT_URI = "app.otakureader://mal-oauth"

    // Shikimori — register at https://shikimori.one/oauth/applications
    val SHIKIMORI_CLIENT_ID: String     get() = BuildConfig.SHIKIMORI_CLIENT_ID
    val SHIKIMORI_CLIENT_SECRET: String get() = BuildConfig.SHIKIMORI_CLIENT_SECRET
    const val SHIKIMORI_REDIRECT_URI = "app.otakureader://shikimori-oauth"
}
