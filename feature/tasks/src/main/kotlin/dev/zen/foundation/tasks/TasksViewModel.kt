package dev.zen.foundation.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zen.foundation.data.Task
import dev.zen.foundation.data.TasksRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val draft: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val loadFailed: Boolean = false,
    val writeFailed: Boolean = false,
)

private data class LoadedTasks(val tasks: List<Task> = emptyList(), val failed: Boolean = false)

private data class WriteState(val saving: Boolean = false, val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel
@Inject
constructor(
    private val repository: TasksRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    private val writeState = MutableStateFlow(WriteState())
    private val draft = savedStateHandle.getStateFlow("draft", "")
    private val tasks = refresh.flatMapLatest {
        repository
            .observeTasks()
            .map { LoadedTasks(tasks = it) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(LoadedTasks(failed = true))
            }
    }

    val uiState =
        combine(tasks, draft, writeState) { loaded, title, write ->
                TasksUiState(
                    tasks = loaded.tasks,
                    draft = title,
                    loading = false,
                    saving = write.saving,
                    loadFailed = loaded.failed,
                    writeFailed = write.failed,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun updateDraft(value: String) {
        savedStateHandle["draft"] = value.take(200)
    }

    fun retryLoad() {
        refresh.update { it + 1 }
    }

    fun dismissWriteError() {
        writeState.update { it.copy(failed = false) }
    }

    fun addTask() {
        val submitted = draft.value
        if (submitted.isBlank()) return
        write {
            repository.add(submitted)
            // A late save must not erase text typed while it was in flight.
            if (draft.value == submitted) savedStateHandle["draft"] = ""
        }
    }

    fun setCompleted(task: Task, completed: Boolean) {
        write { repository.setCompleted(task.id, completed) }
    }

    fun delete(task: Task) {
        write { repository.delete(task.id) }
    }

    private fun write(block: suspend () -> Unit) {
        if (writeState.value.saving) return
        writeState.value = WriteState(saving = true)
        viewModelScope.launch {
            try {
                block()
                writeState.value = WriteState()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                writeState.value = WriteState(failed = true)
            } finally {
                writeState.update { it.copy(saving = false) }
            }
        }
    }
}
