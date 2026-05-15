checked all the dangling references. the migration and opds callbacks in the navhost are all stubbed with empty lambdas or inside comment blocks. the library screen still fires `onNavigateToMigration` but it hits a no-op `{}` from the navhost so it's harmless at runtime.

i also cleaned up the `// navController.navigate(Route.MigrationEntry)` leftover comment in the navhost so it's just a clean empty lambda now.

the only stuff still "active" in code are:
- `SettingsMvi.kt` state fields for migration/discord (harmless defaults)
- `SettingsViewModel.kt` still observes migration prefs and handles migration events (harmless, just updates state no one reads anymore)
- `LibraryScreen.kt` still emits the migration effect (hits no-op lambda)

none of these cause compile errors since they don't import deleted modules. they're just orphaned code that'll get cleaned up in a future pass or left as benign no-ops.

**Phase 2 deletions are done.** here's the full inventory of what was removed:

**deleted directories:**
- `core/discord`
- `feature/migration`
- `feature/opds`
- `feature/more/qr`
- `feature/settings/cloudsync`
- `feature/settings/mangadex`
- `feature/reader/.../ReaderDiscordDelegate.kt` (single file)
- `feature/settings/SettingsDiscordScreen.kt` (single file)

**commented out in gradle:**
- `core.discord` deps in app/details/settings/reader build.gradle.kts
- `feature.migration` and `feature.opds` deps in app build.gradle.kts
- `include(":core:discord")`, `include(":feature:migration")`, `include(":feature:opds")` in settings.gradle.kts

**commented out in navhost:**
- migration screen, migration entry screen, opds screen, qr share/scan screens, cloud sync settings, mangadex sync settings, settings discord route

**commented out in kotlin source:**
- `DetailsViewModel.kt`: discord rpc service import, constructor param, `startDiscordBrowsing()` method
- `ReaderViewModel.kt`: discord delegate import, constructor param, `updatePresence()`/`clearPresence()` calls
- `AppearanceSettingsDelegate.kt`: discord rpc service import, constructor param, all discord preference observers, `handleSetDiscordRpcEnabled()`
- `ReaderViewModelTest.kt`: discord service and delegate imports, mock init, constructor param
- `MoreNavigation.kt`: qr screen imports and composable functions
- `SettingsScreen.kt`: migration section UI and navigate-to-migration list item
- `Route.kt`: all deleted routes

**still wired but dormant:**
- non-anilist tracker api classes and retrofit instances in `TrackingModule.kt` (only the `@IntoSet` bindings were commented out, the api infra is still there)
- tracker oauth intent filters in manifest (only non-anilist ones were commented out)
- migration/discord state/events in `SettingsMvi.kt` (orphaned but benign)

ready for the baseline/build check whenever you are.

---

## Pre-Build Check Results

Phase 2 trim pre-build check complete. No active (un-commented) imports or references to deleted modules remain. All dangling code has been commented out with `REMOVED — trim plan Phase 2` markers.

**Fixed during review:**
- `SettingsNavigation.kt`: commented out `settingsDiscordScreen` import and call (file was deleted in Phase 2)

**Verified clean:**
- All imports to `core.discord`, `feature.migration`, `feature.opds`, `feature.more.qr`, `settings.cloudsync`, `settings.mangadex` are commented out
- All NavHost references to deleted screens are inside `/* */` comment blocks
- All Route references to deleted destinations are commented out in `Route.kt`
- `ReaderViewModel.kt`, `DetailsViewModel.kt`, `AppearanceSettingsDelegate.kt` have Discord code commented out
- Test files (`ReaderViewModelTest.kt`, `SettingsViewModelTest.kt`) have deleted references commented out

**Dormant but harmless (will not cause compile errors):**
- `SettingsMvi.kt` state fields for migration/discord (unused defaults)
- `SettingsViewModel.kt` still handles migration events (writes to orphaned state)
- `LibraryScreen.kt` still emits migration effect (hits no-op lambda)

Ready for baseline/build check.
