package dev.zen.foundation.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomTasksRepositoryTest {
    private lateinit var database: FoundationDatabase
    private lateinit var repository: RoomTasksRepository

    @Before
    fun setup() {
        database =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FoundationDatabase::class.java,
                )
                .build()
        repository = RoomTasksRepository(database.tasksDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun concurrentInsertionsPreserveEveryRecord() = runTest {
        (1..20).map { index -> async { repository.add("Task $index") } }.awaitAll()
        val tasks = repository.observeTasks().first()
        assertEquals(20, tasks.size)
        assertEquals(20, tasks.map { it.id }.distinct().size)
    }

    @Test
    fun updateAndDeleteAffectOnlyTheirTarget() = runTest {
        repository.add("First")
        repository.add("Second")
        val initial = repository.observeTasks().first()
        val first = initial.first { it.title == "First" }
        val second = initial.first { it.title == "Second" }
        repository.setCompleted(first.id, true)
        repository.delete(second.id)
        assertEquals(listOf(Task(first.id, "First", true)), repository.observeTasks().first())
    }

    @Test
    fun validatesBeforeWritingAndTrimsTitles() = runTest {
        for (invalid in listOf(" ", "a".repeat(201))) {
            try {
                repository.add(invalid)
                error("Expected validation failure")
            } catch (_: IllegalArgumentException) {
                /* Expected domain constraint. */
            }
        }
        assertTrue(repository.observeTasks().first().isEmpty())
        repository.add("  Valid  ")
        assertEquals("Valid", repository.observeTasks().first().single().title)
    }
}
