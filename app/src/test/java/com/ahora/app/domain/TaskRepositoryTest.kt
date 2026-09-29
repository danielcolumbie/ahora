package com.ahora.app.domain

import com.ahora.app.data.Task
import com.ahora.app.data.TaskDao
import com.ahora.app.notifications.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Pruebas de la lógica de negocio del repositorio.
 *
 * Usa un [TaskDao] falso en memoria y un [AlarmScheduler] falso que registra
 * llamadas: así se prueba la coordinación BD ↔ alarmas sin Android.
 * El reloj es fijo para que los tiempos sean deterministas.
 */
class TaskRepositoryTest {

    private companion object {
        const val NOW = 1_700_000_000_000L // instante fijo, no "ahora"
        const val HOUR = 3_600_000L
    }

    private class FakeTaskDao : TaskDao {
        private val tasks = MutableStateFlow<List<Task>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<Task>> = tasks

        override fun observePending(): Flow<List<Task>> =
            tasks.map { list -> list.filter { !it.isDone } }

        override suspend fun upsert(task: Task): Long {
            val id = if (task.id == 0L) nextId++ else task.id
            val saved = task.copy(id = id)
            tasks.value = tasks.value.filterNot { it.id == id } + saved
            return id
        }

        override suspend fun deleteById(id: Long) {
            tasks.value = tasks.value.filterNot { it.id == id }
        }

        override suspend fun getById(id: Long): Task? =
            tasks.value.firstOrNull { it.id == id }

        // Nota: el repositorio llama a getPendingReminders() con el default de
        // la interfaz (System.currentTimeMillis()). El falso congela el tiempo
        // en NOW — el instante del reloj de prueba — e ignora el parámetro,
        // para que "pendiente" se evalúe en el tiempo del test.
        override suspend fun getPendingReminders(now: Long): List<Task> =
            tasks.value.filter { it.reminderAt != null && !it.isDone && it.reminderAt > NOW }
    }

    private class FakeScheduler : AlarmScheduler {
        data class Call(val taskId: Long, val atMillis: Long)

        val scheduled = mutableListOf<Call>()
        val cancelled = mutableListOf<Long>()

        override fun schedule(taskId: Long, atMillis: Long) {
            scheduled += Call(taskId, atMillis)
        }

        override fun cancel(taskId: Long) {
            cancelled += taskId
        }
    }

    private lateinit var dao: FakeTaskDao
    private lateinit var scheduler: FakeScheduler
    private lateinit var repo: TaskRepository

    @Before
    fun setUp() {
        dao = FakeTaskDao()
        scheduler = FakeScheduler()
        repo = TaskRepository(dao, scheduler, clock = { NOW })
    }

    private suspend fun addTask(title: String = "Tarea"): Task {
        val id = repo.add(title)
        return dao.getById(id)!!
    }

    @Test
    fun `add guarda la tarea con el titulo limpio`() = runTest {
        val task = addTask("  Comprar pan  ")
        assertEquals("Comprar pan", task.title)
        assertFalse(task.isDone)
    }

    @Test
    fun `add rechaza titulo vacio`() = runTest {
        try {
            repo.add("   ")
            fail("debería lanzar IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
    }

    @Test
    fun `toggleDone completa la tarea y cancela su alarma`() = runTest {
        val task = addTask()
        repo.setReminder(task, NOW + HOUR)
        val withReminder = dao.getById(task.id)!!

        repo.toggleDone(withReminder)

        val done = dao.getById(task.id)!!
        assertTrue(done.isDone)
        assertNull("al completar se limpia el recordatorio", done.reminderAt)
        assertEquals(listOf(task.id), scheduler.cancelled)
    }

    @Test
    fun `toggleDone al desmarcar recupera el recordatorio futuro`() = runTest {
        val task = addTask()
        repo.setReminder(task, NOW + HOUR)
        val withReminder = dao.getById(task.id)!!
        repo.toggleDone(withReminder) // completa (cancela alarma)

        val done = dao.getById(task.id)!!
        // Simula que el recordatorio seguía guardado al desmarcar:
        val doneWithReminder = done.copy(reminderAt = NOW + HOUR)
        dao.upsert(doneWithReminder)
        scheduler.scheduled.clear()

        repo.toggleDone(doneWithReminder) // desmarca

        val pending = dao.getById(task.id)!!
        assertFalse(pending.isDone)
        assertNull(pending.doneAt)
        assertEquals(
            listOf(FakeScheduler.Call(task.id, NOW + HOUR)),
            scheduler.scheduled
        )
    }

    @Test
    fun `toggleDone al desmarcar no reprograma recordatorio vencido`() = runTest {
        val task = addTask()
        val done = task.copy(isDone = true, doneAt = NOW, reminderAt = NOW - HOUR)
        dao.upsert(done)

        repo.toggleDone(done)

        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `updateTitle cambia el titulo y rechaza vacio`() = runTest {
        val task = addTask()
        repo.updateTitle(task, "  Nuevo título ")
        assertEquals("Nuevo título", dao.getById(task.id)!!.title)

        try {
            repo.updateTitle(task, " ")
            fail("debería lanzar IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
    }

    @Test
    fun `delete elimina la tarea y cancela su alarma`() = runTest {
        val task = addTask()
        repo.setReminder(task, NOW + HOUR)

        val deleted = repo.delete(task)

        assertEquals(task.id, deleted.id)
        assertNull(dao.getById(task.id))
        assertEquals(listOf(task.id), scheduler.cancelled)
    }

    @Test
    fun `restore reinserta y reprograma recordatorio futuro`() = runTest {
        val task = addTask().copy(reminderAt = NOW + HOUR)
        dao.deleteById(task.id)

        repo.restore(task)

        assertEquals(task, dao.getById(task.id))
        assertEquals(
            listOf(FakeScheduler.Call(task.id, NOW + HOUR)),
            scheduler.scheduled
        )
    }

    @Test
    fun `restore no programa recordatorio vencido ni de tarea hecha`() = runTest {
        val expired = addTask().copy(reminderAt = NOW - HOUR)
        val done = addTask().copy(isDone = true, reminderAt = NOW + HOUR)
        dao.deleteById(expired.id)
        dao.deleteById(done.id)

        repo.restore(expired)
        repo.restore(done)

        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `setReminder guarda y programa solo en futuro`() = runTest {
        val task = addTask()

        repo.setReminder(task, NOW + 2 * HOUR)

        assertEquals(NOW + 2 * HOUR, dao.getById(task.id)!!.reminderAt)
        assertEquals(
            listOf(FakeScheduler.Call(task.id, NOW + 2 * HOUR)),
            scheduler.scheduled
        )

        try {
            repo.setReminder(task, NOW - HOUR)
            fail("debería lanzar IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
    }

    @Test
    fun `clearReminder limpia y cancela`() = runTest {
        val task = addTask()
        repo.setReminder(task, NOW + HOUR)

        repo.clearReminder(dao.getById(task.id)!!)

        assertNull(dao.getById(task.id)!!.reminderAt)
        assertEquals(listOf(task.id), scheduler.cancelled)
    }

    @Test
    fun `rescheduleAll solo reprograma recordatorios pendientes`() = runTest {
        val future = addTask("futura").copy(reminderAt = NOW + HOUR)
        val expired = addTask("vencida").copy(reminderAt = NOW - HOUR)
        val done = addTask("hecha").copy(isDone = true, reminderAt = NOW + HOUR)
        val plain = addTask("sin recordatorio")
        listOf(future, expired, done, plain).forEach { dao.upsert(it) }

        repo.rescheduleAll()

        assertEquals(
            listOf(FakeScheduler.Call(future.id, NOW + HOUR)),
            scheduler.scheduled
        )
    }
}
