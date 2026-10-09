package dev.zen.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.zen.foundation.tasks.TasksRoute
import kotlinx.serialization.Serializable

@Serializable internal data object Tasks : NavKey

@Serializable internal data object About : NavKey

@Composable
fun FoundationApp(
    tasksContent: @Composable (onAbout: () -> Unit) -> Unit = { TasksRoute(onAbout = it) }
) {
    val backStack = rememberNavBackStack(Tasks)
    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Tasks> {
                    tasksContent { if (backStack.last() != About) backStack.add(About) }
                }
                entry<About> {
                    Scaffold { padding ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(stringResource(R.string.about_description))
                            Button(onClick = { backStack.removeAt(backStack.lastIndex) }) {
                                Text(stringResource(R.string.back))
                            }
                        }
                    }
                }
            },
    )
}
