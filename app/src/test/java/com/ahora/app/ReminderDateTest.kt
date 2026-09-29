package com.ahora.app

import com.ahora.app.ui.components.reminderAtLocalDay
import com.ahora.app.ui.components.reminderInstantMillis
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.TimeZone

/**
 * Regresión de C1 (auditoría ETAPA 1): el DatePicker de Material3 devuelve la
 * fecha como medianoche UTC. El código anterior la cargaba tal cual en un
 * Calendar local, así que en zonas UTC−x (p. ej. America/Havana, UTC−4) el
 * recordatorio se programaba el DÍA ANTERIOR al elegido.
 *
 * Todas las pruebas usan instantes fijos (nada de "ahora"), así que no son
 * frágiles cerca de medianoche UTC.
 */
class ReminderDateTest {

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

    /** Medianoche UTC del 30-sep-2026, tal como la devuelve el DatePicker. */
    private fun pickerMillis(): Long = Instant.parse("2026-09-30T00:00:00Z").toEpochMilli()

    private fun expectedHavanaMillis(): Long =
        ZonedDateTime.of(2026, 9, 30, 9, 30, 0, 0, havana).toInstant().toEpochMilli()

    @Test
    fun `el recordatorio cae el mismo dia elegido en America_Havana`() {
        val at = reminderInstantMillis(pickerMillis(), 9, 30, havana)
        assertEquals(expectedHavanaMillis(), at)
    }

    @Test
    fun `usa la zona del sistema por defecto`() {
        // Cubre la ruta real del diálogo, que no pasa zona explícita.
        val at = reminderInstantMillis(pickerMillis(), 9, 30)
        assertEquals(expectedHavanaMillis(), at)
    }

    @Test
    fun `el algoritmo anterior programaba el dia anterior en Cuba`() {
        // Documenta el bug: esto es exactamente lo que hacía el código viejo.
        val buggy = Calendar.getInstance().apply {
            timeInMillis = pickerMillis()
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val fixed = reminderInstantMillis(pickerMillis(), 9, 30, havana)
        val buggyDay = ZonedDateTime.ofInstant(Instant.ofEpochMilli(buggy), havana).toLocalDate()
        val fixedDay = ZonedDateTime.ofInstant(Instant.ofEpochMilli(fixed), havana).toLocalDate()
        assertEquals(29, buggyDay.dayOfMonth) // el bug: caía el 29
        assertEquals(30, fixedDay.dayOfMonth) // el fix: cae el 30
        assertNotEquals(buggy, fixed)
    }

    @Test
    fun `tambien funciona en zona UTC positiva`() {
        val tokyo = ZoneId.of("Asia/Tokyo") // UTC+9
        val at = reminderInstantMillis(pickerMillis(), 9, 30, tokyo)
        val expected = ZonedDateTime.of(2026, 9, 30, 9, 30, 0, 0, tokyo)
            .toInstant().toEpochMilli()
        assertEquals(expected, at)
    }

    // --- reminderAtLocalDay (atajos Hoy/Mañana del diálogo de recordatorio) ---

    /** "ahora" fijo: 29-sep-2026 10:00 en La Habana. */
    private fun fixedNow(): Long =
        ZonedDateTime.of(2026, 9, 29, 10, 0, 0, 0, havana).toInstant().toEpochMilli()

    @Test
    fun `el atajo de hoy combina el dia local con la hora elegida`() {
        val at = reminderAtLocalDay(0, 14, 30, havana, fixedNow())
        val expected = ZonedDateTime.of(2026, 9, 29, 14, 30, 0, 0, havana)
            .toInstant().toEpochMilli()
        assertEquals(expected, at)
    }

    @Test
    fun `el atajo de manana cae al dia siguiente con la hora elegida`() {
        val at = reminderAtLocalDay(1, 8, 0, havana, fixedNow())
        val expected = ZonedDateTime.of(2026, 9, 30, 8, 0, 0, 0, havana)
            .toInstant().toEpochMilli()
        assertEquals(expected, at)
    }

    @Test
    fun `el atajo respeta la medianoche como inicio del dia`() {
        // A las 00:05 la hora 14:30 debe caer el mismo día, no el anterior.
        val midnight = ZonedDateTime.of(2026, 9, 29, 0, 5, 0, 0, havana)
            .toInstant().toEpochMilli()
        val at = reminderAtLocalDay(0, 14, 30, havana, midnight)
        val day = ZonedDateTime.ofInstant(Instant.ofEpochMilli(at), havana).toLocalDate()
        assertEquals(29, day.dayOfMonth)
    }
}
