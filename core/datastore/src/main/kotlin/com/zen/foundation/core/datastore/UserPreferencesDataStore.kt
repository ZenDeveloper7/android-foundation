package com.zen.foundation.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import kotlinx.coroutines.CoroutineScope

/**
 * Creates the settings store. A corrupt file is replaced with defaults so reads and later writes
 * keep working; these settings are not sensitive, so resetting them is the right recovery. Stores
 * holding data the user cannot recreate need a different policy.
 */
fun createUserPreferencesDataStore(
    scope: CoroutineScope,
    produceFile: () -> File
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    scope = scope,
    produceFile = produceFile
)
