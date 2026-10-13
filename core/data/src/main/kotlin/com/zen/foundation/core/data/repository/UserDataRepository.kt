package com.zen.foundation.core.data.repository

import com.zen.foundation.core.datastore.UserPreferencesDataSource
import com.zen.foundation.core.model.ThemeMode
import com.zen.foundation.core.model.UserSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface UserDataRepository {
    val userSettings: Flow<UserSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setUseDynamicColor(useDynamicColor: Boolean)
}

internal class DefaultUserDataRepository
@Inject
constructor(
    private val preferences: UserPreferencesDataSource
) : UserDataRepository {
    override val userSettings: Flow<UserSettings> = preferences.userSettings

    override suspend fun setThemeMode(themeMode: ThemeMode) = preferences.setThemeMode(themeMode)

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) =
        preferences.setUseDynamicColor(useDynamicColor)
}
