package app.otakureader.data.di

import app.otakureader.data.repository.AiRepositoryImpl
import app.otakureader.domain.repository.AiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds the real [AiRepository] implementation in `full` builds.
 *
 * This module lives in the `full` flavor source set so it is excluded from FOSS
 * builds. FOSS builds get their [AiRepository] binding from
 * [app.otakureader.core.ainoop.di.NoOpAiModule] via `:core:ai-noop`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAiRepository(
        impl: AiRepositoryImpl
    ): AiRepository
}
