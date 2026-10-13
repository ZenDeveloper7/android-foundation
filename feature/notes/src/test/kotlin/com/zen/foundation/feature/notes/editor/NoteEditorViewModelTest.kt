package com.zen.foundation.feature.notes.editor

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.zen.foundation.core.data.repository.NotesRepository
import com.zen.foundation.core.model.Note
import com.zen.foundation.core.testing.repository.FakeNotesRepository
import com.zen.foundation.core.testing.util.MainDispatcherRule
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric because SavedStateHandle.saveable stores state in an Android Bundle.
@RunWith(RobolectricTestRunner::class)
class NoteEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val existing =
        Note(id = 1, title = "Groceries", body = "Milk", updatedAt = Instant.EPOCH)
    private val repository = FakeNotesRepository(listOf(existing))

    private fun viewModel(noteId: Long?) =
        NoteEditorViewModel(noteId, repository, SavedStateHandle())

    @Test
    fun existingNote_isLoadedWithoutUnsavedChanges() {
        val viewModel = viewModel(noteId = 1)

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.title.text.toString()).isEqualTo("Groceries")
        assertThat(viewModel.hasUnsavedChanges()).isFalse()
    }

    @Test
    fun editing_marksUnsavedChanges() {
        val viewModel = viewModel(noteId = 1)

        viewModel.body.setTextAndPlaceCursorAtEnd("Milk, eggs")

        assertThat(viewModel.hasUnsavedChanges()).isTrue()
    }

    @Test
    fun newNote_cannotBeSavedWhileBlank() {
        val viewModel = viewModel(noteId = null)

        viewModel.title.setTextAndPlaceCursorAtEnd("   ")

        assertThat(viewModel.canSave()).isFalse()
    }

    @Test
    fun savingNewNote_createsItAndFinishes() {
        val viewModel = viewModel(noteId = null)
        viewModel.title.setTextAndPlaceCursorAtEnd("Ideas")

        viewModel.save()

        assertThat(viewModel.uiState.value.isFinished).isTrue()
        assertThat(repository.currentNotes.map { it.title }).contains("Ideas")
    }

    @Test
    fun saveFailure_keepsEditorOpenAndReportsError() {
        val viewModel = viewModel(noteId = 1)
        viewModel.title.setTextAndPlaceCursorAtEnd("Changed")
        repository.failWrites = true

        viewModel.save()

        val state = viewModel.uiState.value
        assertThat(state.isFinished).isFalse()
        assertThat(state.error).isEqualTo(NoteEditorError.SaveFailed)
        assertThat(viewModel.hasUnsavedChanges()).isTrue()
    }

    @Test
    fun missingNote_isReported() {
        val viewModel = viewModel(noteId = 99)

        assertThat(viewModel.uiState.value.isMissing).isTrue()
    }

    @Test
    fun delete_removesNoteAndFinishes() {
        val viewModel = viewModel(noteId = 1)

        viewModel.delete()

        assertThat(viewModel.uiState.value.isFinished).isTrue()
        assertThat(repository.currentNotes).isEmpty()
    }

    @Test
    fun typingDuringSave_keepsNewTextAndStaysOpen() {
        repository.writeGate = CompletableDeferred()
        val viewModel = viewModel(noteId = 1)
        viewModel.title.setTextAndPlaceCursorAtEnd("A")

        viewModel.save()
        assertThat(viewModel.uiState.value.isSaving).isTrue()
        viewModel.title.setTextAndPlaceCursorAtEnd("AB")
        repository.writeGate?.complete(Unit)

        assertThat(viewModel.uiState.value.isFinished).isFalse()
        assertThat(viewModel.uiState.value.isSaving).isFalse()
        assertThat(viewModel.hasUnsavedChanges()).isTrue()
        assertThat(viewModel.title.text.toString()).isEqualTo("AB")
        assertThat(repository.currentNotes.single { it.id == 1L }.title).isEqualTo("A")
    }

    @Test
    fun repeatedSaveWhileSaving_writesOnce() {
        repository.writeGate = CompletableDeferred()
        val viewModel = viewModel(noteId = 1)
        viewModel.title.setTextAndPlaceCursorAtEnd("A")

        viewModel.save()
        viewModel.save()
        repository.writeGate?.complete(Unit)

        assertThat(repository.writeCount).isEqualTo(1)
    }

    @Test
    fun newNote_savedAgain_updatesInsteadOfDuplicating() {
        val emptyRepository = FakeNotesRepository()
        emptyRepository.writeGate = CompletableDeferred()
        val viewModel = NoteEditorViewModel(null, emptyRepository, SavedStateHandle())
        viewModel.title.setTextAndPlaceCursorAtEnd("First")

        viewModel.save()
        viewModel.title.setTextAndPlaceCursorAtEnd("First!")
        emptyRepository.writeGate?.complete(Unit)
        emptyRepository.writeGate = null
        viewModel.save()

        assertThat(emptyRepository.currentNotes).hasSize(1)
        assertThat(emptyRepository.currentNotes.single().title).isEqualTo("First!")
    }

    @Test
    fun repeatedDelete_whileDeleting_callsRepositoryOnce() {
        repository.writeGate = CompletableDeferred()
        val countingRepository = CountingNotesRepository(repository)
        val viewModel = NoteEditorViewModel(1, countingRepository, SavedStateHandle())

        viewModel.delete()
        viewModel.delete()
        repository.writeGate?.complete(Unit)

        assertThat(countingRepository.deleteCalls).isEqualTo(1)
        assertThat(viewModel.uiState.value.isFinished).isTrue()
    }

    @Test
    fun loadFailure_thenRetry_loadsNote() {
        repository.failReads = true
        val viewModel = viewModel(noteId = 1)

        assertThat(viewModel.uiState.value.loadFailed).isTrue()
        assertThat(viewModel.uiState.value.isLoading).isFalse()

        repository.failReads = false
        viewModel.retryLoad()

        assertThat(viewModel.uiState.value.loadFailed).isFalse()
        assertThat(viewModel.title.text.toString()).isEqualTo("Groceries")
    }
}

/** Counts [deleteNote] calls; every other call goes straight to the delegate. */
private class CountingNotesRepository(private val delegate: NotesRepository) :
    NotesRepository by delegate {
    var deleteCalls = 0
        private set

    override suspend fun deleteNote(id: Long) {
        deleteCalls++
        delegate.deleteNote(id)
    }
}
