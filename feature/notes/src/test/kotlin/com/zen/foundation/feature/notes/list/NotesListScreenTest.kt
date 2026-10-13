package com.zen.foundation.feature.notes.list

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import com.zen.foundation.core.designsystem.theme.FoundationTheme
import com.zen.foundation.core.model.Note
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Compose UI tests on the JVM via Robolectric: fast enough to run on every change. */
@RunWith(RobolectricTestRunner::class)
class NotesListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyList_showsEmptyState() {
        setContent(NotesListUiState.Success(emptyList()))

        composeRule.onNodeWithText("No notes yet").assertIsDisplayed()
    }

    @Test
    fun blankTitle_showsUntitled() {
        setContent(NotesListUiState.Success(listOf(Note(1, "", "Body", Instant.EPOCH))))

        composeRule.onNodeWithText("Untitled").assertIsDisplayed()
    }

    @Test
    fun clickingNote_reportsItsId() {
        var clickedId: Long? = null
        setContent(
            NotesListUiState.Success(listOf(Note(7, "Groceries", "", Instant.EPOCH))),
            onNoteClick = { clickedId = it }
        )

        composeRule.onNodeWithText("Groceries").performClick()

        assertThat(clickedId).isEqualTo(7L)
    }

    @Test
    fun clickingNewNote_requestsCreation() {
        var createRequested = false
        setContent(NotesListUiState.Success(emptyList()), onCreateNote = { createRequested = true })

        // The extended FAB keeps its label out of the merged tree; clicking the label hits the button.
        composeRule.onNodeWithText("New note", useUnmergedTree = true).performClick()

        assertThat(createRequested).isTrue()
    }

    @Test
    fun errorState_retryRequestsReload() {
        var retryRequested = false
        setContent(NotesListUiState.Error, onRetry = { retryRequested = true })

        composeRule.onNodeWithText("Couldn't load notes").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertThat(retryRequested).isTrue()
    }

    @Test
    @Config(qualifiers = "w900dp-h700dp")
    fun largeFontAndTabletWidth_stillShowsNotesAndAction() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f)
            ) {
                FoundationTheme(dynamicColor = false) {
                    NotesListScreen(
                        uiState =
                            NotesListUiState.Success(
                                listOf(Note(1, "Groceries", "Milk", Instant.EPOCH))
                            ),
                        onNoteClick = {},
                        onCreateNote = {},
                        onRetry = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeRule.onNodeWithText("New note", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun setContent(
        uiState: NotesListUiState,
        onNoteClick: (Long) -> Unit = {},
        onCreateNote: () -> Unit = {},
        onRetry: () -> Unit = {}
    ) {
        composeRule.setContent {
            FoundationTheme(dynamicColor = false) {
                NotesListScreen(
                    uiState = uiState,
                    onNoteClick = onNoteClick,
                    onCreateNote = onCreateNote,
                    onRetry = onRetry
                )
            }
        }
    }
}
