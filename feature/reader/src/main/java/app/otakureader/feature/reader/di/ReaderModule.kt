package app.otakureader.feature.reader.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt dependency injection module for the reader feature.
 */
@Module
@InstallIn(ViewModelComponent::class)
object ReaderModule
