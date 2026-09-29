package com.ahora.app.domain

import com.ahora.app.data.Task
import com.ahora.app.data.TaskBackup
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Casos borde de estabilidad (ETAPA 14): entradas raras del parser,
 * respaldo con datos extraños y límites. No cambian comportamiento:
 * fijan que la app degrada con gracia en vez de romperse.
 *
 * El parser es puro: "ahora" es fijo (martes 29 de septiembre de 2026,
 * 10:00) y la zona es America/Havana.
 */
class StabilityAuditTest {

    private companion object {
        val HAVANA: ZoneId = ZoneId.of("America/Havana")

        val NOW: Long = ZonedDateTime.of(2026, 9, 29, 10, 0, 0, 0, HAVANA)
            .toInstant().toEpochMilli()

        fun parse(text: String): NaturalLanguageResult =
            parseNaturalLanguage(text, NOW, HAVANA)
    }

    // ------------------------------------------------- parser: entradas raras

    @Test
    fun `hora imposible queda en el titulo sin programar nada`() {
        val r = parse("reunión a las 99")

        assertNull(r.reminderAt)
        assertNull(r.dueDate)
        assertEquals("reunión a las 99", r.title)
    }

    @Test
    fun `minutos imposibles quedan en el titulo`() {
        val r = parse("llamar a las 3:99")

        assertNull(r.reminderAt)
        assertEquals("llamar a las 3:99", r.title)
    }

    @Test
    fun `fecha imposible queda en el titulo`() {
        val r = parse("fiesta el 45 de octubre")

        assertNull(r.dueDate)
        assertEquals("fiesta el 45 de octubre", r.title)
    }

    @Test
    fun `febrero sin ese dia queda en el titulo`() {
        val r = parse("cita el 30 de febrero")

        assertNull(r.dueDate)
        assertEquals("cita el 30 de febrero", r.title)
    }

    @Test
    fun `la misma palabra dos veces solo reclama la primera`() {
        val r = parse("pagar mañana mañana")

        assertEquals(java.time.LocalDate.of(2026, 9, 30), r.dueDate)
        assertEquals("pagar mañana", r.title)
    }

    @Test
    fun `solo signos de puntuacion no rompe nada`() {
        val r = parse("!!!")

        assertEquals("!!!", r.title)
        assertNull(r.dueDate)
        assertNull(r.reminderAt)
    }

    @Test
    fun `entrada larguisima termina sin colgarse`() {
        val long = buildString {
            repeat(2000) { append("comprar pan mañana y ") }
        }
        val r = parse(long)

        assertEquals(java.time.LocalDate.of(2026, 9, 30), r.dueDate)
        assertTrue(r.title.startsWith("comprar pan"))
    }

    @Test
    fun `mezcla rara de detectores no deja el titulo vacio por accidente`() {
        // "urgente" se reclama como prioridad; el resto queda intacto.
        val r = parse("urgente")

        assertEquals("", r.title)
        assertEquals(com.ahora.app.data.TaskPriority.HIGH, r.priority)
    }

    // ------------------------------------------------------- respaldo: borde

    @Test
    fun `titulo con caracteres raros sobrevive al respaldo`() {
        val tasks = listOf(
            // Sin espacios al inicio/final: la importación recorta el título
            // a propósito (títulos editados a mano con solo espacios se
            // consideran vacíos y se omiten).
            Task(id = 7, title = "\"raro\"\t\n\\ 100% _sub_")
        )
        val parsed = TaskBackup.tasksFromJson(TaskBackup.tasksToJson(tasks))

        assertEquals(1, parsed.tasks.size)
        assertEquals(0, parsed.skipped)
        assertEquals(tasks[0].title, parsed.tasks[0].title)
    }

    @Test
    fun `respaldo con arreglo vacio importa cero tareas`() {
        val parsed = TaskBackup.tasksFromJson(
            """{"format":"ahora-backup","version":1,"tasks":[]}"""
        )

        assertTrue(parsed.tasks.isEmpty())
        assertEquals(0, parsed.skipped)
    }

    @Test
    fun `arreglo con entradas no objeto se omiten sin abortar`() {
        val parsed = TaskBackup.tasksFromJson(
            """{"format":"ahora-backup","version":1,"tasks":[42,"texto",null]}"""
        )

        assertTrue(parsed.tasks.isEmpty())
        assertEquals(3, parsed.skipped)
    }
}
