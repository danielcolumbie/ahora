package com.ahora.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ahora.app.data.AhoraDatabase
import com.ahora.app.data.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas instrumentadas del [TaskDao] con Room en memoria.
 *
 * Se ejecutan en un dispositivo o emulador Android porque Room necesita
 * el SQLite de la plataforma; no son pruebas JVM puras. Compilar con:
 * `./build-android.sh compileDebugAndroidTestKotlin`.
 */
@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var db: AhoraDatabase

    @Before
    fun crearBd() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AhoraDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun cerrarBd() {
        db.close()
    }

    private fun tarea(
        titulo: String,
        done: Boolean = false,
        reminderAt: Long? = null
    ) = Task(
        title = titulo,
        isDone = done,
        reminderAt = reminderAt,
        createdAt = 1_700_000_000_000L
    )

    @Test
    fun insertarYLeerPorId() = runBlocking {
        val id = db.taskDao().upsert(tarea("Comprar pan"))

        val leida = db.taskDao().getById(id)
        assertNotNull(leida)
        assertEquals("Comprar pan", leida!!.title)
        assertEquals(id, leida.id)
    }

    @Test
    fun actualizarReemplazaLaFila() = runBlocking {
        val id = db.taskDao().upsert(tarea("Original"))
        db.taskDao().upsert(tarea("Modificada").copy(id = id))

        assertEquals("Modificada", db.taskDao().getById(id)?.title)
    }

    @Test
    fun borrarEliminaLaFila() = runBlocking {
        val id = db.taskDao().upsert(tarea("Temporal"))
        db.taskDao().deleteById(id)

        assertNull(db.taskDao().getById(id))
    }

    @Test
    fun observeAllEmiteTodasOrdenadas() = runBlocking {
        db.taskDao().upsert(tarea("Pendiente"))
        db.taskDao().upsert(tarea("Hecha", done = true))

        val todas = db.taskDao().observeAll().first()
        assertEquals(2, todas.size)
        // Las pendientes van primero.
        assertEquals("Pendiente", todas[0].title)
        assertTrue(todas[1].isDone)
    }

    @Test
    fun observePendingSoloEmitePendientes() = runBlocking {
        db.taskDao().upsert(tarea("Pendiente"))
        db.taskDao().upsert(tarea("Hecha", done = true))

        val pendientes = db.taskDao().observePending().first()
        assertEquals(1, pendientes.size)
        assertEquals("Pendiente", pendientes[0].title)
    }

    @Test
    fun getPendingRemindersSoloFuturosYPendientes() = runBlocking {
        val ahora = 1_700_000_000_000L
        db.taskDao().upsert(tarea("Futura", reminderAt = ahora + 3_600_000L))
        db.taskDao().upsert(tarea("Vencida", reminderAt = ahora - 3_600_000L))
        db.taskDao().upsert(tarea("Hecha con recordatorio", done = true, reminderAt = ahora + 3_600_000L))
        db.taskDao().upsert(tarea("Sin recordatorio"))

        val pendientes = db.taskDao().getPendingReminders(ahora)
        assertEquals(1, pendientes.size)
        assertEquals("Futura", pendientes[0].title)
    }

    @Test
    fun searchFiltraPorTituloIgnorandoMayusculas() = runBlocking {
        db.taskDao().upsert(tarea("Comprar PAN"))
        db.taskDao().upsert(tarea("Llamar al banco"))

        val resultados =
            db.taskDao().search(com.ahora.app.data.buildSearchPattern("pan")).first()
        assertEquals(1, resultados.size)
        assertEquals("Comprar PAN", resultados[0].title)
    }

    @Test
    fun searchNoTrataLosComodinesComoPatron() = runBlocking {
        db.taskDao().upsert(tarea("Oferta 100% real"))
        db.taskDao().upsert(tarea("Revisar 1000 correos"))

        // Sin el escape, "100%" como LIKE matchearía "1000 correos" también.
        val resultados =
            db.taskDao().search(com.ahora.app.data.buildSearchPattern("100%")).first()
        assertEquals(1, resultados.size)
        assertEquals("Oferta 100% real", resultados[0].title)
    }

    @Test
    fun searchOrdenaPendientesPrimero() = runBlocking {
        db.taskDao().upsert(tarea("Comprar pan", done = true))
        db.taskDao().upsert(tarea("Comprar leche"))

        val resultados =
            db.taskDao().search(com.ahora.app.data.buildSearchPattern("comprar")).first()
        assertEquals(2, resultados.size)
        assertEquals("Comprar leche", resultados[0].title)
        assertTrue(resultados[1].isDone)
    }
}
