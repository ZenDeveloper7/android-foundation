package com.zen.foundation.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zen.foundation.core.database.dao.NoteDao
import com.zen.foundation.core.database.model.NoteEntity

/**
 * Bump [version] and add an `AutoMigration` (or a manual `Migration`) for every schema change.
 * Exported schemas in `core/database/schemas/` are committed so migrations can be tested.
 */
@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = true
)
abstract class FoundationDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
