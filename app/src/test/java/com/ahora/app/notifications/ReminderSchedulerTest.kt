package com.ahora.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Pruebas de la lógica pura del programador de alarmas.
 *
 * Cada tarea necesita un PendingIntent distinto (si dos tareas compartieran
 * el request code, sus alarmas se pisarían). El cálculo del código es una
 * función pura y se prueba aquí sin AlarmManager ni Context.
 */
class ReminderSchedulerTest {

    @Test
    fun `cada tarea tiene un codigo distinto`() {
        val codes = (1L..200L).map { ReminderScheduler.requestCodeFor(it) }
        assertEquals("los códigos no deben repetirse", 200, codes.toSet().size)
    }

    @Test
    fun `el codigo es estable`() {
        assertEquals(
            ReminderScheduler.requestCodeFor(42L),
            ReminderScheduler.requestCodeFor(42L)
        )
    }

    @Test
    fun `el codigo no es el id en crudo`() {
        // El offset evita colisiones con otros PendingIntents de la app
        // que pudieran usar el id de la tarea como request code.
        assertNotEquals(7, ReminderScheduler.requestCodeFor(7L))
    }

    @Test
    fun `ids consecutivos dan codigos consecutivos`() {
        val a = ReminderScheduler.requestCodeFor(10L)
        val b = ReminderScheduler.requestCodeFor(11L)
        assertEquals(1, b - a)
    }
}
