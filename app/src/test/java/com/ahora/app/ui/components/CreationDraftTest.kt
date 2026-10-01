package com.ahora.app.ui.components

import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas del holder de estado del borrador de creación (bloque C).
 *
 * Lógica pura salvo los holders de Compose: "ahora" es fijo (martes 29 de
 * septiembre de 2026, 10:00) y la zona es America/Havana, así que nada
 * depende del teléfono que corra los tests. Las etiquetas de fecha usan la
 * zona por defecto, que se fija en [setUp].
 */
class CreationDraftTest {

    private companion object {
        val HAVANA: ZoneId = ZoneId.of("America/Havana")

        /** Instante en ms para una fecha/hora local en La Habana. */
        fun at(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
            ZonedDateTime.of(year, month, day, hour, minute, 0, 0, HAVANA)
                .toInstant().toEpochMilli()

        /** Martes 29 de septiembre de 2026, 10:00 (hora de Cuba). */
        val NOW: Long = at(2026, 9, 29, 10, 0)

        fun draft() = CreationDraftState(now = { NOW }, zone = HAVANA)
    }

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Havana"))
    }

    @Test
    fun `el texto se guarda y el parser alimenta los campos`() {
        val draft = draft()
        draft.applyText("Comprar pan mañana")
        assertEquals("Comprar pan mañana", draft.text)
        assertEquals(at(2026, 9, 30), draft.dueAt)
        assertEquals(TaskPriority.NONE, draft.priority)
        assertNull(draft.reminderAt)
    }

    @Test
    fun `detectar prioridad y recordatorio por lenguaje natural`() {
        val draft = draft()
        draft.applyText("Llamar al banco urgente a las 3pm")
        assertEquals(TaskPriority.HIGH, draft.priority)
        assertEquals(at(2026, 9, 29, 15, 0), draft.reminderAt)
    }

    @Test
    fun `vaciar el texto rearma las marcas manuales y limpia lo detectado`() {
        val draft = draft()
        draft.setPriorityManual(TaskPriority.LOW)
        draft.applyText("")
        assertFalse(draft.manualPriority)
        assertEquals(TaskPriority.NONE, draft.priority)
        assertNull(draft.dueAt)
        assertNull(draft.reminderAt)
        assertEquals(TaskRecurrence.NONE, draft.recurrence)
    }

    @Test
    fun `lo manual siempre gana sobre el parser`() {
        val draft = draft()
        draft.applyText("hacer algo urgente")
        assertEquals(TaskPriority.HIGH, draft.priority)
        draft.setPriorityManual(TaskPriority.LOW)
        draft.applyText("hacer algo urgente mañana")
        // La prioridad manual se respeta…
        assertEquals(TaskPriority.LOW, draft.priority)
        assertTrue(draft.manualPriority)
        // …pero el campo no tocado a mano sigue actualizándose.
        assertEquals(at(2026, 9, 30), draft.dueAt)
        assertFalse(draft.manualDueAt)
    }

    @Test
    fun `el recordatorio manual sobrevive a un texto nuevo`() {
        val draft = draft()
        draft.applyText("algo a las 3pm")
        val manual = at(2026, 9, 30, 9, 0)
        draft.setReminderAtManual(manual)
        draft.applyText("otra cosa a las 5pm")
        assertEquals(manual, draft.reminderAt)
        assertTrue(draft.manualReminder)
    }

    @Test
    fun `quitar el recordatorio a mano tambien es manual`() {
        val draft = draft()
        draft.applyText("algo a las 3pm")
        draft.setReminderAtManual(null)
        draft.applyText("otra cosa a las 5pm")
        assertNull(draft.reminderAt)
        assertTrue(draft.manualReminder)
    }

    @Test
    fun `el titulo guardado es el limpio, sin lo detectado`() {
        val draft = draft()
        draft.applyText("Comprar pan mañana urgente")
        assertEquals("Comprar pan", draft.cleanTitle())
    }

    @Test
    fun `si el texto era solo lenguaje natural se guarda tal cual`() {
        val draft = draft()
        draft.applyText("mañana")
        assertEquals("mañana", draft.cleanTitle())
    }

    @Test
    fun `consumeForSave devuelve los valores y vacia el borrador`() {
        val draft = draft()
        draft.applyText("Llamar mañana a las 3pm")
        draft.setPriorityManual(TaskPriority.MEDIUM)
        val values = draft.consumeForSave()
        assertEquals("Llamar", values.title)
        assertEquals(TaskPriority.MEDIUM, values.priority)
        assertEquals(at(2026, 9, 30), values.dueAt)
        assertEquals(at(2026, 9, 30, 15, 0), values.reminderAt)
        assertEquals(TaskRecurrence.NONE, values.recurrence)
        // Borrador vacío después de guardar.
        assertEquals("", draft.text)
        assertEquals(TaskPriority.NONE, draft.priority)
        assertNull(draft.dueAt)
        assertNull(draft.reminderAt)
        assertFalse(draft.manualPriority)
    }

    @Test
    fun `snapshot y restore devuelven el borrador a su estado`() {
        val draft = draft()
        draft.applyText("Comprar pan mañana")
        draft.setPriorityManual(TaskPriority.HIGH)
        draft.toggleTag(5L)
        val snapshot = draft.snapshot()
        draft.applyText("otra cosa pasado mañana")
        draft.setReminderAtManual(at(2026, 10, 1, 8, 0))
        draft.toggleTag(9L)
        draft.restore(snapshot)
        assertEquals(TaskPriority.HIGH, draft.priority)
        assertEquals(at(2026, 9, 30), draft.dueAt)
        assertNull(draft.reminderAt)
        assertTrue(draft.manualPriority)
        assertFalse(draft.manualReminder)
        assertEquals(setOf(5L), draft.tagIds)
    }

    @Test
    fun `el saver guarda y restaura todo el borrador`() {
        val draft = draft()
        draft.applyText("Llamar urgente mañana a las 3pm")
        draft.setRecurrenceManual(TaskRecurrence.WEEKLY)
        draft.toggleTag(5L)
        draft.toggleTag(9L)
        val restored = restoreCreationDraft(saveCreationDraft(draft))
        assertEquals(draft.text, restored.text)
        assertEquals(draft.priority, restored.priority)
        assertEquals(draft.dueAt, restored.dueAt)
        assertEquals(draft.recurrence, restored.recurrence)
        assertEquals(draft.reminderAt, restored.reminderAt)
        assertEquals(draft.manualPriority, restored.manualPriority)
        assertEquals(draft.manualRecurrence, restored.manualRecurrence)
        assertEquals(setOf(5L, 9L), restored.tagIds)
    }

    @Test
    fun `toggleTag marca y desmarca`() {
        val draft = draft()
        draft.toggleTag(5L)
        assertEquals(setOf(5L), draft.tagIds)
        draft.toggleTag(5L)
        assertTrue(draft.tagIds.isEmpty())
    }

    @Test
    fun `consumeForSave lleva las etiquetas y vacia el borrador`() {
        val draft = draft()
        draft.applyText("Comprar pan")
        draft.toggleTag(5L)
        val values = draft.consumeForSave()
        assertEquals(setOf(5L), values.tagIds)
        assertTrue(draft.tagIds.isEmpty())
    }

    @Test
    fun `los chips de feedback resumen lo entendido`() {
        val draft = draft()
        draft.applyText("Llamar urgente mañana a las 3pm todos los días")
        val chips = draft.feedbackChips()
        assertTrue(chips.contains("Alta"))
        assertTrue(chips.contains("Vence: Mañana"))
        assertTrue(chips.contains("Se pondrá para Mañana · 15:00"))
        assertTrue(chips.contains("Todos los días"))
    }

    @Test
    fun `sin nada detectado no hay chips`() {
        val draft = draft()
        draft.applyText("Comprar pan")
        assertTrue(draft.feedbackChips().isEmpty())
    }
}
