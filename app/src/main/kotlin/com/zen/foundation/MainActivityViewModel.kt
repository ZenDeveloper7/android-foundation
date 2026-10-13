package com.zen.foundation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zen.foundation.core.data.repository.UserDataRepository
import com.zen.foundation.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainActivityViewModel
@Inject
constructor(userDataRepository: UserDataRepository) :
    ViewModel() {
    val uiState: StateFlow<MainActivityUiState> =
        userDataRepository.userSettings
            .map<UserSettings, MainActivityUiState>(MainActivityUiState::Success)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = MainActivityUiState.Loading
            )
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Success(val settings: UserSettings) : MainActivityUiState
}
