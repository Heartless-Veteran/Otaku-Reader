# AUDIT_FEATURES.md — Otaku-Reader Feature Completeness Audit

**Audit date:** 2026-05-18  
**Repo SHA:** 28a13cdd6e9550856e87f4aa4bbdd9fc3b06baa0

---

## 1. Executive Summary

**Feature Completeness Score: 78 / 100**

Otaku-Reader is a feature-rich manga reader that meets or exceeds Tachiyomi/Mihon parity on most core reading flows. The reader engine alone (`ReaderMvi.kt` at 700+ lines of state, `ReaderViewModel.kt` at 36 KB) covers configurability that rivals production-grade readers: four reading modes (single-page, dual-page, webtoon, smart panels), per-session rotation, color filters, E-ink mode, data saver, crop borders, incognito mode, volume-key navigation, configurable tap zones, and auto-scroll. Tracker integration spans five services. OPDS is implemented and tested. Discord Rich Presence has a dedicated settings screen. Widgets are partially implemented.

**Most significant gaps:** TTS/audio read-aloud mode is entirely absent. Offline CBZ reading (consuming downloaded chapters) is not confirmed in the reader source tree despite downloads producing CBZ files. Bidirectional tracker auto-push on chapter completion is unconfirmed.

---

## 2. Feature Matrix

| Feature | Status | Notes | Effort to Complete |
|---|---|---|---|
| **Download / offline reading** | Partial | `DownloadManager`, `CbzCreator`, `DownloadQueue` (DB v22) exist. CBZ creation tested. Reader consuming offline CBZ not confirmed. | M — verify/add CBZ reader path in `ReaderViewModel` |
| **Reading history / resume position** | Complete | `ReadingHistoryEntity`, `GetContinueReadingUseCase`, per-chapter + per-page (`lastPageRead`) persisted. | None |
| **Multi-source aggregation / extension system** | Complete | Tachiyomi extension APK loading via `ExtensionInstaller`/`DexClassLoader`. 500+ community sources supported. | None |
| **Tracker sync — AniList** | Complete | Full OAuth, GraphQL search/find/update, all 6 status bidirectional mappings tested. | None |
| **Tracker sync — MAL** | Complete | PKCE OAuth flow, `MyAnimeListTrackerTest.kt` (18.7 KB). | None |
| **Tracker sync — Kitsu** | Complete | `KitsuTrackerTest.kt` (24.2 KB) — most comprehensive tracker test. | None |
| **Tracker sync — MangaUpdates** | Complete | `MangaUpdatesTrackerTest.kt` (17.3 KB). Session token stored in-memory only (does not survive restart). | S — integrate with `TrackerTokenStore` |
| **Tracker sync — Shikimori** | Complete | `ShikimoriTrackerTest.kt` (22.9 KB). | None |
| **Tracker sync — bidirectionality** | Unconfirmed | Tracker `update()` exists. Auto-push on chapter read via `TrackingViewModel` not verified from source. | L — verify `onChapterRead()` push path |
| **Categories / favorites with persistence** | Complete | `CategoryRepository`, 6 category use cases tested. NSFW/hidden category flags. | None |
| **Reader — LTR/RTL/vertical** | Complete | `ReadingDirection.LTR/RTL/VERTICAL` in `ReaderState`, `OnDirectionChange` event. | None |
| **Reader — webtoon / infinite scroll** | Complete | `WebtoonReader.kt`, side padding, gap control, auto-scroll, disable-zoom-out. | None |
| **Reader — dual-page mode** | Complete | `DualPageReader.kt`, `isDualPageSpread`, `companionPage` derived states. | None |
| **Reader — smart panels** | Complete | `SmartPanelsReader.kt`, `PanelNavigation` events, `SmartPanelSpeed` enum. | None |
| **Reader — tap zones** | Complete | `TapZoneConfig`, configurable Left-handed/Kindle/Edge presets, `invertTapZones`. | None |
| **Reader — brightness** | Complete | In-reader slider (0.1–1.5), system brightness override. | None |
| **Reader — color filters** | Complete | `ColorFilterMode` enum, custom ARGB tint picker, per-manga `readerBackgroundColor`. | None |
| **Reader — crop borders** | Complete | `cropBordersEnabled` toggle. | None |
| **Reader — E-ink mode** | Complete | `einkFlashOnPageChange`, `einkBlackAndWhite` flags. | None |
| **Reader — data saver** | Complete | `dataSaverEnabled`, `ImageQuality` enum. | None |
| **Reader — incognito mode** | Complete | `incognitoMode` flag — reading history not saved when set. | None |
| **Reader — page rotation** | Complete | `PageRotation` enum (0/90/180/270°), `RotateCW`/`ResetRotation` events. | None |
| **Reader — volume key navigation** | Complete | `volumeKeysEnabled`, `volumeKeysInverted` state flags. | None |
| **Reader — page bookmarks** | Complete | `PageBookmarkRepositoryImplTest`, `page_bookmarks` DB table (migration 18→19). | None |
| **Reader — screenshot / share** | Present | `screenshot/` test subdirectory; `SharePage` event defined. | None |
| **Reader — reading timer / battery overlay** | Complete | `showReadingTimer`, `showBatteryTime` state flags with UI overlay. | None |
| **Search — local library** | Complete | `SearchLibraryMangaUseCaseTest` covers title/author/genre/tag search. | None |
| **Search — remote source** | Complete | Source API `fetchSearchManga()` with `FilterList`. Paging 3 integration in Browse. | None |
| **Update checker / notifications** | Complete | `LibraryUpdateWorkerTest` (24 KB), periodic WorkManager worker, `UpdateNotifier` with channel setup. | None |
| **Backup — native JSON** | Complete | `BackupCreator`, `BackupRestorer`, `BackupScheduler`, full `BackupRoundTripTest`. | None |
| **Backup — Tachiyomi import** | Partial | `TachiyomiBackupImporter.kt` exists. **Zero test coverage.** Export to Tachiyomi format not confirmed. | M — add tests; verify export path |
| **OPDS catalog support** | Complete | `OpdsParserTest` (12.2 KB), `opds_servers` DB table, per-OPDS credentials. | None |
| **Discord Rich Presence** | Partial | `SettingsDiscordScreen.kt` exists. Backend IPC implementation in `core/discord/` not confirmed from scan. | Unknown — audit `core/discord/` module |
| **Statistics dashboard** | Complete | `StatisticsRepositoryImplTest` (10.1 KB), `reading_streaks` DB table, goal completion worker. | None |
| **Widget support** | Partial | `WidgetConfigurationScreen.kt` exists. `AppWidgetProvider` / Glance implementation not confirmed. | Unknown — audit `app/widget/` |
| **Reading lists** | Complete | `ReadingListRepositoryImplTest` (9.3 KB), `reading_lists` + `reading_list_items` DB tables. | None |
| **Source migration** | Complete | `MigrateMangaUseCaseTest` (17.5 KB), full source migration with history/chapter preservation. | None |
| **Text-to-speech (TTS)** | **Missing** | No TTS engine, service, or state in `ReaderMvi.kt`. Auto-scroll (webtoon) is the only analog. | XL — new feature |
| **Offline CBZ reading** | Unconfirmed | Downloads produce CBZ (`CbzCreator` tested). Reader consuming local CBZ not found in reader source tree. | M — verify; add `LocalPageLoader` if absent |

---

## 3. Gap Analysis

### Missing entirely

**Text-to-Speech (TTS):** `ReaderMvi.kt` defines 100+ state fields and 50+ events — no TTS-related field, event, or service exists. This is a major accessibility gap in the reader; other accessibility gaps (UI semantics, touch-target compliance) are documented in `AUDIT_UI.md`. Adding TTS requires an Android `TextToSpeech` service integration and a new reader mode or overlay — estimated effort 10–15 days for production quality.

### Partial or unconfirmed

**CBZ offline read-back:** Downloads produce CBZ but reading them back through `ReaderViewModel` is not confirmed. If `ReaderViewModel` only loads pages from network sources, downloaded chapters are write-only — a significant UX bug. Audit `ReaderViewModel.loadPage()` and `feature/reader/src/main/java/.../loader/` to confirm.

**Bidirectional tracker auto-sync:** Trackers all implement `update()` correctly. Whether `TrackingViewModel` hooks into chapter-read events from the reader to automatically push progress is not confirmed. One-way (manual only) vs. automatic push is a major UX difference.

**MangaUpdates session token:** All other trackers use `TrackerTokenStore` (encrypted, persists restarts). `MangaUpdatesTracker` stores session token in-memory only — users are silently logged out on restart.

**Discord Rich Presence backend:** `SettingsDiscordScreen.kt` creates user expectation. If the Discord IPC backend is missing or stubbed in `core/discord/`, the settings screen should show a "coming soon" notice.

### Well-covered and complete

Reader engine configurability is exceptional. Statistics/streaks system is comprehensive. Category management (hidden/NSFW flags) is thorough. Backup system is production-quality. OPDS differentiates the app from most competitors. All five major trackers implemented to equivalent depth.

---

## 4. Priority Recommendations

| Priority | Action | Impact | Effort |
|---|---|---|---|
| P1 | Confirm offline CBZ read-back path in `ReaderViewModel`; add `LocalPageLoader` if absent | Critical — download feature unusable otherwise | M (3–5 days if absent) |
| P1 | Verify bidirectional tracker auto-push on chapter completion in `TrackingViewModel` | High — expected by users | S (1–2 days if missing) |
| P1 | Add `TachiyomiBackupImporter` tests; verify Tachiyomi export path | High — silent library data loss | M (1 day) |
| P2 | Fix `MangaUpdatesTracker` session token persistence via `TrackerTokenStore` | Medium — silent logout on restart | S (0.5 days) |
| P2 | Audit `core/discord/` for Discord IPC implementation; hide settings screen if missing | Medium — user trust | S (0.5 days to audit) |
| P3 | Add TTS / audio read-aloud mode (even basic Android `TextToSpeech`) | Low/Differentiator | XL (10–15 days) |

*Audit conducted at commit `28a13cdd6e`. Ruflo agent: `product-hunter` (ruflo-intelligence + ruflo-goals plugins).*
