package com.zen.foundation.core.model

import java.time.Instant

/** A user note. Domain models are plain Kotlin: no Android, Room or serialization annotations. */
data class Note(val id: Long, val title: String, val body: String, val updatedAt: Instant)
