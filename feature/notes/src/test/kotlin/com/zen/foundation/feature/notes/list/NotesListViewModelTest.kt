package com.zen.foundation.feature.notes.list

import com.google.common.truth.Truth.assertThat
import com.zen.foundation.core.model.Note
import com.zen.foundation.core.testing.repository.FakeNotesRepository
import com.zen.foundation.core.testing.util.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun beforeCollection_stateIsLoading() {
        val viewModel = NotesListViewModel(FakeNotesRepository())

        assertThat(viewModel.uiState.value).isEqualTo(NotesListUiState.Loading)
    }

    @Test
    fun notesFromRepository_areExposed() = runTest {
        val note = Note(1, "Groceries", "Milk", Instant.EPOCH)
        val viewModel = NotesListViewModel(FakeNotesRepository(listOf(note)))
        // WhileSubscribed only runs the upstream while something collects.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertThat(viewModel.uiState.value).isEqualTo(NotesListUiState.Success(listOf(note)))
    }

    @Test
    fun loadFailure_thenRetry_showsNotes() = runTest {
        val note = Note(1, "Groceries", "Milk", Instant.EPOCH)
        val repository = FakeNotesRepository(listOf(note))
        repository.failReads = true
        val viewModel = NotesListViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertThat(viewModel.uiState.value).isEqualTo(NotesListUiState.Error)

        repository.failReads = false
        viewModel.retry()

        assertThat(viewModel.uiState.value).isEqualTo(NotesListUiState.Success(listOf(note)))
    }
}
