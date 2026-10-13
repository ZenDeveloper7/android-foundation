package com.zen.foundation.feature.notes.editor

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.zen.foundation.core.common.suspendRunCatching
import com.zen.foundation.core.data.repository.NotesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Text lives in [TextFieldState] (not a StateFlow) so typing is never delayed by a frame; it is
 * saved in [SavedStateHandle] so drafts survive process death. Everything else is [uiState].
 */
@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel(assistedFactory = NoteEditorViewModel.Factory::class)
class NoteEditorViewModel
@AssistedInject
constructor(
    @Assisted private val noteId: Long?,
    private val notesRepository: NotesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(noteId: Long?): NoteEditorViewModel
    }

    val title: TextFieldState by savedStateHandle.saveable(saver = TextFieldState.Saver) {
        TextFieldState()
    }
    val body: TextFieldState by savedStateHandle.saveable(saver = TextFieldState.Saver) {
        TextFieldState()
    }

    /** The id of the persisted note: starts as [noteId] and is set once a new note is created. */
    private var savedNoteId: Long?
        get() = savedStateHandle[KEY_SAVED_NOTE_ID]
        set(value) {
            savedStateHandle[KEY_SAVED_NOTE_ID] = value
        }

    init {
        if (!savedStateHandle.contains(KEY_SAVED_NOTE_ID)) savedNoteId = noteId
    }

    private val needsInitialLoad =
        noteId != null && savedStateHandle.get<Boolean>(KEY_LOADED) != true

    private val _uiState =
        MutableStateFlow(
            NoteEditorUiState(isNewNote = savedNoteId == null, isLoading = needsInitialLoad)
        )
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        if (needsInitialLoad && noteId != null) load(noteId)
    }

    /** Reads Compose state, so composables that call it recompose as the user types. */
    fun hasUnsavedChanges(): Boolean =
        title.text.toString() != (savedStateHandle[KEY_SAVED_TITLE] ?: "") ||
            body.text.toString() != (savedStateHandle[KEY_SAVED_BODY] ?: "")

    fun canSave(): Boolean =
        !_uiState.value.isBusy && (title.text.isNotBlank() || body.text.isNotBlank())

    fun save() {
        if (!canSave()) return
        val submittedTitle = title.text.toString()
        val submittedBody = body.text.toString()
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result =
                suspendRunCatching {
                    val id = savedNoteId
                    if (id == null) {
                        // Record the new id so a later save updates this note, not a duplicate.
                        savedNoteId = notesRepository.createNote(submittedTitle, submittedBody)
                        true
                    } else {
                        notesRepository.updateNote(id, submittedTitle, submittedBody)
                    }
                }
            val written = result.isSuccess && result.getOrThrow()
            if (written) {
                // The submitted text is now the persisted baseline. Anything typed during the
                // save still differs from it, so it stays unsaved and the screen stays open.
                savedStateHandle[KEY_SAVED_TITLE] = submittedTitle
                savedStateHandle[KEY_SAVED_BODY] = submittedBody
            }
            val unchangedSinceSubmit =
                title.text.toString() == submittedTitle && body.text.toString() == submittedBody
            _uiState.update { state ->
                when {
                    result.isFailure -> state.copy(
                        isSaving = false,
                        error = NoteEditorError.SaveFailed
                    )

                    written -> state.copy(
                        isSaving = false,
                        isNewNote = false,
                        isFinished = unchangedSinceSubmit
                    )

                    else -> state.copy(isSaving = false, isMissing = true)
                }
            }
        }
    }

    fun delete() {
        val id = savedNoteId ?: return
        // One mutation at a time: a second tap while a save or delete is in flight is ignored.
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            val result = suspendRunCatching { notesRepository.deleteNote(id) }
            _uiState.update { state ->
                if (result.isSuccess) {
                    state.copy(isDeleting = false, isFinished = true)
                } else {
                    state.copy(isDeleting = false, error = NoteEditorError.DeleteFailed)
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Re-runs the initial load after it failed. */
    fun retryLoad() {
        val id = noteId ?: return
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        load(id)
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            val result = suspendRunCatching { notesRepository.observeNote(id).first() }
            if (result.isFailure) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                return@launch
            }
            val note = result.getOrNull()
            if (note == null) {
                _uiState.update { it.copy(isLoading = false, isMissing = true) }
                return@launch
            }
            title.setTextAndPlaceCursorAtEnd(note.title)
            body.setTextAndPlaceCursorAtEnd(note.body)
            savedStateHandle[KEY_SAVED_TITLE] = note.title
            savedStateHandle[KEY_SAVED_BODY] = note.body
            savedStateHandle[KEY_LOADED] = true
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private companion object {
        const val KEY_LOADED = "loaded"
        const val KEY_SAVED_NOTE_ID = "saved_note_id"
        const val KEY_SAVED_TITLE = "saved_title"
        const val KEY_SAVED_BODY = "saved_body"
    }
}

data class NoteEditorUiState(
    val isNewNote: Boolean,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    /** The initial read failed: show an error with a retry action. */
    val loadFailed: Boolean = false,
    /** The note was deleted elsewhere: show a message instead of the editor. */
    val isMissing: Boolean = false,
    /** Saved or deleted: the screen should close. */
    val isFinished: Boolean = false,
    /** A one-off message. The UI shows it, then calls `consumeError()`. */
    val error: NoteEditorError? = null
) {
    /** A save or delete is in flight; the UI disables mutating actions. */
    val isBusy: Boolean
        get() = isSaving || isDeleting
}

enum class NoteEditorError {
    SaveFailed,
    DeleteFailed
}
