# DataStore Settings Architecture

**Status:** ✅ Implemented (Preferences DataStore)
**Module:** `core/preferences/`

---

## Overview

All app settings use **Preferences DataStore** (not SharedPreferences). Every preference class exposes:
- `Flow<T>` for reactive Compose consumption via `collectAsStateWithLifecycle()`
- `suspend fun setXxx(value)` for updates
- `DataStore<Preferences>` backing with typed keys

---

## Preference Classes

| Class | Scope | Keys |
|-------|-------|------|
| `GeneralPreferences` | Theme, locale, notifications, browse NSFW, Discord RPC, onboarding, app updates, image cache | 20+ |
| `LibraryPreferences` | Default category, sort, filter, display mode, grid columns | 10+ |
| `ReaderPreferences` | Reading mode, brightness, color filter, tap zones, fullscreen, cutout behavior | 15+ |
| `DownloadPreferences` | Location, concurrency, auto-delete, Wi-Fi only, battery threshold | 8+ |
| `BackupPreferences` | Auto-backup interval, last backup time, backup directory | 4+ |
| `LocalSourcePreferences` | CBZ import directory, folder naming | 2+ |
| `ReadingGoalPreferences` | Daily chapter target, streak tracking | 3+ |
| `EncryptedApiKeyStore` | Tracker API tokens (encrypted) | AniList, MAL, Kitsu |
| `EncryptedOpdsCredentialStore` | OPDS server credentials (encrypted) | Username/password pairs |

---

## Theme Settings (GeneralPreferences)

Theme configuration is already reactive:
```kotlin
val themeMode: Flow<Int>          // 0=system, 1=light, 2=dark
val useDynamicColor: Flow<Boolean> // Material You
val usePureBlackDarkMode: Flow<Boolean>
val useHighContrast: Flow<Boolean>
val colorScheme: Flow<Int>         // 0-10 + custom
val customAccentColor: Flow<Long>
```

Consumed in `MainActivity`:
```kotlin
val darkTheme by generalPreferences.themeMode
    .map { it == 2 || (it == 0 && isSystemInDarkTheme()) }
    .collectAsStateWithLifecycle(initialValue = isSystemInDarkTheme())

OtakuReaderTheme(
    darkTheme = darkTheme,
    colorScheme = colorScheme,
    usePureBlack = usePureBlack,
    useHighContrast = useHighContrast,
    customAccentColor = customAccentColor
) { ... }
```

---

## SharedPreferences Check

No `SharedPreferences` usage detected in current codebase:
- All settings migrated to DataStore
- Encrypted values use `EncryptedSharedPreferences` (AndroidX Security) only for API tokens — appropriate for this use case

---

## Proto DataStore Migration (Optional)

Current implementation uses **Preferences DataStore** (key-value). **Proto DataStore** (type-safe schema) would provide:
- Strong typing (no `int`/`string` casts)
- Default values in schema definition
- Better performance for complex objects

**Decision:** Keep Preferences DataStore for now. Migration cost exceeds benefit for a settings layer that is already stable and well-tested. Revisit if settings complexity grows significantly (e.g., nested objects, repeated fields).

---

## CI Check

Add to required checks (Phase 4):
- `./gradlew :core:preferences:testDebugUnitTest` — validates all preference flows

---

## Usage Example

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val generalPreferences: GeneralPreferences,
    private val readerPreferences: ReaderPreferences,
) : ViewModel() {

    val themeMode = generalPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { generalPreferences.setThemeMode(mode) }
    }
}
```

In Compose:
```kotlin
val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
DropdownMenuItem(
    text = { Text("Dark") },
    onClick = { viewModel.setThemeMode(2) },
    trailingIcon = if (themeMode == 2) { { Icon(Icons.Default.Check, null) } } else null
)
```
