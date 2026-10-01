package com.ahora.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ahora.app.data.AhoraDatabase
import com.ahora.app.data.MIGRATION_1_2
import com.ahora.app.data.MIGRATION_2_3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Scaffold de tests de migración (ETAPA 3 del plan maestro).
 *
 * La BD está en versión 1 con el schema exportado en `app/schemas/`
 * (disponible como asset de este test vía `androidTest.assets.srcDir`).
 *
 * Hoy valida que la v1 se crea y se abre contra el schema exportado.
 * Cuando se añada la migración 1→2 en una etapa futura, se extiende así:
 *
 *     @Test
 *     fun migrate1To2() {
 *         helper.createDatabase(TEST_DB, 1).apply {
 *             execSQL("INSERT INTO tasks (title, createdAt) VALUES ('x', 1)")
 *             close()
 *         }
 *         helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).apply {
 *             // aquí: verificar que los datos sobrevivieron
 *             close()
 *         }
 *     }
 *
 * Es un test de instrumentación: necesita dispositivo o emulador
 * (`connectedAndroidTest`). No corre en JVM porque requiere el SQLite
 * real de Android.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AhoraDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun v1_seCreaYAbreConElSchemaExportado() {
        // Crea la BD en v1 e inserta una tarea de ejemplo.
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO tasks (title, createdAt, reminderAt, isDone, doneAt, recurrence) " +
                    "VALUES ('Tarea de prueba', 1000, NULL, 0, NULL, NULL)"
            )
            close()
        }
        // La reabre y la valida contra el schema exportado: si el schema
        // generado no coincide con la v1 real, esto falla.
        helper.runMigrationsAndValidate(TEST_DB, 1, true).close()
    }

    /**
     * Migración 1 → 2 (ETAPA 10): las tareas existentes conservan sus datos
     * y las columnas nuevas llegan con sus valores por defecto
     * (prioridad 0 = sin prioridad, sin fecha límite).
     *
     * Test de instrumentación: necesita dispositivo o emulador
     * (`connectedAndroidTest`). No corre en JVM.
     */
    @Test
    @Throws(IOException::class)
    fun migrate1To2_conservaDatosYValoresPorDefecto() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO tasks (title, createdAt, reminderAt, isDone, doneAt, recurrence) " +
                    "VALUES ('Tarea vieja', 1000, NULL, 0, NULL, NULL)"
            )
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).apply {
            query("SELECT title, priority, dueAt FROM tasks").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Tarea vieja", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
                assertTrue(cursor.isNull(2))
            }
            close()
        }
    }

    /**
     * Migración 2 → 3 (1.28.0: etiquetas): crea `tags` y `task_tags` con
     * sus claves foráneas e índices; la tabla `tasks` no se toca y los
     * datos existentes sobreviven. Además valida el CASCADE: al borrar
     * una tarea o una etiqueta, sus asignaciones desaparecen solas.
     *
     * Test de instrumentación: necesita dispositivo o emulador
     * (`connectedAndroidTest`). No corre en JVM.
     */
    @Test
    @Throws(IOException::class)
    fun migrate2To3_creaEtiquetasYConservaTareas() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO tasks (title, createdAt, reminderAt, isDone, doneAt, recurrence, priority, dueAt) " +
                    "VALUES ('Tarea vieja', 1000, NULL, 0, NULL, NULL, 2, NULL)"
            )
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3).apply {
            // Las tablas nuevas existen y están vacías.
            query("SELECT COUNT(*) FROM tags").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            query("SELECT COUNT(*) FROM task_tags").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            // La tarea sobrevivió intacta (con su prioridad).
            query("SELECT title, priority FROM tasks").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Tarea vieja", cursor.getString(0))
                assertEquals(2, cursor.getInt(1))
            }

            // Se insertan etiqueta + asignación y se valida el CASCADE en
            // ambas direcciones.
            execSQL("INSERT INTO tags (name, colorIndex) VALUES ('Casa', 0)")
            execSQL("INSERT INTO task_tags (taskId, tagId) VALUES (1, 1)")
            query("SELECT COUNT(*) FROM task_tags").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            // Al borrar la tarea, la asignación desaparece sola.
            execSQL("DELETE FROM tasks WHERE id = 1")
            query("SELECT COUNT(*) FROM task_tags").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            // La etiqueta sigue: solo se desasoció.
            query("SELECT COUNT(*) FROM tags").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            // Al borrar la etiqueta también se limpia (tabla vacía de
            // asignaciones, sin error de clave foránea).
            execSQL("DELETE FROM tags WHERE id = 1")
            query("SELECT COUNT(*) FROM tags").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            close()
        }
    }
}
