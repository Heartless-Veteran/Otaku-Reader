package app.otakureader.core.discord.di

import app.otakureader.core.discord.DiscordRpcService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Discord RPC dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DiscordModule {

    @Provides
    @Singleton
    fun provideDiscordRpcService(
        service: DiscordRpcService
    ): DiscordRpcService = service
}
