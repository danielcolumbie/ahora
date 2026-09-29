package com.ahora.app.domain

import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del lenguaje natural (ETAPA 13).
 *
 * El parser es puro: "ahora" es fijo (martes 29 de septiembre de 2026,
 * 10:00) y la zona es America/Havana, así que nada depende del teléfono
 * que corra los tests.
 */
class NaturalLanguageParserTest {

    private companion object {
        val HAVANA: ZoneId = ZoneId.of("America/Havana")

        /** Instante en ms para una fecha/hora local en La Habana. */
        fun at(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
            ZonedDateTime.of(year, month, day, hour, minute, 0, 0, HAVANA)
                .toInstant().toEpochMilli()

        /** Martes 29 de septiembre de 2026, 10:00 (hora de Cuba). */
        val NOW: Long = at(2026, 9, 29, 10, 0)

        fun date(year: Int, month: Int, day: Int): LocalDate =
            LocalDate.of(year, month, day)

        fun parse(text: String): NaturalLanguageResult =
            parseNaturalLanguage(text, NOW, HAVANA)

        fun assertDefaults(result: NaturalLanguageResult) {
            assertNull(result.dueDate)
            assertNull(result.reminderAt)
            assertEquals(TaskPriority.NONE, result.priority)
            assertEquals(TaskRecurrence.NONE, result.recurrence)
        }
    }

    // ------------------------------------------------------- fechas relativas

    @Test
    fun `manana extrae el titulo y la fecha`() {
        val r = parse("pagar la luz mañana")

        assertEquals("pagar la luz", r.title)
        assertEquals(date(2026, 9, 30), r.dueDate)
        assertDefaults(r.copy(dueDate = null))
    }

    @Test
    fun `hoy es la fecha de hoy`() {
        val r = parse("comprar pan hoy")

        assertEquals("comprar pan", r.title)
        assertEquals(date(2026, 9, 29), r.dueDate)
    }

    @Test
    fun `pasado manana son dos dias`() {
        val r = parse("llamar pasado mañana")

        assertEquals("llamar", r.title)
        assertEquals(date(2026, 10, 1), r.dueDate)
    }

    @Test
    fun `para manana se limpia con la preposicion`() {
        val r = parse("comprar pan para mañana")

        assertEquals("comprar pan", r.title)
        assertEquals(date(2026, 9, 30), r.dueDate)
    }

    @Test
    fun `la fecha al inicio tambien se extrae`() {
        val r = parse("mañana pagar la luz")

        assertEquals("pagar la luz", r.title)
        assertEquals(date(2026, 9, 30), r.dueDate)
    }

    // -------------------------------------------------------- días de semana

    @Test
    fun `el viernes es el proximo viernes`() {
        val r = parse("llamar a Gaby el viernes")

        assertEquals("llamar a Gaby", r.title)
        assertEquals(date(2026, 10, 2), r.dueDate)
    }

    @Test
    fun `este lunes es el lunes que viene`() {
        val r = parse("reunión este lunes")

        assertEquals("reunión", r.title)
        assertEquals(date(2026, 10, 5), r.dueDate)
    }

    @Test
    fun `el proximo martes un martes es dentro de 7 dias`() {
        val r = parse("fiesta el próximo martes")

        assertEquals("fiesta", r.title)
        assertEquals(date(2026, 10, 6), r.dueDate)
    }

    @Test
    fun `el martes un martes es hoy`() {
        val r = parse("cita el martes")

        assertEquals("cita", r.title)
        assertEquals(date(2026, 9, 29), r.dueDate)
    }

    @Test
    fun `los dias sin tilde tambien valen`() {
        val r = parse("pagar el miercoles")

        assertEquals("pagar", r.title)
        assertEquals(date(2026, 9, 30), r.dueDate)
    }

    @Test
    fun `el proximo viernes sin ser viernes es el que viene`() {
        val r = parse("cobrar el próximo viernes")

        assertEquals(date(2026, 10, 2), r.dueDate)
    }

    // ------------------------------------------------------ fechas explícitas

    @Test
    fun `dia de mes en palabras`() {
        val r = parse("pago el 3 de octubre")

        assertEquals("pago", r.title)
        assertEquals(date(2026, 10, 3), r.dueDate)
    }

    @Test
    fun `fecha con slash`() {
        val r = parse("vence 15/10")

        assertEquals("vence", r.title)
        assertEquals(date(2026, 10, 15), r.dueDate)
    }

    @Test
    fun `fecha pasada este ano cae el ano que viene`() {
        val r = parse("cita 1/1")

        assertEquals("cita", r.title)
        assertEquals(date(2027, 1, 1), r.dueDate)
    }

    @Test
    fun `dia imposible no se reclama como fecha`() {
        val r = parse("informe 45 de octubre")

        assertNull(r.dueDate)
        assertEquals("informe 45 de octubre", r.title)
    }

    @Test
    fun `setiembre se entiende como septiembre`() {
        val r = parse("reunion el 20 de setiembre")

        assertEquals(date(2027, 9, 20), r.dueDate)
    }

    // ---------------------------------------------------------------- plazos

    @Test
    fun `en N dias`() {
        val r = parse("recordar en 3 días")

        assertEquals("recordar", r.title)
        assertEquals(date(2026, 10, 2), r.dueDate)
    }

    @Test
    fun `en una semana`() {
        val r = parse("viaje en una semana")

        assertEquals("viaje", r.title)
        assertEquals(date(2026, 10, 6), r.dueDate)
    }

    @Test
    fun `en N meses`() {
        val r = parse("pago en 2 meses")

        assertEquals("pago", r.title)
        assertEquals(date(2026, 11, 29), r.dueDate)
    }

    @Test
    fun `numero en palabras en el plazo`() {
        val r = parse("revisar en tres dias")

        assertEquals("revisar", r.title)
        assertEquals(date(2026, 10, 2), r.dueDate)
    }

    // --------------------------------------------------------------- prioridad

    @Test
    fun `urgente es prioridad alta`() {
        val r = parse("informe urgente")

        assertEquals("informe", r.title)
        assertEquals(TaskPriority.HIGH, r.priority)
    }

    @Test
    fun `importante es prioridad alta y combina con fecha`() {
        val r = parse("tarea importante para mañana")

        assertEquals("tarea", r.title)
        assertEquals(TaskPriority.HIGH, r.priority)
        assertEquals(date(2026, 9, 30), r.dueDate)
    }

    @Test
    fun `alta prioridad con dos puntos se limpia`() {
        val r = parse("pagar: alta prioridad")

        assertEquals("pagar", r.title)
        assertEquals(TaskPriority.HIGH, r.priority)
    }

    @Test
    fun `baja prioridad`() {
        val r = parse("revisar baja prioridad")

        assertEquals("revisar", r.title)
        assertEquals(TaskPriority.LOW, r.priority)
    }

    @Test
    fun `media prioridad`() {
        val r = parse("pendiente media prioridad")

        assertEquals("pendiente", r.title)
        assertEquals(TaskPriority.MEDIUM, r.priority)
    }

    @Test
    fun `importancia no es importante`() {
        val r = parse("la importancia de la puntualidad")

        assertEquals("la importancia de la puntualidad", r.title)
        assertEquals(TaskPriority.NONE, r.priority)
    }

    @Test
    fun `urgencia no es urgente`() {
        val r = parse("hay una urgencia")

        assertEquals(TaskPriority.NONE, r.priority)
    }

    // ------------------------------------------------------------- recurrencia

    @Test
    fun `todos los dias es diaria y vence hoy`() {
        val r = parse("regar las matas todos los días")

        assertEquals("regar las matas", r.title)
        assertEquals(TaskRecurrence.DAILY, r.recurrence)
        assertEquals(date(2026, 9, 29), r.dueDate)
    }

    @Test
    fun `cada dia es diaria`() {
        val r = parse("ir al gym cada día")

        assertEquals(TaskRecurrence.DAILY, r.recurrence)
    }

    @Test
    fun `entre semana`() {
        val r = parse("revisar correo entre semana")

        assertEquals("revisar correo", r.title)
        assertEquals(TaskRecurrence.WEEKDAYS, r.recurrence)
    }

    @Test
    fun `cada semana es semanal sin fecha`() {
        val r = parse("limpiar cada semana")

        assertEquals("limpiar", r.title)
        assertEquals(TaskRecurrence.WEEKLY, r.recurrence)
        assertNull(r.dueDate)
    }

    @Test
    fun `cada mes es mensual`() {
        val r = parse("pagar la renta cada mes")

        assertEquals(TaskRecurrence.MONTHLY, r.recurrence)
    }

    @Test
    fun `cada lunes es semanal y fija el lunes`() {
        val r = parse("reunión cada lunes")

        assertEquals("reunión", r.title)
        assertEquals(TaskRecurrence.WEEKLY, r.recurrence)
        assertEquals(date(2026, 10, 5), r.dueDate)
    }

    @Test
    fun `cada tanto no es recurrencia`() {
        val r = parse("regar cada tanto")

        assertEquals("regar cada tanto", r.title)
        assertEquals(TaskRecurrence.NONE, r.recurrence)
    }

    // ------------------------------------------------------------------ horas

    @Test
    fun `a las 3pm programa recordatorio hoy`() {
        val r = parse("llamar a las 3pm")

        assertEquals("llamar", r.title)
        assertEquals(at(2026, 9, 29, 15, 0), r.reminderAt)
        // Sin fecha explícita, la fecha límite es el día del recordatorio.
        assertEquals(date(2026, 9, 29), r.dueDate)
    }

    @Test
    fun `hora con fecha explicita`() {
        val r = parse("reunión mañana a las 9")

        assertEquals("reunión", r.title)
        assertEquals(date(2026, 9, 30), r.dueDate)
        assertEquals(at(2026, 9, 30, 9, 0), r.reminderAt)
    }

    @Test
    fun `de la noche es pm`() {
        val r = parse("cenar a las 8 de la noche")

        assertEquals(at(2026, 9, 29, 20, 0), r.reminderAt)
    }

    @Test
    fun `numero en palabras y de la tarde`() {
        val r = parse("llamar a las tres de la tarde")

        assertEquals("llamar", r.title)
        assertEquals(at(2026, 9, 29, 15, 0), r.reminderAt)
    }

    @Test
    fun `la hora pasada se corre al dia siguiente`() {
        // Son las 10:00; las 6:00 ya pasó hoy.
        val r = parse("despertar a las 6am")

        assertEquals("despertar", r.title)
        assertEquals(at(2026, 9, 30, 6, 0), r.reminderAt)
        assertEquals(date(2026, 9, 30), r.dueDate)
        assertTrue("el recordatorio nunca nace en el pasado", r.reminderAt!! > NOW)
    }

    @Test
    fun `sin marcador se elige la proxima ocurrencia de la hora`() {
        // Son las 10:00: "a las 3" es hoy a las 15:00, no a las 3:00.
        val r = parse("llamar a las 3")

        assertEquals(at(2026, 9, 29, 15, 0), r.reminderAt)
    }

    @Test
    fun `hora con minutos`() {
        val r = parse("cita a las 15:30")

        assertEquals(at(2026, 9, 29, 15, 30), r.reminderAt)
    }

    // ----------------------------------------------------------- combinaciones

    @Test
    fun `fecha y prioridad`() {
        val r = parse("llamar a Gaby el viernes urgente")

        assertEquals("llamar a Gaby", r.title)
        assertEquals(date(2026, 10, 2), r.dueDate)
        assertEquals(TaskPriority.HIGH, r.priority)
    }

    @Test
    fun `fecha hora y recurrencia todo junto`() {
        val r = parse("pagar la luz mañana a las 3pm cada mes")

        assertEquals("pagar la luz", r.title)
        assertEquals(date(2026, 9, 30), r.dueDate)
        assertEquals(at(2026, 9, 30, 15, 0), r.reminderAt)
        assertEquals(TaskRecurrence.MONTHLY, r.recurrence)
    }

    @Test
    fun `prioridad y recurrencia`() {
        val r = parse("informe importante cada semana")

        assertEquals("informe", r.title)
        assertEquals(TaskPriority.HIGH, r.priority)
        assertEquals(TaskRecurrence.WEEKLY, r.recurrence)
    }

    // ------------------------------------------------------- sin coincidencias

    @Test
    fun `texto normal no toca nada`() {
        val r = parse("comprar pan")

        assertEquals("comprar pan", r.title)
        assertDefaults(r)
    }

    @Test
    fun `texto vacio`() {
        val r = parse("")

        assertEquals("", r.title)
        assertDefaults(r)
    }

    @Test
    fun `solo lenguaje natural deja el titulo vacio`() {
        // Quien llama decide el fallback (guardar el texto tal cual).
        val r = parse("mañana")

        assertEquals("", r.title)
        assertEquals(date(2026, 9, 30), r.dueDate)
    }

    @Test
    fun `mayusculas y tildes mezcladas`() {
        val r = parse("PAGAR la Luz MAÑANA")

        assertEquals("PAGAR la Luz", r.title)
        assertEquals(date(2026, 9, 30), r.dueDate)
    }
}
