package dev.zen.foundation.tasks

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.zen.foundation.data.Task
import dev.zen.foundation.designsystem.FoundationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@OptIn(ExperimentalTestApi::class)
class TasksScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(state: TasksUiState, deleted: (Task) -> Unit = {}) {
        compose.setContent {
            FoundationTheme {
                TasksScreen(state, {}, {}, { _, _ -> }, deleted, {}, {}, {})
            }
        }
    }

    @Test
    fun emptyStateDisablesBlankSubmission() {
        render(TasksUiState(loading = false))
        compose.onNodeWithText("No tasks yet. Add your first task above.").assertIsDisplayed()
        compose.onNodeWithText("Add task").assertIsNotEnabled()
    }

    @Test
    fun deletionRequiresConfirmation() {
        var deleted: Task? = null
        val task = Task(7, "Keep until confirmed", false)
        render(TasksUiState(tasks = listOf(task), loading = false)) { deleted = it }
        compose.onNodeWithText("Delete").performClick()
        assertEquals(null, deleted)
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(null, deleted)
        compose.onNodeWithText("Delete").performClick()
        compose.onAllNodesWithText("Delete")[1].performClick()
        assertEquals(task, deleted)
    }

    @Test
    fun confirmationSurvivesStateRestoration() {
        val restorer = StateRestorationTester(compose)
        restorer.setContent {
            FoundationTheme {
                TasksScreen(
                    TasksUiState(tasks = listOf(Task(7, "Saved", false)), loading = false),
                    {},
                    {},
                    { _, _ -> },
                    {},
                    {},
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithText("Delete").performClick()
        restorer.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Delete “Saved”?").assertIsDisplayed()
    }

    @Test
    fun rendersAtPhoneTabletAndLargeFontConfigurations() {
        var width by mutableStateOf(400)
        var scale by mutableStateOf(1f)
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, 800.dp))
            ) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(scale)) {
                    FoundationTheme {
                        TasksScreen(
                            TasksUiState(loading = false),
                            {},
                            {},
                            { _, _ -> },
                            {},
                            {},
                            {},
                            {},
                        )
                    }
                }
            }
        }
        for ((size, font) in listOf(400 to 1f, 900 to 1f, 400 to 1.5f)) {
            compose.runOnIdle {
                width = size
                scale = font
            }
            compose.onNodeWithText("New task").assertIsDisplayed()
            compose.onNodeWithText("About").assertIsDisplayed()
        }
    }
}
