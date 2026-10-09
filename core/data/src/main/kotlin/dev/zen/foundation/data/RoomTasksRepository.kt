package dev.zen.foundation.data

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomTasksRepository @Inject constructor(private val dao: TasksDao) :
    TasksRepository {
    override fun observeTasks(): Flow<List<Task>> =
        dao.observeTasks().map { tasks -> tasks.map { Task(it.id, it.title, it.completed) } }

    override suspend fun add(title: String) {
        dao.insert(TaskEntity(title = normalizedTitle(title)))
    }

    override suspend fun setCompleted(id: Long, completed: Boolean) {
        dao.setCompleted(id, completed)
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }
}
