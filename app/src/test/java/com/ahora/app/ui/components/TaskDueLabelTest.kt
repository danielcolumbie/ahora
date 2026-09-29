package com.ahora.app.ui.components

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.TimeZone

/**
 * Pruebas de la lógica pura de fechas límite (ETAPA 10).
 *
 * Zona fija ([America/Havana]) e instantes fijos, como en
 * [com.ahora.app.TaskDateFormatTest]: nada depende de "ahora".
 */
class TaskDueLabelTest {

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

    /** Instante fijo: 28 de septiembre de 2026, 10:00 en La Habana. */
    private val now: Long
        get() = ZonedDateTime.of(2026, 9, 28, 10, 0, 0, 0, havana)
            .toInstant().toEpochMilli()

    private fun dayStart(month: Int, day: Int): Long =
        LocalDate.of(2026, month, day).atStartOfDay(havana).toInstant().toEpochMilli()

    @Test
    fun `hoy se etiqueta como Hoy`() {
        assertEquals("Hoy", formatDueLabel(dayStart(9, 28), now))
    }

    @Test
    fun `manana se etiqueta como Manana`() {
        assertEquals("Mañana", formatDueLabel(dayStart(9, 29), now))
    }

    @Test
    fun `ayer se etiqueta como Ayer`() {
        assertEquals("Ayer", formatDueLabel(dayStart(9, 27), now))
    }

    @Test
    fun `otra fecha usa dia y mes`() {
        val label = formatDueLabel(dayStart(10, 3), now)
        assertFalse(label.startsWith("Hoy"))
        assertFalse(label.startsWith("Mañana"))
        assertFalse(label.startsWith("Ayer"))
        assertTrue(label.contains("3"))
    }

    @Test
    fun `vencer hoy no es estar vencida`() {
        assertFalse(isOverdue(dayStart(9, 28), now))
    }

    @Test
    fun `ayer ya esta vencida`() {
        assertTrue(isOverdue(dayStart(9, 27), now))
    }

    @Test
    fun `manana no esta vencida`() {
        assertFalse(isOverdue(dayStart(9, 29), now))
    }

    @Test
    fun `startOfDayMillis devuelve el inicio del dia local`() {
        val start = startOfDayMillis(0, havana, now)
        val zoned = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(start), havana
        )
        assertEquals(0, zoned.hour)
        assertEquals(0, zoned.minute)
        assertEquals(LocalDate.of(2026, 9, 28), zoned.toLocalDate())
    }

    @Test
    fun `startOfDayMillis con desplazamiento llega a manana`() {
        val tomorrow = startOfDayMillis(1, havana, now)
        assertEquals(dayStart(9, 29), tomorrow)
    }

    @Test
    fun `dueAtToPickerMillis expresa el dia en UTC para el DatePicker`() {
        // 28 de septiembre en La Habana → medianoche UTC del 28.
        val picker = dueAtToPickerMillis(dayStart(9, 28), havana)
        val utcDate = java.time.Instant.ofEpochMilli(picker)
            .atZone(ZoneOffset.UTC).toLocalDate()
        assertEquals(LocalDate.of(2026, 9, 28), utcDate)
    }
}
