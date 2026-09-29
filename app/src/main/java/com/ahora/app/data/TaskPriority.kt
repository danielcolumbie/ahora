package com.ahora.app.data

/**
 * Prioridad de una tarea (ETAPA 10 del plan maestro).
 *
 * Se guarda en la BD como [level] (entero) para que el orden SQL pueda
 * usarlo directamente. [label] es el texto en español para la UI.
 * El color se resuelve en la UI (depende del tema), no aquí.
 */
enum class TaskPriority(val level: Int, val label: String) {
    NONE(0, "Sin prioridad"),
    LOW(1, "Baja"),
    MEDIUM(2, "Media"),
    HIGH(3, "Alta");

    companion object {
        /** Niveles desconocidos (p. ej. de un respaldo editado a mano) caen a NONE. */
        fun fromLevel(level: Int): TaskPriority =
            entries.firstOrNull { it.level == level } ?: NONE
    }
}
