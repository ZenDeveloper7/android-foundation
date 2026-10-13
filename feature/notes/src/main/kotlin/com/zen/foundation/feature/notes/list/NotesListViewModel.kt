package com.zen.foundation.feature.notes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zen.foundation.core.data.repository.NotesRepository
import com.zen.foundation.core.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class NotesListViewModel
@Inject
constructor(private val notesRepository: NotesRepository) :
    ViewModel() {
    /** Bumped by [retry] to restart the notes subscription after a load error. */
    private val retryCount = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NotesListUiState> =
        retryCount
            .flatMapLatest {
                notesRepository
                    .observeNotes()
                    .map<List<Note>, NotesListUiState>(NotesListUiState::Success)
                    .onStart { emit(NotesListUiState.Loading) }
                    .catch { emit(NotesListUiState.Error) }
            }
            .stateIn(
                scope = viewModelScope,
                // Survives configuration changes; stops the upstream in the background.
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = NotesListUiState.Loading
            )

    /** Restarts loading the notes after a failure. */
    fun retry() {
        retryCount.update { it + 1 }
    }
}

sealed interface NotesListUiState {
    data object Loading : NotesListUiState

    data class Success(val notes: List<Note>) : NotesListUiState

    data object Error : NotesListUiState
}
