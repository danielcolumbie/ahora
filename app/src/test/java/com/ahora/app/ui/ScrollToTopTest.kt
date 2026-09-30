package com.ahora.app.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ahora.app.data.SettingsRepository
import com.ahora.app.data.Task
import com.ahora.app.data.TaskDao
import com.ahora.app.domain.TaskRepository
import com.ahora.app.notifications.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

/**
 * Tocar la pestaña activa emite un evento de "subir al inicio" (bloque H).
 *
 * Usa un [TaskDao] y un [AlarmScheduler] falsos mínimos (solo lo que el
 * ViewModel toca en su `init`) y un DataStore real sobre archivo temporal,
 * como [com.ahora.app.data.SettingsRepositoryTest].
 *
 * Detalles que costó aprender:
 * - `ViewModel.viewModelScope` corre sobre `Dispatchers.Main`, que no
 *   existe en tests JVM: se instala un Main de prueba y se restaura.
 * - El `init` del ViewModel lanza `refreshExactAlarmNudge()` en
 *   `Dispatchers.IO` (hilo real) y ese camino puede leer el DataStore:
 *   el DataStore de prueba usa un scope con dispatchers reales, no el
 *   `testScheduler` virtual, para que la lectura nunca espere al
 *   scheduler y `closeStore` no se bloquee.
 * - El flujo tiene `replay = 0`: un colector nuevo NO recibe lo emitido
 *   antes de suscribirse (`extraBufferCapacity` es buffer para emisores,
 *   no replay). El test suscribe primero y emite después, que es el
 *   patrón real de las pantallas (recolectan continuamente).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScrollToTopTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeTaskDao : TaskDao {
        private val tasks = MutableStateFlow<List<Task>>(emptyList())
        override fun observeAll(): Flow<List<Task>> = tasks
        override fun observePending(): Flow<List<Task>> =
            tasks.map { list -> list.filter { !it.isDone } }
        override fun observeToday(endOfToday: Long): Flow<List<Task>> =
            tasks.map { list -> list.filter { !it.isDone && it.dueAt != null && it.dueAt < endOfToday } }
        override suspend fun getPendingForWidget(limit: Int): List<Task> = emptyList()
        override fun search(pattern: String): Flow<List<Task>> = tasks
        override suspend fun upsert(task: Task): Long = 0L
        override suspend fun upsertAll(tasks: List<Task>) = Unit
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun getById(id: Long): Task? = null
        override suspend fun getAll(): List<Task> = emptyList()
        override suspend fun getPendingReminders(now: Long): List<Task> = emptyList()
        override suspend fun hasPendingReminders(now: Long): Boolean = false
        override suspend fun clearExpiredReminders(now: Long) = Unit
    }

    private class FakeScheduler : AlarmScheduler {
        override fun schedule(taskId: Long, atMillis: Long) = Unit
        override fun cancel(taskId: Long) = Unit
        // false: el init no escribe en el DataStore (solo lo haría si el
        // permiso volviera, para rearmar el aviso).
        override fun hasExactAlarmPermission(): Boolean = false
        override fun exactAlarmSettingsIntent(): android.content.Intent? = null
    }

    private fun TestScope.storeScope(): CoroutineScope =
        // Dispatchers reales, no el testScheduler virtual (ver el KDoc).
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun settings(scope: CoroutineScope): SettingsRepository =
        SettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = {
                    File(tempFolder.root, "scroll-${UUID.randomUUID()}.preferences_pb")
                }
            )
        )

    private suspend fun closeStore(scope: CoroutineScope) {
        scope.coroutineContext[Job]!!.cancelAndJoin()
    }

    @Test
    fun `requestScrollToTop emite un evento de subida al inicio`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val scope = storeScope()
            val vm = MainViewModel(
                TaskRepository(FakeTaskDao(), FakeScheduler()),
                FakeScheduler(),
                settings(scope)
            )
            // Patrón real de las pantallas: recolección continua.
            val received = mutableListOf<Unit>()
            val collector = launch { vm.scrollToTopEvents.collect { received += it } }
            testScheduler.runCurrent() // el colector ya está suscrito
            vm.requestScrollToTop()
            testScheduler.runCurrent() // se entrega el evento
            assertEquals(listOf(Unit), received)
            collector.cancel()
            closeStore(scope)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
