package com.zen.foundation.core.database.di

import android.content.Context
import androidx.room.Room
import com.zen.foundation.core.database.FoundationDatabase
import com.zen.foundation.core.database.dao.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): FoundationDatabase = Room
        .databaseBuilder(context, FoundationDatabase::class.java, "foundation.db")
        .build()

    @Provides
    fun providesNoteDao(database: FoundationDatabase): NoteDao = database.noteDao()
}
