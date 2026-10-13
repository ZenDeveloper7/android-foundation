package com.zen.foundation.navigation

import com.google.common.truth.Truth.assertThat
import com.zen.foundation.feature.notes.navigation.NoteEditorKey
import com.zen.foundation.feature.notes.navigation.NotesListKey
import com.zen.foundation.feature.settings.navigation.SettingsKey
import org.junit.Test

class TopLevelNavigationTest {
    @Test
    fun switchingTabs_keepsStartDestinationAtRoot() {
        val backStack = mutableListOf(NotesListKey, NoteEditorKey(1))

        backStack.navigateToTopLevel(TopLevelDestination.Settings)

        assertThat(backStack).containsExactly(NotesListKey, SettingsKey).inOrder()
        assertThat(backStack.currentTopLevelDestination()).isEqualTo(TopLevelDestination.Settings)
    }

    @Test
    fun returningToStartTab_clearsOtherTabs() {
        val backStack = mutableListOf(NotesListKey, SettingsKey)

        backStack.navigateToTopLevel(TopLevelDestination.Notes)

        assertThat(backStack).containsExactly(NotesListKey)
    }

    @Test
    fun repeatedOpen_addsOnlyOneEditor() {
        val backStack = mutableListOf<androidx.navigation3.runtime.NavKey>(NotesListKey)

        backStack.openNoteEditor(1)
        backStack.openNoteEditor(1)

        assertThat(backStack).containsExactly(NotesListKey, NoteEditorKey(1)).inOrder()
        assertThat(backStack.isOnTopLevelDestination()).isFalse()
    }

    @Test
    fun back_neverPopsTheStartDestination() {
        val backStack = mutableListOf<androidx.navigation3.runtime.NavKey>(NotesListKey)

        backStack.navigateBack()

        assertThat(backStack).containsExactly(NotesListKey)
    }
}
