package com.zen.foundation.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import com.zen.foundation.core.designsystem.theme.FoundationTheme
import com.zen.foundation.core.model.ThemeMode
import com.zen.foundation.core.model.UserSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedTheme_isMarkedAndRowsSelectTheirMode() {
        var selected: ThemeMode? = null
        composeRule.setContent {
            FoundationTheme(dynamicColor = false) {
                SettingsScreen(
                    uiState = SettingsUiState.Success(UserSettings(ThemeMode.Light)),
                    showDynamicColor = false,
                    onThemeModeChange = { selected = it },
                    onDynamicColorChange = {},
                    onWriteErrorConsume = {}
                )
            }
        }

        composeRule.onNodeWithText("Light").assertIsSelected()
        composeRule.onNodeWithText("Dark").performClick()

        assertThat(selected).isEqualTo(ThemeMode.Dark)
    }

    @Test
    @Config(qualifiers = "w900dp-h700dp")
    fun largeFontAndTabletWidth_rendersAllOptions() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)
            ) {
                FoundationTheme(dynamicColor = false) {
                    SettingsScreen(
                        uiState = SettingsUiState.Success(UserSettings(ThemeMode.System)),
                        showDynamicColor = true,
                        onThemeModeChange = {},
                        onDynamicColorChange = {},
                        onWriteErrorConsume = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("System default").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Light").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Dark").performScrollTo().assertIsDisplayed()
    }
}
