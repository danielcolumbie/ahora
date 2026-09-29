package com.ahora.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ahora.app.data.AhoraDatabase
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
}
