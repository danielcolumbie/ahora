package com.ahora.app.ui.components

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Etiqueta corta para la hora de un recordatorio: "Hoy · 14:30",
 * "Mañana · 09:00" o "3 oct · 14:30". Función pura para poder probarla.
 */
internal fun formatReminderLabel(at: Long, now: Long = System.currentTimeMillis()): String {
    val locale = Locale.getDefault()
    val time = SimpleDateFormat("HH:mm", locale).format(Date(at))
    val target = Calendar.getInstance().apply { timeInMillis = at }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val tomorrow = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, 1)
    }
    return when {
        isSameDay(target, today) -> "Hoy · $time"
        isSameDay(target, tomorrow) -> "Mañana · $time"
        else -> SimpleDateFormat("d MMM · HH:mm", locale).format(Date(at))
    }
}

internal fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

/**
 * Convierte la fecha elegida en el DatePicker de Material3 + la hora elegida
 * al instante correcto en la zona [zone].
 *
 * El DatePicker devuelve la fecha como medianoche UTC ([dateMillisUtc]);
 * cargarla tal cual en un Calendar local desplaza el día en zonas UTC−x
 * (en Cuba caía el día anterior). Aquí la fecha se interpreta en UTC —
 * como la define la API — y la hora se fija en la zona local.
 * Función pura para poder probarla.
 */
internal fun reminderInstantMillis(
    dateMillisUtc: Long,
    hour: Int,
    minute: Int,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    val date = Instant.ofEpochMilli(dateMillisUtc).atZone(ZoneOffset.UTC).toLocalDate()
    return date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
}

/**
 * true si [atMillis] es un instante estrictamente futuro respecto a [now].
 * El instante exacto de "ahora" no cuenta como futuro: programar una alarma
 * en el pasado (o en este mismo instante) nunca debe pasar en silencio.
 * Función pura para poder probarla.
 */
internal fun isFutureInstant(atMillis: Long, now: Long = System.currentTimeMillis()): Boolean =
    atMillis > now

/**
 * Etiqueta corta para la fecha límite de una tarea (ETAPA 10): "Hoy",
 * "Mañana", "Ayer" o "3 oct". La fecha límite no lleva hora: se guarda
 * como inicio del día local. Función pura para poder probarla.
 */
internal fun formatDueLabel(dueAt: Long, now: Long = System.currentTimeMillis()): String {
    val locale = Locale.getDefault()
    val target = Calendar.getInstance().apply { timeInMillis = dueAt }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val tomorrow = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, 1)
    }
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return when {
        isSameDay(target, today) -> "Hoy"
        isSameDay(target, tomorrow) -> "Mañana"
        isSameDay(target, yesterday) -> "Ayer"
        else -> SimpleDateFormat("d MMM", locale).format(Date(dueAt))
    }
}

/**
 * true si la fecha límite [dueAt] ya pasó: su día es anterior al día de
 * [now]. Vencer hoy no cuenta como vencida. Función pura para poder probarla.
 */
internal fun isOverdue(dueAt: Long, now: Long = System.currentTimeMillis()): Boolean {
    val startOfToday = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return dueAt < startOfToday.timeInMillis
}

/**
 * Inicio del día local ([dayOffset] = 0 hoy, 1 mañana…) en milisegundos.
 * Así se guarda la fecha límite: el día importa, la hora no.
 * Función pura para poder probarla.
 */
internal fun startOfDayMillis(
    dayOffset: Int = 0,
    zone: ZoneId = ZoneId.systemDefault(),
    now: Long = System.currentTimeMillis()
): Long {
    val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        .plusDays(dayOffset.toLong())
    return date.atStartOfDay(zone).toInstant().toEpochMilli()
}

/**
 * Instante para un recordatorio elegido con un atajo de día (hoy/mañana):
 * el inicio del día local más la hora y el minuto. Los atajos no pasan
 * por el DatePicker (que devuelve medianoche UTC), así que no necesitan
 * la corrección de [reminderInstantMillis].
 * Función pura para poder probarla.
 */
internal fun reminderAtLocalDay(
    dayOffset: Int,
    hour: Int,
    minute: Int,
    zone: ZoneId = ZoneId.systemDefault(),
    now: Long = System.currentTimeMillis()
): Long = startOfDayMillis(dayOffset, zone, now) + hour * 3_600_000L + minute * 60_000L

/**
 * Inicio del día local de la [fecha] dada en milisegundos.
 * Lo usa el lenguaje natural (ETAPA 13): el parser devuelve un
 * [LocalDate] y aquí se convierte a como se guarda la fecha límite.
 * Función pura para poder probarla.
 */
internal fun startOfLocalDateMillis(
    date: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

/**
 * Convierte una fecha límite (inicio del día local) a la medianoche UTC
 * que el DatePicker de Material3 espera como fecha inicial: interpreta
 * los milisegundos en UTC, así que hay que pasarle el día expresado en
 * UTC para que no muestre el día anterior en zonas UTC+x.
 * Función pura para poder probarla.
 */
internal fun dueAtToPickerMillis(
    dueAt: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    val localDate = Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
    return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
