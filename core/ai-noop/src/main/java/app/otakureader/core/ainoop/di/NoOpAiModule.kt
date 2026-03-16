package app.otakureader.core.ainoop.di

import app.otakureader.core.ainoop.NoOpAiRepository
import app.otakureader.domain.repository.AiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for FOSS builds that provides the no-op [AiRepository].
 *
 * In a FOSS product flavor, include `:core:ai-noop` and install this module
 * instead of the real `AiModule` + `RepositoryModule`'s `bindAiRepository`.
 *
 * **Note**: Do **not** install this module alongside the real `AiRepositoryImpl`
 * binding — Hilt will report a duplicate binding error.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NoOpAiModule {

    @Binds
    @Singleton
    abstract fun bindAiRepository(
        impl: NoOpAiRepository
    ): AiRepository
}
