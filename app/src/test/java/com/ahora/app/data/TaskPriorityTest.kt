package com.ahora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de [TaskPriority]: el mapeo nivel ↔ enum y los valores por
 * defecto ante datos inesperados (p. ej. un respaldo editado a mano).
 */
class TaskPriorityTest {

    @Test
    fun `fromLevel recupera cada prioridad`() {
        TaskPriority.entries.forEach { priority ->
            assertEquals(priority, TaskPriority.fromLevel(priority.level))
        }
    }

    @Test
    fun `niveles desconocidos caen a NONE`() {
        assertEquals(TaskPriority.NONE, TaskPriority.fromLevel(99))
        assertEquals(TaskPriority.NONE, TaskPriority.fromLevel(-1))
        assertEquals(TaskPriority.NONE, TaskPriority.fromLevel(Int.MAX_VALUE))
    }

    @Test
    fun `los niveles estan ordenados de menor a mayor importancia`() {
        val levels = TaskPriority.entries.map { it.level }
        assertEquals(listOf(0, 1, 2, 3), levels)
    }

    @Test
    fun `todas las etiquetas estan en espanol y no vacias`() {
        TaskPriority.entries.forEach { priority ->
            assertTrue(priority.label.isNotBlank())
        }
        assertEquals("Alta", TaskPriority.HIGH.label)
        assertEquals("Sin prioridad", TaskPriority.NONE.label)
    }
}
