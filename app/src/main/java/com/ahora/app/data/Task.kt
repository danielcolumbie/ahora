package com.ahora.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Modelo de datos de una tarea.
 * [recurrence] guarda el [TaskRecurrence.code], o null si no se repite
 * (ETAPA 11: al completar una tarea recurrente se marca la ocurrencia
 * como hecha y se genera la siguiente con su fecha desplazada).
 * [priority] es el nivel de [TaskPriority] (0 = sin prioridad).
 * [dueAt] es la fecha límite en milisegundos (inicio del día local) o null si no hay.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderAt: Long? = null,
    val isDone: Boolean = false,
    val doneAt: Long? = null,
    val recurrence: String? = null,
    val priority: Int = 0,
    val dueAt: Long? = null
)
