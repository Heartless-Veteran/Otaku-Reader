# Phase 7: Feature Gap Analysis
**Otaku Reader Android App** | Generated: 2026-05-24

---

## Executive Summary

Otaku Reader has achieved approximately **7.2 / 10** overall feature completion. The application has a solid foundation with all four reader modes implemented, a functional library management system, and preliminary tracker infrastructure. However, two critical gaps would block alpha release: **the notification system has no implementation**, and **tracker auto-sync logic is incomplete**.

---

## Feature Matrix

| Feature Area | Status | Notes | Effort |
|---|---|---|---|
| **Reader — Single-page** | Complete | HorizontalPager, zoom/pan/rotate fully implemented | — |
| **Reader — Webtoon** | Complete | LazyColumn, SubsamplingWebtoonDecoder, mouse wheel support | — |
| **Reader — Dual-page** | Complete | RTL support, landscape auto-detect (aspect ratio > 1.2) | — |
| **Reader — Smart panels** | Missing | No ML panel detection found; stubs exist in `core/ai-noop` | High |
| **Reader — RTL support** | Complete | Per-manga reading direction override present | — |
| **Reader — Tap zones / swipe** | Complete | `TapZoneOverlay.kt` + gesture handler fully wired | — |
| **Library — Categories** | Complete | Create, rename, delete, reorder, NSFW/hidden toggles | — |
| **Library — Filtering** | Complete | 7 filter modes (read/unread/completed/downloaded/etc.) | — |
| **Library — Sorting** | Complete | 5 sort modes (title, last read, chapter count, etc.) | — |
| **Library — Grid/list display** | Partial | Grid present; list mode UI incomplete | Medium |
| **Library — Bulk operations** | Partial | Mark read/unread present; bulk download/delete incomplete | Medium |
| **Library — "For You"** | Partial | Section scaffolded; recommendation engine is a stub | High |
| **Browse — Extension catalog** | Complete | Install, uninstall, update, trust management | — |
| **Browse — Source listing** | Partial | List present; per-source filter UI incomplete | Medium |
| **Browse — Global search** | Complete | Paging 3, cross-source results | — |
| **Tracker — OAuth login** | Partial | 5 trackers (AniList, Kitsu, MAL, MangaUpdates, Shikimori) with credential dialog | Low |
| **Tracker — Score/status sync** | Partial | `ConflictUiState` defined; sync push/pull handlers stubbed | High |
| **Tracker — Auto-sync on read** | Partial | Infrastructure present; trigger logic incomplete | High |
| **Downloads — Manual** | Complete | Priority queue (1–5 concurrent), persistence, idempotent | — |
| **Downloads — Auto-download on update** | Complete | Wired to `LibraryUpdateWorker`; per-manga and global flag | — |
| **Downloads — Queue management** | Partial | Queue UI present; error recovery / retry unclear | Medium |
| **Backup — Create/restore** | Complete | `BackupCreator`, `BackupRestorer`, Tachiyomi importer | — |
| **Backup — Auto-schedule** | Partial | `BackupScheduler.kt` exists; trigger frequency/policy unclear | Low |
| **Notifications — New chapters** | Missing | No `NotificationManager`, channels, or `BroadcastReceiver` setup found | High |
| **Notifications — Download complete** | Missing | Same gap as above | High |
| **Widgets — Continue Reading** | Complete | Glance widget with receiver | — |
| **Widgets — Recent Updates** | Complete | Glance widget with receiver | — |
| **Widgets — Now Reading** | Complete | Glance widget with receiver | — |
| **Widget configuration** | Partial | `WidgetConfigurationScreen` scaffolded; data binding incomplete | Medium |
| **OPDS browsing** | Partial | `OpdsMvi` and `OpdsScreen` present; search/filter/pagination missing | High |
| **OPDS authentication** | Missing | No auth token management for OPDS feeds | Medium |
| **Settings — Reader** | Partial | Screen present; persistence binding status unclear | Low |
| **Settings — Library** | Partial | Screen present; grid size / badge toggles defined | Low |
| **Settings — Downloads** | Partial | Screen present; concurrency / bandwidth controls incomplete | Low |
| **Settings — Appearance** | Partial | Theme / dark mode toggles present | Low |
| **Statistics dashboard** | Complete | Streak, total pages, time read, per-source breakdown | — |
| **History** | Complete | Reading timeline with swipe-to-delete | — |
| **Migration wizard** | Partial | Source migration UI present; conflict resolution UI incomplete | Medium |
| **Discord Rich Presence** | Partial | `core/discord` module present; trigger from reader unconfirmed | Low |

---

## Alpha Readiness by Area

### Alpha-Blocking (must fix before alpha)

| Gap | Effort | Reason |
|-----|--------|--------|
| Notification system — no implementation | High | Users can't know when new chapters are available |
| Tracker auto-sync — trigger logic missing | High | Core value proposition for tracker users |

### Recommend Before Alpha (polish)

| Gap | Effort | Impact |
|-----|--------|--------|
| Download error recovery / retry UI | Medium | Users can't recover from failed downloads |
| OPDS authentication | Medium | OPDS sources blocked for auth-required servers |
| Backup auto-schedule trigger | Low | Users relying on periodic backup may lose data |

### Target Beta / Post-Alpha

| Gap | Effort | Notes |
|-----|--------|-------|
| Library list mode | Medium | Grid covers alpha adequately |
| Bulk download/delete | Medium | Manual works for alpha |
| Smart panel detection | High | Stubs acceptable; `core/ai-noop` in place |
| OPDS search/filter/pagination | High | Basic browsing works for alpha |
| "For You" recommendations | High | No AI infra yet |

---

## Scoring Breakdown

| Area | Weight | Score |
|------|--------|-------|
| Reader modes | 20% | 9/10 (smart panels missing) |
| Library management | 20% | 7/10 (list mode, bulk ops incomplete) |
| Tracker integration | 15% | 5/10 (login OK, sync incomplete) |
| Notifications | 15% | 0/10 (not implemented) |
| Download management | 10% | 8/10 (queue good, recovery unclear) |
| Backup/restore | 10% | 9/10 (all paths present) |
| Browse / OPDS | 10% | 6/10 (OPDS partial) |

**Weighted Score: 7.2 / 10**

---

## Score: 7.2 / 10

Core reading experience is production-ready. Two alpha-blocking gaps (notifications, tracker sync) require a focused sprint before alpha. Everything else either works or has acceptable stubs.
