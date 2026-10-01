package com.ahora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del respaldo local JSON (ETAPA 5). Usan el artefacto real
 * `org.json:json` (testImplementation), porque el android.jar de la JVM
 * trae stubs.
 */
class TaskBackupTest {

    private fun sample() = listOf(
        Task(
            id = 1, title = "Comprar pan \"integral\" 🍞",
            createdAt = 1_700_000_000_000, reminderAt = 1_700_000_360_000,
            isDone = false, doneAt = null, recurrence = null
        ),
        Task(
            id = 2, title = "Llamar a mamá\n(importante)",
            createdAt = 1_700_000_010_000, reminderAt = null,
            isDone = true, doneAt = 1_700_000_020_000, recurrence = null
        )
    )

    @Test fun `round trip conserva todos los campos`() {
        val original = sample()
        val parsed = TaskBackup.tasksFromJson(TaskBackup.tasksToJson(original))
        assertEquals(0, parsed.skipped)
        assertEquals(original, parsed.tasks)
    }

    @Test fun `round trip con lista vacia`() {
        val parsed = TaskBackup.tasksFromJson(TaskBackup.tasksToJson(emptyList()))
        assertTrue(parsed.tasks.isEmpty())
        assertEquals(0, parsed.skipped)
    }

    @Test fun `nulos se conservan como nulos`() {
        val parsed = TaskBackup.tasksFromJson(
            TaskBackup.tasksToJson(listOf(Task(id = 5, title = "x")))
        )
        val task = parsed.tasks.single()
        assertNull(task.reminderAt)
        assertNull(task.doneAt)
        assertNull(task.recurrence)
        assertEquals(0, task.priority)
        assertNull(task.dueAt)
    }

    @Test fun `round trip conserva prioridad y fecha limite`() {
        val original = listOf(
            Task(id = 1, title = "Urgente", priority = 3, dueAt = 1_700_086_400_000),
            Task(id = 2, title = "Normal", priority = 1, dueAt = null)
        )
        val parsed = TaskBackup.tasksFromJson(TaskBackup.tasksToJson(original))
        assertEquals(0, parsed.skipped)
        assertEquals(original, parsed.tasks)
    }

    @Test fun `respaldo viejo sin prioridad ni fecha se importa con valores por defecto`() {
        // Respaldo hecho con la 1.8.0: no trae "priority" ni "dueAt".
        val old = """{"format":"ahora-backup","version":1,"exportedAt":1,
            "tasks":[{"id":1,"title":"Vieja","createdAt":1700000000000,
            "reminderAt":null,"isDone":false,"doneAt":null,"recurrence":null}]}"""
        val parsed = TaskBackup.tasksFromJson(old)
        assertEquals(0, parsed.skipped)
        val task = parsed.tasks.single()
        assertEquals(0, task.priority)
        assertNull(task.dueAt)
    }

    @Test(expected = TaskBackup.BackupFormatException::class)
    fun `json malformado lanza BackupFormatException`() {
        TaskBackup.tasksFromJson("{esto no es json")
    }

    @Test(expected = TaskBackup.BackupFormatException::class)
    fun `formato ajeno lanza BackupFormatException`() {
        TaskBackup.tasksFromJson("""{"format":"otro","version":1,"tasks":[]}""")
    }

    @Test(expected = TaskBackup.BackupFormatException::class)
    fun `version no soportada lanza BackupFormatException`() {
        TaskBackup.tasksFromJson("""{"format":"ahora-backup","version":99,"tasks":[]}""")
    }

    @Test(expected = TaskBackup.BackupFormatException::class)
    fun `sin arreglo de tareas lanza BackupFormatException`() {
        TaskBackup.tasksFromJson("""{"format":"ahora-backup","version":1}""")
    }

    @Test fun `tarea con titulo vacio se omite y se cuenta`() {
        val json = """{"format":"ahora-backup","version":1,"tasks":[
            {"id":1,"title":"válida","createdAt":1000,"isDone":false},
            {"id":2,"title":"   ","createdAt":1000,"isDone":false},
            {"id":3}
        ]}"""
        val parsed = TaskBackup.tasksFromJson(json)
        assertEquals(1, parsed.tasks.size)
        assertEquals("válida", parsed.tasks.single().title)
        assertEquals(2, parsed.skipped)
    }

    @Test fun `exportedAt queda registrado en el json`() {
        val json = TaskBackup.tasksToJson(emptyList(), exportedAt = 1_234_567_890_000)
        assertTrue(json.contains("1234567890000"))
        assertTrue(json.contains("\"format\":\"ahora-backup\""))
        assertTrue(json.contains("\"version\":2"))
    }

    @Test fun `respaldo v2 incluye etiquetas y asignaciones`() {
        val tasks = listOf(
            Task(id = 1, title = "A", createdAt = 1000),
            Task(id = 2, title = "B", createdAt = 1000)
        )
        val casa = Tag(id = 1, name = "Casa", colorIndex = 0)
        val trabajo = Tag(id = 2, name = "Trabajo", colorIndex = 5)
        val json = TaskBackup.tasksToJson(
            tasks,
            listOf(casa, trabajo),
            mapOf(1L to listOf(casa, trabajo)),
            exportedAt = 1
        )
        val parsed = TaskBackup.tasksFromJson(json)
        // Los ids no se persisten (se regeneran al importar): se comparan
        // nombre y color.
        assertEquals(listOf("Casa", "Trabajo"), parsed.tags.map { it.name })
        assertEquals(listOf(0, 5), parsed.tags.map { it.colorIndex })
        assertEquals(listOf("Casa", "Trabajo"), parsed.tagAssignments[1])
        assertNull(parsed.tagAssignments[2])
    }

    @Test fun `respaldo v2 sin etiquetas se lee igual que v1`() {
        val json = TaskBackup.tasksToJson(
            listOf(Task(id = 1, title = "A", createdAt = 1000)),
            exportedAt = 1
        )
        val parsed = TaskBackup.tasksFromJson(json)
        assertEquals(1, parsed.tasks.size)
        assertTrue(parsed.tags.isEmpty())
        assertTrue(parsed.tagAssignments.isEmpty())
    }

    @Test fun `etiquetas duplicadas o sin nombre se limpian al importar`() {
        val json = """{"format":"ahora-backup","version":2,"exportedAt":1,
            "tags":[
                {"name":"Casa","colorIndex":0},
                {"name":"casa","colorIndex":3},
                {"name":"   ","colorIndex":1},
                {"name":"Trabajo","colorIndex":99}
            ],
            "tasks":[{"id":1,"title":"A","createdAt":1000,"tags":["Casa","Inexistente"]}]}"""
        val parsed = TaskBackup.tasksFromJson(json)
        // "casa" duplica a "Casa" (insensible a mayúsculas) y la vacía se
        // descarta; el colorIndex fuera de rango cae al primero.
        assertEquals(listOf("Casa", "Trabajo"), parsed.tags.map { it.name })
        assertEquals(listOf(0, 0), parsed.tags.map { it.colorIndex })
        assertEquals(listOf("Casa", "Inexistente"), parsed.tagAssignments[1])
    }
}
