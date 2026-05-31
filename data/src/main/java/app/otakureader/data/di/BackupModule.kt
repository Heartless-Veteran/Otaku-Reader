package app.otakureader.data.di

import app.otakureader.core.preferences.CloudBackupUploader
import app.otakureader.data.backup.WebDavUploader
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides cloud-backup infrastructure singletons.
 *
 * [WebDavUploader] is a stateful singleton (holds the configured base URL and auth header)
 * so it must live in [SingletonComponent] and be [Singleton]-scoped.
 * It is also bound to [CloudBackupUploader] so that feature modules can inject the
 * interface without depending directly on the data module.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindCloudBackupUploader(impl: WebDavUploader): CloudBackupUploader

    companion object {
        @Provides
        @Singleton
        fun provideWebDavUploader(): WebDavUploader = WebDavUploader()
    }
}
