package com.zen.foundation

import com.google.common.truth.Truth.assertThat
import com.zen.foundation.core.model.ThemeMode
import com.zen.foundation.core.model.UserSettings
import com.zen.foundation.core.testing.repository.FakeUserDataRepository
import com.zen.foundation.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_isLoadingUntilCollected_thenEmitsRepositorySettings() = runTest {
        val settings = UserSettings(themeMode = ThemeMode.Dark, useDynamicColor = false)
        val viewModel = MainActivityViewModel(FakeUserDataRepository(settings))

        assertThat(viewModel.uiState.value).isEqualTo(MainActivityUiState.Loading)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertThat(viewModel.uiState.value).isEqualTo(MainActivityUiState.Success(settings))
    }
}
