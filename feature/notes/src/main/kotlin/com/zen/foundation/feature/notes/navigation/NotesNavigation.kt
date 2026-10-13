package com.zen.foundation.feature.notes.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.zen.foundation.feature.notes.editor.NoteEditorRoute
import com.zen.foundation.feature.notes.list.NotesListRoute
import kotlinx.serialization.Serializable

/**
 * Navigation keys are the feature's public API. They are serializable so the back stack
 * survives process death.
 */
@Serializable
data object NotesListKey : NavKey

/** [noteId] is null for a new note. */
@Serializable
data class NoteEditorKey(val noteId: Long? = null) : NavKey

/**
 * Registers this feature's destinations. The app owns the back stack; features only describe
 * where they want to go through callbacks, so they never depend on each other.
 */
fun EntryProviderScope<NavKey>.notesEntries(
    onOpenEditor: (noteId: Long?) -> Unit,
    onBack: () -> Unit
) {
    entry<NotesListKey> {
        NotesListRoute(
            onNoteClick = { noteId -> onOpenEditor(noteId) },
            onCreateNote = { onOpenEditor(null) }
        )
    }
    entry<NoteEditorKey> { key ->
        NoteEditorRoute(noteId = key.noteId, onBack = onBack)
    }
}
