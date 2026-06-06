# Changelog

All notable changes to Otaku Reader will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
