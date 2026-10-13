package com.zen.foundation.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zen.foundation.core.common.suspendRunCatching
import com.zen.foundation.core.data.repository.UserDataRepository
import com.zen.foundation.core.model.ThemeMode
import com.zen.foundation.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel
@Inject
constructor(private val userDataRepository: UserDataRepository) :
    ViewModel() {
    private val writeFailed = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> =
        combine(userDataRepository.userSettings, writeFailed) { settings, failed ->
            SettingsUiState.Success(settings = settings, writeFailed = failed)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SettingsUiState.Loading
        )

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            suspendRunCatching { userDataRepository.setThemeMode(themeMode) }
                .onFailure { writeFailed.value = true }
        }
    }

    fun setUseDynamicColor(useDynamicColor: Boolean) {
        viewModelScope.launch {
            suspendRunCatching { userDataRepository.setUseDynamicColor(useDynamicColor) }
                .onFailure { writeFailed.value = true }
        }
    }

    /** Acknowledges a shown write error so it is not shown again. */
    fun consumeWriteError() {
        writeFailed.value = false
    }
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(val settings: UserSettings, val writeFailed: Boolean = false) :
        SettingsUiState
}
