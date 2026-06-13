package app.otakureader.core.common.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object ApplicationScopeModule {

    private const val TAG = "ApplicationScope"

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        // Without a CoroutineExceptionHandler, an uncaught exception in ANY coroutine
        // launched on this scope (download-queue restore, shortcut sync that opens the DB,
        // periodic collectors, etc.) propagates to the thread's default uncaught handler
        // and kills the whole process — a crash that can happen during Application.onCreate,
        // before MainActivity is able to show the saved crash report. These are
        // fire-and-forget background tasks: log the failure but keep the app alive. Code
        // paths that must surface errors to the user handle their own try/catch and state.
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Uncaught exception in application-scope coroutine; app kept alive", throwable)
        }
        return CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    }
}
