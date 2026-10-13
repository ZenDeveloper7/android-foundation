package com.zen.foundation.navigation

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.google.common.truth.Truth.assertThat
import com.zen.foundation.feature.notes.navigation.NoteEditorKey
import com.zen.foundation.feature.notes.navigation.NotesListKey
import com.zen.foundation.feature.settings.navigation.SettingsKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackStackRestorationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var captured: NavBackStack<NavKey>? = null

    @Test
    fun backStack_keysSurviveSavedStateRestore() {
        val tester = StateRestorationTester(composeRule)
        tester.setContent {
            captured = rememberNavBackStack(NotesListKey)
        }

        composeRule.runOnIdle {
            val stack = checkNotNull(captured)
            stack.add(NoteEditorKey(5))
            stack.add(SettingsKey)
        }

        tester.emulateSavedInstanceStateRestore()

        composeRule.runOnIdle {
            val restored = checkNotNull(captured)
            assertThat(restored.toList())
                .containsExactly(NotesListKey, NoteEditorKey(5), SettingsKey)
                .inOrder()
        }
    }
}
