package com.zen.foundation.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.zen.foundation.core.common.ApplicationScope
import com.zen.foundation.core.common.Dispatcher
import com.zen.foundation.core.common.FoundationDispatcher
import com.zen.foundation.core.datastore.createUserPreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    /** File name is part of the on-disk format and of the backup rules in :app. */
    private const val USER_SETTINGS_FILE = "user_settings"

    @Provides
    @Singleton
    fun providesUserPreferencesDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
        @Dispatcher(FoundationDispatcher.IO) ioDispatcher: CoroutineDispatcher
    ): DataStore<Preferences> = createUserPreferencesDataStore(
        scope = scope + ioDispatcher,
        produceFile = { context.preferencesDataStoreFile(USER_SETTINGS_FILE) }
    )
}
