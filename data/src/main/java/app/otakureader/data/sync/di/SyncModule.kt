package app.otakureader.data.sync.di

import app.otakureader.core.database.dao.CategoryDao
import app.otakureader.core.database.dao.ChapterDao
import app.otakureader.core.database.dao.MangaDao
import app.otakureader.core.preferences.SyncPreferences
import app.otakureader.data.sync.SyncManagerImpl
import app.otakureader.domain.sync.SyncManager
import app.otakureader.domain.sync.SyncProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.jvm.JvmSuppressWildcards

/**
 * DI module for sync functionality.
 *
 * Note: This currently provides an empty set of sync providers.
 * A self-hosted sync provider will be added in a future update (see issue #462).
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    /**
     * Provides the set of available sync providers.
     *
     * Currently returns an empty set. Self-hosted sync provider
     * will be added here when implemented.
     */
    @Provides
    @Singleton
    fun provideSyncProviders(): Set<SyncProvider> = emptySet()

    @Provides
    @Singleton
    fun provideSyncManager(
        mangaDao: MangaDao,
        chapterDao: ChapterDao,
        categoryDao: CategoryDao,
        syncPreferences: SyncPreferences,
        providers: Set<@JvmSuppressWildcards SyncProvider>
    ): SyncManager = SyncManagerImpl(
        mangaDao = mangaDao,
        chapterDao = chapterDao,
        categoryDao = categoryDao,
        syncPreferences = syncPreferences,
        providers = providers
    )
}
