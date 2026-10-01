package com.ahora.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Tag>>

    /**
     * Todas las asignaciones tarea ↔ etiqueta: el ViewModel las combina
     * con las tareas (ver [combineTaskTags]). Las dos tablas son pequeñas;
     * no se materializa nada pesado.
     */
    @Query("SELECT * FROM task_tags")
    fun observeAssignments(): Flow<List<TaskTagCrossRef>>

    @Insert
    suspend fun insert(tag: Tag): Long

    @Update
    suspend fun update(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteById(tagId: Long)

    /** Cuántas tareas usan una etiqueta (para avisar al eliminarla). */
    @Query("SELECT COUNT(*) FROM task_tags WHERE tagId = :tagId")
    suspend fun taskCount(tagId: Long): Int

    @Query("SELECT tagId FROM task_tags WHERE taskId = :taskId")
    suspend fun tagIdsForTask(taskId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assignAll(refs: List<TaskTagCrossRef>)

    @Query("DELETE FROM task_tags WHERE taskId = :taskId AND tagId IN (:tagIds)")
    suspend fun unassignAll(taskId: Long, tagIds: List<Long>)

    /**
     * Reemplaza las etiquetas de una tarea en una sola transacción: quita
     * las que sobran y añade las nuevas. Con la lista vacía, la tarea
     * queda sin etiquetas.
     */
    @Transaction
    suspend fun replaceTags(taskId: Long, tagIds: List<Long>) {
        val wanted = tagIds.toSet()
        val current = tagIdsForTask(taskId).toSet()
        val toRemove = (current - wanted).toList()
        // Room no admite `IN ()` vacío: por eso el guard con isNotEmpty.
        if (toRemove.isNotEmpty()) unassignAll(taskId, toRemove)
        val toAdd = (wanted - current).map { TaskTagCrossRef(taskId, it) }
        if (toAdd.isNotEmpty()) assignAll(toAdd)
    }

    /** Todas las etiquetas, para el respaldo local (Ajustes → Respaldo). */
    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<Tag>

    /** Todas las asignaciones, para el respaldo local. */
    @Query("SELECT * FROM task_tags")
    suspend fun getAllAssignments(): List<TaskTagCrossRef>
}
