package com.zen.foundation.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zen.foundation.core.model.ThemeMode
import com.zen.foundation.core.model.UserSettings
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Small key-value settings. Use Room for anything relational or user-generated. */
class UserPreferencesDataSource
@Inject
constructor(private val dataStore: DataStore<Preferences>) {
    val userSettings: Flow<UserSettings> =
        dataStore.data
            .catch { error ->
                // A corrupt or unreadable file falls back to defaults instead of crashing.
                if (error is IOException) emit(emptyPreferences()) else throw error
            }.map { preferences ->
                val defaults = UserSettings()
                UserSettings(
                    themeMode =
                        preferences[Keys.themeMode]
                            ?.let { stored ->
                                ThemeMode.entries.firstOrNull { it.name == stored }
                            }
                            ?: defaults.themeMode,
                    useDynamicColor =
                        preferences[Keys.useDynamicColor] ?: defaults.useDynamicColor
                )
            }.distinctUntilChanged()

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.themeMode] = themeMode.name }
    }

    suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
        dataStore.edit { it[Keys.useDynamicColor] = useDynamicColor }
    }

    private object Keys {
        // Stored names are part of the on-disk format: never rename them.
        val themeMode = stringPreferencesKey("theme_mode")
        val useDynamicColor = booleanPreferencesKey("use_dynamic_color")
    }
}
