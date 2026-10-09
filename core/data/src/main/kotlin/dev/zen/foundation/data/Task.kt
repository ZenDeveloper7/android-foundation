package dev.zen.foundation.data

data class Task(val id: Long, val title: String, val completed: Boolean)

/** A UI-independent contract. Writes are main-safe; observation reflects persisted data. */
interface TasksRepository {
    fun observeTasks(): kotlinx.coroutines.flow.Flow<List<Task>>

    suspend fun add(title: String)

    suspend fun setCompleted(id: Long, completed: Boolean)

    suspend fun delete(id: Long)
}

internal fun normalizedTitle(title: String): String {
    val normalized = title.trim()
    require(normalized.isNotEmpty() && normalized.length <= 200) {
        "Task title must contain 1 to 200 characters."
    }
    return normalized
}
