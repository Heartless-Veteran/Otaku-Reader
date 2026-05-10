package app.otakureader.feature.settings

data class LibrarySettingsState(
    val libraryGridSize: Int = 3,
    val showBadges: Boolean = true,
    val updateOnlyOnWifi: Boolean = false,
    val updateOnlyPinnedCategories: Boolean = false,
    val autoRefreshOnStart: Boolean = false,
    val showUpdateProgress: Boolean = true,
)
