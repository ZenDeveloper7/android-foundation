package com.zen.foundation.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.zen.foundation.feature.notes.navigation.notesEntries
import com.zen.foundation.feature.settings.navigation.settingsEntries
import com.zen.foundation.navigation.TopLevelDestination
import com.zen.foundation.navigation.currentTopLevelDestination
import com.zen.foundation.navigation.isOnTopLevelDestination
import com.zen.foundation.navigation.navigateBack
import com.zen.foundation.navigation.navigateToTopLevel
import com.zen.foundation.navigation.openNoteEditor

/**
 * App shell: an adaptive navigation bar/rail around a Navigation 3 back stack.
 * The back stack is saved, so it survives configuration changes and process death.
 */
@Composable
fun FoundationApp(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(TopLevelDestination.entries.first().key)
    val currentDestination = backStack.currentTopLevelDestination()
    val layoutType =
        if (backStack.isOnTopLevelDestination()) {
            NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
        } else {
            NavigationSuiteType.None
        }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = destination == currentDestination,
                    onClick = { backStack.navigateToTopLevel(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(stringResource(destination.labelRes)) }
                )
            }
        },
        modifier = modifier,
        layoutType = layoutType,
        state = rememberNavigationSuiteScaffoldState()
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.navigateBack() },
            entryDecorators =
                listOf(
                    // Saves each screen's rememberSaveable state while it is on the back stack.
                    rememberSaveableStateHolderNavEntryDecorator(),
                    // Gives each screen its own ViewModels, cleared when the screen is popped.
                    rememberViewModelStoreNavEntryDecorator()
                ),
            entryProvider =
                entryProvider {
                    notesEntries(
                        onOpenEditor = { noteId -> backStack.openNoteEditor(noteId) },
                        onBack = { backStack.navigateBack() }
                    )
                    settingsEntries()
                }
        )
    }
}
