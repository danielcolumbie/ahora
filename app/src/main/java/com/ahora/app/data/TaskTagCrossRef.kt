package com.ahora.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Relación muchos-a-muchos tarea ↔ etiqueta: una tarea puede tener varias
 * etiquetas y una etiqueta puede estar en varias tareas.
 *
 * Al borrar una tarea o una etiqueta, sus asignaciones desaparecen solas
 * (CASCADE): nunca quedan referencias colgadas ni hace falta limpiarlas
 * a mano.
 */
@Entity(
    tableName = "task_tags",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("tagId")]
)
data class TaskTagCrossRef(
    val taskId: Long,
    val tagId: Long
)
