package app.otakureader.core.extension.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.otakureader.core.extension.data.remote.ExtensionRemoteDataSourceImpl
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
        private const val DEFAULT_REPO_URL = "https://raw.githubusercontent.com/keiyoushi/extensions/repo"
    }

    override fun getRepositories(): Flow<List<String>> {
        return dataStore.data.map { preferences ->
            val repos = preferences[REPOSITORIES_KEY]?.toList() ?: emptyList()
            if (repos.isEmpty()) listOf(DEFAULT_REPO_URL) else repos
        }
    }

    override suspend fun addRepository(url: String) {
        dataStore.edit { preferences ->
            val currentRepos = preferences[REPOSITORIES_KEY]?.toMutableSet() ?: mutableSetOf()
            currentRepos.add(ExtensionRemoteDataSourceImpl.normalizeRepoUrl(url))
            preferences[REPOSITORIES_KEY] = currentRepos
        }
    }

    override suspend fun removeRepository(url: String) {
        dataStore.edit { preferences ->
            val normalizedUrl = ExtensionRemoteDataSourceImpl.normalizeRepoUrl(url)
            val currentRepos = preferences[REPOSITORIES_KEY]?.toMutableSet() ?: mutableSetOf()
            currentRepos.remove(normalizedUrl)
            currentRepos.remove(url)
            preferences[REPOSITORIES_KEY] = currentRepos
        }
    }

    override suspend fun ensureDefaultRepository() {
        dataStore.edit { preferences ->
            val currentRepos = preferences[REPOSITORIES_KEY]
            if (currentRepos.isNullOrEmpty()) {
                preferences[REPOSITORIES_KEY] = setOf(DEFAULT_REPO_URL)
            }
        }
    }

    override suspend fun clearRepositories() {
        dataStore.edit { preferences ->
            preferences.remove(REPOSITORIES_KEY)
        }
    }
}
