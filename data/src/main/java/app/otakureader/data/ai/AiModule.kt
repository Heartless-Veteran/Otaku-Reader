package app.otakureader.data.ai

import app.otakureader.domain.repository.PromptLoader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for AI-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    /**
     * Binds [AssetsPromptLoader] to [PromptLoader] interface.
     */
    @Binds
    @Singleton
    abstract fun bindPromptLoader(
        impl: AssetsPromptLoader
    ): PromptLoader
}
