# Otaku Reader — Privacy Policy

_Last updated: 2026-06-10_

Otaku Reader is a local-first manga reader. The short version: **the app collects no data about you, has no accounts, no analytics, and no servers of its own.** Everything below explains what stays on your device, what optionally leaves it (and only when you turn it on), and how security-sensitive features work.

## What we collect

**Nothing.** Otaku Reader has no analytics, no telemetry, no advertising identifiers, and no first-party servers. The developers cannot see your library, reading history, or anything else.

## What is stored on your device

All app data lives in local app storage and never leaves the device unless you export it:

- Your library (manga, categories, reading lists, saved views)
- Reading history, statistics, streaks, goals, and chapter notes
- Downloaded chapters (loose images or CBZ archives)
- Preferences and reader presets
- Installed extension list and their settings
- Backup files you create
- Page bookmarks

Uninstalling the app deletes all of it (except backups you exported elsewhere).

## Optional features that use the internet

Each of these is off until you use or enable it:

### Manga sources (extensions)
Browsing or reading from an online source sends requests to that source's website, exactly like visiting it in a browser. The website sees your IP address and standard request headers. Otaku Reader adds no identifiers of its own.

### Tracker sync (AniList, MyAnimeList, Kitsu, MangaUpdates, Shikimori)
Off by default and configured per tracker. When you link a manga and sync, the app sends that tracker your reading progress (chapter number, status, score) for linked entries only. Reading **history timestamps are not synced** — only progress state. Tokens are stored in encrypted preferences on-device. You can unlink a tracker at any time, and incognito mode suppresses sync entirely.

**MangaUpdates note:** MangaUpdates' public API does not yet support OAuth, so logging in requires sending your username and password directly to `api.mangaupdates.com` over HTTPS. Only the resulting session token is stored — never your password. The other four trackers use OAuth and never see your password through this app.

### OPDS (Komga / Kavita / Calibre-Web)
Only contacts the server URL you configure. Credentials are stored encrypted on-device.

### Update check
Checks GitHub Releases for new app versions. GitHub sees a standard HTTPS request.

### Discord Rich Presence
Off by default. When enabled, shares your current reading status with your local Discord client.

### Crash reporting
**Off by default and requires two explicit steps:** you must supply your own Sentry DSN *and* opt in. With no DSN configured, nothing is ever uploaded. Crash logs are stored locally in encrypted preferences.

## Extension safety

Extensions are third-party APKs from repositories you add (e.g. Keiyoushi). Otaku Reader limits what they can do:

- **Classloader isolation** — extensions load in a child-first isolated classloader; they are not granted Android permissions of their own and execute only through the source API surface.
- **HTTPS-only installs** — extension APKs only download over HTTPS.
- **Signer-hash continuity** — the app records each extension's signing certificate hash at first install. If a repository later serves the same extension signed by a different key (a possible supply-chain replacement), a warning is shown.
- **Trust controls** — unverified extensions require explicit confirmation before install, and you can untrust an extension at any time.

Extensions do make network requests to their manga sources — that is their purpose. Treat adding a repository like installing software from it.

## Backups

Backups are human-readable JSON in a ZIP, saved where you choose. They contain your library, categories, history, tracking links, and settings — **but never tracker passwords** (only tokens, and only if you choose to include settings). Optional backup encryption uses AES-256-GCM with a password you set; without the password the backup cannot be read. Unencrypted backups can be read by anyone with the file, so store them accordingly.

## Biometric lock

The optional app lock uses Android's BiometricPrompt. Biometric data never reaches the app — Android only reports success or failure.

## Children's privacy

The app has an NSFW content gate that is off by default. Since no data is collected from any user, no data is collected from children.

## Changes to this policy

Changes are tracked in this file's git history and noted in the [CHANGELOG](CHANGELOG.md).

## Contact

Questions: open an issue at <https://github.com/Heartless-Veteran/Otaku-Reader/issues>.
