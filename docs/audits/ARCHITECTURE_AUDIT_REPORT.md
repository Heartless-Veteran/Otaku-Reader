## 🔴 Critical Issues

- **File**: `feature/library/src/main/java/app/otakureader/feature/library/LibraryViewModel.kt`
- **Issue**: `LibraryViewModel` extends `androidx.lifecycle.ViewModel` directly (line 40) instead of `BaseMviViewModel`. Additionally, `LibraryState` (line 1), `LibraryEvent` (line 1), and `LibraryEffect` (line 1) in `LibraryMvi.kt` do NOT implement `UiState`, `UiEvent`, or `UiEffect` interfaces respectively. This means the Library feature is completely outside the project's custom MVI framework.
- **Suggestion**: Refactor `LibraryViewModel` to extend `BaseMviViewModel<LibraryState, LibraryEvent, LibraryEffect>` and make `LibraryState` implement `UiState`, `LibraryEvent` implement `UiEvent`, and `LibraryEffect` implement `UiEffect`.

- **File**: `feature/reader/src/main/java/app/otakureader/feature/reader/ReaderViewModel.kt`
- **Issue**: `ReaderViewModel` extends `androidx.lifecycle.ViewModel` directly (line 77) instead of `BaseMviViewModel`. `ReaderState` (line 1), `ReaderEvent`, and `ReaderEffect` in `ReaderMvi.kt` do NOT implement `UiState`, `UiEvent`, or `UiEffect` interfaces.
- **Suggestion**: Refactor `ReaderViewModel` to extend `BaseMviViewModel<ReaderState, ReaderEvent, ReaderEffect>` and implement the corresponding MVI interfaces.

- **File**: `feature/reader/src/main/java/app/otakureader/feature/reader/ReaderViewModel.kt`
- **Issue**: `ReaderViewModel` uses hardcoded string keys to extract arguments from `SavedStateHandle` — `savedStateHandle["mangaId"]` (line 81) and `savedStateHandle["chapterId"]` (line 82). This is brittle and bypasses the type-safe navigation route pattern.
- **Suggestion**: Use `savedStateHandle.toRoute<Route.Reader>()` (type-safe) or at minimum define the keys as constants tied to the `Route.Reader` data class.

- **File**: `feature/details/src/main/java/app/otakureader/feature/details/DetailsViewModel.kt`
- **Issue**: `DetailsViewModel` uses a hardcoded string key `savedStateHandle.get<Long>(MANGA_ID_ARG)` (line 48) where `MANGA_ID_ARG` is a string constant. This is a manual string-based extraction instead of using the type-safe `toRoute()` API.
- **Suggestion**: Use `savedStateHandle.toRoute<Route.MangaDetails>()` to extract the `mangaId` in a type-safe manner.

- **File**: `feature/details/build.gradle.kts`
- **Issue**: The Details feature module declares `implementation(projects.data)` (line 16), meaning it depends directly on the `data/` module. Feature modules should only depend on `domain/` (interfaces) — the app module should wire implementations.
- **Suggestion**: Remove `implementation(projects.data)` from `feature/details/build.gradle.kts`. Ensure all repository accesses in Details go through domain UseCases.

- **File**: `feature/reader/build.gradle.kts`
- **Issue**: The Reader feature module declares `implementation(projects.data)` (line 18), meaning it depends directly on the `data/` module.
- **Suggestion**: Remove `implementation(projects.data)` from `feature/reader/build.gradle.kts`. Move any direct `data/` access to UseCases in `domain/`.

- **File**: `feature/settings/build.gradle.kts`
- **Issue**: The Settings feature module declares `implementation(projects.data)` (line 13), meaning it depends directly on the `data/` module.
- **Suggestion**: Remove `implementation(projects.data)` from `feature/settings/build.gradle.kts`. Replace direct repository calls in `SettingsViewModel` with UseCases.

- **File**: `feature/tracking/build.gradle.kts`
- **Issue**: The Tracking feature module declares `implementation(projects.data)` (line 15), meaning it depends directly on the `data/` module.
- **Suggestion**: Remove `implementation(projects.data)` from `feature/tracking/build.gradle.kts`. Route all data access through domain UseCases.

- **File**: `feature/updates/build.gradle.kts`
- **Issue**: The Updates feature module declares `implementation(projects.data)` (line 12), meaning it depends directly on the `data/` module.
- **Suggestion**: Remove `implementation(projects.data)` from `feature/updates/build.gradle.kts`. Replace direct `downloadRepository` and `chapterRepository` calls in `UpdatesViewModel` with UseCases.

## 🟡 Warning Issues

- **File**: `feature/browse/src/main/java/app/otakureader/feature/browse/BrowseViewModel.kt`
- **Issue**: `BrowseViewModel` injects `FeedRepository` directly (line 37) alongside UseCases. The `FeedRepository` is a domain-layer interface, but the ViewModel mixes direct repository access with UseCase-mediated access. Also, `BrowseViewModel` extends `ViewModel` directly instead of `BaseMviViewModel`.
- **Suggestion**: Create a `GetFeedUseCase` (or similar) to wrap `FeedRepository` access, or move feed logic out of Browse. Also migrate to `BaseMviViewModel`.

- **File**: `feature/updates/src/main/java/app/otakureader/feature/updates/UpdatesViewModel.kt`
- **Issue**: `UpdatesViewModel` injects `DownloadRepository` (line 36) and `ChapterRepository` (line 37) directly, and contains business logic like `downloadSelected()` (line ~150) and `markSelectedAsRead()` (line ~180) inside the ViewModel.
- **Suggestion**: Create UseCases — e.g. `DownloadChaptersUseCase`, `MarkChaptersAsReadUseCase` — and move the business logic there. The ViewModel should only coordinate.

- **File**: `feature/history/src/main/java/app/otakureader/feature/history/HistoryViewModel.kt`
- **Issue**: `HistoryViewModel` injects `ChapterRepository` directly (line 24) instead of using a UseCase. It also contains history-clearing and removal logic in the ViewModel.
- **Suggestion**: Create `ClearHistoryUseCase` and `RemoveFromHistoryUseCase` in `domain/` and inject those instead.

- **File**: `feature/settings/src/main/java/app/otakureader/feature/settings/SettingsViewModel.kt`
- **Issue**: `SettingsViewModel` injects `ChapterRepository` directly (line 42) and contains business logic like `clearImageCache()` (line ~260) and `clearHistory()` (line ~280) inside the ViewModel.
- **Suggestion**: Extract `ClearImageCacheUseCase` and `ClearHistoryUseCase` to `domain/`. Replace direct `ChapterRepository` usage with these UseCases.

- **File**: `feature/statistics/src/main/java/app/otakureader/feature/statistics/StatisticsViewModel.kt`
- **Issue**: `StatisticsViewModel` injects `StatisticsRepository` directly (line 22) instead of using only UseCases. It also mixes `GetReadingStatsUseCase` with direct repository access.
- **Suggestion**: Create a `GetReadingGoalProgressUseCase` to wrap `statisticsRepository.getReadingGoalProgress()` and remove the direct repository dependency.

- **File**: `feature/migration/src/main/java/app/otakureader/feature/migration/MigrationViewModel.kt`
- **Issue**: `MigrationViewModel` injects `MangaRepository` and `SourceRepository` directly (lines 30-31) and contains complex migration orchestration logic (similarity threshold filtering, task state management) inside the ViewModel.
- **Suggestion**: Extract migration orchestration into a `RunMigrationUseCase` in `domain/`. The ViewModel should only manage UI state and delegate to the UseCase.

- **File**: `feature/more/src/main/java/app/otakureader/feature/more/qr/ScanLibraryViewModel.kt`
- **Issue**: `ScanLibraryViewModel` injects `MangaRepository` directly (line 25) and contains import/matching business logic inside the ViewModel.
- **Suggestion**: Create an `ImportLibraryUseCase` in `domain/` to encapsulate the source-ID-to-DB matching and favoriting logic.

- **File**: `feature/browse/src/main/java/app/otakureader/feature/browse/SourceMangaDetailViewModel.kt`
- **Issue**: `SourceMangaDetailViewModel` injects `MangaRepository` directly (line 29) and contains stub-insertion logic (lines ~45-60) inside the ViewModel.
- **Suggestion**: Create a `ResolveSourceMangaUseCase` in `domain/` that handles the "find existing or insert stub" logic.

- **File**: `feature/feed/src/main/java/app/otakureader/feature/feed/FeedViewModel.kt`
- **Issue**: `FeedViewModel` injects `FeedRepository` directly (line 21) with no UseCase wrapper. While there may not be a UseCase for feed operations, this is inconsistent with the rest of the architecture.
- **Suggestion**: Create feed-specific UseCases (e.g. `RefreshFeedUseCase`, `MarkFeedItemAsReadUseCase`) in `domain/` and inject those instead.

- **File**: `feature/settings/src/main/java/app/otakureader/feature/settings/SettingsMvi.kt`
- **Issue**: `SettingsState` is a massive god-object with 80+ fields and dozens of computed properties (lines 20-180). It breaks the principle of focused, minimal state.
- **Suggestion**: Split `SettingsState` into focused sub-state objects per screen (AppearanceState, ReaderState, etc.) and have each settings sub-screen collect only the state it needs.

- **File**: `feature/details/src/main/java/app/otakureader/feature/details/DetailsViewModel.kt`
- **Issue**: The class is annotated with `@Suppress("LargeClass")` (line 32) and spans ~1000 lines. It handles Discord RPC, thumbnail caching, chapter loading, download tracking, and more.
- **Suggestion**: Continue the delegate extraction pattern already started in `ReaderViewModel`. Extract Discord, thumbnail caching, and chapter loading into dedicated delegate classes.

- **File**: `feature/reader/src/main/java/app/otakureader/feature/reader/ReaderViewModel.kt`
- **Issue**: The class is annotated with `@Suppress("LargeClass")` (line 52) and spans ~800+ lines, even after delegation to 6 separate delegates. The remaining code still mixes state mapping, event routing, and settings persistence.
- **Suggestion**: Extract the remaining event-routing and state-mapping logic into a `ReaderStateMachine` or further delegate classes.

- **File**: `feature/reader/src/main/java/app/otakureader/feature/reader/ReaderMvi.kt`
- **Issue**: `ReaderState` does not implement `UiState`, `ReaderEvent` does not implement `UiEvent`, and `ReaderEffect` does not implement `UiEffect` (verified by inspecting imports and declarations). The Reader feature is outside the MVI framework.
- **Suggestion**: Implement the MVI interfaces for Reader's state/event/effect classes and migrate `ReaderViewModel` to `BaseMviViewModel`.

- **File**: `feature/library/src/main/java/app/otakureader/feature/library/LibraryMvi.kt`
- **Issue**: `LibraryState` (line 20), `LibraryEvent` (line 58), and `LibraryEffect` (line 86) are plain Kotlin classes/interfaces that do NOT implement `UiState`, `UiEvent`, or `UiEffect`.
- **Suggestion**: Add the interface implementations to bring Library in line with the project's MVI framework.

## 🟢 Polish Issues

- **File**: `feature/statistics/src/main/java/app/otakureader/feature/statistics/StatisticsViewModel.kt`
- **Issue**: `StatisticsViewModel` does not follow the MVI pattern at all — it exposes a plain `StateFlow<StatisticsState>` with no `onEvent()` or `Effect` channel.
- **Suggestion**: Standardize on MVI by adding `StatisticsEvent` and `StatisticsEffect` sealed interfaces and an `onEvent()` handler, or document the exception if intentional.

- **File**: `feature/migration/src/main/java/app/otakureader/feature/migration/MigrationViewModel.kt`
- **Issue**: `MigrationViewModel` does not follow the MVI pattern — it exposes `_state`/`state` and `_effect`/`effect` manually but does not use `BaseMviViewModel`.
- **Suggestion**: Migrate to `BaseMviViewModel` and define `MigrationState`, `MigrationEvent`, `MigrationEffect` implementing the MVI interfaces.

- **File**: `feature/more/src/main/java/app/otakureader/feature/more/qr/ScanLibraryViewModel.kt`
- **Issue**: `ScanLibraryViewModel` does not follow the MVI pattern — it exposes a custom `ImportState` sealed class instead of standard MVI state/event/effect.
- **Suggestion**: Standardize to `ScanLibraryState`, `ScanLibraryEvent`, `ScanLibraryEffect` with MVI interfaces.

- **File**: `feature/browse/src/main/java/app/otakureader/feature/browse/SourceMangaDetailViewModel.kt`
- **Issue**: `SourceMangaDetailViewModel` does not follow the MVI pattern — it only exposes an `effect` channel with no state.
- **Suggestion**: Add a minimal `SourceMangaDetailState` and `SourceMangaDetailEvent`, or document the intentional exception.

- **File**: `feature/feed/src/main/java/app/otakureader/feature/feed/FeedViewModel.kt`
- **Issue**: `FeedViewModel` manually constructs a `combine()` flow for state (line 34-48) instead of using `BaseMviViewModel`'s built-in state reduction.
- **Suggestion**: Migrate to `BaseMviViewModel` and use `reduceState { }` pattern.

- **File**: `feature/history/src/main/java/app/otakureader/feature/history/HistoryViewModel.kt`
- **Issue**: `HistoryViewModel` extends `ViewModel` directly (line 22) despite `HistoryState`, `HistoryEvent`, and `HistoryEffect` all correctly implementing MVI interfaces.
- **Suggestion**: Migrate to `BaseMviViewModel<HistoryState, HistoryEvent, HistoryEffect>` to eliminate the manual `_state` / `_effect` wiring.

- **File**: `feature/updates/src/main/java/app/otakureader/feature/updates/UpdatesViewModel.kt`
- **Issue**: `UpdatesViewModel` extends `ViewModel` directly (line 27) despite `UpdatesState`, `UpdatesEvent`, and `UpdatesEffect` all correctly implementing MVI interfaces.
- **Suggestion**: Migrate to `BaseMviViewModel<UpdatesState, UpdatesEvent, UpdatesEffect>`.

- **File**: `feature/settings/src/main/java/app/otakureader/feature/settings/SettingsViewModel.kt`
- **Issue**: `SettingsViewModel` extends `ViewModel` directly (line 34) despite `SettingsState`, `SettingsEvent`, and `SettingsEffect` all correctly implementing MVI interfaces.
- **Suggestion**: Migrate to `BaseMviViewModel<SettingsState, SettingsEvent, SettingsEffect>`.

## 🎯 Top 3 Recommendations

1. **Standardize ALL ViewModels on `BaseMviViewModel`** — Currently, the MVI framework exists (`core/common/mvi/BaseMviViewModel.kt`) but is inconsistently adopted. `Library`, `Reader`, `Browse`, `Details`, `Updates`, `History`, `Feed`, `Settings`, `Migration`, `Statistics`, `ScanLibrary`, and `SourceMangaDetail` all extend `ViewModel` directly. Only some have their State/Event/Effect classes implementing `UiState`/`UiEvent`/`UiEffect`. Choose one approach: either migrate everything to `BaseMviViewModel` (recommended) or remove the unused framework to reduce confusion.

2. **Eliminate direct `data/` module dependencies from feature modules** — `feature/details`, `feature/reader`, `feature/settings`, `feature/tracking`, and `feature/updates` all depend on `:data` directly. This breaks Clean Architecture's dependency rule. Create the missing UseCases in `domain/` (e.g. `ClearImageCacheUseCase`, `DownloadChaptersUseCase`, `ResolveSourceMangaUseCase`, `RunMigrationUseCase`) and route all feature-module data access through them.

3. **Use type-safe `SavedStateHandle.toRoute()` everywhere** — `ReaderViewModel` (hardcoded `"mangaId"` / `"chapterId"`) and `DetailsViewModel` (string constant `MANGA_ID_ARG`) manually extract navigation arguments. Use `savedStateHandle.toRoute<Route.Reader>()` and `savedStateHandle.toRoute<Route.MangaDetails>()` respectively for compile-time safety. This aligns with the already-correct usage in `SourceMangaDetailViewModel`.
