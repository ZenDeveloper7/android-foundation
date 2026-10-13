package com.zen.foundation.feature.notes.editor

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.zen.foundation.core.designsystem.theme.FoundationTheme
import com.zen.foundation.core.model.Note
import com.zen.foundation.core.testing.repository.FakeNotesRepository
import com.zen.foundation.core.testing.util.MainDispatcherRule
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Compose UI tests for the editor route's dialogs and back handling, via Robolectric. */
@RunWith(RobolectricTestRunner::class)
class NoteEditorRouteTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val viewModel =
        NoteEditorViewModel(
            1L,
            FakeNotesRepository(listOf(Note(1, "Groceries", "Milk", Instant.EPOCH))),
            SavedStateHandle()
        )

    @Test
    fun deleteDialog_survivesStateRestoration() {
        val tester = StateRestorationTester(composeRule)
        tester.setContent {
            FoundationTheme(dynamicColor = false) {
                NoteEditorRoute(noteId = 1L, onBack = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithContentDescription("Delete note").performClick()
        composeRule.onNodeWithText("Delete this note?").assertIsDisplayed()

        tester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Delete this note?").assertIsDisplayed()
    }

    @Test
    fun backWithUnsavedChanges_asksToDiscard() {
        var backCalls = 0
        composeRule.setContent {
            FoundationTheme(dynamicColor = false) {
                NoteEditorRoute(
                    noteId = 1L,
                    onBack = { backCalls++ },
                    viewModel = viewModel
                )
            }
        }
        composeRule.runOnIdle { viewModel.title.setTextAndPlaceCursorAtEnd("Changed") }

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        assertThat(backCalls).isEqualTo(0)
    }
}
