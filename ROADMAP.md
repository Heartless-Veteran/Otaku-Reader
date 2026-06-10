# ROADMAP.md — Otaku Reader

**Status:** Beta: Feature Parity Complete | **Current Phase:** Beta stabilization
**Updated:** 2026-06-10

---

## Phase Status

| Phase | Status | Notes |
|-------|--------|-------|
| Alpha | ✅ **SHIPPED** | All gates green. Build passes, tests pass, security audit clean. |
| Beta | ✅ **FEATURE PARITY COMPLETE** | All 35 parity issues (#926–#958) plus the QoL/extension-system audit batches shipped 2026-06-06 → 2026-06-10. |
| Production | 📋 **PLANNED** | After beta stabilization (remaining follow-ups: #1018, #1019, #1053, #1054, plus maintenance items below). |

---

## ✅ Alpha Complete (Shipped 2026-05-25)

All alpha readiness gates pass:

- [x] Build: `assembleDebug` green ✅
- [x] Tests: All unit tests passing ✅
- [x] Security: No unencrypted creds, AES-256-GCM, HTTPS-only extensions ✅
- [x] Architecture: Clean Architecture enforced, zero layer violations ✅
- [x] DB: 32 migrations (schema v34), no destructive fallback in production ✅
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

## ✅ Beta Phase: Feature Parity (35 Issues) — COMPLETE

All 35 feature parity issues (#926–#958) have shipped and are closed. Tables kept for the issue-to-feature mapping.

### P0 — Beta Blockers (Must Have)

| Issue | Feature | Status |
|-------|---------|--------|
| #926 | Library Search | ✅ Shipped — FTS4 library search (PR #1011, 2026-06-06) |
| #927 | Advanced Search & Filtering | ✅ Shipped |
| #928 | Biometric App Lock | ✅ Shipped |
| #929 | Tachiyomi Backup Import | ✅ Shipped |
| #930 | Auto-Backup Scheduling UI | ✅ Shipped |

### P1 — Competitive Features (Strongly Needed)

| Issue | Feature | Status |
|-------|---------|--------|
| #931 | Dynamic Categories | ✅ Shipped |
| #932 | Hidden Categories | ✅ Shipped |
| #933 | Smart Download Rules | ✅ Shipped |
| #934 | Per-Manga Reader Settings UI | ✅ Shipped |
| #935 | Page Bookmark Management Screen | ✅ Shipped |
| #936 | Chapter Notes UI | ✅ Shipped |
| #937 | Search History & Suggestions | ✅ Shipped |
| #938 | Download Queue Manager | ✅ Shipped |
| #939 | Extension Auto-Update | ✅ Shipped |
| #940 | Smart Notification Batching UI | ✅ Shipped |
| #941 | Statistics Sharing | ✅ Shipped |
| #942 | Tracker Batch Sync | ✅ Shipped |

### P2 — Nice to Have

| Issue | Feature | Status |
|-------|---------|--------|
| #943 | Recommendation Engine | ✅ Shipped |
| #944 | Customizable Feeds & Discovery | ✅ Shipped |
| #945 | Reading List Collections | ✅ Shipped |
| #946 | Completed & Dropped Series Sections | ✅ Shipped |
| #947 | Per-Manga Dynamic Theme | ✅ Shipped |
| #948 | Pure Black AMOLED Mode | ✅ Shipped |
| #949 | Home Screen Widget | ✅ Shipped |
| #950 | QR Code Library Sharing | ✅ Shipped |
| #951 | Read Time Estimation | ✅ Shipped |
| #952 | Crash Reporting Integration | ✅ Shipped |
| #953 | Extension Repository Management | ✅ Shipped |

### P3 — Post-Beta

| Issue | Feature | Status |
|-------|---------|--------|
| #954 | Cloud Backup | ✅ Shipped |
| #955 | Reading Challenges & Achievements | ✅ Shipped |
| #956 | Data Usage Dashboard | ✅ Shipped |
| #957 | WebView Integration | ✅ Shipped |
| #958 | Reader Progress Sync Across Devices | ✅ Shipped |

---

## 📋 How to Pick Up Beta Work

1. Check the open follow-up issues (listed in the shipped-batch section above)
2. Comment on the issue to claim it
3. Branch from `main`: `git checkout -b feat/issue-NNN-short-name`
4. Reference the issue in your PR: `Closes #NNN`

---

## ✅ Beta QoL + Extension Trust Batch Shipped (2026-06-07 → 2026-06-10)

The post-rollout batch from the QoL/layout and extension-system audits, plus the komikku-HV gap audit. All PRs squash-merged to `main` with green CI.

| PR | Feature | Closed issue |
|----|---------|--------------|
| #1015 | Library no-op actions wired/removed, Gradle srcDir fix | — |
| #1016 | WebView hardening + MangaUpdates credential warning | — |
| #1017 | Reader presets expanded 6 → 13 settings | — |
| #1028 | ReindexDownloads domain use case | #1026 |
| #1029 | Cert pin verification dates + rotation docs | tracks #994 |
| #1030 | QR library sharing wired to library menu | — |
| #1035 | Auto-download new chapters by category | #1031 |
| #1036 | Biometric lock time/day scheduling | #1032, #1058 (dup) |
| #1037 | CBZ password/encryption (AES-256-GCM) | #1033 |
| #1055 | Reader preset human-readable labels | — |
| #1056 | Storage analytics delete actions | — |
| #1060/#1063 | Library maintenance center | #1040 |
| #1061 | Local source hidden folders | #1059, #1034 |
| #1062 | Bulk action confirmation dialogs | — |
| #1064 | Saved library filter/sort views | #1039 |
| #1065 | Source health diagnostics | #1048 |
| #1066 | Tracking health page | #1043 |
| #1067 | Update history and diagnostics | #1041 |
| #1068 | Nav tab drag-and-drop reorder + hide/show | #1038 |
| #1069 | Source categories and pinning | #1050 |
| #1070 | Data usage drill-down + monthly budget | #1045 |
| #1071 | Widget configuration studio | #1044 |
| #1072 | Extension Detail Screen 2.0 | #1047 |
| #1073 | Backup checklist + restore preflight | #1042 |
| #1074 | Saved source searches | #1051 |
| #1075 | Extension signer hash provenance | #1049 |
| #1076 | WebView session bridge (Cloudflare) | #1052 |
| #1077 | Cross-source duplicate detection display | #997 |

**Remaining open follow-ups:** extension blocklist (#1018), repository provenance tracking (#1019), MangaUpdates OAuth (blocked upstream, #1020), user-facing privacy docs (#1021), macrobenchmarks (#1022), screenshot tests (#1023), EH favorites sync (#1024), remote library sync (#1025), reader preset round 2 (#1027), full cross-source merge workflow (#1053), extension smoke-test harness (#1054), cert pin live verification (#994).

---

## ✅ P3 QoL Batch Shipped (PR #1011, 2026-06-06)

Mihon/Komikku parity improvements and reader enhancements shipped alongside the beta rollout:

| Feature | Notes |
|---------|-------|
| FTS4 library search | Title, author, artist full-text search (closes #926) |
| Reader quick-settings overlay | Long-press center tap zone → settings sheet |
| Reader chapter-list overlay | Right-slide panel with current chapter highlighted |
| Reader presets quick-switch | FilterChip row in menu overlay |
| Edit manga info | User overrides for title, description, status, genres |
| Merge duplicate library entries | Merge screen + action from library overflow |
| Per-reader-mode volume key behavior | Inherit / Disabled / Normal / Inverted per mode |
| Chapter list text search | Live search in Details screen chapter list |
| Swipe-to-delete in History | EndToStart swipe removes entry |
| Swipe-to-mark-read in Updates | EndToStart swipe marks chapter read |
| Statistics date range selector | All / 90d / 30d / 7d FilterChip row |
| Library sort mode indicator chip | Dismissible chip shows active sort; X resets |
| Reading list export (CSV/JSON) | Export from reading list detail overflow |
| Dark mode scheduling | Scheduled on/off times in display settings |
| Backup encryption | Password-protected local backups |
| Bottom nav tab reorder | Nav Order settings screen |

---

## 🏗️ Architecture Maintenance

Status as of 2026-06-10:

- [x] F-Droid metadata (fastlane) + reproducible-build flags (`dependenciesInfo` disabled in app/build.gradle.kts)
- [x] Macrobenchmark harness — `StartupBenchmarks`/`PerformanceBenchmarks` in `baselineprofile/`, runnable via benchmark.yml manual dispatch
- [x] Baseline profile — curated `app/src/main/baseline-prof.txt` shipped (PR #1080); regenerate on-device when startup code changes significantly
- [x] `<queries>` element in manifest for Android 11+ package visibility
- [x] WorkManager `PendingIntent` mutability flags for API 31+ (all 5 usages use FLAG_IMMUTABLE)
- [x] Import-level layer violations — actually 0; enforced by Detekt ForbiddenImport rules (the "8 remaining" note was stale)
- [x] Kover coverage gates for all of `:domain`, `:data`, `:core:database`, `:feature:reader`, `:feature:tracking`, `:feature:settings` — **note:** Kover 0.8.x could not instrument AGP 9 modules, so the old 60% gates passed vacuously. Kover 0.9.8 now measures for real; gates are honest per-module ratchet floors (domain 60 · data 35 · database 25 · reader 15 · tracking 15 · settings 5). Raise floors as coverage improves; never lower them.

---

## 🗑️ Legacy Audit Artifacts

The full-system audit from 2026-05-24 has been archived. Raw audit files (`AUDIT_*.md`, `PATCH_QUEUE.md`) are preserved in `.github/audit-archive/` for reference but are no longer actively maintained.

---

*Roadmap maintained by the core team. For questions, open a Discussion.*
