package com.ahora.app.domain

import com.ahora.app.data.Tag
import com.ahora.app.data.TagDao
import com.ahora.app.data.TagPalette
import com.ahora.app.data.Task
import com.ahora.app.data.TaskBackup
import com.ahora.app.data.TaskDao
import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import com.ahora.app.data.buildSearchPattern
import com.ahora.app.data.combineTaskTags
import com.ahora.app.data.nextAfter
import com.ahora.app.data.sanitizeTagName
import com.ahora.app.data.startOfTomorrowMillis
import com.ahora.app.data.toCode
import com.ahora.app.notifications.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Lógica de negocio de las tareas.
 * Coordina la base de datos con el programador de recordatorios:
 * cada cambio en una tarea mantiene sus alarmas sincronizadas.
 *
 * [clock] es inyectable para poder probar la lógica de tiempos
 * de forma determinista en JVM.
 */
class TaskRepository(
    private val dao: TaskDao,
    private val tagDao: TagDao,
    private val scheduler: AlarmScheduler,
    private val clock: () -> Long = System::currentTimeMillis
) {

    fun observeAll(): Flow<List<Task>> = dao.observeAll()

    fun observePending(): Flow<List<Task>> = dao.observePending()

    /**
     * Pantalla «Hoy» (auditoría 1.26.0): pendientes con fecha límite hoy o
     * vencida. [endOfToday] se calcula con el reloj inyectable para poder
     * probarlo de forma determinista.
     */
    fun observeToday(endOfToday: Long = startOfTomorrowMillis(clock())): Flow<List<Task>> =
        dao.observeToday(endOfToday)

    /**
     * Busca tareas por título. Con la consulta vacía devuelve todas (así la
     * pantalla "Todas" usa un único flujo para los dos estados). La entrada
     * se consulta con debounce en el ViewModel: no se golpea la BD en cada
     * tecla, y `flatMapLatest` cancela la consulta anterior si llega otra.
     */
    fun search(query: String): Flow<List<Task>> =
        if (query.isBlank()) observeAll() else dao.search(buildSearchPattern(query))

    /** Todas las etiquetas, ordenadas por nombre. */
    fun observeTags(): Flow<List<Tag>> = tagDao.observeAll()

    /**
     * Mapa tarea → etiquetas, combinado de las etiquetas y sus
     * asignaciones (ver [combineTaskTags]). Las dos tablas son pequeñas:
     * no se materializa nada pesado en memoria.
     */
    fun observeTaskTags(): Flow<Map<Long, List<Tag>>> =
        combine(tagDao.observeAll(), tagDao.observeAssignments(), ::combineTaskTags)

    /** Crea una etiqueta. El nombre vacío lanza [IllegalArgumentException]. */
    suspend fun createTag(name: String, colorIndex: Int): Long {
        val clean = sanitizeTagName(name)
            ?: throw IllegalArgumentException("El nombre de la etiqueta no puede estar vacío")
        return tagDao.insert(
            Tag(name = clean, colorIndex = TagPalette.sanitizeColorIndex(colorIndex))
        )
    }

    /** Renombra una etiqueta o le cambia el color. */
    suspend fun updateTag(tag: Tag, name: String, colorIndex: Int) {
        val clean = sanitizeTagName(name)
            ?: throw IllegalArgumentException("El nombre de la etiqueta no puede estar vacío")
        tagDao.update(
            tag.copy(name = clean, colorIndex = TagPalette.sanitizeColorIndex(colorIndex))
        )
    }

    /**
     * Elimina una etiqueta. Sus asignaciones desaparecen solas (CASCADE en
     * la BD): las tareas no se borran, solo quedan sin esa etiqueta.
     */
    suspend fun deleteTag(tag: Tag) {
        tagDao.deleteById(tag.id)
    }

    /** Cuántas tareas usan una etiqueta (para avisar al eliminarla). */
    suspend fun tagTaskCount(tag: Tag): Int = tagDao.taskCount(tag.id)

    /** Reemplaza las etiquetas de una tarea. */
    suspend fun setTaskTags(taskId: Long, tagIds: Set<Long>) {
        tagDao.replaceTags(taskId, tagIds.toList())
    }

    suspend fun add(
        title: String,
        priority: TaskPriority = TaskPriority.NONE,
        dueAt: Long? = null,
        recurrence: TaskRecurrence = TaskRecurrence.NONE,
        /**
         * Recordatorio opcional (ETAPA 13: sale del lenguaje natural,
         * p. ej. "llamar a las 3pm"). Si ya pasó, no se programa:
         * nunca nace una alarma en el pasado.
         */
        reminderAt: Long? = null,
        /** Etiquetas de la tarea nueva (ids). */
        tagIds: Set<Long> = emptySet()
    ): Long {
        val clean = title.trim()
        require(clean.isNotEmpty()) { "El título no puede estar vacío" }
        val id = dao.upsert(
            Task(
                title = clean,
                priority = priority.level,
                dueAt = dueAt,
                recurrence = recurrence.toCode(),
                reminderAt = reminderAt?.takeIf { it > clock() }
            )
        )
        if (tagIds.isNotEmpty()) tagDao.replaceTags(id, tagIds.toList())
        val scheduled = reminderAt?.takeIf { it > clock() }
        if (scheduled != null) scheduler.schedule(id, scheduled)
        return id
    }

    suspend fun toggleDone(task: Task) {
        val recurrence = TaskRecurrence.fromCode(task.recurrence)
        val updated = if (task.isDone) {
            task.copy(isDone = false, doneAt = null)
        } else {
            // Al completar se cancela la alarma y se limpia el recordatorio:
            // si no, el pill quedaría obsoleto para siempre.
            task.copy(isDone = true, doneAt = clock(), reminderAt = null)
        }
        if (recurrence != TaskRecurrence.NONE && !task.isDone) {
            // Tarea recurrente completada (ETAPA 11): la ocurrencia queda
            // marcada como hecha y se genera la siguiente, con su fecha
            // límite y su recordatorio desplazados (nunca en el pasado).
            // La siguiente ocurrencia conserva el recordatorio: si la
            // tarea sonaba cada día a las 8, la próxima también.
            val now = clock()
            val nextDueAt = task.dueAt?.let { recurrence.nextAfter(it, now) }
            val nextReminderAt = task.reminderAt?.let { recurrence.nextAfter(it, now) }
            val next = Task(
                title = task.title,
                createdAt = now,
                reminderAt = nextReminderAt,
                priority = task.priority,
                dueAt = nextDueAt,
                recurrence = recurrence.toCode()
            )
            val nextId = dao.insertNextOccurrence(updated, next)
            scheduler.cancel(task.id)
            nextReminderAt?.let { scheduler.schedule(nextId, it) }
        } else {
            dao.upsert(updated)
            if (updated.isDone) {
                scheduler.cancel(task.id)
            } else {
                // Al desmarcar, se recupera el recordatorio si sigue siendo futuro.
                updated.reminderAt?.let { at ->
                    if (at > clock()) scheduler.schedule(task.id, at)
                }
            }
        }
    }

    suspend fun updateDetails(
        task: Task,
        title: String,
        priority: TaskPriority,
        dueAt: Long?,
        recurrence: TaskRecurrence = TaskRecurrence.NONE,
        /**
         * Etiquetas nuevas de la tarea. `null` = no tocarlas: los llamadores
         * que no manejan etiquetas (p. ej. editar solo el título) no deben
         * borrarlas sin querer.
         */
        tagIds: Set<Long>? = null
    ) {
        val clean = title.trim()
        require(clean.isNotEmpty()) { "El título no puede estar vacío" }
        dao.upsert(
            task.copy(
                title = clean,
                priority = priority.level,
                dueAt = dueAt,
                recurrence = recurrence.toCode()
            )
        )
        tagIds?.let { tagDao.replaceTags(task.id, it.toList()) }
    }

    /** Elimina y devuelve la tarea para poder deshacer. */
    suspend fun delete(task: Task): Task {
        scheduler.cancel(task.id)
        dao.deleteById(task.id)
        return task
    }

    suspend fun restore(task: Task) {
        dao.upsert(task)
        task.reminderAt?.let { at ->
            if (at > clock() && !task.isDone) {
                scheduler.schedule(task.id, at)
            }
        }
    }

    suspend fun setReminder(task: Task, at: Long) {
        require(at > clock()) { "El recordatorio debe ser en el futuro" }
        dao.upsert(task.copy(reminderAt = at))
        scheduler.schedule(task.id, at)
    }

    suspend fun clearReminder(task: Task) {
        scheduler.cancel(task.id)
        dao.upsert(task.copy(reminderAt = null))
    }

    /** Reprograma todos los recordatorios pendientes (p. ej. tras reiniciar el teléfono). */
    suspend fun rescheduleAll() {
        dao.getPendingReminders(clock()).forEach { task ->
            task.reminderAt?.let { scheduler.schedule(task.id, it) }
        }
    }

    /**
     * Limpia los recordatorios ya vencidos (el teléfono pudo estar apagado
     * cuando debían sonar): el pill no debe mostrar una hora del pasado.
     */
    suspend fun pruneExpiredReminders() {
        dao.clearExpiredReminders(clock())
    }

    /**
     * true si hay al menos un recordatorio futuro pendiente. Se usa para
     * decidir si vale la pena avisar sobre el permiso de alarmas exactas:
     * sin recordatorios pendientes, el aviso sería ruido.
     */
    suspend fun hasFutureReminders(): Boolean =
        dao.hasPendingReminders(clock())

    /** Resultado de importar un respaldo: cuántas entraron y cuántas se omitieron. */
    data class ImportResult(val imported: Int, val skipped: Int)

    /**
     * Exporta todas las tareas y etiquetas a JSON. La app no usa el respaldo en la nube
     * de Android: este archivo es el respaldo del usuario, bajo su control.
     */
    suspend fun exportTasks(): String {
        val tasks = dao.getAll()
        val tags = tagDao.getAll()
        val taskTags = combineTaskTags(tags, tagDao.getAllAssignments())
        return TaskBackup.tasksToJson(tasks, tags, taskTags, clock())
    }

    /**
     * Importa tareas desde un respaldo JSON. Es idempotente: conserva los
     * ids, así que importar dos veces no duplica (REPLACE). Al final limpia
     * recordatorios vencidos y reprograma las alarmas con la reconciliación
     * existente: tras reinstalar e importar, todo vuelve a sonar.
     *
     * Las etiquetas (formato v2) se importan por nombre: las que ya existen
     * (insensible a mayúsculas) se reutilizan, las nuevas se crean, y cada
     * tarea importada recibe las suyas. Importar dos veces no duplica
     * etiquetas. Los respaldos v1 no traen etiquetas: no se toca nada.
     */
    suspend fun importTasks(json: String): ImportResult {
        val parsed = TaskBackup.tasksFromJson(json)
        // Se cancelan las alarmas antes de reemplazar, y el lote entra en
        // una sola transacción (upsertAll) en vez de una por tarea.
        parsed.tasks.forEach { task -> scheduler.cancel(task.id) }
        dao.upsertAll(parsed.tasks)
        importTags(parsed)
        pruneExpiredReminders()
        rescheduleAll()
        return ImportResult(parsed.tasks.size, parsed.skipped)
    }

    private suspend fun importTags(parsed: TaskBackup.ParsedBackup) {
        if (parsed.tags.isEmpty() && parsed.tagAssignments.isEmpty()) return
        val byName = tagDao.getAll()
            .associateBy { it.name.lowercase() }
            .toMutableMap()
        for (tag in parsed.tags) {
            val key = tag.name.lowercase()
            if (key !in byName) {
                val id = tagDao.insert(tag.copy(id = 0))
                byName[key] = tag.copy(id = id)
            }
        }
        val importedIds = parsed.tasks.map { it.id }.toSet()
        for ((taskId, names) in parsed.tagAssignments) {
            // Solo tareas que entraron en este respaldo: si el archivo se
            // editó a mano, no se tocan tareas ajenas.
            if (taskId !in importedIds) continue
            val ids = names.mapNotNull { byName[it.lowercase()]?.id }.toSet()
            tagDao.replaceTags(taskId, ids.toList())
        }
    }
}
