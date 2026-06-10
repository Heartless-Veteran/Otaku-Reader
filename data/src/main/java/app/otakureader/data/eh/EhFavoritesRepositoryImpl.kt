package app.otakureader.data.eh

import app.otakureader.core.preferences.EhSessionStore
import app.otakureader.domain.model.EhFavorite
import app.otakureader.domain.repository.EhFavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EhFavoritesRepositoryImpl @Inject constructor(
    private val ehFavoritesApi: EhFavoritesApi,
    private val ehSessionStore: EhSessionStore,
) : EhFavoritesRepository {

    override suspend fun fetchFavorites(): List<EhFavorite> {
        val session = ehSessionStore.getSession()
            ?: error("No EH session configured — call EhSessionStore.saveSession() first")
        return withContext(Dispatchers.IO) {
            ehFavoritesApi.fetchFavorites(session)
        }
    }

    override fun isConfigured(): Boolean = ehSessionStore.getSession() != null
}
