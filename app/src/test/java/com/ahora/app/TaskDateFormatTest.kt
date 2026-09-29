package com.ahora.app

import com.ahora.app.ui.components.formatReminderLabel
import com.ahora.app.ui.components.isSameDay
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.TimeZone

/**
 * Pruebas de la lógica pura de formato de fechas de recordatorios.
 *
 * Todas usan instantes FIJOS y zona fija ([America/Havana]): la versión
 * anterior usaba "ahora" del sistema y fallaba si se ejecutaba cerca de
 * la medianoche UTC (p. ej. "hoy a las 10:00" ya era mañana).
 */
class TaskDateFormatTest {

    private val havana = ZoneId.of("America/Havana")
    private var previousDefault: TimeZone? = null

    @Before
    fun setUp() {
        previousDefault = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(havana))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousDefault)
    }

    /** Instante fijo en septiembre/octubre de 2026, en La Habana. */
    private fun millis(month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, havana)
            .toInstant().toEpochMilli()

    private fun calendar(month: Int, day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply { timeInMillis = millis(month, day, hour, minute) }

    @Test
    fun `hoy se etiqueta como Hoy`() {
        val now = millis(9, 28, 10, 0)
        val at = millis(9, 28, 14, 30)
        assertEquals("Hoy · 14:30", formatReminderLabel(at, now))
    }

    @Test
    fun `manana se etiqueta como Manana`() {
        val now = millis(9, 28, 10, 0)
        val at = millis(9, 29, 9, 5)
        assertEquals("Mañana · 09:05", formatReminderLabel(at, now))
    }

    @Test
    fun `otro dia usa formato de fecha`() {
        val now = millis(9, 28, 10, 0)
        val at = millis(10, 3, 18, 45)
        val label = formatReminderLabel(at, now)
        // No debe decir Hoy ni Mañana, y debe incluir la hora.
        assertFalse(label.startsWith("Hoy"))
        assertFalse(label.startsWith("Mañana"))
        assertTrue(label.endsWith("18:45"))
    }

    @Test
    fun `isSameDay compara solo el dia`() {
        val a = calendar(9, 28, 10, 0)
        val b = calendar(9, 28, 22, 30)
        assertTrue(isSameDay(a, b))
        val nextDay = calendar(9, 29, 0, 30)
        assertFalse(isSameDay(a, nextDay))
    }

    @Test
    fun `isSameDay distingue dias aunque la diferencia sea de una hora`() {
        // 23:30 del 28 vs 00:30 del 29: solo una hora de diferencia,
        // pero son días distintos. La versión anterior con "ahora + 3h"
        // fallaba cerca de la medianoche.
        val a = calendar(9, 28, 23, 30)
        val b = calendar(9, 29, 0, 30)
        assertFalse(isSameDay(a, b))
    }
}
