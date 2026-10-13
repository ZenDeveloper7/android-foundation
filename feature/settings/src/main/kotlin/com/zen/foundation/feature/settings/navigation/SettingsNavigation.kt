package com.zen.foundation.feature.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.zen.foundation.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsKey : NavKey

fun EntryProviderScope<NavKey>.settingsEntries() {
    entry<SettingsKey> { SettingsRoute() }
}
