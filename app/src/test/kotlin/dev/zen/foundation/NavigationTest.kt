package dev.zen.foundation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.zen.foundation.designsystem.FoundationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun aboutAndBackReturnToTheFeature() {
        compose.setContent {
            FoundationTheme {
                FoundationApp { onAbout ->
                    Button(onClick = onAbout) { Text("Open about") }
                }
            }
        }
        compose.onNodeWithText("Open about").performClick()
        compose.onNodeWithText("Back").assertIsDisplayed().performClick()
        compose.onNodeWithText("Open about").assertIsDisplayed()
    }

    @Test
    fun backStackSurvivesSaveableStateRestoration() {
        val restorer = StateRestorationTester(compose)
        restorer.setContent {
            FoundationTheme {
                FoundationApp { onAbout ->
                    Button(onClick = onAbout) { Text("Open about") }
                }
            }
        }
        compose.onNodeWithText("Open about").performClick()
        restorer.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Back").assertIsDisplayed().performClick()
        compose.onNodeWithText("Open about").assertIsDisplayed()
    }
}
