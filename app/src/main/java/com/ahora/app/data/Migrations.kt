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
