package app.otakureader.core.network

import okhttp3.CertificatePinner

/**
 * Certificate pins for tracker OAuth/API endpoints.
 *
 * Pins are SHA-256 SPKI hashes of the leaf or intermediate certificates.
 * Update these whenever a tracker rotates its certificate chain.
 *
 * How to get new pins:
 *   openssl s_client -connect <host>:443 -showcerts </dev/null 2>/dev/null \
 *     | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der \
 *     | openssl dgst -sha256 -binary | base64
 *
 * Or use: `okhttp3.CertificatePinner.pin(certificate)` in a test.
 *
 * All domains are pinned to both the current leaf and at least one CA backup pin
 * so that a leaf rotation does not break the app until the next release.
 */
object TrackerCertificatePinner {

    /**
     * Builds a [CertificatePinner] covering all tracker OAuth endpoints.
     * Add this to the tracker-specific [okhttp3.OkHttpClient].
     */
    fun build(): CertificatePinner = CertificatePinner.Builder()
        // ── MyAnimeList ─────────────────────────────────────────────────────
        // OAuth: myanimelist.net/v1/oauth2/  API: api.myanimelist.net/v2/
        // Leaf: *.myanimelist.net; backup: DigiCert Global G3 TLS ECC SHA384 2020 CA1.
        .add("myanimelist.net", "sha256/1PnQBMduiWmZ1W9s5g4Oc60IzJ7btXnnVSXB5bCkNPk=")
        .add("myanimelist.net", "sha256/qBRjZmOmkSNJL0p70zek7odSIzqs/muR4Jk9xYyCP+E=")
        .add("api.myanimelist.net", "sha256/1PnQBMduiWmZ1W9s5g4Oc60IzJ7btXnnVSXB5bCkNPk=")
        .add("api.myanimelist.net", "sha256/qBRjZmOmkSNJL0p70zek7odSIzqs/muR4Jk9xYyCP+E=")
        // ── AniList ─────────────────────────────────────────────────────────
        // graphql.anilist.co (CDN); leaf: anilist.co; backup: Google Trust Services WE1.
        .add("graphql.anilist.co", "sha256/cMLTuYuw3mxbtrZkTAITlFUb4BJk07ZN+FBEwFAk0p0=")
        .add("graphql.anilist.co", "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=")
        // ── Kitsu ───────────────────────────────────────────────────────────
        // kitsu.app; backup: Google Trust Services WE1.
        .add("kitsu.app", "sha256/b5aSqS7V973sG4EAVWgVHPmEXOnYKSUG8UfJOcbDStQ=")
        .add("kitsu.app", "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=")
        // ── MangaUpdates ────────────────────────────────────────────────────
        // api.mangaupdates.com; backup: Sectigo RSA Domain Validation Secure Server CA.
        .add("api.mangaupdates.com", "sha256/VR9Xi9oQj5iUu0cZo5/Hxmz1j0azgQAvk8L6fVKWyLY=")
        .add("api.mangaupdates.com", "sha256/4a6cPehI7OG6cuDZka5NDZ7FR8a60d3auda+sKfg4Ng=")
        // ── Shikimori ───────────────────────────────────────────────────────
        // shikimori.one; backup: Let's Encrypt R12.
        .add("shikimori.one", "sha256/sUAluX+bd0A/FbvCaXQg7Siq3gMgCfIdbMHX4VkGnMw=")
        .add("shikimori.one", "sha256/kZwN96eHtZftBWrOZUsd6cA4es80n3NzSk/XtYz2EqQ=")
        .build()
}
