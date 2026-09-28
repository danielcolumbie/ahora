package com.ahora.app

import com.ahora.app.ui.components.formatReminderLabel
import com.ahora.app.ui.components.isSameDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Pruebas de la lógica pura de formato de fechas de recordatorios. */
class TaskDateFormatTest {

    private fun millisAt(dayOffset: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `hoy se etiqueta como Hoy`() {
        val now = millisAt(0, 10, 0)
        val at = millisAt(0, 14, 30)
        assertEquals("Hoy · 14:30", formatReminderLabel(at, now))
    }

    @Test
    fun `manana se etiqueta como Manana`() {
        val now = millisAt(0, 10, 0)
        val at = millisAt(1, 9, 5)
        assertEquals("Mañana · 09:05", formatReminderLabel(at, now))
    }

    @Test
    fun `otro dia usa formato de fecha`() {
        val now = millisAt(0, 10, 0)
        val at = millisAt(5, 18, 45)
        val label = formatReminderLabel(at, now)
        // No debe decir Hoy ni Mañana, y debe incluir la hora.
        assertFalse(label.startsWith("Hoy"))
        assertFalse(label.startsWith("Mañana"))
        assertTrue(label.endsWith("18:45"))
    }

    @Test
    fun `isSameDay compara solo el dia`() {
        val a = Calendar.getInstance()
        val b = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 3) }
        assertTrue(isSameDay(a, b))
        b.add(Calendar.DAY_OF_YEAR, 1)
        assertFalse(isSameDay(a, b))
    }
}
