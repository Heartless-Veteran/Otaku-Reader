package app.otakureader.data.di

import app.otakureader.data.repository.AiRepositoryImpl
import app.otakureader.data.repository.RecommendationRepositoryImpl
import app.otakureader.domain.repository.AiRepository
import app.otakureader.domain.repository.RecommendationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds the real AI implementations in `full` builds.
 *
 * This module lives in the `full` flavor source set so it is excluded from FOSS
 * builds. FOSS builds get no-op bindings from `:core:ai-noop`.
 *
 * **Duplicate binding prevention**: The flavor separation ensures this module and
 * the FOSS module are never both compiled into the same build.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAiRepository(
        impl: AiRepositoryImpl
    ): AiRepository

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(
        impl: RecommendationRepositoryImpl
    ): RecommendationRepository
}
