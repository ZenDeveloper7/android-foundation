package com.zen.foundation.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zen.foundation.core.model.Note
import java.time.Instant

@Entity(
    tableName = "notes",
    indices = [Index(value = ["updated_at"])]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMillis: Long
)

fun NoteEntity.asExternalModel(): Note = Note(
    id = id,
    title = title,
    body = body,
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
)
