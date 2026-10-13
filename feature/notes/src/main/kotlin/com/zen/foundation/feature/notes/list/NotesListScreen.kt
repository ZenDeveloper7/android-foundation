package com.zen.foundation.feature.notes.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zen.foundation.core.designsystem.component.FoundationLoadingIndicator
import com.zen.foundation.core.designsystem.component.FoundationMessageState
import com.zen.foundation.core.designsystem.component.PreviewFoundation
import com.zen.foundation.core.designsystem.component.foundationContentWidth
import com.zen.foundation.core.designsystem.theme.FoundationTheme
import com.zen.foundation.core.model.Note
import com.zen.foundation.feature.notes.R
import java.time.Instant

/** Space below the last item so the floating action button never covers it. */
private val FabClearance = 96.dp

/** Stateful entry point: connects the ViewModel. Keep logic out of here. */
@Composable
internal fun NotesListRoute(
    onNoteClick: (Long) -> Unit,
    onCreateNote: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NotesListScreen(
        uiState = uiState,
        onNoteClick = onNoteClick,
        onCreateNote = onCreateNote,
        onRetry = viewModel::retry,
        modifier = modifier
    )
}

/** Stateless screen: renders [uiState] and reports events. Previews and UI tests target this. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesListScreen(
    uiState: NotesListUiState,
    onNoteClick: (Long) -> Unit,
    onCreateNote: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_notes_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNote,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.feature_notes_new_note)) }
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (uiState) {
            NotesListUiState.Loading -> FoundationLoadingIndicator(contentModifier)

            NotesListUiState.Error ->
                FoundationMessageState(
                    title = stringResource(R.string.feature_notes_error_load_title),
                    message = stringResource(R.string.feature_notes_error_load_message),
                    modifier = contentModifier,
                    action = {
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.feature_notes_retry))
                        }
                    }
                )

            is NotesListUiState.Success ->
                if (uiState.notes.isEmpty()) {
                    FoundationMessageState(
                        title = stringResource(R.string.feature_notes_empty_title),
                        message = stringResource(R.string.feature_notes_empty_message),
                        modifier = contentModifier
                    )
                } else {
                    NotesList(
                        notes = uiState.notes,
                        onNoteClick = onNoteClick,
                        modifier = contentModifier
                    )
                }
        }
    }
}

@Composable
private fun NotesList(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = FoundationTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxHeight().foundationContentWidth(),
        contentPadding =
            PaddingValues(
                start = spacing.medium,
                top = spacing.small,
                end = spacing.medium,
                bottom = FabClearance
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        // Stable keys keep scroll position and animations correct when the list changes.
        items(items = notes, key = { it.id }) { note ->
            NoteCard(note = note, onClick = {
                onNoteClick(note.id)
            }, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(FoundationTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(FoundationTheme.spacing.extraSmall)
        ) {
            Text(
                text = note.title.ifBlank { stringResource(R.string.feature_notes_untitled) },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (note.body.isNotBlank()) {
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@PreviewFoundation
@Composable
private fun NotesListScreenPreview() {
    FoundationTheme(dynamicColor = false) {
        NotesListScreen(
            uiState =
                NotesListUiState.Success(
                    listOf(
                        Note(1, "Groceries", "Milk, eggs, coffee", Instant.EPOCH),
                        Note(
                            2,
                            "",
                            "A note without a title wraps onto several lines when long enough.",
                            Instant.EPOCH
                        )
                    )
                ),
            onNoteClick = {},
            onCreateNote = {},
            onRetry = {}
        )
    }
}

@PreviewFoundation
@Composable
private fun NotesListScreenEmptyPreview() {
    FoundationTheme(dynamicColor = false) {
        NotesListScreen(
            uiState = NotesListUiState.Success(emptyList()),
            onNoteClick = {},
            onCreateNote = {},
            onRetry = {}
        )
    }
}

@PreviewFoundation
@Composable
private fun NotesListScreenErrorPreview() {
    FoundationTheme(dynamicColor = false) {
        NotesListScreen(
            uiState = NotesListUiState.Error,
            onNoteClick = {},
            onCreateNote = {},
            onRetry = {}
        )
    }
}
