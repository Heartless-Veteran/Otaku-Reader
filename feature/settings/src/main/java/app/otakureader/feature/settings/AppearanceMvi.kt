@file:Suppress("MatchingDeclarationName")

package app.otakureader.feature.settings

data class AppearanceState(
    val themeMode: Int = 0,
    val useDynamicColor: Boolean = true,
    val usePureBlackDarkMode: Boolean = false,
    val useHighContrast: Boolean = false,
    val colorScheme: Int = 0,
    val customAccentColor: Long = 0xFF1976D2L,
    val locale: String = "",
    val autoThemeColor: Boolean = false,
    val visualEffectsEnabled: Boolean = true,
    // Dark mode schedule
    val darkModeScheduleEnabled: Boolean = false,
    val darkModeStartMinuteOfDay: Int = 22 * 60,
    val darkModeEndMinuteOfDay: Int = 7 * 60,
)
