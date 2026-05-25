# ROADMAP.md — Otaku Reader

**Status:** Alpha Ready ✅ | **Current Phase:** Beta Feature Parity (35 issues tracked)
**Updated:** 2026-05-26

---

## Phase Status

| Phase | Status | Notes |
|-------|--------|-------|
| Alpha | ✅ **READY** | All gates green. Build passes, tests pass, security audit clean. |
| Beta | 🚧 **IN PROGRESS** | 35 feature parity issues created (#926–#958). See breakdown below. |
| Production | 📋 **PLANNED** | After beta feature parity + stabilization. |

---

## ✅ Alpha Complete (Shipped 2026-05-25)

All alpha readiness gates pass:

- [x] Build: `assembleDebug` green ✅
- [x] Tests: All unit tests passing ✅
- [x] Security: No unencrypted creds, AES-256-GCM, HTTPS-only extensions ✅
- [x] Architecture: Clean Architecture enforced, zero layer violations ✅
- [x] DB: 26 clean migrations, no destructive fallback ✅
- [x] Extension system: Tachiyomi API intact, classloader isolation ✅
- [x] Notification system: UpdateNotifier, DownloadNotifier, ReadingReminderWorker ✅
- [x] Tracker auto-sync: ReaderViewModel → TrackerSyncRepository wired ✅
- [x] CI/CD: Detekt, unit tests, signed APK on every `v*` tag ✅

**Alpha PRs merged:** #920–#925
- Backup navigation wiring
- Unverified extension install dialog + trust banner
- Detekt cleanup
- SelectionManager + race fixes
- LibraryScreen decomposition
- Alpha readiness fixes (Detekt, DI, tests, resources)

---

## 🚧 Beta Phase: Feature Parity (35 Issues)

The beta phase focuses on achieving competitive parity with Mihon/Komikku. All 35 features are tracked as GitHub issues.

### P0 — Beta Blockers (Must Have)

| Issue | Feature | Status |
|-------|---------|--------|
| #926 | Library Search | 🔴 Not started |
| #927 | Advanced Search & Filtering | 🔴 Not started |
| #928 | Biometric App Lock | 🔴 Not started |
| #929 | Tachiyomi Backup Import | 🟡 Data layer exists, needs UI |
| #930 | Auto-Backup Scheduling UI | 🟡 Worker exists, needs settings screen |

### P1 — Competitive Features (Strongly Needed)

| Issue | Feature | Status |
|-------|---------|--------|
| #931 | Dynamic Categories | 🔴 Not started |
| #932 | Hidden Categories | 🔴 Not started |
| #933 | Smart Download Rules | 🟡 Branch exists (`feat/smart-download-rules`) |
| #934 | Per-Manga Reader Settings UI | 🟡 Data model exists, no UI |
| #935 | Page Bookmark Management Screen | 🟡 DB + Reader support, no list screen |
| #936 | Chapter Notes UI | 🟡 DB support, no editor UI |
| #937 | Search History & Suggestions | 🟡 Branch exists (`feat/search-history-suggestions`) |
| #938 | Download Queue Manager | 🔴 Not started |
| #939 | Extension Auto-Update | 🔴 Not started |
| #940 | Smart Notification Batching UI | 🟡 Worker exists, needs settings |
| #941 | Statistics Sharing | 🟡 Branch exists (`feat/statistics-sharing`) |
| #942 | Tracker Batch Sync | 🔴 Not started |

### P2 — Nice to Have

| Issue | Feature | Status |
|-------|---------|--------|
| #943 | Recommendation Engine | 🔴 Not started |
| #944 | Customizable Feeds & Discovery | 🔴 Not started |
| #945 | Reading List Collections | 🟡 Branch exists (`feat/reading-list-collections`) |
| #946 | Completed & Dropped Series Sections | 🟡 Data model exists, no UI |
| #947 | Per-Manga Dynamic Theme | 🟡 Branch exists (`feat/per-manga-dynamic-theme`) |
| #948 | Pure Black AMOLED Mode | 🔴 Not started |
| #949 | Home Screen Widget | 🟡 Branch exists (`feat/home-widget`) |
| #950 | QR Code Library Sharing | 🟡 Partial — `ShareLibraryScreen` exists, no QR gen |
| #951 | Read Time Estimation | 🟡 Branch exists (`feat/read-time-estimation`) |
| #952 | Crash Reporting Integration | 🔴 Not started |
| #953 | Extension Repository Management | 🔴 Not started |

### P3 — Post-Beta

| Issue | Feature | Status |
|-------|---------|--------|
| #954 | Cloud Backup | 🔴 Not started |
| #955 | Reading Challenges & Achievements | 🔴 Not started |
| #956 | Data Usage Dashboard | 🔴 Not started |
| #957 | WebView Integration | 🔴 Not started |
| #958 | Reader Progress Sync Across Devices | 🔴 Not started |

---

## 📋 How to Pick Up Beta Work

1. Check the issue list above — anything marked 🔴 is up for grabs
2. Comment on the issue to claim it
3. Branch from `main`: `git checkout -b feat/issue-NNN-short-name`
4. Reference the issue in your PR: `Closes #NNN`

**Priority order:** Start with P0, then P1. P2 and P3 can run in parallel.

---

## 🏗️ Architecture Maintenance (Ongoing)

These are not feature issues but ongoing maintenance tasks:

- [ ] F-Droid metadata + reproducible builds
- [ ] Macrobenchmark module to prevent regressions
- [ ] Baseline profile re-generation after P0 fixes
- [ ] `<queries>` element in manifest for Android 11+ package visibility
- [ ] WorkManager `PendingIntent` mutability flags for API 31+
- [ ] 8 remaining import-level layer violations (see legacy `AUDIT_ARCHITECTURE.md` in archive)
- [ ] Kover coverage gates for `:feature:reader`, `:feature:tracking`, `:feature:settings`

---

## 🗑️ Legacy Audit Artifacts

The full-system audit from 2026-05-24 has been archived. Raw audit files (`AUDIT_*.md`, `PATCH_QUEUE.md`) are preserved in `.github/audit-archive/` for reference but are no longer actively maintained.

---

*Roadmap maintained by the core team. For questions, open a Discussion.*
