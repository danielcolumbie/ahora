package com.ahora.app.ui.components

import java.text.SimpleDateFormat
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
