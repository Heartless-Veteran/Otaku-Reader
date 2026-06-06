package app.otakureader.core.network

import okhttp3.CertificatePinner

/**
 * Certificate pins for tracker OAuth/API endpoints.
 *
 * Pins are SHA-256 SPKI hashes of the leaf certificate plus a shared intermediate
 * CA backup pin (DigiCert Global Root G3) so that a leaf rotation does not break
 * the app until the next release.
 *
 * ## Updating pins
 *
 * Run the following for each host (requires network access):
 *   openssl s_client -connect <host>:443 -servername <host> 2>/dev/null \
 *     | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der \
 *     | openssl dgst -sha256 -binary | base64
 *
 * Or in a test: `okhttp3.CertificatePinner.pin(certificate)`
 *
 * Replace the leaf pin for the affected host, bump its "Last verified" date, and
 * keep the DigiCert Global Root G3 backup pin unchanged — it rarely rotates.
 *
 * Issue tracking: #994
 */
object TrackerCertificatePinner {

    /**
     * Builds a [CertificatePinner] covering all tracker OAuth endpoints.
     * Add this to the tracker-specific [okhttp3.OkHttpClient].
     */
    fun build(): CertificatePinner = CertificatePinner.Builder()
        // ── MyAnimeList ──────────────────────────────────────────────────────
        // OAuth: myanimelist.net/v1/oauth2/  API: api.myanimelist.net/v2/
        // Backup: DigiCert Global Root G3 (shared across all trackers below).
        // Last verified: 2026-05-04
        .add("myanimelist.net", "sha256/efrF+PUxbl1xw8PF8f6KvRrel4/kWrRWFWuu6PQj2bo=")
        .add("myanimelist.net", "sha256/IgG8q1Egd9jBnrvbTB6BsLEvZ1aYqrym+IPQIxy5qiE=")
        .add("api.myanimelist.net", "sha256/JhaUbPsimbNLHKKcoc09+5qss1K+Hb8XpX2uthsDY4A=")
        .add("api.myanimelist.net", "sha256/IgG8q1Egd9jBnrvbTB6BsLEvZ1aYqrym+IPQIxy5qiE=")
        // ── AniList ──────────────────────────────────────────────────────────
        // graphql.anilist.co  —  Last verified: 2026-05-04
        .add("graphql.anilist.co", "sha256/qvGMxVkebAxISqRUcDy3CljP+wieT8aKikYTCVu0PH4=")
        .add("graphql.anilist.co", "sha256/IgG8q1Egd9jBnrvbTB6BsLEvZ1aYqrym+IPQIxy5qiE=")
        // ── Kitsu ────────────────────────────────────────────────────────────
        // kitsu.app  —  Last verified: 2026-05-04
        .add("kitsu.app", "sha256/Hrs6IIkHfcrDYD7JZOmpHh/QXgq4ZfY7IKy+UnzF4bA=")
        .add("kitsu.app", "sha256/IgG8q1Egd9jBnrvbTB6BsLEvZ1aYqrym+IPQIxy5qiE=")
        // ── MangaUpdates ─────────────────────────────────────────────────────
        // api.mangaupdates.com  —  Last verified: 2026-05-04
        .add("api.mangaupdates.com", "sha256/02qttGMepJs9pxcSmAyDxfnjZv0EoAw8IkZSv+rzbu8=")
        .add("api.mangaupdates.com", "sha256/IgG8q1Egd9jBnrvbTB6BsLEvZ1aYqrym+IPQIxy5qiE=")
        // ── Shikimori ────────────────────────────────────────────────────────
        // shikimori.one  —  Last verified: 2026-05-04
        .add("shikimori.one", "sha256/1biKOxoipnu596Dxf12XU0Vu2EXlLUQnUj4p+ycBKSY=")
        .add("shikimori.one", "sha256/IgG8q1Egd9jBnrvbTB6BsLEvZ1aYqrym+IPQIxy5qiE=")
        .build()
}
