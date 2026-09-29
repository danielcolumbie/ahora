package com.ahora.app.data

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la recurrencia de tareas (ETAPA 11).
 *
 * El cálculo de la siguiente ocurrencia es puro (java.time), así que se
 * prueba en JVM con una zona fija (America/Havana): nada depende de la
 * zona del teléfono que corra los tests.
 */
class TaskRecurrenceTest {

    private companion object {
        val HAVANA: ZoneId = ZoneId.of("America/Havana")
        val NEW_YORK: ZoneId = ZoneId.of("America/New_York")

        /** Instante en milisegundos para una fecha/hora local en [zone]. */
        fun zonedMillis(
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            zone: ZoneId = HAVANA
        ): Long = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)
            .toInstant()
            .toEpochMilli()

        fun inZone(millis: Long, zone: ZoneId = HAVANA): ZonedDateTime =
            ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone)
    }

    @Test
    fun `fromCode mapea NULL y desconocidos a NONE`() {
        assertEquals(TaskRecurrence.NONE, TaskRecurrence.fromCode(null))
        assertEquals(TaskRecurrence.NONE, TaskRecurrence.fromCode(""))
        assertEquals(TaskRecurrence.NONE, TaskRecurrence.fromCode("QUINCENAL"))
        assertEquals(TaskRecurrence.DAILY, TaskRecurrence.fromCode("DAILY"))
    }

    @Test
    fun `todos los codigos hacen round-trip`() {
        TaskRecurrence.entries.forEach { recurrence ->
            assertEquals(
                recurrence,
                TaskRecurrence.fromCode(recurrence.toCode())
            )
        }
    }

    @Test
    fun `NONE se guarda como NULL en la BD`() {
        assertNull(TaskRecurrence.NONE.toCode())
        assertEquals("DAILY", TaskRecurrence.DAILY.toCode())
        assertEquals("WEEKLY", TaskRecurrence.WEEKLY.toCode())
        assertEquals("MONTHLY", TaskRecurrence.MONTHLY.toCode())
        assertEquals("WEEKDAYS", TaskRecurrence.WEEKDAYS.toCode())
    }

    @Test
    fun `diaria avanza un dia a la misma hora local`() {
        val base = zonedMillis(2026, 9, 28, 8, 30)
        val now = zonedMillis(2026, 9, 28, 9, 0)

        val next = TaskRecurrence.DAILY.nextAfter(base, now, HAVANA)
        val z = inZone(next)

        assertEquals(29, z.dayOfMonth)
        assertEquals(9, z.monthValue)
        assertEquals(8, z.hour)
        assertEquals(30, z.minute)
    }

    @Test
    fun `diaria respeta el cambio de horario (inicio del dia local)`() {
        // En Nueva York el horario de verano termina el 1 de noviembre de
        // 2026: del 1 al 2 de noviembre el día dura 25 horas. La siguiente
        // ocurrencia debe caer a medianoche local, no 24 horas exactas
        // después. (Se usa New York y no La Habana porque las reglas de
        // horario de Cuba en la tzdata pueden cambiar; las de EE. UU. son
        // estables y el contrato probado es el mismo: hora local.)
        val base = zonedMillis(2026, 11, 1, 0, 0, NEW_YORK)
        val now = zonedMillis(2026, 11, 1, 1, 0, NEW_YORK)

        val next = TaskRecurrence.DAILY.nextAfter(base, now, NEW_YORK)
        val z = inZone(next, NEW_YORK)

        assertEquals(2, z.dayOfMonth)
        assertEquals(11, z.monthValue)
        assertEquals(0, z.hour)
        assertEquals(0, z.minute)
        // 25 horas después, no 24: la prueba que fallaría con "+ 24h" fijo.
        assertEquals(25 * 3_600_000L, next - base)
    }

    @Test
    fun `entre semana salta el fin de semana`() {
        // Viernes 2 de octubre de 2026, 8:00 → lunes 5 de octubre, 8:00.
        val friday = zonedMillis(2026, 10, 2, 8, 0)
        val now = zonedMillis(2026, 10, 2, 9, 0)

        val next = TaskRecurrence.WEEKDAYS.nextAfter(friday, now, HAVANA)
        val z = inZone(next)

        assertEquals(5, z.dayOfMonth)
        assertEquals(8, z.hour)

        // Sábado → lunes también.
        val saturday = zonedMillis(2026, 10, 3, 8, 0)
        val fromSaturday = inZone(TaskRecurrence.WEEKDAYS.nextAfter(saturday, now, HAVANA))
        assertEquals(5, fromSaturday.dayOfMonth)
    }

    @Test
    fun `semanal avanza siete dias`() {
        // Lunes 28 de septiembre de 2026 → lunes 5 de octubre.
        val monday = zonedMillis(2026, 9, 28, 18, 0)
        val now = zonedMillis(2026, 9, 28, 19, 0)

        val next = TaskRecurrence.WEEKDAYS.nextAfter(monday, now, HAVANA)
        // Control: WEEKLY sobre la misma base cae 7 días después.
        val weekly = TaskRecurrence.WEEKLY.nextAfter(monday, now, HAVANA)
        val z = inZone(weekly)

        assertEquals(5, z.dayOfMonth)
        assertEquals(10, z.monthValue)
        assertEquals(18, z.hour)
        // Sanity: el avance semanal real fue de 7 días exactos.
        assertEquals(7 * 24 * 3_600_000L, weekly - monday)
        // Y WEEKDAYS desde un lunes cae al día siguiente (martes).
        assertEquals(29, inZone(next).dayOfMonth)
    }

    @Test
    fun `mensual avanza un mes y ajusta meses cortos`() {
        val jan15 = zonedMillis(2026, 1, 15, 8, 0)
        val now = zonedMillis(2026, 1, 15, 9, 0)

        val z = inZone(TaskRecurrence.MONTHLY.nextAfter(jan15, now, HAVANA))
        assertEquals(2, z.monthValue)
        assertEquals(15, z.dayOfMonth)

        // 31 de enero → 28 de febrero (2026 no es bisiesto): no se
        // inventa un 31 de febrero.
        val jan31 = zonedMillis(2026, 1, 31, 8, 0)
        val zFeb = inZone(TaskRecurrence.MONTHLY.nextAfter(jan31, now, HAVANA))
        assertEquals(2, zFeb.monthValue)
        assertEquals(28, zFeb.dayOfMonth)
    }

    @Test
    fun `si la tarea se completo tarde la siguiente ocurrencia queda en el futuro`() {
        // Tarea diaria vencida hace 5 días: la próxima debe ser mañana,
        // no hace 4 días.
        val base = zonedMillis(2026, 9, 24, 0, 0)
        val now = zonedMillis(2026, 9, 29, 15, 0)

        val next = TaskRecurrence.DAILY.nextAfter(base, now, HAVANA)

        assertTrue("la siguiente ocurrencia debe ser futura", next > now)
        val z = inZone(next)
        assertEquals(30, z.dayOfMonth)
        assertEquals(0, z.hour)
    }

    @Test
    fun `NONE no tiene siguiente ocurrencia`() {
        try {
            TaskRecurrence.NONE.nextAfter(1L, 2L, HAVANA)
            throw AssertionError("debería lanzar IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
    }
}
