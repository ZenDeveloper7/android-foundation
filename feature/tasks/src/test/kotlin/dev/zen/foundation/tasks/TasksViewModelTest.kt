package dev.zen.foundation.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.zen.foundation.data.Task
import dev.zen.foundation.data.TasksRepository
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun TestScope.model(
        repository: FakeRepository,
        state: SavedStateHandle = SavedStateHandle(),
    ): TasksViewModel {
        val model = TasksViewModel(repository, state)
        store.put("tasks", model)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
        runCurrent()
        return model
    }

    @Test
    fun emitsPersistedTasks() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val model = model(repository)
            repository.tasks.value = listOf(Task(1, "Stored task", false))
            runCurrent()
            assertEquals("Stored task", model.uiState.value.tasks.single().title)
            assertFalse(model.uiState.value.loading)
        }

    @Test
    fun ignoresBlankSubmission() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val model = model(repository)
            model.updateDraft("  ")
            model.addTask()
            runCurrent()
            assertEquals(0, repository.adds.size)
        }

    @Test
    fun successfulSaveClearsOnlyTheSubmittedDraft() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val model = model(repository)
            model.updateDraft("First")
            model.addTask()
            runCurrent()
            assertEquals(listOf("First"), repository.adds)
            assertEquals("", model.uiState.value.draft)
        }

    @Test
    fun lateSavePreservesNewTypingAndPreventsDuplicateSubmission() =
        runTest(dispatcher) {
            val repository = FakeRepository().apply { gate = CompletableDeferred() }
            val model = model(repository)
            model.updateDraft("First")
            model.addTask()
            model.addTask()
            runCurrent()
            model.updateDraft("Second")
            repository.gate!!.complete(Unit)
            runCurrent()
            assertEquals(listOf("First"), repository.adds)
            assertEquals("Second", model.uiState.value.draft)
        }

    @Test
    fun writeFailureKeepsDraftAndAllowsRetry() =
        runTest(dispatcher) {
            val repository = FakeRepository().apply { failWrite = true }
            val model = model(repository)
            model.updateDraft("Keep me")
            model.addTask()
            runCurrent()
            assertTrue(model.uiState.value.writeFailed)
            assertEquals("Keep me", model.uiState.value.draft)
            assertFalse(model.uiState.value.saving)
            repository.failWrite = false
            model.addTask()
            runCurrent()
            assertFalse(model.uiState.value.writeFailed)
            assertEquals("", model.uiState.value.draft)
        }

    @Test
    fun failedReadCanBeRetried() =
        runTest(dispatcher) {
            val repository = FakeRepository().apply { failRead = true }
            val model = model(repository)
            assertTrue(model.uiState.value.loadFailed)
            repository.failRead = false
            model.retryLoad()
            runCurrent()
            assertFalse(model.uiState.value.loadFailed)
        }

    @Test
    fun restoresDraftFromSavedStateAndBoundsInput() =
        runTest(dispatcher) {
            val state = SavedStateHandle(mapOf("draft" to "Restored"))
            val model = model(FakeRepository(), state)
            assertEquals("Restored", model.uiState.value.draft)
            model.updateDraft("a".repeat(250))
            runCurrent()
            assertEquals(200, state.get<String>("draft")!!.length)
        }

    @Test
    fun usesStableRecordIdentityForCompletionAndDelete() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val model = model(repository)
            val task = Task(42, "Task", false)
            model.setCompleted(task, true)
            runCurrent()
            model.delete(task)
            runCurrent()
            assertEquals(listOf(42L to true), repository.completions)
            assertEquals(listOf(42L), repository.deletes)
        }
}

private class FakeRepository : TasksRepository {
    val tasks = MutableStateFlow<List<Task>>(emptyList())
    val adds = mutableListOf<String>()
    val completions = mutableListOf<Pair<Long, Boolean>>()
    val deletes = mutableListOf<Long>()
    var failWrite = false
    var failRead = false
    var gate: CompletableDeferred<Unit>? = null

    override fun observeTasks(): Flow<List<Task>> =
        if (failRead) flow { throw IOException() } else tasks

    override suspend fun add(title: String) {
        gate?.await()
        if (failWrite) throw IOException()
        adds.add(title)
    }

    override suspend fun setCompleted(id: Long, completed: Boolean) {
        completions.add(id to completed)
    }

    override suspend fun delete(id: Long) {
        deletes.add(id)
    }
}
