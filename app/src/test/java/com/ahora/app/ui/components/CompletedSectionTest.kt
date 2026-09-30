package com.ahora.app.ui.components

import com.ahora.app.data.Task
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Lógica pura de la sección "Completadas" colapsable (ronda 2 de la
 * evolución visual, 2026-09-30): partición de la lista y textos de
 * TalkBack del encabezado.
 */
class CompletedSectionTest {

    private fun task(id: Long, done: Boolean) =
        Task(id = id, title = "Tarea $id", isDone = done)

    @Test
    fun `parte activas primero y completadas despues en orden`() {
        val tasks = listOf(
            task(1, done = true),
            task(2, done = false),
            task(3, done = true),
            task(4, done = false)
        )
        val (active, completed) = splitActiveCompleted(tasks)
        assertEquals(listOf(2L, 4L), active.map { it.id })
        assertEquals(listOf(1L, 3L), completed.map { it.id })
    }

    @Test
    fun `sin completadas la particion deja el segundo grupo vacio`() {
        val (active, completed) = splitActiveCompleted(
            listOf(task(1, done = false), task(2, done = false))
        )
        assertEquals(2, active.size)
        assertEquals(0, completed.size)
    }

    @Test
    fun `con una tarea el anuncio usa singular`() {
        assertEquals(
            "Completadas, 1 tarea",
            completedHeaderContentDescription(1)
        )
    }

    @Test
    fun `con varias tareas el anuncio usa plural`() {
        assertEquals(
            "Completadas, 3 tareas",
            completedHeaderContentDescription(3)
        )
    }

    @Test
    fun `expandida se anuncia como expandida`() {
        assertEquals("Expandida", completedHeaderStateDescription(expanded = true))
    }

    @Test
    fun `contraida se anuncia como contraida`() {
        assertEquals("Contraída", completedHeaderStateDescription(expanded = false))
    }
}
