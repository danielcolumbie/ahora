package com.ahora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * Pruebas del límite de la pantalla «Hoy» ([startOfTomorrowMillis]).
 * Instantes fijos en zona fija (America/Havana): nada depende del reloj
 * de la máquina que corre los tests.
 */
class DayBoundsTest {

    private val havana = ZoneId.of("America/Havana")

    /** 2026-09-29 15:00 en La Habana. */
    private val now = java.time.ZonedDateTime
        .of(2026, 9, 29, 15, 0, 0, 0, havana)
        .toInstant().toEpochMilli()

    @Test
    fun `startOfTomorrowMillis es el inicio del dia siguiente local`() {
        val expected = java.time.ZonedDateTime
            .of(2026, 9, 30, 0, 0, 0, 0, havana)
            .toInstant().toEpochMilli()
        assertEquals(expected, startOfTomorrowMillis(now, havana))
    }

    @Test
    fun `una tarea que vence hoy queda dentro del limite`() {
        val endOfToday = startOfTomorrowMillis(now, havana)
        val dueTodayStart = java.time.ZonedDateTime
            .of(2026, 9, 29, 0, 0, 0, 0, havana)
            .toInstant().toEpochMilli()
        assertTrue(dueTodayStart < endOfToday)
    }

    @Test
    fun `una tarea vencida queda dentro del limite`() {
        val endOfToday = startOfTomorrowMillis(now, havana)
        val overdue = java.time.ZonedDateTime
            .of(2026, 9, 20, 0, 0, 0, 0, havana)
            .toInstant().toEpochMilli()
        assertTrue(overdue < endOfToday)
    }

    @Test
    fun `una tarea de manana queda fuera del limite`() {
        val endOfToday = startOfTomorrowMillis(now, havana)
        val tomorrow = java.time.ZonedDateTime
            .of(2026, 9, 30, 0, 0, 0, 0, havana)
            .toInstant().toEpochMilli()
        assertTrue(tomorrow >= endOfToday)
    }

    @Test
    fun `cerca de la medianoche el limite sigue siendo manana`() {
        // 23:59:59 local: el "hoy" no se corre un día por el huso horario.
        val lateNight = java.time.ZonedDateTime
            .of(2026, 9, 29, 23, 59, 59, 0, havana)
            .toInstant().toEpochMilli()
        val expected = java.time.ZonedDateTime
            .of(2026, 9, 30, 0, 0, 0, 0, havana)
            .toInstant().toEpochMilli()
        assertEquals(expected, startOfTomorrowMillis(lateNight, havana))
    }
}
