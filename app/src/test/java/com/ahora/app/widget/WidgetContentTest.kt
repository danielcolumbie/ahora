package com.ahora.app.widget

import com.ahora.app.data.Task
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone

/**
 * Pruebas de la selección de tareas del widget (ETAPA 12).
 *
 * Zona fija (America/Havana) e instantes fijos: nada depende de "ahora".
 */
class WidgetContentTest {

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

    /** Instante fijo: 29 de septiembre de 2026, 10:00 en La Habana. */
    private val now: Long
        get() = ZonedDateTime.of(2026, 9, 29, 10, 0, 0, 0, havana)
            .toInstant().toEpochMilli()

    private fun dayStart(month: Int, day: Int): Long =
        LocalDate.of(2026, month, day).atStartOfDay(havana).toInstant().toEpochMilli()

    private fun task(id: Long, title: String, dueAt: Long? = null, priority: Int = 0) =
        Task(id = id, title = title, dueAt = dueAt, priority = priority)

    @Test
    fun `las de hoy y vencidas van primero, estables`() {
        val pending = listOf(
            task(1, "futura", dayStart(10, 5)),
            task(2, "hoy", dayStart(9, 29)),
            task(3, "sin fecha"),
            task(4, "vencida", dayStart(9, 27))
        )
        val ids = selectWidgetItems(pending, now).map { it.id }
        assertEquals(listOf(2L, 4L, 1L, 3L), ids)
    }

    @Test
    fun `vencer hoy cuenta como de hoy pero no como vencida`() {
        val items = selectWidgetItems(listOf(task(1, "hoy", dayStart(9, 29))), now)
        assertEquals("Hoy", items[0].dueLabel)
        assertFalse(items[0].overdue)
    }

    @Test
    fun `vencida se marca overdue con su etiqueta`() {
        val items = selectWidgetItems(listOf(task(1, "vieja", dayStart(9, 27))), now)
        // Etiqueta tipo "27 sep" (el mes depende del locale del sistema).
        assertTrue(items[0].dueLabel!!.startsWith("27 "))
        assertTrue(items[0].overdue)
    }

    @Test
    fun `sin fecha no lleva etiqueta ni overdue`() {
        val items = selectWidgetItems(listOf(task(1, "libre")), now)
        assertNull(items[0].dueLabel)
        assertFalse(items[0].overdue)
    }

    @Test
    fun `se corta en maxItems`() {
        val pending = (1L..20L).map { task(it, "t$it") }
        assertEquals(7, selectWidgetItems(pending, now).size)
        assertEquals(3, selectWidgetItems(pending, now, maxItems = 3).size)
    }

    @Test
    fun `el nivel de prioridad se conserva`() {
        val items = selectWidgetItems(listOf(task(1, "alta", priority = 3)), now)
        assertEquals(3, items[0].priorityLevel)
    }

    @Test
    fun `lista vacia da lista vacia`() {
        assertTrue(selectWidgetItems(emptyList(), now).isEmpty())
    }

    @Test
    fun `la fecha del encabezado no es vacia`() {
        assertTrue(formatWidgetDate(now).isNotBlank())
    }
}
