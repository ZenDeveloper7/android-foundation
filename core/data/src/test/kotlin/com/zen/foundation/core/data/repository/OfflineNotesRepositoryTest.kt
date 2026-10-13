package com.zen.foundation.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.zen.foundation.core.database.dao.NoteDao
import com.zen.foundation.core.database.model.NoteEntity
import com.zen.foundation.core.model.NoteLimits
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test

class OfflineNotesRepositoryTest {
    private val now = Instant.parse("2026-01-15T10:00:00Z")
    private val dao = FakeNoteDao()
    private val repository = OfflineNotesRepository(dao, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun createNote_trimsTitleAndStampsTime() = runTest {
        val id = repository.createNote(title = "  Groceries  ", body = "milk")

        repository.observeNote(id).test {
            val note = awaitItem()!!
            assertThat(note.title).isEqualTo("Groceries")
            assertThat(note.body).isEqualTo("milk")
            assertThat(note.updatedAt).isEqualTo(now)
        }
    }

    @Test
    fun updateNote_afterDelete_reportsFailure() = runTest {
        val id = repository.createNote(title = "t", body = "b")
        repository.deleteNote(id)

        assertThat(repository.updateNote(id, title = "t2", body = "b2")).isFalse()
    }

    @Test
    fun observeNotes_mapsEveryRow() = runTest {
        repository.createNote(title = "a", body = "")
        repository.createNote(title = "b", body = "")

        repository.observeNotes().test {
            assertThat(awaitItem().map { it.title }).containsExactly("a", "b")
        }
    }

    @Test
    fun createNote_titleAtLimit_isAccepted() = runTest {
        val title = "a".repeat(NoteLimits.MAX_TITLE_LENGTH)

        val id = repository.createNote(title = title, body = "")

        repository.observeNote(id).test { assertThat(awaitItem()?.title).isEqualTo(title) }
    }

    @Test
    fun createNote_titleOverLimit_isRejectedAndNotWritten() = runTest {
        val title = "a".repeat(NoteLimits.MAX_TITLE_LENGTH + 1)

        assertRejects { repository.createNote(title = title, body = "") }

        repository.observeNotes().test { assertThat(awaitItem()).isEmpty() }
    }

    @Test
    fun updateNote_bodyOverLimit_isRejectedAndKeepsOldBody() = runTest {
        val id = repository.createNote(title = "t", body = "original")
        val body = "b".repeat(NoteLimits.MAX_BODY_LENGTH + 1)

        assertRejects { repository.updateNote(id, title = "t", body = body) }

        repository.observeNote(id).test { assertThat(awaitItem()?.body).isEqualTo("original") }
    }

    private suspend fun assertRejects(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // Expected.
        }
    }
}

/** In-memory [NoteDao] so repository logic is tested without Robolectric or SQLite. */
private class FakeNoteDao : NoteDao {
    private val rows = MutableStateFlow(emptyList<NoteEntity>())
    private var nextId = 1L

    override fun observeAll(): Flow<List<NoteEntity>> = rows

    override fun observeById(id: Long): Flow<NoteEntity?> = rows.map { list ->
        list.firstOrNull {
            it.id ==
                id
        }
    }

    override suspend fun insert(note: NoteEntity): Long {
        val id = nextId++
        rows.update { it + note.copy(id = id) }
        return id
    }

    override suspend fun update(
        id: Long,
        title: String,
        body: String,
        updatedAtEpochMillis: Long
    ): Int {
        if (rows.value.none { it.id == id }) return 0
        rows.update { list ->
            list.map {
                if (it.id ==
                    id
                ) {
                    it.copy(
                        title = title,
                        body = body,
                        updatedAtEpochMillis = updatedAtEpochMillis
                    )
                } else {
                    it
                }
            }
        }
        return 1
    }

    override suspend fun deleteById(id: Long) {
        rows.update { list -> list.filterNot { it.id == id } }
    }
}
