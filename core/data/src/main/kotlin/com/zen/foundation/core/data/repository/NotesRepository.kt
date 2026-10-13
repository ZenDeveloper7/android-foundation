package com.zen.foundation.core.data.repository

import com.zen.foundation.core.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * The single source of truth for notes. ViewModels depend on this interface, never on the
 * database, so tests can substitute a fake and the storage can change without touching UI code.
 */
interface NotesRepository {
    fun observeNotes(): Flow<List<Note>>

    fun observeNote(id: Long): Flow<Note?>

    /**
     * Returns the id of the new note. Throws [IllegalArgumentException] when the title (after
     * trimming) or body exceeds [com.zen.foundation.core.model.NoteLimits].
     */
    suspend fun createNote(title: String, body: String): Long

    /**
     * Returns false when the note was deleted elsewhere before the update ran. Throws
     * [IllegalArgumentException] when the title (after trimming) or body exceeds
     * [com.zen.foundation.core.model.NoteLimits].
     */
    suspend fun updateNote(id: Long, title: String, body: String): Boolean

    suspend fun deleteNote(id: Long)
}
