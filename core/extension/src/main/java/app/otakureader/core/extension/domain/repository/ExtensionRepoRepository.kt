package app.otakureader.core.extension.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing extension repository URLs.
 * Supports adding/removing third-party extension repositories
 * (e.g., Keiyoushi, Suwayomi, Yuzono).
 *
 * All configured repositories are simultaneously active — extensions from every
 * added repository are fetched and merged. There is no concept of a single
 * "active" repository.
 *
 * The system automatically detects and supports both:
 * - index.min.json (common format for Keiyoushi, Komikku, Suwayomi repositories)
 * - index.json (standard format)
 */
interface ExtensionRepoRepository {

    /**
     * Get all configured repository URLs. All returned URLs are treated as active.
     */
    fun getRepositories(): Flow<List<String>>

    /**
     * Add a new repository URL.
     * @param url The repository URL (should point to a directory containing index.min.json or index.json)
     */
    suspend fun addRepository(url: String)

    /**
     * Remove a repository URL.
     * @param url The repository URL to remove
     */
    suspend fun removeRepository(url: String)

    /**
     * Ensure the default repository is added if no repositories are configured.
     */
    suspend fun ensureDefaultRepository()

    /**
     * Clear all repositories.
     */
    suspend fun clearRepositories()
}
