package com.ahora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de las funciones puras de etiquetas (1.28.0): combinar
 * etiquetas con sus asignaciones y filtrar tareas por etiqueta. Sin
 * Android: se prueban aquí, no en el instrumento.
 */
class TagAssignmentTest {

    private val casa = Tag(id = 1, name = "Casa", colorIndex = 0)
    private val trabajo = Tag(id = 2, name = "Trabajo", colorIndex = 5)

    @Test fun `combineTaskTags agrupa por tarea`() {
        val refs = listOf(
            TaskTagCrossRef(taskId = 10, tagId = 1),
            TaskTagCrossRef(taskId = 10, tagId = 2),
            TaskTagCrossRef(taskId = 20, tagId = 2)
        )
        val map = combineTaskTags(listOf(casa, trabajo), refs)
        assertEquals(listOf(casa, trabajo), map[10])
        assertEquals(listOf(trabajo), map[20])
        assertEquals(2, map.size)
    }

    @Test fun `combineTaskTags con listas vacias devuelve mapa vacio`() {
        assertTrue(combineTaskTags(emptyList(), emptyList()).isEmpty())
        assertTrue(
            combineTaskTags(listOf(casa), emptyList()).isEmpty()
        )
    }

    @Test fun `combineTaskTags descarta asignaciones a etiquetas inexistentes`() {
        val refs = listOf(TaskTagCrossRef(taskId = 10, tagId = 999))
        assertTrue(combineTaskTags(listOf(casa), refs).isEmpty())
    }

    @Test fun `filterTasksByTag sin filtro devuelve todo`() {
        val tasks = listOf(
            Task(id = 1, title = "A"),
            Task(id = 2, title = "B")
        )
        val map = mapOf(1L to listOf(casa))
        assertEquals(tasks, filterTasksByTag(tasks, map, null))
    }

    @Test fun `filterTasksByTag deja solo las que llevan la etiqueta`() {
        val tasks = listOf(
            Task(id = 1, title = "A"),
            Task(id = 2, title = "B"),
            Task(id = 3, title = "C")
        )
        val map = mapOf(
            1L to listOf(casa, trabajo),
            3L to listOf(casa)
        )
        val filtered = filterTasksByTag(tasks, map, 2)
        assertEquals(listOf(tasks[0]), filtered)
    }

    @Test fun `filterTasksByTag con etiqueta sin tareas devuelve vacio`() {
        val tasks = listOf(Task(id = 1, title = "A"))
        val map = mapOf(1L to listOf(casa))
        assertTrue(filterTasksByTag(tasks, map, 2).isEmpty())
    }
}
