package com.zen.foundation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.zen.foundation.R
import com.zen.foundation.feature.notes.navigation.NoteEditorKey
import com.zen.foundation.feature.notes.navigation.NotesListKey
import com.zen.foundation.feature.settings.navigation.SettingsKey

/** Destinations shown in the navigation bar or rail. The first entry is the start destination. */
enum class TopLevelDestination(
    val key: NavKey,
    val icon: ImageVector,
    @param:StringRes val labelRes: Int
) {
    Notes(NotesListKey, Icons.AutoMirrored.Filled.List, R.string.app_nav_notes),
    Settings(SettingsKey, Icons.Default.Settings, R.string.app_nav_settings)
}

private val topLevelKeys: Set<NavKey> = TopLevelDestination.entries.map { it.key }.toSet()

/** The top-level destination the user is in, for highlighting the navigation item. */
fun List<NavKey>.currentTopLevelDestination(): TopLevelDestination =
    TopLevelDestination.entries.firstOrNull { it.key == lastOrNull { key -> key in topLevelKeys } }
        ?: TopLevelDestination.Notes

/** Navigation chrome shows only on top-level screens, not on detail screens like the editor. */
fun List<NavKey>.isOnTopLevelDestination(): Boolean = lastOrNull() in topLevelKeys

/**
 * Switches tabs. The start destination always stays at the root so Back from another tab
 * returns to it, as Material navigation guidance recommends.
 */
fun MutableList<NavKey>.navigateToTopLevel(destination: TopLevelDestination) {
    while (size > 1) removeAt(lastIndex)
    if (destination.key != firstOrNull()) add(destination.key)
}

/** Opens the editor, ignoring a repeated tap that would stack a second editor. */
fun MutableList<NavKey>.openNoteEditor(noteId: Long?) {
    if (lastOrNull() !is NoteEditorKey) add(NoteEditorKey(noteId))
}

/** Pops one screen, never the start destination (the system handles leaving the app). */
fun MutableList<NavKey>.navigateBack() {
    if (size > 1) removeAt(lastIndex)
}
