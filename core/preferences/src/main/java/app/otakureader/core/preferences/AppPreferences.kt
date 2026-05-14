package app.otakureader.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Wrapper around DataStore<Preferences> for type-safe preference access.
 * Exposes reactive [Flow] properties and suspend setter functions.
 * The underlying DataStore is provided via Hilt from [app.otakureader.core.preferences.di.PreferencesModule].
 */
class AppPreferences(private val dataStore: DataStore<Preferences>) {

    // --- Migration settings ---

    /** Minimum similarity score (0.0–1.0) to auto-migrate without confirmation. Default: 0.7. */
    val migrationSimilarityThreshold: Flow<Float> = dataStore.data.map {
        it[Keys.MIGRATION_SIMILARITY_THRESHOLD] ?: 0.7f
    }
    suspend fun setMigrationSimilarityThreshold(value: Float) =
        dataStore.edit { it[Keys.MIGRATION_SIMILARITY_THRESHOLD] = value }

    /** When true, always show the confirmation dialog even for high-confidence matches. */
    val migrationAlwaysConfirm: Flow<Boolean> = dataStore.data.map {
        it[Keys.MIGRATION_ALWAYS_CONFIRM] ?: false
    }
    suspend fun setMigrationAlwaysConfirm(value: Boolean) =
        dataStore.edit { it[Keys.MIGRATION_ALWAYS_CONFIRM] = value }

    /** Minimum chapter count a candidate must have to be considered. Default: 0 (no filter). */
    val migrationMinChapterCount: Flow<Int> = dataStore.data.map {
        it[Keys.MIGRATION_MIN_CHAPTER_COUNT] ?: 0
    }
    suspend fun setMigrationMinChapterCount(value: Int) =
        dataStore.edit { it[Keys.MIGRATION_MIN_CHAPTER_COUNT] = value }

    private object Keys {
        val MIGRATION_SIMILARITY_THRESHOLD = floatPreferencesKey("migration_similarity_threshold")
        val MIGRATION_ALWAYS_CONFIRM = booleanPreferencesKey("migration_always_confirm")
        val MIGRATION_MIN_CHAPTER_COUNT = intPreferencesKey("migration_min_chapter_count")
    }
}
