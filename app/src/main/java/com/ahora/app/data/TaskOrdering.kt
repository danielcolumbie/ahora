package com.ahora.app.data

/**
 * Orden canónico de la lista de tareas, en código Kotlin puro para poder
 * probarlo en JVM (el SQL de [TaskDao] lo replica campo por campo):
 *
 * 1. Pendientes primero, completadas después.
 * 2. Mayor prioridad primero (Alta → Media → Baja → Sin prioridad).
 * 3. Fecha límite más cercana primero; sin fecha al final.
 * 4. Desempate: la creada más recientemente primero.
 */
val TaskListComparator: Comparator<Task> = compareBy<Task> { it.isDone }
    .thenByDescending { it.priority }
    .thenBy(nullsLast(), Task::dueAt)
    .thenByDescending { it.createdAt }
