package com.ahora.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Recurrencia de una tarea (ETAPA 11 del plan maestro).
 *
 * Se guarda en la columna `recurrence` de [Task] como [code]; NULL (o
 * cualquier valor desconocido) significa [NONE] ("sin repetición").
 * La columna ya existía desde el schema v1 reservada para esto: no hay
 * cambios de schema ni migración en esta etapa.
 *
 * El cálculo de la siguiente ocurrencia es puro (java.time, disponible
 * desde minSdk 26) para poder probarlo en JVM sin Android.
 */
enum class TaskRecurrence(val code: String, val label: String) {
    /** Sin repetición: la tarea se archiva como hecha al completarla. */
    NONE("NONE", "No se repite"),

    /** Todos los días, a la misma hora local. */
    DAILY("DAILY", "Todos los días"),

    /** De lunes a viernes, a la misma hora local. */
    WEEKDAYS("WEEKDAYS", "Entre semana"),

    /** Cada 7 días, el mismo día de la semana. */
    WEEKLY("WEEKLY", "Cada semana"),

    /**
     * Cada mes, el mismo día del mes. Si el mes destino es más corto
     * (p. ej. del 31 de enero), cae al último día de ese mes.
     */
    MONTHLY("MONTHLY", "Cada mes");

    companion object {
        /**
         * Valores desconocidos (p. ej. de un respaldo editado a mano)
         * caen a NONE: nunca se inventa una repetición.
         */
        fun fromCode(code: String?): TaskRecurrence =
            entries.firstOrNull { it.code == code } ?: NONE
    }
}

/** Código para guardar en [Task.recurrence]: NONE se guarda como NULL. */
fun TaskRecurrence.toCode(): String? = if (this == TaskRecurrence.NONE) null else code

/**
 * Siguiente ocurrencia después de [base], avanzando por la regla de
 * recurrencia. Si [base] ya quedó en el pasado (la tarea se completó
 * tarde), sigue avanzando hasta quedar en el futuro: nunca se genera
 * una ocurrencia ya vencida.
 *
 * El avance usa la zona horaria local (inicio del día / misma hora),
 * no 24 horas fijas: así sobrevive a los cambios de horario.
 *
 * [zone] es inyectable para tests deterministas; por defecto la zona
 * local del teléfono.
 */
fun TaskRecurrence.nextAfter(
    base: Long,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    require(this != TaskRecurrence.NONE) { "NONE no tiene siguiente ocurrencia" }
    var next = advance(base, zone)
    // Tope de seguridad: con periodos fijos positivos el bucle siempre
    // termina; el tope solo protege contra un avance degenerado.
    var guard = 0
    while (next <= now && guard++ < 10_000) {
        next = advance(next, zone)
    }
    return next
}

private fun TaskRecurrence.advance(base: Long, zone: ZoneId): Long {
    var dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(base), zone)
    dateTime = when (this) {
        TaskRecurrence.DAILY -> dateTime.plusDays(1)
        TaskRecurrence.WEEKDAYS ->
            generateSequence(dateTime.plusDays(1)) { it.plusDays(1) }
                .first { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
        TaskRecurrence.WEEKLY -> dateTime.plusWeeks(1)
        TaskRecurrence.MONTHLY -> dateTime.plusMonths(1)
        TaskRecurrence.NONE -> dateTime
    }
    return dateTime.toInstant().toEpochMilli()
}
