package dev.zen.foundation.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.zen.foundation.data.Task
import dev.zen.foundation.designsystem.FoundationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    state: TasksUiState,
    onDraftChange: (String) -> Unit,
    onAdd: () -> Unit,
    onCompletedChange: (Task, Boolean) -> Unit,
    onDelete: (Task) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val pendingDelete = state.tasks.find { it.id == pendingDeleteId }
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.delete_confirmation, pendingDelete.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(pendingDelete)
                        pendingDeleteId = null
                    }
                ) {
                    Text(stringResource(R.string.delete_task))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tasks_title)) },
                actions = {
                    TextButton(onClick = onAbout) {
                        Text(stringResource(R.string.about_foundation))
                    }
                },
            )
        },
    ) { insets ->
        Box(
            modifier =
                Modifier.padding(insets).consumeWindowInsets(insets).imePadding().fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier =
                    Modifier.widthIn(max = 840.dp).fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    label = { Text(stringResource(R.string.task_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = onAdd, enabled = state.draft.isNotBlank() && !state.saving) {
                    Text(stringResource(R.string.add_task))
                }
                if (state.loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.loading))
                }
                if (state.loadFailed) {
                    Text(stringResource(R.string.load_error))
                    Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
                if (state.writeFailed) {
                    Text(stringResource(R.string.write_error))
                    TextButton(onClick = onDismissError) { Text(stringResource(R.string.dismiss)) }
                }
                if (!state.loading && !state.loadFailed && state.tasks.isEmpty()) {
                    Text(stringResource(R.string.empty_tasks))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.tasks, key = { it.id }) { task ->
                        Column(
                            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
                        ) {
                            Text(
                                task.title,
                                style =
                                    androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = task.completed,
                                    onCheckedChange = { onCompletedChange(task, it) },
                                    enabled = !state.saving,
                                    modifier =
                                        Modifier.semantics { contentDescription = task.title },
                                )
                                val deleteLabel =
                                    stringResource(R.string.delete_task_named, task.title)
                                TextButton(
                                    onClick = { pendingDeleteId = task.id },
                                    enabled = !state.saving,
                                    modifier =
                                        Modifier.semantics { contentDescription = deleteLabel },
                                ) {
                                    Text(stringResource(R.string.delete_task))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Phone", widthDp = 400, heightDp = 800, showBackground = true)
@Preview(name = "Tablet", widthDp = 900, heightDp = 700, showBackground = true)
@Preview(name = "Large text", widthDp = 400, fontScale = 1.5f, showBackground = true)
@Composable
private fun TasksPreview() {
    FoundationTheme {
        TasksScreen(
            state =
                TasksUiState(
                    tasks = listOf(Task(1, "Build a meaningful test", false)),
                    loading = false,
                ),
            onDraftChange = {},
            onAdd = {},
            onCompletedChange = { _, _ -> },
            onDelete = {},
            onRetry = {},
            onDismissError = {},
            onAbout = {},
        )
    }
}
