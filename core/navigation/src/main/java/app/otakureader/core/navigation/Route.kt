package app.otakureader.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Otaku Reader.
 *
 * All destinations are defined as `@Serializable` data objects/classes,
 * consumed by `androidx.navigation.compose` with type-safe navigation.
 *
 * Usage in NavHost:
 * ```kotlin
 * composable<Route.Library> { LibraryScreen(...) }
 * composable<Route.MangaDetails> { backStack ->
 *     val args = backStack.toRoute<Route.MangaDetails>()
 *     MangaDetailsScreen(mangaId = args.mangaId)
 * }
 * ```
 *
 * Usage in navigation:
 * ```kotlin
 * navController.navigate(Route.MangaDetails(mangaId = 123L))
 * ```
 */
sealed interface Route {

    // ─── Top-level tabs ───

    @Serializable
    data object Library : Route

    @Serializable
    data object Browse : Route

    @Serializable
    data object History : Route

    @Serializable
    data object Updates : Route

    @Serializable
    data object More : Route

    // ─── Library / Browse sub-flows ───

    /**
     * Manga details screen.
     * @param mangaId Local manga ID from the database.
     */
    @Serializable
    data class MangaDetails(val mangaId: Long) : Route

    /**
     * Reader screen.
     * @param mangaId Local manga ID.
     * @param chapterId Chapter to open (optional — defaults to first unread).
     */
    @Serializable
    data class Reader(
        val mangaId: Long,
        val chapterId: Long? = null,
    ) : Route

    // ─── Browse sub-flows ───

    /**
     * Source listing screen — shows manga from a single source with filters.
     * @param sourceId Installed extension source ID.
     */
    @Serializable
    data class SourceListing(val sourceId: Long) : Route

    /**
     * Extension catalog — browse/install available extensions.
     */
    @Serializable
    data object ExtensionCatalog : Route

    /**
     * Global search results across all enabled sources.
     * @param query Search string.
     */
    @Serializable
    data class Search(val query: String) : Route

    // ─── More / Settings ───

    @Serializable
    data object Settings : Route

    @Serializable
    data object SettingsTracking : Route

    @Serializable
    data object SettingsBackup : Route

    @Serializable
    data object SettingsDownloads : Route

    @Serializable
    data object SettingsReader : Route

    @Serializable
    data object SettingsLibrary : Route

    // ─── OPDS (Phase 3) ───

    /**
     * OPDS catalog browser.
     * @param serverId OPDS server ID (optional — null = browse list of servers).
     */
    @Serializable
    data class OpdsCatalog(val serverId: Long? = null) : Route

    // ─── Deep-link only ───

    /**
     * OAuth callback for tracker login.
     * @param tracker Tracker ID (e.g., "anilist", "mal", "kitsu").
     * @param code OAuth authorization code.
     */
    @Serializable
    data class TrackerOAuth(
        val tracker: String,
        val code: String,
    ) : Route
}
