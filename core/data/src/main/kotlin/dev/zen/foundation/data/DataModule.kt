package dev.zen.foundation.data

import android.content.Context
import androidx.room.Room
import dagger.Binds
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
    fun database(@ApplicationContext context: Context): FoundationDatabase =
        Room.databaseBuilder(context, FoundationDatabase::class.java, "foundation.db").build()

    @Provides fun tasksDao(database: FoundationDatabase): TasksDao = database.tasksDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds abstract fun tasksRepository(repository: RoomTasksRepository): TasksRepository
}
