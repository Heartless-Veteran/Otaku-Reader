# Otaku Reader

<div align="center">
  <img src="./.github/logo.jpg" alt="Otaku Reader" width="200"/>

  <p><em>A modern, manga-only Android reader — no AI, no cloud, no ads, no tracking.</em></p>

  [![Build](https://github.com/Heartless-Veteran/Otaku-Reader/actions/workflows/build.yml/badge.svg)](https://github.com/Heartless-Veteran/Otaku-Reader/actions/workflows/build.yml)
  [![CI](https://github.com/Heartless-Veteran/Otaku-Reader/actions/workflows/ci.yml/badge.svg)](https://github.com/Heartless-Veteran/Otaku-Reader/actions/workflows/ci.yml)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
  [![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
  [![License](https://img.shields.io/badge/License-Apache%202.0-0877d2?style=flat)](LICENSE)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

</div>

---

> **Privacy First:** All data stays on your device. No accounts, no tracking, no cloud, no AI. Ever.
> <br>**Local-first, never lock-in.**

---

## 📥 Download

| Build | Description | Status |
|-------|-------------|--------|
| **Otaku Reader** | Single flat build — open-source core, no proprietary SDKs, no AI. | 🔨 Build from source or watch for releases |

**Minimum Requirements:** Android 8.0 (API 26) · target APK < 10 MB

---

## ✨ What Makes It Different

Every Tachiyomi fork is a maintenance burden with half-finished features. Otaku Reader is intentionally **manga-only**, **Compose-native**, and **built to actually work** on day one.

### Core Philosophy
- **One app, one job:** Read manga. Nothing else.
- **Zero accounts required:** No Google, no Firebase, no sign-up. Ever.
- **No AI in core:** No ML models, no data mining. Just manga.
- **Switch in 60 seconds:** Restore from Mihon/Komikku/Tachikomi backup → reading immediately.

### Built From Scratch
This is not a fork. Otaku Reader was written from the ground up — the core app, UI, and architecture are original work. The extension system enables compatibility with existing source repositories (Keiyoushi, Komikku). Everything else is homegrown.

---

## 🚀 Features

### Library & Organization
- 📚 **Smart library** — Grid/list views, categories, sorting, filtering, unread badges
- ✅ **Completed series** — Mark manga as finished, dimmed covers + checkmark badge, dedicated filter tab
- ❌ **Dropped series** — Mark abandoned manga, red cancel badge, "Dropped" filter — never re-click a manga you hated
- 🔍 **Fuzzy search** — Find manga instantly instead of scrolling
- 📂 **Reading list collections** — Create custom lists beyond categories: "Summer Binge", "Re-read Later", "Hidden Gems"
- 📝 **Chapter notes** — Add personal notes to any chapter
- 📱 **Widget navigation** — Home screen widgets for continue reading, now reading, and recent updates with deep-link navigation
- 📂 **QR library sharing** — Scan a friend's phone, get their manga list instantly (local, no server)

### Reading Experience
- 📖 **All reader modes** — Paged, webtoon, continuous scroll, dual-page, smart panels
- 🎨 **Per-manga dynamic theme** — Material You palette extracted from cover art, every manga gets its own color scheme
- 🔖 **Page bookmarks** — Bookmark any page within a chapter, revisit favorite artwork/scenes instantly
- ⏱️ **Read time estimation** — "~5 min read" on chapter lists, powered by your actual reading speed
- 📱 **Adaptive layouts** — Optimized for phones, foldables, tablets, and DeX

### Downloads & Offline
- ⬇️ **Smart download rules** — Auto-download next chapters when you hit 80% reading progress
- 📦 **CBZ export** — Archive downloaded chapters for backup or transfer
- 📁 **Local source import** — CBZ/CBR/folder browsing without extensions
- 🔔 **Smart notification batching** — Grouped chapter update alerts with quiet hours, cooldown, and digest mode — never spam

### Discovery & Sources
- 🔌 **Extension system** — Tachiyomi/Komikku-compatible sources (Keiyoushi, Komikku repos)
- 🌐 **OPDS client** — Browse Komga, Kavita, Calibre-Web libraries
- 🔍 **Global search** — Search across all installed sources simultaneously
- 🕐 **Search history** — Recent queries as quick-tap chips — no more re-typing
- 📰 **Feed** — New chapter updates from your sources in one place
- 🔗 **Deep links** — Open manga and chapters directly from external links (extension URLs, tracker links)

### Tracking & Stats
- 📊 **Reading streaks** — Consecutive-day counter with 30-day heatmap
- 🏆 **Reading goals** — Daily/weekly chapter targets with progress
- 📈 **Statistics dashboard** — Time read, chapters completed, genre breakdown
- 📤 **Statistics sharing** — Generate a beautiful shareable card of your reading stats
- 🔗 **Tracker sync** — AniList, MyAnimeList (MAL), Kitsu, MangaUpdates, Shikimori (opt-in, local-only API keys)
- 🎮 **Discord Rich Presence** — Share what you're reading with Discord status integration

### Backup & Migration
- 💾 **Local backup/restore** — Human-readable JSON in ZIP, everything stays on-device
- 🔄 **Auto-backup scheduling** — Periodic automatic backups with configurable interval and retention
- 📲 **Tachiyomi/Mihon/Komikku import** — Bring your entire library from any fork
- 🔄 **Source-to-source migration** — Move manga between sources without losing progress

---

## 📖 Reader Comparison

| Feature | Otaku Reader | Typical Fork |
|---------|-------------|--------------|
| Smooth webtoon scroll | ✅ Pre-rendered, no jank | ❌ Stutters on long chapters |
| Page-stitching | ✅ Smart chunk merge | ❌ Manual zoom required |
| Per-manga zoom memory | ✅ Remembered per title | ❌ Global only |
| Volume-key paging | ✅ Debounced, reliable | ⚠️ Spotty |
| Battery-aware brightness | ✅ Auto curve | ❌ Manual slider only |
| Predictive back (Android 14+) | ✅ Fullscreen gesture | ❌ System default |
| Per-manga color theme | ✅ From cover art | ❌ Not available |
| Page bookmarks | ✅ Any page, any chapter | ❌ Not available |
| Read time estimation | ✅ Adaptive to your speed | ❌ Not available |
| Completed/Dropped status | ✅ Visual badges + filters | ❌ Manual deletion only |
| Chapter notes | ✅ Per-chapter annotations | ❌ Not available |
| Reading list collections | ✅ Custom lists beyond categories | ❌ Not available |
| Statistics sharing | ✅ Social-ready cards | ❌ Not available |
| Home screen widgets | ✅ Continue reading + recent updates | ❌ Not available |
| Deep link support | ✅ Open manga from external URLs | ❌ Not available |
| Discord Rich Presence | ✅ Live reading status | ❌ Not available |

**Reading Modes:** Paged · Webtoon · Continuous Scroll · Dual-Page · Smart Panels

**Navigation:** Gallery thumbnails · 3×3 tap zones · Pinch zoom · Hardware key support · Auto-scroll

**Accessibility:** TalkBack-readable · Dyslexia-friendly font · High-contrast theme · Color-blind safe palettes

---

## 🔐 Privacy & Security

- ✅ **No data collection** — Everything stays local
- ✅ **No accounts required** — Use without any registration
- ✅ **No analytics or tracking** — Reading habits are yours alone
- ✅ **Encrypted preferences** — Secure local storage for tracker API keys
- ✅ **HTTPS-only extensions** — Enforced secure source downloads
- ✅ **Sandboxed extensions** — Isolated classloading for untrusted sources

**Data stored locally:** library, downloaded chapters, preferences, extension sources, backup files, page bookmarks, reading history.

**Optional internet use:** manga source browsing · tracker sync (opt-in) · OPDS server (opt-in) · update check

---

## 📸 Screenshots

<div align="center">

| Library | Browse | Reader | Settings |
|---------|--------|--------|----------|
| <img src="docs/screenshots/library.png" width="180" alt="Library screen"/> | <img src="docs/screenshots/browse.png" width="180" alt="Browse screen"/> | <img src="docs/screenshots/reader.png" width="180" alt="Reader screen"/> | <img src="docs/screenshots/settings.png" width="180" alt="Settings screen"/> |

<em>Screenshots taken on Pixel 7 (Android 14). See <a href="docs/screenshots/">docs/screenshots/</a> for full-resolution images.</em>

</div>

---

## 🗺️ Roadmap

### ✅ Phase 0: Clean Slate
- [x] Remove AI module from core repo (moved to separate repo, on hold)
- [x] Remove cloud sync and self-hosted server modules → [Otaku-Reader-Sync](https://github.com/Heartless-Veteran/Otaku-Reader-Sync)
- [x] Flat single-product build (no `full`/`foss` flavors)

### ✅ Phase 1: Core App Wiring
- [x] Hilt DI audit — no cycles, all bindings present
- [x] Single Compose navigation graph with type-safe routes
- [x] Material3 theme (light / dark / dynamic)
- [x] DataStore settings backbone
- [x] Base MVI pattern for every screen

### ✅ Phase 2: Manga Core Loop
- [x] Room database: Manga, Chapter, History, Category, Feed, OPDS
- [x] Source API (Komikku/Keiyoushi compatible)
- [x] Extension system: install, verify, configure index
- [x] Library + Browse screens (Compose-native)
- [x] Manga details, download, bookmark
- [x] Reader with all modes + accessibility
- [x] Downloader (CBZ, notifications, WorkManager)
- [x] Local source import (CBZ/CBR/folders)
- [x] History, updates, search, settings
- [x] Smart download rules (auto-queue at reading threshold)

### ✅ Phase 3: Trackers, Backup, Polish
- [x] Tracker integration (AniList/MAL/Kitsu/MangaUpdates/Shikimori)
- [x] Backup/restore (human-readable JSON in ZIP)
- [x] Source-to-source migration
- [x] Update check via GitHub Releases
- [x] OPDS client + server
- [x] Reading streaks + stats dashboard
- [x] Completed/Dropped series sections
- [x] Per-manga dynamic theme
- [x] QR library sharing
- [x] Tachiyomi/Mihon/Komikku import
- [x] Page bookmarks
- [x] Read time estimation
- [x] Chapter notes
- [x] Reading list collections
- [x] Statistics sharing
- [x] Auto-backup scheduling
- [x] Smart notification batching
- [x] Search history
- [x] Home screen widgets
- [x] Deep link handling
- [x] Discord Rich Presence

### 🚧 Phase 4: Quality Gates & Release
- [x] Critical Compose UI tests (library, reader) — Robolectric-based
- [x] Green CI: ktlint, unit tests, signed APK on every `v*` tag
- [x] Keystore signing via GitHub Secrets — release builds are installable
- [x] Branch protection enforced
- [x] Gradle convention plugins modernized + SDK levels centralized in version catalog
- [ ] F-Droid metadata + reproducible builds
- [ ] Macrobenchmark module to prevent regressions

### Future Differentiators
- [ ] Curated default extension index (opt-out)
- [ ] Per-source rate limiting with visible queue
- [ ] Optional ActivityPub federation for read-status
- [ ] Double-page spread auto-detection

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 2.1.10 |
| UI | Jetpack Compose 100% — no XML layouts |
| Architecture | Clean Architecture + MVI |
| Dependency Injection | Hilt 2.55 |
| Database | Room 2.7.0 + KSP |
| Preferences | DataStore |
| Networking | OkHttp 4.12.0 + Coil 3.1.0 |
| Background Work | WorkManager 2.10.0 |
| Build | Gradle 8.7 + convention plugins + version catalogs + signed release APKs |

---

## 🏗️ Architecture

Otaku Reader follows **Clean Architecture** with three horizontal layers and feature-based vertical modules:

```
app/                    — Application module (DI wiring, manifest, widgets, deep links)
├── core/
│   ├── common/         — Shared utilities, Result type, ReadTimeEstimator
│   ├── ui/             — Compose design system, theme, dynamic color extraction
│   ├── navigation/     — Type-safe navigation graph
│   ├── preferences/    — DataStore wrappers (General, Reader, Download, Goals, OAuth)
│   ├── database/       — Room entities, DAOs, migrations (v21)
│   ├── network/        — OkHttp interceptors, certificate pinning, network DI
│   ├── extension/      — ExtensionLoader, TrustedSignatureStore
│   ├── tachiyomi-compat/ — Bridges Tachiyomi APKs to source-api interfaces
│   └── discord/        — Discord Rich Presence service
├── domain/             — Pure Kotlin: use cases, repository interfaces, models. Zero Android deps.
├── data/               — Repository implementations, workers, network
│   ├── backup/         — Backup/restore logic (human-readable JSON in ZIP)
│   ├── download/       — Download manager, CBZ export
│   ├── tracking/       — Tracker sync (AniList, MAL, Kitsu, MangaUpdates, Shikimori)
│   └── opds/           — OPDS client/server
├── source-api/         — Extension SDK contract (Source, HttpSource, SManga, etc.). No Android deps.
└── feature/
    ├── library/        — Library grid, categories, filters, completed/dropped
    ├── browse/         — Sources, extensions, global search, search history
    ├── details/        — Manga info, chapters, read time estimates
    ├── reader/         — All reading modes + page bookmarks + smart download trigger
    ├── history/        — Reading history
    ├── updates/        — New chapter notifications + smart batching
    ├── tracking/       — Tracker settings + sync
    ├── settings/       — App preferences
    ├── migration/      — Source-to-source + Tachiyomi/Mihon/Komikku import
    ├── onboarding/     — First-launch setup wizard
    ├── about/          — Credits, licenses, updates
    ├── statistics/     — Reading stats + streaks + heatmap + shareable cards
    ├── feed/           — New chapter updates feed from sources
    ├── opds/           — OPDS client/server mode
    └── more/           — QR library sharing, additional tools
```

### Module Dependency Rules

- **`domain`** — Pure Kotlin, zero dependencies. Contains use cases, repository interfaces, and models.
- **`data`** — Depends on `domain` + `core/*`. Contains repository implementations, Room DAOs, WorkManager workers.
- **`feature/*`** — Depends on `domain` + `core/*`. Each feature is self-contained. **No feature module may depend on another feature module.**

---

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Quick Start

```bash
# Clone
git clone https://github.com/Heartless-Veteran/Otaku-Reader.git
cd Otaku-Reader

# Build debug APK
./gradlew assembleDebug

# Run checks
./gradlew detekt
./gradlew testDebugUnitTest
```

See [docs/contributing/ci.md](docs/contributing/ci.md) for the full CI command reference.

---

## 🔗 See Also

- **[Otaku-Reader-Sync](https://github.com/Heartless-Veteran/Otaku-Reader-Sync)** — Optional cloud sync server for cross-device library sync.

---

<div align="center">

```
Copyright 2025 Manny Carter

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

</div>

---

## 🙏 Acknowledgments

- [Komikku](https://github.com/komikku-app/komikku) — Architecture & feature baseline
- [Keiyoushi](https://github.com/keiyoushi) — Extension repository
- Tachiyomi community — Extension ecosystem foundation
