package app.otakureader.data.di

import app.otakureader.core.common.di.ApplicationScope
import app.otakureader.core.common.network.NetworkMonitor
import app.otakureader.core.network.BytesRecorder
import app.otakureader.domain.repository.DataUsageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Provides the [BytesRecorder] binding that bridges [core/network]'s OkHttp event listener
 * with [DataUsageRepository] in [data], avoiding a cross-layer dependency.
 *
 * [Provider] is used to defer the first access to [DataUsageRepository] until after Hilt
 * has fully initialized the object graph, breaking any potential initialization cycle with Room.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkRecorderModule {

    @Provides
    @Singleton
    fun provideBytesRecorder(
        dataUsageRepositoryProvider: Provider<DataUsageRepository>,
        networkMonitor: NetworkMonitor,
        @ApplicationScope appScope: CoroutineScope,
    ): BytesRecorder = BytesRecorder { category, bytes ->
        val network = networkMonitor.currentNetwork().name
        appScope.launch {
            dataUsageRepositoryProvider.get().recordBytes(
                category = category.name,
                network = network,
                bytes = bytes
            )
        }
    }
}
