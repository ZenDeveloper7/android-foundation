package dev.zen.foundation.tasks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TasksRoute(onAbout: () -> Unit, viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TasksScreen(
        state = state,
        onDraftChange = viewModel::updateDraft,
        onAdd = viewModel::addTask,
        onCompletedChange = viewModel::setCompleted,
        onDelete = viewModel::delete,
        onRetry = viewModel::retryLoad,
        onDismissError = viewModel::dismissWriteError,
        onAbout = onAbout,
    )
}
