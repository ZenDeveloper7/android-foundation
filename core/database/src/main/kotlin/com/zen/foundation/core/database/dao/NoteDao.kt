package com.zen.foundation.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zen.foundation.core.database.model.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Reads are observable [Flow]s; writes are per-row suspend functions. Room runs both off the main
 * thread, so callers need no dispatcher switching. Never read-modify-write a whole table.
 */
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updated_at DESC, id DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<NoteEntity?>

    /** Returns the new row id. */
    @Insert
    suspend fun insert(note: NoteEntity): Long

    /** Returns the number of rows changed: 0 when the note no longer exists. */
    @Query(
        "UPDATE notes SET title = :title, body = :body, updated_at = :updatedAtEpochMillis " +
            "WHERE id = :id"
    )
    suspend fun update(id: Long, title: String, body: String, updatedAtEpochMillis: Long): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
