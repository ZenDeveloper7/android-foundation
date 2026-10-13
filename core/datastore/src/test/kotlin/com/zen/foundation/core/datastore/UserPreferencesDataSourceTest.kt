package com.zen.foundation.core.datastore

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.zen.foundation.core.model.ThemeMode
import com.zen.foundation.core.model.UserSettings
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesDataSourceTest {
    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private fun settingsFile() = File(tempFolder.root, "user_settings.preferences_pb")

    private fun dataSource(file: File = settingsFile()) = UserPreferencesDataSource(
        createUserPreferencesDataStore(scope = testScope.backgroundScope, produceFile = { file })
    )

    @Test
    fun emptyStore_emitsDefaults() = testScope.runTest {
        dataSource().userSettings.test { assertThat(awaitItem()).isEqualTo(UserSettings()) }
    }

    @Test
    fun writes_areObserved() = testScope.runTest {
        val dataSource = dataSource()

        dataSource.setThemeMode(ThemeMode.Dark)
        dataSource.setUseDynamicColor(false)

        dataSource.userSettings.test {
            assertThat(
                awaitItem()
            ).isEqualTo(UserSettings(ThemeMode.Dark, useDynamicColor = false))
        }
    }

    @Test
    fun corruptFile_isReplacedAndStaysWritable() = testScope.runTest {
        val file = settingsFile().apply { writeBytes(byteArrayOf(0x0F, 0x7F, 0x01, 0x00, 0x13)) }
        val dataSource = dataSource(file)

        dataSource.userSettings.test { assertThat(awaitItem()).isEqualTo(UserSettings()) }
        // Without a corruption handler this write throws CorruptionException.
        dataSource.setThemeMode(ThemeMode.Light)

        dataSource.userSettings.test {
            assertThat(awaitItem().themeMode).isEqualTo(ThemeMode.Light)
        }
    }
}
