package com.zen.foundation.core.data.repository

import com.zen.foundation.core.database.dao.NoteDao
import com.zen.foundation.core.database.model.NoteEntity
import com.zen.foundation.core.database.model.asExternalModel
import com.zen.foundation.core.model.Note
import com.zen.foundation.core.model.NoteLimits
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Local-only implementation. Add a network data source here when the app gets a backend. */
internal class OfflineNotesRepository
@Inject
constructor(
    private val noteDao: NoteDao,
    private val clock: Clock
) : NotesRepository {
    override fun observeNotes(): Flow<List<Note>> = noteDao.observeAll().map { notes ->
        notes.map(NoteEntity::asExternalModel)
    }

    override fun observeNote(id: Long): Flow<Note?> = noteDao.observeById(id).map {
        it?.asExternalModel()
    }

    override suspend fun createNote(title: String, body: String): Long {
        val cleanTitle = title.trim()
        validate(cleanTitle, body)
        return noteDao.insert(
            NoteEntity(
                title = cleanTitle,
                body = body,
                updatedAtEpochMillis = clock.millis()
            )
        )
    }

    override suspend fun updateNote(id: Long, title: String, body: String): Boolean {
        val cleanTitle = title.trim()
        validate(cleanTitle, body)
        return noteDao.update(
            id = id,
            title = cleanTitle,
            body = body,
            updatedAtEpochMillis = clock.millis()
        ) > 0
    }

    override suspend fun deleteNote(id: Long) = noteDao.deleteById(id)

    /** Rejects oversized input before any DAO call, so nothing is written. */
    private fun validate(title: String, body: String) {
        require(title.length <= NoteLimits.MAX_TITLE_LENGTH) {
            "Title exceeds ${NoteLimits.MAX_TITLE_LENGTH} characters"
        }
        require(body.length <= NoteLimits.MAX_BODY_LENGTH) {
            "Body exceeds ${NoteLimits.MAX_BODY_LENGTH} characters"
        }
    }
}
