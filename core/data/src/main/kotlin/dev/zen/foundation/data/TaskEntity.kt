package dev.zen.foundation.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
internal data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val completed: Boolean = false,
)
