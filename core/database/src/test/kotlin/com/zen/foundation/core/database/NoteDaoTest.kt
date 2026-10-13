package com.zen.foundation.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.zen.foundation.core.database.dao.NoteDao
import com.zen.foundation.core.database.model.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NoteDaoTest {
    private lateinit var database: FoundationDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, FoundationDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeAll_ordersByMostRecentlyUpdated() = runTest {
        dao.insert(note(title = "old", updatedAt = 1))
        dao.insert(note(title = "new", updatedAt = 2))

        dao.observeAll().test {
            assertThat(awaitItem().map { it.title }).containsExactly("new", "old").inOrder()
        }
    }

    @Test
    fun update_changesOnlyTheTargetRow() = runTest {
        val first = dao.insert(note(title = "first", updatedAt = 1))
        val second = dao.insert(note(title = "second", updatedAt = 1))

        val changed = dao.update(first, title = "edited", body = "b", updatedAtEpochMillis = 5)

        assertThat(changed).isEqualTo(1)
        dao.observeById(second).test { assertThat(awaitItem()?.title).isEqualTo("second") }
        dao.observeById(first).test { assertThat(awaitItem()?.title).isEqualTo("edited") }
    }

    @Test
    fun update_missingRow_reportsNoChange() = runTest {
        assertThat(
            dao.update(id = 99, title = "t", body = "b", updatedAtEpochMillis = 1)
        ).isEqualTo(0)
    }

    @Test
    fun deleteById_removesRow() = runTest {
        val id = dao.insert(note(title = "gone", updatedAt = 1))

        dao.deleteById(id)

        dao.observeById(id).test { assertThat(awaitItem()).isNull() }
    }

    @Test
    fun concurrentInserts_keepEveryRow() = runTest {
        val ids = withContext(Dispatchers.Default) {
            (1..50).map { i ->
                async { dao.insert(note(title = "n$i", updatedAt = i.toLong())) }
            }.awaitAll()
        }

        assertThat(ids.toSet()).hasSize(50)
        dao.observeAll().test { assertThat(awaitItem()).hasSize(50) }
    }

    private fun note(title: String, updatedAt: Long) =
        NoteEntity(title = title, body = "", updatedAtEpochMillis = updatedAt)
}
