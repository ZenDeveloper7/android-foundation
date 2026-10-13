package com.zen.foundation.core.data.di

import com.zen.foundation.core.data.repository.DefaultUserDataRepository
import com.zen.foundation.core.data.repository.NotesRepository
import com.zen.foundation.core.data.repository.OfflineNotesRepository
import com.zen.foundation.core.data.repository.UserDataRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindsNotesRepository(repository: OfflineNotesRepository): NotesRepository

    @Binds
    abstract fun bindsUserDataRepository(repository: DefaultUserDataRepository): UserDataRepository
}
