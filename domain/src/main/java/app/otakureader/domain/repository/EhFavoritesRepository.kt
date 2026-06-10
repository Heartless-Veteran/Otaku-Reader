package app.otakureader.domain.repository

import app.otakureader.domain.model.EhFavorite

interface EhFavoritesRepository {
    /** Fetches the first page of E-Hentai favorites using the stored session cookies. */
    suspend fun fetchFavorites(): List<EhFavorite>

    /** Returns true when EH session cookies have been saved (session may still be expired). */
    fun isConfigured(): Boolean
}
