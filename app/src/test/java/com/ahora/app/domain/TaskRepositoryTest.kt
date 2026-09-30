package com.ahora.app.domain

import com.ahora.app.data.Task
import com.ahora.app.data.TaskBackup
import com.ahora.app.data.TaskDao
import com.ahora.app.data.TaskListComparator
import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import com.ahora.app.data.nextAfter
import com.ahora.app.data.startOfTomorrowMillis
import com.ahora.app.data.toCode
import com.ahora.app.notifications.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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

        /** Emula el filtro de «Hoy»: pendientes con fecha límite hoy o vencida. */
        override fun observeToday(endOfToday: Long): Flow<List<Task>> =
            tasks.map { list ->
                list.filter { !it.isDone && it.dueAt != null && it.dueAt < endOfToday }
            }

        override suspend fun getPendingForWidget(limit: Int): List<Task> =
            tasks.value.filter { !it.isDone }.take(limit)

        /**
         * Emula el LIKE de Room con `ESCAPE '\'`: el patrón llega como
         * %<texto escapado>% desde [com.ahora.app.data.buildSearchPattern].
         * Se des-escapa y se busca como subcadena (LIKE es insensible a
         * mayúsculas en ASCII), con el mismo orden que la consulta real.
         */
        override fun search(pattern: String): Flow<List<Task>> =
            tasks.map { list ->
                val needle = unescapeLike(pattern)
                list.filter { it.title.contains(needle, ignoreCase = true) }
                    // Mismo orden que el SQL de Room (ETAPA 10): replica
                    // [TaskListComparator] en vez de duplicar la lógica.
                    .sortedWith(TaskListComparator)
            }

        private fun unescapeLike(pattern: String): String {
            val inner = pattern.removePrefix("%").removeSuffix("%")
            val out = StringBuilder(inner.length)
            var i = 0
            while (i < inner.length) {
                val c = inner[i]
                if (c == '\\' && i + 1 < inner.length) {
                    out.append(inner[i + 1])
                    i += 2
                } else {
                    out.append(c)
                    i++
                }
            }
            return out.toString()
        }

        override suspend fun upsert(task: Task): Long {
            val id = if (task.id == 0L) nextId++ else task.id
            val saved = task.copy(id = id)
            tasks.value = tasks.value.filterNot { it.id == id } + saved
            return id
        }

        override suspend fun insertNextOccurrence(done: Task, next: Task): Long {
            upsert(done)
            return upsert(next)
        }

        /** Cuántas veces se llamó al lote: la importación debe usarlo una sola vez. */
        var upsertAllCalls = 0

        override suspend fun upsertAll(tasks: List<Task>) {
            upsertAllCalls++
            tasks.forEach { upsert(it) }
        }

        override suspend fun deleteById(id: Long) {
            tasks.value = tasks.value.filterNot { it.id == id }
        }

        override suspend fun getById(id: Long): Task? =
            tasks.value.firstOrNull { it.id == id }

        override suspend fun getAll(): List<Task> = tasks.value

        // Nota: el repositorio llama a getPendingReminders() con el default de
        // la interfaz (System.currentTimeMillis()). El falso congela el tiempo
        // en NOW — el instante del reloj de prueba — e ignora el parámetro,
        // para que "pendiente" se evalúe en el tiempo del test.
        override suspend fun getPendingReminders(now: Long): List<Task> =
            tasks.value.filter { it.reminderAt != null && !it.isDone && it.reminderAt > NOW }

        // Misma semántica que el EXISTS de Room: solo existencia, sin lista.
        override suspend fun hasPendingReminders(now: Long): Boolean =
            tasks.value.any { it.reminderAt != null && !it.isDone && it.reminderAt > NOW }

        override suspend fun clearExpiredReminders(now: Long) {
            tasks.value = tasks.value.map { task ->
                if (task.reminderAt != null && !task.isDone && task.reminderAt <= NOW) {
                    task.copy(reminderAt = null)
                } else {
                    task
                }
            }
        }
    }

    private class FakeScheduler : AlarmScheduler {
        data class Call(val taskId: Long, val atMillis: Long)

        val scheduled = mutableListOf<Call>()
        val cancelled = mutableListOf<Long>()
        var exactAlarmPermission = true

        override fun schedule(taskId: Long, atMillis: Long) {
            scheduled += Call(taskId, atMillis)
        }

        override fun cancel(taskId: Long) {
            cancelled += taskId
        }

        override fun hasExactAlarmPermission(): Boolean = exactAlarmPermission

        // Solo se devuelve null: nunca se invoca nada del stub de Android.
        override fun exactAlarmSettingsIntent(): android.content.Intent? = null
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
    fun `add guarda prioridad y fecha limite`() = runTest {
        val due = NOW + 24 * HOUR
        val id = repo.add("Urgente", TaskPriority.HIGH, due)
        val task = dao.getById(id)!!
        assertEquals(TaskPriority.HIGH.level, task.priority)
        assertEquals(due, task.dueAt)
    }

    @Test
    fun `add usa sin prioridad y sin fecha por defecto`() = runTest {
        val task = addTask()
        assertEquals(TaskPriority.NONE.level, task.priority)
        assertNull(task.dueAt)
    }

    @Test
    fun `add con recordatorio futuro lo guarda y programa la alarma`() = runTest {
        val at = NOW + HOUR
        val id = repo.add("Llamar", reminderAt = at)
        val task = dao.getById(id)!!

        assertEquals(at, task.reminderAt)
        assertEquals(listOf(FakeScheduler.Call(id, at)), scheduler.scheduled)
    }

    @Test
    fun `add con recordatorio pasado no programa ni guarda nada`() = runTest {
        val id = repo.add("Llamar", reminderAt = NOW - HOUR)
        val task = dao.getById(id)!!

        assertNull("el recordatorio pasado no se guarda", task.reminderAt)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `updateDetails cambia titulo prioridad y fecha y rechaza vacio`() = runTest {
        val task = addTask()
        val due = NOW + 24 * HOUR
        repo.updateDetails(task, "  Nuevo título ", TaskPriority.MEDIUM, due)
        val updated = dao.getById(task.id)!!
        assertEquals("Nuevo título", updated.title)
        assertEquals(TaskPriority.MEDIUM.level, updated.priority)
        assertEquals(due, updated.dueAt)

        try {
            repo.updateDetails(task, " ", TaskPriority.LOW, null)
            fail("debería lanzar IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
    }

    @Test
    fun `updateDetails conserva el recordatorio`() = runTest {
        val task = addTask()
        repo.setReminder(task, NOW + HOUR)
        val withReminder = dao.getById(task.id)!!

        repo.updateDetails(withReminder, "Mismo título", TaskPriority.HIGH, null)

        val updated = dao.getById(task.id)!!
        assertEquals(NOW + HOUR, updated.reminderAt)
        assertEquals(TaskPriority.HIGH.level, updated.priority)
    }

    @Test
    fun `updateDetails puede quitar la fecha limite`() = runTest {
        val id = repo.add("Con fecha", TaskPriority.LOW, NOW + HOUR)
        val task = dao.getById(id)!!

        repo.updateDetails(task, "Con fecha", TaskPriority.LOW, null)

        assertNull(dao.getById(id)!!.dueAt)
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

    @Test
    fun `pruneExpiredReminders limpia los vencidos y respeta los futuros`() = runTest {
        val expired = addTask("vencida").copy(reminderAt = NOW - HOUR)
        val future = addTask("futura").copy(reminderAt = NOW + HOUR)
        val doneExpired = addTask("hecha vencida").copy(isDone = true, reminderAt = NOW - HOUR)
        listOf(expired, future, doneExpired).forEach { dao.upsert(it) }

        repo.pruneExpiredReminders()

        assertNull(dao.getById(expired.id)!!.reminderAt)
        assertEquals(NOW + HOUR, dao.getById(future.id)!!.reminderAt)
        // Las completadas no se tocan (su recordatorio ya se limpió al completar).
        assertEquals(NOW - HOUR, dao.getById(doneExpired.id)!!.reminderAt)
    }

    @Test
    fun `hasFutureReminders refleja si hay recordatorios por sonar`() = runTest {
        assertFalse(repo.hasFutureReminders())

        val task = addTask().copy(reminderAt = NOW + HOUR)
        dao.upsert(task)
        assertTrue(repo.hasFutureReminders())

        repo.pruneExpiredReminders()
        assertTrue(repo.hasFutureReminders())

        dao.upsert(task.copy(reminderAt = NOW - HOUR))
        repo.pruneExpiredReminders()
        assertFalse(repo.hasFutureReminders())
    }

    @Test
    fun `importTasks restaura tareas y reprograma recordatorios futuros`() = runTest {
        val json = TaskBackup.tasksToJson(
            listOf(
                Task(id = 10, title = "futura", createdAt = NOW, reminderAt = NOW + HOUR),
                Task(id = 11, title = "vencida", createdAt = NOW, reminderAt = NOW - HOUR),
                Task(id = 12, title = "sin recordatorio", createdAt = NOW)
            ),
            exportedAt = NOW
        )

        val result = repo.importTasks(json)

        assertEquals(3, result.imported)
        assertEquals(0, result.skipped)
        assertEquals("futura", dao.getById(10)?.title)
        // La vencida se limpia al importar; la futura se reprograma.
        assertNull(dao.getById(11)?.reminderAt)
        assertTrue(scheduler.scheduled.any { it.taskId == 10L && it.atMillis == NOW + HOUR })
        assertFalse(scheduler.scheduled.any { it.taskId == 11L })
    }

    @Test
    fun `importTasks es idempotente y cuenta las omitidas`() = runTest {
        val json = """{"format":"ahora-backup","version":1,"exportedAt":1,"tasks":[
            {"id":20,"title":"una","createdAt":1000,"isDone":false},
            {"id":21,"title":"   ","createdAt":1000,"isDone":false}
        ]}"""

        val first = repo.importTasks(json)
        val second = repo.importTasks(json)

        assertEquals(1, first.imported)
        assertEquals(1, first.skipped)
        assertEquals(1, second.imported)
        // Sin duplicados: el id se conserva y el upsert reemplaza.
        assertEquals(1, dao.getAll().size)
        assertEquals("una", dao.getById(20)?.title)
    }

    @Test
    fun `importTasks guarda el lote en una sola llamada (una transaccion)`() = runTest {
        val json = TaskBackup.tasksToJson(
            listOf(
                Task(id = 30, title = "a", createdAt = NOW),
                Task(id = 31, title = "b", createdAt = NOW),
                Task(id = 32, title = "c", createdAt = NOW)
            ),
            exportedAt = NOW
        )
        repo.importTasks(json)
        // Una sola llamada al lote para las 3 tareas, no un upsert por tarea.
        assertEquals(1, dao.upsertAllCalls)
        assertEquals(3, dao.getAll().size)
    }

    @Test
    fun `exportTasks produce un respaldo que importTasks entiende`() = runTest {
        addTask("una").copy(reminderAt = NOW + HOUR).let { dao.upsert(it) }
        addTask("dos").let { dao.upsert(it) }

        val json = repo.exportTasks()
        // Vacía la BD simulando una reinstalación y restaura.
        dao.getAll().forEach { dao.deleteById(it.id) }
        val result = repo.importTasks(json)

        assertEquals(2, result.imported)
        assertEquals(2, dao.getAll().size)
    }

    @Test
    fun `search con consulta vacia devuelve todas`() = runTest {
        repo.add("Comprar pan")
        repo.add("Llamar al banco")

        assertEquals(2, repo.search("").first().size)
        assertEquals(2, repo.search("   ").first().size)
    }

    @Test
    fun `search filtra por titulo sin distinguir mayusculas`() = runTest {
        repo.add("Comprar PAN")
        repo.add("Llamar al banco")

        val resultados = repo.search("pan").first()
        assertEquals(1, resultados.size)
        assertEquals("Comprar PAN", resultados[0].title)
    }

    @Test
    fun `search trata los comodines como texto literal`() = runTest {
        repo.add("Oferta 100% real")
        repo.add("Revisar 1000 correos")

        val resultados = repo.search("100%").first()
        assertEquals(1, resultados.size)
        assertEquals("Oferta 100% real", resultados[0].title)
    }

    @Test
    fun `search pone las pendientes primero como observeAll`() = runTest {
        dao.upsert(Task(title = "Comprar pan", isDone = true, createdAt = NOW))
        dao.upsert(Task(title = "Comprar leche", isDone = false, createdAt = NOW - 1))

        val resultados = repo.search("comprar").first()
        assertEquals(2, resultados.size)
        assertEquals("Comprar leche", resultados[0].title)
        assertTrue(resultados[1].isDone)
    }

    // ---- ETAPA 11: recurrencia ----

    @Test
    fun `add guarda la recurrencia y NONE queda en NULL`() = runTest {
        val dailyId = repo.add("Vitaminas", recurrence = TaskRecurrence.DAILY)
        val plainId = repo.add("Una vez")

        assertEquals("DAILY", dao.getById(dailyId)!!.recurrence)
        assertNull(dao.getById(plainId)!!.recurrence)
    }

    @Test
    fun `toggleDone en tarea diaria genera la siguiente ocurrencia`() = runTest {
        val due = NOW + 24 * HOUR
        val reminder = NOW + HOUR
        val id = repo.add("Vitaminas", TaskPriority.MEDIUM, due, TaskRecurrence.DAILY)
        repo.setReminder(dao.getById(id)!!, reminder)
        val task = dao.getById(id)!!
        // Limpia el registro de la programación inicial: lo que sigue
        // debe ser solo lo que provoque completar la tarea.
        scheduler.scheduled.clear()

        repo.toggleDone(task)

        // La ocurrencia completada queda marcada como hecha, sin recordatorio.
        val done = dao.getById(id)!!
        assertTrue(done.isDone)
        assertNull(done.reminderAt)
        // Su alarma se canceló.
        assertEquals(listOf(id), scheduler.cancelled)

        // La siguiente ocurrencia: pendiente, con fecha y recordatorio
        // desplazados un día, misma prioridad y misma recurrencia.
        val all = dao.getAll()
        assertEquals(2, all.size)
        val next = all.first { it.id != id }
        assertFalse(next.isDone)
        assertNull(next.doneAt)
        assertEquals("Vitaminas", next.title)
        assertEquals(TaskPriority.MEDIUM.level, next.priority)
        assertEquals("DAILY", next.recurrence)
        assertEquals(TaskRecurrence.DAILY.nextAfter(due, NOW), next.dueAt)
        assertEquals(TaskRecurrence.DAILY.nextAfter(reminder, NOW), next.reminderAt)
        // Su recordatorio quedó programado con la alarma del sistema.
        assertEquals(
            listOf(FakeScheduler.Call(next.id, next.reminderAt!!)),
            scheduler.scheduled
        )
    }

    @Test
    fun `toggleDone en tarea recurrente sin fechas tambien regenera`() = runTest {
        val id = repo.add("Meditar", recurrence = TaskRecurrence.WEEKLY)
        val task = dao.getById(id)!!

        repo.toggleDone(task)

        assertTrue(dao.getById(id)!!.isDone)
        val next = dao.getAll().first { it.id != id }
        assertFalse(next.isDone)
        assertNull(next.dueAt)
        assertNull(next.reminderAt)
        assertEquals("WEEKLY", next.recurrence)
        // Sin recordatorio no hay nada que programar.
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `toggleDone en tarea no recurrente no crea otra ocurrencia`() = runTest {
        val task = addTask()

        repo.toggleDone(task)

        assertEquals(1, dao.getAll().size)
        assertTrue(dao.getById(task.id)!!.isDone)
    }

    @Test
    fun `toggleDone en tarea recurrente vencida desplaza al futuro`() = runTest {
        // Diaria con fecha límite de hace 3 días: al completarla tarde, la
        // siguiente no puede nacer ya vencida.
        val overdueDue = NOW - 3 * 24 * HOUR
        val id = repo.add("Atrasada", dueAt = overdueDue, recurrence = TaskRecurrence.DAILY)

        repo.toggleDone(dao.getById(id)!!)

        val next = dao.getAll().first { it.id != id }
        assertTrue("la siguiente ocurrencia debe ser futura", next.dueAt!! > NOW)
    }

    @Test
    fun `updateDetails cambia la recurrencia`() = runTest {
        val task = addTask()

        repo.updateDetails(task, task.title, TaskPriority.NONE, null, TaskRecurrence.MONTHLY)
        assertEquals("MONTHLY", dao.getById(task.id)!!.recurrence)

        repo.updateDetails(
            dao.getById(task.id)!!, task.title, TaskPriority.NONE, null, TaskRecurrence.NONE
        )
        assertNull(dao.getById(task.id)!!.recurrence)
    }

    @Test
    fun `toCode y fromCode son consistentes con lo que guarda el repositorio`() = runTest {
        TaskRecurrence.entries.forEach { recurrence ->
            val id = repo.add("t", recurrence = recurrence)
            assertEquals(
                recurrence.toCode(),
                dao.getById(id)!!.recurrence
            )
        }
    }

    @Test
    fun `observeToday solo trae pendientes con fecha de hoy o vencida`() = runTest {
        // Reloj del test fijo (NOW) en zona fija: el "hoy" es determinista.
        val havana = ZoneId.of("America/Havana")
        val endOfToday = startOfTomorrowMillis(NOW, havana)
        val todayStart = ZonedDateTime.ofInstant(Instant.ofEpochMilli(NOW), havana)
            .toLocalDate().atStartOfDay(havana).toInstant().toEpochMilli()
        val overdue = todayStart - 5 * 86_400_000L
        val future = endOfToday + 86_400_000L

        val idToday = dao.upsert(Task(title = "Hoy", dueAt = todayStart))
        val idOverdue = dao.upsert(Task(title = "Vencida", dueAt = overdue))
        // Fuera: mañana, futuro, sin fecha y ya completada.
        dao.upsert(Task(title = "Manana", dueAt = endOfToday))
        dao.upsert(Task(title = "Futura", dueAt = future))
        dao.upsert(Task(title = "Sin fecha", dueAt = null))
        dao.upsert(Task(title = "Hecha", dueAt = todayStart, isDone = true, doneAt = NOW))

        val shown = repo.observeToday(endOfToday).first().map { it.id }.toSet()
        assertEquals(setOf(idToday, idOverdue), shown)
    }
}
