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
        assertTrue(json.contains("\"version\":1"))
    }
}
