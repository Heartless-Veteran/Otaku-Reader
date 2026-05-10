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
)
