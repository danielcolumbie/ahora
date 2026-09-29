package com.ahora.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas del orden canónico de la lista ([TaskListComparator]).
 *
 * Este comparador en Kotlin puro es lo que el SQL de [TaskDao] replica
 * (`ORDER BY isDone ASC, priority DESC, (dueAt IS NULL), dueAt ASC,
 * createdAt DESC`): si cambia la regla de negocio, cambia aquí primero
 * y el SQL la sigue.
 */
class TaskOrderingTest {

    private companion object {
        const val DAY = 86_400_000L
        const val NOW = 1_700_000_000_000L
    }

    private fun task(
        id: Long,
        priority: Int = 0,
        dueAt: Long? = null,
        isDone: Boolean = false,
        createdAt: Long = NOW
    ) = Task(
        id = id,
        title = "T$id",
        createdAt = createdAt,
        isDone = isDone,
        priority = priority,
        dueAt = dueAt
    )

    private fun orderOf(vararg tasks: Task): List<Long> =
        tasks.sortedWith(TaskListComparator).map { it.id }

    @Test
    fun `las completadas van al final aunque tengan alta prioridad`() {
        val doneHigh = task(1, priority = 3, isDone = true)
        val pendingNone = task(2, priority = 0)
        assertEquals(listOf(2L, 1L), orderOf(doneHigh, pendingNone))
    }

    @Test
    fun `mayor prioridad primero`() {
        val low = task(1, priority = 1)
        val high = task(2, priority = 3)
        val none = task(3, priority = 0)
        val medium = task(4, priority = 2)
        assertEquals(listOf(2L, 4L, 1L, 3L), orderOf(low, high, none, medium))
    }

    @Test
    fun `fecha mas cercana primero y sin fecha al final`() {
        val noDue = task(1, priority = 2)
        val later = task(2, priority = 2, dueAt = NOW + 3 * DAY)
        val sooner = task(3, priority = 2, dueAt = NOW + DAY)
        assertEquals(listOf(3L, 2L, 1L), orderOf(noDue, later, sooner))
    }

    @Test
    fun `la prioridad manda sobre la fecha`() {
        val urgentNoDue = task(1, priority = 3)
        val lowDueSoon = task(2, priority = 1, dueAt = NOW + DAY)
        assertEquals(listOf(1L, 2L), orderOf(lowDueSoon, urgentNoDue))
    }

    @Test
    fun `desempate por creacion reciente`() {
        val older = task(1, createdAt = NOW - 1000)
        val newer = task(2, createdAt = NOW)
        assertEquals(listOf(2L, 1L), orderOf(older, newer))
    }
}
