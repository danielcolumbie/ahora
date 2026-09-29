package com.ahora.app.ui.components

import java.text.SimpleDateFormat
import java.time.Instant
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
