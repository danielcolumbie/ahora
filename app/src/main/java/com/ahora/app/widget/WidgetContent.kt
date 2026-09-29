package com.ahora.app.widget

import com.ahora.app.data.Task
import com.ahora.app.ui.components.formatDueLabel
import com.ahora.app.ui.components.isOverdue
import com.ahora.app.ui.components.startOfDayMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Contenido del widget de pantalla de inicio (ETAPA 12).
 *
 * Lógica pura (probable en JVM): qué tareas muestra el widget y cómo se
 * formatean. El widget muestra las pendientes de HOY primero (vencidas o
 * con fecha límite hoy) y luego las próximas, con el mismo orden de la
 * pantalla "Hoy" (prioridad y fecha). Sin polling: el widget se
 * refresca solo cuando cambia la base de datos.
 */

/** Máximo de filas que muestra el widget: cabe en el tamaño por defecto. */
const val WIDGET_MAX_ITEMS = 7

/** Fila del widget, ya formateada para RemoteViews. */
data class WidgetItem(
    val id: Long,
    val title: String,
    /** Etiqueta de fecha límite ("Hoy", "Mañana", "Ayer", "3 oct") o null si no hay. */
    val dueLabel: String?,
    /** true si la fecha límite ya pasó (se pinta en rojo). */
    val overdue: Boolean,
    /** Nivel de [com.ahora.app.data.TaskPriority] (0 = sin prioridad). */
    val priorityLevel: Int
)

/**
 * true si [dueAt] (inicio del día local) es hoy o ya pasó: la tarea
 * pertenece al grupo "de hoy" del widget. Vencer hoy no cuenta como
 * vencida ([isOverdue]), pero sí como "de hoy".
 */
internal fun isDueTodayOrEarlier(dueAt: Long, now: Long): Boolean =
    dueAt < startOfDayMillis(dayOffset = 1, now = now)

/**
 * Selecciona qué tareas muestra el widget a partir de las pendientes ya
 * ordenadas (el DAO las trae por prioridad y fecha). Las de hoy
 * (vencidas o con fecha hoy) van primero, estables; luego las próximas.
 * Se corta en [maxItems] para no cargar la lista del widget.
 */
internal fun selectWidgetItems(
    pending: List<Task>,
    now: Long = System.currentTimeMillis(),
    maxItems: Int = WIDGET_MAX_ITEMS
): List<WidgetItem> {
    val (today, later) = pending.partition { task ->
        task.dueAt?.let { isDueTodayOrEarlier(it, now) } ?: false
    }
    return (today + later).take(maxItems).map { task ->
        WidgetItem(
            id = task.id,
            title = task.title,
            dueLabel = task.dueAt?.let { formatDueLabel(it, now) },
            overdue = task.dueAt?.let { isOverdue(it, now) } ?: false,
            priorityLevel = task.priority
        )
    }
}

/** Fecha corta para el encabezado del widget, p. ej. "mar 29 sep". */
internal fun formatWidgetDate(now: Long = System.currentTimeMillis()): String =
    SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(now))
