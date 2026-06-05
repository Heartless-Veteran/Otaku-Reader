package app.otakureader.data.di

import app.otakureader.core.preferences.CloudBackupUploader
import app.otakureader.data.backup.WebDavUploader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindCloudBackupUploader(impl: WebDavUploader): CloudBackupUploader
}
