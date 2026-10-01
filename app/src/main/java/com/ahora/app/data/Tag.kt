package com.ahora.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Etiqueta (categoría) para organizar tareas.
 *
 * [colorIndex] es un índice en la paleta fija [TagPalette], no un color
 * arbitrario: la etiqueta se ve igual en modo claro y oscuro, y la paleta
 * de la app sigue controlada (un solo acento + colores de datos, como
 * pide el design system).
 */
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int = 0
)

/**
 * Combina las etiquetas con sus asignaciones en un mapa tarea → etiquetas.
 * Las asignaciones que apuntan a etiquetas inexistentes se descartan (no
 * deberían ocurrir por las claves foráneas, pero el mapa no debe romperse
 * si la BD trae algo raro). Función pura para poder probarla.
 */
fun combineTaskTags(
    tags: List<Tag>,
    refs: List<TaskTagCrossRef>
): Map<Long, List<Tag>> {
    if (tags.isEmpty() || refs.isEmpty()) return emptyMap()
    val byId = tags.associateBy { it.id }
    return refs.groupBy({ it.taskId }, { byId[it.tagId] })
        .mapValues { (_, list) -> list.filterNotNull() }
        .filterValues { it.isNotEmpty() }
}

/**
 * Filtra tareas por etiqueta. Con [tagId] null no filtra nada: la pantalla
 * "Todas" usa un único flujo para los dos estados (con y sin filtro),
 * como ya hacía la búsqueda sola. Función pura para poder probarla.
 */
fun filterTasksByTag(
    tasks: List<Task>,
    taskTags: Map<Long, List<Tag>>,
    tagId: Long?
): List<Task> =
    if (tagId == null) tasks
    else tasks.filter { task -> taskTags[task.id].orEmpty().any { it.id == tagId } }
