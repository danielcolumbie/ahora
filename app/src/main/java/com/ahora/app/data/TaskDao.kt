package com.ahora.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isDone ASC, createdAt DESC")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDone = 0 ORDER BY createdAt DESC")
    fun observePending(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task): Long

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
