package com.zen.foundation.core.testing.repository

import com.zen.foundation.core.data.repository.NotesRepository
import com.zen.foundation.core.model.Note
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [NotesRepository] for ViewModel and UI tests.
 *
 * - [failWrites]: writes throw [IOException].
 * - [failReads]: flows collected while it is true throw [IOException] (test load errors and retry).
 * - [writeGate]: when set, writes suspend until it completes (test in-flight saves).
 */
class FakeNotesRepository(initialNotes: List<Note> = emptyList()) : NotesRepository {
    private val notes = MutableStateFlow(initialNotes)
    private var nextId = (initialNotes.maxOfOrNull { it.id } ?: 0L) + 1

    var failWrites: Boolean = false
    var failReads: Boolean = false
    var writeGate: CompletableDeferred<Unit>? = null

    /** Number of completed create and update calls, to detect duplicate saves. */
    var writeCount: Int = 0
        private set

    val currentNotes: List<Note> get() = notes.value

    override fun observeNotes(): Flow<List<Note>> = readable(notes)

    override fun observeNote(id: Long): Flow<Note?> =
        readable(notes).map { list -> list.firstOrNull { it.id == id } }

    override suspend fun createNote(title: String, body: String): Long {
        beforeWrite()
        val id = nextId++
        notes.update { it + Note(id, title.trim(), body, Instant.EPOCH) }
        writeCount++
        return id
    }

    override suspend fun updateNote(id: Long, title: String, body: String): Boolean {
        beforeWrite()
        if (notes.value.none { it.id == id }) return false
        notes.update { list ->
            list.map { note ->
                if (note.id == id) note.copy(title = title.trim(), body = body) else note
            }
        }
        writeCount++
        return true
    }

    override suspend fun deleteNote(id: Long) {
        beforeWrite()
        notes.update { list -> list.filterNot { it.id == id } }
    }

    private fun <T> readable(source: Flow<T>): Flow<T> = flow {
        if (failReads) throw IOException("Simulated read failure")
        source.collect { emit(it) }
    }

    private suspend fun beforeWrite() {
        writeGate?.await()
        if (failWrites) throw IOException("Simulated write failure")
    }
}
