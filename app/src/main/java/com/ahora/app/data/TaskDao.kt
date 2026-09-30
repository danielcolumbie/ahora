package com.ahora.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    /**
     * Orden de la lista (ver [TaskListComparator], que lo replica en Kotlin
     * puro para los tests JVM): pendientes primero, luego mayor prioridad,
     * luego fecha límite más cercana (sin fecha al final) y desempate por
     * creación reciente.
     */
    @Query("SELECT * FROM tasks ORDER BY isDone ASC, priority DESC, (dueAt IS NULL), dueAt ASC, createdAt DESC")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDone = 0 ORDER BY priority DESC, (dueAt IS NULL), dueAt ASC, createdAt DESC")
    fun observePending(): Flow<List<Task>>

    /**
     * Tareas de la pantalla «Hoy» (auditoría 1.26.0): solo pendientes con
     * fecha límite hoy o ya vencida. [endOfToday] es el inicio de mañana
     * en milisegundos (límite exclusivo, ver [startOfTomorrowMillis]);
     * las tareas sin fecha viven en «Todas», no en «Hoy».
     */
    @Query("SELECT * FROM tasks WHERE isDone = 0 AND dueAt IS NOT NULL AND dueAt < :endOfToday ORDER BY priority DESC, dueAt ASC, createdAt DESC")
    fun observeToday(endOfToday: Long): Flow<List<Task>>

    /**
     * Pendientes para el widget (ETAPA 12): el mismo orden que
     * [observePending] pero con límite, en una sola consulta suspendida.
     * El widget solo necesita las primeras filas, no toda la tabla en
     * memoria (teléfonos modestos).
     */
    @Query("SELECT * FROM tasks WHERE isDone = 0 ORDER BY priority DESC, (dueAt IS NULL), dueAt ASC, createdAt DESC LIMIT :limit")
    suspend fun getPendingForWidget(limit: Int): List<Task>

    /**
     * Búsqueda por título. [pattern] ya viene escapado desde
     * [buildSearchPattern] (`%`, `_` y `\` son literales, no comodines).
     * El orden es el mismo que [observeAll]: pendientes primero.
     */
    @Query("SELECT * FROM tasks WHERE title LIKE :pattern ESCAPE '\\' ORDER BY isDone ASC, priority DESC, (dueAt IS NULL), dueAt ASC, createdAt DESC")
    fun search(pattern: String): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task): Long

    /**
     * Marca [done] como completada y crea [next] como la siguiente
     * ocurrencia en una sola transacción: o entran las dos o ninguna.
     * Devuelve el id de la nueva ocurrencia (para programar su alarma).
     * Se usa al completar una tarea recurrente (ETAPA 11).
     */
    @Transaction
    suspend fun insertNextOccurrence(done: Task, next: Task): Long {
        upsert(done)
        return upsert(next)
    }

    /**
     * Inserción en lote: Room la envuelve en una sola transacción.
     * Se usa al importar un respaldo para no abrir una transacción
     * por tarea (menos escrituras a disco, menos batería).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<Task>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    /** Todas las tareas, para el respaldo local (Ajustes → Respaldo). */
    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<Task>

    @Query("SELECT * FROM tasks WHERE reminderAt IS NOT NULL AND isDone = 0 AND reminderAt > :now")
    suspend fun getPendingReminders(now: Long = System.currentTimeMillis()): List<Task>

    /**
     * Consulta de existencia para saber si hay recordatorios por sonar
     * sin materializar la lista completa en memoria: basta un booleano.
     * (Antes se cargaban todas las tareas pendientes solo para ver si
     * la lista estaba vacía.)
     */
    @Query("SELECT EXISTS(SELECT 1 FROM tasks WHERE reminderAt IS NOT NULL AND isDone = 0 AND reminderAt > :now)")
    suspend fun hasPendingReminders(now: Long = System.currentTimeMillis()): Boolean

    /**
     * Limpia los recordatorios ya vencidos para que el pill de la tarea no
     * muestre una hora obsoleta (p. ej. si el teléfono estuvo apagado cuando
     * debía sonar). No borra tareas, solo el campo del recordatorio.
     */
    @Query("UPDATE tasks SET reminderAt = NULL WHERE reminderAt IS NOT NULL AND isDone = 0 AND reminderAt <= :now")
    suspend fun clearExpiredReminders(now: Long = System.currentTimeMillis())
}
