package app.otakureader.data.di

import app.otakureader.data.repository.CategoryRepositoryImpl
import app.otakureader.data.repository.ChapterRepositoryImpl
import app.otakureader.data.repository.MangaRepositoryImpl
import app.otakureader.domain.repository.CategoryRepository
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.MangaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository interfaces to their implementations.
 * Lives in the `:data` module where the implementations reside.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMangaRepository(impl: MangaRepositoryImpl): MangaRepository

    @Binds
    @Singleton
    abstract fun bindChapterRepository(impl: ChapterRepositoryImpl): ChapterRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
}
