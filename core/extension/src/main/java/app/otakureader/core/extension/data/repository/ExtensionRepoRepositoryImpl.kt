package app.otakureader.core.extension.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.otakureader.core.extension.domain.repository.ExtensionRepoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of ExtensionRepoRepository using DataStore.
 * All stored repository URLs are simultaneously active — there is no single "active" repo concept.
 */
class ExtensionRepoRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : ExtensionRepoRepository {

    companion object {
        private val REPOSITORIES_KEY = stringSetPreferencesKey("extension_repositories")
    }

    override fun getRepositories(): Flow<List<String>> {
        return dataStore.data.map { preferences ->
            preferences[REPOSITORIES_KEY]?.toList() ?: emptyList()
        }
    }

    override suspend fun addRepository(url: String) {
        dataStore.edit { preferences ->
            val currentRepos = preferences[REPOSITORIES_KEY]?.toMutableSet() ?: mutableSetOf()
            currentRepos.add(url)
            preferences[REPOSITORIES_KEY] = currentRepos
        }
    }

    override suspend fun removeRepository(url: String) {
        dataStore.edit { preferences ->
            val currentRepos = preferences[REPOSITORIES_KEY]?.toMutableSet() ?: mutableSetOf()
            currentRepos.remove(url)
            preferences[REPOSITORIES_KEY] = currentRepos
        }
    }

    @Deprecated("No longer used as there is no default repository.")
    override suspend fun ensureDefaultRepository() {
        // No default — users add their own extension repositories via the Extensions screen.
    }

    override suspend fun clearRepositories() {
        dataStore.edit { preferences ->
            preferences.remove(REPOSITORIES_KEY)
        }
    }
}
