# Phase 2 Architecture — Manga Core Loop

## Overview

The manga core loop is the heart of Otaku Reader: **Browse → Add to Library → Read → Track Progress → Check Updates**.

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Browse    │───▶│   Details   │───▶│   Reader    │───▶│   Updates   │
│  (Sources)  │    │  (Chapters) │    │  (Pages)    │    │ (New Chaps) │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       │                  ▲                  │
       │                  │                  │
       ▼                  │                  ▼
┌─────────────┐           │           ┌─────────────┐
│   Library   │───────────┘           │   History   │
│  (Favorites)│                       │  (Progress) │
└─────────────┘                       └─────────────┘
```

## Data Flow

### 1. Browse → SourceMangaDetail → MangaDetails

```
Source API (MangaSource)
    │
    ▼
BrowseScreen ──onMangaClick──▶ Route.SourceMangaDetail(sourceId, mangaUrl, title)
    │
    ▼
SourceMangaDetailScreen ──resolves URL──▶ MangaRepository.getOrAddFromSource()
    │
    ▼
If manga exists in DB: navigate to Route.MangaDetails(mangaId)
If not: fetch from source, insert to DB, then navigate
```

**Key Classes:**
- `SourceMangaDetailScreen` — resolves source URL to local manga ID
- `MangaRepository.getOrAddFromSource()` — fetches + inserts if needed
- `Route.SourceMangaDetail` → `Route.MangaDetails` navigation

### 2. Library → MangaDetails

```
LibraryScreen ──onMangaClick──▶ Route.MangaDetails(mangaId)
    │
    ▼
DetailsScreen (mangaId known)
    │
    ▼
DetailsViewModel loads: manga, chapters, tracking status, related manga
```

### 3. MangaDetails → Reader

```
DetailsScreen ──onChapterClick──▶ Route.Reader(mangaId, chapterId)
    │
    ▼
ReaderScreen
    │
    ▼
ReaderViewModel: loads chapter pages, manages reading mode, progress
```

### 4. Reader → History + Updates

```
ReaderScreen ──onFinishChapter──▶
    ├─▶ ChapterRepository.updateChapterProgress() ──▶ DB
    ├─▶ HistoryRepository.add() ──▶ DB
    └─▶ TrackerSyncRepository.updateProgress() ──▶ AniList/MAL/Kitsu
```

## Database Entities

### Core Loop Tables

| Entity | Purpose | Key Relations |
|--------|---------|---------------|
| `MangaEntity` | Local manga metadata | 1:N with `ChapterEntity`, `MangaCategoryEntity` |
| `ChapterEntity` | Chapter metadata + read progress | N:1 with `MangaEntity` |
| `CategoryEntity` | Library organization labels | M:N with `MangaEntity` via `MangaCategoryEntity` |
| `MangaCategoryEntity` | Junction table | Links manga to categories |
| `ReadingHistoryEntity` | Reading session log | N:1 with `MangaEntity`, `ChapterEntity` |

### Read/Unread Logic

```kotlin
// ChapterEntity
val read: Boolean          // fully read
val bookmark: Boolean      // user bookmarked
val lastPageRead: Int      // last page viewed (0-based)
val pagesLeft: Int         // total pages - lastPageRead

// Unread count per manga
SELECT COUNT(*) FROM chapters
WHERE mangaId = :id AND read = 0
```

## Repository Layer

### MangaRepository

```
getMangaById(id: Long): Flow<Manga>
getFavoriteManga(): Flow<List<Manga>>
getLibraryManga(): Flow<List<Manga>>
getMangaBySourceAndUrl(sourceId, url): Manga?
getOrAddFromSource(sourceId, url, title): Result<Manga>
updateFavorite(id, favorite): Result<Unit>
searchLibrary(query): Flow<List<Manga>>
```

### ChapterRepository

```
getChaptersByMangaId(mangaId): Flow<List<Chapter>>
getChapterById(id): Chapter
updateChapterProgress(ids, read, lastPageRead): Result<Unit>
markChaptersRead(ids, read): Result<Unit>
getUnreadCount(mangaId): Flow<Int>
```

## ViewModel Architecture

All Phase 2 ViewModels follow MVI:

```kotlin
@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getMangaDetailsUseCase: GetMangaDetailsUseCase,
    private val getChaptersUseCase: GetChaptersUseCase,
    savedStateHandle: SavedStateHandle
) : BaseMviViewModel<DetailsState, DetailsEvent, DetailsEffect>(
    initialState = DetailsState()
) {
    private val mangaId = savedStateHandle.toRoute<Route.MangaDetails>().mangaId

    init { loadMangaDetails() }

    override fun handleEvent(event: DetailsEvent) {
        when (event) {
            DetailsEvent.Refresh -> loadMangaDetails()
            is DetailsEvent.ChapterClick -> 
                sendEffect(DetailsEffect.NavigateToReader(mangaId, event.chapterId))
            // ...
        }
    }
}
```

## Navigation Flow

```
Route.Browse
    ├── Route.SourceListing(sourceId)
    │       └── Route.SourceMangaDetail(sourceId, url, title)
    │               └── Route.MangaDetails(mangaId)  [adds to library]
    ├── Route.ExtensionCatalog
    │       └── Route.ExtensionInstall
    ├── Route.Search(query)
    │       └── Route.SourceMangaDetail(sourceId, url, title)
    └── Route.OpdsCatalog(serverId?)
            └── Route.Search(query)

Route.Library
    ├── Route.MangaDetails(mangaId)
    │       ├── Route.Reader(mangaId, chapterId)
    │       ├── Route.Tracking(mangaId, title)
    │       └── Route.Search(query)
    ├── Route.CategoryManagement
    └── Route.MigrationEntry
            └── Route.Migration(selectedIds)

Route.History
    └── Route.Reader(mangaId, chapterId)

Route.Updates
    ├── Route.MangaDetails(mangaId)
    └── Route.Downloads

Route.More
    ├── Route.Settings
    │       ├── Route.SettingsTracking
    │       ├── Route.SettingsBackup
    │       ├── Route.SettingsDownloads
    │       ├── Route.SettingsReader
    │       └── Route.SettingsLibrary
    ├── Route.Downloads
    ├── Route.Statistics
    ├── Route.Feed
    └── Route.About
```

## Domain Models

### Manga
```kotlin
data class Manga(
    val id: Long,           // local DB ID
    val sourceId: Long,     // source extension ID
    val url: String,        // source-specific URL
    val title: String,
    val thumbnailUrl: String?,
    val favorite: Boolean,
    val unreadCount: Int = 0,
)
```

### Chapter
```kotlin
data class Chapter(
    val id: Long,
    val mangaId: Long,
    val name: String,
    val url: String,
    val chapterNumber: Float?,
    val read: Boolean,
    val bookmark: Boolean,
    val lastPageRead: Int,
    val dateUpload: Long,
)
```

## Use Cases

| Use Case | Input | Output | Scope |
|----------|-------|--------|-------|
| `GetLibraryMangaUseCase` | — | `Flow<List<Manga>>` | Library |
| `GetMangaDetailsUseCase` | `mangaId: Long` | `Flow<MangaDetails>` | Details |
| `GetChaptersUseCase` | `mangaId: Long` | `Flow<List<Chapter>>` | Details |
| `GetPopularMangaUseCase` | `sourceId, page` | `Result<MangaPage>` | Browse |
| `SearchMangaUseCase` | `sourceId, query, page, filters` | `Result<MangaPage>` | Browse |
| `AddMangaToLibraryUseCase` | `List<SourceManga>, sourceId` | `Result<Int>` | Browse |
| `UpdateChapterProgressUseCase` | `chapterId, page, read` | `Result<Unit>` | Reader |
| `GetRecentUpdatesUseCase` | — | `Flow<List<UpdateItem>>` | Updates |
| `GetReadingHistoryUseCase` | — | `Flow<List<HistoryItem>>` | History |

## Testing Strategy

### Unit Tests (ViewModel)
- Mock use cases + repositories
- Verify state transitions
- Verify effect emissions
- Test error handling

### Integration Tests (Repository + DAO)
- In-memory Room database
- Test CRUD + Flow emissions
- Verify cascade deletes
- Test search/filter queries

### E2E Tests (Screen → Navigation)
- Compose test harness
- Navigate through core loop
- Verify screen states
- Test deep links

## Phase 2 Completion Criteria

- [x] Database entities + DAOs for manga core loop
- [x] Database tests (MangaDao, ChapterDao, CategoryDao, MangaCategoryDao)
- [x] Navigation migration (all features use unified Route.kt)
- [ ] Use case tests
- [ ] ViewModel tests for all core screens
- [ ] E2E test: Browse → Add to Library → Read → Check History
- [ ] Documentation (this doc)

## Related Issues

- #744 Database schema
- #737 Browse + sources
- #736 Manga details
- #738 Reader
- #735 Library
- #734 Updates
- #733 History
- #732 Downloads
