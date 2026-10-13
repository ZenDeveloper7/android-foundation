package com.zen.foundation.core.testing.repository

import com.zen.foundation.core.data.repository.UserDataRepository
import com.zen.foundation.core.model.ThemeMode
import com.zen.foundation.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeUserDataRepository(initial: UserSettings = UserSettings()) : UserDataRepository {
    private val settings = MutableStateFlow(initial)

    override val userSettings: Flow<UserSettings> = settings

    override suspend fun setThemeMode(themeMode: ThemeMode) = settings.update {
        it.copy(themeMode = themeMode)
    }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) = settings.update {
        it.copy(useDynamicColor = useDynamicColor)
    }
}
