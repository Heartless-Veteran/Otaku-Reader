## Wiring Audit Complete - Fixes Applied

### Fixed Wiring Issues:

1. **feature.more module** ✅
   - Created `feature/more/build.gradle.kts`
   - Added to `settings.gradle.kts` include list
   - Added to `app/build.gradle.kts` dependencies

2. **LibraryPreferences.newUpdatesCount** ✅
   - Added property for bottom nav badge
   - Added DataStore key `library_new_updates_count`

3. **Navigation Wiring** ✅
   - All routes registered in OtakuReaderNavHost.kt
   - CategoryManagementRoute properly wired
   - MoreRoute properly wired
   - All callbacks connected

4. **ViewModel Wiring** ✅
   - CategoryManagementViewModel: Hilt + DAO injection
   - DetailsViewModel: Source suggestions functions
   - LibraryViewModel: Category filter state

5. **Bottom Bar Wiring** ✅
   - OtakuReaderBottomBar receives libraryPreferences
   - Observes newUpdatesCount for badge
   - Proper navigation with state restoration

### Files Modified:
- settings.gradle.kts
- app/build.gradle.kts
- core/preferences/LibraryPreferences.kt
- feature/more/build.gradle.kts (new)
