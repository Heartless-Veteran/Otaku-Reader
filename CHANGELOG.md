# Changelog

All notable changes to Otaku Reader will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added — Extension Trust & Health
- Extension Detail Screen 2.0 — full-screen extension page with version, package name, trust badge, signer hash, repo link, capability chips, expandable source list, and trust/untrust action (#1072, closes #1047)
- Extension signer hash provenance — first-seen signer hash recorded per extension; a changed signing certificate after install shows a red warning icon and label in the extensions list (#1075, closes #1049)
- Source health diagnostics — per-source failure tracking with warning badges and a diagnostic sheet in Browse (#1065)
- WebView session bridge — sources can request a WebView Cloudflare challenge and share the resulting cookies with OkHttp (#1076, closes #1052)

### Added — Browse & Discovery
- Source categories and pinning — pin sources to a top "Pinned" section and group the rest under custom category labels via long-press (#1069, closes #1050)
- Saved source searches — name and save the current query as a chip row above the source list; tap to re-run, × to delete (#1074, closes #1051)
- Local source hidden folders — optional scanning of dot-prefixed directories in the local source path (#1061, closes #1059)

### Added — Library
- Saved library views — save named filter+sort combinations and re-apply them from a chip row (#1064, closes #1039)
- Library maintenance center — dedicated screen for cover refresh, metadata refresh, reindex downloads, and orphaned-file cleanup (#1060, #1063)
- Cross-source duplicate detection — duplicate groups show a "Cross-source" chip and resolved source names in the merge screen (#1077, closes #997)
- Bulk action confirmation dialogs — destructive library selection actions (remove, mark completed/dropped) now confirm first (#1062)
- Update history and diagnostics — last-run stats with checked/skipped/failed counts and per-manga skip reasons (#1067)
- Auto-download new chapters by category — per-category include/exclude lists control which categories trigger auto-download (#1035)

### Added — Downloads & Storage
- CBZ password protection — AES-256-GCM encryption for downloaded CBZ archives with Keystore-backed passphrase storage and transparent decrypt-on-read (#1037, closes #1033)
- Storage analytics delete actions — per-manga delete buttons with confirmation dialog in the storage dashboard (#1056)
- Reindex downloads — domain use case reconciling on-disk chapter files with database state, wired to the library menu (#1028)

### Added — Settings & System
- Widget configuration studio — per-widget count limit, tap action, category filter, thumbnail toggle, and live preview (#1071, closes #1044)
- Navigation tab drag-and-drop reorder with per-tab hide/show switches (#1068, closes #1038)
- Backup contents checklist and restore preflight — live item counts before backup; filename + irreversibility warning before restore (#1073, closes #1042)
- Tracking health page — per-tracker token expiry, last sync, and failed-update queue (#1066)
- Data usage per-source drill-down and monthly budget with progress bar (#1070, closes #1045)
- Biometric lock scheduling — time-of-day and day-of-week enforcement windows (#1036)
- Reader preset human-readable labels — "Single Page · Fit Width" instead of raw mode/scale integers (#1055)
- Reader presets expanded from 6 to 13 captured settings — tap zones, volume keys, page number, skip flags (#1017)
- QR library sharing wired into the library overflow menu (#1030)

### Fixed
- Library no-op menu actions wired or removed; deprecated Gradle `srcDir` API replaced (#1015)
- Certificate pin rotation — per-tracker verification dates and openssl rotation instructions documented (#1029, tracks #994)

### Security
- WebView hardening — file/content access disabled, Safe Browsing enabled (#1016)
- MangaUpdates credential login now shows a security note explaining password-based auth (#1016)
- CBZ archives can be encrypted at rest; passphrase held in EncryptedSharedPreferences backed by the Android Keystore (#1037)
- Extension signer continuity check guards against silent supply-chain package replacement (#1075)

## [0.1.0-beta] - 2026-06-06

### Added
- FTS4-powered library search — title, author, artist full-text search (closes #926)
- Reader quick-settings overlay — long-press center tap zone opens a settings sheet
- Reader chapter-list overlay — right-slide panel with current chapter highlighted
- Reader presets quick-switch — FilterChip row in menu overlay for one-tap profile switch
- Edit manga info — user metadata overrides (title, description, status, genres)
- Merge duplicate library entries — merge screen accessible from library overflow menu
- Per-reader-mode volume key behavior — Inherit / Disabled / Normal / Inverted per reading mode
- Chapter list text search in Details screen — live filtering by chapter name
- Swipe-to-delete in History — EndToStart swipe removes a history entry
- Swipe-to-mark-read in Updates — EndToStart swipe marks a chapter as read
- Statistics date range selector — All / 90d / 30d / 7d FilterChip row
- Library sort mode indicator chip — dismissible chip shows active sort; tap × resets to Alphabetical
- Reading list export as CSV and JSON — from reading list detail overflow menu
- Dark mode scheduling — configurable on/off times in display settings
- Backup encryption with password prompt — AES-256-GCM encrypted local backups
- Bottom nav tab reorder screen — drag-to-reorder in Nav Order settings
- Onboarding flow for first-time users (5-page intro)
- `onboarding_completed` preference tracking
- Beta feature parity backlog: 35 GitHub issues created (#926–#958)

### Changed
- `versionCode` bumped to 2; `versionName` set to `0.1.0-beta`
- Build is now a single flat artifact: removed `full` / `foss` product flavors and the `distribution` flavor dimension. Use `./gradlew assembleDebug` / `assembleRelease` directly.
- Statistics screen period filtering applied in-memory; library count and streaks always show all-time data

### Fixed
- H-6: DataStore write failures now show snackbar (no longer silent)
- H-12: Reader chapter load failures show error message (no longer blank)
- Onboarding screen now triggers for new users
- Alpha readiness: all gates green (build ✅, tests ✅, security ✅, architecture ✅, extension compat ✅, notifications ✅, tracker sync ✅)
- Override default-param Kotlin compile error in `StatisticsRepositoryImpl`
- Removed stale `ImageDecoderDecoder` reference from `OtakuReaderApplication` (Coil 3 compat)
- `MangaRepositoryImplTest` constructor mismatch after `mangaCategoryDao` was added
- `ReaderViewModelTest` missing `readerPreferences` mock and `getChaptersByMangaId` stub

### Security
- HTTPS-only extension downloads (C-3 compliance)
- Child-first classloader isolation for extensions
- Not exported broadcast receiver for extension lifecycle

## [0.1.0-alpha] - 2026-05-25

### Added
- Complete manga reader with 4 reading modes (Single, Dual, Webtoon, Smart Panels)
- Extension system with 2000+ Tachiyomi-compatible sources
- Library management with categories and favorites
- Download system with offline reading and CBZ export
- Tracker sync (MAL, AniList, Kitsu, MangaUpdates, Shikimori)
- Discord Rich Presence integration
- OPDS catalog support
- Feed system for content discovery
- Material 3 UI with dynamic theming
- Edge-to-edge display support
- Home screen widgets (Continue Reading, Recent Updates)
- Dynamic shortcuts (Library, Updates, Continue Reading)
- Deep link support (MangaDex URLs, share intents)

### Technical
- Clean Architecture with 26 modules
- MVI pattern throughout
- Jetpack Compose UI
- Room database with 13 entities
- Hilt dependency injection
- WorkManager background tasks
- DataStore preferences
- Coil 3 image loading
- Full Komikku feature parity

## Release Template

When creating a new release, include:

```markdown
## [VERSION] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes to existing functionality

### Deprecated
- Soon-to-be removed features

### Removed
- Now removed features

### Fixed
- Bug fixes

### Security
- Security improvements
```
