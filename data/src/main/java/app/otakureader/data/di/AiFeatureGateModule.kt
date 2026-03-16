package app.otakureader.data.di

import app.otakureader.data.ai.AiFeatureGateImpl
import app.otakureader.domain.ai.AiFeatureGate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiFeatureGateModule {

    @Binds
    @Singleton
    abstract fun bindAiFeatureGate(
        impl: AiFeatureGateImpl
    ): AiFeatureGate
}
