package com.zen.foundation.feature.settings

import com.google.common.truth.Truth.assertThat
import com.zen.foundation.core.data.repository.UserDataRepository
import com.zen.foundation.core.model.ThemeMode
import com.zen.foundation.core.model.UserSettings
import com.zen.foundation.core.testing.repository.FakeUserDataRepository
import com.zen.foundation.core.testing.util.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun changes_arePersistedAndExposed() = runTest {
        // Create ViewModels inside the test, after MainDispatcherRule replaces Dispatchers.Main.
        val viewModel = SettingsViewModel(FakeUserDataRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setThemeMode(ThemeMode.Dark)
        viewModel.setUseDynamicColor(false)

        assertThat(viewModel.uiState.value)
            .isEqualTo(
                SettingsUiState.Success(UserSettings(ThemeMode.Dark, useDynamicColor = false))
            )
    }

    @Test
    fun failedWrite_exposesErrorWithoutCrashing_andConsumeClearsIt() = runTest {
        val viewModel = SettingsViewModel(FailingUserDataRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setThemeMode(ThemeMode.Dark)

        assertThat(viewModel.uiState.value)
            .isEqualTo(SettingsUiState.Success(UserSettings(), writeFailed = true))

        viewModel.consumeWriteError()

        assertThat(viewModel.uiState.value)
            .isEqualTo(SettingsUiState.Success(UserSettings(), writeFailed = false))
    }

    /** Every write fails the way a broken DataStore would. */
    private class FailingUserDataRepository : UserDataRepository {
        override val userSettings: Flow<UserSettings> = MutableStateFlow(UserSettings())

        override suspend fun setThemeMode(themeMode: ThemeMode): Unit =
            throw IOException("disk full")

        override suspend fun setUseDynamicColor(useDynamicColor: Boolean): Unit =
            throw IOException("disk full")
    }
}
