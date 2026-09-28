package com.ahora.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Modelo de datos de una tarea.
 * [recurrence] queda reservado para la futura función de tareas recurrentes (V1 no lo usa).
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderAt: Long? = null,
    val isDone: Boolean = false,
    val doneAt: Long? = null,
    val recurrence: String? = null
)
