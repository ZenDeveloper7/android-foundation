package dev.zen.foundation.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class], version = 1, exportSchema = true)
internal abstract class FoundationDatabase : RoomDatabase() {
    abstract fun tasksDao(): TasksDao
}
