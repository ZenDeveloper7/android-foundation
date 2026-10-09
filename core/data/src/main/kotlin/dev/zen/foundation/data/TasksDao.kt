package dev.zen.foundation.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TasksDao {
    @Query("SELECT * FROM tasks ORDER BY id DESC") fun observeTasks(): Flow<List<TaskEntity>>

    @Insert suspend fun insert(task: TaskEntity)

    @Query("UPDATE tasks SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id") suspend fun delete(id: Long)
}
