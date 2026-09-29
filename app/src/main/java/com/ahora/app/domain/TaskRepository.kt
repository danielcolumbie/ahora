package com.ahora.app.domain

import com.ahora.app.data.Task
import com.ahora.app.data.TaskDao
import com.ahora.app.notifications.AlarmScheduler
import kotlinx.coroutines.flow.Flow

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
    private val scheduler: AlarmScheduler,
    private val clock: () -> Long = System::currentTimeMillis
) {

    fun observeAll(): Flow<List<Task>> = dao.observeAll()

    fun observePending(): Flow<List<Task>> = dao.observePending()

    suspend fun add(title: String): Long {
        val clean = title.trim()
        require(clean.isNotEmpty()) { "El título no puede estar vacío" }
        return dao.upsert(Task(title = clean))
    }

    suspend fun toggleDone(task: Task) {
        val updated = if (task.isDone) {
            task.copy(isDone = false, doneAt = null)
        } else {
            // Al completar se cancela la alarma y se limpia el recordatorio:
            // si no, el pill quedaría obsoleto para siempre.
            task.copy(isDone = true, doneAt = clock(), reminderAt = null)
        }
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

    suspend fun updateTitle(task: Task, title: String) {
        val clean = title.trim()
        require(clean.isNotEmpty()) { "El título no puede estar vacío" }
        dao.upsert(task.copy(title = clean))
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
        dao.getPendingReminders().forEach { task ->
            task.reminderAt?.let { scheduler.schedule(task.id, it) }
        }
    }
}
