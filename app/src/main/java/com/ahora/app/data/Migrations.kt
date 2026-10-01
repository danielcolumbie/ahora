package com.ahora.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migración 1 → 2 (ETAPA 10: prioridades y fechas).
 *
 * Añade las columnas `priority` (entero, 0 = sin prioridad por defecto)
 * y `dueAt` (fecha límite en milisegundos, NULL = sin fecha).
 * Las tareas existentes quedan con prioridad NONE y sin fecha: ningún
 * dato previo cambia de significado.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN dueAt INTEGER")
    }
}

/**
 * Migración 2 → 3 (1.28.0: etiquetas).
 *
 * Crea las tablas `tags` y `task_tags` (relación muchos-a-muchos con
 * borrado en cascada e índices en ambas columnas). No toca la tabla
 * `tasks`: ningún dato previo cambia de significado.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS tags (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "colorIndex INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS task_tags (" +
                "taskId INTEGER NOT NULL, " +
                "tagId INTEGER NOT NULL, " +
                "PRIMARY KEY(taskId, tagId), " +
                "FOREIGN KEY(taskId) REFERENCES tasks(id) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(tagId) REFERENCES tags(id) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_task_tags_taskId ON task_tags (taskId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_task_tags_tagId ON task_tags (tagId)")
    }
}
